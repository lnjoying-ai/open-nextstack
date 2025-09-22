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
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.SnapCreateReq;
import com.lnjoying.vm.domain.dto.response.*;
import com.lnjoying.vm.entity.PciDevice;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.entity.VmSnap;
import com.lnjoying.vm.entity.search.VmSnapSearchCritical;
import com.lnjoying.vm.service.PciDeviceService;
import com.lnjoying.vm.service.VmInstanceService;
import com.lnjoying.vm.service.VmSnapService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service
@Slf4j
public class VmSnapServiceBiz
{
    @Autowired
    private VmSnapService vmSnapService;

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private PciDeviceService pciDeviceService;

    public VmSnapBaseRsp addVmSnap(SnapCreateReq addSnapReq, String userId) throws WebSystemException
    {
        VmInstance tblVmInstance = vmInstanceService.getById(addSnapReq.getVmInstanceId());
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            log.info("vm instance not exists, vmInstanceId:{}", addSnapReq.getVmInstanceId());
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
//        if (VmInstanceStatus.INSTANCE_RUNNING != tblVmInstance.getPhaseStatus() &&
//            VmInstanceStatus.INSTANCE_MIGRATE_CLEAN != tblVmInstance.getPhaseStatus() &&
//            VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE != tblVmInstance.getPhaseStatus() &&
//            VmInstanceStatus.INSTANCE_POWEROFF != tblVmInstance.getPhaseStatus()
//        )
//        {
//            log.info("vm instance status error, vmInstanceId:{}, status:{}",addSnapReq.getVmInstanceId(), tblVmInstance.getPhaseStatus());
//            throw new WebSystemException(ErrorCode.VM_STATUS_ERROR, ErrorLevel.INFO);
//        }

//        checkPciDeviceVmPowerOff(tblVmInstance);

        VmSnap tblVmSnap = new VmSnap();
        tblVmSnap.setSnapId(Utils.assignUUId());
        tblVmSnap.setName(addSnapReq.getName());
        tblVmSnap.setVmInstanceId(addSnapReq.getVmInstanceId());
        tblVmSnap.setUserId(userId);
        tblVmSnap.setDescription(addSnapReq.getDescription());
        tblVmSnap.setPhaseStatus(VmInstanceStatus.SNAP_INIT);
        tblVmSnap.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVmSnap.setUpdateTime(tblVmSnap.getCreateTime());
        tblVmSnap.setIsCurrent(false);
        boolean ok = vmSnapService.save(tblVmSnap);
        if (!ok)
        {
            log.error("add vm snap error, vmInstanceId:{}", addSnapReq.getVmInstanceId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        VmSnapBaseRsp vmSnapBaseRsp = new VmSnapBaseRsp();
        vmSnapBaseRsp.setSnapId(tblVmSnap.getSnapId());
        return vmSnapBaseRsp;
    }

    public VmSnapBaseRsp updateSnap(CommonReq req, String instanceId)
    {
        VmSnap tblVmSnap = vmSnapService.getById(instanceId);
        if (null == tblVmSnap)
        {
            throw new WebSystemException(ErrorCode.VM_SNAP_NOT_EXIST, ErrorLevel.INFO);
        }
        if (null != req.getName() && !req.getName().isEmpty())
        {
            tblVmSnap.setName(req.getName());
        }
        if (null != req.getDescription() && !req.getDescription().isEmpty())
        {
            tblVmSnap.setDescription(req.getDescription());
        }
        tblVmSnap.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = vmSnapService.updateById(tblVmSnap);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        VmSnapBaseRsp vmSnapBaseRsp = new VmSnapBaseRsp();
        vmSnapBaseRsp.setSnapId(tblVmSnap.getSnapId());
        return vmSnapBaseRsp;
    }

    public void checkPciDeviceVmPowerOff(VmInstance tblVmInstance)
    {

        LambdaQueryWrapper<PciDevice> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PciDevice::getVmInstanceId, tblVmInstance.getVmInstanceId())
                .ne(PciDevice::getPhaseStatus, REMOVED);
        if (pciDeviceService.count(lambdaQueryWrapper) > 0 && VmInstanceStatus.INSTANCE_POWEROFF != tblVmInstance.getPhaseStatus())
        {
            log.info("vm instance has pci device group, vmInstanceId:{}", tblVmInstance.getVmInstanceId());
            throw new WebSystemException(ErrorCode.VM_PCI_DEVICE_POWER_OFF, ErrorLevel.INFO);
        }
    }

    public VmSnapsRsp getSnaps(VmSnapSearchCritical vmSnapSearchCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        if (!StrUtil.isBlank(vmSnapSearchCritical.getVmSnapId()))
        {
            queryWrapper.eq(VmSnap::getSnapId, vmSnapSearchCritical.getVmSnapId());
        }
        else if (!StrUtil.isBlank(vmSnapSearchCritical.getName()))
        {
            queryWrapper.like(VmSnap::getName, vmSnapSearchCritical.getName());
        }
        else if (!StrUtil.isBlank(vmSnapSearchCritical.getVmInstanceId()))
        {
            queryWrapper.eq(VmSnap::getVmInstanceId, vmSnapSearchCritical.getVmInstanceId());
        }
        if (null != userId)
        {
            queryWrapper.eq(VmSnap::getUserId, userId);
        }
        queryWrapper.ne(VmSnap::getPhaseStatus, REMOVED);
        long totalNum = vmSnapService.count(queryWrapper);
        VmSnapsRsp getSnapsRsp = new VmSnapsRsp();
        getSnapsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getSnapsRsp;
        }

        //query with page number and page size
//        int begin = ((vmSnapSearchCritical.getPageNum() - 1) * vmSnapSearchCritical.getPageSize());
        queryWrapper.orderByDesc(VmSnap::getCreateTime);

        //to do
        Page<VmSnap> page = new Page<>(vmSnapSearchCritical.getPageNum(), vmSnapSearchCritical.getPageSize());
        Page<VmSnap> snapPage = vmSnapService.page(page, queryWrapper);
        List<VmSnap> tblVmSnaps = snapPage.getRecords();

        List<VmSnapInfo> getSnapInfos = tblVmSnaps.stream().map(
                tblVmSnap ->
                {
                    VmSnapInfo getSnapInfo = new VmSnapInfo();
                    String vmName = vmInstanceService.getById(tblVmSnap.getVmInstanceId()).getName();
//                    String vmName = vmComputeRepository.getVmInstanceById(tblVmSnap.getVmInstanceId()).getName();
                    getSnapInfo.setVmInstanceName(vmName);
                    getSnapInfo.setSnapInfo(tblVmSnap);
                    return getSnapInfo;
                }
        ).collect(Collectors.toList());
        getSnapsRsp.setSnaps(getSnapInfos);
        return getSnapsRsp;


    }

