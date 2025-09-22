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
import com.lnjoying.justice.schema.service.compute.ComputeService;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.common.PortType;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.request.IpInfo;
import com.lnjoying.justice.network.domain.backend.request.PortCreateFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.BaseRsp;
import com.lnjoying.justice.network.domain.backend.response.EipPortMapInfo;
import com.lnjoying.justice.network.domain.backend.response.PortFromAgentRsp;
import com.lnjoying.justice.network.entity.Eip;
import com.lnjoying.justice.network.entity.Port;
import com.lnjoying.justice.network.entity.Subnet;
import com.lnjoying.justice.network.service.EipService;
import com.lnjoying.justice.network.service.PortService;
import com.lnjoying.justice.network.service.SubnetService;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.lnjoying.justice.network.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import com.micro.core.utils.UnderlineAndHump;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class PortTimerProcessor extends AbstractRunnableProcessor
{
//    private static final Logger log = LogManager.getLogger();

    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    PortService portService;

    @Autowired
    SubnetService subnetService;

    @Autowired
    EipService eipService;

    @Autowired
    EipPortMapTimerProcessor eipPortMapTimerProcessor;

    @Autowired
    LogRpcService logRpcService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    public PortTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("port timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("port timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processPorts(getMiddleStatusPorts());
            processEipStatusPorts(getMiddleEipStatusPorts());
        }
        catch (Exception e)
        {
            log.error("port timer processor exception: {}", e.getMessage());
        }
    }

    // 处理端口的中间状态
    private List<Port> getMiddleStatusPorts()
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Port::getPhaseStatus, PhaseStatus.ADDING, PhaseStatus.DELETING, PhaseStatus.ARPING,
                PhaseStatus.AGENT_ADDING, PhaseStatus.AGENT_DELETING, PhaseStatus.ARPING_DONE);
        return portService.list(queryWrapper);
    }

    // 处理EIP绑定的中间状态
    private List<Port> getMiddleEipStatusPorts()
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Port::getEipPhaseStatus, PhaseStatus.ATTACH_EIP_INIT, PhaseStatus.ATTACH_EIP_ING,
                PhaseStatus.DETACH_EIP_INIT, PhaseStatus.DETACH_EIP_ING);
        return portService.list(queryWrapper);
    }

    private void processPorts(List<Port> tblPorts)
    {
        if ( null == tblPorts || tblPorts.isEmpty())
        {
            return;
        }
        for (Port tblPort : tblPorts)
        {
            try
            {
                processPort(tblPort);
            }
            catch (Exception e)
            {
                log.error("process port exception: {}", e.getMessage());
            }
        }
    }

    private void processEipStatusPorts(List<Port> tblPorts)
    {
        if ( null == tblPorts || tblPorts.isEmpty())
        {
            return;
        }
        for (Port tblPort : tblPorts)
        {
            try
            {
                processEipStatusPort(tblPort);
            }
            catch (Exception e)
            {
                log.error("process port exception: {}", e.getMessage());
            }
        }
    }

    private void processEipStatusPort(Port tblPort)
    {
        int phaseStatus = tblPort.getEipPhaseStatus();
        switch (phaseStatus)
        {
            case PhaseStatus.ATTACH_EIP_INIT:
                processAttachEipInit(tblPort);
                break;
            case PhaseStatus.ATTACH_EIP_ING:
                processAttachEipIng(tblPort);
                break;
            case PhaseStatus.DETACH_EIP_INIT:
                processDetachEipInit(tblPort);
                break;
            case PhaseStatus.DETACH_EIP_ING:
                processDetachEipIng(tblPort);
                break;
            default:
                break;
        }
    }

    private void processDetachEipIng(Port tblPort)
    {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        Eip tblEip = eipService.getById(tblPort.getEipId());
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            log.error("processDetachEipIng eip is null, portId: {}", tblPort.getPortId());
            dataSourceTransactionManager.commit(transactionStatus);
            return;
        }

        PortFromAgentRsp result = eipPortMapTimerProcessor.getEipPortMapFromAgent(tblPort.getPortIdFromAgent());
        if (null == result)
        {
            log.error("processAttachEipIng getEipPortMapFromAgent failed, portId: {}", tblPort.getPortId());
            dataSourceTransactionManager.commit(transactionStatus);
            return;
        }

        if ((Objects.equals(result.getPhaseStatus(), AgentConstant.SUCCESS) && Objects.equals(result.getPhaseType(),"unbind"))
            || (Objects.equals(result.getPhaseStatus(), AgentConstant.FAILED) && result.getReason().contains(AgentConstant.NOT_FOUND)))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.DETACH_EIP_DONE);
            tblPort.setEipId(null);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processDetachEipIng update tblPort failed, portId: {}", tblPort.getPortId());
                dataSourceTransactionManager.rollback(transactionStatus);
            }
            if (dbOk)
            {
                String userId = tblEip.getUserId();
                tblEip.setBoundId(null);
                tblEip.setBoundType(null);
                tblEip.setUserId(null);
                tblEip.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                dbOk = eipService.updateById(tblEip);
                if (!dbOk)
                {
                    log.error("processDetachEipIng update tblEip failed, portId: {}", tblPort.getPortId());
                    dataSourceTransactionManager.rollback(transactionStatus);
                }
                else
                {
                    String rpcResult = combRpcSerice.getVmService().setVmInstanceEip("",tblPort.getInstanceId());
                    if (!Objects.equals(rpcResult, tblPort.getInstanceId()))
                    {
                        log.error("processDetachEipInit setVmInstanceEip failed, portId: {}", tblPort.getPortId());
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return;
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    logRpcService.getLogService().addEvent(userId,
                            "Agent 解绑Eip成功", String.format("请求参数: EIP:%s", tblEip.getIpaddr()), "解绑成功");
                }
            }
        }
        else if (Objects.equals(result.getPhaseStatus(), AgentConstant.FAIL) && Objects.equals(result.getPhaseType(),"unbind"))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.ATTACH_EIP_ERR);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processDetachEipIng update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 解绑Eip失败",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"解绑失败");
            }
            dataSourceTransactionManager.commit(transactionStatus);
        }
        else
        {
            log.info("processDetachEipIng result: {}", result);
            dataSourceTransactionManager.commit(transactionStatus);
        }

    }

    private void processAttachEipInit(Port tblPort)
    {
        Eip tblEip = eipService.getById(tblPort.getEipId());
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            log.error("processAttachEipInit eip is null, portId: {}", tblPort.getPortId());
            return;
        }
        String rpcResult = combRpcSerice.getVmService().setVmInstanceEip(tblPort.getEipId(), tblPort.getInstanceId());
        if (!Objects.equals(tblPort.getInstanceId(), rpcResult))
        {
            log.error("processAttachEipInit setVmInstanceEip failed, portId: {}", tblPort.getPortId());
            return;
        }
        EipPortMapInfo.PutEipPortMapReq portMapReq = EipPortMapInfo.PutEipPortMapReq.builder()
                .eip(tblEip.getIpaddr())
                .mapping("ip,")
                .build();

        BaseRsp result = eipPortMapTimerProcessor.createEipPortMapFromAgent(tblPort.getPortIdFromAgent(), portMapReq);
        if (null == result)
        {
            log.error("processAttachEipInit createEipPortMapFromAgent failed, portId: {}", tblPort.getPortId());
            return;
        }

        if (Objects.equals(result.getStatus(), AgentConstant.PENDING))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.ATTACH_EIP_ING);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processAttachEipInit update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 正在创建绑定Eip",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"绑定中");
            }
        }
        else if(Objects.equals(result.getStatus(), AgentConstant.FAILED))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.ATTACH_EIP_ERR);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processAttachEipInit update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 创建绑定Eip失败",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"绑定失败");
            }
        }

    }

    private void processAttachEipIng(Port tblPort)
    {
        Eip tblEip = eipService.getById(tblPort.getEipId());
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            log.error("processAttachEipIng eip is null, portId: {}", tblPort.getPortId());
            return;
        }
        String nodeIp = combRpcSerice.getVmService().getHypervisorNodeIp(tblPort.getInstanceId());
        PortFromAgentRsp result = eipPortMapTimerProcessor.getEipPortMapFromAgent( tblPort.getPortIdFromAgent());
        if (null == result)
        {
            log.error("processAttachEipIng getEipPortMapFromAgent failed, portId: {}", tblPort.getPortId());
            return;
        }

        if (Objects.equals(result.getPhaseStatus(), AgentConstant.SUCCESS) && Objects.equals(result.getPhaseType(),"bind"))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.ATTACH_EIP_DONE);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processAttachEipIng update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 创建绑定Eip成功",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"绑定成功");
            }
        }
        else if (Objects.equals(result.getPhaseStatus(), AgentConstant.FAIL) && Objects.equals(result.getPhaseType(),"bind"))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.ATTACH_EIP_ERR);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processAttachEipIng update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 创建绑定Eip失败",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"绑定失败");
            }
        }

    }

    private void processDetachEipInit(Port tblPort)
    {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        Eip tblEip = eipService.getById(tblPort.getEipId());
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            log.error("processDetachEipInit eip is null, portId: {}", tblPort.getPortId());
            return;
        }
