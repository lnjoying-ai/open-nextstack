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

import com.lnjoying.justice.commonweb.util.JwtUtils;
import com.lnjoying.justice.schema.constant.UserHeadInfo;
import com.lnjoying.justice.schema.constant.WebConstants;
import com.lnjoying.justice.usermanager.db.model.TblBpInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.repo.BpRepository;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {


    @Autowired
    private MecUserDetailsService mecUserDetailsService;

    @Value("${jwtkey}")
    private String jwtkey;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BpRepository bpRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException
    {
        String userName = authentication.getName();
        TblUserInfo tblUserInfo = userRepository.getUserByUserName(userName);

        TblBpInfo tblBpInfo = null;
        if ( !StringUtils.isEmpty(tblUserInfo.getBpId()))
        {
            tblBpInfo = bpRepository.getBpInfo(tblUserInfo.getBpId());
        }

        mecUserDetailsService.clearFailedCount(userName);
        User user= (User) authentication.getPrincipal();

        buildHttpRsp(user, request, response, tblUserInfo, tblBpInfo);
    }

    void buildHttpRsp(User user, HttpServletRequest request, HttpServletResponse response, TblUserInfo tblUserInfo, TblBpInfo tblBpInfo)
    {
        Object authorities = user.getAuthorities();

        Map<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("user-agent", request.getHeader("user-agent"));
        headerInfo.put("remote",  request.getRemoteHost());
        headerInfo.put(UserHeadInfo.USERNAME, user.getUsername());
        headerInfo.put(UserHeadInfo.USERID, tblUserInfo.getUserId());
        headerInfo.put(UserHeadInfo.AUTHORITIES, authorities);

        String bpId = (tblBpInfo != null)   ? tblBpInfo.getBpId() : "";
        String bpName = (tblBpInfo != null) ? tblBpInfo.getBpName() : "";
        headerInfo.put(UserHeadInfo.BPID, bpId);
        headerInfo.put(UserHeadInfo.BpName, bpName);

        String token = JwtUtils.getNewJwtToken(user.getUsername(),
                headerInfo,
                WebConstants.LNJOYING_TOKEN_INDATE,
                jwtkey);
        response.addHeader("X-Access-Token", JwtUtils.getAuthorizationHeaderPrefix()+token);
        response.addCookie(JwtUtils.getNewCookie(WebConstants.ACCESS_TOKEN_NAME, token, WebConstants.LNJOYING_TOKEN_INDATE, "/"));
    }
}
