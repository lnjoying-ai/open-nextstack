// Copyright 2024 The NEXTSTACK Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.lnjoying.justice.network.processor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.request.SubnetCreateFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.BaseRsp;
import com.lnjoying.justice.network.domain.backend.response.SubnetFromAgentRsp;
import com.lnjoying.justice.network.entity.Subnet;
import com.lnjoying.justice.network.entity.Vpc;
import com.lnjoying.justice.network.service.SubnetService;
import com.lnjoying.justice.network.service.VpcService;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.lnjoying.justice.network.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class SubnetTimerProcessor extends AbstractRunnableProcessor
{


    @Autowired
    private SubnetService subnetService;

    @Autowired
    private VpcService vpcService;

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    private CombRpcSerice combRpcSerice;

    public SubnetTimerProcessor() {}

    @Override
    public void start() {
        log.info("subnet timer processor start");
    }

    @Override
    public void stop() {
        log.info("subnet timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            List<Subnet> tblSubnets = getMiddleStatusSubnets();
            if (tblSubnets != null && tblSubnets.size() > 0) {
                for (Subnet tblSubnet : tblSubnets) {
                    processSubnet(tblSubnet);
                }
            }
        }
        catch (Exception e)
        {
            log.error("subnet timer processor exception: {}", e.getMessage());
        }
    }

    private void processSubnet(Subnet tblSubnet) {
        int phaseStatus = tblSubnet.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processAdding(tblSubnet);
                    break;
                case PhaseStatus.AGENT_ADDING:
                    processCheckAgentAddingResult(tblSubnet);
                    break;
                case PhaseStatus.DELETING:
                    processRemoving(tblSubnet);
                    break;
                case PhaseStatus.AGENT_DELETING:
                    processCheckAgentDeletingResult(tblSubnet);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("subnet timer processor error: subnetId {}, phase status {} , exception {}", tblSubnet.getSubnetId(), phaseStatus, e.getMessage());
        }
    }

    private List<Subnet> getMiddleStatusSubnets()
    {
        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Subnet::getPhaseStatus, PhaseStatus.ADDED)
                .ne(Subnet::getPhaseStatus, REMOVED)
                .ne(Subnet::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(Subnet::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(Subnet::getPhaseStatus, PhaseStatus.AGENT_ADDING_ERR)
                .ne(Subnet::getPhaseStatus, PhaseStatus.AGENT_DELETING_ERR);
        return subnetService.list(queryWrapper);
    }

    private void processAdding(Subnet tblSubnet)
    {
        String cidr = tblSubnet.getCidr();
        String vpcId = tblSubnet.getVpcId();
        Vpc tblVpc = vpcService.getById(vpcId);
        if (StrUtil.isBlank(tblVpc.getVpcIdFromAgent())) return;
        if (PhaseStatus.ADDED == tblVpc.getPhaseStatus() && !StrUtil.isBlank(cidr))
        {
            BaseRsp result = createSubnet(tblVpc.getVpcIdFromAgent(), cidr);
            if (null == result)
            {
                log.error("create subnet failed, subnetId: {}, result is null", tblSubnet.getSubnetId());
                return;
            }
            if (AgentConstant.FAILED.equals(result.getStatus()))
            {
                log.error("create subnet failed, subnetId: {}", tblSubnet.getSubnetId());
                tblSubnet.setPhaseStatus(PhaseStatus.ADD_FAILED);
                tblSubnet.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                boolean ok = subnetService.updateById(tblSubnet);
                if (!ok)
                {
                    log.error("current status is ADDING: update database error.");
                }
                else
                {
                    logRpcService.getLogService().addEvent(tblSubnet.getUserId(), "Agent 创建Subnet异常",
                            String.format("请求参数 cidr:%s", tblSubnet.getCidr()), "创建失败");
                }
            }
            else if (AgentConstant.PENDING.equals(result.getStatus()))
            {
                tblSubnet.setPhaseStatus(PhaseStatus.AGENT_ADDING);
                tblSubnet.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                tblSubnet.setSubnetIdFromAgent(result.getUuid());
                boolean ok = subnetService.updateById(tblSubnet);
                if (!ok)
                {
                    log.error("current status is ADDING: update database error.");
                }
                else
                {
                    logRpcService.getLogService().addEvent(tblSubnet.getUserId(), "Agent 正在创建Subnet",
                            String.format("请求参数 cidr:%s", tblSubnet.getCidr()), "创建中");
                }
            }
        }
    }

    private void processRemoving(Subnet tblSubnet)
    {
        String status = removeSubnet(tblSubnet);
        if (AgentConstant.PENDING.equals(status) || AgentConstant.FAILED.equals(status))
        {
            tblSubnet.setPhaseStatus(PhaseStatus.AGENT_DELETING);
        }
        tblSubnet.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = subnetService.updateById(tblSubnet);
        if (!ok)
        {
            log.error("current status is REMOVING: update database error.");
        }
        else
        {
            logRpcService.getLogService().addEvent(tblSubnet.getUserId(),"Agent 正在删除subnet",
                    String.format("请求参数 vpcId:%s",tblSubnet.getSubnetId()), "删除中");
        }
    }

    private void processCheckAgentAddingResult(Subnet tblSubnet)
    {
        String subnetId = tblSubnet.getSubnetIdFromAgent();
        SubnetFromAgentRsp rsp = getSubnet(subnetId);
        if (rsp == null) {
            log.error("get subnet from agent failed, subnetId: {}", tblSubnet.getSubnetId());
            return;
        }
        if (Objects.equals(rsp.getPhaseStatus(), AgentConstant.SUCCESS) &&
                Objects.equals(rsp.getStatus(), AgentConstant.OK))
        {
            tblSubnet.setPhaseStatus(PhaseStatus.ADDED);
            tblSubnet.setSubnetIdFromAgent(rsp.getSubnetIdFromAgent());
        }
        else if (Objects.equals(rsp.getStatus(), AgentConstant.FAILED))
        {
            tblSubnet.setPhaseStatus(PhaseStatus.AGENT_ADDING_ERR);
        }
        tblSubnet.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = subnetService.updateById(tblSubnet);
        if (!ok)
        {
            log.error("current status is AGENT_ADDING: update database error.");
        }
        else
        {
            String logResult = String.format("创建中,subnetId: %s",tblSubnet.getSubnetId());
            if (PhaseStatus.AGENT_ADDING_ERR == tblSubnet.getPhaseStatus())
            {
                logResult = "创建失败";
            }
            else if (PhaseStatus.ADDED == tblSubnet.getPhaseStatus())
            {
                logResult = String.format("创建成功,subnetId: %s",tblSubnet.getSubnetId());
            }
            logRpcService.getLogService().addEvent(tblSubnet.getUserId(),"Agent 获取subnet的状态",
                    String.format("获取subnet的状态,subnetId:%s",tblSubnet.getSubnetId()), logResult);
        }
    }

    // Check the result of the backend service processing the delete request
    private void processCheckAgentDeletingResult(Subnet tblSubnet) {
        String subnetId = tblSubnet.getSubnetIdFromAgent();
        SubnetFromAgentRsp rsp = getSubnet(subnetId);
        if (rsp == null) {
            log.error("get subnet from agent failed, subnetId: {}", tblSubnet.getSubnetId());
            return;
        }
        if (Objects.equals(rsp.getPhaseStatus(), AgentConstant.FAIL) &&
                Objects.equals(rsp.getStatus(), AgentConstant.OK)) {
            tblSubnet.setPhaseStatus(PhaseStatus.AGENT_DELETING_ERR);
        } else if (Objects.equals(rsp.getStatus(), AgentConstant.FAILED) &&
                rsp.getReason().contains(AgentConstant.NOT_FOUND)) {
            tblSubnet.setPhaseStatus(REMOVED);
        }
        tblSubnet.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = subnetService.updateById(tblSubnet);
        if (!ok) {
            log.error("current status is AGENT_ADDING: update database error.");
        } else {
            log.info("subnet:(subnet: {},subnetIdFromAgent: {}) is deleted successfully."
                    , tblSubnet.getSubnetId(), tblSubnet.getSubnetIdFromAgent());
            logRpcService.getLogService().addEvent(tblSubnet.getUserId(),"Agent 删除subnet成功",
                    String.format("请求参数 subnetId:%s",tblSubnet.getSubnetId()), "删除成功");
        }
    }

    // create subnet from agent
    // return null if an exception occurred on the request backend
    private BaseRsp createSubnet(String vpcIdFromAgent,String cidr)
    {
        String postUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getVpcUrl() + "/"
                + vpcIdFromAgent + "/" + AgentConstant.SUBNETS;
        BaseRsp result ;
        SubnetCreateFromAgentReq createSubnet = new SubnetCreateFromAgentReq(cidr);
        String jsonString = JsonUtil.objectToJson(createSubnet);
        log.info("create subnet from agent, url:{}, request body: {}", postUrl, jsonString);
        try {
            result = HttpActionUtil.post(postUrl, jsonString, BaseRsp.class);
            log.info("create subnet from agent, result :{}", result);
        } catch (Exception e) {
            log.error("create subnet from agent failed, error: {}", e.getMessage());
            return null;
        }
        return result;
    }

    // get subnet info from agent
    // return null if an exception occurred on the request backend
    private SubnetFromAgentRsp getSubnet(String subnetId)
    {
        String getUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getSubnetUrl() + "/" + subnetId;
        SubnetFromAgentRsp result = null;
        try {
            result = HttpActionUtil.getObject(getUrl, SubnetFromAgentRsp.class);
        } catch (Exception e) {
            log.error("get subnet from agent failed, error: {}", e.getMessage());
            return null;
        }
        log.info("get subnet from agent, result :{}", result);
        return result;
    }

    // delete subnet from agent
    // return null if an exception occurred on the request backend
    private String removeSubnet(Subnet tblSubnet)
    {
        String delUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getSubnetUrl() + "/" + tblSubnet.getSubnetIdFromAgent();
        log.info("delete subnet from agent, subnetId: {}", tblSubnet.getSubnetId());
        BaseRsp baseRsp;
        try {
            baseRsp = HttpActionUtil.delete(delUrl, BaseRsp.class);
            if (baseRsp != null && StrUtil.isBlank(baseRsp.getReason())) {
                return baseRsp.getStatus();
            }
            if (baseRsp != null && StrUtil.isNotBlank(baseRsp.getReason()))  {
                if (baseRsp.getReason().contains(AgentConstant.NOT_FOUND)) {
                    return AgentConstant.PENDING;
                }
                log.error("delete subnet from agent error, subnetId: {}, reason: {}", tblSubnet.getSubnetId(), baseRsp.getReason());
                return baseRsp.getReason();
            }
            if (baseRsp == null)
            {
                log.error("delete subnet from agent error, subnetId: {}, baseRsp is null", tblSubnet.getSubnetId());
                return null;
            }
        } catch (Exception e) {
            log.error("delete subnet from agent error, subnetId: {}", tblSubnet.getSubnetId());
            return null;
        }
        return baseRsp.getStatus();
    }
}
