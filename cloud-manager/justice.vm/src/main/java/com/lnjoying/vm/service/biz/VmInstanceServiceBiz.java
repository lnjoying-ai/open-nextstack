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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.entity.search.PageSearchCritical;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.justice.schema.service.repo.FlavorService.FlavorInfo;
import com.lnjoying.justice.schema.service.repo.ImageService;
import com.lnjoying.justice.schema.service.repo.VolumeService;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ImageOsType;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.response.IsoIsInjectedRsp;
import com.lnjoying.vm.domain.dto.request.*;
import com.lnjoying.vm.domain.dto.response.*;
import com.lnjoying.vm.entity.*;
import com.lnjoying.vm.entity.search.PciDeviceSearchCritical;
import com.lnjoying.vm.entity.search.VmInstanceSearchCritical;
import com.lnjoying.vm.mapper.DeviceInfoMapper;
import com.lnjoying.vm.mapper.HypervisorNodeAllocationMapper;
import com.lnjoying.vm.processor.VmInstanceTimerProcessor;
import com.lnjoying.vm.processor.VmScheduler;
import com.lnjoying.vm.service.*;
import com.micro.core.common.Utils;
import com.micro.core.utils.AesCryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.jruby.ext.socket.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service
@Slf4j
public class VmInstanceServiceBiz
{
    @Autowired
    CombRpcSerice combRpcSerice;

    @Autowired
    ComputeConfig computeConfig;

    @Autowired
    InstanceNetworkRefService instanceNetworkRefService;

    @Autowired
    VmInstanceService vmInstanceService;

    @Autowired
    DiskInfoService diskInfoService;

    @Autowired
    PciDeviceService pciDeviceService;

    @Autowired
    VmScheduler vmScheduler;

//    @Autowired
//    PciDeviceGroupService pciDeviceGroupService;

    @Autowired
    HypervisorNodeService hypervisorNodeService;

    @Autowired
    VmSnapService vmSnapService;

    @Autowired
    ResourceStatsService resourceStatsService;

    @Resource
    DeviceInfoMapper deviceInfoMapper;

    @Autowired
    LogRpcService logRpcSerice;

    @Autowired
    VmInstanceTimerProcessor vmInstanceTimerProcessor;

    @Autowired
    LogRpcService logRpcService;

    @Resource
    HypervisorNodeAllocationMapper hypervisorNodeAllocationMapper;

    @Autowired
    PciDeviceServiceBiz pciDeviceServiceBiz;

    @Autowired
    VmSnapServiceBiz vmSnapServiceBiz;


    //add a vm instance
    @Transactional(rollbackFor = {Exception.class})
    public VmInstanceBaseRsp addVmInstance(VmInstanceCreateReq addVmInstanceReq, String userId) throws WebSystemException
    {
        if (0 == addVmInstanceReq.getNetworkInfos().size() ||
                StrUtil.isBlank(addVmInstanceReq.getFlavorId()))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }


        VmInstance tblVmInstance = new VmInstance();
        tblVmInstance.setVmInstanceId(Utils.assignUUId());
        tblVmInstance.setImageId(addVmInstanceReq.getImageId());
        tblVmInstance.setFlavorId(addVmInstanceReq.getFlavorId());
        if (StrUtil.isBlank(addVmInstanceReq.getHostname()))
        {
            tblVmInstance.setHostName("instance-" + Utils.assignUUId().substring(0, 8));
//            tblVmInstance.setHostName(addVmInstanceReq.getName());
//            if (containsChineseCharacters(addVmInstanceReq.getName()))
//            {
//
//            }
        }
        else
        {
            tblVmInstance.setHostName(addVmInstanceReq.getHostname());
        }
        tblVmInstance.setSysUsername(addVmInstanceReq.getSysUsername());
        tblVmInstance.setSysPassword(AesCryptoUtils.encryptHex(addVmInstanceReq.getSysPassword()));
        tblVmInstance.setPubkeyId(addVmInstanceReq.getPubkeyId());
        tblVmInstance.setName(addVmInstanceReq.getName());
        tblVmInstance.setDescription(addVmInstanceReq.getDescription());
        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_INIT);
        tblVmInstance.setUserId(userId);
        tblVmInstance.setCmpTenantId(addVmInstanceReq.getCmpTenantId());
        tblVmInstance.setCmpUserId(addVmInstanceReq.getCmpUserId());
        tblVmInstance.setStoragePoolId(addVmInstanceReq.getStoragePoolId());
        tblVmInstance.setNodeId(addVmInstanceReq.getNodeId());
        tblVmInstance.setRootDisk(addVmInstanceReq.getRootDisk());
        if (StrUtil.isNotBlank(addVmInstanceReq.getNodeId()))
        {
            HypervisorNode node = hypervisorNodeService.getById(addVmInstanceReq.getNodeId());
            if (null == node || REMOVED == node.getPhaseStatus() || VmInstanceStatus.HYPERVISOR_NODE_OFFLINE == node.getPhaseStatus())
            {
                throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
            }
        }
        tblVmInstance.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVmInstance.setUpdateTime(tblVmInstance.getCreateTime());
        if (ImageOsType.LINUX == addVmInstanceReq.getImageOsType())
        {
            tblVmInstance.setOsType("linux");
        }
        else
        {
            tblVmInstance.setOsType("windows");
        }
        if (StrUtil.isNotBlank(addVmInstanceReq.getEipId()))
        {
            tblVmInstance.setEipId(addVmInstanceReq.getEipId());
        }

        tblVmInstance = addNetworkNics(tblVmInstance, addVmInstanceReq.getNetworkInfos());

        boolean ok = vmInstanceService.save(tblVmInstance);
        if (!ok)
        {
            log.error("add vm instance failed: instance name {}, update database error.", addVmInstanceReq.getName());
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        if (null != addVmInstanceReq.getDiskInfos())
        {
            if (addVmInstanceReq.getDiskInfos().size() > 0)
            {
                addDisks(addVmInstanceReq.getDiskInfos(), tblVmInstance.getVmInstanceId(), tblVmInstance.getName());
            }
        }
//        if (addVmInstanceReq.getGpuIds().size() > 0)
//        {
//            setPciDevices(addVmInstanceReq.getGpuIds(), tblVmInstance.getVmInstanceId(), userId);
//        }

        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(userId).getUserName();
        String desc = StrUtil.format("创建虚拟机【id：{}，名称：{}，镜像id：{}，规格id：{}，根盘：{}，磁盘信息：{}，网络信息：{}，主机名：{}，存储池id：{}】", tblVmInstance.getVmInstanceId(), addVmInstanceReq.getName(), addVmInstanceReq.getImageId(), addVmInstanceReq.getFlavorId(), addVmInstanceReq.getRootDisk(), addVmInstanceReq.getDiskInfos(), addVmInstanceReq.getNetworkInfos(), addVmInstanceReq.getHostname(), addVmInstanceReq.getStoragePoolId());
        logRpcSerice.getLogService().addLog(userId, userName, "计算-虚拟机", desc);


//        Map<String, String> retValue = new HashMap<>();
//        retValue.put("instanceId", tblVmInstance.getVmInstanceId());
        return vmInstanceBoundSgs(tblVmInstance, addVmInstanceReq.getSgIds());
    }

    private boolean containsChineseCharacters(String input)
    {
        String regex = ".*[\\u4e00-\\u9fa5]+.*"; // 正则表达式匹配包含中文字符的字符串
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    @Async
    public void addVmInstances(VmInstanceCreateReq addVmInstanceReq, Integer count, String userId)
    {
        checkAddVmInstanceReq(addVmInstanceReq);
        if (count == 1)
        {
            addVmInstance(addVmInstanceReq, userId);
        }
        String instanceName = addVmInstanceReq.getName();
        String hostName = addVmInstanceReq.getHostname();
        if (StrUtil.isBlank(hostName))
        {
            hostName = instanceName;
        }
        for (int i = 0; i < count; i++)
        {
            int index = i + 1;
            addVmInstanceReq.setName(instanceName + "-" + index);
            addVmInstanceReq.setHostname(hostName + "-" + index);
            addVmInstance(addVmInstanceReq, userId);
        }
    }

    // check addVmInstanceReq
    public void checkAddVmInstanceReq(VmInstanceCreateReq addVmInstanceReq)
    {
        if (0 == addVmInstanceReq.getNetworkInfos().size() ||
                StrUtil.isBlank(addVmInstanceReq.getFlavorId()))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        if (null == addVmInstanceReq.getDiskInfos() || addVmInstanceReq.getDiskInfos().size() == 0) return;
        addVmInstanceReq.getDiskInfos().forEach(diskInfo ->
        {
            // diskInfo.getVolumeId()不为空或者diskInfo.getSize()<0 则抛异常
            if (StrUtil.isNotBlank(diskInfo.getVolumeId()) || diskInfo.getSize() < 0)
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
        });
    }


    @Transactional(rollbackFor = Exception.class)
    public VmInstanceBaseRsp addVmInstance(VmInstanceRenewReq vmInstanceRenewReq, String userId)
    {
        if (0 == vmInstanceRenewReq.getNetworkInfos().size() ||
                StrUtil.isBlank(vmInstanceRenewReq.getFlavorId()) ||
                0 == vmInstanceRenewReq.getDiskInfos().size())
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }

        VmInstance tblVmInstance = new VmInstance();
        tblVmInstance.setVmInstanceId(Utils.assignUUId());
        tblVmInstance.setFlavorId(vmInstanceRenewReq.getFlavorId());
        tblVmInstance.setHostName(vmInstanceRenewReq.getHostname());
        tblVmInstance.setSysUsername(vmInstanceRenewReq.getSysUsername());
        tblVmInstance.setSysPassword(AesCryptoUtils.encryptHex(vmInstanceRenewReq.getSysPassword()));
        tblVmInstance.setPubkeyId(vmInstanceRenewReq.getPubkeyId());
        tblVmInstance.setName(vmInstanceRenewReq.getName());
        tblVmInstance.setDescription(vmInstanceRenewReq.getDescription());
        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_INIT);
        tblVmInstance.setUserId(userId);
        tblVmInstance.setNodeId(vmInstanceRenewReq.getNodeId());
        tblVmInstance.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVmInstance.setUpdateTime(tblVmInstance.getCreateTime());
        tblVmInstance.setImageId("");
        tblVmInstance.setOsType("linux");
        tblVmInstance.setVolumeId(vmInstanceRenewReq.getDiskInfos().get(0).getVolumeId());
        tblVmInstance = addNetworkNics(tblVmInstance, vmInstanceRenewReq.getNetworkInfos());
        tblVmInstance.setStoragePoolId(vmInstanceRenewReq.getStoragePoolId());

