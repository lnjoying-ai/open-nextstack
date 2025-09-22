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

package com.lnjoying.justice.usermanager.config.security;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component("loginFailHandler")
public class LoginFailHandler extends SimpleUrlAuthenticationFailureHandler {

    private static Logger LOGGER = LogManager.getLogger();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        LOGGER.info("error auth. {}", exception.getMessage());

        JsonObject jsonObject = new JsonObject();
        ErrorCode errorCode = ErrorCode.InvalidAuthority;
        if (exception.getMessage().equals("user have been disabled"))
        {
            errorCode = ErrorCode.USER_NOT_ACTIVE;
        }

        jsonObject.put("code", errorCode.getCode());
        WebSystemException.setResponseStatus(errorCode, response);
        jsonObject.put("message", ErrorCode.fromCode(errorCode.getCode()).getMessage());

        PrintWriter out = response.getWriter();
        out.print(jsonObject.toString());
    }

}
