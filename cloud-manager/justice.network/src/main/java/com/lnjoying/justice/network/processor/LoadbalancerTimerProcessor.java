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
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.common.PortType;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.request.HaproxyCreateFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.LoadbalancerRspFromAgent;
import com.lnjoying.justice.network.domain.backend.response.ResultFromAgentRsp;
import com.lnjoying.justice.network.entity.Loadbalancer;
import com.lnjoying.justice.network.entity.Port;
import com.lnjoying.justice.network.entity.Subnet;
import com.lnjoying.justice.network.entity.Vpc;
import com.lnjoying.justice.network.service.LoadbalancerService;
import com.lnjoying.justice.network.service.PortService;
import com.lnjoying.justice.network.service.SubnetService;
import com.lnjoying.justice.network.service.VpcService;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class LoadbalancerTimerProcessor extends AbstractRunnableProcessor
{
    @Autowired
    private LoadbalancerService loadbalancerService;

    @Autowired
    private SubnetService subnetService;

    @Autowired
    private VpcService vpcService;

    @Autowired
    private PortService portService;

    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    private CombRpcSerice combRpcSerice;

    public LoadbalancerTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("loadbalancer timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("loadbalancer timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processLoadbalancers(getMiddleStatusLoadbalancers());
        }
        catch (Exception e)
        {
            log.error("loadbalancer timer processor exception: {}", e.getMessage());
        }
    }

    private List<Loadbalancer> getMiddleStatusLoadbalancers()
    {
        LambdaQueryWrapper<Loadbalancer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Loadbalancer::getPhaseStatus, PhaseStatus.ADDED )
                .ne(Loadbalancer::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(Loadbalancer::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(Loadbalancer::getPhaseStatus,REMOVED);
        return loadbalancerService.list(queryWrapper);
    }

    private void processLoadbalancers(List<Loadbalancer> tblLoadbalancers)
    {
        try
        {
            log.debug("get loadbalancers :{}", tblLoadbalancers);
            for ( Loadbalancer tblLoadbalancer: tblLoadbalancers )
            {
                processLoadbalancer(tblLoadbalancer);
            }
        }
        catch (Exception e)
        {
            log.error("loadbalancer timer processor error:  {}", e.getMessage());
        }
    }
    
    private void processLoadbalancer(Loadbalancer tblLoadbalancer)
    {
        int phaseStatus = tblLoadbalancer.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreatePort(tblLoadbalancer);
                    break;
                case PhaseStatus.PORT_CREATING:
                    processCreateLoadbalancer(tblLoadbalancer);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveLoadbalancer(tblLoadbalancer);
                    break;
                default:
                    defaultProcessLoadbalancer(tblLoadbalancer);
                    break;
            }
        }
        catch (Exception e)
        {
            log.error("loadbalancer timer processor error: loadbalancerId {}, phase status {} , exception {}", tblLoadbalancer.getLbId(), phaseStatus, e.getMessage());
        }
    }

    // 创建负载均衡器process
    private void processCreateLoadbalancer(Loadbalancer tblLoadbalancer)
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Port::getInstanceId, tblLoadbalancer.getLbId())
                .ne(Port::getPhaseStatus, REMOVED);
        Port tblPort = portService.getOne(queryWrapper);
        if (tblPort == null)
        {
            log.error("loadbalancer timer processor error: loadbalancerId {}, port not found", tblLoadbalancer.getLbId());
            return;
        }
        if (PhaseStatus.ADDED != tblPort.getPhaseStatus())
        {
            return;
        }
        createLoadbalancer(tblLoadbalancer, tblPort);
        log.info("process create loadbalancer: {}", tblLoadbalancer);
    }

    // 创建port process
    private void processCreatePort(Loadbalancer tblLoadbalancer)
    {
        log.info("process create port: {}", tblLoadbalancer);
        Port tblPort = new Port();
        tblPort.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblPort.setUpdateTime(tblPort.getCreateTime());
        tblPort.setPhaseStatus(PhaseStatus.ADDING);
        tblPort.setSubnetId(tblLoadbalancer.getSubnetId());
        tblPort.setInstanceId(tblLoadbalancer.getLbId());
//                .vmInstanceId(vmInstanceId)
        tblPort.setType(PortType.lb);
        boolean ok = portService.save(tblPort);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        tblLoadbalancer.setPhaseStatus(PhaseStatus.PORT_CREATING);
        ok = loadbalancerService.updateById(tblLoadbalancer);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
    }

    //删除负载均衡器process
    private void processRemoveLoadbalancer(Loadbalancer tblLoadbalancer)
    {
        log.info("process remove loadbalancer: {}", tblLoadbalancer);
        try
        {
            String lbId = removeLoadbalancerFromAgent(tblLoadbalancer);
            if (null == lbId)
            {
                log.info("removeLoadbalancerFromAgent error, lbId:{}", tblLoadbalancer.getLbId());
            }
            tblLoadbalancer.setPhaseStatus(PhaseStatus.AGENT_DELETING);
            tblLoadbalancer.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = loadbalancerService.updateById(tblLoadbalancer);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (WebSystemException e)
        {
            log.error("remove loadbalancer error: loadbalancerId {}, {}", tblLoadbalancer.getLbId(), e.getMessage());
        }
    }

    private void defaultProcessLoadbalancer(Loadbalancer tblLoadbalancer)
    {
        try
        {
            LoadbalancerRspFromAgent loadbalancerRspFromAgent = getLoadbalancerStatusFromAgent(tblLoadbalancer);

            switch (Objects.requireNonNull(Objects.requireNonNull(loadbalancerRspFromAgent).getPhase()))
            {
                case AgentConstant.ADDED:
                    tblLoadbalancer.setPhaseStatus(PhaseStatus.ADDED);
                    break;
                case AgentConstant.HAPROXY_NOT_EXIST:
                    tblLoadbalancer.setPhaseStatus(REMOVED);
                    break;
                case AgentConstant.ADD_FAILED:
                    tblLoadbalancer.setPhaseStatus(PhaseStatus.ADD_FAILED);
                    break;
                case AgentConstant.DELETE_FAILED:
                    tblLoadbalancer.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                    break;
                default:
                    return;
            }
            tblLoadbalancer.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = loadbalancerService.updateById(tblLoadbalancer);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("getLoadbalancerCreateStatus error:{}, loadbalancerId:{}, ", e.getMessage(), tblLoadbalancer.getLbId());
        }
    }

    // 创建负载均衡器
    private void createLoadbalancer(Loadbalancer tblLoadbalancer, Port tblPort)
    {
        try
        {
            Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
            Vpc tblVpc = vpcService.getById(tblSubnet.getVpcId());
            log.info("create loadbalancer: {}", tblLoadbalancer);
            HaproxyCreateFromAgentReq req = new HaproxyCreateFromAgentReq();
            req.setIp(new ArrayList<>(Collections.singletonList(tblPort.getIpAddress())));
            req.setMac(new ArrayList<>(Collections.singletonList(tblPort.getMacAddress())));
            req.setOfport(new ArrayList<>(Collections.singletonList(tblPort.getOfPort().toString())));
            req.setSubnetCidr(new ArrayList<>(Collections.singletonList(tblSubnet.getCidr())));
            req.setVpcCidr(new ArrayList<>(Collections.singletonList(tblVpc.getCidr())));
            req.setVlanId(new ArrayList<>(Collections.singletonList(tblVpc.getVlanId().toString())));
            String lbIdFromAgent = createLoadbalancerFromAgent(tblLoadbalancer, req);
            if (null == lbIdFromAgent) return;
            tblLoadbalancer.setLbIdFromAgent(lbIdFromAgent);
            tblLoadbalancer.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = loadbalancerService.updateById(tblLoadbalancer);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("create loadbalancer from agent error: {}", e.getMessage());

        }
    }


    // agent 创建负载均衡器
    private String createLoadbalancerFromAgent(Loadbalancer tblLoadbalancer,HaproxyCreateFromAgentReq req)
    {
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.HAPROXY_URL;
        try
        {
            ResponseEntity<ResultFromAgentRsp> result = HttpActionUtil.postForEntity(url, req, ResultFromAgentRsp.class);
            ResultFromAgentRsp resultFromAgentRsp = result.getBody();
            if (null == resultFromAgentRsp)
            {
                log.error("get response of creating loadbalancer  error,  loadbalancerId:{}", tblLoadbalancer.getLbIdFromAgent());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            if (!StrUtil.isBlank(resultFromAgentRsp.getReason()))
            {
                String pattern = "(.*already added.*):\\s*(.*)\\s*";
                Pattern r = Pattern.compile(pattern);
                Matcher matcher = r.matcher(resultFromAgentRsp.getReason());
                if (matcher.find())
                {
                    return matcher.group(2);
                }
            }
            else if (StrUtil.isBlank(resultFromAgentRsp.getUuid()))
            {
                log.error("loadbalancerId is null, loadbalancerRspFromAgent: {}", resultFromAgentRsp);
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            return resultFromAgentRsp.getUuid();
        }
        catch (Exception e)
        {
            log.error("create loadbalancer from agent error: {}", e.getMessage());
            return null;
        }
    }
    
    // agent 删除负载均衡器
    private String removeLoadbalancerFromAgent(Loadbalancer tblLoadbalancer)
    {
        log.info("remove loadbalancer from agent: {}", tblLoadbalancer);
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.HAPROXY_URL + "/" + tblLoadbalancer.getLbIdFromAgent();
        String result = HttpActionUtil.delete(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.error("get response of removing loadbalancer error,  lbId:{}", tblLoadbalancer.getLbIdFromAgent());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        String status = (String) resultMap.get("status");
        if (AgentConstant.PENDING.equals(status) ||
                (AgentConstant.FAILED.equals(status) && result.contains(AgentConstant.NOT_EXIST)))
        {
            return tblLoadbalancer.getLbId();
        }
        
        return null;
    }
    
    //agent 获取负载均衡器状态
    private LoadbalancerRspFromAgent getLoadbalancerStatusFromAgent(Loadbalancer tblLoadbalancer)
    {
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.HAPROXY_URL + "/" + tblLoadbalancer.getLbIdFromAgent();
        try
        {
            return HttpActionUtil.getObject(url, LoadbalancerRspFromAgent.class);
        }
        catch (Exception e)
        {
            log.error("get loadbalancer status from agent error: {}", e.getMessage());
            return null;
        }
    }

}