//        List<String> volumeIds = vmInstanceRenewReq.getDiskInfos();
        List<VmInstanceCreateReq.DiskInfo> diskInfos = vmInstanceRenewReq.getDiskInfos();

        //删除第一个元素
        diskInfos.remove(0);

        VolumeService.ImageInfo imageInfo = combRpcSerice.getVolumeService().getImageInfoByRecycleVolumeId(tblVmInstance.getVolumeId());
        if (null != imageInfo)
        {
            if (StrUtil.isNotBlank(imageInfo.getImageId()))
                tblVmInstance.setImageId(imageInfo.getImageId());
            if (StrUtil.isNotBlank(imageInfo.getOsType()))
                tblVmInstance.setOsType(imageInfo.getOsType());
        }
        VolumeService.VolumeInfo volumeInfo = combRpcSerice.getVolumeService().setRootDiskAttached(userId, tblVmInstance.getVolumeId());
        if (null == volumeInfo || null == volumeInfo.getSize())
        {
            log.error("add vm instance failed: instance name {}, reset root disk error.", vmInstanceRenewReq.getName());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        if (!volumeInfo.getVolumeId().equals(tblVmInstance.getVolumeId()))
        {
            log.error("add vm instance failed: instance name {}, reset root disk error.", vmInstanceRenewReq.getName());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        tblVmInstance.setRootDisk(volumeInfo.getSize());
//        else throw new WebSystemException(ErrorCode.PARAM_ERROR,ErrorLevel.INFO);
        boolean ok = vmInstanceService.save(tblVmInstance);
        if (!ok)
        {
            log.error("add vm instance failed: instance name {}, update database error.", vmInstanceRenewReq.getName());
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        if (diskInfos.size() > 0)
        {
            addDisks(diskInfos, tblVmInstance.getVmInstanceId(), tblVmInstance.getName());
        }
        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(userId).getUserName();
        String desc = StrUtil.format("恢复虚拟机【名称：{}，规格id：{}，磁盘信息：{}，网络信息：{}，主机名：{}，存储池id：{}】", vmInstanceRenewReq.getName(), vmInstanceRenewReq.getFlavorId(), vmInstanceRenewReq.getDiskInfos(), vmInstanceRenewReq.getNetworkInfos(), vmInstanceRenewReq.getHostname(), vmInstanceRenewReq.getStoragePoolId());
        logRpcSerice.getLogService().addLog(userId, userName, "计算-虚拟机", desc);


        return vmInstanceBoundSgs(tblVmInstance, vmInstanceRenewReq.getSgIds());
    }

    @Transactional
    public VmInstance addNetworkNics(VmInstance tblVmInstance, List<VmInstanceCreateReq.NetworkInfo> networkInfos)
    {
        int size = networkInfos.size();
        for (int i = 0; i < size; i++)
        {
            VmInstanceCreateReq.NetworkInfo networkInfo = networkInfos.get(i);
            if (null != networkInfo.getStaticIp() && !networkInfo.getStaticIp().isEmpty())
            {
                if (!staticIpIsValid(networkInfo.getStaticIp(), networkInfo.getSubnetId()))
                {
                    log.error("static ip is not valid,staticIp:{},subnetId:{}", networkInfo.getStaticIp(), networkInfo.getSubnetId());
                    throw new WebSystemException(ErrorCode.STATIC_IP_INVALID, ErrorLevel.INFO);
                }
                if (combRpcSerice.getNetworkService().isIpInUse(networkInfo.getSubnetId(), networkInfo.getStaticIp()))
                {
                    throw new WebSystemException(ErrorCode.STATIC_IP_IS_OCCUPIED, ErrorLevel.INFO);
                }
            }
            if (0 == i)
            {
                if (null != networkInfo.getIsVip() && networkInfo.getIsVip())
                {
                    log.error("the first nic should not be a vip port.");
                    throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
                }
                NetworkService.Vpc vpc = combRpcSerice.getNetworkService().getVpc(networkInfo.getVpcId());
                if (null == vpc)
                {
                    log.error("vpc is not exist, vpcId:{}", networkInfo.getVpcId());
                    throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
                }
                NetworkService.Subnet subnet = combRpcSerice.getNetworkService().getSubnet(networkInfo.getSubnetId());
                if (null == subnet)
                {
                    log.error("subnet is not exist, subnetId:{}", networkInfo.getSubnetId());
                    throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
                }
                tblVmInstance.setVpcId(networkInfos.get(i).getVpcId());
                tblVmInstance.setSubnetId(networkInfos.get(i).getSubnetId());
                tblVmInstance.setStaticIp(networkInfo.getStaticIp());
            }
            else
            {
                InstanceNetworkRef tblInstanceNetworkRef = new InstanceNetworkRef();
                tblInstanceNetworkRef.setInstanceNetworkId(Utils.assignUUId());
                tblInstanceNetworkRef.setVpcId(networkInfo.getVpcId());
                tblInstanceNetworkRef.setSubnetId(networkInfo.getSubnetId());
                tblInstanceNetworkRef.setStaticIp(networkInfo.getStaticIp());
                tblInstanceNetworkRef.setInstanceType(VmInstanceStatus.VM);
                tblInstanceNetworkRef.setInstanceId(tblVmInstance.getVmInstanceId());
                tblInstanceNetworkRef.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATED);
                if (null == networkInfo.getIsVip())
                {
                    tblInstanceNetworkRef.setIsVip(false);
                }
                else
                {
                    tblInstanceNetworkRef.setIsVip(networkInfo.getIsVip());
                }
                tblInstanceNetworkRef.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                tblInstanceNetworkRef.setUpdateTime(tblInstanceNetworkRef.getCreateTime());
                boolean ok = instanceNetworkRefService.save(tblInstanceNetworkRef);
                if (!ok)
                {
                    log.info("insert tblInstanceNetworkRef error, networkInfo: {}", networkInfo);
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            }
        }
        return tblVmInstance;
    }

    private VmInstanceBaseRsp vmInstanceBoundSgs(VmInstance tblVmInstance, List<String> sgIds)
    {
        String returnString = combRpcSerice.getNetworkService().vmInstanceBoundSgs(sgIds, tblVmInstance.getVmInstanceId());
        if ("ok".equals(returnString))
        {
            VmInstanceBaseRsp rsp = new VmInstanceBaseRsp();
            rsp.setInstanceId(tblVmInstance.getVmInstanceId());
            return rsp;
        }
        else
        {
            if (!StrUtil.isBlank(tblVmInstance.getVolumeId()))
                combRpcSerice.getVolumeService().setRootDiskDetached(tblVmInstance.getVolumeId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }


    //data disk
    @Transactional(rollbackFor = Exception.class)
    public void addDiskVolumeIds(List<String> volumeIds, String vmInstanceId, int start) throws WebSystemException
    {
        if (start >= volumeIds.size())
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        for (int i = start; i < volumeIds.size(); i++)
        {
            DiskInfo tblDiskInfo = new DiskInfo();


            if (StrUtil.isBlank(volumeIds.get(i))) continue;
            if (!combRpcSerice.getVolumeService().isDetached(volumeIds.get(i)))
            {
                throw new WebSystemException(ErrorCode.VOLUME_ALREADY_ATTACHED, ErrorLevel.INFO);
            }
            tblDiskInfo.setVmInstanceId(vmInstanceId);
            tblDiskInfo.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATED);
            tblDiskInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblDiskInfo.setUpdateTime(tblDiskInfo.getCreateTime());
            tblDiskInfo.setVolumeId(volumeIds.get(i));
            tblDiskInfo.setIsNew(false);
            boolean ok = diskInfoService.save(tblDiskInfo);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
    }

    //attach volumes
    @Transactional(rollbackFor = Exception.class)
    public VmInstanceBaseRsp attachVolumes(List<String> dataDiskIds, String vmInstanceId) throws WebSystemException
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (VmInstanceStatus.INSTANCE_MIGRATE_INIT <= tblVmInstance.getPhaseStatus() && VmInstanceStatus.INSTANCE_RESUMED >= tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_IS_MIGRATING, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<DiskInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DiskInfo::getVolumeId, dataDiskIds)
                .ne(DiskInfo::getPhaseStatus, REMOVED);
        if (diskInfoService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.VOLUME_ALREADY_ATTACHED, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<DiskInfo> countQueryWrapper = new LambdaQueryWrapper<>();
        countQueryWrapper.eq(DiskInfo::getVmInstanceId, vmInstanceId)
                .ne(DiskInfo::getPhaseStatus, REMOVED);
        if (diskInfoService.count(countQueryWrapper) + dataDiskIds.size() > AgentConstant.MAX_DATA_DISK_NUM)
        {
            throw new WebSystemException(ErrorCode.VOLUME_COUNT_EXCEED, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VmSnap> vmQueryWrapper = new LambdaQueryWrapper<>();
        vmQueryWrapper.eq(VmSnap::getVmInstanceId, vmInstanceId)
                .ne(VmSnap::getPhaseStatus, REMOVED);
        if (vmSnapService.count(vmQueryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.VM_HAS_SNAPS, ErrorLevel.INFO);
        }
        addDiskVolumeIds(dataDiskIds, vmInstanceId, 0);

        String nodeId = tblVmInstance.getNodeId();
        String nodeIp = hypervisorNodeService.getById(nodeId).getManageIp();
        List<String> volumeIds = combRpcSerice.getVolumeService().attachVolumes(dataDiskIds, vmInstanceId, nodeIp);
        if (volumeIds.size() != dataDiskIds.size())
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        for (String volumeId : volumeIds)
        {
            if (StrUtil.isBlank(volumeId))
            {
                throw new WebSystemException(ErrorCode.OPERATION_RETRY, ErrorLevel.INFO);
            }
        }
        VmInstanceBaseRsp rsp = new VmInstanceBaseRsp();
        rsp.setInstanceId(vmInstanceId);
        return rsp;
    }

    //data disk
    @Transactional(rollbackFor = Exception.class)
    public void addDisks(List<VmInstanceCreateReq.DiskInfo> diskInfos, String vmInstanceId, String vmInstanceName) throws WebSystemException
    {
        if (diskInfos.size() > AgentConstant.MAX_DATA_DISK_NUM)
        {
            throw new WebSystemException(ErrorCode.VOLUME_ATTACHED_TOO_MANY, ErrorLevel.INFO);
        }

        int i = 0;
        for (VmInstanceCreateReq.DiskInfo diskInfo : diskInfos)
        {
            DiskInfo tblDiskInfo = new DiskInfo();
            tblDiskInfo.setDiskId(Utils.assignUUId());

            if (!StrUtil.isBlank(diskInfo.getVolumeId()))
            {
                if (!combRpcSerice.getVolumeService().isDetached(diskInfo.getVolumeId()))
                {
                    throw new WebSystemException(ErrorCode.VOLUME_ALREADY_ATTACHED, ErrorLevel.INFO);
                }
                tblDiskInfo.setVolumeId(diskInfo.getVolumeId());
                tblDiskInfo.setIsNew(false);
            }
            else
            {
                tblDiskInfo.setDiskType(diskInfo.getDiskType());
                if (null != diskInfo.getSize() &&
                        diskInfo.getSize() > AgentConstant.DISK_MAX_SIZE)
                {
                    throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
                }
                tblDiskInfo.setSize(diskInfo.getSize());
                int diskIndex = i++;
                String volumeName = vmInstanceName + "-data-" + diskIndex + "-" + diskInfo.getSize() + "GB";
                tblDiskInfo.setName(volumeName);
                tblDiskInfo.setIsNew(true);
            }
            tblDiskInfo.setVmInstanceId(vmInstanceId);
            tblDiskInfo.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATED);
            tblDiskInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblDiskInfo.setUpdateTime(tblDiskInfo.getCreateTime());
            boolean ok = diskInfoService.save(tblDiskInfo);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }

    }

    //get vm instance
    public VmInstancesRsp getVmInfos(VmInstanceSearchCritical vmInstanceSearchCritical, String userId) throws WebSystemException
    {
        List<String> eipMapPorts;
        List<VmInstance> vmInstancesFromNetworkRef = new ArrayList<>();
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmInstance::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(VmInstance::getUserId, userId);
        }
        if (!StrUtil.isBlank(vmInstanceSearchCritical.getVmInstanceId()))
        {
            queryWrapper.eq(VmInstance::getVmInstanceId, vmInstanceSearchCritical.getVmInstanceId());
        }
        else if (!StrUtil.isBlank(vmInstanceSearchCritical.getName()))
        {
            queryWrapper.like(VmInstance::getName, vmInstanceSearchCritical.getName());
        }
        if (null != vmInstanceSearchCritical.getPortIdIsNull() && vmInstanceSearchCritical.getPortIdIsNull())
        {
            queryWrapper.isNull(VmInstance::getPortId);
        }
        else if (null != vmInstanceSearchCritical.getPortIdIsNull() && !vmInstanceSearchCritical.getPortIdIsNull())
        {
            queryWrapper.isNotNull(VmInstance::getPortId);
        }
        if (null != vmInstanceSearchCritical.getSubnetId() && !vmInstanceSearchCritical.getSubnetId().isEmpty())
        {
            queryWrapper.eq(VmInstance::getSubnetId, vmInstanceSearchCritical.getSubnetId());
            vmInstancesFromNetworkRef = getVmInstancesBySubnetId(vmInstanceSearchCritical.getSubnetId());
        }
        if (!StrUtil.isBlank(vmInstanceSearchCritical.getInstanceGroupId()))
        {
            queryWrapper.eq(VmInstance::getInstanceGroupId, vmInstanceSearchCritical.getInstanceGroupId());
        }
        else if (null != vmInstanceSearchCritical.getInstanceGroupIdIsNull() && vmInstanceSearchCritical.getInstanceGroupIdIsNull())
        {
            queryWrapper.and(wrapper -> wrapper.isNull(VmInstance::getInstanceGroupId)
                    .or().eq(VmInstance::getInstanceGroupId, ""));
        }
        else if (null != vmInstanceSearchCritical.getInstanceGroupIdIsNull() && !vmInstanceSearchCritical.getInstanceGroupIdIsNull())
        {
            queryWrapper.isNotNull(VmInstance::getInstanceGroupId);
        }
        if (null != vmInstanceSearchCritical.getEipIdIsNull() && vmInstanceSearchCritical.getEipIdIsNull())
        {
            queryWrapper.isNull(VmInstance::getEipId);
        }
        else if (null != vmInstanceSearchCritical.getEipIdIsNull() && !vmInstanceSearchCritical.getEipIdIsNull())
        {
            queryWrapper.isNotNull(VmInstance::getEipId);
        }

        //忽略失败的实例
        if (null != vmInstanceSearchCritical.getIgnoreFailed() && vmInstanceSearchCritical.getIgnoreFailed())
        {
            queryWrapper.ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANED)
                    .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED)
                    .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANING)
                    .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_MIGRATE_FAILED)
                    .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_REMOVE_FAILED);
        }
        if (StrUtil.isNotBlank(vmInstanceSearchCritical.getEipId()))
        {
            queryWrapper.eq(VmInstance::getEipId, vmInstanceSearchCritical.getEipId());
        }

        if (null != vmInstanceSearchCritical.getEipMapIsUsing() && !vmInstanceSearchCritical.getEipMapIsUsing())
        {
            eipMapPorts = combRpcSerice.getNetworkService().getPorts(vmInstanceSearchCritical.getSubnetId());
            eipMapPorts.forEach(eipMapPort -> queryWrapper.ne(VmInstance::getPortId, eipMapPort));


            if (vmInstancesFromNetworkRef.size() > 0)
            {
                Set<String> eipMapPortSet = new HashSet<>(eipMapPorts);
//                    Map<String, TblVmInstance> vmInstancePortMap = vmInstancesFromNetworkRef.stream().collect(Collectors.toMap(TblVmInstance::getPortId, Function.identity()));
//                    Set<String> tmpSet = vmInstancePortMap.keySet();
//                    tmpSet.removeAll(eipMapPortSet);
//                    vmInstancesFromNetworkRef = tmpSet.stream().map(vmInstancePortMap::get).collect(Collectors.toList());
                vmInstancesFromNetworkRef = vmInstancesFromNetworkRef.stream().filter(
                        tblVmInstance -> !eipMapPortSet.contains(tblVmInstance.getPortId())
                ).collect(Collectors.toList());
            }
        }
        if (!StrUtil.isBlank(vmInstanceSearchCritical.getNodeId()))
        {
            queryWrapper.eq(VmInstance::getNodeId, vmInstanceSearchCritical.getNodeId());
        }
        long totalNum = vmInstanceService.count(queryWrapper);

        VmInstancesRsp getVmInstancesRsp = new VmInstancesRsp();
        getVmInstancesRsp.setTotalNum(totalNum);
        if (totalNum < 1 && 0 == vmInstancesFromNetworkRef.size())
        {
            return getVmInstancesRsp;
        }

        //query with page number and page size
