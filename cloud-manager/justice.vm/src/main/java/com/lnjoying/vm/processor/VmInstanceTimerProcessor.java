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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.constant.NetworkDeployStatus;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.justice.schema.service.repo.ImageService;
import com.lnjoying.vm.aspect.CheckVmTimeout;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.request.*;
import com.lnjoying.vm.domain.backend.response.BaseRsp;
import com.lnjoying.vm.domain.backend.response.GpuPartitionDeactiveRsp;
import com.lnjoying.vm.domain.backend.response.MonitorTagRsp;
import com.lnjoying.vm.domain.dto.response.HypervisorNodeMemInfo;
import com.lnjoying.vm.entity.*;
import com.lnjoying.vm.mapper.DeviceDetailInfoMapper;
import com.lnjoying.vm.mapper.HypervisorNodeMemInfoMapper;
import com.lnjoying.vm.service.*;
import com.lnjoying.vm.service.biz.CombRpcSerice;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.AesCryptoUtils;
import com.micro.core.utils.JsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class VmInstanceTimerProcessor extends AbstractRunnableProcessor
{
    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private HypervisorNodeService hypervisorNodeService;

    @Autowired
    private InstanceNetworkRefService instanceNetworkRefService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private ComputeConfig computeConfig;

    @Autowired
    private VmScheduler vmScheduler;

    @Autowired
    private VmSnapService vmSnapService;

    @Autowired
    private DiskInfoService diskInfoService;

    @Autowired
    private PubkeyService pubkeyService;

    @Resource
    private DeviceDetailInfoMapper deviceDetailInfoMapper;

    @Autowired
    private PciDeviceGroupService pciDeviceGroupService;

    @Autowired
    private PciDeviceService pciDeviceService;

    @Autowired
    private LogRpcService logRpcService;

    @Resource
    private HypervisorNodeMemInfoMapper hypervisorNodeMemInfoMapper;

    public VmInstanceTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("vm instance timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("vm instance timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            //get middle status instances
            List<VmInstance> tblVmInstanceList = getMiddleStatusInstances();

            //log.info(" vm instance timer processor run, instances size: {}", tblVmInstanceList.size());

            //check each instance and process
            for (VmInstance tblVmInstance : tblVmInstanceList)
            {
                processVmInstance(tblVmInstance);
            }
        }
        catch (WebSystemException e)
        {
            log.error("vm instance timer processor error: {}", e.getMessage());
            if (ErrorCode.UPDATE_DATABASE_ERR == e.getCode())
            {
                log.error("vm instance timer process error: {}", e.getMessage());
            }
        }
        catch (Exception e)
        {
            log.error("vm instance timer processor exception: {}", e.getMessage());
        }
    }

    //get middle status vm instances (not final status)
    private List<VmInstance> getMiddleStatusInstances()
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmInstance::getPhaseStatus, REMOVED)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_MIGRATE_FAILED)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_POWEROFF)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANED)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI);
//                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED);
        return vmInstanceService.list(queryWrapper);
    }

    private void processVmInstance(VmInstance tblVmInstance)
    {
        int phaseStatus = tblVmInstance.getPhaseStatus();

        try
        {
            switch (phaseStatus)
            {
                case VmInstanceStatus.INSTANCE_INIT:
                    ((VmInstanceTimerProcessor) AopContext.currentProxy()).scheduleVmAndCreateVolumes(tblVmInstance);
//                    createPort(tblVmInstance);
                    break;
                case VmInstanceStatus.PORT_CREATE:
                    ((VmInstanceTimerProcessor) AopContext.currentProxy()).createPort(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_PORT_PHASE_STATUS:
                    ((VmInstanceTimerProcessor) AopContext.currentProxy()).checkPortVolumePhaseAndApplySgs(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_APPLY_SG_RESULT:
                    ((VmInstanceTimerProcessor) AopContext.currentProxy()).checkSgApplyPhaseAndCreateVmInstance(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_INSTANCE_CREATED_STATUS:
                    ((VmInstanceTimerProcessor) AopContext.currentProxy()).checkCreateVmInstanceResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_CREATED:
                    ((VmInstanceTimerProcessor) AopContext.currentProxy()).injectIsoVmCreate(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_RESET_PASSWORD_HOSTNAME:
                    injectIsoToCloudInitVmInstance(tblVmInstance, VmInstanceStatus.WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME);
                    break;
                case VmInstanceStatus.WAIT_INSTANCE_CLOUD_INIT_RESULT:
                case VmInstanceStatus.WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME:
                    checkCloudInitResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_RUNNING:
                    createMonitorTags(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_REMOVING:
                    removeVmInstance(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_INSTANCE_REMOVED_STATUS:
                    checkRemoveVmInstanceResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_RESIZE_INIT:
                    resizeVmInstanceFromAgent(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_BOOT_DEV_SWITCHING:
                    switchBootDevFromAgent(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_INSTANCE_UPDATED_STATUS:
                case VmInstanceStatus.GET_INSTANCE_BOOT_DEV_STATUS:
                    checkUpdateVmInstanceResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_REBOOT_POWEROFFING:
                    checkRebootResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_POWEROFFING:
                case VmInstanceStatus.INSTANCE_POWERING_OFF_DETACH_PCI:
                    powerOff(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_POWERING_ON_PREPARE_PCI:
                    preparePciForPowerOn(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_POWERONING:
                case VmInstanceStatus.INSTANCE_POWERING_ON_ATTACH_PCI:
                    powerOn(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_INSTANCE_POWEROFF_RESULT:
                case VmInstanceStatus.GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS:
                    checkPoweroffResult(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_INSTANCE_POWERON_RESULT:
                    checkPoweronResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_MIGRATE_INIT:
                    suspendVmInstance(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_SUSPENDING:
                case VmInstanceStatus.INSTANCE_SUSPENDED:
                    checkInstanceSuspendAndResume(tblVmInstance);
                    break;
                case VmInstanceStatus.GET_INSTANCE_RESUME_STATUS:
                    getResumeResult(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_RESUMED:
                    UpdateVmInstanceRunning(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_MIGRATE_CLEAN:
                    cleanAndUpdatePhaseStatus(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_CREATE_FAILED:
                    cleanCreateFailedVmInstance(tblVmInstance);
                    break;
                case VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANING:
                    getCleanCreateFailedVmInstanceResult(tblVmInstance);
                    break;
            }
        }
        catch (WebSystemException e)
        {
            log.error("vm instance timer processor error: node id {}, instance id {}, phase status {} , exception {}", tblVmInstance.getNodeId(), tblVmInstance.getVmInstanceId(), phaseStatus, e.getMessage());
            if (ErrorCode.UPDATE_DATABASE_ERR == e.getCode()) throw e;
        }
        catch (Exception e)
        {
            log.error("vm instance timer processor error: node id {}, instance id {}, phase status {} , exception {}", tblVmInstance.getNodeId(), tblVmInstance.getVmInstanceId(), phaseStatus, e);
        }
    }

    private void cleanAndUpdatePhaseStatus(VmInstance tblVmInstance)
    {
        log.info("phase: migrate_clean,{}", tblVmInstance.getVmInstanceId());
        try
        {

            //check lastNode is OK
            HypervisorNode lastNode = hypervisorNodeService.getById(tblVmInstance.getLastNodeId());
            if (null == lastNode || REMOVED == lastNode.getPhaseStatus())
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RUNNING);
                tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                vmInstanceService.updateById(tblVmInstance);
                return;
            }
            if (lastNode.getErrorCount() > 0)
            {
                log.info("Last Node is not ok, lastNodeId:{}", lastNode.getNodeId());
                return;
            }

            //String lastNodeManageIp = getLastNodeManageIp(tblVmInstance);
            String lastNodeManageIp = lastNode.getManageIp();

            //remove monitorTag from last node;
            String monitorTagUrl = "http://" + lastNodeManageIp + ComputeUrl.LIBVERT_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
            removeMonitorTags(tblVmInstance, monitorTagUrl);
            //remove gpu monitorTag
            String gpuMonitorTagUrl = "http://" + lastNodeManageIp + ComputeUrl.GPU_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
            removeGpuMonitorTags(tblVmInstance, gpuMonitorTagUrl);
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE);
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            vmInstanceService.updateById(tblVmInstance);
        }
        catch (Exception e)
        {
            log.error("last node is not ok, vmInstanceId:{},lastNodeId:{},nodeId:{}",
                    tblVmInstance.getVmInstanceId(), tblVmInstance.getLastNodeId(), tblVmInstance.getNodeId());
        }
    }

    //memory 是否充足
    public boolean checkMemoryEnough(VmInstance tblVmInstance)
    {
        if (VmInstanceStatus.INSTANCE_POWERING_ON_ATTACH_PCI == tblVmInstance.getPhaseStatus())
        {
            FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
            List<HypervisorNodeMemInfo> memInfos = hypervisorNodeMemInfoMapper.selectNodeMemInfo(tblVmInstance.getNodeId());
            if (null == memInfos || memInfos.size() == 0)
            {
                log.error("nodeId:{} memInfos is null", tblVmInstance.getNodeId());
                return false;
            }
            HypervisorNodeMemInfo memInfo = memInfos.get(0);
            int memFree = memInfo.getMemTotal() - memInfo.getMemUsed() + memInfo.getMemRecycle();
            return memFree > flavorInfo.getMem();
        }
        return true;
    }

    @CheckVmTimeout()
    public void scheduleVmAndCreateVolumes(VmInstance tblVmInstance)
    {
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        if (StrUtil.isBlank(tblVmInstance.getNodeId()))
        {
            String nodeId;
            if (flavorInfo.getGpuCount() != null && flavorInfo.getGpuCount() > 0)
            {
                nodeId = vmScheduler.getAvailableGpuNodeId(tblVmInstance, flavorInfo);
            }
            else
            {
                nodeId = vmScheduler.getAvailableNodeId(tblVmInstance, flavorInfo);
            }
            if (null == nodeId) return;
            tblVmInstance.setNodeId(nodeId);
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vmInstanceService.updateById(tblVmInstance);
            if (!ok)
            {
                log.error("update database error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
                return;
            }
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "虚机调度到计算节点上",
                    String.format("计算节点为:%s", tblVmInstance.getNodeId()), "调度成功");
        }
        else
        {
            if (flavorInfo.getGpuCount() != null && flavorInfo.getGpuCount() > 0)
            {
                String vmInstanceId = vmScheduler.setPciDevicesByNodeId(tblVmInstance, flavorInfo, tblVmInstance.getNodeId());
                if (StrUtil.isBlank(vmInstanceId))
                {
                    log.error("set pci devices error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
                    return;
                }
            }
        }
        String createVolumeStatus = createVolumes(tblVmInstance);
        if (AgentConstant.OK.equals(createVolumeStatus))
        {
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent正在创建虚机根盘",
                    String.format("请求参数: imageId: %s", tblVmInstance.getImageId()), "创建成功");
            tblVmInstance.setPhaseStatus(VmInstanceStatus.PORT_CREATE);
        }
        else if (AgentConstant.GET_STATUS_FAILED.equals(createVolumeStatus))
        {
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED);
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent正在创建虚机根盘",
                    String.format("请求参数: imageId: %s", tblVmInstance.getImageId()), "创建失败");
        }
        else
        {
            return;
        }
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        vmInstanceService.updateById(tblVmInstance);

    }

    @CheckVmTimeout()
    public void createPort(VmInstance tblVmInstance)
    {
        List<NetworkService.CreatePortReq> createPortReqs = new ArrayList<>();
        NetworkService.CreatePortReq createPortReq = new NetworkService.CreatePortReq();
        createPortReq.setStaticIp(tblVmInstance.getStaticIp());
        createPortReq.setSubnetId(tblVmInstance.getSubnetId());
        createPortReq.setContext(tblVmInstance.getVmInstanceId());
        createPortReq.setIsVip(false);
        HypervisorNode node = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == node || REMOVED == node.getPhaseStatus())
        {
            log.error("nodeId:{} is not exist", tblVmInstance.getNodeId());
            return;
        }
        createPortReq.setAgentId(node.getAgentId());

        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(InstanceNetworkRef::getPhaseStatus, REMOVED)
                .isNull(InstanceNetworkRef::getPortId)
                .eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId());
        if (instanceNetworkRefService.count(queryWrapper) > 0)
        {
            createPortReqs.add(createPortReq);
            List<InstanceNetworkRef> tblInstanceNetworkRefs = instanceNetworkRefService.list(queryWrapper);
            createPortReqs.addAll(
                    tblInstanceNetworkRefs.stream().map(
                            tblInstanceNetworkRef ->
                            {
                                NetworkService.CreatePortReq req = new NetworkService.CreatePortReq();
                                req.setContext(tblInstanceNetworkRef.getInstanceId());
                                req.setStaticIp(tblInstanceNetworkRef.getStaticIp());
                                req.setSubnetId(tblInstanceNetworkRef.getSubnetId());
                                req.setIsVip(tblInstanceNetworkRef.getIsVip());
                                return req;
                            }
                    ).collect(Collectors.toList())
            );
            List<String> portIds = combRpcSerice.getNetworkService().createPorts(createPortReqs, true);
            if (portIds.size() != createPortReqs.size())
            {
                log.info("get portIds error: {}", createPortReqs);
                return;
            }
            for (int i = 0; i < portIds.size(); i++)
            {
                if (StrUtil.isBlank(portIds.get(i)))
                {
                    continue;
                }
                if (0 == i)
                {
                    tblVmInstance.setPortId(portIds.get(i));
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.GET_PORT_PHASE_STATUS);
//                    ok = vmInstanceService.updateById(tblVmInstance);
                }
                else
                {
                    tblInstanceNetworkRefs.get(i - 1).setPortId(portIds.get(i));
                    boolean ok = instanceNetworkRefService.updateById(tblInstanceNetworkRefs.get(i - 1));
                    if (!ok)
                    {
                        log.info("update database tbl_vm_instance error, portId:{}", portIds.get(i));
                    }
                }

            }
        }
        else
        {
            String portId = combRpcSerice.getNetworkService().createPort(createPortReq, true, null);
            if (StrUtil.isBlank(portId))
            {
                log.error("get portId null, vmInstanceId:{}", tblVmInstance.getPortId());
                return;
            }
            tblVmInstance.setPortId(portId);
            tblVmInstance.setPhaseStatus(VmInstanceStatus.GET_PORT_PHASE_STATUS);
        }
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.info("update database tbl_vm_instance error");
            return;
        }
        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 正在配置网卡",
                String.format("请求参数: vmInstanceId: %s", tblVmInstance.getVmInstanceId()), "创建成功");
    }

    private List<InstanceNetworkRef> getRemovingInstanceNetworkRefs(VmInstance tblVmInstance)
    {
        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId())
                .eq(InstanceNetworkRef::getPhaseStatus, VmInstanceStatus.INSTANCE_REMOVING);
        return instanceNetworkRefService.list(queryWrapper);
    }

    private List<InstanceNetworkRef> getInstanceNetworkRefs(VmInstance tblVmInstance)
    {
        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId())
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);
        return instanceNetworkRefService.list(queryWrapper);
    }

    private long countDiskInfos(VmInstance tblVmInstance)
    {
        LambdaQueryWrapper<DiskInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(DiskInfo::getPhaseStatus, REMOVED)
                .eq(DiskInfo::getVmInstanceId, tblVmInstance.getVmInstanceId());
        return diskInfoService.count(queryWrapper);
    }

    private long countInstanceNetworkRefs(VmInstance tblVmInstance)
    {
        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId())
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);
        return instanceNetworkRefService.count(queryWrapper);
    }


    private List<String> getDataDiskVolumeIdFromAgent(VmInstance tblVmInstance)
    {
        List<DiskInfo> tblDiskInfo = getDiskInfos(tblVmInstance.getVmInstanceId());
        List<String> volumeIds = tblDiskInfo.stream().map(DiskInfo::getVolumeId).collect(Collectors.toList());
        log.info("get data disk volumeIds:{}", volumeIds);
        return combRpcSerice.getVolumeService().getVolumeIdFromAgentList(volumeIds);
    }

    private List<NetworkService.TenantNetworkPort> checkPortPhase(VmInstance tblVmInstance)
    {
        List<InstanceNetworkRef> tblInstanceNetworkRefs = getInstanceNetworkRefs(tblVmInstance);
        List<String> portIds = new ArrayList<>();
        portIds.add(tblVmInstance.getPortId());
        if (null != tblInstanceNetworkRefs && tblInstanceNetworkRefs.size() > 0)
        {
            portIds.addAll(tblInstanceNetworkRefs.stream().map(InstanceNetworkRef::getPortId).collect(Collectors.toList()));
        }
        List<NetworkService.TenantNetworkPort> tenantNetworkPorts = combRpcSerice.getNetworkService().getTenantNetworkPorts(portIds);
        if (tenantNetworkPorts.size() != portIds.size())
        {
            log.info("get tenantNetworkPorts error, portIds:{}", portIds);
            return null;
        }
        for (String portId : portIds)
        {
            switch (tenantNetworkPorts.get(0).getPhaseStatus())
            {
                case NetworkDeployStatus.ADD_FAILED:
                    log.info("port add failed, portId:{}", portId);
                    boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_CREATE_FAILED);
                    if (!ok)
                    {
                        log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                    }
                    return null;
                case NetworkDeployStatus.ADDED:
                    continue;
                default:
                    return null;
            }
        }
        return tenantNetworkPorts;
    }

    private String createVolumes(VmInstance tblVmInstance)
    {
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        if (null == flavorInfo) return AgentConstant.GET_STATUS_FAILED;

        String nodeIp = hypervisorNodeService.getById(tblVmInstance.getNodeId()).getManageIp();
        String nodeStoragePoolId = combRpcSerice.getStoragePoolService().createNodeStoragePool(nodeIp, tblVmInstance.getStoragePoolId());
        if (StrUtil.isBlank(nodeStoragePoolId)) return null;
//        String nodeStoragePoolIdFromAgent = combRpcSerice.getStoragePoolService()
        if (!StrUtil.isBlank(tblVmInstance.getImageId()))
        {
            String nodeImageId = combRpcSerice.getImageService().createNodeImage(tblVmInstance.getImageId(), nodeIp, nodeStoragePoolId);
            if (StrUtil.isBlank(nodeImageId)) return null;
        }
        if (StrUtil.isBlank(tblVmInstance.getVolumeId()))
        {
            String volumeId = combRpcSerice.getVolumeService().createRootDisk(tblVmInstance.getUserId(),
                    tblVmInstance.getRootDisk(), tblVmInstance.getVmInstanceId(), tblVmInstance.getStoragePoolId(),
                    nodeIp, tblVmInstance.getImageId(), tblVmInstance.getName());
            if (StrUtil.isBlank(volumeId)) return AgentConstant.GET_STATUS_FAILED;
            tblVmInstance.setVolumeId(volumeId);
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vmInstanceService.updateById(tblVmInstance);
            if (!ok)
            {
                log.error("update tbl_vm_instance error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
                return AgentConstant.GET_STATUS_FAILED;
            }
        }
        else
        {
            attachRootVolume(tblVmInstance.getVmInstanceId(), tblVmInstance.getVolumeId(), nodeIp);
        }
        List<DiskInfo> tblDiskInfos = getDiskInfos(tblVmInstance.getVmInstanceId());
        if (tblDiskInfos.size() > 0)
        {
            List<String> volumeIds = new ArrayList<>();
            for (DiskInfo tblDiskInfo : tblDiskInfos)
            {
                if (StrUtil.isBlank(tblDiskInfo.getVolumeId()))
                {
                    String dataVolumeId = combRpcSerice.getVolumeService().createDataDisk(tblVmInstance.getUserId(),
                            tblDiskInfo.getSize(), tblDiskInfo.getName(), tblVmInstance.getVmInstanceId(), tblVmInstance.getStoragePoolId(),
                            nodeIp);
                    if (null == dataVolumeId) return AgentConstant.GET_STATUS_FAILED;
                    tblDiskInfo.setVolumeId(dataVolumeId);
                    tblDiskInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    boolean ok = diskInfoService.updateById(tblDiskInfo);
                    if (!ok)
                    {
                        log.error("update tbl_disk_info error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
                        return AgentConstant.GET_STATUS_FAILED;
                    }
                }
                else
                {
                    volumeIds.add(tblDiskInfo.getVolumeId());
                }

            }
            attachDataVolumes(tblVmInstance.getVmInstanceId(), volumeIds, nodeIp);

//            diskInfoService.updateBatchById(tblDiskInfos);
        }
        return AgentConstant.OK;

    }

    private void attachRootVolume(String vmInstanceId, String volumeId, String nodeIp)
    {
        String rpcVolumeId = combRpcSerice.getVolumeService().attachVolume(
                volumeId, vmInstanceId, nodeIp);
        log.info("get root volumeId: {}", rpcVolumeId);
        if (!Objects.equals(rpcVolumeId, volumeId))
        {
            log.error("attachRootVolume error, rpcVolumeId:{}, volumeId:{}", rpcVolumeId, volumeId);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    private void attachDataVolumes(String vmInstanceId, List<String> volumeIds,
                                   String nodeIp)
    {
        List<String> rpcVolumeIds = combRpcSerice.getVolumeService().attachVolumes(
                volumeIds, vmInstanceId, nodeIp);
        for (String rpcVolumeId : rpcVolumeIds)
        {
            if (StrUtil.isBlank(rpcVolumeId))
            {
//                log.error("attachDataVolumes error, rpcVolumeId:{}, volumeIds:{}", rpcVolumeId, volumeIds);
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
        }
    }


    @CheckVmTimeout()
    public void checkPortVolumePhaseAndApplySgs(VmInstance tblVmInstance)
    {
        List<NetworkService.TenantNetworkPort> ports = checkPortPhase(tblVmInstance);
        if (null == ports)
        {
            return;
        }
        String ok = combRpcSerice.getNetworkService().vmInstanceApplySgs(tblVmInstance.getVmInstanceId());
        if (!Objects.equals(ok, AgentConstant.OK))
        {
            log.error("vmInstanceApplySgs error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            return;
        }
        updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_APPLY_SG_RESULT);
    }

    @CheckVmTimeout()
    public void checkSgApplyPhaseAndCreateVmInstance(VmInstance tblVmInstance)
    {
        String result = combRpcSerice.getNetworkService().getApplySgsResult(tblVmInstance.getVmInstanceId());
        if (StrUtil.isBlank(result))
        {
            return;
        }
        List<NetworkService.TenantNetworkPort> ports = checkPortPhase(tblVmInstance);
        if (null == ports)
        {
            return;
        }
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        String volumeIdFromAgent = combRpcSerice.getVolumeService().getVolumeIdFromAgent(tblVmInstance.getVolumeId());
        if (StrUtil.isBlank(volumeIdFromAgent))
        {
            log.info("rootDisk null, volumeIdFromAgent {}", volumeIdFromAgent);
            return;
        }
        List<String> volumeIdFromAgentList = new ArrayList<>();
        volumeIdFromAgentList.add(volumeIdFromAgent);
        if (countDiskInfos(tblVmInstance) > 0)
        {
            List<String> tmpList = getDataDiskVolumeIdFromAgent(tblVmInstance);
            if (null == tmpList)
            {
                return;
            }
            for (String volumeId : tmpList)
            {
                if (StrUtil.isBlank(volumeId))
                {
                    log.info("dataDisk null, volumeIdFromAgent {}", volumeId);
                    return;
                }
            }
            volumeIdFromAgentList.addAll(tmpList);
        }

//        NetworkService.TenantNetworkPort port = combRpcSerice.getNetworkService().getTenantNetworkPort(tblVmInstance.getPortId());
//        if (null == port )
//        {
//            return;
//        }
//        switch (port.getPhaseStatus())
//        {
//            case NetworkDeployStatus.ADD_FAILED:
//                log.info("port add failed, portId:{}", tblVmInstance.getPortId());
//                int count = updateVmInstancePhaseStatus(tblVmInstance,VmInstanceStatus.INSTANCE_CREATE_FAILED);
//                if (1 != count)
//                {
//                    log.info("update database tbl_vm_instance error, vmInstanceId: {}",tblVmInstance.getVmInstanceId());
//                }
//                return;
//            case  NetworkDeployStatus.ADDED:
//                break;
//            default:
//                return;
//        }


        List<String> vpcIds = new ArrayList<>();
        vpcIds.add(tblVmInstance.getVpcId());
        List<InstanceNetworkRef> tblInstanceNetworkRefs = getInstanceNetworkRefs(tblVmInstance);
        if (null != tblInstanceNetworkRefs && tblInstanceNetworkRefs.size() > 0)
        {
            vpcIds.addAll(tblInstanceNetworkRefs.stream().map(InstanceNetworkRef::getVpcId).collect(Collectors.toList()));
        }
        List<NetworkService.Vpc> vpcs = combRpcSerice.getNetworkService().getVpcs(vpcIds);
        if (null == vpcs || 0 == vpcs.size())
        {
            log.info("get vpc  null, vpcId: {}", tblVmInstance.getVpcId());
            return;
        }
        AddVmInstanceFromAgentReq addVmInstanceFromAgentReq = new AddVmInstanceFromAgentReq();
        List<String> pciDeviceIdFromAgents = deviceDetailInfoMapper.selectDeviceIdFromAgentByVmId(tblVmInstance.getVmInstanceId());

        addVmInstanceFromAgentReq.setGpus(pciDeviceIdFromAgents);
//        addVmInstanceFromAgentReq.setMigrate(true);
        addVmInstanceFromAgentReq.setMem(flavorInfo.getMem());
        addVmInstanceFromAgentReq.setVcpu(flavorInfo.getCpu());
        addVmInstanceFromAgentReq.setVols(volumeIdFromAgentList);
        addVmInstanceFromAgentReq.setPorts(ports.stream().map(NetworkService.TenantNetworkPort::getPortIdFromAgent).collect(Collectors.toList()));
//        addVmInstanceFromAgentReq.setOs(tblVmInstance.getOsType());

        createVmInstance(tblVmInstance, addVmInstanceFromAgentReq);
    }

    private List<DiskInfo> getDiskInfos(String vmInstanceId)
    {
        LambdaQueryWrapper<DiskInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(DiskInfo::getPhaseStatus, REMOVED)
                .eq(DiskInfo::getVmInstanceId, vmInstanceId);
        return diskInfoService.list(queryWrapper);
    }

    private void createVmInstance(VmInstance tblVmInstance, AddVmInstanceFromAgentReq req)
    {
        String vmReq = JsonUtil.objectToJson(req);
        //todo get hypervisor node ip
        String nodeIp = getManageUrl(tblVmInstance);
        String url = nodeIp + ComputeUrl.V1_VM_URL;
        BaseRsp result = HttpActionUtil.post(url, vmReq, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating vm instance :null,  vm instance id {}", tblVmInstance.getVmInstanceId());
            return;
        }
        log.info("create vm instance result: {}, vmInstanceId:{}", result, tblVmInstance.getVmInstanceId());

        String status = result.getStatus();

        //not pending status -> error
        if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
        {
            boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_CREATE_FAILED);
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                return;
            }
            log.error("create vm error: vm instance id {}, result: {}", tblVmInstance.getVmInstanceId(), result);
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent创建虚机失败",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建失败");
//            updateVmInstancePhaseStatus(tblVmInstance,VmInstanceStatus.INSTANCE_CREATE_FAILED);
        }
        else
        {
            //update phase status, instance id from agent
            String vmInstanceIdFromAgent = result.getUuid();
            tblVmInstance.setInstanceIdFromAgent(vmInstanceIdFromAgent);
            boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_CREATED_STATUS);
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                return;
            }
            log.info("creating vm instance, : vm instance id {}, vm instance id from agent {}",
                    tblVmInstance.getVmInstanceId(), vmInstanceIdFromAgent);
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent创建虚机",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建中");
        }
    }

    @CheckVmTimeout()
    public void checkCreateVmInstanceResult(VmInstance tblVmInstance)
    {

        GetVmInstanceRspFromAgent getVmInstanceRsp = getVmInstanceStatusFromAgent(tblVmInstance);
        log.info("get vm instance status form agent : {} , vm instance id:{}", getVmInstanceRsp, tblVmInstance.getVmInstanceId());

        if (null == getVmInstanceRsp)
        {
            return;
        }

        switch (getVmInstanceRsp.getPhase())
        {
            case AgentConstant.SUCCESS:
                if (!getVmInstanceRsp.getAdded()) return;
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATED);
                tblVmInstance.setBootDev(getVmInstanceRsp.getBootDevice());
                if (AgentConstant.SHUT.equals(getVmInstanceRsp.getPower()))
                {
                    powerChange(tblVmInstance, AgentConstant.POWER_ON);
                    return;
                }
                List<String> portIds = getPortIds(tblVmInstance);
                List<String> hostIds = combRpcSerice.getNetworkService().createDnsHosts(portIds, tblVmInstance.getHostName());
                if (hostIds.size() != portIds.size())
                {
                    log.info("create dns hostname error， portIds:{}, hostIds:{}", portIds, hostIds);
                }
//                updateNodePort(getVmInstanceStatus, tblVmInstance, VmInstanceStatus.INSTANCE_CREATE_FAILED);
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 创建虚机成功",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建成功");
                break;
            case AgentConstant.FAIL:
                if ("add".equals(getVmInstanceRsp.getPhaseType()))
                {
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED);
                    logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent创建虚机失败",
                            String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建失败");
                }
                break;
            default:
                return;
        }
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
        }
    }

