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

import com.lnjoying.justice.commonweb.controller.RestWebController;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.vm.config.ComputeMonitorConfig;
import com.lnjoying.vm.config.GpuMonitorConfig;
import com.lnjoying.vm.config.VmMonitorConfig;
import com.lnjoying.vm.domain.dto.response.HasPermissionRsp;
import com.lnjoying.vm.service.biz.MonitorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestSchema(schemaId = "monitor")
@RequestMapping("/vm/v1/monitor")
@Api(value = "Monitor Controller", tags = {"Monitor Controller"})
@Slf4j
public class MonitorController extends RestWebController
{

    @Autowired
    private MonitorService monitorService;


    public MonitorController()
    {
        System.out.println("Monitor Controller");
    }


    @GetMapping(value = "/has_permission", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "whether you have permission on the resource", response = Object.class)
    public ResponseEntity<HasPermissionRsp> hasPermission(
            @ApiParam(name = "var-project_name") @RequestParam(required = false) String projectId,
            @ApiParam(name = "var-vm_name") @RequestParam(required = false) String vmName,
            @ApiParam(name = "var-vm_instance_id") @RequestParam(required = false) String vmInstanceId,
            @ApiParam(name = "var-compute_node") @RequestParam(required = false) int computeNodeId,
            @ApiParam(name = "var-node") @RequestParam(required = false) String nodeId,
            @ApiParam(name = "var-vm_user_id") @RequestParam(required = false) String queryUserId,
            @ApiParam(name = "dashboard_id") @RequestParam(required = false) String dashboardId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get monitor info : vmInstanceId: {}, userId: {}, dashboardId: {}", vmInstanceId, userId, dashboardId);
            HasPermissionRsp result = new HasPermissionRsp();
            result.setHasPermission(monitorService.hasPermission(vmInstanceId, userId, dashboardId, queryUserId, nodeId));
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {

            log.error("get resource monitor error: , vmInstanceId: {}", vmInstanceId, e);
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/vm_panels", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vm panels", response = Object.class)
    public ResponseEntity<VmMonitorConfig> getVmPanels(
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            return ResponseEntity.ok(monitorService.getVmAllPanels());
        }
        catch (Exception e)
        {
            log.error("get vm monitor error: ", e);
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/compute_panels", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get node panels", response = Object.class)
    public ResponseEntity<ComputeMonitorConfig> getComputePanels(
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            return ResponseEntity.ok(monitorService.getComputeAllPanels());
        }
        catch (Exception e)
        {
            log.error("get node monitor error: ", e);
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/gpu_panels", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get gpu panels", response = Object.class)
    public ResponseEntity<GpuMonitorConfig> getGpuPanels(
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            return ResponseEntity.ok(monitorService.getGpuAllPanels());
        }
        catch (Exception e)
        {
            log.error("get gpu monitor error: ", e);
            throw throwWebException(e);
        }
    }
}
