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

import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.usermanager.db.model.TblBpInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import com.micro.core.persistence.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class EmailLoginSuccessHandler extends LoginSuccessHandler
{

//    private static final Logger LOGGER = LoggerFactory.getLogger(EmailLoginSuccessHandler.class);

    @Autowired
    private MecUserDetailsService mecUserDetailsService;

    @Value("${jwtkey}")
    private String jwtkey;

    @Autowired
    UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException
    {
        String email = authentication.getName();
        TblUserInfo tblUserInfo = userRepository.getUserByEmail(email);

        TblBpInfo tblBpInfo = null;
        if ( !StringUtils.isEmpty(tblUserInfo.getBpId()))
        {
            tblBpInfo = bpRepository.getBpInfo(tblUserInfo.getBpId());
        }

        RedisUtil.delete(RedisCacheField.AUTH_VER_CODE + email);

        mecUserDetailsService.clearFailedCount(email);
        User user= (User) authentication.getPrincipal();

        buildHttpRsp(user, request, response, tblUserInfo, tblBpInfo);
    }
}
