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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.EipPortMapStatus;
import com.lnjoying.justice.network.common.Protocols;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.response.BaseRsp;
import com.lnjoying.justice.network.domain.backend.response.EipPortMapInfo;
import com.lnjoying.justice.network.domain.backend.response.PortFromAgentRsp;
import com.lnjoying.justice.network.entity.*;
import com.lnjoying.justice.network.service.EipMapService;
import com.lnjoying.justice.network.service.EipService;
import com.lnjoying.justice.network.service.PortMapService;
import com.lnjoying.justice.network.service.PortService;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.lnjoying.justice.network.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Component
@Slf4j
public class EipPortMapTimerProcessor extends AbstractRunnableProcessor 
{
    
    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    EipMapService eipMapService;

    @Autowired
    EipService eipService;

    @Autowired
    PortMapService portMapService;

    @Autowired
    PortService portService;

    @Autowired
    CombRpcSerice combRpcSerice;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    LogRpcService logRpcService;

    public EipPortMapTimerProcessor()
    {
    }

    @Override
    public void start() {
        log.info("eipPortMap timer processor start");
    }

    @Override
    public void stop() {
        log.info("eipPortMap timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
//            processMapping();
//            processUnmapping();
//            processCheckAgentResult(EipPortMapStatus.AGENT_MAPPING);
//            processCheckAgentResult(EipPortMapStatus.AGENT_UNMAPPING);
            processEipMaps(getMiddleStatusEipMaps());
        }
        catch (Exception e)
        {
            log.error("network timer processor exception: {}", e.getMessage());
        }
    }

    private List<EipMap> getMiddleStatusEipMaps()
    {
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(EipMap::getStatus, EipPortMapStatus.MAPPED )
                .ne(EipMap::getStatus, EipPortMapStatus.UNMAPPED)
                .ne(EipMap::getStatus, EipPortMapStatus.AGENT_MAPPING_ERR)
                .ne(EipMap::getStatus,EipPortMapStatus.AGENT_UNMAPPING_ERR)
                .ne(EipMap::getStatus,REMOVED);
        return eipMapService.list(queryWrapper);

    }

    private void processEipMaps(List<EipMap> tblEipMaps)
    {
        try
        {
            if (null == tblEipMaps || tblEipMaps.isEmpty())
            {
                return;
            }
            log.info("get tblEipMaps :{}", tblEipMaps);
            for ( EipMap tblEipMap: tblEipMaps )
            {
                processEipMap(tblEipMap);
            }
        }
        catch (Exception e)
        {
            log.error("eipMap timer processor error:  {}", e.getMessage());
        }
    }

    private void processEipMap(EipMap tblEipMap)
    {
        int phaseStatus = tblEipMap.getStatus();
        try
        {
            switch (phaseStatus)
            {
                case EipPortMapStatus.MAPPING:
                    processCreateEipMap(tblEipMap);
                    break;
                case EipPortMapStatus.UNMAPPING:
                    processRemoveEipMap(tblEipMap);
                    break;
                default:
                    getEipMapStatus(tblEipMap);
                    break;
            }
        }
        catch (Exception e)
        {
            log.error("eipMap timer processor error: eipMapId {}, phase status {} , exception {}", tblEipMap.getEipMapId(), phaseStatus, e.getMessage());
        }
    }

