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
import com.lnjoying.justice.network.domain.dto.request.BackendCreateReq;
import com.lnjoying.justice.network.domain.dto.response.BackendBaseRsp;
import com.lnjoying.justice.network.entity.search.BackendSearchCritical;
import com.lnjoying.justice.network.service.BackendService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.Pattern;

/**
 * <p>
 * 负载均衡-后端服务组 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@RestSchema(schemaId = "backend")
@RequestMapping("/network/v1")
@Controller
@Slf4j
@Api(value = "Backend Controller", tags = {"Backend Controller"})
public class BackendController  extends RestWebController
{

    private static final String REG_UUID = "[0-9a-f]{32}";
    
    @Resource
    private BackendService backendService;
    

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/backends", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get backends", response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getBackends(@ApiParam(name = "name") @RequestParam(required = false) String name,
                             @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                             @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
                             @ApiParam(name = "lb_id") @RequestParam(required = false,value = "lb_id") String lbId,
                             @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId
    ) throws WebSystemException
    {
        try
        {
            BackendSearchCritical pageSearchCritical = new BackendSearchCritical();
            pageSearchCritical.setName(name);
            pageSearchCritical.setLbId(lbId);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return backendService.getBackendServices(pageSearchCritical, userId);
            }
            return backendService.getBackendServices(pageSearchCritical, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器-后端服务组",description = "创建后端服务组【名称：{}，负载均衡器Id：{}】", obtainParameter = "name,lbId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/backends", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create new backend", response = BackendBaseRsp.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postBackend(@ApiParam(value = "CreateBackendReq", required = true, name = "CreateBackendReq")
                             @RequestBody BackendCreateReq backend
    ) throws WebSystemException
    {
        try
        {
            log.info("post backend info: {}", backend);
            BackendBaseRsp baseRsp = backendService.addBackend(backend, ServiceCombRequestUtils.getUserId());
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器-后端服务组",description = "删除后端服务组【id：{}】", obtainParameter = "backendId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/backends/{backendId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete backend", response = BackendBaseRsp.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object delBackend(@ApiParam(value = "backendId", required = true, name = "backendId")
                            @PathVariable("backendId") @Pattern(regexp = REG_UUID) String backendId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete backend, backendId: {}", backendId);
            return backendService.delBackend(backendId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器-后端服务组",description = "修改后端服务组【id：{}，名称：{}，】", obtainParameter = "name,description,vlanId")
    @PutMapping(value = "/backends/{backendId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update backend", response = BackendBaseRsp.class)
    public ResponseEntity<BackendBaseRsp> updateBackend(
            @ApiParam(value = "backendId", required = true, name = "backendId") @PathVariable("backendId") String backendId,
            @RequestBody BackendCreateReq request)
    {
        try
        {
            log.info("update backendId: backendId:{} name:{}", backendId, request);
            return ResponseEntity.ok(backendService.updateBackend(backendId, request, ServiceCombRequestUtils.getUserId()));
        }
        catch (Exception e)
        {
            log.error("update  backend failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }
}