//    private void updateNodePort(GetVmInstanceRspFromAgent getVmInstanceStatus, VmInstance tblVmInstance, Integer failedPhase)
//    {
//        List<String> portIdsResult = combRpcSerice.getNetworkService().updateNodePort(getVmInstanceStatus.getMac(),getVmInstanceStatus.getPortIds());
//
//        if (getVmInstanceStatus.getPortIds().size() != portIdsResult.size())
//        {
//            log.info("update node port error， portIds:{}, portIdsResult:{}", getVmInstanceStatus.getPortIds(), portIdsResult);
//            tblVmInstance.setPhaseStatus(failedPhase);
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//        }
//    }

    private void checkCloudInitResult(VmInstance tblVmInstance)
    {
        Integer startPhase = tblVmInstance.getPhaseStatus();
        GetVmInstanceRspFromAgent getVmInstanceStatus = getVmInstanceStatusFromAgent(tblVmInstance);
        if (null == getVmInstanceStatus)
        {
            log.info("get vm instance status form agent is null, vm instance id:{}", tblVmInstance.getVmInstanceId());
            return;

        }
        int phaseStatus = -2;

        if (null == getVmInstanceStatus.getCloudinitMetas() || !getVmInstanceStatus.getCloudinitMetas().getDone())
        {
            log.info("cloud-init is running , vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            return;
        }
        if (getVmInstanceStatus.getCloudinitMetas().getDone())
        {
            boolean ok = ejectIsoAndUpdatePhase(tblVmInstance);
            if (ok) setIbCount(tblVmInstance, true);
            phaseStatus = VmInstanceStatus.DEVICE_ATTACHED;
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                return;
            }
        }
//        else
//        {
//            ok = updateVmInstancePhaseStatus(tblVmInstance,VmInstanceStatus.INSTANCE_CREATE_FAILED);
//            phaseStatus = VmInstanceStatus.DEVICE_ATTACH_FAILED;
//        }


        if (VmInstanceStatus.DEVICE_ATTACH_FAILED == tblVmInstance.getPhaseStatus())
        {
            if (VmInstanceStatus.WAIT_INSTANCE_CLOUD_INIT_RESULT == startPhase)
            {
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent cloudInit执行失败",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建失败");
            }
            else
            {
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent cloudInit执行失败",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "重置虚机失败");
            }
        }
        else
        {
            if (VmInstanceStatus.WAIT_INSTANCE_CLOUD_INIT_RESULT == startPhase)
            {
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent cloudInit执行成功，虚机创建成功",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建成功");
            }
            else
            {
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent cloudInit执行成功，虚机重置成功",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "重置成功");
            }
        }
        if (VmInstanceStatus.WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME == startPhase)
        {
            phaseStatus = startPhase;
        }
        log.info("update vm instance phase status success, vmInstanceId:{}, phaseStatus:{}", tblVmInstance.getVmInstanceId(), phaseStatus);
        updatePciDeviceAttachedStatus(tblVmInstance, getVmInstanceStatus.getGpus(), phaseStatus);
    }

    // update pci device Attached status
    public void updatePciDeviceAttachedStatus(VmInstance tblVmInstance, List<String> pciDeviceIdFromAgents, Integer phaseStatus)
    {
        if (pciDeviceIdFromAgents.isEmpty())
        {
            return;
        }
        if (VmInstanceStatus.WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME == phaseStatus)
        {
            return;
        }
        LambdaUpdateWrapper<PciDevice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(PciDevice::getDeviceIdFromAgent, pciDeviceIdFromAgents);
        List<PciDevice> tblPciDevices = pciDeviceService.list(updateWrapper);
        if (tblPciDevices.isEmpty())
        {
            log.info("get pci device from database is empty, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            return;
        }
        String partitionId = tblPciDevices.get(0).getPartitionId();
        PciDevice tblPciDevice = new PciDevice();
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblPciDevice.setUserId(tblVmInstance.getUserId());
        tblPciDevice.setPhaseStatus(phaseStatus);
        tblPciDevice.setVmInstanceId(tblVmInstance.getVmInstanceId());
        tblPciDevice.setPartitionId(partitionId);
        boolean ok = pciDeviceService.update(tblPciDevice, updateWrapper);
        if (!ok)
        {
            log.info("update database tbl_pci_device error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent PCI设备挂载",
                String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "挂载成功");
    }

    // update libvirt-exporter vmName and vmUserId
    public MonitorTagRsp createMonitorTagFromAgent(VmInstance tblVmInstance)
    {
        MonitorTagsCreateReq req = new MonitorTagsCreateReq(tblVmInstance.getInstanceIdFromAgent(),
                tblVmInstance.getVmInstanceId(), tblVmInstance.getName(), tblVmInstance.getFlavorId(),
                tblVmInstance.getUserId(), tblVmInstance.getInstanceGroupId(), tblVmInstance.getCmpTenantId(), tblVmInstance.getCmpUserId());
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == tblHypervisorNode)
        {
            log.error("get hypervisorNode :null, vm instanceId:{}", tblVmInstance.getVmInstanceId());
            return null;
        }
        String url = "http://" + tblHypervisorNode.getManageIp() + ComputeUrl.LIBVERT_EXPORTER_TAG_PORT_URL;
        String jsonStr = JsonUtil.objectToJson(req);
        try
        {
            String result = HttpActionUtil.post(url, jsonStr);
            MonitorTagRsp monitorTagRsp = JsonUtil.jsonToPojo(result, MonitorTagRsp.class);
            assert monitorTagRsp != null;
            if (AgentConstant.MONITOR_TAG_OK == monitorTagRsp.getCode())
            {
                LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId())
                        .ne(PciDevice::getPhaseStatus, REMOVED);
                if (0 == pciDeviceService.count(queryWrapper))
                {
                    return monitorTagRsp;
                }
                String gpuUrl = "http://" + tblHypervisorNode.getManageIp() + ComputeUrl.GPU_EXPORTER_TAG_PORT_URL;
                return createGpuMonitorTags(tblVmInstance, gpuUrl);
            }
            return monitorTagRsp;
        }
        catch (Exception e)
        {
            log.error("create monitor tag error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    // create gpu exporter
    public MonitorTagRsp createGpuMonitorTags(VmInstance tblVmInstance, String url)
    {
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        if (null == flavorInfo.getGpuCount() || 0 == flavorInfo.getGpuCount())
        {
            MonitorTagRsp monitorTagRsp = new MonitorTagRsp();
            monitorTagRsp.setCode(AgentConstant.MONITOR_TAG_OK);
            return monitorTagRsp;
        }
        GpuMonitorTagsCreateReq req = new GpuMonitorTagsCreateReq();
        req.setDomainId(tblVmInstance.getInstanceIdFromAgent());
        GpuMonitorTagsCreateReq.Metadata metadata = new GpuMonitorTagsCreateReq.Metadata();
        metadata.setInstanceId(tblVmInstance.getVmInstanceId());
        metadata.setUserId(tblVmInstance.getUserId());
        metadata.setCmpTenantId(tblVmInstance.getCmpTenantId());
        metadata.setCmpUserId(tblVmInstance.getCmpUserId());
        metadata.setInstanceName(tblVmInstance.getName());
        metadata.setGroupId(tblVmInstance.getInstanceGroupId());
        metadata.setNodeId(tblVmInstance.getNodeId());
        req.setMetadata(metadata);

        String jsonStr = JsonUtil.objectToJson(req);
        try
        {
            String result = HttpActionUtil.post(url, jsonStr);
            return JsonUtil.jsonToPojo(result, MonitorTagRsp.class);
        }
        catch (Exception e)
        {
            log.error("create monitor tag error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return null;
    }

    public void createMonitorTags(VmInstance tblVmInstance)
    {
        if (StrUtil.isNotBlank(tblVmInstance.getEipId()))
        {
            String resultId = combRpcSerice.getNetworkService().setEipByPortId(tblVmInstance.getPortId(), tblVmInstance.getEipId(), tblVmInstance.getUserId());
            if (!Objects.equals(resultId, tblVmInstance.getEipId()))
            {
                log.error("set eip error, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
                return;
            }
        }
        MonitorTagRsp getMonitorTagRsp = createMonitorTagFromAgent(tblVmInstance);
        assert getMonitorTagRsp != null;
        if (AgentConstant.MONITOR_TAG_OK != getMonitorTagRsp.getCode())
        {
            log.error("get GetMonitorTagRsp : err, vm instanceId:{} msg:{}", tblVmInstance.getVmInstanceId(), getMonitorTagRsp.getMessage());
            return;
        }
        updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE);
    }

    public void setIbCount(VmInstance tblVmInstance, boolean isUsed)
    {
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        HypervisorNode node = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        int availableIb = node.getAvailableIbCount();
        if (vmScheduler.needPartition(flavorInfo.getGpuName()))
        {
            int diff = 0;
            if (4 == computeConfig.getIbCount())
            {
                diff = 1;
            }
            else if (8 == computeConfig.getIbCount())
            {
                diff = 2;
            }
            switch (flavorInfo.getGpuCount())
            {
                case 1:
                    if (flavorInfo.getNeedIb())
                    {
                        if (isUsed) availableIb = availableIb - 1;
                        else availableIb = availableIb + 1;
                    }
                    break;
                case 2:
                    if (isUsed) availableIb = availableIb - diff;
                    else availableIb = availableIb + diff;
                    break;
                case 4:
                    if (isUsed) availableIb = availableIb - 2 * diff;
                    else availableIb = availableIb + 2 * diff;
                    break;
                case 8:
                    if (isUsed) availableIb = availableIb - 4 * diff;
                    else availableIb = availableIb + 4 * diff;
                    break;
            }
            if (availableIb < 0) availableIb = 0;
            if (availableIb > computeConfig.getIbCount()) availableIb = computeConfig.getIbCount();
            node.setAvailableIbCount(availableIb);
            hypervisorNodeService.updateById(node);
        }
    }

    // remove libvirt-exporter vmName and vmUserId
    public void removeMonitorTags(VmInstance tblVmInstance, String url)
    {
        try
        {
            String result = HttpActionUtil.delete(url);
            log.info("remove monitorTag request url:{} , result:{}", url, result);
            MonitorTagRsp getMonitorTagRsp = JsonUtil.jsonToPojo(result, MonitorTagRsp.class);
            assert getMonitorTagRsp != null;
            if (AgentConstant.MONITOR_TAG_OK != getMonitorTagRsp.getCode())
            {
                log.error("get GetMonitorTagRsp : err,url:{}, vm instanceId:{} msg:{}", url, tblVmInstance.getVmInstanceId(), getMonitorTagRsp.getMessage());
            }
        }
        catch (Exception exception)
        {
            log.error("remove monitorTag request url:{} , error:{}", url, exception.getMessage());
        }
    }

    // remove gpu monitor tags
    public void removeGpuMonitorTags(VmInstance tblVmInstance, String url)
    {
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        if (null == flavorInfo.getGpuCount() || 0 == flavorInfo.getGpuCount())
        {
            return;
        }
        removeMonitorTags(tblVmInstance, url);
    }


    //get vm instance status
    public GetVmInstanceRspFromAgent getVmInstanceStatusFromAgent(VmInstance tblVmInstance)
    {
        String instanceIdFromAgent = tblVmInstance.getInstanceIdFromAgent();
        //todo get hypervisor node ip
        String nodeIp = getManageUrl(tblVmInstance);
        String url = nodeIp + "/" + ComputeUrl.V1_VM_URL + "/" + instanceIdFromAgent;
        return HttpActionUtil.getObject(url, GetVmInstanceRspFromAgent.class);
    }

    private void resetHostName(VmInstance tblVmInstance)
    {

        List<String> portIds = new ArrayList<>();
        portIds.add(tblVmInstance.getPortId());
        combRpcSerice.getNetworkService().removeDnsHosts(portIds);
        List<String> hostIds = combRpcSerice.getNetworkService().createDnsHosts(portIds, tblVmInstance.getHostName());
        if (hostIds.size() != portIds.size())
        {
            log.info("create dns hostname error， portIds:{}, hostIds:{}", portIds, hostIds);
        }
        String managerIp = getManageIp(tblVmInstance);
        String removeGpuMonitorUrl = "http://" + managerIp + ComputeUrl.GPU_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
        removeGpuMonitorTags(tblVmInstance, removeGpuMonitorUrl);
        String gpuUrl = "http://" + managerIp + ComputeUrl.GPU_EXPORTER_TAG_PORT_URL;
        createGpuMonitorTags(tblVmInstance, gpuUrl);
    }

    @CheckVmTimeout()
    public void injectIsoVmCreate(VmInstance tblVmInstance)
    {
        injectIsoToCloudInitVmInstance(tblVmInstance, null);
    }

    private void injectIsoToCloudInitVmInstance(VmInstance tblVmInstance, Integer phaseStatus)
    {
        //get image info
        ImageService.Image image = null;
        if (StrUtil.isNotBlank(tblVmInstance.getImageId()))
        {
            image = combRpcSerice.getImageService().getImage(tblVmInstance.getImageId());
            if (null == image)
            {
                log.error("get image response error: instance id {}, image id {}", tblVmInstance.getVmInstanceId(), tblVmInstance.getImageId());
                boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_CREATE_FAILED);
                if (!ok)
                {
                    log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                }
                return;
            }
        }
        // iso file,虚机的镜像如何是基于iso文件创建的镜像，也不需要判断cloud-init的状态（iso 文件安装的虚机可能没有安装cloud-init)
        if (StrUtil.isBlank(tblVmInstance.getImageId()) || (StrUtil.isNotBlank(Objects.requireNonNull(image).getVmInstanceId()) && StrUtil.isBlank(image.getImageBase())))
        {
            GetVmInstanceRspFromAgent getVmInstanceStatus = getVmInstanceStatusFromAgent(tblVmInstance);
            boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_RUNNING);
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                return;
            }
            updatePciDeviceAttachedStatus(tblVmInstance, getVmInstanceStatus.getGpus(), VmInstanceStatus.DEVICE_ATTACHED);
            return;
        }

        InjectIsoReq agentInjectIsoReq = new InjectIsoReq();

        //todo get hypervisor node ip
        String nodeIp = getManageUrl(tblVmInstance);
        String url = nodeIp + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent() + "/inject";

        //params
        agentInjectIsoReq.setOs(getImageOsType(image.getImageOsType()));
        agentInjectIsoReq.setHostname(tblVmInstance.getHostName());
        agentInjectIsoReq.setUsername(tblVmInstance.getSysUsername());
        agentInjectIsoReq.setPassword(AesCryptoUtils.decryptStr(tblVmInstance.getSysPassword()));

        //pubkey
        String pubkeyId = tblVmInstance.getPubkeyId();
        if (null != pubkeyId && !pubkeyId.isEmpty())
        {
            Pubkey tblPubkey = pubkeyService.getById(pubkeyId);
            if (null != tblPubkey)
            {
                agentInjectIsoReq.setPubkey(tblPubkey.getPubkey());
            }
            else
            {
                agentInjectIsoReq.setPubkey("");
            }

        }
        else
        {
            agentInjectIsoReq.setPubkey("");
        }

        String postString = JsonUtil.objectToJson(agentInjectIsoReq);
        if (postString == null || postString.isEmpty())
        {
            return;
        }

        //send request to pxe agent
        String status = putHypervisorNode(url, postString);
//        String result = HttpActionUtil.put(url, postString);
//        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == status)
        {
            log.error("get vm agent response error:  instance id {}, image id {}", tblVmInstance.getVmInstanceId(), tblVmInstance.getImageId());
            boolean ok = vmInstanceService.updateById(tblVmInstance);
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            }
            return;
        }

        //not pending status -> error
        if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
        {
            log.error("get image response status error:  instance id {}, {}", tblVmInstance.getVmInstanceId(), tblVmInstance.getImageId());
            return;
        }

        //update instance  phase status to db
        boolean ok;
        // 创建虚机时的cloud-init
        if (null == phaseStatus)
        {
            ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.WAIT_INSTANCE_CLOUD_INIT_RESULT);
        }
        // 重置hostname,密码
        else
        {
            resetHostName(tblVmInstance);
            ok = updateVmInstancePhaseStatus(tblVmInstance, phaseStatus);
        }
        if (!ok)
        {
            log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
        }
        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "正在执行cloudInit",
                String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "等待cloudInit的执行结果");
    }


    private boolean updateVmInstancePhaseStatus(VmInstance tblVmInstance, int phaseStatus)
    {
        tblVmInstance.setPhaseStatus(phaseStatus);
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        //update instance deploying phase status to db
        return vmInstanceService.updateById(tblVmInstance);
    }

    private boolean ejectIsoAndUpdatePhase(VmInstance tblVmInstance)
    {
        //todo get hypervisor node ip
        String nodeIp = getManageUrl(tblVmInstance);
        String url = nodeIp + "/" + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent() + "/eject";
        //do not care the result, so we won't check the result.
        HttpActionUtil.put(url, "");

        return updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_RUNNING);

    }

    private void removePort(VmInstance tblVmInstance)
    {
        //unapply security group
        String result = combRpcSerice.getNetworkService().vmInstanceUnBoundSgs(tblVmInstance.getVmInstanceId());
        if (!"ok".equals(result))
        {
            log.error("vm instance unBound security group error, vminstanceId :{}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        //del port from tenant network
        List<InstanceNetworkRef> tblInstanceNetworkRefs = getRemovingInstanceNetworkRefs(tblVmInstance);
        List<String> portIds = new ArrayList<>();
        portIds.add(tblVmInstance.getPortId());
//            String portId = combRpcSerice.getNetworkService().delFromTenantNetwork(tblVmInstance.getPortId());
        if (null != tblInstanceNetworkRefs && tblInstanceNetworkRefs.size() > 0)
        {
            portIds.addAll(tblInstanceNetworkRefs.stream().map(InstanceNetworkRef::getPortId).collect(Collectors.toList()));
        }
        combRpcSerice.getNetworkService().removeDnsHosts(portIds);
        List<String> portIdsReps = combRpcSerice.getNetworkService().delPortsFromTenantNetwork(portIds);
        if (null == portIdsReps || portIdsReps.size() != portIds.size())
        {
            //update failed phase status to db
            log.error("vm instance timer processor error: del from tenant network port id {}, instance id {}", tblVmInstance.getPortId(), tblVmInstance.getVmInstanceId());
        }
        else
        {
            if (null != tblInstanceNetworkRefs && tblInstanceNetworkRefs.size() > 0)
            {
                tblInstanceNetworkRefs.forEach(
                        tblInstanceNetworkRef ->
                        {
                            tblInstanceNetworkRef.setPhaseStatus(REMOVED);
                            tblInstanceNetworkRef.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                            instanceNetworkRefService.updateById(tblInstanceNetworkRef);
                        }
                );
            }
        }
    }

    private void removeDisks(String vmInstanceId)
    {
        Boolean detachedOk = combRpcSerice.getVolumeService().detachVolumesByVmId(vmInstanceId, false);
        if (!detachedOk)
        {
            log.info("detach volumes err, vmInstanceId:{}", vmInstanceId);
        }
        LambdaUpdateWrapper<DiskInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DiskInfo::getVmInstanceId, vmInstanceId)
                .eq(DiskInfo::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATED);
        DiskInfo tblDiskInfo = new DiskInfo();
        tblDiskInfo.setPhaseStatus(REMOVED);
        tblDiskInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        diskInfoService.update(tblDiskInfo, updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeVmInstance(VmInstance tblVmInstance)
    {
        try
        {
            removePort(tblVmInstance);
            removeDisks(tblVmInstance.getVmInstanceId());

            //todo get hypervisor node ip
//        String nodeIp = vmComputeRepository.getHypervisorNodeById(tblVmInstance.getNodeId()).getManageIp();
//        List<String> sgs = new ArrayList<>();
//        if (null == ComputeServiceImpl.vmBondSecurityGroup(nodeIp,tblVmInstance.getInstanceIdFromAgent(),sgs))
//        {
//            return;
//        }
            if (StrUtil.isBlank(tblVmInstance.getInstanceIdFromAgent()))
            {
                updateVmInstancePhaseStatus(tblVmInstance, REMOVED);
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 删除虚机",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "删除成功");
                return;
            }
            String url = getManageUrl(tblVmInstance) + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
            BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);
            if (null == result)
            {
                log.error("get response of removing vm instance :null,  vm instance id {}", tblVmInstance.getVmInstanceId());
                return;
            }

            String status = result.getStatus();

            //not pending status -> error
            boolean ok;
            if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
            {
                result = HttpActionUtil.getObject(url, BaseRsp.class);
                if (null == result)
                {
                    log.error("get response of removing vm instance :null,  vm instance id {}", tblVmInstance.getVmInstanceId());
                    return;
                }
                if (Objects.equals(result.getStatus(), AgentConstant.FAILED)
                        && result.getReason().contains(AgentConstant.NOT_EXIST))
                {
                    ok = updateVmInstancePhaseStatus(tblVmInstance, REMOVED);
                    logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 删除虚机",
                            String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "删除成功");
                }
//                else if (Objects.equals(result.getStatus(), AgentConstant.FAILED)
//                        && Objects.equals(resultMap.get("reason"), AgentConstant.VM_DELETING))
//                {
//                    ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_REMOVED_STATUS);
//                }
                else
                {
                    log.error("remove vm error: vm instance id {}", tblVmInstance.getVmInstanceId());
                    ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_REMOVE_FAILED);
                    logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 删除虚机",
                            String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "删除失败");
                }
            }
            else
            {
                ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_REMOVED_STATUS);
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 删除虚机",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "删除中");
            }
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            }
        }
        catch (Exception e)
        {
            log.error("remove vm instance error:{}, vm instance id {}", e.getMessage(), tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
    }

    // phaseStatus 为REMOVED, 同步处理PCI设备的状态
    @Transactional(rollbackFor = Exception.class)
    public void setPciDevicePhase(VmInstance tblVmInstance, Integer phaseStatus)
    {
        //list pciDeviceGroup by vmInstanceId
        LambdaUpdateWrapper<PciDevice> updatePciDeviceWrapper = new LambdaUpdateWrapper<>();
        updatePciDeviceWrapper.ne(PciDevice::getPhaseStatus, REMOVED)
                .eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId());

        List<PciDevice> tblPciDevices = pciDeviceService.list(updatePciDeviceWrapper);
        if (null == tblPciDevices || tblPciDevices.isEmpty())
        {
            return;
        }
        for (PciDevice tblPciDevice : tblPciDevices)
        {
//            tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHED);
            tblPciDevice.setPhaseStatus(phaseStatus);
            tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            if (VmInstanceStatus.DEVICE_DETACHED == phaseStatus)
            {
                tblPciDevice.setVmInstanceId(null);
            }
            pciDeviceService.updateById(tblPciDevice);
        }
//        pciDeviceService.updateBatchById(tblPciDevices);
        //update pciDeviceGroup
//        LambdaUpdateWrapper<PciDeviceGroup> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(PciDeviceGroup::getVmInstanceId, tblVmInstance.getVmInstanceId());
//        PciDeviceGroup pciDeviceGroup = new PciDeviceGroup();
//        pciDeviceGroup.setVmInstanceId(null);
//        pciDeviceGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//        pciDeviceGroupService.update(pciDeviceGroup, updateWrapper);

        // update pci device detach phase
//        LambdaUpdateWrapper<PciDevice> updatePciDeviceWrapper = new LambdaUpdateWrapper<>();
//        updatePciDeviceWrapper.in(PciDevice::getDeviceGroupId, pciDeviceGroupIds);
//        PciDevice tblPciDevice = new PciDevice();
//
//        pciDeviceService.update(tblPciDevice, updatePciDeviceWrapper);
    }


    private void checkRemoveVmInstanceResult(VmInstance tblVmInstance)
    {
        try
        {
            String nodeIp = getManageUrl(tblVmInstance);
            if (StrUtil.isBlank(nodeIp) && StrUtil.isBlank(tblVmInstance.getInstanceIdFromAgent()))
            {
                boolean ok = updateVmInstancePhaseStatus(tblVmInstance, REMOVED);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 删除虚机",
                            String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "删除成功");
                }
                return;
            }
            String url = nodeIp + "/" + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
            boolean ok = false;
            //send request to pxe agent
            GetVmInstanceRspFromAgent result = HttpActionUtil.getObject(url, GetVmInstanceRspFromAgent.class);

            if (result != null)
            {
                if (Objects.equals(result.getStatus(), "failed")
                        && result.getReason().contains(AgentConstant.NOT_EXIST))
                {

                    ok = updateVmInstancePhaseStatus(tblVmInstance, REMOVED);
                    if (ok)
                    {
                        setIbCount(tblVmInstance, false);
                        String managerIp = getManageIp(tblVmInstance);
                        String removeMonitorUrl = "http://" + managerIp + ComputeUrl.LIBVERT_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
                        removeMonitorTags(tblVmInstance, removeMonitorUrl);
                        String removeGpuMonitorUrl = "http://" + managerIp + ComputeUrl.GPU_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
                        removeGpuMonitorTags(tblVmInstance, removeGpuMonitorUrl);
                        deactiveGpuPartition(tblVmInstance);
                        setPciDevicePhase(tblVmInstance, VmInstanceStatus.DEVICE_DETACHED);
                        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 删除虚机",
                                String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "删除成功");
                    }
                    else
                    {
                        log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                    }
                }
                // ok = updateVmInstancePhaseStatus(tblVmInstance,VmInstanceStatus.INSTANCE_REMOVE_FAILED);
            }
        }
        catch (Exception e)
        {
            log.error("remove vm instance  error: instance id {}, {}", tblVmInstance.getVmInstanceId(), e.getMessage());
        }
    }

    private boolean isStatusFailed(String phase)
    {
        return (phase.equalsIgnoreCase(AgentConstant.INJECT_BOOT_FAILED)
                || phase.equalsIgnoreCase(AgentConstant.EJECT_FAILED));
    }

    String getImageOsType(int imageOsType)
    {
        if (imageOsType == 1)
        {
            return "windows";
        }
        return "linux";
    }

    public String getManageIp(VmInstance tblVmInstance)
    {
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == tblHypervisorNode)
        {
            log.error("get node manager ip :null, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            return null;
        }
        return tblHypervisorNode.getManageIp();
    }

    public String getLastNodeManageIp(VmInstance tblVmInstance)
    {
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getLastNodeId());
        if (null == tblHypervisorNode)
        {
            log.error("get node manager ip :null, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            return null;
        }
        return tblHypervisorNode.getManageIp();
    }


    public String getManageUrl(VmInstance tblVmInstance)
    {
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        if (null == vmAgentPort || 0 == vmAgentPort)
        {
            vmAgentPort = ComputeUrl.VM_AGENT_PORT;
        }
        String managerIp = getManageIp(tblVmInstance);
        if (StrUtil.isBlank(managerIp)) return null;
        return "http://" + managerIp + ":" + vmAgentPort;
    }


    private String putHypervisorNode(String url, String putStr)
    {
        try
        {
            BaseRsp result = HttpActionUtil.put(url, putStr, BaseRsp.class);
            if (null == result)
            {
                log.error("url: {}, put:{} err: {}", url, putStr, "result is null");
                return null;
            }
            if (!AgentConstant.PENDING_STATUS.equals(result.getStatus()))
            {
                log.error("url: {}, put:{} err: {}", url, putStr, result);
                return result.getStatus();
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
            }
            return AgentConstant.PENDING_STATUS;
        }
        catch (Exception e)
        {
            log.error("url: {}, put:{} err: {}", url, putStr, e.getMessage());
            return null;
        }
    }

    void checkPoweronResult(VmInstance tblVmInstance)
    {
        if (AgentConstant.POWER_ON.equals(getPowerStatus(tblVmInstance)))
        {
            Integer currentPhase = vmInstanceService.getById(tblVmInstance.getVmInstanceId()).getPhaseStatus();
            log.info("checkPoweronResult currentPhase:{}, vmInstanceId:{}", currentPhase, tblVmInstance.getVmInstanceId());
            if (VmInstanceStatus.INSTANCE_RUNNING == currentPhase || VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE == currentPhase)
            {
                return;
            }
            boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_RUNNING);
            if (ok)
            {
                log.info("power on success,phase:{} vmInstanceId:{}", currentPhase, tblVmInstance.getVmInstanceId());
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 启动虚机",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "开机成功");
            }
        }
    }

    void checkPoweroffResult(VmInstance tblVmInstance)
    {
        boolean needDetachPciDev = tblVmInstance.getPhaseStatus().equals(VmInstanceStatus.GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS);
        boolean ok;

        if (AgentConstant.POWER_OFF.equals(getPowerStatus(tblVmInstance)))
        {
            Integer currentPhase = vmInstanceService.getById(tblVmInstance.getVmInstanceId()).getPhaseStatus();
            if (VmInstanceStatus.INSTANCE_POWEROFF == currentPhase || VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI == currentPhase)
                return;
            if (VmInstanceStatus.GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS == tblVmInstance.getPhaseStatus())
            {
                ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI);
            }
            else
            {
                ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_POWEROFF);
            }
            if (ok)
            {
                log.info("power off success,phase:{} vmInstanceId:{}", currentPhase, tblVmInstance.getVmInstanceId());
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 关闭虚机",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "关机成功");
            }
            if (needDetachPciDev)
            {
                setPciDevicePhase(tblVmInstance, VmInstanceStatus.DEVICE_DETACHING);
            }
        }

    }

    String getPowerStatus(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent getVmInstanceStatus = getVmInstanceStatusFromAgent(tblVmInstance);
        if (null == getVmInstanceStatus)
        {
            log.info("get vm instance status form agent is null, vm instance id:{}", tblVmInstance.getVmInstanceId());
            return null;
        }
        if (AgentConstant.RUNNING.equalsIgnoreCase(getVmInstanceStatus.getPower()))
            return AgentConstant.POWER_ON;
        else if (AgentConstant.SHUT_OFF.equalsIgnoreCase(getVmInstanceStatus.getPower()))
            return AgentConstant.POWER_OFF;
        return null;
    }

    public String powerChange(VmInstance tblVmInstance, String powerState)
    {
        if (!(AgentConstant.POWER_OFF.equalsIgnoreCase(powerState) || AgentConstant.POWER_ON.equalsIgnoreCase(powerState)))
        {
            return null;
        }
        String nodeIp = getManageUrl(tblVmInstance);
        String url = nodeIp + "/" + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
        GetVmInstanceRspFromAgent getVmInstanceRspFromAgent = HttpActionUtil.getObject(url, GetVmInstanceRspFromAgent.class);
//        if (AgentConstant.PENDING_STATUS.equals(getVmInstanceRspFromAgent.getPoweroffPhase())||
//                AgentConstant.PENDING_STATUS.equals(getVmInstanceRspFromAgent.getPoweronPhase()))
//        {
//            return null;
//        }
        String powerUrl = nodeIp + "/" + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent() + "/power" + powerState.toLowerCase();
        String status = putHypervisorNode(powerUrl, "{}");
        //        String result = HttpActionUtil.put(url,"");
//        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == status)
        {
            log.error("powerChange vm instance and get agent response error: instance id from agent {}", tblVmInstance.getVmInstanceId());
            return null;
        }
//        String status = (String) resultMap.get("status");
        //not pending status -> error
        if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
        {
            log.error("powerChange vm instance and get agent response error:  instance id from agent {}, status {}", tblVmInstance.getVmInstanceId(), status);
            return null;
        }
        return status;

    }

    void powerOff(VmInstance tblVmInstance)
    {
        String status = powerChange(tblVmInstance, AgentConstant.POWER_OFF);
        if (null != status)
        {
            if (tblVmInstance.getPhaseStatus().equals(VmInstanceStatus.INSTANCE_POWERING_OFF_DETACH_PCI))
            {
                updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS);
            }
            else
            {
                updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_POWEROFF_RESULT);
            }
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 正在关机",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "关机中");
        }
    }

    public void preparePciForPowerOn(VmInstance tblVmInstance)
    {
        boolean memEnough = checkMemoryEnough(tblVmInstance);
        if (!memEnough)
        {
            log.error("powerOn vm instance and check memory error: instance id from agent {}", tblVmInstance.getVmInstanceId());
            updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_POWER_ON_FAILED);
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 启动虚机",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "资源不足，开机失败");
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }

        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());

        String vmInstanceId = vmScheduler.setPciDevicesByNodeId(tblVmInstance, flavorInfo, tblVmInstance.getNodeId());
        if (StrUtil.isBlank(vmInstanceId))
        {
            updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_POWER_ON_FAILED);
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }
        else setPciDevicePhase(tblVmInstance, VmInstanceStatus.DEVICE_ATTACHING);

        updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_POWERING_ON_ATTACH_PCI);
    }

    void powerOn(VmInstance tblVmInstance)
    {
        String status = powerChange(tblVmInstance, AgentConstant.POWER_ON);
        if (null != status)
        {
            tblVmInstance.setRecycleMemSize(0);
            tblVmInstance.setRecycleCpuCount(0);
            updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_POWERON_RESULT);
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 启动虚机",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "开机中");
        }

    }

    void checkRebootResult(VmInstance tblVmInstance)
    {
        checkPoweroffResult(tblVmInstance);
        tblVmInstance = vmInstanceService.getById(tblVmInstance.getVmInstanceId());
        if (VmInstanceStatus.INSTANCE_POWEROFF == tblVmInstance.getPhaseStatus())
        {
            powerOn(tblVmInstance);
        }
        else if (VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI == tblVmInstance.getPhaseStatus())
        {
            preparePciForPowerOn(tblVmInstance);
        }
        else if (VmInstanceStatus.INSTANCE_POWEROFFING == tblVmInstance.getPhaseStatus())
        {
            log.info("vm instance is poweroffing, vm instance id:{}", tblVmInstance.getVmInstanceId());
        }
        else
        {
            powerChange(tblVmInstance, AgentConstant.POWER_OFF);
        }
    }


    List<String> getPortIds(VmInstance tblVmInstance)
    {
        List<String> portIds = new ArrayList<>();
        portIds.add(tblVmInstance.getPortId());
        List<InstanceNetworkRef> instanceNetworkRefs = getInstanceNetworkRefs(tblVmInstance);
        if (instanceNetworkRefs.size() > 0)
        {
            portIds.addAll(instanceNetworkRefs.stream().map(
                    InstanceNetworkRef::getPortId
            ).collect(Collectors.toList()));
        }
        return portIds;
    }

    //    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
    private void suspendVmInstance(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent destRsp = getVmInstanceRspFromDestAgent(tblVmInstance);
        if (null != destRsp.getPhase())
        {
            if (destRsp.getPhase().equals(AgentConstant.ADDED) || destRsp.getPhase().equals(AgentConstant.RESUMED))
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESUMED);
                tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            }
            else if (destRsp.getPhase().equals(AgentConstant.RESUMING))
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.GET_INSTANCE_RESUME_STATUS);
                tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            }
            vmInstanceService.updateById(tblVmInstance);
            return;
        }
        GetVmInstanceRspFromAgent rsp = getVmInstanceStatusFromAgent(tblVmInstance);

        if (Objects.equals(rsp.getPhaseType(), "suspend"))
        {
            if (rsp.getPhase().equals(AgentConstant.FAIL))
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
            }
            else
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_SUSPENDING);
            }
        }
        else
        {
            String url = getManageUrl(tblVmInstance) + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent() + "/suspend";
            String status = putHypervisorNode(url, "{}");
            if (null == status)
            {
                log.error("suspend vm instance and get agent response error: instance id from agent {}", tblVmInstance.getVmInstanceId());
                return;
            }
            //not pending status -> error
            if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
            {
                log.error("suspend vm instance and get agent response error:  instance id from agent {}, status {}", tblVmInstance.getVmInstanceId(), status);
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
            }
            else
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_SUSPENDING);
            }
        }

        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.error("update database err: result is 0, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
        }
        if (VmInstanceStatus.INSTANCE_SUSPENDING == tblVmInstance.getPhaseStatus())
        {
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 正在挂起虚机",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "挂起中");
        }
        else if (VmInstanceStatus.INSTANCE_MIGRATE_FAILED == tblVmInstance.getPhaseStatus())
        {
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 挂起虚机失败",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "挂起失败");
        }
    }


    //    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
    private String getSuspendResult(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent rsp = getVmInstanceStatusFromAgent(tblVmInstance);
        if (!rsp.getPhaseType().equals("suspend")) return null;
        boolean err = false;
        String result = null;
        log.info("vm instanceId: {},get vm instance phase: {}", tblVmInstance.getVmInstanceId(), rsp.getPhase());
        switch (rsp.getPhase())
        {
            case AgentConstant.FAIL:
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
                result = AgentConstant.SUSPEND_FAILED;
                break;
            case AgentConstant.SUCCESS:
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_SUSPENDED);
                result = AgentConstant.SUSPENDED;
                break;
            case AgentConstant.PENDING_STATUS:
                log.info("vmInstanceId:{} is suspending", tblVmInstance.getVmInstanceId());
                result = AgentConstant.PENDING_STATUS;
                break;
//        else if (rsp.getPhase().equals(AgentConstant.ADDED) && StrUtil.isBlank(tblVmInstance.getDestNodeId()))
//        {
//            return AgentConstant.SUSPENDED;
//        }
            default:
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
                result = AgentConstant.SUSPEND_FAILED;
                err = true;
                break;
        }
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.error("update database err, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
        if (err)
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return result;
    }

    //    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
    private void checkInstanceSuspendAndResume(VmInstance tblVmInstance)
    {

//        String result = combRpcSerice.getNetworkService().getMigratePortResult(tblVmInstance.getVmInstanceId());
        if (Objects.equals(tblVmInstance.getPhaseStatus(), VmInstanceStatus.INSTANCE_SUSPENDED) ||
                Objects.equals(getSuspendResult(tblVmInstance), AgentConstant.SUSPENDED)
        )
        {
            if (StrUtil.isBlank(tblVmInstance.getDestNodeId()))
            {
                GetVmInstanceRspFromAgent rspFromAgent = getVmInstanceStatusFromAgent(tblVmInstance);
                if (AgentConstant.ADDED.equals(rspFromAgent.getPhase()))
                {
//                    updateNodePort(rspFromAgent,tblVmInstance,VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESUMED);
                    tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    vmInstanceService.updateById(tblVmInstance);
                }
            }
            else
            {
                String destNodeIp = hypervisorNodeService.getById(tblVmInstance.getDestNodeId()).getManageIp();
                String nodeStoragePoolId = combRpcSerice.getStoragePoolService().createNodeStoragePool(destNodeIp, tblVmInstance.getStoragePoolId());
                if (StrUtil.isBlank(nodeStoragePoolId)) return;
                resumeVmInstance(tblVmInstance);
            }
//            getResumeResult(tblVmInstance);
        }
        else
        {
            log.info("vmInstanceId:{} ,get portsMigrateResult : null", tblVmInstance.getVmInstanceId());
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }

    }

    private void resumeVmInstance(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent rsp = getVmInstanceRspFromDestAgent(tblVmInstance);
        if (rsp.getStatus().equals(AgentConstant.GET_STATUS_FAILED) || !rsp.getAdded())
//                && !rsp.getPhase().equals(AgentConstant.RESUME_FAILED)))
        {
            String destNodeId = tblVmInstance.getDestNodeId();
            String destIp = hypervisorNodeService.getById(destNodeId).getManageIp();
            if (StrUtil.isBlank(destIp))
            {
                log.error("resume vm instance and get agent response error: dest node ip is null, instance id from agent {}", tblVmInstance.getVmInstanceId());
                return;
            }
            String phaseStatus = combRpcSerice.getStoragePoolService().getNodeStoragePoolPhaseStatus(destIp, tblVmInstance.getStoragePoolId());
            if (StrUtil.isBlank(phaseStatus))
            {
                log.info("node storage pool is creating, destNodeIp:{}", destIp);
                return;
            }
            if (phaseStatus.equals(AgentConstant.GET_STATUS_FAILED))
            {
                log.error("create node storage pool failed, destNodeIp:{}", destIp);
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
                return;
            }
            if (!phaseStatus.equals(AgentConstant.OK))
            {
                log.error("create node storage pool failed: unknown error, get nodeStoragePool status :{} , destNodeIp:{}", phaseStatus, destIp);
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
                return;
            }

            String url = "http://" + destIp + ":" + computeConfig.getVmAgentPort() + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent() + "/resume";
            String status = putHypervisorNode(url, "");
            if (null == status)
            {
                log.error("resume vm instance and get agent response error: instance id from agent {}", tblVmInstance.getVmInstanceId());
                return;
            }

            if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
            {
                log.error("resume vm instance and get agent response error:  instance id from agent {}, status {}", tblVmInstance.getVmInstanceId(), status);
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
            }
            else
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.GET_INSTANCE_RESUME_STATUS);
            }
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vmInstanceService.updateById(tblVmInstance);
            if (!ok)
            {
                log.error("update database err: result is 0, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            }
        }
    }

    private GetVmInstanceRspFromAgent getVmInstanceRspFromDestAgent(VmInstance tblVmInstance)
    {
        String destNodeId = tblVmInstance.getDestNodeId();
        String manageIp = hypervisorNodeService.getById(destNodeId).getManageIp();
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        if (null == vmAgentPort || 0 == vmAgentPort)
        {
            vmAgentPort = ComputeUrl.VM_AGENT_PORT;
        }
        String url = "http://" + manageIp + ":" + vmAgentPort + "/" + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
        return HttpActionUtil.getObject(url, GetVmInstanceRspFromAgent.class);
    }


    //    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
