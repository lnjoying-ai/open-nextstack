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

package com.lnjoying.justice.commonweb.util;

import com.micro.core.common.JwtTools;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtUtils
{
    private static final String AUTHORIZATION_HEADER_PREFIX = "AnyThingJustice";

    public static String getRawToken(String authorizationHeader)
    {
        return authorizationHeader.substring(AUTHORIZATION_HEADER_PREFIX.length());
    }

    public static String getTokenHeader(String rawToken)
    {
        return AUTHORIZATION_HEADER_PREFIX + rawToken;
    }

    public static boolean validate(String authorizationHeader)
    {
        return StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(AUTHORIZATION_HEADER_PREFIX);
    }

    public static String getAuthorizationHeaderPrefix()
    {
        return AUTHORIZATION_HEADER_PREFIX;
    }

    public static Cookie getNewCookie(String key, String value, int maxAge, String path)
    {
        Cookie cookie = new Cookie(key, getTokenHeader(value));
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        return cookie;
    }

    public static Cookie getLogoutCookie(String key, String value, int maxAge, String path)
    {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        return cookie;
    }

    public static String getNewJwtToken(String subject, Map<String, Object> headerInfo,  int expireTime, String privateSecret)
    {
        Long curTime = System.currentTimeMillis();

        headerInfo.put("begin", curTime.toString());

        String token = JwtTools.getNewJwtToken(subject, headerInfo, expireTime, privateSecret);

//        String token = Jwts.builder()
//                .setSubject(subject)
//                .setHeader(headerInfo)
//                .setExpiration(new Date(System.currentTimeMillis() + expireTime * 1000))
//                .signWith(SignatureAlgorithm.HS512, privateSecret)
//                .compact();
        return token;
    }

    public static String getFSAuthorityToken(String user, String root, String remote, int expireTime, String privateSecret)
    {
        Long curTime = System.currentTimeMillis();

        Map<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("user", user);
        headerInfo.put("root", root);
        headerInfo.put("remote", remote);

        String token = JwtTools.getNewJwtToken(user, headerInfo, expireTime, privateSecret);

        return token;
    }

    public static void setAllowCookie(HttpServletRequest request, HttpServletResponse response)
    {
        String origin = request.getHeader("Origin");
        if(origin == null)
        {
            origin = request.getHeader("Referer");
        }

        response.setHeader("Access-Control-Allow-Origin", origin);                // 允许指定域访问跨域资源
        response.setHeader("Access-Control-Allow-Credentials", "true");       // 允许客户端携带跨域cookie，此时origin值不能为“*”，只能为指定单一域名

        if(RequestMethod.OPTIONS.toString().equals(request.getMethod()))
        {
            String allowMethod  = request.getHeader("Access-Control-Request-Method");
            String allowHeaders = request.getHeader("Access-Control-Request-Headers");
            response.setHeader("Access-Control-Max-Age", "86400");            // 浏览器缓存预检请求结果时间,单位:秒
            response.setHeader("Access-Control-Allow-Methods", allowMethod);  // 允许浏览器在预检请求成功之后发送的实际请求方法名
            response.setHeader("Access-Control-Allow-Headers", allowHeaders); // 允许浏览器发送的请求消息头
            return;
        }
    }

    public static List<GrantedAuthority> createAuthorities(String roleStr)
    {
        String[] roles = roleStr.split(",");
        List<GrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
        for (String role : roles)
        {
            role = role.toUpperCase();
            if (! role.startsWith("ROLE_"))
            {
                role = "ROLE_"+role;
            }
            simpleGrantedAuthorities.add(new SimpleGrantedAuthority(role));
        }

        return simpleGrantedAuthorities;
    }
}
