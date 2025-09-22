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
import com.lnjoying.justice.commonweb.util.HttpContextUtils;
import com.lnjoying.justice.commonweb.util.JwtUtils;
import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.schema.constant.WebConstants;
import com.lnjoying.justice.usermanager.config.DescriptionConfig;
import com.micro.core.common.Utils;
import com.micro.core.persistence.redis.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;

@RestSchema(schemaId = "auth")
@RequestMapping("/api/ums/v1/auth")
@Controller
@Api(value = "Auth Controller",tags = {"Auth Controller"})
public class AuthController extends RestWebController
{
    @DeleteMapping(value = "/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "logout and delete token",notes = DescriptionConfig.DELETE_TOKEN_MSG)
    void deleteToken(HttpServletResponse servletResponse)
    {
        Integer leftTime = HttpContextUtils.getIntAttribute("leftTime");
        String accessToken = HttpContextUtils.getStrAttribute("accessToken");
        if (leftTime != null && leftTime > 0 && accessToken != null)
        {
            String accessHash = Utils.getSHA(accessToken);
            RedisUtil.set(RedisCacheField.ACCESS_TOKEN_EXPIRE, accessHash, "dropped",leftTime);
        }

        servletResponse.addCookie(JwtUtils.getLogoutCookie(WebConstants.ACCESS_TOKEN_NAME, "logout", WebConstants.LNJOYING_TOKEN_INDATE, "/"));
    }
}
