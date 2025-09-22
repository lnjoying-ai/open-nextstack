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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.HypervisorNodeAddReq;
import com.lnjoying.vm.domain.dto.response.*;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.PciDeviceGroup;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.entity.search.HypervisorNodeSearchCritical;
import com.lnjoying.vm.mapper.AvailableGPUMapper;
import com.lnjoying.vm.mapper.HypervisorNodeAllocationMapper;
import com.lnjoying.vm.mapper.PciDeviceMapper;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.PciDeviceGroupService;
import com.lnjoying.vm.service.PciDeviceService;
import com.lnjoying.vm.service.VmInstanceService;
import com.micro.core.common.Utils;
import com.micro.core.utils.AesCryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Slf4j
@Service
public class HypervisorNodeServiceBiz
{

    @Autowired
    private HypervisorNodeService hypervisorNodeService;

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private PciDeviceGroupService pciDeviceGroupService;

    @Resource
    private HypervisorNodeAllocationMapper hypervisorNodeAllocationMapper;

    @Resource
    private AvailableGPUMapper availableGPUMapper;

    @Resource
    private ComputeConfig computeConfig;

    @Autowired
    private PciDeviceService pciDeviceService;

    @Resource
    private PciDeviceMapper pciDeviceMapper;

    //add node
    @Transactional(rollbackFor = Exception.class)
    public HypervisorNodeBaseRsp addHypervisorNode(HypervisorNodeAddReq addHypervisorNodeReq) throws WebSystemException
    {

        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(HypervisorNode::getPhaseStatus, REMOVED)
                .eq(HypervisorNode::getManageIp, addHypervisorNodeReq.getManageIp());
        if (hypervisorNodeService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.ERROR);
        }
        HypervisorNode tblHypervisorNode = new HypervisorNode();
        tblHypervisorNode.setNodeId(Utils.assignUUId());
        tblHypervisorNode.setName(addHypervisorNodeReq.getName());
        tblHypervisorNode.setManageIp(addHypervisorNodeReq.getManageIp());
        tblHypervisorNode.setHostName(addHypervisorNodeReq.getHostname());
        tblHypervisorNode.setDescription(addHypervisorNodeReq.getDescription());
        tblHypervisorNode.setSysUsername(addHypervisorNodeReq.getSysUsername());
        tblHypervisorNode.setSysPassword(AesCryptoUtils.encryptHex(addHypervisorNodeReq.getSysPassword()));
        tblHypervisorNode.setPubkeyId(addHypervisorNodeReq.getPubkeyId());
        tblHypervisorNode.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblHypervisorNode.setUpdateTime(tblHypervisorNode.getCreateTime());
        tblHypervisorNode.setPhaseStatus(VmInstanceStatus.HYPERVISOR_NODE_CHECKING);
        tblHypervisorNode.setErrorCount(0);
        boolean ok = hypervisorNodeService.save(tblHypervisorNode);
        if (!ok)
        {
            log.info("add hypervisor node failed");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HypervisorNodeBaseRsp hypervisorNodeBaseRsp = new HypervisorNodeBaseRsp();
        hypervisorNodeBaseRsp.setNodeId(tblHypervisorNode.getNodeId());
        return hypervisorNodeBaseRsp;
    }