//        int begin = ((vmInstanceSearchCritical.getPageNum() - 1) * vmInstanceSearchCritical.getPageSize());
        queryWrapper.orderByDesc(VmInstance::getCreateTime);
        Page<VmInstance> page = new Page<>(vmInstanceSearchCritical.getPageNum(), vmInstanceSearchCritical.getPageSize());
        Page<VmInstance> vmInstancePage = vmInstanceService.page(page, queryWrapper);
        if (vmInstancePage.getTotal() < 1 && 0 == vmInstancesFromNetworkRef.size())
        {
            return getVmInstancesRsp;
        }
        List<VmInstance> tblVmInstances = vmInstancePage.getRecords();


        if (tblVmInstances.isEmpty() && vmInstancesFromNetworkRef.isEmpty())
        {
            return getVmInstancesRsp;
        }
        else if (tblVmInstances.isEmpty())
        {
            tblVmInstances = vmInstancesFromNetworkRef;
        }
        else if (vmInstancesFromNetworkRef.size() > 0)
        {
            getVmInstancesRsp.setTotalNum(totalNum + vmInstancesFromNetworkRef.size());
            tblVmInstances.addAll(vmInstancesFromNetworkRef);
        }

        List<String> imageIdList = new ArrayList<>();
        List<NetworkService.NetworkDetailInfoReq> networkInfoList = new ArrayList<>();

        for (VmInstance tblVmInstance : tblVmInstances)
        {
            //image
            imageIdList.add(tblVmInstance.getImageId());

            //network
            NetworkService.NetworkDetailInfoReq req = new NetworkService.NetworkDetailInfoReq();
            req.setVpcId(tblVmInstance.getVpcId());
            req.setSubnetId(tblVmInstance.getSubnetId());
            req.setPortId(tblVmInstance.getPortId());
            networkInfoList.add(req);
        }
        //get batch image infos from image service
        List<ImageService.Image> imageList = combRpcSerice.getImageService().getBatchImages(imageIdList);
        if (null == imageList || imageList.size() != imageIdList.size())
        {
            log.error("get vm instances failed, get image service error");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        //get batch network infos from network service
        List<NetworkService.NetworkDetailInfo> networkInfoDetailList = combRpcSerice.getNetworkService().getBatchNetworkInfos(networkInfoList);
        if (null == networkInfoDetailList || networkInfoDetailList.size() != networkInfoList.size())
        {
            log.error("get vm instances failed, get network service error");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        List<VmInstanceInfo> vmInstanceInfoList = new ArrayList<>();

        for (int i = 0; i < tblVmInstances.size(); i++)
        {
            VmInstance tblVmInstance = tblVmInstances.get(i);

            VmInstanceInfo vmInstanceInfo = new VmInstanceInfo();
            vmInstanceInfo.setInstanceInfo(tblVmInstance);

            //set image info
            if (imageList.size() == tblVmInstances.size())
            {
                ImageService.Image image = imageList.get(i);
                if (null != image)
                {
                    ImageAbbrInfo imageAbbrInfo = new ImageAbbrInfo();
                    imageAbbrInfo.setImageId(tblVmInstance.getImageId());
                    imageAbbrInfo.setName(image.getName());
                    vmInstanceInfo.setImageInfo(imageAbbrInfo);
                }
            }

            //set network info
            if (networkInfoDetailList.size() == tblVmInstances.size())
            {
                NetworkService.NetworkDetailInfo networkDetailInfo = networkInfoDetailList.get(i);

                //Eip
                vmInstanceInfo.setEipId(networkDetailInfo.getEipId());
                vmInstanceInfo.setEip(networkDetailInfo.getEip());
                vmInstanceInfo.setBoundPhaseStatus(networkDetailInfo.getBoundPhaseStatus());
                vmInstanceInfo.setBoundType(networkDetailInfo.getBoundType());
                vmInstanceInfo.setPublicIp(networkDetailInfo.getPublicIp());
                vmInstanceInfoList.add(vmInstanceInfo);

                //vpc info
                VpcAbbrInfo vpcAbbrInfo = new VpcAbbrInfo();
                vpcAbbrInfo.setVpcId(networkDetailInfo.getVpcId());
                vpcAbbrInfo.setName(networkDetailInfo.getVpcName());
                vpcAbbrInfo.setCidr(networkDetailInfo.getVpcCidr());
                vmInstanceInfo.setVpcInfo(vpcAbbrInfo);

                //subnet info
                SubnetAbbrInfo subnetAbbrInfo = new SubnetAbbrInfo();
                subnetAbbrInfo.setSubnetId(networkDetailInfo.getSubnetId());
                subnetAbbrInfo.setName(networkDetailInfo.getSubnetName());
                subnetAbbrInfo.setCidr(networkDetailInfo.getSubnetCidr());
                vmInstanceInfo.setSubnetInfo(subnetAbbrInfo);

                //port info
                PortAbbrInfo portAbbrInfo = new PortAbbrInfo();
                portAbbrInfo.setPortId(networkDetailInfo.getPortId());
                portAbbrInfo.setIpAddress(networkDetailInfo.getIpAddress());
                vmInstanceInfo.setPortInfo(portAbbrInfo);


            }
        }

        getVmInstancesRsp.setVmInstancesInfo(vmInstanceInfoList);
        return getVmInstancesRsp;
    }

    private List<InstanceNetworkRef> getInstanceNetworkRefs(String subnetId)
    {
        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceNetworkRef::getSubnetId, subnetId)
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);
        if (instanceNetworkRefService.count(queryWrapper) > 0)
        {
            return instanceNetworkRefService.list(queryWrapper);
        }

        return new ArrayList<>();
    }

    public List<VmInstance> getVmInstancesBySubnetId(String subnetId)
    {
        return getInstanceNetworkRefs(subnetId).stream().map(
                tblInstanceNetworkRef ->
                {
                    VmInstance tblVmInstance = vmInstanceService.getById(tblInstanceNetworkRef.getInstanceId());
                    tblVmInstance.setPortId(tblInstanceNetworkRef.getPortId());
                    tblVmInstance.setVpcId(tblInstanceNetworkRef.getVpcId());
                    return tblVmInstance;
                }
        ).collect(Collectors.toList());
    }

    public VmInstanceDetailInfoRsp getVmInstance(String vmInstanceId) throws WebSystemException
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        FlavorInfo flavor = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
//        Flavor tblFlavor = flavorService.getById(tblVmInstance.getFlavorId());
        if (null == flavor)
        {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }

        VmInstanceDetailInfoRsp getVmInstanceDetailInfoRsp = new VmInstanceDetailInfoRsp();
        getVmInstanceDetailInfoRsp.setInstanceDetailInfo(tblVmInstance, flavor);

        LambdaQueryWrapper<InstanceNetworkRef> networkRefQueryWrapper = new LambdaQueryWrapper<>();
        networkRefQueryWrapper.ne(InstanceNetworkRef::getPhaseStatus, REMOVED)
                .eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId());

        List<NetworkService.NetworkDetailInfoReq> networkDetailInfoReqs = new ArrayList<>();
        NetworkService.NetworkDetailInfoReq networkDetailInfoReq = new NetworkService.NetworkDetailInfoReq();
        networkDetailInfoReq.setVpcId(tblVmInstance.getVpcId());
        networkDetailInfoReq.setSubnetId(tblVmInstance.getSubnetId());
        networkDetailInfoReq.setPortId(tblVmInstance.getPortId());
        networkDetailInfoReq.setInstanceId(tblVmInstance.getVmInstanceId());
        networkDetailInfoReqs.add(networkDetailInfoReq);

        if (instanceNetworkRefService.count(networkRefQueryWrapper) > 0)
        {
            List<NetworkService.NetworkDetailInfoReq> reqs = instanceNetworkRefService.list(networkRefQueryWrapper).stream().map(
                    tblInstanceNetworkRef ->
                    {
                        NetworkService.NetworkDetailInfoReq req = new NetworkService.NetworkDetailInfoReq();
                        req.setInstanceId(tblInstanceNetworkRef.getInstanceId());
                        req.setVpcId(tblInstanceNetworkRef.getVpcId());
                        req.setSubnetId(tblInstanceNetworkRef.getSubnetId());
                        req.setPortId(tblInstanceNetworkRef.getPortId());
                        return req;
                    }
            ).collect(Collectors.toList());
            networkDetailInfoReqs.addAll(reqs);
        }
        List<NetworkService.NetworkDetailInfo> networkDetailInfos = combRpcSerice.getNetworkService().getBatchNetworkInfos(networkDetailInfoReqs);
        if (null != networkDetailInfos)
        {
            getVmInstanceDetailInfoRsp.setNetworkDetailInfos(networkDetailInfos);
        }
        List<NetworkService.SgInfo> sgInfos = combRpcSerice.getNetworkService().getSgInfos(vmInstanceId);
        if (null != sgInfos)
        {
            getVmInstanceDetailInfoRsp.setSgInfos(sgInfos);
        }
        if (StrUtil.isNotBlank(tblVmInstance.getNodeId()))
        {
            HypervisorNode node = hypervisorNodeService.getById(tblVmInstance.getNodeId());
            if (null == node || REMOVED == node.getPhaseStatus())
            {
                throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
            }
            getVmInstanceDetailInfoRsp.setHypervisorNodeName(node.getName());
        }
        //snapDetailInfo
        LambdaQueryWrapper<VmSnap> snapQueryWrapper = new LambdaQueryWrapper<>();
        snapQueryWrapper.eq(VmSnap::getVmInstanceId, vmInstanceId)
                .ne(VmSnap::getPhaseStatus, REMOVED);

        List<VmSnap> tblVmSnaps = vmSnapService.list(snapQueryWrapper);
        if (tblVmSnaps.size() > 0)
        {
            getVmInstanceDetailInfoRsp.setSnapInfos(tblVmSnaps.stream().map(tblVmSnap ->
            {
                VmInstanceDetailInfoRsp.SnapInfo snapInfo = new VmInstanceDetailInfoRsp.SnapInfo();
                snapInfo.setSnapId(tblVmSnap.getSnapId());
                snapInfo.setSnapName(tblVmSnap.getName());
                snapInfo.setIsCurrent(tblVmSnap.getIsCurrent());
                snapInfo.setPhaseStatus(tblVmSnap.getPhaseStatus());
                snapInfo.setCreateTime(Utils.formatDate(tblVmInstance.getCreateTime()));
                snapInfo.setUpdateTime(Utils.formatDate(tblVmInstance.getUpdateTime()));
                return snapInfo;
            }).collect(Collectors.toList()));
        }
        //disks
        List<VolumeService.VolumeInfo> volumeInfos = combRpcSerice.getVolumeService().getVolumeInfosByVmId(vmInstanceId);

        getVmInstanceDetailInfoRsp.setDiskInfos(volumeInfos);
