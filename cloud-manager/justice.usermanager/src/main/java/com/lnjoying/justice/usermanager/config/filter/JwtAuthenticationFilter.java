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
import com.lnjoying.justice.commonweb.util.JwtUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.schema.constant.UserHeadInfo;
import com.lnjoying.justice.schema.constant.WebConstants;
import com.lnjoying.justice.usermanager.config.data.ApiKeyAuthenticationToken;
import com.micro.core.common.JwtTools;
import com.micro.core.common.Utils;
import com.micro.core.persistence.redis.RedisUtil;
import io.jsonwebtoken.impl.JwtMap;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class JwtAuthenticationFilter extends BasicAuthenticationFilter
{
    private String jwtkey;
    private static Logger LOGGER = LogManager.getLogger();

    private static final String[] urlPatterns = {"/health/", "/logout", "/api/ums/v1/verification", "/AuthImpl","/cse","/login","/error","/exception","/inspector/", "/UmsServiceImpl",
                                    "/api/ums/v1/users/registration", "/api/ums/v1/users/retrieved-password"};

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, String jwtkey)
    {
        super(authenticationManager);
        this.jwtkey = jwtkey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        String url = request.getRequestURI();
        if (StringUtils.startsWithAny(url, urlPatterns) ||
                SecurityContextHolder.getContext().getAuthentication() instanceof ApiKeyAuthenticationToken)
        {
            chain.doFilter(request, response);
            return;
        }

        String accessToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null)
        {
            for (Cookie k:cookies)
            {
                if (k.getName().equals(WebConstants.ACCESS_TOKEN_NAME)  && k.getValue().startsWith(JwtUtils.getAuthorizationHeaderPrefix()))
                {
                    accessToken = k.getValue();
                    break;
                }
            }
        }

        if (accessToken == null)
        {
            accessToken = request.getHeader(WebConstants.HEADER_ACCESS_TOKEN_NAME);
        }

        if (accessToken == null)
        {
            sendErrorResponse(ErrorCode.InvalidAuthority, response);
            return;
        }

        JwtMap jwtInfo = getJwtInfo(accessToken);
        if (null == jwtInfo)
        {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Access token is empty");
            return;
        }

        if (false == checkJwt(jwtInfo, request, response))
        {
            LOGGER.info("jwt check failed.uri: {}", request.getRequestURI());
            sendErrorResponse(ErrorCode.InvalidAuthority, response);
            return;
        }

        String accessHash = Utils.getSHA(accessToken);
        if (RedisUtil.get(RedisCacheField.ACCESS_TOKEN_EXPIRE, accessHash) != null)
        {
            LOGGER.info("jwt check have been dropped for {}", jwtInfo.get(UserHeadInfo.USERNAME));
            sendErrorResponse(ErrorCode.InvalidAuthority, response);
            return;
        }

        List<Map<String,String>> role = (List<Map<String,String>>)jwtInfo.get(UserHeadInfo.AUTHORITIES);
        UsernamePasswordAuthenticationToken authenticationToken = getUsernamePasswordAuthenticationToken((String)jwtInfo.get(UserHeadInfo.USERNAME), (String)jwtInfo.get(UserHeadInfo.USERID), role);

        if (request.getAttribute(UserHeadInfo.USERNAME) != null)
        {
            sendErrorResponse(ErrorCode.InvalidAuthority, response);
            return;
        }

        request.setAttribute(UserHeadInfo.USERID,  jwtInfo.get(UserHeadInfo.USERID));
        request.setAttribute(UserHeadInfo.USERNAME, jwtInfo.get(UserHeadInfo.USERNAME));
        request.setAttribute(UserHeadInfo.BPID, jwtInfo.get(UserHeadInfo.BPID));
        request.setAttribute(UserHeadInfo.BpName, jwtInfo.get(UserHeadInfo.BpName));
        request.setAttribute(UserHeadInfo.AUTHORITIES, role.toString());
        if (request.getMethod().equals(HttpMethod.DELETE.name()) && request.getRequestURI().endsWith("/auth/tokens"))
        {
            request.setAttribute("accessToken", accessToken);
        }

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
        chain.doFilter(request, response);
    }

    private JwtMap getJwtInfo(String token)
    {
        try
        {
            JwtMap jwtInfo = JwtTools.getJwtInfo(token, jwtkey, JwtUtils.getAuthorizationHeaderPrefix());
            LOGGER.info("jwtInfo: {}", jwtInfo);
            return jwtInfo;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.error(token + "parse error." + e);
            return null;
        }
    }

    private Boolean checkJwt(Map<String, Object> jwtInfo, HttpServletRequest request, HttpServletResponse response)
    {
        if (null == jwtInfo || jwtInfo.size() < 4)
        {
            return false;
        }

        String agent = request.getHeader("user-agent");
        String remote = request.getRemoteHost();

        String userName = jwtInfo.get(UserHeadInfo.USERNAME).toString();
        String userId = jwtInfo.get(UserHeadInfo.USERID).toString();
        Collection<? extends GrantedAuthority> role = (Collection<? extends GrantedAuthority>)jwtInfo.get(UserHeadInfo.AUTHORITIES);

        if (! agent.equals(jwtInfo.get("user-agent")))
        {
            LOGGER.info("user-agent: {} is not match jwt: {}", agent, jwtInfo.get("user-agent"));
            return false;
        }

        if (! remote.equals(jwtInfo.get("remote")))
        {
            LOGGER.info("remote: {} is not match jwt: {}", remote, jwtInfo.get("remote"));
            return false;
        }

        Long validBegin = Long.parseLong(jwtInfo.get("begin").toString());

        Long curTime = System.currentTimeMillis();

        Long spanTime = curTime - validBegin;
        if (spanTime > (WebConstants.LNJOYING_TOKEN_INDATE*1000))
        {
            LOGGER.info("jwt over time");
            return false;
        }


        request.setAttribute("leftTime", spanTime/1000);
        //if the remain time is less than 5 min, then update the token
        if (spanTime > ((WebConstants.LNJOYING_TOKEN_INDATE-2)*1000))
        {
            Map<String, Object> headerInfo = new HashMap<>();
            headerInfo.put("user-agent", request.getHeader("user-agent"));
            headerInfo.put("remote",  request.getRemoteHost());
            headerInfo.put(UserHeadInfo.USERNAME, userName);
            headerInfo.put(UserHeadInfo.USERID, userId);
            headerInfo.put(UserHeadInfo.AUTHORITIES, role);
            headerInfo.put(UserHeadInfo.BPID, jwtInfo.get(UserHeadInfo.BPID));
            headerInfo.put(UserHeadInfo.BpName, jwtInfo.get(UserHeadInfo.BpName));


            String token = JwtUtils.getNewJwtToken(userName,
                                    headerInfo,
                                    WebConstants.LNJOYING_TOKEN_INDATE,
                                    jwtkey);
            response.addCookie(JwtUtils.getNewCookie(WebConstants.ACCESS_TOKEN_NAME, token, WebConstants.LNJOYING_TOKEN_INDATE, "/"));
        }

        return true;
    }

    private UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(String userName, String userId, List<Map<String, String>> role)
    {
        if (null != userName)
        {
            List<SimpleGrantedAuthority> s = new ArrayList<>();
            if (role == null)
            {
                return new UsernamePasswordAuthenticationToken(userName, null, Collections.singleton(new SimpleGrantedAuthority("ROLE_TENANT")));
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            for (Map<String,String> g:role)
            {
                authorities.add(new SimpleGrantedAuthority(g.get("authority")));
            }

            UsernamePasswordAuthenticationToken userToken = new UsernamePasswordAuthenticationToken(userName, null, authorities);
            Map<String, String> detailMap =  new HashMap<>();
            detailMap.put(UserHeadInfo.USERID, userId);
            userToken.setDetails(detailMap);
            return userToken;
        }

        return null;
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
        out.print(jsonObject.toString());
    }

}
