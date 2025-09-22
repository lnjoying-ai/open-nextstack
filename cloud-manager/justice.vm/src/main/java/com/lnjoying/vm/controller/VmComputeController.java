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
import com.lnjoying.justice.commonweb.controller.RestWebController;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.vm.domain.backend.response.IsoIsInjectedRsp;
import com.lnjoying.vm.domain.dto.request.*;
import com.lnjoying.vm.domain.dto.response.SnapsTreeRsp;
import com.lnjoying.vm.domain.dto.response.VmInstanceBaseRsp;
import com.lnjoying.vm.domain.dto.response.VmInstanceDetailInfoRsp;
import com.lnjoying.vm.domain.dto.response.VmInstancesRsp;
import com.lnjoying.vm.entity.search.VmInstanceSearchCritical;
import com.lnjoying.vm.service.biz.VmInstanceServiceBiz;
import com.lnjoying.vm.service.biz.VmSnapServiceBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestSchema(schemaId = "vm")
@RequestMapping("/vm/v1/")
@Api(value = "Virtual Machine Controller", tags = {"Virtual Machine Controller"})
public class VmComputeController extends RestWebController
{
//    private static final Logger log = LogManager.getLogger();

    @Autowired
    private VmInstanceServiceBiz vmComputeService;

    @Autowired
    private VmSnapServiceBiz vmSnapService;

    public VmComputeController()
    {
        System.out.println("VmComputeController");
    }