    public VmSnapDetailInfoRsp getSnap(String snapId) throws WebSystemException
    {
        VmSnap tblVmSnap = vmSnapService.getById(snapId);
        if (null == tblVmSnap || REMOVED == tblVmSnap.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_SNAP_NOT_EXIST, ErrorLevel.INFO);
        }
        VmInstance tblVmInstance = vmInstanceService.getById(tblVmSnap.getVmInstanceId());
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        VmSnapDetailInfoRsp getSnapDetailInfoRsp = new VmSnapDetailInfoRsp();
        getSnapDetailInfoRsp.setDescription(tblVmSnap.getDescription());
        getSnapDetailInfoRsp.setSnapInfo(tblVmSnap);
        getSnapDetailInfoRsp.setUserId(tblVmSnap.getUserId());
        getSnapDetailInfoRsp.setVmInstanceName(tblVmInstance.getName());
        return getSnapDetailInfoRsp;
    }

    public VmSnapBaseRsp removeSnap(String snapId) throws WebSystemException
    {
        //get instance from db
        VmSnap tblVmSnap = vmSnapService.getById(snapId);
        VmSnapBaseRsp vmSnapBaseRsp = new VmSnapBaseRsp();
        if (null == tblVmSnap || REMOVED == tblVmSnap.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.ERROR);
        }
        VmInstance tblVmInstance = vmInstanceService.getById(tblVmSnap.getVmInstanceId());
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.ERROR);
        }
