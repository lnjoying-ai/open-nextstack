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
import com.lnjoying.justice.schema.entity.search.PageSearchCritical;
import com.lnjoying.vm.domain.dto.request.PciDeviceAttachReq;
import com.lnjoying.vm.domain.dto.response.HypervisorNodeInfo;
import com.lnjoying.vm.domain.dto.response.PciDeviceBaseRsp;
import com.lnjoying.vm.domain.dto.response.PciDeviceDetailInfo;
import com.lnjoying.vm.domain.dto.response.PciDeviceInfo;
import com.lnjoying.vm.entity.search.PciDeviceSearchCritical;
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
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author George
 * @since 2023-03-27
 */
@Slf4j
@RestSchema(schemaId = "pciDevice")
@RequestMapping("/vm/v1/")
@Api(value = "PCI Device Controller", tags = {"PCI Device Controller"})
public class PciDeviceController
{
    @Autowired
    private PciDeviceServiceBiz pciDeviceServiceBiz;

    @GetMapping(value = "/pci_devices", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get device", response = Object.class)
    @ResponseBody
    public ResponseEntity<List<PciDeviceDetailInfo>> getDevices(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum,
            @ApiParam(name = "device_name") @RequestParam(required = false, value = "device_name") String pciDeviceName,
//            @ApiParam(name = "device_group_id") @RequestParam(required = false,value = "device_group_id") String pciDeviceGroupId,
            @ApiParam(name = "node_id") @RequestParam(required = true, value = "node_id") String nodeId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get pci device list");
            PciDeviceSearchCritical pageSearchCritical = new PciDeviceSearchCritical();
            if (!StrUtil.isBlank(pciDeviceName)) pageSearchCritical.setPciDeviceName(pciDeviceName);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);

            List<PciDeviceDetailInfo> pciDeviceInfos = pciDeviceServiceBiz.getPciDevices(pageSearchCritical, nodeId);
            return ResponseEntity.ok(pciDeviceInfos);
        }
        catch (Exception e)
        {
            log.error("get devices error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/pci_devices/{device_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get device", response = Object.class)
    @ResponseBody
    public ResponseEntity<PciDeviceDetailInfo> getDevice(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(value = "device_id", required = true, name = "device_id") @PathVariable("device_id") String deviceId) throws WebSystemException
    {
        try
        {
            log.info("get pci device detail, device_id: {}", deviceId);
            PciDeviceDetailInfo pciDeviceDetailInfo = pciDeviceServiceBiz.getPciDeviceDetailInfo(deviceId);
            return ResponseEntity.ok(pciDeviceDetailInfo);
        }
        catch (Exception e)
        {
            log.error("get device error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "计算-虚拟机", description = "绑定pci【虚机id：{}】", obtainParameter = "vmInstanceId")
    @PutMapping(value = "/pci_devices/{device_id}/attach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "attach pci device")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public PciDeviceBaseRsp attachDevice(
            @ApiParam(value = "device_id", required = true, name = "device_id") @PathVariable("device_id") String deviceId,
            @ApiParam(value = "pciDeviceAttachReq", required = true, name = "pciDeviceAttachReq") @RequestBody @Valid PciDeviceAttachReq pciDeviceAttachReq,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {

        try
        {
            log.info("attach pci device: deviceId:{} vmId:{}", deviceId, pciDeviceAttachReq.getVmInstanceId());
            if (ServiceCombRequestUtils.isAdmin())
            {
                return pciDeviceServiceBiz.attachPciDevice(pciDeviceAttachReq.getVmInstanceId(), deviceId, null);
            }
            return pciDeviceServiceBiz.attachPciDevice(pciDeviceAttachReq.getVmInstanceId(), deviceId, userId);
        }
        catch (Exception e)
        {
            log.error("attach devices error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "卸载pci【id：{}】", obtainParameter = "deviceId")
    @PutMapping(value = "/pci_devices/{device_id}/detach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "detach pci device")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public PciDeviceBaseRsp detachPci(
            @ApiParam(value = "device_id", required = true, name = "device_id") @PathVariable("device_id") String deviceId,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {

        try
        {
            log.info("detach pci device: deviceId:{} ", deviceId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return pciDeviceServiceBiz.detachPciDevice(deviceId, null);
            }
            return pciDeviceServiceBiz.detachPciDevice(deviceId, userId);
        }
        catch (Exception e)
        {
            log.error("detach device error: {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @GetMapping(value = "/pci_devices/available_nodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get device", response = Object.class)
    @ResponseBody
    public ResponseEntity<List<HypervisorNodeInfo>> getAvailableNodes(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(name = "is_gpu") @RequestParam(required = true, value = "is_gpu") Boolean isGpu,
            @ApiParam(name = "vm_id") @RequestParam(required = false, value = "vm_id") String vmId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum
    ) throws WebSystemException
    {
        try
        {
            PageSearchCritical pageSearchCritical = new PciDeviceSearchCritical();
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(pciDeviceServiceBiz.getAvailableNode(pageSearchCritical, vmId, null, isGpu));
            }
            return ResponseEntity.ok(pciDeviceServiceBiz.getAvailableNode(pageSearchCritical, vmId, userId, isGpu));
        }
        catch (Exception e)
        {
            log.error("get devices error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    @GetMapping(value = "/pci_devices/available_devices", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get device", response = Object.class)
    @ResponseBody
    public ResponseEntity<List<PciDeviceInfo>> getAvailableDevices(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(name = "vm_id") @RequestParam(required = false, value = "vm_id") String vmId,
            @ApiParam(name = "node_id") @RequestParam(required = true, value = "node_id") String nodeId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum
    ) throws WebSystemException
    {
        try
        {
            PageSearchCritical pageSearchCritical = new PciDeviceSearchCritical();
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(pciDeviceServiceBiz.getAvailableDeviceInfos(pageSearchCritical, nodeId, vmId, null));
            }
            return ResponseEntity.ok(pciDeviceServiceBiz.getAvailableDeviceInfos(pageSearchCritical, nodeId, vmId, userId));
        }
        catch (Exception e)
        {
            log.error("get devices error: {}", e.getMessage());
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
