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

package com.lnjoying.vm.processor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.TsQueryParameters;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.response.AgentRsp;
import com.lnjoying.vm.domain.backend.response.AgentsRsp;
import com.lnjoying.vm.domain.backend.response.GPURspFromAgent;
import com.lnjoying.vm.domain.dto.response.PrometheusRsp;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.PciDevice;
import com.lnjoying.vm.entity.PciDeviceGroup;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.PciDeviceGroupService;
import com.lnjoying.vm.service.PciDeviceService;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Slf4j
@Component
public class HypervisorNodeTimerProcessor extends AbstractRunnableProcessor
{
    @Autowired
    private HypervisorNodeService nodeService;

    @Autowired
    private PciDeviceTimerProcessor pciDeviceTimerProcessor;

    @Autowired
    private PciDeviceGroupService pciDeviceGroupService;

    @Autowired
    private ComputeConfig computeConfig;

    @Autowired
    private PciDeviceService pciDeviceService;

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    public HypervisorNodeTimerProcessor()
    {
    }

    @Override
    public void run()
    {
        try
        {
            processAgents();
            processNodes(getMiddleStatusNodes());
        }
        catch (Exception e)
        {
            log.error("hypervisor node timer processor exception: {}", e.getMessage());
        }
    }

    private List<HypervisorNode> getMiddleStatusNodes()
    {
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_OFFLINE)
                .ne(HypervisorNode::getPhaseStatus, REMOVED)
                .ne(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
                .or(wrapper -> wrapper.eq(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
//                        .isNull(HypervisorNode::getBackupNodeId)
                        .isNull(HypervisorNode::getCpuModel)
                        .isNull(HypervisorNode::getCpuPhyCount)
//                        .isNull(HypervisorNode::getMemTotal)
//                        .isNull(HypervisorNode::getCpuLogCount)
                ).or(wrapper -> wrapper.isNull(HypervisorNode::getBackupNodeId).eq(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED));
        return nodeService.list(queryWrapper);
    }

    public void processNodes(List<HypervisorNode> tblNodes)
    {
        try
        {
//            log.info("get nodes :{}", tblNodes);
            for (HypervisorNode tblNode : tblNodes)
            {
                processNode(tblNode);
            }
        }
        catch (Exception e)
        {
            log.error("node timer processor error:  {}", e.getMessage());
        }
    }

    public void setNodeCpuMemInfo(HypervisorNode tblNode)
    {
        boolean needUpdate = false;
        if (StrUtil.isEmpty(tblNode.getCpuModel()))
        {
            String cpuModel = getCpuModel(tblNode);
            tblNode.setCpuModel(cpuModel);
            needUpdate = true;
        }
        if (null == tblNode.getCpuLogCount())
        {
            Integer logicalCpuCount = getLogicalCpuCount(tblNode);
            tblNode.setCpuLogCount(logicalCpuCount);
            needUpdate = true;
        }
        if (null == tblNode.getCpuPhyCount())
        {
            Integer physicalCpuCount = getPhysicalCpuCount(tblNode);
            tblNode.setCpuPhyCount(physicalCpuCount);
            needUpdate = true;
        }
        if (null == tblNode.getMemTotal())
        {
            Integer memTotalGB = getMemTotalGB(tblNode);
            tblNode.setMemTotal(memTotalGB);
            needUpdate = true;
        }
        if (needUpdate)
        {
            tblNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            nodeService.updateById(tblNode);
        }
    }
    // 移除到不存在的pci_device_group
//    private void removeNotExistsPciDeviceGroup()
//    {
//        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.select(PciDevice::getDeviceGroupId)
//                .ne(PciDevice::getPhaseStatus, REMOVED);
//        List<String> pciDeviceGroupIds = pciDeviceService.listObjs(queryWrapper, Object::toString).stream().distinct().collect(Collectors.toList());
//        LambdaUpdateWrapper<PciDeviceGroup> groupLambdaQueryWrapper = new LambdaUpdateWrapper<>();
//        groupLambdaQueryWrapper.notIn(PciDeviceGroup::getDeviceGroupId, pciDeviceGroupIds);
//
//    }

    public void processAgents()
    {
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(HypervisorNode::getAgentId)
                .ne(HypervisorNode::getPhaseStatus, REMOVED);
        List<String> agentIds = nodeService.listObjs(queryWrapper, Object::toString);
//        Set<String> agentIdSet = new HashSet<>(agentIds);
        AgentsRsp agentsRsp = getAgents();
        if (null == agentsRsp) return;
        //  agentsRsp.getAgents()是全部的，获取与数据库中的差集
        agentsRsp.getAgents().removeAll(agentIds);
        // 遍历agent Id ，添加到数据库中
        for (String agentId : agentsRsp.getAgents())
        {
            AgentRsp agentRsp = getAgent(agentId);
            if (null == agentRsp) continue;
            HypervisorNode newTblNode = new HypervisorNode();
            newTblNode.setNodeId(Utils.assignUUId());
            newTblNode.setAgentId(agentId);
            newTblNode.setManageIp(agentRsp.getAgentIp());
            newTblNode.setName(agentRsp.getAgentIp());
            newTblNode.setPhaseStatus(VmInstanceStatus.HYPERVISOR_NODE_CHECKING);
            newTblNode.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            newTblNode.setUpdateTime(newTblNode.getCreateTime());
            newTblNode.setErrorCount(0);
            newTblNode.setMasterL3(agentRsp.getMasterL3());
            if (agentRsp.getMasterL3())
            {
                AgentConstant.L3Ip.set(agentRsp.getAgentIp());
            }
            nodeService.save(newTblNode);
        }
    }

    private AgentsRsp getAgents()
    {
        String ips = computeConfig.getAgentIps();
        String[] ipArray = ips.split(",");
        for (String ip : ipArray)
        {
            String url = "http://" + ip + ":" + computeConfig.getVmAgentPort() + ComputeUrl.V1_AGENT_URL;
            try
            {
                return HttpActionUtil.getObject(url, AgentsRsp.class);
            }
            catch (Exception e)
            {
                log.error("get agents error: {}", e.getMessage());
            }
        }
        return null;
    }

    public AgentRsp getAgent(String agentId)
    {
        String ips = computeConfig.getAgentIps();
        String[] ipArray = ips.split(",");
        for (String ip : ipArray)
        {
            String url = "http://" + ip + ":" + computeConfig.getVmAgentPort() + ComputeUrl.V1_AGENT_URL + "/" + agentId;
            try
            {
                return HttpActionUtil.getObject(url, AgentRsp.class);
            }
            catch (Exception e)
            {
                log.error("get agent error: {}", e.getMessage());
//                return null;
            }
        }
        return null;
    }

    public void processNode(HypervisorNode tblNode)
    {
        setBackupNodeId(tblNode);
        setNodeCpuMemInfo(tblNode);
        if (VmInstanceStatus.HYPERVISOR_NODE_CREATED == tblNode.getPhaseStatus()) return;
        List<String> gpuIdFromAgents = pciDeviceTimerProcessor.getGpusFromAgent(tblNode);
        if (0 == gpuIdFromAgents.size())
        {
            tblNode.setPhaseStatus(VmInstanceStatus.HYPERVISOR_NODE_CREATED);
            tblNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            nodeService.updateById(tblNode);
            return;
        }
        TransactionStatus transactionStatus = null;
        try
        {
//            Map<String,String> deviceGroupIdMap = new HashMap<>();
            transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            for (String gpuIdFromAgent : gpuIdFromAgents)
            {
                GPURspFromAgent gpuRspFromAgent = getGpuRspFromAgent(tblNode, gpuIdFromAgent);
                if (AgentConstant.AUDIO.equals(gpuRspFromAgent.getDeviceType()))
                {
                    continue;
                }
//                String deviceGroupId ;
//                if (!deviceGroupIdMap.containsKey(gpuRspFromAgent.getDeviceGroup()))
//                {
//                    deviceGroupId = getDeviceGroupId(gpuRspFromAgent, tblNode.getNodeId());
//                    deviceGroupIdMap.put(gpuRspFromAgent.getDeviceGroup(), deviceGroupId);
//                }
//                else
//                {
//                    deviceGroupId = deviceGroupIdMap.get(gpuRspFromAgent.getDeviceGroup());
//                }
                PciDevice tblPciDevice = new PciDevice();
                tblPciDevice.setDeviceId(Utils.assignUUId());
                tblPciDevice.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                tblPciDevice.setUpdateTime(tblPciDevice.getCreateTime());
                tblPciDevice.setName(gpuRspFromAgent.getGpuName());
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHED);
                tblPciDevice.setDeviceIdFromAgent(gpuRspFromAgent.getGpuId());
                tblPciDevice.setType(gpuRspFromAgent.getDeviceType());
                tblPciDevice.setNodeId(tblNode.getNodeId());
                pciDeviceService.save(tblPciDevice);
                logRpcService.getLogService().addEvent("", "添加PCI设备",
                        String.format("请求参数: hypervisorNodeId:%s, deviceId:%s", tblNode.getNodeId(), tblPciDevice.getDeviceId()), "添加成功");
            }
            tblNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblNode.setPhaseStatus(VmInstanceStatus.HYPERVISOR_NODE_CREATED);
            if (null == computeConfig.getIbCount())
            {
                tblNode.setAvailableIbCount(0);
            }
            else
            {
                tblNode.setAvailableIbCount(computeConfig.getIbCount());
            }
            nodeService.updateById(tblNode);
            dataSourceTransactionManager.commit(transactionStatus);
            logRpcService.getLogService().addEvent("", "添加计算节点",
                    String.format("请求参数: hypervisorNodeId:%s, backupNodeId:%s", tblNode.getNodeId(), tblNode.getBackupNodeId()), "添加成功");
        }
        catch (Exception e)
        {
            log.error("processNode error:{}", e.getMessage());
            if (null != transactionStatus)
            {
                dataSourceTransactionManager.rollback(transactionStatus);
            }
        }

    }

