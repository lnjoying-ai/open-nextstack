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
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.usermanager.config.DescriptionConfig;
import com.lnjoying.justice.usermanager.domain.dto.request.user.*;
import com.lnjoying.justice.usermanager.domain.dto.response.user.ApiKeyRsp;
import com.lnjoying.justice.usermanager.domain.dto.response.user.UserRsp;
import com.lnjoying.justice.usermanager.domain.model.UserContactInfo;
import com.lnjoying.justice.usermanager.domain.model.search.UserSearchCritical;
import com.lnjoying.justice.usermanager.service.UserManagerService;
import com.lnjoying.justice.usermanager.utils.ServiceCombRequestUtils;
import com.lnjoying.justice.usermanager.utils.ValidatorUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Objects;

import static com.lnjoying.justice.usermanager.utils.ServiceCombRequestUtils.*;

@RestSchema(schemaId = "users-manager")
@RequestMapping("/api/ums/v1/users")
@Controller
@Api(value = "User Controller",tags = {"User Controller"})
public class UserController extends RestWebController
{
    private static Logger LOGGER = LogManager.getLogger();
    private static final String REG_UUID = "[0-9a-f]{32}";

    @Autowired
    private UserManagerService userManagerService;

    @PostMapping(value = "/registration", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "registration", response = Object.class, notes = DescriptionConfig.REGISTER_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.CREATED)
    public Object register(
        @ApiParam(value = "UserRegReq", required = true, name = "UserRegReq") @RequestBody UserRegReq request) throws IOException
    {
        try
        {
            LOGGER.info("reg user info: {}", request);
            UserContactInfo contactInfo = request.getContact_info();
            if (Objects.isNull(contactInfo) || StringUtils.isBlank(contactInfo.getPhone())) {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
            }
            ValidatorUtil.checkValidateCode(request.getVerification_code(), RedisCacheField.REG_VER_CODE, contactInfo.getPhone());
            return userManagerService.register(request);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "admin add new user", response = Object.class, notes = DescriptionConfig.ADMIN_ADD_USER_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.CREATED)
    public Object addNewUser(
            @ApiParam(value = "UserAddReq", required = true, name = "addUserRegReq") @RequestBody UserAddReq request) throws IOException
    {
        try
        {
            LOGGER.info("add user info: {}", request);
            return userManagerService.addUser(request);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw throwWebException(e);
        }
    }


    @PatchMapping(value = "/retrieved-password", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "retrieved password", response = Object.class, notes = DescriptionConfig.RETRIEVE_PASSWORD_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void retrievePassword(
        @ApiParam(value = "RetrievePasswordReq", required = true, name = "RetrievePasswordReq") @RequestBody RetrievePasswordReq request) throws IOException
    {
        try
        {
            userManagerService.retrievePassword(request);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PatchMapping(value = "/password", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update password", response = Object.class, notes = DescriptionConfig.UPDATE_PASSWORD_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void updatePassword(
            @ApiParam(value = "update password", required = true, name = "updatepassword") @RequestBody UpdatePasswordReq request) throws IOException
    {
        try
        {
            userManagerService.updatePassword(getUserId(), request);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PostMapping(value = "/uniqueness", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "check uniqueness", response = Object.class, notes = DescriptionConfig.UNIQUENESS_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public Object uniqueness(
        @ApiParam(value = "uniquenessRequest", required = true, name = "uniqueness") @RequestBody UniqueReq request) throws IOException
    {
        try
        {
            return userManagerService.uniqueness(request);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @DeleteMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete user by userId", response = Object.class, notes = DescriptionConfig.DELETE_USER_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void deleteUser(
        @ApiParam(value = "user id") @PathVariable("userId") @Pattern(regexp = REG_UUID) String userId) throws IOException
    {
        try
        {
            userManagerService.deleteUser(userId);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PutMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update user by userId", response = Object.class, notes = DescriptionConfig.UPDATE_USER_BY_ID_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void updateUser(
            @ApiParam(value = "user id") @PathVariable("userId") @Pattern(regexp = REG_UUID) String userId,
            @ApiParam(value = "userInfo", required = true, name = "userInfo") @RequestBody UserUpdateReq request) throws IOException
    {
        try
        {
            ServiceCombRequestUtils.checkRoleUmsTenantOrAdmin();

            LOGGER.info("update user id: {} user info: {}", userId, request);
            userManagerService.updateUser(userId, request);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PutMapping(value = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update current user", response = Object.class, notes = DescriptionConfig.UPDATE_CURRENT_USER_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void updateCurrentUser(@ApiParam(value = "userInfo", required = true, name = "userInfo") @RequestBody UserBasicReq request) throws IOException
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            LOGGER.info("update user id: {} user info: {}", userId, request);
            userManagerService.updateCurrentUser(userId, request);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }


    /**
     * get current login user.
     */
    @GetMapping(value = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get current login user.", response = UserRsp.class, notes = DescriptionConfig.CURRENT_USER_MSG)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public UserRsp  getUserInfo() throws IOException
    {
        try
        {
            return userManagerService.getUserDtoInfo(ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    /**
     * get user list
     */
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN', 'ROLE_UMS_ADMIN', 'ROLE_UMS_TENANT' )")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get user list.", response = Object.class, notes = DescriptionConfig.USER_LIST_MSG)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public Object getUserInfos(@ApiParam(value = "", required = false, name = "name") @RequestParam(required = false) String name,
                               @ApiParam(value = "", required = false, name = "bp_id") @RequestParam(value = "queryBpId", required = false) String queryBpId,
                               @ApiParam(name = "page_size") @RequestParam(required = false) Integer page_size,
                               @ApiParam(name = "page_num") @RequestParam(required = false) Integer page_num) throws IOException
    {
        try
        {
            UserSearchCritical pageSearchCritical = new UserSearchCritical();
            Pair<String, String> userAttributes = getUserAttributes();
            pageSearchCritical.setBpId(userAttributes.getLeft());
            pageSearchCritical.setUserId(userAttributes.getRight());
            pageSearchCritical.setName(name);
            pageSearchCritical.setQueryBpId(queryBpId);
            if (page_num != null) pageSearchCritical.setPageNum(page_num);
            if (page_size != null) pageSearchCritical.setPageSize(page_size);
            return userManagerService.getUserDtoInfos(pageSearchCritical);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PatchMapping(value = "/phone", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "patch current user phone.", response = Object.class, notes = DescriptionConfig.UPDATE_USER_PHONE_MSG)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public void updateUserphone(@ApiParam(value = "", required = false, name = "phone") @RequestBody PhoneRawInfo phoneRawInfo) throws IOException
    {
        try
        {
             userManagerService.updateUserPhone(ServiceCombRequestUtils.getUserId(), phoneRawInfo);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PatchMapping(value = "/email", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "patch current user email.", response = Object.class, notes = DescriptionConfig.UPDATE_USER_EMAIL_MSG)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public void updateUserEmail(@ApiParam(value = "", required = false, name = "email") @RequestBody EmailRawInfo emailRawInfo) throws IOException
    {
        try
        {
            userManagerService.updateUserEmail(ServiceCombRequestUtils.getUserId(), emailRawInfo);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @PostMapping(value = "/apikey", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "set  user apiKey.", response = Object.class, notes = DescriptionConfig.SET_USER_API_KEY)
    @ResponseBody     @ResponseStatus(HttpStatus.OK)
    public ApiKeyRsp setApiKey() throws IOException
    {
        try
        {
            return userManagerService.setApiKey(getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }
}
