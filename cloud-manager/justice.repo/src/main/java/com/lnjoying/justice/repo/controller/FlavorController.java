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
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.repo.domain.dto.request.FlavorCreateReq;
import com.lnjoying.justice.repo.domain.dto.request.FlavorUpdateReq;
import com.lnjoying.justice.repo.domain.dto.response.FlavorBaseRsp;
import com.lnjoying.justice.repo.domain.dto.response.FlavorDetailInfoRsp;
import com.lnjoying.justice.repo.domain.dto.response.FlavorMaxNumInfoRsp;
import com.lnjoying.justice.repo.domain.dto.response.FlavorsRsp;
import com.lnjoying.justice.repo.entity.search.FlavorSearchCritical;
import com.lnjoying.justice.repo.service.biz.FlavorServiceBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author George
 * @since 2023-03-07
 */
@RestSchema(schemaId = "flavor")
@RequestMapping("/repo/v1")
@Api(value = "Flavors Controller",tags = {"Flavors Controller"})
@Slf4j
public class FlavorController {
    private static final String REG_UUID = "[0-9a-f]{32}";

    @Autowired
    private FlavorServiceBiz flavorServiceBiz;

    @LogAnnotation(resource = "计算-规格",description = "添加规格【名称:{},类型:{},CPU:{},内存:{},根盘：{}】", obtainParameter = "name,mem,cpu,rootDisk,type")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @ResponseBody
    @PostMapping(value = "/flavors", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "add flavor", response = Object.class)
    public ResponseEntity<FlavorBaseRsp> createFlavor(@ApiParam(value = "FlavorCreateReq", required = true, name = "FlavorCreateReq") @RequestBody @Valid FlavorCreateReq request)  throws IOException
    {
        try
        {
            log.info("add flavor: {}",request);
            return ResponseEntity.status(HttpStatus.CREATED).body(flavorServiceBiz.createFlavor(request, null));
        }
        catch (Exception e)
        {
            log.error("add  flavor failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/flavor/gpus", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "get available gpu", response = Object.class)
    public ResponseEntity<List<VmService.GpuFlavorInfo>> getGpuFlavors(
            @RequestHeader(name = "X-UserId", required = false) String userId
    )
    {
        try
        {
            return ResponseEntity.ok(flavorServiceBiz.getAvailableGpu());
        }
        catch (Exception e)
        {
            log.error("get available gpu failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/flavors", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "get flavors", response = Object.class)
    public ResponseEntity<FlavorsRsp> getFlavors(
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(value = "type", name = "type") @RequestParam(required=true,value = "type") Integer flavorType,
            @ApiParam(value = "gpu", name = "gpu") @RequestParam(required=false,value = "gpu") Boolean gpu,
            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
            @RequestHeader(name = "X-UserId", required = false) String userId
    )
    {
        try
        {
            FlavorSearchCritical flavorSearchCritical = new FlavorSearchCritical();
            flavorSearchCritical.setFlavorName(name);
            flavorSearchCritical.setFlavorType(flavorType);
            if (gpu != null) flavorSearchCritical.setGpu(gpu);
            if (pageNum != null) flavorSearchCritical.setPageNum(pageNum);
            if (pageSize != null)flavorSearchCritical.setPageSize(pageSize);
//            flavorSearchCritical.setUserId(userId);

            flavorSearchCritical.setUserId(null);
            FlavorsRsp getFlavorsRsp = flavorServiceBiz.getFlavors(flavorSearchCritical);

            return ResponseEntity.ok(getFlavorsRsp);
        }
        catch (Exception e)
        {
            log.error("get flavor failed: {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @LogAnnotation(resource = "计算-规格",description = "编辑规格【id:{},名称：{}】", obtainParameter = "flavorId,name")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @ResponseBody
    @PutMapping(value = "/flavors/{flavor_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update flavor", response = Object.class)
    public ResponseEntity<FlavorBaseRsp> updateFlavorName(
            @ApiParam(value = "flavor_id", required = true, name = "flavor_id") @PathVariable("flavor_id")String flavorId,
            @ApiParam(value = "FlavorUpdateReq", required = true, name = "FlavorUpdateReq") @RequestBody @Valid FlavorUpdateReq request)
    {
        try
        {
            log.info("update flavor: flavorId:{} name:{}",flavorId, request.getName());
            FlavorBaseRsp rsp= flavorServiceBiz.updateFlavor(flavorId,request.getName());
            return ResponseEntity.ok(rsp);
        }
        catch (Exception e)
        {
            log.error("update  flavor failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-规格",description = "删除规格【id：{}】",obtainParameter = "flavorId")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @ResponseBody
    @DeleteMapping(value = "/flavors/{flavor_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove flavor", response = Object.class)
    public ResponseEntity<FlavorBaseRsp> removeFlavor(
            @ApiParam(value = "flavor_id", required = true, name = "flavor_id") @PathVariable("flavor_id")String flavorId)
    {
        try
        {
            FlavorBaseRsp rsp = flavorServiceBiz.removeFlavor(flavorId);
            return ResponseEntity.ok(rsp);
        }
        catch (Exception e)
        {
            log.error("remove  flavor failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @ResponseBody
    @GetMapping(value = "/flavors/{flavor_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get flavor info", response = Object.class)
    public ResponseEntity<FlavorDetailInfoRsp> getFlavor(
            @ApiParam(value = "flavor_id", required = true, name = "flavor_id") @PathVariable("flavor_id")String flavorId)
    {
        try
        {
            FlavorDetailInfoRsp flavorDetailInfoRsp = flavorServiceBiz.getFlavor(flavorId);
            return ResponseEntity.ok(flavorDetailInfoRsp);
        }
        catch (Exception e)
        {
            log.error("get flavor failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @ResponseBody
    @GetMapping(value = "/flavors/max_info", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get max flavor info", response = Object.class)
    public ResponseEntity<FlavorMaxNumInfoRsp> getMaxFlavorInfo()
    {
        try
        {
            return ResponseEntity.ok(flavorServiceBiz.getFlavorMaxNumInfo());
        }
        catch (Exception e)
        {
            log.error("get max flavor failed: {}", e.getMessage());
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