//        EipPortMapInfo.PutEipPortMapReq putEipPortMapReq = EipPortMapInfo.PutEipPortMapReq.builder().build();
        BaseRsp result = eipPortMapTimerProcessor.removeEipPortMapFromAgent( tblPort.getPortIdFromAgent(), null);
        if (null == result)
        {
            log.error("processDetachEipInit deleteEipPortMapFromAgent failed, portId: {}", tblPort.getPortId());
            return;
        }

        if ( Objects.equals(result.getStatus(), AgentConstant.PENDING))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.DETACH_EIP_ING);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processDetachEipInit update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 正在解绑Eip",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"解绑中");
            }
            log.info("processDetachEipInit update tblPort success, portId: {}, eipPhaseStatus:{}", tblPort.getPortId(), tblPort.getEipPhaseStatus());
            dataSourceTransactionManager.commit(transactionStatus);
        }
        else if (Objects.equals(result.getStatus(), AgentConstant.FAILED) &&
                result.getReason().toString().contains("not bound"))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.DETACH_EIP_DONE);
            tblPort.setEipId(null);
            tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processDetachEipInit update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                String userId = tblEip.getUserId();
                tblEip.setBoundId(null);
                tblEip.setBoundType(null);
                tblEip.setUserId(null);
                tblEip.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                dbOk = eipService.updateById(tblEip);
                if (dbOk)
                {
                    String rpcResult = combRpcSerice.getVmService().setVmInstanceEip("",tblPort.getInstanceId());
                    log.info("processDetachEipInit setVmInstanceEip, portId: {}, rpcResult: {}", tblPort.getPortId(), rpcResult);
                    if (!Objects.equals(rpcResult, tblPort.getInstanceId()))
                    {
                        log.error("processDetachEipInit setVmInstanceEip failed, portId: {}", tblPort.getPortId());
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return;
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    logRpcService.getLogService().addEvent(userId,
                            "Agent 解绑Eip成功",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"解绑成功");
                }
                else
                {
                    dataSourceTransactionManager.rollback(transactionStatus);
                }
            }
        }
        else if (Objects.equals(result.getStatus(), AgentConstant.FAILED))
        {
            tblPort.setEipPhaseStatus(PhaseStatus.DETACH_EIP_ERR);
            boolean dbOk = portService.updateById(tblPort);
            if (!dbOk)
            {
                log.error("processDetachEipInit update tblPort failed, portId: {}", tblPort.getPortId());
            }
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblEip.getUserId(),
                        "Agent 解绑Eip失败",String.format("请求参数: EIP:%s", tblEip.getIpaddr()),"解绑失败");
            }
            dataSourceTransactionManager.commit(transactionStatus);
        }
    }


    private void processPort(Port tblPort)
    {
        int phaseStatus = tblPort.getPhaseStatus();
        switch (phaseStatus)
        {
            case PhaseStatus.ADDING:
                processAdding(tblPort);
                break;
            case PhaseStatus.DELETING:
                processRemoving(tblPort);
                break;
//            case PhaseStatus.ARPING:
//                processArping(tblPort);
//                break;
            case PhaseStatus.AGENT_ADDING:
                processCheckAgentAddingResult(tblPort);
                break;
            case PhaseStatus.AGENT_DELETING:
                processCheckAgentDeletingResult(tblPort);
                break;
            default:
                break;
        }
    }


    private List<Port> getMiddleStatusPorts(int phaseStatus)
    {
        if (checkMiddleStatus(phaseStatus))
        {
            LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Port::getPhaseStatus, phaseStatus);
            if (portService.count(queryWrapper) > 0)
            {
                return portService.list(queryWrapper);
            }

        }
        return null;
    }

    private boolean checkMiddleStatus(int phaseStatus)
    {
        switch (phaseStatus)
        {
            case PhaseStatus.ADDED:
            case PhaseStatus.ADD_FAILED:
            case PhaseStatus.AGENT_ADDING_ERR:
            case PhaseStatus.AGENT_DELETING_ERR:
            case (short) REMOVED:
            case PhaseStatus.DELETE_FAILED:
            case PhaseStatus.ARPING_ERR:
            case PhaseStatus.ARPING_OK:
                return false;
            case PhaseStatus.ADDING:
            case PhaseStatus.AGENT_ADDING:
            case PhaseStatus.AGENT_DELETING:
            case PhaseStatus.DELETING:
            case PhaseStatus.ARPING:
            case PhaseStatus.ARPING_DONE:
                return true;
        }
        return false;
    }

    private void processAdding(Port tblPort)
    {
        Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
        String subnetIdFromAgent = tblSubnet.getSubnetIdFromAgent();
        if (StrUtil.isBlank(subnetIdFromAgent))
        {
            log.error("subnetId {} subnetIdFromAgent is null, subnet is creating...", tblSubnet.getSubnetId());
            return;
        }
        BaseRsp result = createPort(tblPort, subnetIdFromAgent);
        if (null == result)
        {
            log.error("create port failed, vpcId: {}, result is null", tblPort.getPortId());
            return;
        }
        if (AgentConstant.FAILED.equals(result.getStatus()))
        {
            log.error("create port failed, portId: {}, reason:{}", tblPort.getPortId(), result.getReason());
            tblPort.setPhaseStatus(PhaseStatus.ADD_FAILED);
            tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblPort.setPortIdFromAgent(result.getUuid());
            boolean ok = portService.updateById(tblPort);
            if (!ok) {
                log.error("current status is ADDING: update database error.");
            }
            else {
                logRpcService.getLogService().addEvent(tblSubnet.getUserId(),"Agent 创建port异常",
                        String.format("请求参数 subnetId:%s ", tblSubnet.getSubnetId() ), "创建失败");
            }
        }
        else if (AgentConstant.PENDING.equals(result.getStatus()))
        {
            tblPort.setPhaseStatus(PhaseStatus.AGENT_ADDING);
            tblPort.setPortIdFromAgent(result.getUuid());
            tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = portService.updateById(tblPort);
            if (!ok) {
                log.error("current status is ADDING: update database error.");
            }
            else {
                logRpcService.getLogService().addEvent(tblSubnet.getUserId(),"Agent 正在创建port",
                        String.format("请求参数 subnetId:%s",tblSubnet.getSubnetId()), "创建中");
            }
        }
    }

    private void processRemoving(Port tblPort)
    {

        String result = removePort(tblPort);
        if (AgentConstant.OK.equals(result))
        {
            tblPort.setPhaseStatus(PhaseStatus.AGENT_DELETING);
        }
        else if (AgentConstant.FAILED.equals(result))
        {
            tblPort.setPhaseStatus(REMOVED);
        }
        else
        {
            return;
        }
        tblPort.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = portService.updateById(tblPort);
        if (!ok)
        {
            log.error("current status is DELETING: update database error.");
        }
    }

    private void processCheckAgentAddingResult(Port tblPort)
    {
        String subnetId = tblPort.getSubnetId();
        Subnet tblSubnet = subnetService.getById(subnetId);
        PortFromAgentRsp rsp = getPort(tblPort);
        if (rsp == null) {
            log.error("get port from agent failed, portId: {}", tblPort.getPortId());
            return;
        }
        if (Objects.equals(rsp.getPhaseStatus(), AgentConstant.SUCCESS) &&
                Objects.equals(rsp.getStatus(), AgentConstant.OK))
        {
            tblPort.setIpAddress(rsp.getIp());
            tblPort.setMacAddress(rsp.getMac());
            tblPort.setPhaseStatus(PhaseStatus.ADDED);
            tblPort.setPortIdFromAgent(rsp.getPortIdFromAgent());
        }
        else if (Objects.equals(rsp.getStatus(), AgentConstant.FAILED))
        {
            tblPort.setPhaseStatus(PhaseStatus.AGENT_ADDING_ERR);
        }
        tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = portService.updateById(tblPort);
        if (!ok)
        {
            log.error("current status is AGENT_ADDING: update database error.");
        }
        else
        {
            String logResult = String.format("创建中,ip地址: %s, portId:%s",tblPort.getIpAddress(), tblPort.getPortId());
            if (PhaseStatus.AGENT_ADDING_ERR == tblPort.getPhaseStatus())
            {
                logResult = "创建失败";
            }
            else if (PhaseStatus.ADDED == tblPort.getPhaseStatus())
            {
                logResult = String.format("创建成功,ip地址: %s, portId:%s",tblPort.getIpAddress(), tblPort.getPortId());
            }
            logRpcService.getLogService().addEvent(tblSubnet.getUserId(),"Agent 获取port的状态",
                    String.format("获取port的状态,portId:%s",tblPort.getPortId()), logResult);
        }
    }

    // Check the result of the backend service processing the delete request
    private void processCheckAgentDeletingResult(Port tblPort)
    {
        String subnetId = tblPort.getSubnetId();
        Subnet tblSubnet = subnetService.getById(subnetId);
        PortFromAgentRsp rsp = getPort(tblPort);
        if (rsp == null)
        {
            log.error("get port from agent failed, portId: {}", tblPort.getPortId());
            return;
        }
        if (Objects.equals(rsp.getPhaseStatus(), AgentConstant.FAIL) &&
                Objects.equals(rsp.getStatus(), AgentConstant.OK))
        {
            tblPort.setPhaseStatus(PhaseStatus.AGENT_DELETING_ERR);
        }
        else if (Objects.equals(rsp.getStatus(), AgentConstant.FAILED) &&
                rsp.getReason().contains(AgentConstant.NOT_FOUND))
        {
            tblPort.setPhaseStatus(REMOVED);
        }
        tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//                int count = networkRepository.updateShare(vpc);
        boolean ok = portService.updateById(tblPort);
        if (!ok)
        {
            log.error("current status is AGENT_ADDING: update database error.");
        }
        else
        {
            log.info("port:(port: {},portIdFromAgent: {}) is deleted successfully."
                    , tblPort.getPortId(), tblPort.getPortIdFromAgent());
            logRpcService.getLogService().addEvent(tblSubnet.getUserId(), "Agent 删除port成功",
                    String.format("请求参数 portId:%s", tblPort.getPortId()), "删除成功");
        }
    }

