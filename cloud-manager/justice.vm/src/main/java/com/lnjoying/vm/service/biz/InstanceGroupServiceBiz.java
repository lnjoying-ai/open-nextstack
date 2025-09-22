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
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.domain.backend.response.MonitorTagRsp;
import com.lnjoying.vm.domain.dto.request.InstanceGroupCreateReq;
import com.lnjoying.vm.domain.dto.request.InstanceGroupUpdateReq;
import com.lnjoying.vm.domain.dto.response.InstanceGroupBaseRsp;
import com.lnjoying.vm.domain.dto.response.InstanceGroupsBaseRsp;
import com.lnjoying.vm.entity.InstanceGroup;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.entity.search.InstanceGroupSearchCritical;
import com.lnjoying.vm.processor.VmInstanceTimerProcessor;
import com.lnjoying.vm.service.InstanceGroupService;
import com.lnjoying.vm.service.VmInstanceService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service
@Slf4j
public class InstanceGroupServiceBiz
{
    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private VmInstanceTimerProcessor vmInstanceTimerProcessor;

    @Autowired
    private InstanceGroupService instanceGroupService;

    @Transactional(rollbackFor = Exception.class)
    public InstanceGroupBaseRsp addInstanceGroup(InstanceGroupCreateReq addInstanceGroupReq, String userId) throws WebSystemException
    {
        InstanceGroup tblInstanceGroup = new InstanceGroup();
        tblInstanceGroup.setInstanceGroupId(Utils.assignUUId());
        tblInstanceGroup.setName(addInstanceGroupReq.getName());
        tblInstanceGroup.setUserId(userId);
        tblInstanceGroup.setDescription(addInstanceGroupReq.getDescription());
        tblInstanceGroup.setPhaseStatus(VmInstanceStatus.GROUP_CREATED);
        tblInstanceGroup.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblInstanceGroup.setUpdateTime(tblInstanceGroup.getCreateTime());
        boolean ok = instanceGroupService.save(tblInstanceGroup);
        if (!ok)
        {
            log.error("add instance group error, vmInstanceIds:{}, name:{}", addInstanceGroupReq.getVmInstanceIds(), addInstanceGroupReq.getName());
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        setInstanceGroupId(addInstanceGroupReq.getVmInstanceIds(), tblInstanceGroup.getInstanceGroupId(), AgentConstant.ADD_ACTION);

        InstanceGroupBaseRsp instanceGroupBaseRsp = new InstanceGroupBaseRsp();
        instanceGroupBaseRsp.setInstanceGroupId(tblInstanceGroup.getInstanceGroupId());
        return instanceGroupBaseRsp;
    }

    private InstanceGroupUpdateReq getInstanceGroupUpdateReq(String instanceGroupId, List<String> vmInstanceIds)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(VmInstance::getVmInstanceId)
                .eq(VmInstance::getInstanceGroupId, instanceGroupId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        List<String> oldIds = vmInstanceService.listObjs(queryWrapper, Objects::toString);

        // addInstanceIds
        HashSet<String> set = new HashSet<>(oldIds);
        vmInstanceIds.forEach(set::remove);
        List<String> removeInstanceIds = new ArrayList<>(set);
        set.clear();
        set.addAll(vmInstanceIds);
        oldIds.forEach(set::remove);
        List<String> addInstanceIds = new ArrayList<>(set);

        InstanceGroupUpdateReq req = new InstanceGroupUpdateReq();
        req.setAddInstanceIds(addInstanceIds);
        req.setRemoveInstanceIds(removeInstanceIds);
        return req;
    }


    @Transactional(rollbackFor = Exception.class)
    public InstanceGroupBaseRsp updateInstanceGroup(InstanceGroupCreateReq req, String instanceGroupId, String userId)
    {
        InstanceGroup tblInstanceGroup = getInstanceGroup(instanceGroupId, userId);
        if (!StrUtil.isBlank(req.getName()))
        {
            tblInstanceGroup.setName(req.getName());
        }
        if (!StrUtil.isBlank(req.getDescription()))
        {
            tblInstanceGroup.setDescription(req.getDescription());
        }
//        if (!StrUtil.isBlank(userId))
//        {
//            tblInstanceGroup.setUserId(userId);
//        }
        tblInstanceGroup.setUserId(userId);
        tblInstanceGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = instanceGroupService.updateById(tblInstanceGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        if (null != req.getVmInstanceIds() && req.getVmInstanceIds().size() > 0)
        {
            InstanceGroupUpdateReq instanceGroupUpdateReq = getInstanceGroupUpdateReq(instanceGroupId, req.getVmInstanceIds());
            updateInstanceIdsByGroupId(instanceGroupUpdateReq, instanceGroupId);
        }
        InstanceGroupBaseRsp instanceGroupBaseRsp = new InstanceGroupBaseRsp();
        instanceGroupBaseRsp.setInstanceGroupId(tblInstanceGroup.getInstanceGroupId());
        return instanceGroupBaseRsp;
    }

    public InstanceGroupBaseRsp removeInstanceGroup(String instanceGroupId, String userId)
    {
        InstanceGroup tblInstanceGroup = getInstanceGroup(instanceGroupId, userId);
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getInstanceGroupId, instanceGroupId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        if (vmInstanceService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.INSTANCE_GROUP_NOT_EMPTY, ErrorLevel.INFO);
        }
        tblInstanceGroup.setPhaseStatus(REMOVED);
        tblInstanceGroup.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = instanceGroupService.updateById(tblInstanceGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        InstanceGroupBaseRsp instanceGroupBaseRsp = new InstanceGroupBaseRsp();
        instanceGroupBaseRsp.setInstanceGroupId(tblInstanceGroup.getInstanceGroupId());
        return instanceGroupBaseRsp;
    }

    public InstanceGroupBaseRsp removeInstancesFromGroup(String instanceId, String instanceGroupId, String userId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(instanceId);
        if (!StrUtil.isBlank(userId))
        {
            if (!userId.equals(tblVmInstance.getUserId()))
            {
                throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
            }
        }
        tblVmInstance.setInstanceGroupId("");
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vmInstanceService.updateById(tblVmInstance);
        InstanceGroupBaseRsp instanceGroupBaseRsp = new InstanceGroupBaseRsp();
        instanceGroupBaseRsp.setInstanceGroupId(instanceGroupId);
        if (ok)
        {
            MonitorTagRsp getMonitorTagRsp = vmInstanceTimerProcessor.createMonitorTagFromAgent(tblVmInstance);
            if (null == getMonitorTagRsp)
            {
                return instanceGroupBaseRsp;
            }

            if (AgentConstant.MONITOR_TAG_OK != getMonitorTagRsp.getCode())
            {
                throw new WebSystemException(ErrorCode.INSTANCE_GROUP_CREATE_FAILED, ErrorLevel.ERROR);
            }
        }
        else
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
//        vmInstanceService.updateById(tblVmInstance);
//        InstanceGroup tblInstanceGroup = getInstanceGroup(instanceGroupId, null);
//        setInstanceGroupId(instanceIds, instanceGroupId, AgentConstant.REMOVE_ACTION);

        return instanceGroupBaseRsp;
    }

    public InstanceGroupsBaseRsp getInstanceGroups(InstanceGroupSearchCritical instanceGroupSearchCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<InstanceGroup> queryWrapper = new LambdaQueryWrapper<>();
        if (!StrUtil.isBlank(instanceGroupSearchCritical.getName()))
        {
            queryWrapper.like(InstanceGroup::getName, instanceGroupSearchCritical.getName());
        }
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(InstanceGroup::getUserId, userId);
        }
        queryWrapper.ne(InstanceGroup::getPhaseStatus, REMOVED);
        long totalNum = instanceGroupService.count(queryWrapper);
        InstanceGroupsBaseRsp instanceGroupsBaseRsp = new InstanceGroupsBaseRsp();
        instanceGroupsBaseRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return instanceGroupsBaseRsp;
        }

        //query with page number and page size
//        int begin = ((vmSnapSearchCritical.getPageNum() - 1) * vmSnapSearchCritical.getPageSize());
        queryWrapper.orderByDesc(InstanceGroup::getCreateTime);

        //to do
        Page<InstanceGroup> page = new Page<>(instanceGroupSearchCritical.getPageNum(), instanceGroupSearchCritical.getPageSize());
        Page<InstanceGroup> instanceGroupPage = instanceGroupService.page(page, queryWrapper);
        List<InstanceGroup> tblInstanceGroups = instanceGroupPage.getRecords();
        List<InstanceGroupsBaseRsp.InstanceGroupInfo> instanceGroupInfos = new ArrayList<>();
        for (InstanceGroup tblInstanceGroup : tblInstanceGroups)
        {
            InstanceGroupsBaseRsp.InstanceGroupInfo instanceGroupInfo = new InstanceGroupsBaseRsp.InstanceGroupInfo();
            instanceGroupInfo.setInstanceGroupId(tblInstanceGroup.getInstanceGroupId());
            instanceGroupInfo.setName(tblInstanceGroup.getName());
            instanceGroupInfo.setDescription(tblInstanceGroup.getDescription());
            instanceGroupInfo.setCreateTime(tblInstanceGroup.getCreateTime());
            instanceGroupInfos.add(instanceGroupInfo);
            LambdaQueryWrapper<VmInstance> vmInstanceLambdaQueryWrapper = new LambdaQueryWrapper<>();
            vmInstanceLambdaQueryWrapper.eq(VmInstance::getInstanceGroupId, tblInstanceGroup.getInstanceGroupId())
                    .ne(VmInstance::getPhaseStatus, REMOVED);
            instanceGroupInfo.setInstanceCount(vmInstanceService.count(vmInstanceLambdaQueryWrapper));
        }
        instanceGroupsBaseRsp.setInstanceGroupInfos(instanceGroupInfos);
        return instanceGroupsBaseRsp;
    }

