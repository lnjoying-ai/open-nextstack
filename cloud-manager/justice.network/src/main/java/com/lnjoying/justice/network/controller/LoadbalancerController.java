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
import com.lnjoying.justice.network.domain.dto.request.LoadbalancerCreateReq;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancerBaseRsp;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancerDetailInfoRsp;
import com.lnjoying.justice.network.entity.search.LoadbalancerSearchCritical;
import com.lnjoying.justice.network.service.BackendService;
import com.lnjoying.justice.network.service.LoadbalancerService;
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
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

/**
 * <p>
 * 负载均衡器实例 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@RestSchema(schemaId = "loadbalancer")
@RequestMapping("/network/v1")
@Controller
@Slf4j
@Api(value = "loadbalancer Controller", tags = {"loadbalancer Controller"})
public class LoadbalancerController extends RestWebController
{

    @Autowired
    private LoadbalancerService loadbalancerService;


    private static final String REG_UUID = "[0-9a-f]{32}";

    @Resource
    private BackendService backendService;


    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/loadbalancers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get loadbalancers", response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getloadbalancers(@ApiParam(name = "name") @RequestParam(required = false) String name,
                               @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                               @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
                               @ApiParam(name = "lb_id") @RequestParam(required = false,value = "lb_id") String lbId,
                               @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId
    ) throws WebSystemException
    {
        try
        {
            LoadbalancerSearchCritical pageSearchCritical = new LoadbalancerSearchCritical();
            pageSearchCritical.setName(name);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return loadbalancerService.getLoadbalancers(pageSearchCritical, null);
            }
            return loadbalancerService.getLoadbalancers(pageSearchCritical, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器",description = "创建负载均衡器【名称：{}，subnetId：{}】", obtainParameter = "name,subnetId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/loadbalancers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create loadbalancer", response = LoadbalancerBaseRsp.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postloadbalancer(@ApiParam(value = "CreateloadbalancerReq", required = true, name = "CreateloadbalancerReq")
                               @RequestBody @Valid LoadbalancerCreateReq loadbalancer
    ) throws WebSystemException
    {
        try
        {
            log.info("post loadbalancer info: {}", loadbalancer);

            LoadbalancerBaseRsp baseRsp = loadbalancerService.addLoadbalancer(loadbalancer, ServiceCombRequestUtils.getUserId());
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器",description = "删除负载均衡器【id：{}】", obtainParameter = "loadbalancerId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/loadbalancers/{loadbalancerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete loadbalancer", response = LoadbalancerBaseRsp.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object delloadbalancer(@ApiParam(value = "loadbalancerId", required = true, name = "loadbalancerId")
                              @PathVariable("loadbalancerId") @Pattern(regexp = REG_UUID) String loadbalancerId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete loadbalancer, loadbalancerId: {}", loadbalancerId);
            return loadbalancerService.delLoadbalancer(loadbalancerId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-负载均衡器",description = "修改负载均衡器【id：{}，名称：{}，subnetId：{}】", obtainParameter = "name,subnetId,vpcId")
    @PutMapping(value = "/loadbalancers/{loadbalancerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update loadbalancer", response = LoadbalancerBaseRsp.class)
    public ResponseEntity<LoadbalancerBaseRsp> updateloadbalancer(
            @ApiParam(value = "loadbalancerId", required = true, name = "loadbalancerId") @PathVariable("loadbalancerId") String loadbalancerId,
            @RequestBody LoadbalancerCreateReq request)
    {
        try
        {
            log.info("update loadbalancerId: loadbalancerId:{} name:{}", loadbalancerId, request);
            return ResponseEntity.ok(loadbalancerService.updateLoadbalancer(loadbalancerId, request, ServiceCombRequestUtils.getUserId()));
        }
        catch (Exception e)
        {
            log.error("update  loadbalancer failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/loadbalancers/{loadbalancerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update loadbalancer", response = LoadbalancerBaseRsp.class)
    public ResponseEntity<LoadbalancerDetailInfoRsp> getLoadbalancer(
            @ApiParam(value = "loadbalancerId", required = true, name = "loadbalancerId") @PathVariable("loadbalancerId") String loadbalancerId)
    {
        try
        {
            log.info("get loadbalancerId: loadbalancerId:{} ", loadbalancerId);
            return ResponseEntity.ok(loadbalancerService.getLoadbalancerDetailInfo(loadbalancerId, ServiceCombRequestUtils.getUserId()));
        }
        catch (Exception e)
        {
            log.error("get  loadbalancer failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }
}