    // vm
//    @LogAnnotation(resource = "计算-虚拟机",description = "创建虚拟机【名称：{}】", obtainParameter = "name")
    @PostMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create vm instance", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> createVmInstance(
            @ApiParam(value = "VmInstanceCreateReq", required = true, name = "VmInstanceCreateReq") @RequestBody @Valid VmInstanceCreateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("create vm instance, request:{}, userId:{}", request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(vmComputeService.addVmInstance(request, userId));
        }
        catch (Exception e)
        {
            log.error("create vm instance error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @PostMapping(value = "/instances/counts", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create vm instances", response = Object.class)
    public ResponseEntity<String> createVmInstances(
            @ApiParam(value = "VmInstancesCreateReq", required = true, name = "VmInstancesCreateReq") @RequestBody @Valid VmInstancesCreateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("create vm instances, request:{}, userId:{}", request, userId);
            vmComputeService.addVmInstances(request.getVmInstanceCreateReq(), request.getCount(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body("start creating the vm instances");
        }
        catch (Exception e)
        {
            log.error("create vm instance error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "重建虚拟机")
    @PostMapping(value = "/instances/renews", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "renew vm instance", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> renewVmInstance(
            @ApiParam(value = "VmInstanceRenewReq", required = true, name = "VmInstanceRenewReq") @RequestBody @Valid VmInstanceRenewReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("create vm instance, request:{}, userId:{}", request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(vmComputeService.addVmInstance(request, userId));
        }
        catch (Exception e)
        {
            log.error("create vm instance error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "绑定安全组【id：{}】", obtainParameter = "vmInstanceId")
    @PostMapping(value = "/instances/{instanceId}/bound_sgs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "bound sgs", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> boundSgs(
            @ApiParam(value = "SgsUpdateReq", required = true, name = "SgsUpdateReq") @RequestBody @Valid SgsUpdateReq request,
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String vmInstanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("bound security groups, vmInstanceId:{}, userId:{}, sgIds:{}", vmInstanceId, userId, request.getSgIds());
            return ResponseEntity.status(HttpStatus.CREATED).body(vmComputeService.boundSgs(request, vmInstanceId, userId));
        }
        catch (Exception e)
        {
            log.error("bound security groups error: {}, vmId:{}, sgIds:{}", e.getMessage(), vmInstanceId, request.getSgIds());
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "计算-虚拟机", description = "修改绑定安全组【id：{}】", obtainParameter = "vmInstanceId")
    @PutMapping(value = "/instances/{instanceId}/bound_sgs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update sgs", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> updateBoundSgs(
            @ApiParam(value = "UpdateSgsReq", required = true, name = "UpdateSgsReq") @RequestBody @Valid SgsUpdateReq request,
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String vmInstanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("update security groups, vmInstanceId:{}, userId:{}, sgIds:{}", vmInstanceId, userId, request.getSgIds());
            return ResponseEntity.status(HttpStatus.OK).body(vmComputeService.updateSgs(request, vmInstanceId, userId));
        }
        catch (Exception e)
        {
            log.error("update security groups error: {}, vmId:{}, sgIds:{}", e.getMessage(), vmInstanceId, request.getSgIds());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/instances/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vm instance detail info", response = Object.class)
    public ResponseEntity<VmInstanceDetailInfoRsp> getVmInstanceDetailInfo(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String vmInstanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get vm instance, request:{}, userId:{}", vmInstanceId, userId);
            VmInstanceDetailInfoRsp vmDetailInfo = vmComputeService.getVmInstance(vmInstanceId);
            return ResponseEntity.ok(vmDetailInfo);
        }
        catch (Exception e)
        {
            log.error("get vm instance error: {}, vmInstanceId: {}", e.getMessage(), vmInstanceId);
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "计算-虚拟机", description = "关闭虚拟机【id：{}】", obtainParameter = "instanceId")
    @PutMapping(value = "/instances/{instanceId}/poweroff", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "power off instance by instance id", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> poweroffInstance(@ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
                                                              // 是否卸载GPU
                                                              @ApiParam(name = "detachment") @RequestParam(required = false) Boolean detachment,
                                                              @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("power off vm instance, request:{}, userId:{}", instanceId, userId);
            if (null != detachment && detachment)
            {
                return ResponseEntity.ok(vmComputeService.powerOffInstanceWithNoPci(instanceId));
            }
            return ResponseEntity.ok(vmComputeService.powerOffInstance(instanceId));
        }
        catch (Exception e)
        {
            log.error("power off vm instance error: {}, vmInstanceId: {}", e.getMessage(), instanceId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "打开虚拟机【id：{}】", obtainParameter = "instanceId")
    @PutMapping(value = "/instances/{instanceId}/poweron", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "power off instance by instance id", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> poweronInstance(@ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
                                                             @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("power on vm instance, request:{}, userId:{}", instanceId, userId);
            return ResponseEntity.ok(vmComputeService.powerOnInstance(instanceId));
        }
        catch (Exception e)
        {
            log.error("power on vm instance error: {}, vmInstanceId: {}", e.getMessage(), instanceId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "重启虚拟机【id：{}】", obtainParameter = "instanceId")
    @PutMapping(value = "/instances/{instanceId}/reboot", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "restart instance by instance id", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> restartInstance(@ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
                                                             @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("restart vm instance, request:{}, userId:{}", instanceId, userId);
            return ResponseEntity.ok(vmComputeService.rebootInstance(instanceId));
        }
        catch (Exception e)
        {
            log.error("restart vm instance error: {}, vmInstanceId: {}", e.getMessage(), instanceId);
            throw throwWebException(e);
        }
    }


    @GetMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get instances", response = Object.class)
    @ResponseBody
    public ResponseEntity<VmInstancesRsp> getInstances(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum,
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(name = "uuid") @RequestParam(required = false) String vmInstanceId,
            @ApiParam(name = "subnet_id") @RequestParam(required = false, value = "subnet_id") String subnetId,
            @ApiParam(name = "node_id") @RequestParam(required = false, value = "node_id") String nodeId,
            @ApiParam(name = "port_id_is_null") @RequestParam(required = false, value = "port_id_is_null") Boolean portIdIsNull,
            @ApiParam(name = "instance_group_id") @RequestParam(required = false, value = "instance_group_id") String instanceGroupId,
            @ApiParam(name = "instance_group_id_is_null") @RequestParam(required = false, value = "instance_group_id_is_null") Boolean instanceGroupIdIsNull,
            @ApiParam(name = "eipMap_is_using") @RequestParam(required = false, value = "eipMap_is_using") Boolean eipMapIsUsing,
            @ApiParam(name = "eip_id_is_null") @RequestParam(required = false, value = "eip_id_is_null") Boolean eipIdIsNull,
            @ApiParam(name = "ignore_failed") @RequestParam(required = false, value = "ignore_failed") Boolean ignoreFailed,
            @ApiParam(name = "eip_id") @RequestParam(required = false, value = "eip_id") String eipId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get vm instance list");
            VmInstanceSearchCritical pageSearchCritical = new VmInstanceSearchCritical();
            pageSearchCritical.setName(name);
            if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
            if (null != portIdIsNull) pageSearchCritical.setPortIdIsNull(portIdIsNull);
            if (null != eipMapIsUsing) pageSearchCritical.setEipMapIsUsing(eipMapIsUsing);
            if (null != instanceGroupId) pageSearchCritical.setInstanceGroupId(instanceGroupId);
            if (null != instanceGroupIdIsNull) pageSearchCritical.setInstanceGroupIdIsNull(instanceGroupIdIsNull);
            if (null != eipIdIsNull) pageSearchCritical.setEipIdIsNull(eipIdIsNull);
            if (!StrUtil.isBlank(eipId)) pageSearchCritical.setEipId(eipId);
            if (!StrUtil.isBlank(subnetId)) pageSearchCritical.setSubnetId(subnetId);
            if (!StrUtil.isBlank(vmInstanceId)) pageSearchCritical.setVmInstanceId(vmInstanceId);
            if (!StrUtil.isBlank(nodeId)) pageSearchCritical.setNodeId(nodeId);
            if (null != ignoreFailed) pageSearchCritical.setIgnoreFailed(ignoreFailed);
            VmInstancesRsp getVmInstancesRsp;
            if (ServiceCombRequestUtils.isAdmin())
            {
                getVmInstancesRsp = vmComputeService.getVmInfos(pageSearchCritical, null);
            }
            else
            {
                getVmInstancesRsp = vmComputeService.getVmInfos(pageSearchCritical, userId);

            }
            return ResponseEntity.ok(getVmInstancesRsp);
        }
        catch (InvocationException e)
        {
            return ResponseEntity.ok(new VmInstancesRsp());
        }
        catch (Exception e)
        {
            log.error("get instances error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/instances/{instanceId}/vnc", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vnc info", response = Object.class)
    public ResponseEntity<Map<String, String>> getVncInfo(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get vnc info, vm instanceId: {}", instanceId);
            return ResponseEntity.ok(vmComputeService.getVncInfo(instanceId, userId));
        }
        catch (Exception e)
        {
            log.error("get vnc info error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/instances/{instanceId}/iso", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vnc info", response = Object.class)
    @LogAnnotation(resource = "计算-虚拟机", description = "虚拟机【id：{}】挂载ISO镜像", obtainParameter = "instanceId")
    public ResponseEntity<Map<String, String>> getInstanceIdFromAgent(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get vm instanceIdFromAgent, vm instanceId: {}", instanceId);
            return ResponseEntity.ok(vmComputeService.getVncInfo(instanceId, userId));
        }
        catch (Exception e)
        {
            log.error("get vm instanceIdFromAgent info error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/instances/{instanceId}/snaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get snap info", response = Object.class)
//    @LogAnnotation(resource = "计算-虚拟机",description = "虚拟机【id：{}】挂载ISO镜像", obtainParameter = "instanceId")
    public ResponseEntity<List<SnapsTreeRsp>> getSnapsTree(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("getSnapsTree, vm instanceId: {}", instanceId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(vmSnapService.getSnapsTree(instanceId, null));
            }
            return ResponseEntity.ok(vmSnapService.getSnapsTree(instanceId, userId));
        }
        catch (Exception e)
        {
            log.error("getSnapsTree info error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    @GetMapping(value = "/instances/{instanceId}/injection", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "is iso injected", response = Object.class)
    public ResponseEntity<IsoIsInjectedRsp> isInjected(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get injection,vm instanceId: {}", instanceId);
            return ResponseEntity.ok(vmComputeService.isoIsInjected(instanceId, userId));
        }
        catch (Exception e)
        {
            log.error("get injection  error: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "计算-虚拟机", description = "编辑虚拟机【id：{},name：{}】", obtainParameter = "instanceId,name")
    @PutMapping(value = "/instances/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update instance", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> updateInstance(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @ApiParam(value = "VmInstanceUpdateReq", required = true, name = "VmInstanceUpdateReq") @RequestBody @Valid VmInstanceUpdateReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("put vm instance: {}, userId:{}", request, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                userId = null;
            }
            VmInstanceBaseRsp result = vmComputeService.updateVmInstance(request, instanceId, userId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("update vm instance error: {}, instanceId: {}", e.getMessage(), instanceId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "重置虚拟机密码、密钥或主机名【id：{}】", obtainParameter = "instanceId")
    @PutMapping(value = "/instances/{instanceId}/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "reset instance", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> resetInstance(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @ApiParam(value = "ResetPasswordHostnameReq", required = true, name = "ResetPasswordHostnameReq") @RequestBody @Valid ResetPasswordHostnameReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("reset vm instance: {}, userId:{}", request, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                userId = null;
            }
            VmInstanceBaseRsp result = vmComputeService.resetPasswordAndHostname(request, instanceId, userId);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("reset vm instance error: {}, instanceId: {}", e.getMessage(), instanceId);
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "计算-虚拟机", description = "删除虚拟机【id：{}】", obtainParameter = "instanceId")
    @DeleteMapping(value = "/instances/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete vm instance", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> removeInstance(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @ApiParam(name = "remove_root_disk") @RequestParam(required = false, value = "page_size") Boolean removeRootDisk,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("delete instanceId: {}, userId: {}", instanceId, userId);
            if (null == removeRootDisk)
            {
                removeRootDisk = true;
            }
            VmInstanceBaseRsp result = vmComputeService.removeVmInstance(instanceId, removeRootDisk);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("delete vm instance error: {}, vmInstanceId: {}", e.getMessage(), instanceId);
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "计算-虚拟机", description = "迁移虚拟机【id：{}】", obtainParameter = "instanceId")
    @PutMapping(value = "/instances/{instanceId}/migrate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "migrate vm instance", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> migrateVmInstance(
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String instanceId,
            @ApiParam(value = "MigrateVmInstanceReq", required = true, name = "MigrateVmInstanceReq") @RequestBody @Valid VmInstanceMigrateReq request
    )
    {
        try
        {
            return ResponseEntity.ok(vmComputeService.migrateInstance(instanceId, request.getDestNodeId()));
        }
        catch (Exception e)
        {
            log.error("migrate vm instance failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    //
    @LogAnnotation(resource = "计算-虚拟机", description = "虚拟机挂载云盘【虚拟机id：{}，云盘id:{}】", obtainParameter = "vmInstanceId,volumeIds")
    // vm instance attach volumes api
    @PutMapping(value = "/instances/{instanceId}/volumes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "attach volumes", response = Object.class)
    public ResponseEntity<VmInstanceBaseRsp> attachVolumes(
            @ApiParam(value = "VmInstanceAttachVolumesReq", required = true, name = "VmInstanceAttachVolumesReq") @RequestBody @Valid VmInstanceAttachVolumesReq request,
            @ApiParam(value = "instanceId", required = true, name = "instanceId") @PathVariable("instanceId") String vmInstanceId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("attach volumes, vmInstanceId:{}, userId:{}, volumeIds:{}", vmInstanceId, userId, request.getVolumeIds());

            return ResponseEntity.status(HttpStatus.OK).body(vmComputeService.attachVolumes(request.getVolumeIds(), vmInstanceId));
        }
        catch (Exception e)
        {
            log.error("attach volumes error: {}, vmId:{}, volumeIds:{}", e.getMessage(), vmInstanceId, request.getVolumeIds());
            throw throwWebException(e);
        }
    }
}
