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

package com.lnjoying.justice.network.controller;

import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.controller.RestWebController;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.network.domain.dto.request.FrontendCreateReq;
import com.lnjoying.justice.network.domain.dto.response.FrontendBaseRsp;
import com.lnjoying.justice.network.entity.search.FrontendSearchCritical;
import com.lnjoying.justice.network.service.BackendService;
import com.lnjoying.justice.network.service.FrontendService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.Pattern;

/**
 * <p>
 * 负载均衡器-监听器 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@RestSchema(schemaId = "frontend")
@RequestMapping("/network/v1")
@Controller
@Slf4j
@Api(value = "Frontend Controller", tags = {"Frontend Controller"})
public class FrontendController  extends RestWebController
{


    @Autowired
    private FrontendService frontendService;


    private static final String REG_UUID = "[0-9a-f]{32}";

    @Resource
    private BackendService backendService;


    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/frontends", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get frontends", response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getFrontends(@ApiParam(name = "name") @RequestParam(required = false) String name,
                              @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                              @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
                              @ApiParam(name = "lb_id") @RequestParam(required = false,value = "lb_id") String lbId,
                              @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId
    ) throws WebSystemException
    {
        try
        {
            FrontendSearchCritical pageSearchCritical = new FrontendSearchCritical();
            pageSearchCritical.setName(name);
            pageSearchCritical.setLbId(lbId);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return frontendService.getFrontendServices(pageSearchCritical, userId);
            }
            return frontendService.getFrontendServices(pageSearchCritical, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器-监听器",description = "创建监听器【名称：{}，负载均衡器Id：{}】", obtainParameter = "name,lbId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/frontends", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create new frontend", response = FrontendBaseRsp.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postFrontend(@ApiParam(value = "CreateFrontendReq", required = true, name = "CreateFrontendReq")
                              @RequestBody FrontendCreateReq frontend
    ) throws WebSystemException
    {
        try
        {
            log.info("post frontend info: {}", frontend);
            FrontendBaseRsp baseRsp = frontendService.addFrontend(frontend, ServiceCombRequestUtils.getUserId());
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器-监听器",description = "删除监听器【id：{}】", obtainParameter = "frontendId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/frontends/{frontendId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete frontend", response = FrontendBaseRsp.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object delFrontend(@ApiParam(value = "frontendId", required = true, name = "frontendId")
                             @PathVariable("frontendId") @Pattern(regexp = REG_UUID) String frontendId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete frontend, frontendId: {}", frontendId);
            return frontendService.delFrontend(frontendId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器-监听器",description = "修改监听器【id：{}，名称：{}，】", obtainParameter = "name,description,vlanId")
    @PutMapping(value = "/frontends/{frontendId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update frontend", response = FrontendBaseRsp.class)
    public ResponseEntity<FrontendBaseRsp> updateFrontend(
            @ApiParam(value = "frontendId", required = true, name = "frontendId") @PathVariable("frontendId") String frontendId,
            @RequestBody FrontendCreateReq request)
    {
        try
        {
            log.info("update frontendId: frontendId:{} name:{}", frontendId, request);
            return ResponseEntity.ok(frontendService.updateFrontend(frontendId, request, ServiceCombRequestUtils.getUserId()));
        }
        catch (Exception e)
        {
            log.error("update  frontend failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }
}
