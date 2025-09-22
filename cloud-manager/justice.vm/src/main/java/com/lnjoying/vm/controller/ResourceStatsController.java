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

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.domain.dto.response.InstanceStatsRsp;
import com.lnjoying.vm.domain.dto.response.StorageInfoRsp;
import com.lnjoying.vm.entity.ResourceStats;
import com.lnjoying.vm.service.biz.StatisticsServiceBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-02-07
 */
@Slf4j
@RestSchema(schemaId = "resourceStats")
@RequestMapping("/vm/v1/")
@Api(value = "Resource Stats Controller", tags = {"Resource Stats  Controller"})
public class ResourceStatsController
{
    @Autowired
    private StatisticsServiceBiz vmComputeService;

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @GetMapping(value = "/all_storage_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get storage stats", response = Object.class)
    public ResponseEntity<StorageInfoRsp> getStorageStats()
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.getAllStorageSize());
        }
        catch (Exception e)
        {
            log.error("get all storage size failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/user_storage_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user storage stats", response = Object.class)
    public ResponseEntity<StorageInfoRsp> getUserStorageStats(@RequestHeader(name = "X-UserId", required = false) String userId)
    {
        StorageInfoRsp getStorageInfoRsp = new StorageInfoRsp();
        try
        {
            getStorageInfoRsp.setUnit("GB");
            getStorageInfoRsp.setUsed((float) vmComputeService.getUserStorageSize(userId));
            return ResponseEntity.ok(getStorageInfoRsp);
        }
        catch (Exception e)
        {
            log.error("get user storage size failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/resource_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user storage stats", response = Object.class)
    public ResponseEntity<List<ResourceStats>> getResourceStats(
            @ApiParam(name = "name") @RequestParam(required = true) String name,
            @ApiParam(name = "days") @RequestParam(required = true) Integer days,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.getResourceStats(userId, name, days));
        }
        catch (Exception e)
        {
            log.error("get resource stats failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/vm_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user vm stats", response = Object.class)
    public ResponseEntity<InstanceStatsRsp> getVmStats(
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.getInstanceStats(userId));
        }
        catch (Exception e)
        {
            log.error("get vm stats failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/vm_count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user vm count", response = Object.class)
    public ResponseEntity<Long> getVmCount(
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.getVmCount(userId));
        }
        catch (Exception e)
        {
            log.error("get vm count failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/cpu_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user vm cpu stats", response = Object.class)
    public ResponseEntity<ResourceStats> getCpuStats(
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.getVmCpuSummery(userId));
        }
        catch (Exception e)
        {
            log.error("get vm cpu stats failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/mem_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user vm memory stats", response = Object.class)
    public ResponseEntity<ResourceStats> getMemStats(
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.getVmMemSummery(userId));
        }
        catch (Exception e)
        {
            log.error("get vm memory stats failed: {}", e.getMessage());
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