    public NodeAllocationInfosRsp getHypervisorNodes(HypervisorNodeSearchCritical critical) throws WebSystemException
    {
        NodeAllocationInfosRsp nodeAllocationInfosRsp = new NodeAllocationInfosRsp();
        LambdaQueryWrapper<HypervisorNodeAllocationInfo> queryWrapper = new LambdaQueryWrapper<>();

//        HypervisorNodesRsp getHypervisorNodesRsp = new HypervisorNodesRsp();
        LambdaQueryWrapper<HypervisorNode> nodeQueryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(HypervisorNodeAllocationInfo::getNodePhaseStatus, REMOVED);
        if (null != critical.getNodeId() && !critical.getNodeId().isEmpty())
        {
            queryWrapper.eq(HypervisorNodeAllocationInfo::getNodeId, critical.getNodeId());
            nodeQueryWrapper.eq(HypervisorNode::getNodeId, critical.getNodeId());
        }
        else if (null != critical.getName() && !critical.getName().isEmpty())
        {
            queryWrapper.like(HypervisorNodeAllocationInfo::getName, critical.getName());
            nodeQueryWrapper.like(HypervisorNode::getName, critical.getName());
        }
        if (null != critical.getIsHealthy() && critical.getIsHealthy())
        {
            queryWrapper.eq(HypervisorNodeAllocationInfo::getNodePhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
                    .eq(HypervisorNodeAllocationInfo::getErrorCount, 0);
            nodeQueryWrapper.eq(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
                    .eq(HypervisorNode::getErrorCount, 0);
        }
        long totalNum = hypervisorNodeService.count(nodeQueryWrapper);
        nodeAllocationInfosRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return nodeAllocationInfosRsp;
        }

        queryWrapper.orderByAsc(HypervisorNodeAllocationInfo::getCreateTime);
        //query with page number and page size
//        int begin = ((critical.getPageNum() - 1) * critical.getPageSize());
        Page<HypervisorNodeAllocationInfo> page = new Page<>(critical.getPageNum(), critical.getPageSize());
        List<HypervisorNodeAllocationInfo> nodeInfos = hypervisorNodeAllocationMapper.selectNodeAllocationInfo(page, queryWrapper, computeConfig.getIbCount());
//        List<HypervisorNode> tblHypervisorNodes = nodePage.getRecords();

//        getHypervisorNodesRsp.setHypervisorNodes(tblHypervisorNodes.stream().map(tblHypervisorNode -> {
//            HypervisorNodesRsp.HypervisorNodeRsp node = new HypervisorNodesRsp.HypervisorNodeRsp();
//            node.setHypervisorNode(tblHypervisorNode);
//            return node;
//        }).collect(Collectors.toList()));
        nodeInfos = nodeInfos.stream().peek(nodeInfo ->
        {
            nodeInfo.setMemSum(nodeInfo.getMemSum() - nodeInfo.getMemRecycle());
            nodeInfo.setCpuSum(nodeInfo.getCpuSum() - nodeInfo.getCpuRecycle());
        }).collect(Collectors.toList());
        List<String> nodeIds = nodeInfos.stream().map(HypervisorNodeAllocationInfo::getNodeId).collect(Collectors.toList());
        LambdaQueryWrapper<AvailableGPURsp> gpuQueryWrapper = new LambdaQueryWrapper<>();
        gpuQueryWrapper.in(AvailableGPURsp::getNodeId, nodeIds);
        List<AvailableGPURsp> availableGPURsps = availableGPUMapper.selectAvailableGPURsp(gpuQueryWrapper);
        Map<String, AvailableGPURsp> gpuRspMap = availableGPURsps.stream().collect(Collectors.toMap(AvailableGPURsp::getNodeId, Function.identity()));

        long gpuCount = pciDeviceMapper.selectPciDeviceCount();
        for (HypervisorNodeAllocationInfo allocationInfo : nodeInfos)
        {
            if (gpuRspMap.containsKey(allocationInfo.getNodeId()))
            {
                allocationInfo.setGpuTotal(gpuCount);
                allocationInfo.setGpuCount(gpuRspMap.get(allocationInfo.getNodeId()).getGpuCount());
                allocationInfo.setGpuName(gpuRspMap.get(allocationInfo.getNodeId()).getGpuName());
            }
        }
        nodeAllocationInfosRsp.setNodeAllocationInfos(nodeInfos);
        return nodeAllocationInfosRsp;
    }

    public HypervisorNodeDetailInfoRsp getHypervisorNode(String nodeId) throws WebSystemException
    {
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(nodeId);
        if (null == tblHypervisorNode)
        {
            log.info("get hypervisor node error: not exists, hypervisorNodeId: {}", nodeId);
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        HypervisorNodeDetailInfoRsp getHypervisorNodeDetailInfoRsp = new HypervisorNodeDetailInfoRsp();
        getHypervisorNodeDetailInfoRsp.setHasGpu(false);
        LambdaQueryWrapper<PciDeviceGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDeviceGroup::getNodeId, nodeId)
                .ne(PciDeviceGroup::getPhaseStatus, REMOVED);
        if (pciDeviceGroupService.count(queryWrapper) > 0)
        {
            getHypervisorNodeDetailInfoRsp.setHasGpu(true);
        }
        getHypervisorNodeDetailInfoRsp.setHypervisorNodeDetailInfo(tblHypervisorNode);

//        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(VmInstance::getNodeId, nodeId)
//                .ne(VmInstance::getPhaseStatus, REMOVED);
//
//        if (vmInstanceService.count(queryWrapper) > 0)
//        {
//            List<VmInstance> tblVmInstances = vmInstanceService.list(queryWrapper);
//            List<HypervisorNodeDetailInfoRsp.VmInfo> vmInfos = tblVmInstances.stream().map(
//                    tblVmInstance -> {
//                        HypervisorNodeDetailInfoRsp.VmInfo vmInfo = new HypervisorNodeDetailInfoRsp.VmInfo();
//                        vmInfo.setVmInstanceId(tblVmInstance.getVmInstanceId());
//                        vmInfo.setVmName(tblVmInstance.getName());
//                        return vmInfo;
//                    }
//            ).collect(Collectors.toList());
//            getHypervisorNodeDetailInfoRsp.setVmInfos(vmInfos);
//        }
        return getHypervisorNodeDetailInfoRsp;
    }

    public HypervisorNodeBaseRsp removeHypervisorNode(String nodeId) throws WebSystemException
    {
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(nodeId);
        if (null == tblHypervisorNode)
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getNodeId, nodeId)
                .ne(VmInstance::getPhaseStatus, REMOVED);

        long totalNum = vmInstanceService.count(queryWrapper);
        if (totalNum > 0)
        {
            log.info("can't remove hypervisorNode, reason: vm instances on the node, hypervisorNodeId: {}", nodeId);
            throw new WebSystemException(ErrorCode.VM_RUNNING_ON_HYPERVISOR_NODE, ErrorLevel.INFO);
        }

        tblHypervisorNode.setPhaseStatus(REMOVED);
        tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = hypervisorNodeService.updateById(tblHypervisorNode);
        if (!ok)
        {
            log.info("remove hypervisorNode failed, hypervisorNode:{}", nodeId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HypervisorNodeBaseRsp hypervisorNodeBaseRsp = new HypervisorNodeBaseRsp();
        hypervisorNodeBaseRsp.setNodeId(tblHypervisorNode.getNodeId());
        return hypervisorNodeBaseRsp;
    }

    public HypervisorNodeBaseRsp updateHypervisorNode(String nodeId, CommonReq commonReq) throws WebSystemException
    {

        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(nodeId);
        if (null == tblHypervisorNode || REMOVED == tblHypervisorNode.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        tblHypervisorNode.setName(commonReq.getName());
        tblHypervisorNode.setDescription(commonReq.getDescription());
        tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = hypervisorNodeService.updateById(tblHypervisorNode);
        if (!ok)
        {
            log.info("update hypervisorNode  failed, hypervisorNodeId:{}", nodeId);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        HypervisorNodeBaseRsp hypervisorNodeBaseRsp = new HypervisorNodeBaseRsp();
        hypervisorNodeBaseRsp.setNodeId(tblHypervisorNode.getNodeId());
        return hypervisorNodeBaseRsp;

    }
}