//        LambdaQueryWrapper<DiskInfo> diskInfoQueryWrapper = new LambdaQueryWrapper<>();
//        diskInfoQueryWrapper.select(DiskInfo::getSize).eq(DiskInfo::getVmInstanceId,vmInstanceId)
//                .ne(DiskInfo::getPhaseStatus, REMOVED);
//
//        List<Integer> diskInfos = diskInfoService.listObjs(diskInfoQueryWrapper, o->Integer.valueOf(o.toString()));
//        getVmInstanceDetailInfoRsp.setDiskInfos(diskInfos);

        //image_name
        ImageService.Image image = combRpcSerice.getImageService().getImage(tblVmInstance.getImageId());
        if (null != image)
        {
            getVmInstanceDetailInfoRsp.setImageName(image.getName());
            getVmInstanceDetailInfoRsp.setImageOsType(image.getImageOsType());
        }
        else
        {
            Integer osType = "linux".equals(tblVmInstance.getOsType()) ? ImageOsType.LINUX : ImageOsType.WINDOWS;
            getVmInstanceDetailInfoRsp.setImageOsType(osType);
        }
        //pci devices
        List<PciDeviceInfo> pciDeviceInfos = new ArrayList<>();
        Page<PciDevice> page = new Page<>(1, 8);
        LambdaQueryWrapper<PciDevice> pciDeviceWrapper = new LambdaQueryWrapper<>();
        pciDeviceWrapper.eq(PciDevice::getVmInstanceId, vmInstanceId)
                .ne(PciDevice::getPhaseStatus, REMOVED)
                .orderByDesc(PciDevice::getCreateTime);
        List<PciDevice> pciDevices = pciDeviceService.page(page, pciDeviceWrapper).getRecords();
        pciDeviceInfos = pciDevices.stream().map(pciDevice ->
        {
            PciDeviceInfo pciDeviceInfo = new PciDeviceInfo();
            pciDeviceInfo.setPciDevice(pciDevice);
            return pciDeviceInfo;
        }).collect(Collectors.toList());

