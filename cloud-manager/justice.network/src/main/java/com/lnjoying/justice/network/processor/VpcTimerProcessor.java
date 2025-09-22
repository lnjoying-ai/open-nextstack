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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.request.SubnetCreateFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.BaseRsp;
import com.lnjoying.justice.network.domain.backend.response.VpcFromAgentRsp;
import com.lnjoying.justice.network.entity.Vpc;
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
public class VpcTimerProcessor extends AbstractRunnableProcessor
{

//    private static final Logger LOGGER = LogManager.getLogger();


    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    LogRpcService logRpcService;

    @Autowired
    VpcService vpcService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    public VpcTimerProcessor() {
    }

    @Override
    public void start() {
        log.info("vpc timer processor start");
    }

    @Override
    public void stop() {
        log.info("vpc timer processor stop");
    }

    @Override
    public void run() {
        try {
            List<Vpc> tblVpcs = getMiddleStatusVpcs();
            if (tblVpcs != null && tblVpcs.size() > 0)
            {
                for (Vpc tblVpc : tblVpcs)
                {
                    processVpc(tblVpc);
                }
            }
        } catch (Exception e) {
            log.error("vpc timer processor exception: {}", e.getMessage());
        }
    }

    private void processVpc(Vpc tblVpc) {
        int phaseStatus = tblVpc.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processAdding(tblVpc);
                    break;
                case PhaseStatus.AGENT_ADDING:
                    processCheckAgentAddingResult(tblVpc);
                    break;
                case PhaseStatus.DELETING:
                    processRemoving(tblVpc);
                    break;
                case PhaseStatus.AGENT_DELETING:
                    processCheckAgentDeletingResult(tblVpc);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("vpc timer processor error: vpcId {}, phase status {} , exception {}", tblVpc.getVpcId(), phaseStatus, e.getMessage());
        }
    }

    private List<Vpc> getMiddleStatusVpcs() {
        LambdaQueryWrapper<Vpc> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.ne(Vpc::getPhaseStatus, PhaseStatus.ADDED)
                .ne(Vpc::getPhaseStatus, REMOVED)
                .ne(Vpc::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(Vpc::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(Vpc::getPhaseStatus, PhaseStatus.AGENT_ADDING_ERR)
                .ne(Vpc::getPhaseStatus, PhaseStatus.AGENT_DELETING_ERR);
        return vpcService.list(queryWrapper);
    }

    private boolean checkMiddleStatus(int phaseStatus) {
        switch (phaseStatus) {
            case PhaseStatus.ADDED:
            case PhaseStatus.ADD_FAILED:
            case PhaseStatus.AGENT_ADDING_ERR:
            case PhaseStatus.AGENT_DELETING_ERR:
            case (short)REMOVED:
            case PhaseStatus.DELETE_FAILED:
                return false;
            case PhaseStatus.ADDING:
            case PhaseStatus.AGENT_ADDING:
            case PhaseStatus.AGENT_DELETING:
            case PhaseStatus.DELETING:
                return true;
        }
        return false;
    }

    private void processAdding(Vpc tblVpc)
    {
        if (StrUtil.isBlank(tblVpc.getCidr()))
        {
            log.error("create vpc failed, vpcId: {}, cidr is empty", tblVpc.getVpcId());
            return;
        }

        BaseRsp result = createVpc(tblVpc.getCidr());
        if (null == result)
        {
            log.error("create vpc failed, vpcId: {}, result is null", tblVpc.getVpcId());
            return;
        }
        if (AgentConstant.FAILED.equals(result.getStatus()))
        {
            log.error("create vpc failed, vpcId: {}", tblVpc.getVpcId());
            tblVpc.setPhaseStatus(PhaseStatus.ADD_FAILED);
            tblVpc.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblVpc.setVpcIdFromAgent(result.getUuid());
            boolean ok = vpcService.updateById(tblVpc);
            if (!ok) {
                log.error("current status is ADDING: update database error.");
            }
            else {
                logRpcService.getLogService().addEvent(tblVpc.getUserId(),"Agent 创建vpc异常",
                        String.format("请求参数 cidr:%s",tblVpc.getCidr()), "创建失败");
            }
        }
        else if (AgentConstant.PENDING.equals(result.getStatus()))
        {
            tblVpc.setPhaseStatus(PhaseStatus.AGENT_ADDING);
            tblVpc.setVpcIdFromAgent(result.getUuid());
            tblVpc.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vpcService.updateById(tblVpc);
            if (!ok) {
                log.error("current status is ADDING: update database error.");
            }
            else {
                logRpcService.getLogService().addEvent(tblVpc.getUserId(),"Agent 正在创建vpc",
                        String.format("请求参数 cidr:%s",tblVpc.getCidr()), "创建中");
            }
        }

    }

    private void processRemoving(Vpc tblVpc) {
        String status = removeVpc(tblVpc);
        if (AgentConstant.PENDING.equals(status) || AgentConstant.FAILED.equals(status))
        {
            tblVpc.setPhaseStatus(PhaseStatus.AGENT_DELETING);
        }
        tblVpc.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = vpcService.updateById(tblVpc);
        if (!ok) {
            log.error("current status is REMOVING: update database error.");
        }
        else
        {
            logRpcService.getLogService().addEvent(tblVpc.getUserId(),"Agent 正在删除vpc",
                    String.format("请求参数 vpcId:%s",tblVpc.getVpcId()), "删除中");
        }
    }

    private void processCheckAgentAddingResult(Vpc tblVpc) {
        String vpcId = tblVpc.getVpcIdFromAgent();
        VpcFromAgentRsp rsp = getVpc(tblVpc);
        if (rsp == null) {
            log.error("get vpc from agent failed, vpcId: {}", tblVpc.getVpcId());
            return;
        }
        if (Objects.equals(rsp.getPhaseStatus(), AgentConstant.SUCCESS) &&
                Objects.equals(rsp.getStatus(), AgentConstant.OK))
        {
            // 字符串转换成int
            int vlanId = Integer.parseInt(rsp.getNid());
            tblVpc.setVpcIdFromAgent(vpcId);
            tblVpc.setVlanId(vlanId);
            tblVpc.setPhaseStatus(PhaseStatus.ADDED);
        }
        else if (Objects.equals(rsp.getStatus(), AgentConstant.FAILED))
        {
            tblVpc.setPhaseStatus(PhaseStatus.AGENT_ADDING_ERR);
        }
        tblVpc.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vpcService.updateById(tblVpc);
        if (!ok)
        {
            log.error("current status is AGENT_ADDING: update database error.");
        }
        else
        {
            String logResult = String.format("创建中,networkId: %d, vpcId:%s",tblVpc.getVlanId(), tblVpc.getVpcId());
            if (PhaseStatus.AGENT_ADDING_ERR == tblVpc.getPhaseStatus())
            {
                logResult = "创建失败";
            }
            else if (PhaseStatus.ADDED == tblVpc.getPhaseStatus())
            {
                logResult = String.format("创建成功,networkId: %d, vpcId:%s",tblVpc.getVlanId(), tblVpc.getVpcId());
            }
            logRpcService.getLogService().addEvent(tblVpc.getUserId(),"Agent 获取vpc的状态",
                    String.format("获取vpc的状态,vpcId:%s",tblVpc.getVpcId()), logResult);
        }

    }

    // Check the result of the backend service processing the delete request
    private void processCheckAgentDeletingResult(Vpc tblVpc) {
        VpcFromAgentRsp rsp = getVpc(tblVpc);
        if (rsp == null) {
            log.error("get vpc from agent failed, vpcId: {}", tblVpc.getVpcId());
            return;
        }
        if (Objects.equals(rsp.getPhaseStatus(), AgentConstant.FAIL) &&
                Objects.equals(rsp.getStatus(), AgentConstant.OK)) {
            tblVpc.setPhaseStatus(PhaseStatus.AGENT_DELETING_ERR);
        } else if (Objects.equals(rsp.getStatus(), AgentConstant.FAILED) &&
                Objects.equals(rsp.getReason(), AgentConstant.VPC_NOT_EXIST)) {
            tblVpc.setPhaseStatus(REMOVED);
        }
        tblVpc.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//                int count = networkRepository.updateShare(vpc);
        boolean ok = vpcService.updateById(tblVpc);
        if (!ok) {
            log.error("current status is AGENT_ADDING: update database error.");
        } else {
            log.info("vpc:(vpc: {},vpcIdFromAgent: {}) is deleted successfully."
                    , tblVpc.getVpcId(), tblVpc.getVpcIdFromAgent());
            logRpcService.getLogService().addEvent(tblVpc.getUserId(),"Agent 删除vpc成功",
                    String.format("请求参数 vpcId:%s",tblVpc.getVpcId()), "删除成功");
        }
    }

    // create vpc from agent
    // return null if an exception occurred on the request backend
    private BaseRsp createVpc(String cidr) {
        String postUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getVpcUrl();
        BaseRsp result;
        SubnetCreateFromAgentReq createSubnet = new SubnetCreateFromAgentReq(cidr);
        String jsonString = JsonUtil.objectToJson(createSubnet);

        log.info("create vpc from agent");
        try {
            result = HttpActionUtil.post(postUrl, jsonString, BaseRsp.class);
        } catch (Exception e) {
            log.error("create vpc from agent error", e);
            return null;
        }
        return result;
    }

    // get vpc info from agent
    // return null if an exception occurred on the request backend
    private VpcFromAgentRsp getVpc(Vpc tblVpc) {
        String getUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getVpcUrl() + "/" + tblVpc.getVpcIdFromAgent();
        VpcFromAgentRsp result = null;
        try {
            result = HttpActionUtil.getObject(getUrl, VpcFromAgentRsp.class);
        } catch (Exception e) {
            log.error("get vpc from agent error, vpcId: {}", tblVpc.getVpcId(), e);
            return null;
        }
        log.info("get vpc from agent, result :{}", result);
        return result;
    }

    // delete vpc from agent
    // return null if an exception occurred on the request backend
    private String removeVpc(Vpc tblVpc) {
        String delUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getVpcUrl() + "/" + tblVpc.getVpcIdFromAgent();
        log.info("delete vpc from agent, vpcId: {}", tblVpc.getVpcId());
        BaseRsp baseRsp;
        try {
            //对Agent 执行http接口的删除操作
            baseRsp = HttpActionUtil.delete(delUrl, BaseRsp.class);
            if (baseRsp != null && StrUtil.isBlank(baseRsp.getReason())) {
                return baseRsp.getStatus();
            }
            if (baseRsp != null && StrUtil.isNotBlank(baseRsp.getReason()))  {
                if (baseRsp.getReason().contains(AgentConstant.NOT_FOUND) || baseRsp.getReason().contains(AgentConstant.NOT_READY))
                {
                    return AgentConstant.PENDING;
                }
                log.error("delete vpc from agent error, vpcId: {}, reason: {}", tblVpc.getVpcId(), baseRsp.getReason());
                return baseRsp.getReason();
            }
            if (baseRsp == null)
            {
                log.error("delete vpc from agent error, vpcId: {}, baseRsp is null", tblVpc.getVpcId());
                return null;
            }
        } catch (Exception e) {
            log.error("delete vpc from agent error, vpcId: {}", tblVpc.getVpcId());
            return null;
        }
        return baseRsp.getStatus();
    }
}