//        checkPciDeviceVmPowerOff(tblVmInstance);
        vmIsMigrating(tblVmSnap.getVmInstanceId());
        if (null == tblVmSnap.getSnapIdFromAgent())
        {
            tblVmSnap.setPhaseStatus(REMOVED);
            tblVmSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vmSnapService.updateById(tblVmSnap);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            vmSnapBaseRsp.setSnapId(tblVmSnap.getSnapId());
            return vmSnapBaseRsp;
        }
        //set the `waiting delete` phase status and timestamp
        tblVmSnap.setPhaseStatus(VmInstanceStatus.SNAP_REMOVING);
        tblVmSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmSnapService.updateById(tblVmSnap);
        if (!ok)
        {
            log.info("remove vmSnap {} :update database error ", snapId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        vmSnapBaseRsp.setSnapId(tblVmSnap.getSnapId());
        return vmSnapBaseRsp;
    }

    public VmSnapBaseRsp switchSnap(String snapId, String userId)
    {
        VmSnap tblVmSnap = vmSnapService.getById(snapId);
        if (null == tblVmSnap || REMOVED == tblVmSnap.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.ERROR);
        }
        if (!StrUtil.isBlank(userId) && !Objects.equals(tblVmSnap.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        vmIsMigrating(tblVmSnap.getVmInstanceId());
        if (VmInstanceStatus.SNAP_SWITCHING == tblVmSnap.getPhaseStatus() ||
                VmInstanceStatus.GET_SNAP_SWITCHED_STATUS == tblVmSnap.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_SNAP_SWITCHING, ErrorLevel.INFO);
        }
        tblVmSnap.setPhaseStatus(VmInstanceStatus.SNAP_SWITCHING);
        tblVmSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmSnapService.updateById(tblVmSnap);
        if (!ok)
        {
            log.info("switch vmSnap {} :update database error ", snapId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        VmSnapBaseRsp vmSnapBaseRsp = new VmSnapBaseRsp();
        vmSnapBaseRsp.setSnapId(tblVmSnap.getSnapId());
        return vmSnapBaseRsp;
    }

    public void vmIsMigrating(String vmInstanceId)
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
    }

    public List<SnapsTreeRsp> getSnapsTree(String vmInstanceId, String userId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(userId) && !tblVmInstance.getUserId().equals(userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmSnap::getVmInstanceId, vmInstanceId)
                .ne(VmSnap::getPhaseStatus, REMOVED);
        List<VmSnap> tblVmSnaps = vmSnapService.list(queryWrapper);
        if (null == tblVmSnaps || tblVmSnaps.isEmpty())
        {
            return new ArrayList<>();
        }
        List<SnapsTreeRsp> snapsTreeRspList = new ArrayList<>();
        for (VmSnap tblVmSnap : tblVmSnaps)
        {
            SnapsTreeRsp snapsTreeRsp = new SnapsTreeRsp();
            snapsTreeRsp.setSnapId(tblVmSnap.getSnapId());
            snapsTreeRsp.setParentId(tblVmSnap.getParentId());
            if (StrUtil.isBlank(tblVmSnap.getParentId())) {
                snapsTreeRsp.setParentId("");
            }
            snapsTreeRsp.setSnapInfo(tblVmSnap);
            snapsTreeRspList.add(snapsTreeRsp);
        }
        Map<String, List<SnapsTreeRsp>> parentMap = snapsTreeRspList.stream().collect(Collectors.groupingBy(SnapsTreeRsp::getParentId));
        for (SnapsTreeRsp snapsTreeRsp : snapsTreeRspList)
        {
            if (parentMap.containsKey(snapsTreeRsp.getSnapId()))
            {
                snapsTreeRsp.setChildren(parentMap.get(snapsTreeRsp.getSnapId()));
            }
        }
//        snapsTreeRspList.forEach(item -> item.setChildren(parentMap.get(item.getSnapId())));
        return snapsTreeRspList.stream().filter(item -> "".equals(item.getParentId())).collect(Collectors.toList());
    }

}