//        LambdaQueryWrapper<PciDeviceGroup> pciDeviceGroupLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        pciDeviceGroupLambdaQueryWrapper.select(PciDeviceGroup::getDeviceGroupId)
//                .eq(PciDeviceGroup::getVmInstanceId, vmInstanceId)
//                .ne(PciDeviceGroup::getPhaseStatus, REMOVED);

//        pciDeviceInfoQueryWrapper.in(PciDeviceInfo::getPciDeviceGroupId, pciDeviceGroupIds)
//                .ne(PciDeviceInfo::getPhaseStatus, REMOVED)
//                .ne(PciDeviceInfo::getPhaseStatus, VmInstanceStatus.DEVICE_DETACHED)
//                .orderByDesc(PciDeviceInfo::getCreateTime);
//        pciDeviceInfos = deviceInfoMapper.selectPCIDevices(page, pciDeviceInfoQueryWrapper);


        getVmInstanceDetailInfoRsp.setPciInfos(pciDeviceInfos);

        return getVmInstanceDetailInfoRsp;
    }

    @Transactional(rollbackFor = Exception.class)
    public VmInstanceBaseRsp updateVmInstance(VmInstanceUpdateReq req, String instanceId, String userId)
    {

        VmInstance tblVmInstance = vmInstanceService.getById(instanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(userId) && !userId.equals(tblVmInstance.getUserId()))
        {
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.ERROR);
        }
        if (StrUtil.isNotBlank(req.getBootDev()) || StrUtil.isNotBlank(req.getFlavorId()))
        {
            if (!canUpdateVmInstance(tblVmInstance))
            {
                throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_ALLOW_UPDATE, ErrorLevel.INFO);
            }
        }
        if (StrUtil.isNotBlank(req.getName()))
        {
            tblVmInstance.setName(req.getName());
        }
        if (StrUtil.isNotBlank(req.getDescription()))
        {
            tblVmInstance.setDescription(req.getDescription());
        }
        if (StrUtil.isNotBlank(req.getFlavorId()))
        {
            FlavorInfo flavor = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
            FlavorInfo newFlavor = combRpcSerice.getFlavorService().getFlavorInfo(req.getFlavorId());
            if (null == flavor)
            {
                throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
            }
//            if (!Objects.equals(flavor.getRootDisk(), newFlavor.getRootDisk()))
//            {
//                throw new WebSystemException(ErrorCode.FLAVOR_ROOT_DISK_SIZE_NOT_SAME, ErrorLevel.INFO);
//            }
            if (newFlavor.getMem() > flavor.getMem())
            {
                checkVmMemEnough(tblVmInstance.getNodeId(), newFlavor.getMem());
            }
            if (StrUtil.isNotBlank(flavor.getGpuName()) && StrUtil.isNotBlank(newFlavor.getGpuName()) &&
                    !Objects.equals(flavor.getGpuName(), newFlavor.getGpuName()))
            {
                throw new WebSystemException(ErrorCode.FLAVOR_GPU_NAME_NOT_SAME, ErrorLevel.INFO);
            }
            if (tblVmInstance.getPhaseStatus() != VmInstanceStatus.INSTANCE_POWEROFF)
            {
                throw new WebSystemException(ErrorCode.INSTANCE_NOT_POWER_OFF, ErrorLevel.INFO);
            }
            checkVmSnap(tblVmInstance);
            checkGpu(tblVmInstance.getNodeId(), tblVmInstance, flavor.getGpuCount(), newFlavor);
            tblVmInstance.setFlavorId(req.getFlavorId());
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESIZE_INIT);
        }

        if (StrUtil.isNotBlank(req.getBootDev()))
        {
            if (!"hd".equals(req.getBootDev()) && !"cdrom".equals(req.getBootDev()))
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            tblVmInstance.setBootDev(req.getBootDev());
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_BOOT_DEV_SWITCHING);
        }
        if (null != req.getCpuCount() && req.getCpuCount() > 0)
        {
            tblVmInstance.setCpuCount(req.getCpuCount());
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESIZE_INIT);
        }
        if (null != req.getMemorySize() && req.getMemorySize() > 0)
        {
            tblVmInstance.setMemSize(req.getMemorySize());
            tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESIZE_INIT);
        }

        checkCpuMem(tblVmInstance.getNodeId(), req.getCpuCount(), req.getMemorySize());
        tblVmInstance.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        vmInstanceBaseRsp.setInstanceId(tblVmInstance.getVmInstanceId());
        return vmInstanceBaseRsp;
    }

    // checkVmSnap 检查虚机是否有快照
    public void checkVmSnap(VmInstance tblVmInstance)
    {
        LambdaQueryWrapper<VmSnap> snapQueryWrapper = new LambdaQueryWrapper<>();
        snapQueryWrapper.eq(VmSnap::getVmInstanceId, tblVmInstance.getVmInstanceId())
                .ne(VmSnap::getPhaseStatus, REMOVED);

        long snapCount = vmSnapService.count(snapQueryWrapper);
        if (snapCount > 0)
        {
            throw new WebSystemException(ErrorCode.VM_HAS_SNAPS, ErrorLevel.INFO);
        }
    }

    public void checkVmMemEnough(String nodeId, Integer memSize)
    {
        if (StrUtil.isBlank(nodeId))
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        if (null == memSize || memSize <= 0)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        HypervisorNode node = hypervisorNodeService.getById(nodeId);
        if (null == node || REMOVED == node.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(VmInstance::getFlavorId)
                .isNotNull(VmInstance::getFlavorId)
                .eq(VmInstance::getNodeId, nodeId)
                .ne(VmInstance::getPhaseStatus, REMOVED)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANING)
                .ne(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED_CLEANED);
        List<String> flavorIds = vmInstanceService.listObjs(queryWrapper, Object::toString);
        Integer usedMem = combRpcSerice.getFlavorService().getMemSizeTotalByFlavorIds(flavorIds);
        if (null == usedMem) usedMem = 0;
        if (node.getMemTotal() - usedMem < memSize)
        {
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }
    }


    // resetPasswordAndHostname 更新vm登录密码和主机名
    public VmInstanceBaseRsp resetPasswordAndHostname(ResetPasswordHostnameReq req, String vmInstanceId, String userId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(userId) && !userId.equals(tblVmInstance.getUserId()))
        {
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.ERROR);
        }
        if (VmInstanceStatus.INSTANCE_RUNNING != tblVmInstance.getPhaseStatus()
                && VmInstanceStatus.INSTANCE_POWEROFF != tblVmInstance.getPhaseStatus()
                && VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE != tblVmInstance.getPhaseStatus()
                && VmInstanceStatus.INSTANCE_MIGRATE_CLEAN != tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_STATUS_ERROR, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(req.getSysPassword()))
        {
            tblVmInstance.setSysPassword(AesCryptoUtils.encryptHex(req.getSysPassword()));
        }
        if (StrUtil.isNotBlank(req.getHostname()))
        {
            tblVmInstance.setHostName(req.getHostname());
        }
        if (StrUtil.isNotBlank(req.getPubkeyId()))
        {
            tblVmInstance.setPubkeyId(req.getPubkeyId());
        }
        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RESET_PASSWORD_HOSTNAME);
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        vmInstanceBaseRsp.setInstanceId(tblVmInstance.getVmInstanceId());
        return vmInstanceBaseRsp;
    }

    // 抢占新的GPU资源，如果newGpuCount 小于 oldGpuCount，则表示释放GPU资源
    @Transactional(rollbackFor = Exception.class)
    public void checkGpu(String nodeId, VmInstance tblVmInstance, Integer oldGpuCount, FlavorInfo newFlavor)
    {
        Integer newGpuCount = newFlavor.getGpuCount();
        if (null == newGpuCount || 0 >= newGpuCount)
        {
            return;
        }
        if (null != oldGpuCount && newGpuCount <= oldGpuCount)
        {
            return;
        }
        if (null == oldGpuCount) oldGpuCount = 0;

        PageSearchCritical pageSearchCritical = new PciDeviceSearchCritical();
        pageSearchCritical.setPageNum(1);
        pageSearchCritical.setPageSize(8);
        List<PciDeviceInfo> pciDeviceInfos = pciDeviceServiceBiz.getAvailableDeviceInfos(pageSearchCritical, nodeId, tblVmInstance.getVmInstanceId());
        if (0 == pciDeviceInfos.size())
        {
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }
//        List<PciDeviceInfo> gpuInfos = pciDeviceInfos.stream().filter(pciDeviceInfo -> Objects.equals(pciDeviceInfo.getPciDeviceType(), "GPU")).collect(Collectors.toList());
        if (pciDeviceInfos.size() < newGpuCount - oldGpuCount)
        {
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }
        if (vmScheduler.needPartition(newFlavor.getGpuName()))
        {
            vmInstanceTimerProcessor.resizeGpuPartition(tblVmInstance, newFlavor);
            return;
        }
        //实际抢占的GPU数量
        int needGpuCount = 0;
        for (PciDeviceInfo gpuInfo : pciDeviceInfos)
        {
            if (needGpuCount == newGpuCount - oldGpuCount)
            {
                break;
            }
            PciDevice tblPciDevice = pciDeviceService.getById(gpuInfo.getDeviceId());
            if (null == tblPciDevice || REMOVED == tblPciDevice.getPhaseStatus())
            {
                throw new WebSystemException(ErrorCode.PCI_DEVICE_NOT_EXIST, ErrorLevel.INFO);
            }
            if (VmInstanceStatus.DEVICE_DETACHED != tblPciDevice.getPhaseStatus())
            {
                continue;
            }

//            PciDeviceGroup tblPciDeviceGroup = pciDeviceGroupService.getById(tblPciDevice.getDeviceGroupId());
//            if (null == tblPciDeviceGroup || REMOVED == tblPciDeviceGroup.getPhaseStatus())
//            {
//                throw new WebSystemException(ErrorCode.PCI_DEVICE_GROUP_NOT_EXIST, ErrorLevel.INFO);
//            }
            if (StrUtil.isNotBlank(tblPciDevice.getVmInstanceId()))
            {
                continue;
            }
            tblPciDevice.setUserId(tblVmInstance.getUserId());
            tblPciDevice.setVmInstanceId(tblVmInstance.getVmInstanceId());
            tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACHING);
            tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = pciDeviceService.updateById(tblPciDevice);
            if (ok)
            {
                needGpuCount += 1;
            }
        }
        //needGpuCount 表示实际抢占的GPU数量，如果实际抢占的GPU数量小于应该抢占的GPU数量，则表示GPU资源不足
        if (needGpuCount < newGpuCount - oldGpuCount)
        {
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }

    }

    private void checkCpuMem(String nodeId, Integer cpuCount, Integer memSize)
    {
        if (null == cpuCount || null == memSize)
        {
            return;
        }
        HypervisorNode node = hypervisorNodeService.getById(nodeId);
        if (null == node || REMOVED == node.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<HypervisorNodeAllocationInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNodeAllocationInfo::getNodeId, nodeId);
        Page<HypervisorNodeAllocationInfo> page = new Page<>(1, 1);
        List<HypervisorNodeAllocationInfo> nodeAllocationInfos = hypervisorNodeAllocationMapper.selectNodeAllocationInfo(page, queryWrapper, computeConfig.getIbCount());
        if (nodeAllocationInfos.size() == 0)
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        if (cpuCount > node.getCpuLogCount() - nodeAllocationInfos.get(0).getCpuSum() + nodeAllocationInfos.get(0).getCpuRecycle() ||
                memSize > node.getMemTotal() - nodeAllocationInfos.get(0).getMemSum() + nodeAllocationInfos.get(0).getMemRecycle())
        {
            throw new WebSystemException(ErrorCode.NODE_RESOURCE_NOT_ENOUGH, ErrorLevel.INFO);
        }
    }

    private boolean canUpdateVmInstance(VmInstance tblVmInstance)
    {
        if (VmInstanceStatus.INSTANCE_MIGRATE_INIT <= tblVmInstance.getPhaseStatus() &&
                VmInstanceStatus.INSTANCE_RESUMED >= tblVmInstance.getPhaseStatus())
        {
            return false;
        }

        return VmInstanceStatus.INSTANCE_RUNNING == tblVmInstance.getPhaseStatus() ||
                VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE == tblVmInstance.getPhaseStatus() ||
                VmInstanceStatus.INSTANCE_MIGRATE_CLEAN == tblVmInstance.getPhaseStatus() ||
                VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI == tblVmInstance.getPhaseStatus() ||
                VmInstanceStatus.INSTANCE_POWEROFF == tblVmInstance.getPhaseStatus();
    }

    @Transactional(rollbackFor = Exception.class)
    public VmInstanceBaseRsp removeVmInstance(String instanceId, Boolean removeRootDisk) throws WebSystemException
    {
        //get instance from db
        VmInstance tblVmInstance = vmInstanceService.getById(instanceId);
        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }

        LambdaQueryWrapper<InstanceNetworkRef> instanceNetworkRefQueryWrapper = new LambdaQueryWrapper<>();
        instanceNetworkRefQueryWrapper.select(InstanceNetworkRef::getPortId)
                .eq(InstanceNetworkRef::getInstanceId, instanceId)
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);
        List<String> portIds = instanceNetworkRefService.listObjs(instanceNetworkRefQueryWrapper, Object::toString);
        portIds.add(tblVmInstance.getPortId());
        List<Long> eipMapCount = combRpcSerice.getNetworkService().getEipMapCount(portIds);
        if (null == eipMapCount || eipMapCount.size() != 2)
        {
            throw new WebSystemException(ErrorCode.EIP_CHECK_ERR, ErrorLevel.ERROR);
        }
        if (eipMapCount.get(1) > 0)
        {
            throw new WebSystemException(ErrorCode.INSTANCE_USED_BY_NAT, ErrorLevel.INFO);
        }
        if (eipMapCount.get(0) > 0)
        {
            throw new WebSystemException(ErrorCode.PORT_HAS_EIP, ErrorLevel.INFO);
        }

        checkVmSnap(tblVmInstance);

        if (null == tblVmInstance.getInstanceIdFromAgent())
        {
            tblVmInstance.setPhaseStatus(VmInstanceStatus.GET_INSTANCE_REMOVED_STATUS);
            tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            if (vmInstanceService.updateById(tblVmInstance))
            {
                String result = combRpcSerice.getNetworkService().vmInstanceUnBoundSgs(tblVmInstance.getVmInstanceId());
                if (!"ok".equals(result))
                {
                    log.error("vm instance unBound security group error, vminstanceId :{}", tblVmInstance.getVmInstanceId());
                    throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
                }
                removePorts(tblVmInstance);
                removeDisks(tblVmInstance.getVmInstanceId(), removeRootDisk);
                vmInstanceBaseRsp.setInstanceId(tblVmInstance.getVmInstanceId());
                return vmInstanceBaseRsp;
            }
//            return vmInstanceBaseRsp;
        }
        if (removeRootDisk)
        {
            removeDisks(tblVmInstance.getVmInstanceId(), true);
        }
        //set the waiting delete phase status and timestamp
        LambdaUpdateWrapper<InstanceNetworkRef> instanceNetworkRefUpdateWrapper = new LambdaUpdateWrapper<>();
        instanceNetworkRefUpdateWrapper.eq(InstanceNetworkRef::getInstanceId, instanceId)
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);

        InstanceNetworkRef tblInstanceNetworkRef = new InstanceNetworkRef();
        tblInstanceNetworkRef.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblInstanceNetworkRef.setPhaseStatus(VmInstanceStatus.INSTANCE_REMOVING);
        if (instanceNetworkRefService.count(instanceNetworkRefUpdateWrapper) > 0)
        {
            if (!instanceNetworkRefService.update(tblInstanceNetworkRef, instanceNetworkRefUpdateWrapper))
            {
                log.info("remove vmInstance {} :update database error ", instanceId);
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }

        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_REMOVING);
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.info("remove vmInstance {} :update database error ", instanceId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        vmInstanceBaseRsp.setInstanceId(tblVmInstance.getVmInstanceId());
        return vmInstanceBaseRsp;
    }

    private void removeDisks(String vmInstanceId, Boolean removeRootDisk)
    {
        Boolean detachedOk = combRpcSerice.getVolumeService().detachVolumesByVmId(vmInstanceId, removeRootDisk);
        if (!detachedOk)
        {
            log.info("detach volumes err, vmInstanceId:{}", vmInstanceId);
        }
        LambdaUpdateWrapper<DiskInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DiskInfo::getVmInstanceId, vmInstanceId)
                .eq(DiskInfo::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATED);
        if (0 == diskInfoService.count(updateWrapper))
        {
            return;
        }
        DiskInfo tblDiskInfo = new DiskInfo();
        tblDiskInfo.setPhaseStatus(REMOVED);
        tblDiskInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        diskInfoService.update(tblDiskInfo, updateWrapper);
    }

    public VmInstanceBaseRsp powerOffInstance(String instanceId) throws WebSystemException
    {
        return powerChangeTo(instanceId, VmInstanceStatus.INSTANCE_POWEROFF);
    }

    public VmInstanceBaseRsp rebootInstance(String instanceId) throws WebSystemException
    {
        return powerChangeTo(instanceId, VmInstanceStatus.INSTANCE_REBOOT_POWEROFFING);
    }


    public VmInstanceBaseRsp powerOnInstance(String instanceId) throws WebSystemException
    {
        return powerChangeTo(instanceId, VmInstanceStatus.INSTANCE_RUNNING);
    }

    //关机卸载GPU
    public VmInstanceBaseRsp powerOffInstanceWithNoPci(String instanceId) throws WebSystemException
    {
        // 判断是否有虚机有快照，如果有则不允许执行节省模式的关机
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmSnap::getVmInstanceId, instanceId)
                .ne(VmSnap::getPhaseStatus, REMOVED);
        if (vmSnapService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.VM_HAS_SNAPS, ErrorLevel.INFO);
        }
        return powerChangeTo(instanceId, VmInstanceStatus.INSTANCE_POWERING_OFF_DETACH_PCI);
    }

    public VmInstanceBaseRsp boundSgs(SgsUpdateReq request, String vmInstanceId, String userId)
    {

        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        vmInstanceBaseRsp.setInstanceId(vmInstanceId);
        String returnString = combRpcSerice.getNetworkService().vmInstanceApplySgWithSgIds(request.getSgIds(), vmInstanceId);
        if ("ok".equals(returnString))
        {
            return vmInstanceBaseRsp;
        }
        else if ("exist".equals(returnString))
        {
            throw new WebSystemException(ErrorCode.SG_INSTANCE_EXISTS, ErrorLevel.INFO);
        }
        else
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public VmInstanceBaseRsp updateSgs(SgsUpdateReq request, String vmInstanceId, String userId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getVmInstanceId, vmInstanceId)
                .eq(VmInstance::getUserId, userId)
                .ne(VmInstance::getPhaseStatus, REMOVED);

        if (vmInstanceService.count(queryWrapper) < 1)
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        vmInstanceBaseRsp.setInstanceId(vmInstanceId);
        String returnString = combRpcSerice.getNetworkService().vmInstanceUpdateSgs(request.getSgIds(), vmInstanceId);
        if (!"ok".equals(returnString))
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return vmInstanceBaseRsp;
    }

    public IsoIsInjectedRsp isoIsInjected(String vmInstanceId, String userId)
    {
        VmInstance tblVmInstance = getVmInstance(vmInstanceId, userId);
        IsoIsInjectedRsp isoIsInjectedRsp = new IsoIsInjectedRsp();
        isoIsInjectedRsp.setVmInstanceId(vmInstanceId);
        try
        {
            VmInstanceTimerProcessor.GetVmInstanceRspFromAgent getVmInstanceStatusFromAgent = vmInstanceTimerProcessor.getVmInstanceStatusFromAgent(tblVmInstance);
            if (null == getVmInstanceStatusFromAgent)
            {
                throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
            }
            if (Objects.equals(getVmInstanceStatusFromAgent.getPhaseType(), "inject"))
            {
                isoIsInjectedRsp.setInjected(!getVmInstanceStatusFromAgent.getPhase().equals(AgentConstant.FAIL));
            }

        }
        catch (Exception e)
        {
            isoIsInjectedRsp.setInjected(false);
        }
        return isoIsInjectedRsp;
    }

    private boolean staticIpIsValid(String staticIp, String subnetId)
    {
        String subnetCidr = combRpcSerice.getNetworkService().getSubnet(subnetId).getCidr();
        if (StrUtil.isBlank(subnetCidr))
        {
            log.error("combRpc Service get subnetCidr error, subnetId: {}", subnetId);
            return false;
        }
        SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(subnetCidr).getInfo();
        if (!subnetInfo.isInRange(staticIp))
        {
            log.error("staticIp is not in the range of subnetCidr, staticIp:{}", staticIp);
            return false;
        }
        return true;
    }

    public VmInstanceBaseRsp powerChangeTo(String instanceId, int phaseStatus) throws WebSystemException
    {
        VmInstance tblVmInstance = vmInstanceService.getById(instanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (tblVmInstance.getPhaseStatus() < VmInstanceStatus.INSTANCE_RUNNING ||
                tblVmInstance.getPhaseStatus() == VmInstanceStatus.INSTANCE_POWER_ON_FAILED ||
                (tblVmInstance.getPhaseStatus() > VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE && tblVmInstance.getPhaseStatus() <= VmInstanceStatus.WAIT_INSTANCE_CLOUD_INIT_RESULT))
        {
            throw new WebSystemException(ErrorCode.VM_STATUS_ERROR, ErrorLevel.INFO);
        }

        if (VmInstanceStatus.INSTANCE_MIGRATE_INIT <= tblVmInstance.getPhaseStatus() && VmInstanceStatus.INSTANCE_RESUMED >= tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_IS_MIGRATING, ErrorLevel.INFO);
        }

        switch (phaseStatus)
        {
            case VmInstanceStatus.INSTANCE_POWEROFF:
            case VmInstanceStatus.INSTANCE_POWERING_OFF_DETACH_PCI:
                if (VmInstanceStatus.INSTANCE_RUNNING != tblVmInstance.getPhaseStatus() &&
                        VmInstanceStatus.INSTANCE_POWERONING != tblVmInstance.getPhaseStatus() &&
                        VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE != tblVmInstance.getPhaseStatus() &&
                        VmInstanceStatus.INSTANCE_MIGRATE_CLEAN != tblVmInstance.getPhaseStatus())
                {
                    log.error("vmInstance phase: {}, is not RUNNING phase", tblVmInstance.getPhaseStatus());
                    throw new WebSystemException(ErrorCode.INSTANCE_NOT_POWER_ON, ErrorLevel.INFO);
                }
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWEROFFING);
                if (VmInstanceStatus.INSTANCE_POWERING_OFF_DETACH_PCI == phaseStatus)
                {
                    FlavorInfo flavor = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
                    tblVmInstance.setRecycleMemSize(flavor.getMem());
                    tblVmInstance.setRecycleCpuCount(flavor.getCpu());
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWERING_OFF_DETACH_PCI);
                }
                break;
            case VmInstanceStatus.INSTANCE_RUNNING:
                if (VmInstanceStatus.INSTANCE_POWEROFF != tblVmInstance.getPhaseStatus() &&
                        VmInstanceStatus.INSTANCE_MIGRATE_CLEAN != tblVmInstance.getPhaseStatus() &&
                        VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI != tblVmInstance.getPhaseStatus())
                {
                    log.error("vmInstance phase: {}, is not POWEROFF phase", tblVmInstance.getPhaseStatus());
                    throw new WebSystemException(ErrorCode.INSTANCE_NOT_POWER_OFF, ErrorLevel.INFO);
                }
                if (VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI == tblVmInstance.getPhaseStatus()
                        || VmInstanceStatus.INSTANCE_POWER_ON_FAILED == tblVmInstance.getPhaseStatus())
                {
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWERING_ON_PREPARE_PCI);
                }
                else
                {
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWERONING);
                }
                break;
            case VmInstanceStatus.INSTANCE_REBOOT_POWEROFFING:
                if (VmInstanceStatus.INSTANCE_RESIZE_INIT <= tblVmInstance.getPhaseStatus() &&
                        VmInstanceStatus.GET_INSTANCE_BOOT_DEV_STATUS >= tblVmInstance.getPhaseStatus())
                {
                    log.error("vmInstance phase: {}, is not REBOOT_POWEROFFING phase", tblVmInstance.getPhaseStatus());
                    throw new WebSystemException(ErrorCode.INSTANCE_NOT_POWER_ON, ErrorLevel.INFO);
                }
                logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "Agent 正在关机",
                        String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "关机中");
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_REBOOT_POWEROFFING);
                break;

        }
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.info("update vm phase failed, vm instanceId:{}", instanceId);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        vmInstanceBaseRsp.setInstanceId(tblVmInstance.getVmInstanceId());
        return vmInstanceBaseRsp;
    }

    public Map<String, String> getVncInfo(String vmInstanceId, String userId) throws WebSystemException
    {
        log.info("get vnc info : vmInstanceId: {}, userId: {}", vmInstanceId, userId);
        VmInstance tblVmInstance = getVmInstance(vmInstanceId, userId);
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == tblHypervisorNode)
        {
            throw new WebSystemException(ErrorCode.HYPERVISOR_NODE_EXIST, ErrorLevel.INFO);
        }
        Map<String, String> vncInfo = new HashMap<>();
        vncInfo.put("agent", tblHypervisorNode.getManageIp());
        vncInfo.put("token", tblVmInstance.getInstanceIdFromAgent());
        return vncInfo;
    }

    public VmInstance getVmInstance(String vmInstanceId, String userId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            log.info("the vm does not exist, vmInstanceId: {}", vmInstanceId);
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!Objects.equals(userId, tblVmInstance.getUserId()))
        {
            log.info("The VM is not owned by the user, vmInstanceId:{} userId:{}", vmInstanceId, userId);
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        return tblVmInstance;
    }

    //migrate  vm instance
    public VmInstanceBaseRsp migrateInstance(String instanceId, String destNodeId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(instanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            log.info("vm instance not exists,vm instanceId:{}", instanceId);
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.ERROR);
        }
        if (tblVmInstance.getNodeId().equals(destNodeId))
        {
            log.info("vm instance {} already on node {}", instanceId, destNodeId);
            throw new WebSystemException(ErrorCode.BAD_REQUST, ErrorLevel.ERROR);
        }
        if (VmInstanceStatus.INSTANCE_RUNNING != tblVmInstance.getPhaseStatus() &&
                VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE != tblVmInstance.getPhaseStatus() &&
                VmInstanceStatus.INSTANCE_MIGRATE_CLEAN != tblVmInstance.getPhaseStatus() &&
                VmInstanceStatus.INSTANCE_POWEROFF != tblVmInstance.getPhaseStatus() &&
                VmInstanceStatus.INSTANCE_MIGRATE_FAILED != tblVmInstance.getPhaseStatus()
        )
        {
            log.info("vm instance {}  can not migrate", instanceId);
            throw new WebSystemException(ErrorCode.VM_STATUS_ERROR, ErrorLevel.ERROR);
        }
        vmSnapServiceBiz.checkPciDeviceVmPowerOff(tblVmInstance);
        if (!canMigrate(instanceId))
        {
            log.info("vm instance {}  can not migrate", instanceId);
            throw new WebSystemException(ErrorCode.VM_SNAP_IS_UPDATING, ErrorLevel.ERROR);
        }
        if (!combRpcSerice.getVolumeService().canMigrate(instanceId))
        {
            log.info("vm instance {}  can not migrate", instanceId);
            throw new WebSystemException(ErrorCode.VOLUME_IS_UPDATING, ErrorLevel.ERROR);
        }

        tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_MIGRATE_INIT);
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVmInstance.setDestNodeId(destNodeId);
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        if (!ok)
        {
            log.info("migrate vm instance err, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);

        }
        VmInstanceBaseRsp vmInstanceBaseRsp = new VmInstanceBaseRsp();
        vmInstanceBaseRsp.setInstanceId(tblVmInstance.getVmInstanceId());
        return vmInstanceBaseRsp;
    }

    // set gpuIds

    @Transactional(rollbackFor = Exception.class)
    public void setPciDevice(String pciDeviceId, String vmId, String userId)
    {

        PciDevice tblPciDevice = pciDeviceService.getById(pciDeviceId);
        if (VmInstanceStatus.DEVICE_DETACHED != tblPciDevice.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PCI_DEVICE_GROUP_ALREADY_ATTACHED, ErrorLevel.INFO);
        }
        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_INIT_CREATE);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblPciDevice.setUserId(userId);
        tblPciDevice.setVmInstanceId(vmId);
        pciDeviceService.updateById(tblPciDevice);