    // 通过Prometheus获取物理CPU个数
    private Integer getPhysicalCpuCount(HypervisorNode tblHypervisorNode)
    {
        String physicalCpuCountUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{physicalCpuCount}";
        try
        {
            PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(physicalCpuCountUrl, PrometheusRsp.class, TsQueryParameters.getPhysicalCpuCount(tblHypervisorNode.getManageIp()));
            return Integer.parseInt(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
        }
        catch (Exception e)
        {
            log.error("getPhysicalCpuCount error: {}", e.getMessage());
            return null;
        }
    }

    //通过Prometheus 获取逻辑CPU个数
    private Integer getLogicalCpuCount(HypervisorNode tblHypervisorNode)
    {
        String cpuLogicalCountUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{logicalCpuCount}";
        try
        {
            PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(cpuLogicalCountUrl, PrometheusRsp.class, TsQueryParameters.getLogicalCpuCount(tblHypervisorNode.getManageIp()));
            return Integer.parseInt(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
        }
        catch (Exception e)
        {
            log.error("getLogicalCpuCount error: {}", e.getMessage());
            return null;
        }
    }

    //通过Prometheus 获取 CPU 型号
    private String getCpuModel(HypervisorNode tblHypervisorNode)
    {
        String cpuModelUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{cpuModel}";
        try
        {
            PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(cpuModelUrl, PrometheusRsp.class, TsQueryParameters.getCpuModel(tblHypervisorNode.getManageIp()));
            return getPrometheusRsp.getData().getResult()[0].getMetric().getModelName();
        }
        catch (Exception e)
        {
            log.error("getCpuModel error: {}", e.getMessage());
            return null;
        }
    }

    //通过Prometheus 获取内存大小
    private Integer getMemTotalGB(HypervisorNode tblHypervisorNode)
    {
        String memTotalGBUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{memTotalGB}";
        try
        {
            PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(memTotalGBUrl, PrometheusRsp.class, TsQueryParameters.getMemoryTotalGB(tblHypervisorNode.getManageIp()));
            return Integer.parseInt(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
        }
        catch (Exception e)
        {
            log.error("getMemTotalGB error: {}", e.getMessage());
            return null;
        }
    }


    private void setBackupNodeId(HypervisorNode tblHypervisorNode)
    {
        if (StrUtil.isNotBlank(tblHypervisorNode.getBackupNodeId()))
        {
            return;
        }
        String url = "http://" + tblHypervisorNode.getManageIp() + ":" + computeConfig.getVmAgentPort() + ComputeUrl.V1_AGENT_URL + "/" + tblHypervisorNode.getAgentId();
        AgentRsp agentRsp = HttpActionUtil.getObject(url, AgentRsp.class);
        if (null == agentRsp)
        {
            log.error("get ha ip from agent failed, node id: {}", tblHypervisorNode.getNodeId());
            return;
        }
        if (StrUtil.isBlank(agentRsp.getHaId()))
        {
            tblHypervisorNode.setBackupNodeId(tblHypervisorNode.getNodeId());
            tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            nodeService.updateById(tblHypervisorNode);
            return;
        }
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(HypervisorNode::getNodeId)
                .eq(HypervisorNode::getAgentId, agentRsp.getHaId())
                .ne(HypervisorNode::getPhaseStatus, REMOVED);
        if (0 == nodeService.count(queryWrapper))
        {
            log.info("get ha node failed, node id {} does not exists", tblHypervisorNode.getNodeId());
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//            return;
        }
        String backupNodeId = nodeService.getObj(queryWrapper, Object::toString);
        tblHypervisorNode.setBackupNodeId(backupNodeId);
        tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        nodeService.updateById(tblHypervisorNode);
    }

    private String getDeviceGroupId(GPURspFromAgent gpuRspFromAgent, String nodeId)
    {
//        String  deviceGroupId = isDeviceGroupIdExists(gpuRspFromAgent.getDeviceGroup());
//        if (!StrUtil.isBlank(deviceGroupId))
//        {
//            return deviceGroupId;
//        }
        PciDeviceGroup tblPciDeviceGroup = new PciDeviceGroup();
        tblPciDeviceGroup.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblPciDeviceGroup.setUpdateTime(tblPciDeviceGroup.getCreateTime());
        tblPciDeviceGroup.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATED);
        tblPciDeviceGroup.setDeviceGroupId(Utils.assignUUId());
        tblPciDeviceGroup.setNodeId(nodeId);
        tblPciDeviceGroup.setDeviceGroupIdFromAgent(gpuRspFromAgent.getDeviceGroup());
        boolean ok = pciDeviceGroupService.save(tblPciDeviceGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.ERROR);
        }
        return tblPciDeviceGroup.getDeviceGroupId();
    }

    private String isDeviceGroupIdExists(String deviceGroupIdFromAgent)
    {
        LambdaQueryWrapper<PciDeviceGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDeviceGroup::getDeviceGroupIdFromAgent, deviceGroupIdFromAgent)
                .ne(PciDeviceGroup::getPhaseStatus, REMOVED);
        if (pciDeviceGroupService.count(queryWrapper) > 0)
        {
            return pciDeviceGroupService.getOne(queryWrapper).getDeviceGroupId();
        }
        return null;
    }

    private GPURspFromAgent getGpuRspFromAgent(HypervisorNode tblNode, String gpuIdFromAgent)
    {
        String url = String.format("http://%s:%s%s/%s", tblNode.getManageIp(), computeConfig.getVmAgentPort(), ComputeUrl.V1_GPU_URL, gpuIdFromAgent);
        GPURspFromAgent gpuRspFromAgent = HttpActionUtil.getObject(url, GPURspFromAgent.class);
        log.info("url:{}, get result:{}", url, gpuRspFromAgent);
        if (!StrUtil.isBlank(gpuRspFromAgent.getReason()) && gpuRspFromAgent.getReason().contains(AgentConstant.NOT_EXIST))
        {
            gpuRspFromAgent.setPhase(AgentConstant.NOT_EXIST);
        }
        return gpuRspFromAgent;
    }
}