//    private void processArping(Port tblPort)
//    {
//        String result;
//        String ip = tblPort.getIpAddress();
//        if (ip == null)
//        {
//            result = getPort(tblPort);
//            Map resultMap = JsonUtil.jsonToMap(result);
//            if (resultMap == null)
//            {
//                return;
//            }
//            if (!resultMap.containsKey("ip"))
//            {
//                return;
//            }
//            ip = (String) resultMap.get("ip");
//            tblPort.setIpAddress(ip);
//        }
//
//        result = arping(tblPort.getPortIdFromAgent(), ip);
//
//        if (result == null)
//        {
//            tblPort.setPhaseStatus(PhaseStatus.ARPING_ERR);
//        }
//        else if (result.equals(tblPort.getPortIdFromAgent()))
//        {
//            tblPort.setPhaseStatus(PhaseStatus.ARPING_DONE);
//        }
//        boolean ok = portService.updateById(tblPort);
//        if (!ok)
//        {
//            log.error("current status is ARPING: update database error.");
//        }
//        else
//        {
//            log.info("port:(port: {},portIdFromAgent: {}) ARPING successfully."
//                    , tblPort.getPortId(), tblPort.getPortIdFromAgent());
//        }
//
//    }

    // create port from agent
    // return null if an exception occurred on the request backend
    private BaseRsp createPort(Port tblPort, String subnetId)
    {
//        String mac, String subnetId, String ip, int portType, String instanceId
        String postUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getSubnetUrl() + "/"
                + subnetId + "/" + AgentConstant.PORTS;
        BaseRsp result = null;
        String jsonString;
        ComputeService.AgentIpPort agentIpPort = new ComputeService.AgentIpPort();
        PortCreateFromAgentReq createPortReq = new PortCreateFromAgentReq();
        Integer portType = tblPort.getType();
        switch (tblPort.getType())
        {
            case PortType.vm:
            case PortType.vip:
//                agentIpPort = combRpcSerice.getVmService().getAgentIpPort((short) PortType.vm, instanceId);
                createPortReq.setPurpose("kvm");
                createPortReq.setVip(false);
                if (PortType.vip == portType)
                {
                    createPortReq.setVip(true);
                }
                createPortReq.setAgentId(tblPort.getAgentId());
                break;
            case PortType.lb:
                createPortReq.setPurpose("haproxy");
                createPortReq.setVip(false);
                createPortReq.setAgentId(tblPort.getAgentId());
                break;
            case PortType.nfs:
                createPortReq.setPurpose("nfs");
                createPortReq.setVip(false);
                createPortReq.setAgentId(tblPort.getAgentId());
                break;
            case PortType.baremetal:
                agentIpPort = combRpcSerice.getBmService().getAgentIpPort((short) PortType.baremetal, tblPort.getInstanceId());
                break;
        }

        if (!StrUtil.isEmpty(tblPort.getIpAddress()))
        {
            createPortReq.setIp(tblPort.getIpAddress());
        }
        else
        {
            createPortReq.setIp("");
        }

        jsonString = JsonUtil.objectToJson(createPortReq);
        jsonString = UnderlineAndHump.humpToUnderline(jsonString).toLowerCase();

        log.info("create port from agent, request json: {}", jsonString);
        try
        {
            result = HttpActionUtil.post(postUrl, jsonString, BaseRsp.class);
        }
        catch (Exception e)
        {
            log.error("create port from agent error: {}", e.getMessage());
        }
        return result;
    }

    // get port info from agent
    // return null if an exception occurred on the request backend
    private PortFromAgentRsp getPort(Port tblPort)
    {
        String getUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getPortUrl() + "/" + tblPort.getPortIdFromAgent();
        PortFromAgentRsp result = null;
        try
        {
            result = HttpActionUtil.getObject(getUrl, PortFromAgentRsp.class);
        }
        catch (Exception e)
        {
            log.error("get port from agent error: {}", e.getMessage());
            return null;
        }
        return result;
    }

    // delete port from agent
    // return null if an exception occurred on the request backend
    private String removePort(Port tblPort)
    {
        String delUrl = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getPortUrl() + "/" + tblPort.getPortIdFromAgent();
        if (StrUtil.isEmpty(tblPort.getPortIdFromAgent()))
        {
            log.error("remove port from agent, portId is null");
            return AgentConstant.OK;
        }
        BaseRsp baseRsp;
        log.info("remove port from agent, portId: {}", tblPort.getPortIdFromAgent());
        try
        {
            baseRsp = HttpActionUtil.delete(delUrl, BaseRsp.class);
            if (baseRsp != null && StrUtil.isBlank(baseRsp.getReason())) {
                return baseRsp.getStatus();
            }
            if (baseRsp != null && StrUtil.isNotBlank(baseRsp.getReason()))  {
                if (baseRsp.getReason().contains(AgentConstant.NOT_FOUND)) {
                    return AgentConstant.OK;
                }
                log.error("delete port from agent error, portId: {}, reason: {}", tblPort.getPortId(), baseRsp.getReason());
                return baseRsp.getReason();
            }
            if (baseRsp == null)
            {
                log.error("delete port from agent error, portId: {}, baseRsp is null", tblPort.getPortId());
                return null;
            }
        } catch (Exception e) {
            log.error("delete port from agent error, vpcId: {}", tblPort.getPortId());
            return null;
        }
        return baseRsp.getStatus();
    }

    public String arping(String portId, String ip)
    {
        String url = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getPortUrl()
                + "/" + portId + "/arping";
        IpInfo ipInfo = IpInfo.builder().ip(ip).build();
        String putJson = JsonUtil.objectToJson(ipInfo);
        log.info("put url:{} json:{},", url, putJson);
        String result;
        try
        {
            result = HttpActionUtil.put(url, putJson);
        }
        catch (Exception e)
        {
            log.error("arping error: {}", e.getMessage());
            return null;
        }
        Map statusMap = JsonUtil.jsonToMap(result);
        if (statusMap == null)
        {
            return null;
        }
        if (!Objects.equals(statusMap.get("status"), "pending"))
        {
            return null;
        }
        return portId;
    }
}
