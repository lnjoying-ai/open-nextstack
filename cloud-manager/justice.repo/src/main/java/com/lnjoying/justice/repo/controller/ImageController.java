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
import com.lnjoying.justice.repo.domain.dto.request.ImageCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.ImageDetailInfoRsp;
import com.lnjoying.justice.repo.entity.search.ImageSearchCritical;
import com.lnjoying.justice.repo.service.biz.ImageServiceBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RestSchema(schemaId = "image")
@Controller
@Api(value = "Image Controller", tags = {"Image Controller"})
@RequestMapping("/repo/v1")
@Slf4j
public class ImageController
{
    private static final String REG_UUID = "[0-9a-f]{32}";

    @Autowired
    private ImageServiceBiz imageServiceBiz;

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/images", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get images", response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getImages(@ApiParam(name = "name") @RequestParam(required = false, value = "name") String imageName,
                            @ApiParam(name = "image_os_type") @RequestParam(required = false,value = "image_os_type") Short imageOsType,
                            @ApiParam(name = "image_os_vendor") @RequestParam(required = false,value = "image_os_vendor") Short imageOsVendor,
                            @RequestHeader(name = "X-UserId", required = false) String userId,
                            @ApiParam(name="is_vm") @RequestParam(required = false,value = "is_vm") Boolean isVm,
                            @ApiParam(name="is_gpu") @RequestParam(required = false,value = "is_gpu") Boolean isGpu,
                            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size")  Integer pageSize,
                            @ApiParam(name = "is_ok") @RequestParam(required = false,value = "is_ok") Boolean isOk,
                            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num")  Integer pageNum)
    {
        try {
            log.debug("get image list");
            ImageSearchCritical pageSearchCritical = new ImageSearchCritical();
            pageSearchCritical.setImageName(imageName);
            pageSearchCritical.setImageOsType(imageOsType);
            pageSearchCritical.setImageOsVendor(imageOsVendor);
            pageSearchCritical.setIsVm(isVm);
            pageSearchCritical.setIsPublic(true);
            if (null != isOk) {
                pageSearchCritical.setIsOk(isOk);
            }
            if (null != isGpu) {
                pageSearchCritical.setIsGpu(isGpu);
            }

            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return imageServiceBiz.getImages(pageSearchCritical, null);
            }

            return imageServiceBiz.getImages(pageSearchCritical, userId);
        }
        catch (Exception e)
        {
            log.error("get image list error: {}",e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/images/{image_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get image detail info", response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ImageDetailInfoRsp getImage(
            @RequestHeader(name = "X-UserId", required = false) String userId,
            @ApiParam(value = "image_id", required = true, name = "image_id") @PathVariable("image_id") @Pattern(regexp = REG_UUID) String imageId)
    {
        try
        {
            log.info("get image detail info, imageId: {}", imageId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return imageServiceBiz.getImage(imageId, null);
            }
            return imageServiceBiz.getImage(imageId, userId);
        }
        catch (Exception e)
        {
            log.error("get image detail info error: {}",e.getMessage());
            throw throwWebException(e);
        }
    }

//    @LogAnnotation(resource = "镜像", description = "添加镜像【镜像名称：{}，镜像类型：{}】", obtainParameter = "imageName,imageOsType")
    @PostMapping(value = "/images", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create new image",response =  Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    public Object createImage(@ApiParam(value = "imageCreateReq", required = true, name = "imageCreateReq")@RequestBody @Valid ImageCreateReq imageCreateReq)
    {
        try
        {
            log.info("post image info: {}",imageCreateReq);
            return imageServiceBiz.createImage(imageCreateReq);
        }
        catch (Exception e)
        {
            log.error("post image info error: {}",e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "镜像", description = "更新镜像【id：{}, name: {}】",obtainParameter = "imageId,name")
    @PutMapping(value = "/images/{image_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "更新 image",response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object updateImage(
            @RequestHeader(name = "X-UserId", required = false) String userId,
             @ApiParam(value = "image_id", required = true, name = "image_id")@PathVariable("image_id") @Pattern(regexp = REG_UUID) String imageId,
                           @ApiParam(value = "CommonReq", required = true, name = "CommonReq")@RequestBody @Valid CommonReq commonReq)
    {
        try
        {
            log.info("update image, imageId: {}", imageId);
            return imageServiceBiz.updateImage(imageId, commonReq, userId);
        }
        catch (Exception e)
        {
            log.error("update image error: {}",e.getMessage());
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "镜像", description = "删除镜像【id：{}】",obtainParameter = "imageId")
    @DeleteMapping(value = "/images/{image_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete image",response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object removeImage(@ApiParam(value = "image_id", required = true, name = "image_id")@PathVariable("image_id") @Pattern(regexp = REG_UUID) String imageId,
                           @RequestHeader(name = "X-UserId", required = false) String userId) throws WebSystemException
    {
        try
        {
            log.info("remove image, imageId: {}", imageId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return imageServiceBiz.removeImage(imageId, null);
            }
            return imageServiceBiz.removeImage(imageId, userId);
        }
        catch (Exception e)
        {
            log.error("remove image error: {}",e.getMessage());
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
