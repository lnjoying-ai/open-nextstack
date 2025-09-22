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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.schema.entity.search.PageSearchCritical;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.TsQueryParameters;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.request.GpuPartitionActiveReq;
import com.lnjoying.vm.domain.backend.response.GpuPartitionActiveRsp;
import com.lnjoying.vm.domain.dto.response.*;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.PciDevice;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.mapper.AvailableGPUMapper;
import com.lnjoying.vm.mapper.DeviceInfoMapper;
import com.lnjoying.vm.mapper.HypervisorNodeAllocationMapper;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.PciDeviceGroupService;
import com.lnjoying.vm.service.PciDeviceService;
import com.lnjoying.vm.service.VmInstanceService;
import com.lnjoying.vm.service.biz.CombRpcSerice;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.lnjoying.vm.service.biz.PciDeviceServiceBiz;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.yaml.snakeyaml.util.UriEncoder;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class VmScheduler
{

    @Autowired
    CombRpcSerice combRpcSerice;

    @Autowired
    ComputeConfig computeConfig;

    @Autowired
    HypervisorNodeService hypervisorNodeService;

    @Autowired
    VmInstanceService vmInstanceService;

    @Resource
    AvailableGPUMapper availableGPUMapper;

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private PciDeviceGroupService pciDeviceGroupService;

    @Autowired
    private PciDeviceService pciDeviceService;

    @Autowired
    private PciDeviceServiceBiz pciDeviceServiceBiz;

    @Resource
    private HypervisorNodeAllocationMapper hypervisorNodeAllocationMapper;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    TransactionDefinition transactionDefinition;

    private String getBottomMemNode()
    {
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(HypervisorNode::getPhaseStatus, REMOVED);
        List<HypervisorNode> hypervisors = hypervisorNodeService.list(queryWrapper);
        List<String> hypervisorIds = hypervisors.stream().map(HypervisorNode::getNodeId).collect(Collectors.toList());
        int minSize = 5000;
        Random rand = new Random();
        String minHypervisorId = hypervisorIds.get(rand.nextInt(hypervisorIds.size()));
        for (HypervisorNode hypervisor : hypervisors)
        {
            LambdaQueryWrapper<VmInstance> vmQueryWrapper = new LambdaQueryWrapper<>();
            vmQueryWrapper.select(VmInstance::getFlavorId).eq(VmInstance::getNodeId, hypervisor.getNodeId())
                    .ne(VmInstance::getPhaseStatus, REMOVED);
            List<String> flavorIds = vmInstanceService.listObjs(vmQueryWrapper, Object::toString);
            Integer memCount = combRpcSerice.getFlavorService().getMemSizeTotalByFlavorIds(flavorIds);
            if (minSize > memCount)
            {
                minSize = memCount;
                minHypervisorId = hypervisor.getInstanceId();
            }
        }
        return minHypervisorId;
    }

    public List<String> getBottom5VmsNode()
    {
        List<HypervisorNodeVmsInfo> nodeVmsInfos = vmInstanceService.getNodeVmInfo(5);
        return nodeVmsInfos.stream().map(HypervisorNodeVmsInfo::getManageIp).collect(Collectors.toList());
    }

//    private List<String> checkStorageEnough(List<String> manageIps, TblFlavor tblFlavor)
//    {
////        TblFlavor tblFlavor = flavorRepository.getFlavorById(tblVmInstance.getFlavorId());
//        String url = computeConfig.getPrometheusServer()+ TsQueryParameters.API + "{availFileSystem}";
//        GetPrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(url, GetPrometheusRsp.class, TsQueryParameters.getAvailFileSystemSql(manageIps) );
//        List<String> newManageIps = new ArrayList<>();
//        Arrays.stream(getPrometheusRsp.getData().getResult()).forEach(
//                result -> {
//                    long availFs = Long.parseLong(result.getValue()[1]);
//                    String instance = result.getMetric().getInstance();
//                    String manageIp = instance.split(":")[0];
//                    if(availFs > tblFlavor.getRootDisk())
//                    {
//                        newManageIps.add(manageIp);
//                    }
//                });
//        return newManageIps;
//    }

    private List<String> checkMemAllocation(String flavorId)
    {
        PageSearchCritical pageSearchCritical = new PageSearchCritical();
        pageSearchCritical.setPageSize(5);
        pageSearchCritical.setPageNum(1);
        NodeAllocationInfosRsp rsp = pciDeviceServiceBiz.getResourceAllocation(pageSearchCritical, flavorId);
        if (0 == rsp.getTotalNum() || rsp.getNodeAllocationInfos().isEmpty())
        {
            return null;
        }
        return rsp.getNodeAllocationInfos().stream().map(HypervisorNodeAllocationInfo::getNodeId).collect(Collectors.toList());
    }

    private List<String> checkMemEnough(List<String> manageIps, int flavorMem)
    {
        String prometheusSql = TsQueryParameters.getAvailMemSql(manageIps);
        String url = computeConfig.getPrometheusServer() + TsQueryParameters.API + UriEncoder.encode(prometheusSql);
        URI uri;
        try
        {
            uri = new URI(url);
        }
        catch (URISyntaxException e)
        {
            log.error("new URI error:{}, url:{}", e.getMessage(), url);
            return null;
        }
        List<String> newManageIps = new ArrayList<>();
        PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(uri, PrometheusRsp.class);
        Arrays.stream(getPrometheusRsp.getData().getResult()).forEach(
                result ->
                {
                    long mem = Long.parseLong(result.getValue()[1]);
                    String instance = result.getMetric().getInstance();
                    String manageIp = instance.split(":")[0];
                    if (mem * 1024 > flavorMem)
                    {
                        newManageIps.add(manageIp);
                    }
                });

        return newManageIps;
    }

    private String getNodeIdFromManageIp(String manageIp)
    {
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNode::getManageIp, manageIp)
                .ne(HypervisorNode::getPhaseStatus, REMOVED);
        HypervisorNode hypervisorNode = hypervisorNodeService.getOne(queryWrapper);
        if (null == hypervisorNode) return null;
        return hypervisorNode.getNodeId();
    }

    public String getAvailableNodeId(VmInstance tblVmInstance, FlavorService.FlavorInfo flavorInfo)
    {
        List<String> nodeIds = checkMemAllocation(tblVmInstance.getFlavorId());
        log.info("checkMemAllocation nodeIds:{}", nodeIds);
//        List<String> manageIps = getBottom5VmsNode();
//        List<String> checkedIps = checkMemEnough(manageIps, flavorInfo.getMem());
        if (null == nodeIds || 0 == nodeIds.size())
        {
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "计算节点无可用资源",
                    String.format("vmInstanceId:%s, 规格: CPU 为:%d核, 内存为:%dG", tblVmInstance.getVmInstanceId(), flavorInfo.getCpu(), flavorInfo.getMem()), "调度失败");
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED);
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            vmInstanceService.updateById(tblVmInstance);
            return null;
        }