    private void processCreateEipMap(EipMap tblEipMap)
    {
        String portId = tblEipMap.getPortId();
        if (null == portId || portId.isEmpty())
        {
            tblEipMap.setStatus(EipPortMapStatus.AGENT_MAPPING_ERR);
            eipMapService.updateById(tblEipMap);
            return;
        }
        Port tblPort = portService.getById(tblEipMap.getPortId());
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus())
        {
            tblEipMap.setStatus(EipPortMapStatus.AGENT_MAPPING_ERR);
            eipMapService.updateById(tblEipMap);
            return ;
        }
        BaseRsp result;
        EipPortMapInfo.PutEipPortMapReq portMapReq = null;
        if (tblEipMap.getIsOneToOne())
        {
            portMapReq = processIpEipMap(tblEipMap, tblPort);
            result = createEipPortMapFromAgent( tblPort.getPortIdFromAgent(), portMapReq);
        }
        else
        {
            portMapReq = processTcpUdpEipMap(tblEipMap, tblPort);
            result = removeEipPortMapFromAgent(tblPort.getPortIdFromAgent(),null);
//            result = getEipPortMapFromAgent( tblPort.getPortIdFromAgent());

            if (null == result) return;
//            Port tblPort = portService.getById(tblEipMap.getPortId());
            if ( Objects.equals(AgentConstant.FAILED,result.getStatus()) && StrUtil.contains(result.getReason(),AgentConstant.PORT_NOT_BOUND))
            {
                result = createEipPortMapFromAgent( tblPort.getPortIdFromAgent(), portMapReq);
            }
            else
            {
                return;
            }
        }

