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
import com.lnjoying.justice.network.domain.backend.request.FrontendCreateFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.FrontendRspFromAgent;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class FrontendTimerProcessor extends AbstractRunnableProcessor
{

    @Autowired
    private FrontendService frontendService;

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

    public FrontendTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("frontend timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("frontend timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processFrontends(getMiddleStatusFrontends());
        }
        catch (Exception e)
        {
            log.error("frontend timer processor exception: {}", e.getMessage());
        }
    }

    private List<Frontend> getMiddleStatusFrontends()
    {
        LambdaQueryWrapper<Frontend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Frontend::getPhaseStatus, PhaseStatus.ADDED )
                .ne(Frontend::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(Frontend::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(Frontend::getPhaseStatus,REMOVED);
        return frontendService.list(queryWrapper);
    }

    private void processFrontends(List<Frontend> tblFrontends)
    {
        try
        {
            log.debug("get tblFrontends :{}", tblFrontends);
            for ( Frontend tblFrontend: tblFrontends )
            {
                processFrontend(tblFrontend);
            }
        }
        catch (Exception e)
        {
            log.error("frontends timer processor error:  {}", e.getMessage());
        }
    }


    private void processFrontend(Frontend tblFrontend)
    {
        int phaseStatus = tblFrontend.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateFrontend(tblFrontend);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveFrontend(tblFrontend);
                    break;
                default:
                    defaultProcessFrontend(tblFrontend);
                    break;
            }
        }
        catch (Exception e)
        {
            log.error("Frontend timer processor error: FrontendId {}, phase status {} , exception {}", tblFrontend.getFrontendId(), phaseStatus, e.getMessage());
        }
    }

    // 创建后端服务组process
    private void processCreateFrontend(Frontend tblFrontend)
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Port::getInstanceId, tblFrontend.getFrontendId())
                .ne(Port::getPhaseStatus, REMOVED);
        Port tblPort = portService.getOne(queryWrapper);
        if (tblPort == null)
        {
            log.error("Frontend timer processor error: FrontendId {}, port not found", tblFrontend.getFrontendId());
            return;
        }
        if (PhaseStatus.ADDED != tblPort.getPhaseStatus())
        {
            return;
        }
        createFrontend(tblFrontend, tblPort);
        log.info("process create Frontend: {}", tblFrontend);
    }

    //删除后端服务组process
    private void processRemoveFrontend(Frontend tblFrontend)
    {
        log.info("process remove Frontend: {}", tblFrontend);
        try
        {
            String frontendId = removeFrontendFromAgent(tblFrontend);
            if (null == frontendId)
            {
                log.info("removeFrontendFromAgent error, lbId:{}", tblFrontend.getFrontendId());
            }
            tblFrontend.setPhaseStatus(PhaseStatus.AGENT_DELETING);
            tblFrontend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = frontendService.updateById(tblFrontend);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (WebSystemException e)
        {
            log.error("remove Frontend error: FrontendId {}, {}", tblFrontend.getFrontendId(), e.getMessage());
        }
    }

    private void defaultProcessFrontend(Frontend tblFrontend)
    {
        try
        {
            FrontendRspFromAgent FrontendRspFromAgent = getFrontendStatusFromAgent(tblFrontend);

            switch (Objects.requireNonNull(Objects.requireNonNull(FrontendRspFromAgent).getPhase()))
            {
                case AgentConstant.ADDED:
                    tblFrontend.setPhaseStatus(PhaseStatus.ADDED);
                    break;
                case AgentConstant.BACKEND_NOT_EXIST:
                    tblFrontend.setPhaseStatus(REMOVED);
                    break;
                case AgentConstant.ADD_FAILED:
                    tblFrontend.setPhaseStatus(PhaseStatus.ADD_FAILED);
                    break;
                case AgentConstant.DELETE_FAILED:
                    tblFrontend.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                    break;
                default:
                    return;
            }
            tblFrontend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = frontendService.updateById(tblFrontend);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("getFrontendCreateStatus error:{}, FrontendId:{}, ", e.getMessage(), tblFrontend.getFrontendId());
        }
    }

    // 创建后端服务组
    private void createFrontend(Frontend tblFrontend, Port tblPort)
    {
        try
        {
            Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
            Vpc tblVpc = vpcService.getById(tblSubnet.getVpcId());
            log.info("create Frontend: {}", tblFrontend);
            FrontendCreateFromAgentReq req = new FrontendCreateFromAgentReq();

            req.setBackend(tblFrontend.getBackendId());
            req.setEnabled(true);
            req.setBindPort(new ArrayList<>(Collections.singletonList(tblFrontend.getListenPort().toString())));
            req.setIp(new ArrayList<>(Collections.singletonList(tblPort.getIpAddress())));
            req.setVlanId(new ArrayList<>(Collections.singletonList(tblVpc.getVlanId().toString())));

            String frontendIdFromAgent = createFrontendFromAgent(tblFrontend, req);
            if (null == frontendIdFromAgent) return;
            tblFrontend.setFrontendIdFromAgent(frontendIdFromAgent);
            tblFrontend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = frontendService.updateById(tblFrontend);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("create Frontend from agent error: {}", e.getMessage());

        }
    }


    // agent 创建后端服务组
    private String createFrontendFromAgent(Frontend tblFrontend,FrontendCreateFromAgentReq req)
    {
        Loadbalancer loadbalancer = loadbalancerService.getById(tblFrontend.getLbId());
        if (StrUtil.isBlank(loadbalancer.getLbIdFromAgent())) return null;
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.HAPROXY_URL + "/" + loadbalancer.getLbIdFromAgent()+"/frontends" ;
        try
        {
            ResponseEntity<ResultFromAgentRsp> result = HttpActionUtil.postForEntity(url, req, ResultFromAgentRsp.class);
            ResultFromAgentRsp resultFromAgentRsp = result.getBody();
            if (null == resultFromAgentRsp)
            {
                log.error("get response of creating Frontend  error,  FrontendId:{}", tblFrontend.getFrontendIdFromAgent());
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
                log.error("FrontendId is null, FrontendRspFromAgent: {}", resultFromAgentRsp);
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            return resultFromAgentRsp.getUuid();
        }
        catch (Exception e)
        {
            log.error("create Frontend from agent error: {}", e.getMessage());
            return null;
        }
    }

    // agent 删除后端服务组
    private String removeFrontendFromAgent(Frontend tblFrontend)
    {
        log.info("remove Frontend from agent: {}", tblFrontend);
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.BACKEND_URL + "/" + tblFrontend.getFrontendIdFromAgent();
        String result = HttpActionUtil.delete(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.error("get response of removing Frontend error,  lbId:{}", tblFrontend.getFrontendIdFromAgent());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        String status = (String) resultMap.get("status");
        if (AgentConstant.PENDING.equals(status) ||
                (AgentConstant.FAILED.equals(status) && result.contains(AgentConstant.NOT_EXIST)))
        {
            return tblFrontend.getFrontendId();
        }

        return null;
    }

    //agent 获取后端服务组状态
    private FrontendRspFromAgent getFrontendStatusFromAgent(Frontend tblFrontend)
    {
        String url = combRpcSerice.getVmService().getL3IpPort() + AgentConstant.BACKEND_URL + "/" + tblFrontend.getFrontendIdFromAgent();
        try
        {
            return HttpActionUtil.getObject(url, FrontendRspFromAgent.class);
        }
        catch (Exception e)
        {
            log.error("get Frontend status from agent error: {}", e.getMessage());
            return null;
        }
    }
}
