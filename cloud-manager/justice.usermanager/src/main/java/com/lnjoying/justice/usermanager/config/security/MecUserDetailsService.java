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
import com.lnjoying.justice.usermanager.common.constant.AuthType;
import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import com.micro.core.persistence.redis.RedisUtil;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class MecUserDetailsService implements UserDetailsService
{
//    private static final Logger LOGGER = LoggerFactory.getLogger(MecUserDetailsService.class);

    // when login failed 5 times, account will be locked.
    private static final Set<RequestLimitRule> rules =
        Collections.singleton(RequestLimitRule.of(Duration.ofMinutes(5), 4));

    // locked overtime
    private static final long OVERTIME = 5 * 60 * 1000;

    private static final RequestRateLimiter LIMITER = new InMemorySlidingWindowRequestRateLimiter(rules);

    private static final Map<String, Long> LOCKED_USERS_MAP = new Hashtable<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Pbkdf2PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String uniqueUserFlag) throws UsernameNotFoundException
    {
        if (uniqueUserFlag.startsWith(AuthType.SMS_AUTH_PREFIX))
        {
            return loadUserBySms(uniqueUserFlag.replace(AuthType.SMS_AUTH_PREFIX,""));
        }

        if (uniqueUserFlag.startsWith(AuthType.EMAIL_AUTH_PREFIX))
        {
            return loadUserByEmail(uniqueUserFlag.replace(AuthType.EMAIL_AUTH_PREFIX,""));
        }

        return loadUserByKey(uniqueUserFlag);
    }

    public UserDetails loadUserByKey(String uniqueUserFlag) throws UsernameNotFoundException
    {
        TblUserInfo tblUserInfo = userRepository.getUserByKey(uniqueUserFlag);
        if (tblUserInfo == null)
        {
            throw new UsernameNotFoundException(
                    "User not found: " + uniqueUserFlag);
        }

        if (!tblUserInfo.getIsAllowed()) {
            throw new UsernameNotFoundException(
                    "User is not allowed to login");
        }

        List<TblRoleInfo> rolePos = userRepository.getRolesByUserId(tblUserInfo.getUserId());
        List<GrantedAuthority> authorities = new ArrayList<>();
        rolePos.forEach(rolePo -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rolePo.getPlatform() + "_" + rolePo.getRole())));

        boolean isLocked = isLocked(uniqueUserFlag);
        if (isLocked) {
            log.info("username:{} have been locked.", tblUserInfo.getUserName());
        }
        return new User(tblUserInfo.getUserName(), tblUserInfo.getPassword(), true, true, true, !isLocked, authorities);
    }

    public UserDetails loadUserBySms(String uniqueUserFlag) throws UsernameNotFoundException
    {
        TblUserInfo tblUserInfo = userRepository.getUserByPhone(uniqueUserFlag);
        if (tblUserInfo == null)
        {
            throw new UsernameNotFoundException(
                    "User not found: " + uniqueUserFlag);
        }

        return buildDetailByVerCode(tblUserInfo, uniqueUserFlag);
    }


    public UserDetails loadUserByEmail(String uniqueUserFlag) throws UsernameNotFoundException
    {
        TblUserInfo tblUserInfo = userRepository.getUserByEmail(uniqueUserFlag);
        if (tblUserInfo == null)
        {
            throw new UsernameNotFoundException(
                    "User not found: " + uniqueUserFlag);
        }
        return buildDetailByVerCode(tblUserInfo, uniqueUserFlag);
    }

    public UserDetails buildDetailByVerCode(TblUserInfo tblUserInfo, String uniqueUserFlag)
    {
        if (!tblUserInfo.getIsAllowed()) {
            throw new UsernameNotFoundException(
                    "User is not allowed to login");
        }

        String ver_code = RedisUtil.get(RedisCacheField.AUTH_VER_CODE, uniqueUserFlag);
        if (ver_code == null)
        {
            throw new UsernameNotFoundException(
                    "code is invalid");
        }

        List<TblRoleInfo> rolePos = userRepository.getRolesByUserId(tblUserInfo.getUserId());
        List<GrantedAuthority> authorities = new ArrayList<>();
        rolePos.forEach(rolePo -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rolePo.getPlatform() + "_" + rolePo.getRole())));

        boolean isLocked = isLocked(uniqueUserFlag);
        if (isLocked) {
            log.info("username:{} have been locked.", tblUserInfo.getUserName());
        }
        return new User(tblUserInfo.getUserName(), passwordEncoder.encode(ver_code), true, true, true, !isLocked, authorities);
    }

    private boolean isLocked(String userId)
    {
        if (LOCKED_USERS_MAP.containsKey(userId))
        {
            long lockedTime = LOCKED_USERS_MAP.get(userId);
            if (System.currentTimeMillis() - lockedTime < OVERTIME)
            {
                return true;
            }
            else
            {
                LOCKED_USERS_MAP.remove(userId);
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * when login failed.
     */
    public void addFailedCount(String userId)
    {
        boolean isOver = LIMITER.overLimitWhenIncremented(userId);
        if (isOver)
        {
            LOCKED_USERS_MAP.put(userId, System.currentTimeMillis());
        }
    }

    /**
     * when login success.
     */
    public void clearFailedCount(String userId)
    {
        LIMITER.resetLimit(userId);
    }
}
