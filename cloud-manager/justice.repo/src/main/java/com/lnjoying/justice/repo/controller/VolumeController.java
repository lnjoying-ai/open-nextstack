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
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeAttachReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeCreateReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeExportReq;
import com.lnjoying.justice.repo.domain.dto.response.*;
import com.lnjoying.justice.repo.entity.search.VolumeSearchCritical;
import com.lnjoying.justice.repo.service.biz.VolumeServiceBiz;
import com.lnjoying.justice.repo.service.biz.VolumeSnapServiceBiz;
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

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author George
 * @since 2023-03-07
 */
@RestSchema(schemaId = "volume")
@Controller
@Api(value = "Volume Controller", tags = {"Volume Controller"})
@RequestMapping("/repo/v1")
@Slf4j
public class VolumeController
{
    private static final String REG_UUID = "[0-9a-f]{32}";

    @Autowired
    private VolumeServiceBiz volumeServiceBiz;

    @Autowired
    private VolumeSnapServiceBiz volumeSnapServiceBiz;

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/volumes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get volumes")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumesRsp getVolumes(@ApiParam(name = "name") @RequestParam(required = false,value = "name") String volumeName,
                            @ApiParam(name = "storage_pool_id") @RequestParam(required = false,value = "storage_pool_id") String storagePoolId,
                            @RequestHeader(name = "X-UserId", required = false) String userId,
                            @ApiParam(name = "detached" )@RequestParam(required = false,value = "detached") Boolean isDetached,
                            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size")  Integer pageSize,
                            @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num")  Integer pageNum)
    {

        log.debug("get volume list");
        VolumeSearchCritical pageSearchCritical = new VolumeSearchCritical();
        pageSearchCritical.setVolumeName(volumeName);
        pageSearchCritical.setPoolId(storagePoolId);
        pageSearchCritical.setIsRoot(false);

        if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
        if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
        if (isDetached != null && isDetached)
        {
            pageSearchCritical.setPhaseStatus(PhaseStatus.DETACHED);
            pageSearchCritical.setIsEqPhaseStatus(true);
        }
        else if (isDetached != null)
        {
            pageSearchCritical.setPhaseStatus(PhaseStatus.DETACHED);
            pageSearchCritical.setIsEqPhaseStatus(false);
        }

        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.getVolumes(pageSearchCritical, null);
            }

