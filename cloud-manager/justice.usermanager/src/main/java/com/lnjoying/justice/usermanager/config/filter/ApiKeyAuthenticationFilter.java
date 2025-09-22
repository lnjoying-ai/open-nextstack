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

package com.lnjoying.justice.usermanager.config.filter;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.constant.UserHeadInfo;
import com.lnjoying.justice.usermanager.config.data.ApiKeyAuthenticationToken;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class ApiKeyAuthenticationFilter extends BasicAuthenticationFilter
{
    private String principalRequestHeader;

    private String credentialRequestHeader;


    public ApiKeyAuthenticationFilter(AuthenticationManager authenticationManager,
                                      String principalRequestHeader, String credentialRequestHeader)
    {
        super(authenticationManager);
        this.credentialRequestHeader = credentialRequestHeader;
        this.principalRequestHeader = principalRequestHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        String principal = request.getHeader(principalRequestHeader);

        String credential = request.getHeader(credentialRequestHeader);

        if ( null == principal && null == credential )
        {
            doFilter(request, response, chain);
        }
        else if (checkApiKey(principal, credential))
        {
            request.setAttribute(UserHeadInfo.USERID, "39937079-99fe-4cd8-881f-04ca8c4fe09d" );
            request.setAttribute(UserHeadInfo.USERNAME, "admin");
            request.setAttribute(UserHeadInfo.BPID, "");
            request.setAttribute(UserHeadInfo.BpName, "admin");
            ApiKeyAuthenticationToken authenticationToken = new ApiKeyAuthenticationToken("key", AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
            log.info("Use apiKey authentication.");
            chain.doFilter(request, response);
        }
        else
        {
            log.info("api key check failed.uri: {}", request.getRequestURI());
            sendErrorResponse(ErrorCode.InvalidAuthority, response);
        }
    }

    private boolean checkApiKey(String principal, String credential)
    {
        if ("admin".equals(principal) && "admin".equals(credential))
        {
            return true;
        }
        return false;
    }

    void sendErrorResponse(ErrorCode e, HttpServletResponse response)
    {
        WebSystemException.setResponseStatus(e, response);

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("code", e.getCode());
        jsonObject.put("message", e.getMessage());
        response.setContentType("application/json;charset=UTF-8");

        PrintWriter out = null;
        try
        {
            out = response.getWriter();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
        assert out != null;
        out.print(jsonObject.toString());
    }
}
