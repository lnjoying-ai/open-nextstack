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

import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.NfsCreateReq;
import com.lnjoying.vm.domain.dto.response.NfsBaseRsp;
import com.lnjoying.vm.domain.dto.response.NfsInfoRsp;
import com.lnjoying.vm.domain.dto.response.NfsInfosRsp;
import com.lnjoying.vm.entity.search.NfsSearchCritical;
import com.lnjoying.vm.service.biz.NfsServiceBiz;
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
@RestSchema(schemaId = "nfs")
@RequestMapping("/vm/v1/")
@Api(value = "NFS Service Controller", tags = {"NFS Service Controller"})
public class NfsController
{
    @Autowired
    private NfsServiceBiz nfsService;

    @PostMapping(value = "/nfs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create nfs server", response = Object.class)
    public ResponseEntity<NfsBaseRsp> createNfs(
            @ApiParam(value = "NfsCreateReq", required = true, name = "NfsCreateReq") @RequestBody @Valid NfsCreateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("create nfs server, request:{}, userId:{}", request, userId);
            NfsBaseRsp response = nfsService.createNfs(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        catch (Exception e)
        {
            log.error("create nfs server error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/nfs/{nfsId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get nfs detail info", response = Object.class)
    public ResponseEntity<NfsInfoRsp> getNfs(
            @ApiParam(value = "nfsId", required = true, name = "nfsId") @PathVariable("nfsId") String nfsId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get nfs, request:{}, userId:{}", nfsId, userId);
            NfsInfoRsp nfsInfoRsp = nfsService.getNfs(nfsId, userId);
            return ResponseEntity.ok(nfsInfoRsp);
        }
        catch (Exception e)
        {
            log.error("get nfs error: {}, nfsId: {}", e.getMessage(), nfsId);
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/nfs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get nfs servers", response = Object.class)
    @ResponseBody
    public ResponseEntity<NfsInfosRsp> getNfsServers(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum,
            @ApiParam(name = "name") @RequestParam(required = false) String name
    )
    {
        try
        {
            log.info("get nfs servers");
            NfsSearchCritical nfsSearchCritical = new NfsSearchCritical();
            if (StrUtil.isNotBlank(name))
            {
                log.info("get nfs servers by name:{}", name);
                nfsSearchCritical.setName(name);
            }
            if (pageSize != null && pageNum != null)
            {
                nfsSearchCritical.setPageSize(pageSize);
                nfsSearchCritical.setPageNum(pageNum);
            }
            if (ServiceCombRequestUtils.isAdmin())
            {
                NfsInfosRsp nfsInfosRsp = nfsService.listNfs(nfsSearchCritical, null);
                return ResponseEntity.ok(nfsInfosRsp);
            }
            NfsInfosRsp nfsInfosRsp = nfsService.listNfs(nfsSearchCritical, userId);
            return ResponseEntity.ok(nfsInfosRsp);
        }
        catch (Exception e)
        {
            log.error("get nfs servers error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-NFS", description = "编辑NFS【id：{},名称：{},描述：{} 】", obtainParameter = "nfsId,name,description")
    @PutMapping(value = "/nfs/{nfsId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update nfs", response = Object.class)
    public ResponseEntity<NfsBaseRsp> updateInstance(
            @ApiParam(value = "nfsId", required = true, name = "nfsId") @PathVariable("nfsId") String nfsId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("update nfs, request:{}, userId:{}", request, userId);
            NfsBaseRsp response = nfsService.updateNfs(nfsId, request, userId);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("update nfs error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-NFS", description = "删除NFS【id：{}】", obtainParameter = "nfsId")
    @DeleteMapping(value = "/nfs/{nfsId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove nfs server", response = Object.class)
    public ResponseEntity<NfsBaseRsp> removeNfs(
            @ApiParam(value = "nfsId", required = true, name = "nfsId") @PathVariable("nfsId") String nfsId,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("remove nfs server, nfsId:{}, userId:{}", nfsId, userId);
            NfsBaseRsp response = nfsService.removeNfs(nfsId, userId);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("delete nfs server error: {}", e.getMessage());
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