//        PciDeviceGroup tblPciDeviceGroup = pciDeviceGroupService.getById(tblPciDevice.getDeviceGroupId());
//        tblPciDeviceGroup.setVmInstanceId(vmId);
//        tblPciDeviceGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//        pciDeviceGroupService.updateById(tblPciDeviceGroup);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setPciDevices(List<String> pciDeviceIds, String vmId, String userId)
    {
        if (null != pciDeviceIds && pciDeviceIds.size() > 0)
        {
            Set<String> pciDeviceIdsSet = new HashSet<>(pciDeviceIds);
            for (String pciDeviceId : pciDeviceIdsSet)
            {
                if (StrUtil.isBlank(pciDeviceId)) continue;
                setPciDevice(pciDeviceId, vmId, userId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removePorts(VmInstance tblVmInstance)
    {
        //del port from tenant network
        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId())
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);
        List<InstanceNetworkRef> tblInstanceNetworkRefs = instanceNetworkRefService.list(queryWrapper);
        List<String> portIds = new ArrayList<>();
        portIds.add(tblVmInstance.getPortId());
        if (null != tblInstanceNetworkRefs && tblInstanceNetworkRefs.size() > 0)
        {
            portIds.addAll(tblInstanceNetworkRefs.stream().map(InstanceNetworkRef::getPortId).collect(Collectors.toList()));
        }
        combRpcSerice.getNetworkService().removeDnsHosts(portIds);
        List<String> portIdsReps = combRpcSerice.getNetworkService().delPortsFromTenantNetwork(portIds);
        if (null == portIdsReps || portIdsReps.size() != portIds.size())
        {
            //update failed phase status to db
            log.error("remove ports error: del from tenant network port id {}, instance id {}", tblVmInstance.getPortId(), tblVmInstance.getVmInstanceId());
            //throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
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

    public boolean canMigrate(String vmInstanceId)
    {
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmSnap::getVmInstanceId, vmInstanceId)
                .and(wrapper -> wrapper.eq(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_INIT)
                        .or().eq(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_CREATING)
                        .or().eq(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_SWITCHING)
                        .or().eq(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_REMOVING)
                        .or().eq(VmSnap::getPhaseStatus, VmInstanceStatus.GET_SNAP_SWITCHED_STATUS));
        long count = vmSnapService.count(queryWrapper);
        return count == 0;
    }
}
