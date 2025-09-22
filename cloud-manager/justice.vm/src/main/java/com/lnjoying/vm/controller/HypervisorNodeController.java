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
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.entity.search.PageSearchCritical;
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.HypervisorNodeAddReq;
import com.lnjoying.vm.domain.dto.response.HypervisorNodeAllocationInfo;
import com.lnjoying.vm.domain.dto.response.HypervisorNodeBaseRsp;
import com.lnjoying.vm.domain.dto.response.NodeAllocationInfosRsp;
import com.lnjoying.vm.entity.search.HypervisorNodeSearchCritical;
import com.lnjoying.vm.service.biz.HypervisorNodeServiceBiz;
import com.lnjoying.vm.service.biz.PciDeviceServiceBiz;
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

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-02-07
 */
@Slf4j
@RestSchema(schemaId = "hypervisorNode")
@RequestMapping("/vm/v1/")
@Api(value = "Hypervisor Node Controller", tags = {"Virtual Machine Controller"})
public class HypervisorNodeController
{

    @Autowired
    private HypervisorNodeServiceBiz vmComputeService;

    @Autowired
    private PciDeviceServiceBiz pciDeviceService;


    @LogAnnotation(resource = "计算-虚拟机", description = "添加虚拟机计算节点【管理IP：{}】", obtainParameter = "manageIp")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @PostMapping(value = "/hypervisor_nodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "add hypervisor node", response = Object.class)
    public ResponseEntity<HypervisorNodeBaseRsp> addHypervisorNode(@ApiParam(value = "HypervisorNodeAddReq", required = true, name = "HypervisorNodeAddReq") @RequestBody @Valid HypervisorNodeAddReq request)
    {
        try
        {
            log.info("add hypervisor node: {}", request);
            return ResponseEntity.status(HttpStatus.CREATED).body(vmComputeService.addHypervisorNode(request));
        }
        catch (Exception e)
        {
            log.error("add  hypervisor node failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "编辑虚拟机计算节点【名称：{}，描述：{}】", obtainParameter = "name,description")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @PutMapping(value = "/hypervisor_nodes/{nodeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update hypervisor node", response = Object.class)
    public ResponseEntity<HypervisorNodeBaseRsp> updateHypervisorNode(
            @ApiParam(value = "nodeId", required = true, name = "nodeId") @PathVariable("nodeId") String nodeId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request)
    {
        try
        {
            log.info("update hypervisor node: hypervisorNodeId:{} request:{} {}", nodeId, request.getName(), request.getDescription());
            HypervisorNodeBaseRsp result = vmComputeService.updateHypervisorNode(nodeId, request);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("update hypervisor node failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "删除虚拟机计算节点【id：{}】", obtainParameter = "nodeId")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @DeleteMapping(value = "/hypervisor_nodes/{nodeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove hypervisor node", response = Object.class)
    public ResponseEntity<HypervisorNodeBaseRsp> removeHypervisorNode(
            @ApiParam(value = "nodeId", required = true, name = "nodeId") @PathVariable("nodeId") String nodeId)
    {
        try
        {
            HypervisorNodeBaseRsp result = vmComputeService.removeHypervisorNode(nodeId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("remove  hypervisor node failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @GetMapping(value = "/hypervisor_nodes/{nodeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get hypervisor node info", response = Object.class)
    public ResponseEntity<HypervisorNodeAllocationInfo> getHypervisorNode(
            @ApiParam(value = "nodeId", required = true, name = "nodeId") @PathVariable("nodeId") String nodeId)
    {
        try
        {
            HypervisorNodeAllocationInfo allocationInfo = pciDeviceService.resourceAllocation(nodeId);
            if (null != allocationInfo)
            {
                if (null == allocationInfo.getMemSum())
                {
                    allocationInfo.setMemSum(0);
                }
                if (null == allocationInfo.getCpuSum())
                {
                    allocationInfo.setCpuSum(0);
                }
            }


            return ResponseEntity.ok(allocationInfo);
        }
        catch (Exception e)
        {
            log.error("get hypervisor node failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @GetMapping(value = "/hypervisor_nodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get hypervisor nodes", response = Object.class)
    public ResponseEntity<NodeAllocationInfosRsp> getHypervisorNodes(
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(name = "is_healthy") @RequestParam(required = false) Boolean isHealthy,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum)
    {
        try
        {
            log.debug("get hypervisor node list");
            HypervisorNodeSearchCritical pageSearchCritical = new HypervisorNodeSearchCritical();
            if (null != name) pageSearchCritical.setName(name);
            if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
            if (null != isHealthy) pageSearchCritical.setIsHealthy(isHealthy);
            NodeAllocationInfosRsp getHypervisorNodesRsp = vmComputeService.getHypervisorNodes(pageSearchCritical);
            return ResponseEntity.ok(getHypervisorNodesRsp);
        }
        catch (Exception e)
        {
            log.error("get hypervisor node list failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @GetMapping(value = "/hypervisor_nodes/allocation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get hypervisor nodes", response = Object.class)
    public ResponseEntity<NodeAllocationInfosRsp> getResourceAllocation(
            @ApiParam(name = "flavor_id") @RequestParam(required = true, value = "flavor_id") String flavorId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum)
    {
        try
        {
            PageSearchCritical pageSearchCritical = new PageSearchCritical();
            if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
            NodeAllocationInfosRsp allocationInfosRsp = pciDeviceService.getResourceAllocation(pageSearchCritical, flavorId);
            return ResponseEntity.ok(allocationInfosRsp);
        }
        catch (Exception e)
        {
            log.error("get hypervisor node list failed: {}", e.getMessage());
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
