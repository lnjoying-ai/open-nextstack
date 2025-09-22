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
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.SnapCreateReq;
import com.lnjoying.vm.domain.dto.response.VmSnapBaseRsp;
import com.lnjoying.vm.domain.dto.response.VmSnapDetailInfoRsp;
import com.lnjoying.vm.domain.dto.response.VmSnapsRsp;
import com.lnjoying.vm.entity.search.VmSnapSearchCritical;
import com.lnjoying.vm.service.biz.VmSnapServiceBiz;
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

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-02-07
 */
@Slf4j
@RestSchema(schemaId = "vmSnap")
@RequestMapping("/vm/v1")
@Api(value = "Virtual Machine Snap Controller", tags = {"Virtual Machine Snap Controller"})
public class VmSnapController
{
    @Autowired
    private VmSnapServiceBiz vmComputeService;

    @LogAnnotation(resource = "计算-快照", description = "创建虚拟机快照【名称：{}】", obtainParameter = "name")
    @PostMapping(value = "/snaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create vm snap", response = Object.class)
    public ResponseEntity<VmSnapBaseRsp> createVmSnap(
            @ApiParam(value = "SnapCreateReq", required = true, name = "SnapCreateReq") @RequestBody @Valid SnapCreateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("create vm snap, request:{}, userId:{}", request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(vmComputeService.addVmSnap(request, userId));
        }
        catch (Exception e)
        {
            log.error("create vm snap error: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/snaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get snaps", response = Object.class)
    public ResponseEntity<VmSnapsRsp> getSnaps(
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(name = "instance_id") @RequestParam(required = false, value = "instance_id") String instanceId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get vm snap list");
            VmSnapSearchCritical pageSearchCritical = new VmSnapSearchCritical();
            if (null != name) pageSearchCritical.setName(name);
            if (null != instanceId) pageSearchCritical.setVmInstanceId(instanceId);
            if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(vmComputeService.getSnaps(pageSearchCritical, null));
            }
            VmSnapsRsp getSnapsRsp = vmComputeService.getSnaps(pageSearchCritical, userId);
            return ResponseEntity.ok(getSnapsRsp);
        }
        catch (Exception e)
        {
            log.error("get snaps error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/snaps/{snapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vm snap detail info", response = Object.class)
    public ResponseEntity<VmSnapDetailInfoRsp> getVmSnapDetailInfo(
            @ApiParam(value = "snapId", required = true, name = "snapId") @PathVariable("snapId") String vmSnapId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get vm snap, request:{}, userId:{}", vmSnapId, userId);
            VmSnapDetailInfoRsp vmSnapInfo = vmComputeService.getSnap(vmSnapId);
            return ResponseEntity.ok(vmSnapInfo);
        }
        catch (Exception e)
        {
            log.error("get vm snap error: {}, vmSnapId: {}", e.getMessage(), vmSnapId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-快照", description = "回滚快照【id：{}】", obtainParameter = "vmSnapId")
    @PutMapping(value = "/snaps/{snapId}/switch", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "switch snap", response = Object.class)
    public ResponseEntity<VmSnapBaseRsp> switchVmSnap(
            @ApiParam(value = "snapId", required = true, name = "snapId") @PathVariable("snapId") String vmSnapId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("switch vm snap, request:{}, userId:{}", vmSnapId, userId);
            return ResponseEntity.ok(vmComputeService.switchSnap(vmSnapId, userId));
        }
        catch (Exception e)
        {
            log.error("switch vm snap error: {}, vmSnapId: {}", e.getMessage(), vmSnapId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-快照", description = "编辑快照【名称：{}，描述：{}】", obtainParameter = "name,description")
    @PutMapping(value = "/snaps/{snapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update snap", response = Object.class)
    public ResponseEntity<VmSnapBaseRsp> updateSnap(
            @ApiParam(value = "snapId", required = true, name = "snapId") @PathVariable("snapId") String snapId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("put vm snap: {}, userId:{}", request, userId);
            VmSnapBaseRsp result = vmComputeService.updateSnap(request, snapId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("update vm snap error: {}, instanceId: {}", e.getMessage(), snapId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-快照", description = "删除快照【id：{}】", obtainParameter = "snapId")
    @DeleteMapping(value = "/snaps/{snapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete vm snap", response = Object.class)
    public ResponseEntity<VmSnapBaseRsp> removeSnap(
            @ApiParam(value = "snapId", required = true, name = "snapId") @PathVariable("snapId") String snapId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete vmSnapId: {}, userId: {}", snapId, userId);
            VmSnapBaseRsp result = vmComputeService.removeSnap(snapId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("delete vm snap error: {}, vmSnapId: {}", e.getMessage(), snapId);
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
