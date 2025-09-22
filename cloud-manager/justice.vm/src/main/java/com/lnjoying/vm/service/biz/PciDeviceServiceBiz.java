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

package com.lnjoying.vm.service.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.entity.search.PageSearchCritical;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.dto.response.*;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.PciDevice;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.entity.VmSnap;
import com.lnjoying.vm.entity.search.PciDeviceSearchCritical;
import com.lnjoying.vm.mapper.*;
import com.lnjoying.vm.processor.VmScheduler;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.PciDeviceService;
import com.lnjoying.vm.service.VmInstanceService;
import com.lnjoying.vm.service.VmSnapService;
import com.micro.core.common.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service
public class PciDeviceServiceBiz
{
    @Autowired
    private PciDeviceService pciDeviceService;


    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private HypervisorNodeService hypervisorNodeService;

    @Resource
    private DeviceDetailInfoMapper deviceDetailInfoMapper;

    @Resource
    private HypervisorNodeInfoMapper hypervisorNodeInfoMapper;

    @Resource
    private HypervisorNodeAllocationMapper hypervisorNodeAllocationMapper;

    @Resource
    private AvailableGPUMapper availableGPUMapper;

    @Resource
    private CombRpcSerice combRpcSerice;

    @Resource
    private PciDeviceMapper pciDeviceMapper;

    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    @Resource
    private VmInstanceMapper vmInstanceMapper;

    @Autowired
    private VmScheduler vmScheduler;

    @Autowired
    private VmSnapService vmSnapService;

    @Autowired
    private ComputeConfig computeConfig;

    public List<PciDeviceDetailInfo> getPciDevices(PciDeviceSearchCritical searchCritical, String nodeId)
    {
        LambdaQueryWrapper<PciDeviceDetailInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(PciDeviceDetailInfo::getPhaseStatus, REMOVED)
                .eq(PciDeviceDetailInfo::getNodeId, nodeId);
        if (!StrUtil.isBlank(searchCritical.getPciDeviceName()))
        {
            queryWrapper.eq(PciDeviceDetailInfo::getPciDeviceName, searchCritical.getPciDeviceName());
        }

        if (0 == deviceDetailInfoMapper.countPCIDevices(queryWrapper))
        {
            return new ArrayList<>();
        }
        queryWrapper.orderByDesc(PciDeviceDetailInfo::getNodeId)
                .orderByDesc(PciDeviceDetailInfo::getCreateTime);
        Page<PciDeviceDetailInfo> page = new Page<>(searchCritical.getPageNum(), searchCritical.getPageSize());

        List<PciDeviceDetailInfo> deviceDetailInfos = deviceDetailInfoMapper.selectPCIDevices(page, queryWrapper);
        for (PciDeviceDetailInfo deviceDetailInfo : deviceDetailInfos)
        {
            if (VmInstanceStatus.DEVICE_DETACHED == deviceDetailInfo.getPhaseStatus())
            {
                deviceDetailInfo.setVmInstanceName(null);
                deviceDetailInfo.setVmInstanceId(null);
            }
        }

        return deviceDetailInfos;
    }