        processCreateEipMapResult(tblEipMap, portMapReq, result);
    }

    public EipPortMapInfo.PutEipPortMapReq processIpEipMap(EipMap tblEipMap,Port tblPort)
    {
        //格式 protocol1:port1#port2,protocol2:port3#port4
        EipPortMapInfo.PutEipPortMapReq portMapReq = EipPortMapInfo.PutEipPortMapReq.builder()
                .eip(eipService.getById(tblEipMap.getEipId()).getIpaddr())
                .mapping("ip,")
                .build();
        return portMapReq;
//        String nodeIp = combRpcSerice.getVmService().getHypervisorNodeIp(tblPort.getInstanceId());
//        return createEipPortMapFromAgent(nodeIp, tblPort.getNodePortId(), portMapReq);
    }

    public void processCreateEipMapResult(EipMap tblEipMap, EipPortMapInfo.PutEipPortMapReq portMapReq, BaseRsp result)
    {
        log.info("createEipPortMapFromAgent result: {}", result);
        if (null == result) return;

        if (Objects.equals(result.getStatus(), AgentConstant.PENDING))
        {
            if (updateEipMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_MAPPING))
            {
                logRpcService.getLogService().addEvent(tblEipMap.getUserId(),
                        "Agent 正在创建Nat网关", String.format("请求参数: eipMapId: %s  EIP: %s", tblEipMap.getEipMapId(), portMapReq.getEip()), "创建中");
            }
            else
            {
                log.info("update database eipMapStatus error.");
            }
        }
        else if (Objects.equals(result.getStatus(), AgentConstant.FAILED))
        {
            if (Objects.equals(result.getReason(), AgentConstant.PORT_ALREADY_BOUND))
            {
                boolean ok = updateEipMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_MAPPING);
                if (!ok)
                {
                    log.info("update database portMapStatus error. eipMapId:{}", tblEipMap.getEipMapId());
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            }
            else
            {
                if (updatePortMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_MAPPING_ERR))
                {
                    logRpcService.getLogService().addEvent(tblEipMap.getUserId(),
                            "Agent 正在创建Nat网关", String.format("请求参数: eipMapId: %s EIP:%s", tblEipMap.getEipMapId(), portMapReq.getEip()), "创建失败");
                }
                else
                {
                    log.info("update database portMapStatus error. eipMapId:{}", tblEipMap.getEipMapId());
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            }
        }

    }

    public void processRemoveEipMapResult(EipMap tblEipMap, BaseRsp result)
    {
        if (null == result)
        {
            log.error("removeEipPortMapFromAgent result is null. eipMapId:{}", tblEipMap.getEipMapId());
            return;
        }
        String eip = eipService.getById(tblEipMap.getEipId()).getIpaddr();

        if (Objects.equals(result.getStatus(), AgentConstant.PENDING))
        {
            log.info("agent is unbinding port");
            if (!updatePortMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_UNMAPPING)) {
                log.info("update database table: portMap error. eipMapId:{}",tblEipMap.getEipMapId());
            }
            if (!updateEipMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_UNMAPPING)) {
                log.info("update database table: eipMap error. eipMapId:{}",tblEipMap.getEipMapId());
            }
            else
            {
                logRpcService.getLogService().addEvent(eipMapService.getById(tblEipMap.getEipMapId()).getUserId(),
                        "Agent 正在删除Nat网关",String.format("请求参数: EIP:%s",eip),"删除中");
            }
        }
        else if (Objects.equals(result.getStatus(), AgentConstant.FAILED) && (result.getReason().contains(AgentConstant.PORT_NOT_BOUND)
        || result.getReason().contains(AgentConstant.NOT_FOUND)))
        {
            if (!updatePortMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_UNMAPPING) )
            {
                log.info("update database table: portMap error. eipMapId:{}",tblEipMap.getEipMapId());
                return;
            }
            if (!updateEipMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_UNMAPPING) ) {
                log.info("update database table: eipMap error. eipMapId:{}",tblEipMap.getEipMapId());
            }
            else
            {
                logRpcService.getLogService().addEvent(eipMapService.getById(tblEipMap.getEipMapId()).getUserId(),
                        "Agent 正在删除Nat网关",String.format("请求参数: EIP:%s",eip),"删除成功");
            }
        }


    }

    public EipPortMapInfo.PutEipPortMapReq processTcpUdpEipMap(EipMap tblEipMap,Port tblPort)
    {
        //格式 protocol1:port1#port2,protocol2:port3#port4

        String portIdFromAgent = tblPort.getPortIdFromAgent();
        if (null == portIdFromAgent || portIdFromAgent.isEmpty())
        {
            tblEipMap.setStatus(EipPortMapStatus.AGENT_MAPPING_ERR);
            eipMapService.updateById(tblEipMap);
            return null;
        }
        String forwardTcpStr;
        String forwardUdpStr;
        String forwardTcpUdpStr;
        String tcpPorts = getLocalPorts(tblEipMap.getEipMapId(), Protocols.TCP, EipPortMapStatus.MAPPING);
        if (null == tcpPorts) {
            forwardTcpStr="";
        }
        else
        {
            forwardTcpStr = "tcp:" + tcpPorts;
        }
        String udpPorts = getLocalPorts(tblEipMap.getEipMapId(), Protocols.UDP, EipPortMapStatus.MAPPING);
        if (null == udpPorts) {
            forwardUdpStr="";
        }
        else
        {
            forwardUdpStr = "udp:" + udpPorts;
        }
        if (forwardTcpStr.isEmpty() && forwardUdpStr.isEmpty())
        {
            tblEipMap.setStatus(EipPortMapStatus.AGENT_MAPPING_ERR);
            eipMapService.updateById(tblEipMap);
            return null;
        }
        if (forwardTcpStr.isEmpty())
        {
            forwardTcpUdpStr = forwardUdpStr;
        }
        else if (forwardUdpStr.isEmpty())
        {
            forwardTcpUdpStr = forwardTcpStr;
        }
        else
        {
            forwardTcpUdpStr = forwardTcpStr + "," + forwardUdpStr;
        }

        EipPortMapInfo.PutEipPortMapReq portMapReq = EipPortMapInfo.PutEipPortMapReq.builder()
                .eip(eipService.getById(tblEipMap.getEipId()).getIpaddr())
                .mapping(forwardTcpUdpStr)
                .build();
        return portMapReq;
//        return createEipPortMapFromAgent(null, portIdFromAgent, portMapReq);
    }

    private void processRemoveEipMap(EipMap tblEipMap)
    {
        if (StrUtil.isBlank(tblEipMap.getPortId()))
        {
            tblEipMap.setStatus(REMOVED);
            tblEipMap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            eipMapService.updateById(tblEipMap);
            return;
        }
        Port tblPort = portService.getById(tblEipMap.getPortId());
        BaseRsp result;

        result = removeEipPortMapFromAgent(tblPort.getPortIdFromAgent(), null);

        processRemoveEipMapResult(tblEipMap, result);

    }


    private void geEipMapMappingStatus(EipMap tblEipMap, PortFromAgentRsp result)
    {
        boolean eipMapOk;
        boolean portMapOk;
        if (null == result) return;
        if (Objects.equals(result.getPhaseType(), "bind") && Objects.equals(result.getPhaseStatus(), AgentConstant.SUCCESS))
        {
            eipMapOk = updateEipMapInsideIpStatus(tblEipMap.getEipMapId(), result.getIp(), EipPortMapStatus.MAPPED);
            portMapOk = updatePortMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.MAPPED);
            logRpcService.getLogService().addEvent(eipMapService.getById(tblEipMap.getEipMapId()).getUserId(),
                    "Agent 正在获取Nat网关的状态",String.format("请求参数: eipMapId:%s",tblEipMap.getEipMapId()),"创建成功");
        }
        else if (Objects.equals(result.getPhaseType(), "bind") && Objects.equals(result.getPhaseStatus(), AgentConstant.FAILED))
        {
            eipMapOk = updateEipMapInsideIpStatus(tblEipMap.getEipMapId(), result.getIp(),EipPortMapStatus.AGENT_MAPPING_ERR);
            portMapOk = updatePortMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_MAPPING_ERR);
            logRpcService.getLogService().addEvent(eipMapService.getById(tblEipMap.getEipMapId()).getUserId(),
                    "Agent 正在获取Nat网关的状态",String.format("请求参数: eipMapId:%s",tblEipMap.getEipMapId()),"创建失败");
        }
        else
        {
            logRpcService.getLogService().addEvent(eipMapService.getById(tblEipMap.getEipMapId()).getUserId(),
                    "Agent 正在获取Nat网关的状态",String.format("请求参数: mapId:%s",tblEipMap.getEipMapId()),"创建中");
            return;
        }
        if (!eipMapOk || !portMapOk) {
            log.error("current status is AGENT_MAPPING: update database error.");
        }
    }

    private void geEipMapUnMappingStatus(EipMap tblEipMap, PortFromAgentRsp result)
    {
        boolean eipMapOk;
        boolean portMapOk;
        if (null == result) return;
        Port tblPort = portService.getById(tblEipMap.getPortId());
        if ((Objects.equals(result.getPhaseStatus(),AgentConstant.SUCCESS) && Objects.equals(result.getPhaseType(),"unbind"))
        || null == result.getFloatingIp())
        {
            String rpcResult = combRpcSerice.getVmService().setVmInstanceEip("", tblPort.getInstanceId());
            if (!Objects.equals(rpcResult, tblPort.getInstanceId()))  return;
            LambdaUpdateWrapper<PortMap> portMapUpdateWrapper = new LambdaUpdateWrapper<>();
            portMapUpdateWrapper.eq(PortMap::getEipMapId, tblEipMap.getEipMapId());
            PortMap tblRsPortMap = new PortMap();
            tblRsPortMap.setStatus(REMOVED);
            tblRsPortMap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            portMapOk = portMapService.update(tblRsPortMap, portMapUpdateWrapper);

            //eipMap
            tblEipMap.setStatus(REMOVED);
            tblEipMap.setUpdateTime(new Date(System.currentTimeMillis()));
            eipMapOk = eipMapService.updateById(tblEipMap);
            //eip
            Eip tblEip = eipService.getById(tblEipMap.getEipId());
            String boundId = tblEip.getBoundId();
            log.info("boundId:{}, contains(,):{}",boundId, boundId.contains(","));
            if (!boundId.contains(","))
            {
                tblEip.setBoundType(null);
                tblEip.setBoundId(null);
                tblEip.setUserId(null);
            }
            else
            {
                String[] boundIds = boundId.split(",");
                List<String> boundIdList = Arrays.asList(boundIds);
                String eipMapId = tblEipMap.getEipMapId();
                log.info("boundIdList:{}, eipPortMapInfo.getEipMapId():{}",boundIdList, tblEipMap.getEipMapId());
                String boundIdStr = "";
                for (String id : boundIdList)
                {
                    if (eipMapId.equals(id))
                    {
                        continue;
                    }
                    boundIdStr += id + ",";
                }

                //boundIdList 转换为字符串
//                                String boundIdStr = String.join(",", boundIdList);
                //如果有”,”，去掉最后一个
                boundIdStr = boundIdStr.endsWith(",") ? boundIdStr.substring(0, boundIdStr.length() - 1) : boundIdStr;
                log.info("boundIdStr:{}",boundIdStr);
                tblEip.setBoundId(boundIdStr);
            }
            tblEip.setUpdateTime(new Date(System.currentTimeMillis()));
            boolean eipOk = eipService.updateById(tblEip);
            if (eipOk && eipMapOk && portMapOk)
            {
                logRpcService.getLogService().addEvent(tblEipMap.getUserId(),
                        "Agent 正在获取Nat网关的状态", String.format("请求参数: eipMapId:%s",  tblEipMap.getEipMapId()), "删除成功");
            }
            //                             updatePortMapStatus(eipMapId, EipPortMapStatus.UNMAPPED);
        }
        else if(Objects.equals(result.getPhaseStatus(), AgentConstant.FAILED) && Objects.equals(result.getPhaseType(), "unbind"))
        {
            portMapOk = updatePortMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_UNMAPPING_ERR);
            eipMapOk = updateEipMapStatus(tblEipMap.getEipMapId(), EipPortMapStatus.AGENT_UNMAPPING_ERR);
            logRpcService.getLogService().addEvent(tblEipMap.getUserId(),
                    "Agent 正在获取Nat网关的状态",String.format("请求参数: eipMapId:%s",tblEipMap.getEipMapId()),"删除失败");
        }
        else{
            logRpcService.getLogService().addEvent(tblEipMap.getUserId(),
                    "Agent 正在获取Nat网关的状态",String.format("请求参数: eipMapId:%s",tblEipMap.getEipMapId()),"获取中");
            return;
        }
        if (!eipMapOk || !portMapOk) {
            log.error("current status is AGENT_UNMAPPING: update database error.");
        }
    }

    private void getEipMapStatus(EipMap tblEipMap)
    {
        PortFromAgentRsp result;
        Port tblPort = portService.getById(tblEipMap.getPortId());
        try
        {
            result = getEipPortMapFromAgent(tblPort.getPortIdFromAgent());

            int phaseStatus = tblEipMap.getStatus();
            switch (phaseStatus)
            {
                case EipPortMapStatus.AGENT_MAPPING:
                    geEipMapMappingStatus(tblEipMap, result);
                    break;
                case EipPortMapStatus.AGENT_UNMAPPING:
                    geEipMapUnMappingStatus(tblEipMap, result);
                    break;
                default:
            }
//
//            logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(), "Agent 获取安全组状态",
//                    String.format("请求参数 sgId:%s ",tblSecurityGroup.getSgId()), result);
        }
        catch (Exception e)
        {
            log.error("getEipMapStatus error:{}, eipMapId:{}, ", e.getMessage(), tblEipMap.getEipMapId());
        }
    }

    private List<PortMap> getMiddleStatusPortMaps(int mapStatus) {
        if (mapStatus == EipPortMapStatus.MAPPED || mapStatus == EipPortMapStatus.UNMAPPED) {
            return null;
        }
        LambdaQueryWrapper<PortMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PortMap::getStatus, mapStatus);
        if (portMapService.count(queryWrapper) > 0)
        {
            return portMapService.list(queryWrapper);
        }
        return  null;
    }

    private String getLocalPorts(String eipMapId, int protocol, int mapStatus) {
        //格式 protocol1:port1#port2,protocol2:port3#port4
        LambdaQueryWrapper<PortMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PortMap::getProtocol, protocol)
                .eq(PortMap::getEipMapId, eipMapId)
                .ne(PortMap::getStatus, REMOVED);