    public InstanceGroup getInstanceGroup(String instanceGroupId, String userId)
    {
        InstanceGroup instanceGroup = instanceGroupService.getById(instanceGroupId);
        if (null == instanceGroup || REMOVED == instanceGroup.getPhaseStatus())
        {
            log.info("instance group not exists, instanceGroupId:{}", instanceGroupId);
            throw new WebSystemException(ErrorCode.INSTANCE_GROUP_NOT_EXIST, ErrorLevel.ERROR);
        }
        if (!StrUtil.isBlank(userId) && !Objects.equals(instanceGroup.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }

        return instanceGroup;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInstanceIdsByGroupId(InstanceGroupUpdateReq req, String instanceGroupId)
    {
        if (null != req.getAddInstanceIds() && req.getAddInstanceIds().size() > 0)
        {
            List<String> addInstanceIds = req.getAddInstanceIds();
            setInstanceGroupId(addInstanceIds, instanceGroupId, AgentConstant.ADD_ACTION);
        }

        if (null != req.getRemoveInstanceIds() && req.getRemoveInstanceIds().size() > 0)
        {
            List<String> removeInstanceIds = req.getRemoveInstanceIds();
            setInstanceGroupId(removeInstanceIds, instanceGroupId, AgentConstant.REMOVE_ACTION);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void setInstanceGroupId(List<String> vmInstanceIds, String instanceGroupId, String action)
    {

        if (null == vmInstanceIds || vmInstanceIds.isEmpty())
        {
            return;
        }
        HashSet<String> vmInstanceIdSet = new HashSet<>(vmInstanceIds);
        for (String vmInstanceId : vmInstanceIdSet)
        {
            VmInstance vmInstance = vmInstanceService.getById(vmInstanceId);
            if (null == vmInstance || REMOVED == vmInstance.getPhaseStatus())
            {
                throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
            }
            if (AgentConstant.ADD_ACTION.equals(action))
            {
                if (!StrUtil.isBlank(vmInstance.getInstanceGroupId()))
                {
                    throw new WebSystemException(ErrorCode.VM_INSTANCE_HAS_GROUP, ErrorLevel.INFO);
                }
                vmInstance.setInstanceGroupId(instanceGroupId);
            }
            else if (AgentConstant.REMOVE_ACTION.equals(action))
            {
                if (!instanceGroupId.equals(vmInstance.getInstanceGroupId()))
                {
                    throw new WebSystemException(ErrorCode.INSTANCE_GROUP_ID_NOT_CORRECT, ErrorLevel.INFO);
                }
                vmInstance.setInstanceGroupId("");
            }
            else
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            vmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = vmInstanceService.updateById(vmInstance);
//            vmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RUNNING);
            if (ok)
            {
                MonitorTagRsp getMonitorTagRsp = vmInstanceTimerProcessor.createMonitorTagFromAgent(vmInstance);
                if (getMonitorTagRsp == null)
                {
                    continue;
                }
                if (AgentConstant.MONITOR_TAG_OK != getMonitorTagRsp.getCode())
                {
                    throw new WebSystemException(ErrorCode.INSTANCE_GROUP_CREATE_FAILED, ErrorLevel.ERROR);
                }
            }
//            vmInstanceService.updateById(vmInstance);
        }
    }

}
