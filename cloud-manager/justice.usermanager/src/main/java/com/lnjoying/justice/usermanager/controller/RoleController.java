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
import com.lnjoying.justice.usermanager.domain.dto.response.role.RoleDto;
import com.lnjoying.justice.usermanager.service.UserManagerService;
import com.lnjoying.justice.usermanager.utils.ServiceCombRequestUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestSchema(schemaId = "role-manager")
@RequestMapping("/api/ums/v1/roles")
@Controller
@Api(value = "Role Controller",tags = {"Role Controller"})
public class RoleController extends RestWebController
{
    private static Logger LOGGER = LogManager.getLogger();

    @Autowired
    private UserManagerService userManagerService;


    @PostMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "add roles to user by userId", response = Object.class, notes = DescriptionConfig.ADD_USER_ROLE_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void addUserRoles(
            @ApiParam(value = "", name = "roleRaw") @RequestBody List<RoleDto> roleRawReq,
            @ApiParam(value = "", required = true, name = "userId")@PathVariable String userId) throws IOException
    {
        try
        {
            LOGGER.debug("add user role {}", roleRawReq);
            userManagerService.addRolesByUserId(userId, roleRawReq);
            return;
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user's roles by userId", response = Object.class, notes = DescriptionConfig.GET_USER_ROLE_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public Object getUserRole(
            @ApiParam(value = "", required = true, name = "userId")@PathVariable String userId) throws IOException
    {
        try
        {
            ServiceCombRequestUtils.checkRoleUmsTenantOrAdmin();

            LOGGER.debug("add user role, userId: {}", userId);
            return userManagerService.getRolesByUserId(userId);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    /**
     * get bp list
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get role list", response = Object.class, notes = DescriptionConfig.ROLE_LIST_MSG)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public Object getRoles() throws IOException
    {
        try
        {
           return userManagerService.getRoles();
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }
}