    public PciDeviceDetailInfo getPciDeviceDetailInfo(String pciDeviceId)
    {
        PciDevice pciDevice = pciDeviceService.getById(pciDeviceId);
        if (null == pciDevice || REMOVED == pciDevice.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_NOT_EXIST, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<PciDeviceDetailInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDeviceDetailInfo::getDeviceId, pciDeviceId)
                .ne(PciDeviceDetailInfo::getPhaseStatus, REMOVED);
        Page<PciDeviceDetailInfo> page = new Page<>(1, 1);
        return deviceDetailInfoMapper.selectPCIDevices(page, queryWrapper).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public PciDeviceBaseRsp attachPciDevice(String vmId, String pciDeviceId, String userId)
    {
//        if (!isVmInstancePowerOff(vmId))
//        {
//            throw new WebSystemException(ErrorCode.INSTANCE_SHOULD_BE_POWER_OFF, ErrorLevel.INFO);
//        }
        PciDevice tblPciDevice = pciDeviceService.getById(pciDeviceId);
        if (null == tblPciDevice || REMOVED == tblPciDevice.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (VmInstanceStatus.DEVICE_DETACHED != tblPciDevice.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_NOT_DETACHED, ErrorLevel.INFO);
        }
        String attachedVmId = tblPciDevice.getVmInstanceId();
        if (!StrUtil.isBlank(attachedVmId))
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_GROUP_ALREADY_ATTACHED, ErrorLevel.INFO);
        }
        vmInstanceOwnByUser(vmId, userId);
        tblPciDevice.setVmInstanceId(vmId);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        pciDeviceService.updateById(tblPciDevice);
        tblPciDevice.setUserId(userId);
        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACHING);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (!pciDeviceService.updateById(tblPciDevice))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        PciDeviceBaseRsp pciDeviceBaseRsp = new PciDeviceBaseRsp();
        pciDeviceBaseRsp.setPciDeviceId(tblPciDevice.getDeviceId());
        return pciDeviceBaseRsp;
    }

    public PciDeviceBaseRsp detachPciDevice(String pciDeviceId, String userId)
    {
        PciDevice tblPciDevice = pciDeviceService.getById(pciDeviceId);
        if (null == tblPciDevice || REMOVED == tblPciDevice.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_NOT_EXIST, ErrorLevel.INFO);
        }
        Integer phaseStatus = deviceDetailInfoMapper.selectVmInstancePhaseStatusByDeviceId(tblPciDevice.getDeviceId());
        if (null == phaseStatus)
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_GROUP_NOT_EXIST, ErrorLevel.INFO);
        }