//                .eq(PortMap::getStatus, mapStatus);
        if (portMapService.count(queryWrapper) > 0)
        {
            List<PortMap> portMaps = portMapService.list(queryWrapper);
            List<String> globalLocalPorts = portMaps.stream().map(portMap -> {
                    return portMap.getGlobalPort()+"-"+portMap.getLocalPort();
                    }).collect(Collectors.toList());
            return StrUtil.join("#",globalLocalPorts);
//            return StringUtils.join(portMapService.list(queryWrapper).stream()
//                    .map(PortMap::getLocalPort).toArray());
        }

        return null;
    }

    private boolean isOneToOne(String eipMapId, int mapStatus)
    {
        LambdaQueryWrapper<PortMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PortMap::getProtocol, Protocols.IP)
                .eq(PortMap::getEipMapId, eipMapId)
                .eq(PortMap::getStatus, mapStatus);
        return portMapService.count(queryWrapper) > 0;
    }

    private boolean updatePortMapStatus(String eipMapId, int portMapStatus)
    {
        LambdaUpdateWrapper<PortMap> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortMap::getEipMapId, eipMapId)
                .ne(PortMap::getStatus, REMOVED);
        PortMap portMap = new PortMap();
        portMap.setUpdateTime(new Date(System.currentTimeMillis()));
        portMap.setStatus(portMapStatus);
        return portMapService.update(portMap, updateWrapper);
    }

    private boolean updateEipMapStatus(String eipMapId, int eipMapStatus) {
        EipMap eipMap = eipMapService.getById(eipMapId);
        if (eipMap != null) {
            eipMap.setStatus(eipMapStatus);
            eipMap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            return eipMapService.updateById(eipMap);
        }
        return false;
    }

    private boolean updateEipMapInsideIpStatus(String eipMapId, String insideIp, int eipMapStatus) {
        EipMap eipMap = eipMapService.getById(eipMapId);
        if (eipMap != null) {
            eipMap.setInsideIp(insideIp);
            eipMap.setStatus(eipMapStatus);
            return eipMapService.updateById(eipMap);
        }
        return false;
    }

    private boolean isIpBind(String forwardings)
    {
        // forwardings 是否包含”ip“字符串
        if (StrUtil.isBlank(forwardings))
        {
            return false;
        }
        return forwardings.contains("ip");

    }

    public BaseRsp createEipPortMapFromAgent( String portId, EipPortMapInfo.PutEipPortMapReq putEipPortMapReq) {
        String jsonString = JsonUtil.objectToJson(putEipPortMapReq);
        String url = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getPortUrl() + "/" + portId + "/bind";

        BaseRsp result = null;
        log.info("create eipPortMap from Agent，url:{}, json: {}", url, jsonString);
        try {
            result = HttpActionUtil.put(url, jsonString, BaseRsp.class);
        }
        catch (Exception e) {
            log.error("create eipPortMap from Agent error: {}", e.getMessage());
            return null;
        }
        return result;
    }

    public BaseRsp removeEipPortMapFromAgent(String portId, EipPortMapInfo.PutEipPortMapReq putEipPortMapReq) {
        String jsonString = null;
        if (null != putEipPortMapReq)
        {
            jsonString = JsonUtil.objectToJson(putEipPortMapReq);
        }

        String url = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getPortUrl() + "/" + portId + "/unbind";

        BaseRsp result = null;
        log.info("delete eipPortMap from Agent，url:{}, json: {}", url, jsonString);
        try {
            result = HttpActionUtil.put(url, jsonString, BaseRsp.class);
        } catch (Exception e) {
            log.error("delete eipPortMap from Agent error: {}", e.getMessage());
            return null;
        }
        return result;
    }

    public PortFromAgentRsp getEipPortMapFromAgent(String portId)
    {
        String url = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getPortUrl() + "/" + portId;


        PortFromAgentRsp result = null;
        log.info("get eipPortMap from Agent，url:{}", url);
        try {
            result = HttpActionUtil.getObject(url,PortFromAgentRsp.class);
        } catch (Exception e) {
            log.error("get eipPortMap from Agent error: {}", e.getMessage());
            return null;
        }
        return result;
    }

}
