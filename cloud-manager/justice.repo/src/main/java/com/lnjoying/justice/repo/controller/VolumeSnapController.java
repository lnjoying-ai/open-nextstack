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
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeSnapCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.*;
import com.lnjoying.justice.repo.entity.search.VolumeSnapSearchCritical;
import com.lnjoying.justice.repo.service.biz.VolumeSnapServiceBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author George
 * @since 2023-03-07
 */
@RestSchema(schemaId = "volumeSnap")
@Controller
@Api(value = "VolumeSnap Controller", tags = {"VolumeSnap Controller"})
@RequestMapping("/repo/v1")
@Slf4j
public class VolumeSnapController
{

    private static final String REG_UUID = "[0-9a-f]{32}";

    @Autowired
    private VolumeSnapServiceBiz volumeSnapServiceBiz;

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/volume_snaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get volumeSnaps", response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getVolumeSnaps(@ApiParam(name = "name") @RequestParam(required = false, value = "name") String volumeSnapName,
                             @ApiParam(name = "volume_id") @RequestParam(required = false,value = "volume_id") String volumeId,
                             @RequestHeader(name = "X-UserId", required = false) String userId,
                             @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size")  Integer pageSize,
                             @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num")  Integer pageNum)
    {
        try {
            log.debug("get volumeSnap list");
            VolumeSnapSearchCritical pageSearchCritical = new VolumeSnapSearchCritical();
            pageSearchCritical.setVolumeSnapName(volumeSnapName);
            pageSearchCritical.setVolumeId(volumeId);

            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeSnapServiceBiz.getVolumeSnaps(pageSearchCritical, null);
            }

            return volumeSnapServiceBiz.getVolumeSnaps(pageSearchCritical, userId);
        }
        catch (Exception e) 
        {
            log.error("get volumeSnap list error: ",e);
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/volume_snaps/{volume_snap_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get volumeSnap detail info", response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getVolumeSnap(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(value = "volume_snap_id", required = true, name = "volume_snap_id") @PathVariable("volume_snap_id") @Pattern(regexp = REG_UUID) String volumeSnapId)
    {
        try
        {
            log.info("get volumeSnap detail info, volumeSnapId: {}", volumeSnapId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeSnapServiceBiz.getVolumeSnap(volumeSnapId, null);
            }
            return volumeSnapServiceBiz.getVolumeSnap(volumeSnapId, userId);
        }
        catch (Exception e)
        {
            log.error("get volumeSnap detail info error: ",e);
            throw throwWebException(e);

        }
    }

    @LogAnnotation(resource = "存储-云盘快照", description = "创建云盘快照【名称：{}，volumeId：{}】", obtainParameter = "name,volumeId")
    @PostMapping(value = "/volume_snaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create new volumeSnap",response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Object createVolumeSnap(@ApiParam(value = "volumeSnapCreateReq", required = true, name = "volumeSnapCreateReq")@RequestBody @Valid VolumeSnapCreateReq volumeSnapCreateReq,
                             @RequestHeader(name = "X-UserId", required = false) String userId
    )
    {
        try
        {
            log.info("post volumeSnap info: {}",volumeSnapCreateReq);
            return volumeSnapServiceBiz.createVolumeSnap(volumeSnapCreateReq, userId);
        }
        catch (Exception e){
            log.error("post volumeSnap info error: ",e);    
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "存储-云盘快照", description = "删除云盘快照【id：{}】", obtainParameter = "volumeSnapId")
    @DeleteMapping(value = "/volume_snaps/{volume_snap_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove volumeSnap",response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object removeVolumeSnap(@ApiParam(value = "volume_snap_id", required = true, name = "volume_snap_id")@PathVariable("volume_snap_id") @Pattern(regexp = REG_UUID) String volumeSnapId,
                            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("remove volumeSnap, volumeSnapId: {}", volumeSnapId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeSnapServiceBiz.removeVolumeSnap(volumeSnapId, null);
            }
            return volumeSnapServiceBiz.removeVolumeSnap(volumeSnapId, userId);
        }
        catch (Exception e)
        {
            log.error("remove volumeSnap failed, volumeSnapId: {}", volumeSnapId, e);
            throw throwWebException(e);

        }
    }

    @LogAnnotation(resource = "存储-云盘快照", description = "编辑云盘快照【名称：{}，描述：{}，id：{}】", obtainParameter = "name,description,volumeSnapId")
    @PutMapping(value = "/volume_snaps/{volume_snap_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update volumeSnap", response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeSnapBaseRsp updateVolume(
            @ApiParam(value = "volume_snap_id", required = true, name = "volume_snap_id") @PathVariable("volume_snap_id")String volumeSnapId,
            @ApiParam(value = "commonReq", required = true, name = "commonReq") @RequestBody @Valid CommonReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("update volume: volumeSnapId:{} name:{}",volumeSnapId, request.getName());
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeSnapServiceBiz.updateVolumeSnap(volumeSnapId, request, null);
            }
            return  volumeSnapServiceBiz.updateVolumeSnap(volumeSnapId, request, userId);

        }
        catch (Exception e)
        {
            log.error("update  volumeSnap failed: ", e);

            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "存储-云盘快照", description = "改变云盘快照【id：{}】", obtainParameter = "volumeSnapId")
    @PutMapping(value = "/volume_snaps/{volume_snap_id}/switch", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update volumeSnap", response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public VolumeSnapBaseRsp switchVolume(
            @ApiParam(value = "volume_snap_id", required = true, name = "volume_snap_id") @PathVariable("volume_snap_id")String volumeSnapId,
            @RequestHeader(name = "X-UserId", required = false) String userId)
    {
        try
        {
            log.info("switch volume: volumeSnapId:{} ",volumeSnapId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return volumeSnapServiceBiz.switchSnapVolume(volumeSnapId, null);
            }
            return  volumeSnapServiceBiz.switchSnapVolume(volumeSnapId, userId);

        }
        catch (Exception e)
        {
            log.error("switch  volumeSnap failed: ", e);

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
