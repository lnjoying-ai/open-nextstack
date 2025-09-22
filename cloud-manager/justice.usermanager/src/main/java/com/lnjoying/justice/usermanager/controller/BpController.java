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

package com.lnjoying.justice.usermanager.controller;

import com.lnjoying.justice.commonweb.controller.RestWebController;
import com.lnjoying.justice.usermanager.config.DescriptionConfig;
import com.lnjoying.justice.usermanager.domain.dto.request.bp.BpRawReq;
import com.lnjoying.justice.usermanager.domain.model.search.BpSearchCritical;
import com.lnjoying.justice.usermanager.service.BpManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Pattern;
import java.io.IOException;

@RestSchema(schemaId = "bps-manager")
@RequestMapping("/api/ums/v1/bps")
@Controller
@Api(value = "BP Controller",tags = {"BP Controller"})
public class BpController extends RestWebController
{

    private static final String REG_UUID = "[0-9a-f]{32}";
    private static Logger LOGGER = LogManager.getLogger();

    @Autowired
    private BpManagerService bpManagerService;

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "add bp", response = Object.class, notes = DescriptionConfig.ADD_BP_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.CREATED)
    public Object addBP(
        @ApiParam(value = "bp raw info", required = true, name = "bp raw info") @RequestBody BpRawReq request) throws IOException
    {
        try
        {
            LOGGER.debug("add new bp: {}", request);
            return bpManagerService.addBp(request);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @DeleteMapping(value = "/{bpId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete bp by bpId", response = Object.class, notes = DescriptionConfig.DELETE_BP_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void deleteBp(
        @ApiParam(value = "bp id") @PathVariable("bpId") @Pattern(regexp = REG_UUID) String bpId) throws IOException
    {
        try
        {
            LOGGER.debug("delete bp: {}", bpId);
            bpManagerService.deleteBp(bpId);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PutMapping(value = "/{bpId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update bp by bpId", response = Object.class, notes = DescriptionConfig.UPDATE_BP_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void updateBp(
            @ApiParam(value = "", name = "updateBp") @RequestBody BpRawReq bpInfo,
            @ApiParam(value = "", required = true, name = "bpId")@PathVariable String bpId) throws IOException
    {
        try
        {
            LOGGER.debug("update bp: {} bpinfo: {}", bpId,  bpInfo);
            bpManagerService.updateBpInfo(bpId, bpInfo);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/{bpId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get bp by bpId", response = Object.class, notes = DescriptionConfig.BP_INFO_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public Object getBp(
            @ApiParam(value = "bpId", required = true, name = "bpId") @PathVariable("bpId") @Pattern(regexp = REG_UUID) String bpId) throws IOException
    {
        try
        {
            LOGGER.debug("get bp info, bpId: {}", bpId);
            return bpManagerService.getBpDtoInfo(bpId);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    /**
     * get bp list
     */
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get bp list", response = Object.class, notes = DescriptionConfig.BP_LIST_MSG)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public Object getBps(@ApiParam(name = "name") @RequestParam(required = false) String name,
                         @ApiParam(name = "page_size") @RequestParam(required = false)  Integer page_size,
                         @ApiParam(name = "page_num") @RequestParam(required = false)  Integer page_num) throws IOException
    {
        try
        {
            LOGGER.debug("get bp list");
            BpSearchCritical pageSearchCritical = new BpSearchCritical();
            pageSearchCritical.setName(name);
            if (page_num != null) pageSearchCritical.setPageNum(page_num);
            if (page_size != null) pageSearchCritical.setPageSize(page_size);
            return bpManagerService.getBpDtoInfos(pageSearchCritical);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }
}