            return volumeServiceBiz.getVolumes(pageSearchCritical, userId);
        }
        catch (Exception e)
        {
            log.error("get volumes error {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @GetMapping(value = "/volumes/recycle", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get volumes")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public RootVolumesRsp getRecycleVolumes(@ApiParam(name = "name") @RequestParam(required = false,value = "name") String volumeName,
                                            @ApiParam(name = "storage_pool_id") @RequestParam(required = false,value = "storage_pool_id") String storagePoolId,
                                            @RequestHeader(name = "X-UserId", required = false) String userId,
                                            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size")  Integer pageSize,
                                            @ApiParam(name = "page_num") @RequestParam(required = false)  Integer pageNum)
    {

        log.debug("get recycle volume list");
        VolumeSearchCritical pageSearchCritical = new VolumeSearchCritical();
        pageSearchCritical.setVolumeName(volumeName);
        pageSearchCritical.setPoolId(storagePoolId);

        pageSearchCritical.setPhaseStatus(PhaseStatus.DETACHED);
        pageSearchCritical.setIsEqPhaseStatus(true);

        if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
        if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.getRootVolumes(pageSearchCritical, null);
            }

            return volumeServiceBiz.getRootVolumes(pageSearchCritical, userId);
        }
        catch (Exception e)
        {
            log.error("get volumes error {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/volumes/{volume_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get volume detail info")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeDetailInfoRsp getVolume(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(value = "volume_id", required = true, name = "volume_id") @PathVariable("volume_id") @Pattern(regexp = REG_UUID) String volumeId)
    {

        log.info("get volume detail info, volumeId: {}", volumeId);
        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.getVolume(volumeId, null);
            }
            return volumeServiceBiz.getVolume(volumeId, userId);
        }
        catch (Exception e)
        {
            log.error("get volume detail info error {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @LogAnnotation(resource = "存储-云盘管理", description = "创建云盘【名称：{}，存储池：{}，大小：{}GB】", obtainParameter = "name,storagePoolId,size")
    @PostMapping(value = "/volumes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create new volume")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public VolumeBaseRsp createVolume(@ApiParam(value = "volumeCreateReq", required = true, name = "volumeCreateReq")@RequestBody @Valid VolumeCreateReq volumeCreateReq,
                             @RequestHeader(name = "X-UserId", required = false) String userId
                             )
    {

        log.info("post volume info: {}",volumeCreateReq);
        try
        {
            return volumeServiceBiz.createVolume(volumeCreateReq, userId, false, null, null, null);
        }
        catch (Exception e)
        {
            log.error("create volume error {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "存储-云盘管理", description = "删除云盘【id：{}】",obtainParameter = "volumeId")
    @DeleteMapping(value = "/volumes/{volume_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove volume")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeBaseRsp removeVolume(@ApiParam(value = "volume_id", required = true, name = "volume_id")@PathVariable("volume_id") @Pattern(regexp = REG_UUID) String volumeId,
                           @RequestHeader(name = "X-UserId", required = false) String userId)
    {

        log.info("remove volume, volumeId: {}", volumeId);
        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.removeVolume(volumeId, null);
            }
            return volumeServiceBiz.removeVolume(volumeId, userId);
        }
        catch (Exception e)
        {
            log.error("remove volume error {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "存储-云盘管理", description = "挂载云盘【id：{}，vmId：{}】",obtainParameter = "volumeId,vmId")
    @PutMapping(value = "/volumes/{volume_id}/attach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update volume")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeBaseRsp attachVolume(
            @ApiParam(value = "volume_id", required = true, name = "volume_id") @PathVariable("volume_id")String volumeId,
            @ApiParam(value = "volumeAttachReq", required = true, name = "volumeAttachReq")@RequestBody @Valid VolumeAttachReq volumeAttachReq,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {

        log.info("attach volume: volumeId:{} vmId:{}",volumeId, volumeAttachReq.getVmId());
        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.attachVolume(volumeId, volumeAttachReq.getVmId(), null);
            }
            return volumeServiceBiz.attachVolume(volumeId, volumeAttachReq.getVmId(), userId);
        }
        catch (Exception e)
        {
            log.error("attach volume error {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @LogAnnotation(resource = "存储-云盘管理", description = "拆卸云盘【id：{}】",obtainParameter = "volumeId")
    @PutMapping(value = "/volumes/{volume_id}/detach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update volume")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeBaseRsp detachVolume(
            @ApiParam(value = "volume_id", required = true, name = "volume_id") @PathVariable("volume_id")String volumeId,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {

        log.info("detach volume: volumeId:{} ",volumeId);
        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.detachVolume(volumeId, null);
            }
            return volumeServiceBiz.detachVolume(volumeId, userId);
        }
        catch (Exception e)
        {
            log.error("detach volume error {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @LogAnnotation(resource = "存储-云盘管理", description = "编辑云盘【id：{}，名称：{}，描述：{}】",obtainParameter = "volumeId,name,description")
    @PutMapping(value = "/volumes/{volume_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update volume")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeBaseRsp updateVolume(
            @ApiParam(value = "volume_id", required = true, name = "volume_id") @PathVariable("volume_id")String volumeId,
            @ApiParam(value = "commonReq", required = true, name = "commonReq") @RequestBody @Valid CommonReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        log.info("update volume: volumeId:{} name:{}",volumeId, request.getName());
        try
        {
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeServiceBiz.updateVolume(volumeId, request, null);
            }
            return volumeServiceBiz.updateVolume(volumeId, request, userId);
        }
        catch (Exception e)
        {
            log.error("update volume error {}", e.getMessage());
            throw throwWebException(e);
        }
    }

//    @LogAnnotation(resource = "存储-云盘管理", description = "导出云盘成镜像【id：{}，镜像名称：{}，是否公开：{}】",obtainParameter = "volumeId,imageName,isPublic")
    @PutMapping(value = "/volumes/{volume_id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "export volume")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ImageBaseRsp exportVolume(
            @ApiParam(value = "volume_id", required = true, name = "volume_id") @PathVariable("volume_id")String volumeId,
            @ApiParam(value = "volumeExportReq", required = true, name = "volumeExportReq") @RequestBody @Valid VolumeExportReq volumeExportReq,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        log.info("export volume: volumeId:{} exportName:{}",volumeId, volumeExportReq.getImageName());
//        if(ServiceCombRequestUtils.isAdmin())
//        {
//            return volumeServiceBiz.exportVolume(volumeId, null, volumeExportReq);
//        }
        try
        {
            return volumeServiceBiz.exportVolume(volumeId, userId, volumeExportReq);
        }
        catch (Exception e)
        {
            log.error("export volume error {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/volumes/{volumeId}/snaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get snap list", response = Object.class)
//    @LogAnnotation(resource = "计算-虚拟机",description = "虚拟机【id：{}】挂载ISO镜像", obtainParameter = "instanceId")
    public ResponseEntity<List<SnapsTreeRsp>> getSnapsTree(
            @ApiParam(value = "volumeId", required = true, name = "volumeId") @PathVariable("volumeId") String volumeId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("getSnapsTree,  volumeId: {}", volumeId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(volumeSnapServiceBiz.getSnapsTree(volumeId,null));
            }
            return ResponseEntity.ok(volumeSnapServiceBiz.getSnapsTree(volumeId, userId));
        }
        catch (Exception e)
        {
            log.error("getSnapsTree info error: {}", e.getMessage());
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
