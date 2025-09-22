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
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.request.BackendCreateFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.BackendRspFromAgent;
import com.lnjoying.justice.network.domain.backend.response.ResultFromAgentRsp;
import com.lnjoying.justice.network.entity.*;
import com.lnjoying.justice.network.service.*;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class BackendTimerProcessor extends AbstractRunnableProcessor
{

    @Autowired
    private BackendService backendService;

    @Autowired
    private SubnetService subnetService;

    @Autowired
    private VpcService vpcService;

    @Autowired
    private PortService portService;

    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    private LoadbalancerService loadbalancerService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    public BackendTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("backend timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("backend timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processBackends(getMiddleStatusBackends());
        }
        catch (Exception e)
        {
            log.error("backend timer processor exception: {}", e.getMessage());
        }
    }

    private List<Backend> getMiddleStatusBackends()
    {
        LambdaQueryWrapper<Backend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Backend::getPhaseStatus, PhaseStatus.ADDED )
                .ne(Backend::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(Backend::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(Backend::getPhaseStatus,REMOVED);
        return backendService.list(queryWrapper);
    }

    private void processBackends(List<Backend> tblBackends)
    {
        try
        {
            log.debug("get tblBackends :{}", tblBackends);
            for ( Backend tblBackend: tblBackends )
            {
                processBackend(tblBackend);
            }
        }
        catch (Exception e)
        {
            log.error("backends timer processor error:  {}", e.getMessage());
        }
    }


    private void processBackend(Backend tblBackend)
    {
        int phaseStatus = tblBackend.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateBackend(tblBackend);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveBackend(tblBackend);
                    break;
                default:
                    defaultProcessBackend(tblBackend);
                    break;
            }
        }
        catch (Exception e)
        {
            log.error("Backend timer processor error: BackendId {}, phase status {} , exception {}", tblBackend.getBackendId(), phaseStatus, e.getMessage());
        }
    }

    // 创建后端服务组process
    private void processCreateBackend(Backend tblBackend)
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Port::getInstanceId, tblBackend.getBackendId())
                .ne(Port::getPhaseStatus, REMOVED);
        Port tblPort = portService.getOne(queryWrapper);
        if (tblPort == null)
        {
            log.error("Backend timer processor error: BackendId {}, port not found", tblBackend.getBackendId());
            return;
        }
        if (PhaseStatus.ADDED != tblPort.getPhaseStatus())
        {
            return;
        }
        createBackend(tblBackend, tblPort);
        log.info("process create Backend: {}", tblBackend);
    }

    //删除后端服务组process
    private void processRemoveBackend(Backend tblBackend)
    {
        log.info("process remove Backend: {}", tblBackend);
        try
        {
            String backendId = removeBackendFromAgent(tblBackend);
            if (null == backendId)
            {
                log.info("removeBackendFromAgent error, lbId:{}", tblBackend.getBackendId());
            }
            tblBackend.setPhaseStatus(PhaseStatus.AGENT_DELETING);
            tblBackend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = backendService.updateById(tblBackend);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (WebSystemException e)
        {
            log.error("remove Backend error: BackendId {}, {}", tblBackend.getBackendId(), e.getMessage());
        }
    }

    private void defaultProcessBackend(Backend tblBackend)
    {
        try
        {
            BackendRspFromAgent BackendRspFromAgent = getBackendStatusFromAgent(tblBackend);

            switch (Objects.requireNonNull(Objects.requireNonNull(BackendRspFromAgent).getPhase()))
            {
                case AgentConstant.ADDED:
                    tblBackend.setPhaseStatus(PhaseStatus.ADDED);
                    break;
                case AgentConstant.BACKEND_NOT_EXIST:
                    tblBackend.setPhaseStatus(REMOVED);
                    break;
                case AgentConstant.ADD_FAILED:
                    tblBackend.setPhaseStatus(PhaseStatus.ADD_FAILED);
                    break;
                case AgentConstant.DELETE_FAILED:
                    tblBackend.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                    break;
                default:
                    return;
            }
            tblBackend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = backendService.updateById(tblBackend);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("getBackendCreateStatus error:{}, BackendId:{}, ", e.getMessage(), tblBackend.getBackendId());
        }
    }

    // 创建后端服务组
    private void createBackend(Backend tblBackend, Port tblPort)
    {
        try
        {
            Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
            Vpc tblVpc = vpcService.getById(tblSubnet.getVpcId());
            log.info("create Backend: {}", tblBackend);
            BackendCreateFromAgentReq req = new BackendCreateFromAgentReq();
           
            req.setBalance(tblBackend.getBalance());
            req.setMode(tblBackend.getProtocol());
            List<String> serverIps = new ArrayList<>();
            List<String> serverPorts = new ArrayList<>();
            List<String> ips = new ArrayList<>();
            List<String> vlanIds = new ArrayList<>();
            for (String backendServer : StrUtil.split(tblBackend.getBackendServer(),","))
            {
                if (backendServer.contains(":"))
                {
                    serverIps.add(StrUtil.subBefore(backendServer,":",false));
                    serverPorts.add(StrUtil.subAfter(backendServer,":",false));
                    ips.add(tblPort.getIpAddress());
                    vlanIds.add(tblVpc.getVlanId().toString());
                }

            }
            req.setServerHostname(serverIps);
            req.setServerPort(serverPorts);
            req.setVlanId(vlanIds); 
            req.setIp(ips);
            String backendIdFromAgent = createBackendFromAgent(tblBackend, req);
            if (null == backendIdFromAgent) return;
            tblBackend.setBackendIdFromAgent(backendIdFromAgent);
            tblBackend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = backendService.updateById(tblBackend);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("create Backend from agent error: {}", e.getMessage());

        }
    }


    // agent 创建后端服务组
    private String createBackendFromAgent(Backend tblBackend,BackendCreateFromAgentReq req)
    {
        Loadbalancer loadbalancer = loadbalancerService.getById(tblBackend.getLbId());
        if (StrUtil.isBlank(loadbalancer.getLbIdFromAgent())) return null;
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.HAPROXY_URL + "/" + loadbalancer.getLbIdFromAgent()+"/backends" ;
        try
        {
            ResponseEntity<ResultFromAgentRsp> result = HttpActionUtil.postForEntity(url, req, ResultFromAgentRsp.class);
            ResultFromAgentRsp resultFromAgentRsp = result.getBody();
            if (null == resultFromAgentRsp)
            {
                log.error("get response of creating Backend  error,  BackendId:{}", tblBackend.getBackendIdFromAgent());
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
                log.error("BackendId is null, BackendRspFromAgent: {}", resultFromAgentRsp);
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            return resultFromAgentRsp.getUuid();
        }
        catch (Exception e)
        {
            log.error("create Backend from agent error: {}", e.getMessage());
            return null;
        }
    }

    // agent 删除后端服务组
    private String removeBackendFromAgent(Backend tblBackend)
    {
        log.info("remove Backend from agent: {}", tblBackend);
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.BACKEND_URL + "/" + tblBackend.getBackendIdFromAgent();
        String result = HttpActionUtil.delete(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.error("get response of removing Backend error,  lbId:{}", tblBackend.getBackendIdFromAgent());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        String status = (String) resultMap.get("status");
        if (AgentConstant.PENDING.equals(status) ||
                (AgentConstant.FAILED.equals(status) && result.contains(AgentConstant.NOT_EXIST)))
        {
            return tblBackend.getBackendId();
        }

        return null;
    }

    //agent 获取后端服务组状态
    private BackendRspFromAgent getBackendStatusFromAgent(Backend tblBackend)
    {
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.BACKEND_URL + "/" + tblBackend.getBackendIdFromAgent();
        try
        {
            return HttpActionUtil.getObject(url, BackendRspFromAgent.class);
        }
        catch (Exception e)
        {
            log.error("get Backend status from agent error: {}", e.getMessage());
            return null;
        }
    }

}