//        if (VmInstanceStatus.INSTANCE_POWEROFF != phaseStatus)
//        {
//            throw new WebSystemException(ErrorCode.INSTANCE_SHOULD_BE_POWER_OFF, ErrorLevel.INFO);
//        }

        if (!StrUtil.isBlank(userId) && !userId.equals(tblPciDevice.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        if (VmInstanceStatus.DEVICE_ATTACHED != tblPciDevice.getPhaseStatus() && VmInstanceStatus.DEVICE_DETACH_FAILED != tblPciDevice.getPhaseStatus()
                && VmInstanceStatus.DEVICE_ATTACH_FAILED != tblPciDevice.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_NOT_ATTACHED, ErrorLevel.INFO);
        }

        String vmInstanceId = deviceDetailInfoMapper.selectVmInstanceIdByDeviceId(tblPciDevice.getDeviceId());
        if (StrUtil.isBlank(vmInstanceId))
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VmSnap> vmSnapLambdaQueryWrapper = new LambdaQueryWrapper<>();
        vmSnapLambdaQueryWrapper.eq(VmSnap::getVmInstanceId, vmInstanceId)
                .ne(VmSnap::getPhaseStatus, REMOVED);
        if (vmSnapService.count(vmSnapLambdaQueryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.VM_HAS_SNAPS, ErrorLevel.INFO);
        }
        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHING);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (!pciDeviceService.updateById(tblPciDevice))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        PciDeviceBaseRsp pciDeviceBaseRsp = new PciDeviceBaseRsp();
        pciDeviceBaseRsp.setPciDeviceId(tblPciDevice.getDeviceId());
        return pciDeviceBaseRsp;
    }

    public List<HypervisorNodeInfo> getAvailableNode(PageSearchCritical searchCritical, String vmId, String userId, boolean isGpu)
    {
        if (!StrUtil.isBlank(userId) && !StrUtil.isBlank(vmId))
        {
            vmInstanceOwnByUser(vmId, userId);
        }
        if (!isGpu)
        {
            return getCustomAvailableNode(searchCritical, vmId);
        }
        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(PciDevice::getDeviceId)
                .isNull(PciDevice::getVmInstanceId);
//        if (!StrUtil.isBlank(vmId))
//        {
//                queryWrapper.or()
//                        .eq(PciDeviceGroup::getVmInstanceId, vmId)
//                        .ne(PciDeviceGroup::getPhaseStatus, REMOVED);
//        }
        List<String> deviceIds = pciDeviceService.listObjs(queryWrapper, Object::toString);
        if (0 == deviceIds.size())
        {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<HypervisorNodeInfo> nodeInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        nodeInfoLambdaQueryWrapper.in(HypervisorNodeInfo::getPciDeviceGroupId, deviceGroupIds);
        nodeInfoLambdaQueryWrapper.eq(HypervisorNodeInfo::getPciDevicePhaseStatus, VmInstanceStatus.DEVICE_DETACHED)
                .ne(HypervisorNodeInfo::getNodePhaseStatus, REMOVED);
//                .eq(HypervisorNodeInfo::getPciDeviceType, "GPU");
        Page<HypervisorNodeInfo> page = new Page<>(searchCritical.getPageNum(), searchCritical.getPageSize());
        //Where tbl_pci_device.phase_status = 87 and tbl_hypervisor_node.phase_status <> -1 and tbl_pci_device.type='GPU'
        List<HypervisorNodeInfo> hypervisorNodeInfos = hypervisorNodeInfoMapper.selectGpuNodeInfo(page, nodeInfoLambdaQueryWrapper);

        if (hypervisorNodeInfos.isEmpty()) return hypervisorNodeInfos;
        for (HypervisorNodeInfo nodeInfo : hypervisorNodeInfos)
        {
            HypervisorNodeAllocationInfo allocationInfo = resourceAllocation(nodeInfo.getNodeId());
            if (null != allocationInfo)
            {
                if (null != allocationInfo.getCpuSum())
                {
                    nodeInfo.setCpuAllocation(allocationInfo.getCpuSum());
                }
                else
                {
                    nodeInfo.setCpuAllocation(0);
                }
                if (null != allocationInfo.getMemSum())
                {
                    nodeInfo.setMemAllocation(allocationInfo.getMemSum());
                }
                else
                {
                    nodeInfo.setMemAllocation(0);
                }
            }
        }
        return hypervisorNodeInfos;
    }

    // 获取非GPU设备的节点
    public List<HypervisorNodeInfo> getCustomAvailableNode(PageSearchCritical searchCritical, String userId)
    {
        LambdaQueryWrapper<HypervisorNodeInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(HypervisorNodeInfo::getNodePhaseStatus, REMOVED);
        Page<HypervisorNodeInfo> page = new Page<>(searchCritical.getPageNum(), searchCritical.getPageSize());
        return hypervisorNodeInfoMapper.selectCustomNodeInfo(page, queryWrapper);
    }

    //获取资源分配情况（CPU/内存）
    public HypervisorNodeAllocationInfo resourceAllocation(String hypervisorNodeId)
    {
        LambdaQueryWrapper<HypervisorNodeAllocationInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNodeAllocationInfo::getNodeId, hypervisorNodeId)
                .ne(HypervisorNodeAllocationInfo::getNodePhaseStatus, REMOVED);
//                .ne(HypervisorNodeAllocationInfo::getVmInstancePhaseStatus, REMOVED);
        Page<HypervisorNodeAllocationInfo> page = new Page<>(1, 1);
        List<HypervisorNodeAllocationInfo> hypervisorNodeAllocationInfos = hypervisorNodeAllocationMapper.selectNodeAllocationInfo(page, queryWrapper, computeConfig.getIbCount());
        if (hypervisorNodeAllocationInfos.isEmpty())
        {
            return null;
        }
        hypervisorNodeAllocationInfos = hypervisorNodeAllocationInfos.stream().peek(nodeInfo ->
        {
            nodeInfo.setMemSum(nodeInfo.getMemSum() - nodeInfo.getMemRecycle());
            nodeInfo.setCpuSum(nodeInfo.getCpuSum() - nodeInfo.getCpuRecycle());
        }).collect(Collectors.toList());

        LambdaQueryWrapper<PciDevice> pciDeviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pciDeviceLambdaQueryWrapper.eq(PciDevice::getNodeId, hypervisorNodeId)
                .ne(PciDevice::getPhaseStatus, REMOVED);

        List<PciDevice> pciDevices = pciDeviceService.list(pciDeviceLambdaQueryWrapper);

        hypervisorNodeAllocationInfos.get(0).setGpuTotal((long) pciDevices.size());
        if(pciDevices.isEmpty())
        {
            hypervisorNodeAllocationInfos.get(0).setGpuName("");
            hypervisorNodeAllocationInfos.get(0).setGpuCount(0);
            return hypervisorNodeAllocationInfos.get(0);
        }
        hypervisorNodeAllocationInfos.get(0).setGpuName(pciDevices.get(0).getName());
        long availableGpuCount = pciDevices.stream().filter(pciDevice -> null == pciDevice.getVmInstanceId()).count();
        hypervisorNodeAllocationInfos.get(0).setGpuCount((int) availableGpuCount);
        return hypervisorNodeAllocationInfos.get(0);
    }

    // 获取全部计算节点的资源分配情况(CPU/内存)
    public NodeAllocationInfosRsp getResourceAllocation(PageSearchCritical searchCritical, String flavorId)
    {
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(flavorId);
        NodeAllocationInfosRsp nodeAllocationInfosRsp = new NodeAllocationInfosRsp();
        if (null == flavorInfo)
        {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }
        Map<String, AvailableGPURsp> gpuRspMap;
        long gpuTotal = pciDeviceMapper.selectPciDeviceCount();
        Page<HypervisorNodeAllocationInfo> page = new Page<>(searchCritical.getPageNum(), searchCritical.getPageSize());
        // 通用计算型
        if (null == flavorInfo.getGpuCount() || 0 == flavorInfo.getGpuCount())
        {
            LambdaQueryWrapper<HypervisorNode> nodeQueryWrapper = new LambdaQueryWrapper<>();
            nodeQueryWrapper.eq(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
                    .eq(HypervisorNode::getErrorCount, 0);
            long count = hypervisorNodeService.count(nodeQueryWrapper);
            nodeAllocationInfosRsp.setTotalNum(count);
            if (0 == count)
            {
                return nodeAllocationInfosRsp;
            }
            LambdaQueryWrapper<HypervisorNodeAllocationInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HypervisorNodeAllocationInfo::getNodePhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED);
            List<HypervisorNodeAllocationInfo> nodeAllocationInfos = hypervisorNodeAllocationMapper.selectNodeAllocationInfo(page, queryWrapper, computeConfig.getIbCount());
//                    .ne(HypervisorNodeAllocationInfo::getVmInstancePhaseStatus, REMOVED);
            List<HypervisorNodeAllocationInfo> filterNodeAllocationInfos = nodeAllocationInfos.stream().filter(nodeAllocationInfo ->
//                    nodeAllocationInfo.getCpuLogCount()>nodeAllocationInfo.getCpuSum() &&
                    nodeAllocationInfo.getMemTotal() - nodeAllocationInfo.getMemSum() + nodeAllocationInfo.getMemRecycle() > flavorInfo.getMem()).collect(Collectors.toList());
            nodeAllocationInfosRsp.setNodeAllocationInfos(filterNodeAllocationInfos);
            if (filterNodeAllocationInfos.isEmpty())
            {
                nodeAllocationInfosRsp.setTotalNum(0);
                return nodeAllocationInfosRsp;
            }
            LambdaQueryWrapper<AvailableGPURsp> gpuQueryWrapper = new LambdaQueryWrapper<>();
            gpuQueryWrapper.in(AvailableGPURsp::getNodeId, nodeAllocationInfosRsp.getNodeAllocationInfos().stream().map(HypervisorNodeAllocationInfo::getNodeId).collect(Collectors.toList()));
            List<AvailableGPURsp> availableGPURsps = availableGPUMapper.selectAvailableGPURsp(gpuQueryWrapper);
            if (availableGPURsps.isEmpty())
            {
                return nodeAllocationInfosRsp;
            }
            gpuRspMap = availableGPURsps.stream().collect(Collectors.toMap(AvailableGPURsp::getNodeId, Function.identity()));
            for (HypervisorNodeAllocationInfo allocationInfo : nodeAllocationInfosRsp.getNodeAllocationInfos())
            {
                allocationInfo.setMemSum(allocationInfo.getMemSum() - allocationInfo.getMemRecycle());
                allocationInfo.setCpuSum(allocationInfo.getCpuSum() - allocationInfo.getCpuRecycle());
                if (gpuRspMap.containsKey(allocationInfo.getNodeId()))
                {
                    allocationInfo.setGpuTotal(gpuTotal);
                    allocationInfo.setGpuCount(gpuRspMap.get(allocationInfo.getNodeId()).getGpuCount());
                    allocationInfo.setGpuName(gpuRspMap.get(allocationInfo.getNodeId()).getGpuName());
                }
            }
            return nodeAllocationInfosRsp;
        }
        // GPU计算型,先获取可用的GPU，再通过node_id，获取资源分配情况
        long count = availableGPUMapper.selectTotalAvailableGPURspByNameAndCount(flavorInfo.getGpuName(), flavorInfo.getGpuCount());
        nodeAllocationInfosRsp.setTotalNum(count);
        if (0 == count)
        {
            return nodeAllocationInfosRsp;
        }
        int index = (searchCritical.getPageNum() - 1) * searchCritical.getPageSize();
        Integer ibCount = vmScheduler.getIbCount(flavorInfo);
        List<AvailableGPURsp> availableGPUs = availableGPUMapper.selectAvailableGPURspByNameAndCount(flavorInfo.getGpuName(), flavorInfo.getGpuCount(), index, searchCritical.getPageSize(), ibCount);
        if (availableGPUs.isEmpty())
        {
            return nodeAllocationInfosRsp;
        }
        // key:node_id, value:AvailableGPURsp
        gpuRspMap = new HashMap<>();
        for (AvailableGPURsp availableGPURsp : availableGPUs)
        {
            gpuRspMap.put(availableGPURsp.getNodeId(), availableGPURsp);
        }

        LambdaQueryWrapper<HypervisorNodeAllocationInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNodeAllocationInfo::getNodePhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
//                .ne(HypervisorNodeAllocationInfo::getVmInstancePhaseStatus, REMOVED)
                .in(HypervisorNodeAllocationInfo::getNodeId, availableGPUs.stream().map(AvailableGPURsp::getNodeId).collect(Collectors.toList()));
        nodeAllocationInfosRsp.setNodeAllocationInfos(hypervisorNodeAllocationMapper.selectNodeAllocationInfo(page, queryWrapper, computeConfig.getIbCount()));

        nodeAllocationInfosRsp.getNodeAllocationInfos().forEach(nodeAllocationInfo ->
        {
            nodeAllocationInfo.setMemSum(nodeAllocationInfo.getMemSum() - nodeAllocationInfo.getMemRecycle());
            nodeAllocationInfo.setCpuSum(nodeAllocationInfo.getCpuSum() - nodeAllocationInfo.getCpuRecycle());
            nodeAllocationInfo.setGpuTotal(gpuTotal);
            nodeAllocationInfo.setGpuName(gpuRspMap.get(nodeAllocationInfo.getNodeId()).getGpuName());
            nodeAllocationInfo.setGpuCount(gpuRspMap.get(nodeAllocationInfo.getNodeId()).getGpuCount());
        });
        return nodeAllocationInfosRsp;
    }


    public List<PciDeviceInfo> getAvailableDeviceInfos(PageSearchCritical searchCritical, @NotBlank String nodeId, String vmId, String userId)
    {
        if (!StrUtil.isBlank(vmId) && !StrUtil.isBlank(userId))
        {
            vmInstanceOwnByUser(vmId, userId);
        }
        return getAvailableDeviceInfos(searchCritical, nodeId, vmId);
    }

    public List<PciDeviceInfo> getAvailableDeviceInfos(PageSearchCritical searchCritical, String nodeId, String vmId)
    {
        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_DETACHED)
                .isNull(PciDevice::getVmInstanceId)
                .eq(PciDevice::getNodeId, nodeId)
                .ne(PciDevice::getPhaseStatus, REMOVED);


        long pciDeviceCount = pciDeviceService.count(queryWrapper);
        if (0 == pciDeviceCount)
        {
            return new ArrayList<>();
        }
        Page<PciDevice> page = new Page<>(searchCritical.getPageNum(), searchCritical.getPageSize());
        List<PciDevice> pciDevices = pciDeviceService.page(page, queryWrapper).getRecords();
        // 用stream替代for
        List<PciDeviceInfo> pciDeviceInfos = pciDevices.stream().map(pciDevice ->
        {
            PciDeviceInfo pciDeviceInfo = new PciDeviceInfo();
            pciDeviceInfo.setPciDevice(pciDevice);
            return pciDeviceInfo;
        }).collect(Collectors.toList());
        return pciDeviceInfos;
    }

    private void vmInstanceOwnByUser(String vmId, String userId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(userId) && !userId.equals(tblVmInstance.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
    }

    private Boolean isVmInstancePowerOff(String vmId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        return tblVmInstance.getPhaseStatus() == VmInstanceStatus.INSTANCE_POWEROFF;
    }
}
