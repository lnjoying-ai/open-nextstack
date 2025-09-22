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

package com.lnjoying.justice.commonweb.filter;

import com.lnjoying.justice.schema.constant.UserHeadInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class AccessFilter extends OncePerRequestFilter {
//    private static final Logger LOGGER = LoggerFactory.getLogger(AccessFilter.class);

    private static ThreadLocal<HttpServletRequest> httpServletRequestHolder =
            new ThreadLocal<HttpServletRequest>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException
    {
//        request.setAttribute(UserHeadInfo.USERID,request.getHeader(UserHeadInfo.USERID));
//        request.setAttribute(UserHeadInfo.USERNAME,request.getHeader(UserHeadInfo.USERNAME));
//        request.setAttribute(UserHeadInfo.AUTIORITIES,request.getHeader(UserHeadInfo.AUTIORITIES));
        log.info("access filter authorites: {} user Id: {}", request.getHeader(UserHeadInfo.AUTHORITIES), request.getHeader(UserHeadInfo.USERID));
        RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
        filterChain.doFilter(request, response);
    }
}
