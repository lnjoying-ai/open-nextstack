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

package com.lnjoying.justice.repo.controller;

import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.response.*;
import com.lnjoying.justice.repo.entity.search.StoragePoolSearchCritical;
import com.lnjoying.justice.repo.service.biz.StoragePoolServiceBiz;
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

import javax.validation.Valid;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author George
 * @since 2023-03-07
 */
@RestSchema(schemaId = "storagePool")
@RequestMapping("/repo/v1")
@Api(value = "StoragePool Controller",tags = {"StoragePool Controller"})
@Slf4j
public class StoragePoolController {
    @Autowired
    private StoragePoolServiceBiz storagePoolServiceBiz;

    @GetMapping(value = "/storage_pools", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get storagePools", response = StoragePoolsRsp.class)
    public ResponseEntity<StoragePoolsRsp> getStoragePools(
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(value = "type", name = "type") @RequestParam(required=false,value = "type") Integer poolType,
            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
            @RequestHeader(name = "X-UserId", required = false) String userId
    )
    {
        try
        {
            StoragePoolSearchCritical storagePoolCritical = new StoragePoolSearchCritical();
            storagePoolCritical.setPoolName(name);
            storagePoolCritical.setPoolType(poolType);
            if (pageNum != null) storagePoolCritical.setPageNum(pageNum);
            if (pageNum != null) storagePoolCritical.setPageSize(pageSize);
//            flavorSearchCritical.setUserId(userId);

//            flavorSearchCritical.setUserId(null);
            StoragePoolsRsp storagePoolsRsp = storagePoolServiceBiz.getStoragePools(storagePoolCritical);

            return ResponseEntity.ok(storagePoolsRsp);
        }
        catch (Exception e)
        {
            log.error("get storagePool failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/storage_pools/{storage_pool_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get storagePool info", response = StoragePoolDetailInfoRsp.class)
    public ResponseEntity<StoragePoolDetailInfoRsp> getStoragePool(
            @ApiParam(value = "storage_pool_id", required = true, name = "storage_pool_id") @PathVariable("storage_pool_id")String storagePoolId)
    {
        try
        {
            StoragePoolDetailInfoRsp storagePoolDetailInfoRsp = storagePoolServiceBiz.getStoragePool(storagePoolId);
            return ResponseEntity.ok(storagePoolDetailInfoRsp);
        }
        catch (Exception e)
        {
            log.error("get storagePool failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "硬件设施-存储池", description = "编辑存储池【id：{}，名称：{}，描述：{}】",obtainParameter = "storagePoolId,name,description")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @PutMapping(value = "/storage_pools/{storage_pool_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update storagePool", response = StoragePoolBaseRsp.class)
    public ResponseEntity<StoragePoolBaseRsp> updateStoragePool(
            @ApiParam(value = "storage_pool_id", required = true, name = "storage_pool_id") @PathVariable("storage_pool_id")String storagePoolId,
            @ApiParam(value = "commonReq", required = true, name = "commonReq") @RequestBody @Valid CommonReq request)
    {
        try
        {
            log.info("update storagePool: storagePoolId:{} name:{}",storagePoolId, request.getName());
            StoragePoolBaseRsp rsp= storagePoolServiceBiz.updateStoragePool(storagePoolId, request);
            return ResponseEntity.ok(rsp);
        }
        catch (Exception e)
        {
            log.error("update  storagePool failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    public WebSystemException throwWebException(Exception e)
    {
        if (e instanceof WebSystemException)
        {
            return  (WebSystemException)e;
        }
        else
        {
            return new WebSystemException(ErrorCode.SystemError , ErrorLevel.CRITICAL);
        }
    }
}
