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

package com.lnjoying.vm.controller;

import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.domain.dto.request.InstanceGroupCreateReq;
import com.lnjoying.vm.domain.dto.response.InstanceGroupBaseRsp;
import com.lnjoying.vm.domain.dto.response.InstanceGroupsBaseRsp;
import com.lnjoying.vm.entity.InstanceGroup;
import com.lnjoying.vm.entity.search.InstanceGroupSearchCritical;
import com.lnjoying.vm.service.biz.InstanceGroupServiceBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestSchema(schemaId = "instanceGroup")
@RequestMapping("/vm/v1")
@Api(value = "Virtual Machine Group Controller", tags = {"Virtual Machine Group Controller"})
public class InstanceGroupController
{
    @Autowired
    private InstanceGroupServiceBiz instanceGroupService;

    //    虚机
    @LogAnnotation(resource = "计算-虚拟机", description = "创建虚机组【名称：{}，虚机队列：{}】", obtainParameter = "name,vmInstanceIds")
    @PostMapping(value = "/instance-groups", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create instance group", response = Object.class)
    public ResponseEntity<InstanceGroupBaseRsp> createInstanceGroup(
            @ApiParam(value = "InstanceGroupCreateReq", required = true, name = "InstanceGroupCreateReq") @RequestBody @Valid InstanceGroupCreateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("create instance group , request:{}, userId:{}", request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(instanceGroupService.addInstanceGroup(request, userId));
        }
        catch (Exception e)
        {
            log.error("create instance group error: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/instance-groups", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vm groups", response = Object.class)
    public ResponseEntity<InstanceGroupsBaseRsp> getGroups(
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get instance group list");
            InstanceGroupSearchCritical pageSearchCritical = new InstanceGroupSearchCritical();
            if (null != name) pageSearchCritical.setName(name);
            if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(instanceGroupService.getInstanceGroups(pageSearchCritical, null));
            }
            InstanceGroupsBaseRsp instanceGroupsBaseRsp = instanceGroupService.getInstanceGroups(pageSearchCritical, userId);
            return ResponseEntity.ok(instanceGroupsBaseRsp);
        }
        catch (Exception e)
        {
            log.error("get instance groups error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/instance-groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get instance group detail info", response = Object.class)
    public ResponseEntity<InstanceGroup> getVmGroupDetailInfo(
            @ApiParam(value = "groupId", required = true, name = "groupId") @PathVariable("groupId") String groupId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get instance group, request:{}, userId:{}", groupId, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(instanceGroupService.getInstanceGroup(groupId, null));
            }
            InstanceGroup tblInstanceGroup = instanceGroupService.getInstanceGroup(groupId, userId);
            return ResponseEntity.ok(tblInstanceGroup);
        }
        catch (Exception e)
        {
            log.error("get instance group error: {}, vmGroupId: {}", e.getMessage(), groupId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "编辑虚机组【名称：{}，虚拟机id：{}】", obtainParameter = "name,vmInstanceIds")
    @PutMapping(value = "/instance-groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "switch snap", response = Object.class)
    public ResponseEntity<InstanceGroupBaseRsp> switchVmGroup(
            @ApiParam(value = "groupId", required = true, name = "groupId") @PathVariable("groupId") String groupId,
            @ApiParam(value = "InstanceGroupCreateReq", required = true, name = "InstanceGroupCreateReq") @RequestBody @Valid InstanceGroupCreateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("switch instance group, request:{}, userId:{}", groupId, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(instanceGroupService.updateInstanceGroup(request, groupId, null));
            }
            return ResponseEntity.ok(instanceGroupService.updateInstanceGroup(request, groupId, userId));
        }
        catch (Exception e)
        {
            log.error("switch instance group error: {}, vmGroupId: {}", e.getMessage(), groupId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "从虚机组【id：{}】删除虚机【id:{}】", obtainParameter = "groupId,vmInstanceId")
    @DeleteMapping(value = "/instance-groups/{groupId}/{vmInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete instance group", response = Object.class)
    public ResponseEntity<InstanceGroupBaseRsp> removeVmInstanceIdFromGroup(
            @ApiParam(value = "groupId", required = true, name = "groupId") @PathVariable("groupId") String groupId,
            @ApiParam(value = "vmInstanceId", required = true, name = "vmInstanceId") @PathVariable("vmInstanceId") String vmInstanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete vmInstanceId {} from instanceGroupId: {}, userId: {}", vmInstanceId, groupId, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(instanceGroupService.removeInstancesFromGroup(vmInstanceId, groupId, null));
            }
            InstanceGroupBaseRsp result = instanceGroupService.removeInstancesFromGroup(vmInstanceId, groupId, userId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("delete instance group error: {}, vmGroupId: {}", e.getMessage(), groupId);
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "计算-虚拟机", description = "删除虚机组【id：{}】", obtainParameter = "groupId")
    @DeleteMapping(value = "/instance-groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete instance group", response = Object.class)
    public ResponseEntity<InstanceGroupBaseRsp> removeGroup(
            @ApiParam(value = "groupId", required = true, name = "groupId") @PathVariable("groupId") String groupId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete instanceGroupId: {}, userId: {}", groupId, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(instanceGroupService.removeInstanceGroup(groupId, null));
            }
            InstanceGroupBaseRsp result = instanceGroupService.removeInstanceGroup(groupId, userId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("delete instance group error: {}, vmGroupId: {}", e.getMessage(), groupId);
            throw throwWebException(e);
        }
    }

    public WebSystemException throwWebException(Exception e)
    {
        if (e instanceof WebSystemException)
        {
            return (WebSystemException) e;
        }
        else
        {
            return new WebSystemException(ErrorCode.SystemError, ErrorLevel.CRITICAL);
        }
    }
}