//    @Transactional(rollbackFor = Exception.class)
    public void getResumeResult(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent rsp = getVmInstanceRspFromDestAgent(tblVmInstance);
        boolean err = false;
        if (!rsp.getPhaseType().equals(AgentConstant.RESUME))
        {
            log.error("vmInstanceId: {}, get vm instance phaseType error: {}", tblVmInstance.getVmInstanceId(), rsp.getPhaseType());
            return;
        }
        log.info("vm instanceId: {},get vm instance phase: {}", tblVmInstance.getVmInstanceId(), rsp.getPhase());
        if (rsp.getPhase().equals(AgentConstant.SUCCESS))
        {
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESUMED);
//            updateNodePort(rsp, tblVmInstance,VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
            updatePciDeviceAndGroupByVmInstance(tblVmInstance);
        }
        else if (rsp.getPhase().equals(AgentConstant.PENDING_STATUS))
        {
            return;
        }
        else
        {
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_FAILED);
            err = true;
        }
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.error("vmInstanceId: {}, update database error", tblVmInstance.getVmInstanceId());
        }
        if (err)
        {
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 迁移虚机失败",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "迁移失败");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

    }

    //    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
    private String removeInstanceFromAgent(VmInstance tblVmInstance)
    {
        String manageUrl = getManageUrl(tblVmInstance);
        String snapUrl = manageUrl + ComputeUrl.V1_SNAP_URL;
        removeSnapsFromAgent(tblVmInstance, snapUrl);
        String vmUrl = manageUrl + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
        removeResource(vmUrl);
        return AgentConstant.PENDING_STATUS;

    }

    private void removeResource(String url)
    {
        BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);
        log.info("del url: {} ,result: {}", url, result);
        if (null == result)
        {
            log.error("url: {}, del error: null", url);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
        if (!AgentConstant.PENDING_STATUS.equals(result.getPhaseStatus()) &&
                !AgentConstant.GET_STATUS_FAILED.equals(result.getPhaseStatus()))
        {
            log.error("url: {}, del error: {}", url, result);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
        String reason = result.getReason();
        if (null != reason)
        {
            if (!reason.contains("not exist"))
            {
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
            }
        }
    }


    private void removeSnapsFromAgent(VmInstance tblVmInstance, String url)
    {
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmSnap::getPhaseStatus, REMOVED)
                .eq(VmSnap::getVmInstanceId, tblVmInstance.getVmInstanceId());
//        TblVmSnapExample example = new TblVmSnapExample();
//        TblVmSnapExample.Criteria criteria = example.createCriteria();
//        criteria.andPhaseStatusNotEqualTo(REMOVED);
//        criteria.andVmInstanceIdEqualTo(tblVmInstance.getVmInstanceId());
        List<VmSnap> snaps = vmSnapService.list(queryWrapper);
        List<String> snapIds = snaps.stream().map(VmSnap::getSnapIdFromAgent).filter(snapIdFromAgent ->
                null != snapIdFromAgent && !Objects.equals(snapIdFromAgent, "")
        ).collect(Collectors.toList());
        for (String snapId : snapIds)
        {
            String snapUrl = url + "/" + snapId;
            removeResource(snapUrl);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void UpdateVmInstanceRunning(VmInstance tblVmInstance)
    {
        try
        {
            if (!StrUtil.isBlank(tblVmInstance.getDestNodeId()) && !Objects.equals(tblVmInstance.getDestNodeId(), tblVmInstance.getNodeId()))
            {
                String nodeIp = hypervisorNodeService.getById(tblVmInstance.getDestNodeId()).getManageIp();
                String rpcResult = combRpcSerice.getVolumeService().setDestIp(tblVmInstance.getVmInstanceId(), nodeIp);
                if (!rpcResult.equals(tblVmInstance.getVmInstanceId()))
                {
                    return;
                }
                String managerIp = getManageIp(tblVmInstance);
                String url = "http://" + managerIp + ComputeUrl.LIBVERT_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
                removeMonitorTags(tblVmInstance, url);

                String removeGpuMonitorTagUrl = "http://" + managerIp + ComputeUrl.LIBVERT_EXPORTER_TAG_PORT_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
                removeGpuMonitorTags(tblVmInstance, removeGpuMonitorTagUrl);


                tblVmInstance.setLastNodeId(tblVmInstance.getNodeId());
                tblVmInstance.setNodeId(tblVmInstance.getDestNodeId());
            }
            else if (StrUtil.isBlank(tblVmInstance.getDestNodeId()))
            {
                String nodeIp = hypervisorNodeService.getById(tblVmInstance.getNodeId()).getManageIp();
                String rpcResult = combRpcSerice.getVolumeService().setDestIp(tblVmInstance.getVmInstanceId(), nodeIp);
                if (!rpcResult.equals(tblVmInstance.getVmInstanceId()))
                {
                    return;
                }
            }
            GetVmInstanceRspFromAgent rsp = getVmInstanceStatusFromAgent(tblVmInstance);

            if (AgentConstant.SHUT.equals(rsp.getPower()))
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWEROFF);
            }
            else if (AgentConstant.RUNNING.equals(rsp.getPower()))
            {
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RUNNING);
            }


            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            vmInstanceService.updateById(tblVmInstance);
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 虚机(云盘)迁移成功",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "迁移成功");
        }
        catch (Exception e)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePciDeviceAndGroupByVmInstance(VmInstance tblVmInstance)
    {
        if (StrUtil.isBlank(tblVmInstance.getDestNodeId()))
        {
            return;
        }
        GetVmInstanceRspFromAgent getVmInstanceRspFromAgent = getVmInstanceRspFromDestAgent(tblVmInstance);
        log.info("vmInstanceId: {} ,getVmInstanceRspFromAgent:{}", tblVmInstance.getVmInstanceId(), getVmInstanceRspFromAgent);
        List<String> gpuIds = getVmInstanceRspFromAgent.getGpus();
        if (null == gpuIds || gpuIds.isEmpty())
        {
            return;
        }

        // update pci device detach phase
        LambdaUpdateWrapper<PciDevice> updatePciDeviceWrapper = new LambdaUpdateWrapper<>();
        updatePciDeviceWrapper.eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId())
                .ne(PciDevice::getPhaseStatus, REMOVED);
        PciDevice tblPciDevice = new PciDevice();
        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHED);
        tblPciDevice.setUserId(tblVmInstance.getUserId());
        tblPciDevice.setVmInstanceId(null);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        pciDeviceService.update(tblPciDevice, updatePciDeviceWrapper);


        //update new pciDevice and pciDeviceGroup
        LambdaUpdateWrapper<PciDevice> newUpdatePciDeviceWrapper = new LambdaUpdateWrapper<>();
        newUpdatePciDeviceWrapper.in(PciDevice::getDeviceIdFromAgent, gpuIds)
                .ne(PciDevice::getPhaseStatus, REMOVED);
        List<PciDevice> tblPciDevices = pciDeviceService.list(newUpdatePciDeviceWrapper);
        if (null == tblPciDevices || tblPciDevices.isEmpty())
        {
            log.error("updatePciDeviceAndGroupByVmInstance: pciDevice is null");
            return;
        }
        PciDevice newTblPciDevice = new PciDevice();
        newTblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACHED);
        newTblPciDevice.setUserId(tblVmInstance.getUserId());
        newTblPciDevice.setVmInstanceId(tblVmInstance.getVmInstanceId());
        newTblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = pciDeviceService.update(newTblPciDevice, newUpdatePciDeviceWrapper);
        if (!ok)
        {
            log.error("updatePciDeviceAndGroupByVmInstance: update pciDevice failed");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.ERROR);
        }

    }

    public void resizeVmInstanceFromAgent(VmInstance tblVmInstance)
    {
        UpdateVmInstanceFromAgentReq updateVmInstanceFromAgentReq = new UpdateVmInstanceFromAgentReq();
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());

        resizeVmInstanceGpu(tblVmInstance, flavorInfo);
        if (null != tblVmInstance.getCpuCount() && tblVmInstance.getCpuCount() > 0)
        {
            updateVmInstanceFromAgentReq.setVcpu(tblVmInstance.getCpuCount());
        }
        else
        {
            updateVmInstanceFromAgentReq.setVcpu(flavorInfo.getCpu());
        }
        if (null != tblVmInstance.getMemSize() && tblVmInstance.getMemSize() > 0)
        {
            updateVmInstanceFromAgentReq.setMem(tblVmInstance.getMemSize());
        }
        else
        {
            updateVmInstanceFromAgentReq.setMem(flavorInfo.getMem());
        }
        updateVmInstanceFromAgentReq.setBootDevice(tblVmInstance.getBootDev());
        updateVmInstanceFromAgent(tblVmInstance, updateVmInstanceFromAgentReq);
    }

    //发现flavor 中GPU的数量和实际的GPU数量不一致，如果多于实际的不处理，由PciDeviceProcess处理，少于实际的则进行卸载
    public void resizeVmInstanceGpu(VmInstance tblVmInstance, FlavorService.FlavorInfo flavorInfo)
    {
        GetVmInstanceRspFromAgent getVmInstanceStatus = getVmInstanceStatusFromAgent(tblVmInstance);
        if (null != getVmInstanceStatus.getGpus() && getVmInstanceStatus.getGpus().size() > 0)
        {
            if (null == flavorInfo.getGpuCount()) flavorInfo.setGpuCount(0);

            if (getVmInstanceStatus.getGpus().size() <= flavorInfo.getGpuCount())
            {
                return;
            }
            int needDetachCount = getVmInstanceStatus.getGpus().size() - flavorInfo.getGpuCount();
            LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(PciDevice::getDeviceIdFromAgent, getVmInstanceStatus.getGpus())
                    .orderByDesc(PciDevice::getNodeId);
            List<PciDevice> tblPciDevices = pciDeviceService.list(queryWrapper);
            if (null == tblPciDevices || tblPciDevices.isEmpty())
            {
                return;
            }

            int execCount = 0;
            for (PciDevice tblPciDevice : tblPciDevices)
            {
                if (execCount >= needDetachCount)
                {
                    break;
                }
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHING);
                tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                boolean ok = pciDeviceService.updateById(tblPciDevice);
                if (ok)
                {
                    execCount++;
                }
            }

        }
    }


    public void switchBootDevFromAgent(VmInstance tblVmInstance)
    {
        UpdateVmInstanceFromAgentReq updateVmInstanceFromAgentReq = new UpdateVmInstanceFromAgentReq();
        updateVmInstanceFromAgentReq.setBootDevice(tblVmInstance.getBootDev());
        updateVmInstanceFromAgent(tblVmInstance, updateVmInstanceFromAgentReq);
    }

    public void updateVmInstanceFromAgent(VmInstance tblVmInstance, UpdateVmInstanceFromAgentReq req)
    {
        String nodeIp = getManageUrl(tblVmInstance);
        String url = nodeIp + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent() + "/modify";

        String vmReq = JsonUtil.objectToJson(req);
        BaseRsp result = HttpActionUtil.put(url, vmReq, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating vm instance :null,  vm instance id {}", tblVmInstance.getVmInstanceId());
            return;
        }

        String status = result.getStatus();

        //not pending status -> error
        if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
        {

            log.error("update vm error: vm instance id {}", tblVmInstance.getVmInstanceId());
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent修改虚机失败",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "修改失败");
        }
        else
        {
            //update phase status, instance id from agent
//            String uuid = (String) resultMap.get("uuid");
//            tblVmInstance.setInstanceIdFromAgent(uuid);
            boolean ok;
            if (VmInstanceStatus.INSTANCE_RESIZE_INIT == tblVmInstance.getPhaseStatus())
            {
                ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_UPDATED_STATUS);
            }
            else
            {
                ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.GET_INSTANCE_BOOT_DEV_STATUS);
            }
            if (!ok)
            {
                log.info("update database tbl_vm_instance error, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
                return;
            }
            log.info("updating vm instance, : vm instance id {}", tblVmInstance.getVmInstanceId());
            logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent修改虚机",
                    String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "修改中");
        }
    }

    public void cleanCreateFailedVmInstance(VmInstance tblVmInstance)
    {
        removePort(tblVmInstance);
        deactiveGpuPartition(tblVmInstance);
        LambdaQueryWrapper<DiskInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(DiskInfo::getVolumeId).eq(DiskInfo::getVmInstanceId, tblVmInstance.getVmInstanceId())
                .ne(DiskInfo::getPhaseStatus, REMOVED)
                .isNotNull(DiskInfo::getVolumeId)
                .eq(DiskInfo::getIsNew, true);
        List<String> volumeIds = diskInfoService.list(queryWrapper).stream().map(DiskInfo::getVolumeId).collect(Collectors.toList());
        if (!volumeIds.isEmpty())
        {
            combRpcSerice.getVolumeService().removeDataVolume(volumeIds);
        }
        if (StrUtil.isBlank(tblVmInstance.getInstanceIdFromAgent()))
        {
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANED);
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vmInstanceService.updateById(tblVmInstance);
            if (ok)
            {
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "清理创建失败的虚机资源",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "清理成功");
            }
            return;
        }
        String url = getManageUrl(tblVmInstance) + ComputeUrl.V1_VM_URL + "/" + tblVmInstance.getInstanceIdFromAgent();
        removeResource(url);
        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANING);
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        vmInstanceService.updateById(tblVmInstance);
        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "清理创建失败的虚机资源",
                String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "清理中");
    }

    public void getCleanCreateFailedVmInstanceResult(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent rsp = getVmInstanceStatusFromAgent(tblVmInstance);
        log.info("vm instanceId: {},get vm instance phase: {}", tblVmInstance.getVmInstanceId(), rsp.getPhase());
        if (rsp.getStatus().equals(AgentConstant.FAILED) && rsp.getReason().equals(AgentConstant.VM_NOT_EXIST))
        {
            boolean ok = updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANED);
            if (ok)
            {
                LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId())
                        .ne(PciDevice::getPhaseStatus, REMOVED);
                List<PciDevice> pciDevices = pciDeviceService.list(queryWrapper);
                if (!pciDevices.isEmpty())
                {
                    pciDevices.forEach(pciDevice ->
                    {
                        pciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHED);
                        pciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                        pciDevice.setVmInstanceId(null);
                        pciDeviceService.updateById(pciDevice);
                    });
                }
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "清理创建失败的虚机资源",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "清理成功");
            }
        }
    }

    public void checkUpdateVmInstanceResult(VmInstance tblVmInstance)
    {
        GetVmInstanceRspFromAgent getVmInstanceStatus = getVmInstanceStatusFromAgent(tblVmInstance);
        log.info("get vm instance status form agent : {} , vm instance id:{}", getVmInstanceStatus, tblVmInstance.getVmInstanceId());

        if (null == getVmInstanceStatus)
        {
            return;
        }

        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());

        if (null != tblVmInstance.getCpuCount() && tblVmInstance.getCpuCount() > 0)
        {
            flavorInfo.setCpu(tblVmInstance.getCpuCount());
        }
        if (null != tblVmInstance.getMemSize() && tblVmInstance.getMemSize() > 0)
        {
            flavorInfo.setMem(tblVmInstance.getMemSize());
        }

        if (!(Objects.equals(getVmInstanceStatus.getCpuCount(), flavorInfo.getCpu().toString()) &&
                Objects.equals(getVmInstanceStatus.getMemSize(), flavorInfo.getMem().toString()) &&
                (VmInstanceStatus.GET_INSTANCE_BOOT_DEV_STATUS == tblVmInstance.getPhaseStatus()
                        || VmInstanceStatus.GET_INSTANCE_UPDATED_STATUS == tblVmInstance.getPhaseStatus()
                )))
        {
            return;
        }


        if (getVmInstanceStatus.getPower().equals(AgentConstant.RUNNING))
        {
            updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_RUNNING);
        }
        else
        {
            updateVmInstancePhaseStatus(tblVmInstance, VmInstanceStatus.INSTANCE_POWEROFF);
        }
        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent修改虚机",
                String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "修改成功");
    }

    public List<PciDevice> getGpuWithPartition(VmInstance tblVmInstance)
    {
        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId())
                .ne(PciDevice::getPhaseStatus, REMOVED)
                .isNotNull(PciDevice::getPartitionId);
        if (pciDeviceService.count(queryWrapper) == 0)
        {
            log.info("no gpu partition need to deactive, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            return null;
        }
        return pciDeviceService.list(queryWrapper);
    }

    public void deactiveGpuPartition(VmInstance tblVmInstance)
    {
        try
        {
            List<PciDevice> pciDevices = getGpuWithPartition(tblVmInstance);
            if (null == pciDevices)
            {
                return;
            }
            // distinct partitionIds
            List<String> partitionIds = pciDevices.stream().map(PciDevice::getPartitionId).distinct().collect(Collectors.toList());
            String partitionId = partitionIds.get(0);
            String url = getGpuUrl(tblVmInstance);
            url = url + "/deactive";
            GpuPartitionDeactiveReq req = new GpuPartitionDeactiveReq();
            req.setPartitionId(partitionId);
            String putArgs = JsonUtil.objectToJson(req);
            String result = HttpActionUtil.put(url, putArgs);
            if (null == result)
            {
                log.error("deactive gpu partition failed, url: {}, args: {}", url, putArgs);
                return;
            }
            log.info("deactive gpu partition , vmInstanceId:{}, url: {}, args: {}, result: {}", tblVmInstance.getVmInstanceId(), url, putArgs, result);
            GpuPartitionDeactiveRsp rsp = JsonUtil.jsonToPojo(result, GpuPartitionDeactiveRsp.class);
            if (null == rsp)
            {
                log.error("deactive gpu partition failed, url: {}, args: {}, result: {}", url, putArgs, result);
                return;
            }
            if (StrUtil.isNotBlank(rsp.getReason()))
            {
                log.error("deactive gpu partition failed, url: {}, args: {}, result: {}", url, putArgs, result);
                return;
            }
            if (partitionId.equals(rsp.getPartitionId()))
            {
                pciDevices.forEach(pciDevice ->
                {
//                    pciDevice.setVmInstanceId(tblVmInstance.getVmInstanceId());
                    pciDevice.setPartitionId(null);
                    pciDeviceService.updateById(pciDevice);
                });
                log.info("deactive gpu partition success, url: {}, args: {}, result: {}", url, putArgs, result);
            }
            else
            {
                log.error("deactive gpu partition failed, url: {}, args: {}, result: {}", url, putArgs, result);
            }
        }
        catch (Exception e)
        {
            log.error("deactive gpu partition failed, vmInstanceId: {}", tblVmInstance.getVmInstanceId(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resizeGpuPartition(VmInstance tblVmInstance, FlavorService.FlavorInfo newFlavor)
    {
        List<PciDevice> pciDevices = getGpuWithPartition(tblVmInstance);
        if (null == pciDevices)
        {
            return;
        }
        Integer gpuCount = newFlavor.getGpuCount();
        boolean needIb = newFlavor.getNeedIb() != null && newFlavor.getNeedIb();
        GpuPartitionActiveReq req = new GpuPartitionActiveReq();
        req.setNum(gpuCount);
        req.setPartitionId(pciDevices.get(0).getPartitionId());
        req.setNeedIb(needIb);
        String url = getGpuUrl(tblVmInstance);
        url = url + "/active";
        String resultId = vmScheduler.activeGpuPartition(req, url, tblVmInstance.getNodeId(), tblVmInstance.getVmInstanceId(), VmInstanceStatus.DEVICE_ATTACHING);
        if (null == resultId)
        {
            log.error("resize gpu partition failed, url: {}, args: {}", url, JsonUtil.objectToJson(req));
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }
        Set<String> beforePartitionIds = pciDevices.stream().map(PciDevice::getDeviceId).collect(Collectors.toSet());
        List<PciDevice> afterPciDevices = getGpuWithPartition(tblVmInstance);
        Set<String> afterPartitionIds = afterPciDevices.stream().map(PciDevice::getDeviceId).collect(Collectors.toSet());
        // beforePartitionIds - afterPartitionIds 差集，即需要修改的pciDevice, 更新phaseStatus
        beforePartitionIds.removeAll(afterPartitionIds);
        if (beforePartitionIds.size() == pciDevices.size() || beforePartitionIds.size() == 0)
        {
            log.info("resize gpu partition,only need update phaseStatus ATTACHING, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            return;
        }
        LambdaUpdateWrapper<PciDevice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(PciDevice::getPartitionId, beforePartitionIds)
                .ne(PciDevice::getPhaseStatus, REMOVED);
        PciDevice updatePciDevice = new PciDevice();
        updatePciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        updatePciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHING);
        boolean ok = pciDeviceService.update(updatePciDevice, updateWrapper);
        if (!ok)
        {
            log.error("resize gpu partition failed, update phaseStatus failed, vmInstanceId: {}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
    }

    public String getGpuUrl(VmInstance tblVmInstance)
    {
        HypervisorNode node = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == node || REMOVED == node.getPhaseStatus()) return null;
        String managerIp = node.getManageIp();
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        if (null == vmAgentPort || 0 == vmAgentPort)
        {
            vmAgentPort = ComputeUrl.VM_AGENT_PORT;
        }
        return "http://" + managerIp + ":" + vmAgentPort + ComputeUrl.V1_GPU_URL;
    }


    @Data
    @JsonIgnoreProperties
    static class UpdateVmInstanceFromAgentReq
    {
        int mem;

        int vcpu;

        @JsonProperty(value = "bootdev")
        String bootDevice;
    }


    @Data
    @JsonIgnoreProperties
    static class AddVmInstanceFromAgentReq
    {
        //        String image;
//        String flavor;
        //unit GB
        Integer mem;
        Integer vcpu;
        List<String> vols;
        List<String> ports;
        List<String> gpus;
        //subnet cidr
        //vpc cidr
//        List<String> disk;
//        String os;
//
//        Boolean migrate; //should be true
    }

    @Data
    @JsonIgnoreProperties
    public static class GetVmInstanceRspFromAgent
    {
        @JsonProperty(value = "phase_type")
        String phaseType;

        @JsonProperty(value = "cloudinit")
        CloudinitMetas cloudinitMetas;

        //        @JsonProperty(value = "cloudinit_phase")
//        String cloudinitPhase;
        @JsonProperty(value = "vcpu")
        String cpuCount;
        @JsonProperty(value = "mem")
        String memSize;
        @JsonProperty(value = "bootdev")
        String bootDevice;

        @JsonProperty(value = "ports")
        List<String> portIds;

        @JsonProperty(value = "phase_status")
        String phase;

        String status;

        @JsonProperty(value = "uuid")
        String vmInstanceIdFromAgent;


        //        List<String> vncport;
//        List<String> sgUuids;
        List<String> gpus;

        String reason;

        String power;

        @JsonProperty(value = "added")
        Boolean added;

    }

    /*
    "os": os,
                    "hostname": hostname,
                    "username": username,
                    "password": password,
                    "pubkey": pubkey,
                    "subnets": subnets,
                    "ipmodes": ipmodes,
                    "instance_id": "".join(random.sample(string.hexdigits, 16)).upper(),
     */
    @Data
    @JsonIgnoreProperties
    static class CloudinitMetas
    {
        String os;
        String hostname;
        String username;
        String password;
        String pubkey;
        @JsonProperty(value = "instance_id")
        String instanceId;

        Boolean done;
    }
}