//        checkedIps = checkStorageEnough(checkedIps,tblFlavor);

        Random rand = new Random();
        return nodeIds.get(rand.nextInt(nodeIds.size()));
//        String manageIp = nodeIds.get(rand.nextInt(checkedIps.size()));
//        return getNodeIdFromManageIp(manageIp);
    }

    public boolean checkGpuNodeCpuMemEnough(String nodeId, int cpu, int mem)
    {
        LambdaQueryWrapper<HypervisorNodeAllocationInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNodeAllocationInfo::getNodePhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED);
        Page<HypervisorNodeAllocationInfo> page = new Page<>(1, 1);
        List<HypervisorNodeAllocationInfo> nodeAllocationInfos = hypervisorNodeAllocationMapper.selectNodeAllocationInfo(page, queryWrapper, 0);
        if (null == nodeAllocationInfos || 0 == nodeAllocationInfos.size())
        {
            return false;
        }
        HypervisorNodeAllocationInfo nodeAllocationInfo = nodeAllocationInfos.get(0);
        return nodeAllocationInfo.getMemTotal() - nodeAllocationInfo.getMemSum() - 2 > mem &&
                nodeAllocationInfo.getCpuLogCount() - nodeAllocationInfo.getCpuSum() - 1 >= cpu;

    }

    public Integer getIbCount(FlavorService.FlavorInfo flavorInfo)
    {
        int ibCount = 0;
        int configIbCount = (null == computeConfig.getIbCount() ? 0 : computeConfig.getIbCount());
        if (4 == configIbCount && needPartition(flavorInfo.getGpuName()))
        {
            switch (flavorInfo.getGpuCount())
            {
                case 2:
                    ibCount = 1;
                    break;
                case 4:
                    ibCount = 2;
                    break;
                case 8:
                    ibCount = 4;
                    break;
                case 1:
                    if (flavorInfo.getNeedIb()) ibCount = 1;
                    break;
            }
        }
        else if (8 == configIbCount && needPartition(flavorInfo.getGpuName()))
        {
            return flavorInfo.getGpuCount();
        }
        return ibCount;
    }

    public String getAvailableGpuNodeId(VmInstance tblVmInstance, FlavorService.FlavorInfo flavorInfo)
    {
        //需要增加内存是否充足的判断
        TransactionStatus transactionStatus = null;
        transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        if (flavorInfo.getGpuCount() == 0)
        {
            return null;
        }
        Integer ibCount = getIbCount(flavorInfo);
        List<AvailableGPURsp> availableGPUs = availableGPUMapper.selectAvailableGPURspByName(flavorInfo.getGpuName(), ibCount);
        if (null == availableGPUs || 0 == availableGPUs.size())
        {
            setInstanceFailed(tblVmInstance, flavorInfo, transactionStatus);
            return null;
        }
        //基于availableGPUs的gpuCount排序,升序
        availableGPUs.sort(Comparator.comparing(AvailableGPURsp::getGpuCount));
        boolean needIb = flavorInfo.getNeedIb() != null && flavorInfo.getNeedIb();
        for (AvailableGPURsp gpuRsp : availableGPUs)
        {
            if (gpuRsp.getGpuCount() >= flavorInfo.getGpuCount())
            {
                if (needPartition(flavorInfo.getGpuName()))
                {
                    String nodeId = checkGpuPartition(gpuRsp.getNodeId(), flavorInfo.getGpuCount(), tblVmInstance.getVmInstanceId(), needIb);
                    if (StrUtil.isBlank(nodeId))
                    {
                        continue;
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    return nodeId;
                }
                return setPciDevices(tblVmInstance, flavorInfo, gpuRsp.getNodeId(), transactionStatus);
            }
        }

        setInstanceFailed(tblVmInstance, flavorInfo, transactionStatus);
        return null;
    }


    public void setInstanceFailed(VmInstance tblVmInstance, FlavorService.FlavorInfo flavorInfo, TransactionStatus transactionStatus)
    {
        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "计算节点无可用GPU或IB卡",
                String.format("虚拟机: id: %s, 规格：GPU 为:%s, %d个", tblVmInstance.getVmInstanceId(), flavorInfo.getGpuName(), flavorInfo.getGpuCount()), "调度失败");
        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED);
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        vmInstanceService.updateById(tblVmInstance);
        dataSourceTransactionManager.commit(transactionStatus);
    }

    public String setPciDevicesByNodeId(VmInstance tblVmInstance, FlavorService.FlavorInfo flavorInfo, String nodeId)
    {
        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId())
                .eq(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_INIT_CREATE);
        if (flavorInfo.getGpuCount() == pciDeviceService.count(queryWrapper))
        {
            return tblVmInstance.getVmInstanceId();
        }
        if (needPartition(flavorInfo.getGpuName()))
        {
            checkGpuPartition(nodeId, flavorInfo.getGpuCount(), tblVmInstance.getVmInstanceId(), flavorInfo.getNeedIb());
            return tblVmInstance.getVmInstanceId();
        }
        TransactionStatus transactionStatus = null;
        transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        if (null == setPciDevices(tblVmInstance, flavorInfo, nodeId, transactionStatus))
        {
            transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            setInstanceFailed(tblVmInstance, flavorInfo, transactionStatus);
            return null;
        }
        return tblVmInstance.getVmInstanceId();
    }

    public String setPciDevices(VmInstance tblVmInstance, FlavorService.FlavorInfo flavorInfo, String nodeId, TransactionStatus transactionStatus)
    {
        try
        {
            int count = flavorInfo.getGpuCount();
            String gpuName = flavorInfo.getGpuName();
            LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    .isNull(PciDevice::getVmInstanceId)
                    .eq(PciDevice::getName, gpuName)
                    .eq(PciDevice::getNodeId, nodeId)
                    .ne(PciDevice::getPhaseStatus, REMOVED);
            List<PciDevice> pciDevices = pciDeviceService.list(queryWrapper);
            if (null == pciDevices || pciDevices.size() < count)
            {
                log.error("gpus are not enough, nodeId: {}, flavorInfo: {}", nodeId, flavorInfo);
                dataSourceTransactionManager.rollback(transactionStatus);
                return null;
            }
//        List<String>  deviceGroupIds = pciDeviceGroupService.listObjs(queryWrapper, Object::toString);
            long gpuCount = 0;
            for (int i = 0; i < pciDevices.size(); i++)
            {
                if (gpuCount == count) break;
                PciDevice tblPciDevice = pciDevices.get(i);
                tblPciDevice.setVmInstanceId(tblVmInstance.getVmInstanceId());
                tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_INIT_CREATE);
                boolean dbOk = pciDeviceService.updateById(tblPciDevice);
                if (!dbOk)
                {
                    continue;
                }
                gpuCount += 1;
            }

            if (gpuCount < count)
            {
                log.error("gpus are not enough");
                dataSourceTransactionManager.rollback(transactionStatus);
                return null;
            }
            dataSourceTransactionManager.commit(transactionStatus);
            return nodeId;
        }
        catch (Exception e)
        {
            log.error("getAvailableGpuNodeIdError :setPciDevices error:{}", e.getMessage());
            dataSourceTransactionManager.rollback(transactionStatus);
            return null;
        }
    }

    public boolean needPartition(String gpuName)
    {
        return gpuName.contains("H100") || gpuName.contains("H800") || gpuName.contains("A100") || gpuName.contains("A800");
    }

    public String checkGpuPartition(String nodeId, Integer gpuCount, String vmInstanceId, Boolean needIb)
    {
        try
        {
            HypervisorNode node = hypervisorNodeService.getById(nodeId);
            if (null == node || REMOVED == node.getPhaseStatus()) return null;
            String managerIp = node.getManageIp();
            Integer vmAgentPort = computeConfig.getVmAgentPort();
            if (null == vmAgentPort || 0 == vmAgentPort)
            {
                vmAgentPort = ComputeUrl.VM_AGENT_PORT;
            }
            String url = "http://" + managerIp + ":" + vmAgentPort + ComputeUrl.V1_GPU_URL + "/active";
            GpuPartitionActiveReq req = new GpuPartitionActiveReq();
            req.setNum(gpuCount);
            req.setNeedIb(needIb);
            String resultId = activeGpuPartition(req, url, nodeId, vmInstanceId, VmInstanceStatus.DEVICE_INIT_CREATE);
            if (vmInstanceId.equals(resultId))
            {
                return nodeId;
            }
            return null;
        }
        catch (Exception e)
        {
            log.error("checkGpuPartition error:{}", e.getMessage());
            return null;
        }
    }

    public String activeOneGpuPartition(String nodeId, String vmInstanceId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            return null;
        }
        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDevice::getNodeId, nodeId)
                .isNull(PciDevice::getVmInstanceId)
                .isNotNull(PciDevice::getPartitionId);
        List<PciDevice> pciDevices = pciDeviceService.list(queryWrapper);
        if (null == pciDevices || pciDevices.isEmpty())
        {
            return null;
        }
        PciDevice tblPciDevice = pciDevices.get(0);
        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_INIT_CREATE);
        tblPciDevice.setVmInstanceId(vmInstanceId);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = pciDeviceService.updateById(tblPciDevice);
        if (ok) return vmInstanceId;
        return null;
    }

    public String activeGpuPartition(GpuPartitionActiveReq req, String url, String nodeId, String vmInstanceId, Integer phaseStatus)
    {
        String putArgs = JsonUtil.objectToJson(req);
        String result = HttpActionUtil.put(url, putArgs);
        if (null == result)
        {
            log.error("checkGpuPartition error, url: {}, putArgs: {}", url, putArgs);
        }

        GpuPartitionActiveRsp rsp = JsonUtil.jsonToPojo(result, GpuPartitionActiveRsp.class);
        if (null == rsp)
        {
            log.error("checkGpuPartition error, url: {}, putArgs: {}", url, putArgs);
            return null;
        }
        if (null == rsp.getGpuIds() || rsp.getGpuIds().isEmpty())
        {
            if (Objects.equals(req.getNum(), "1"))
            {
                return activeOneGpuPartition(nodeId, vmInstanceId);
            }
            log.error("checkGpuPartition error, url: {}, putArgs: {}, gpuIds: {}", url, putArgs, rsp.getGpuIds());
            return null;
        }
        if (AgentConstant.OK.equals(rsp.getStatus()))
        {
            LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PciDevice::getVmInstanceId, vmInstanceId)
                    .ne(PciDevice::getPhaseStatus, REMOVED);
            List<PciDevice> beforePciDevices = pciDeviceService.list(queryWrapper);
            Set<String> beforeAgentGpuIds = beforePciDevices.stream().map(PciDevice::getDeviceIdFromAgent).collect(Collectors.toSet());
            // 更新PCI设备状态
            LambdaUpdateWrapper<PciDevice> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(PciDevice::getDeviceIdFromAgent, rsp.getGpuIds())
                    .ne(PciDevice::getPhaseStatus, REMOVED);
            List<PciDevice> pciDevices = pciDeviceService.list(updateWrapper);
            if (null == pciDevices || pciDevices.isEmpty())
            {
                log.error("checkGpuPartition error, url: {}, putArgs: {}, gpuIds: {}", url, putArgs, rsp.getGpuIds());
                return null;
            }
            PciDevice tblPciDevice = new PciDevice();
            tblPciDevice.setPhaseStatus(phaseStatus);
            tblPciDevice.setVmInstanceId(vmInstanceId);
            tblPciDevice.setPartitionId(rsp.getPartitionId());
            tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = pciDeviceService.update(tblPciDevice, updateWrapper);
            if (ok)
            {
                log.info("checkGpuPartition success, url: {}, putArgs: {}, gpuIds: {}, partitionId:{}", url, putArgs, rsp.getGpuIds(), rsp.getPartitionId());
                //list 转换为set
                Set<String> afterAgentGpuIds = pciDevices.stream().map(PciDevice::getDeviceIdFromAgent).collect(Collectors.toSet());
                beforeAgentGpuIds.removeAll(afterAgentGpuIds);
                log.info("beforeAgentGpuIds:{}, vmInstanceId:{}", beforeAgentGpuIds, vmInstanceId);
                if (beforeAgentGpuIds.isEmpty())
                {
                    return vmInstanceId;
                }
                updateWrapper.clear();
                updateWrapper.eq(PciDevice::getVmInstanceId, vmInstanceId)
                        .in(PciDevice::getDeviceIdFromAgent, beforeAgentGpuIds);
                tblPciDevice = new PciDevice();
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHING);
                tblPciDevice.setVmInstanceId(vmInstanceId);
                tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                pciDeviceService.update(tblPciDevice, updateWrapper);
                return vmInstanceId;
            }

        }
        return null;
    }
}
