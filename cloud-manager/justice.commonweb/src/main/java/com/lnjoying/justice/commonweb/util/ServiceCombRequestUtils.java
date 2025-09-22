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

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.constant.RoleConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.vertx.http.StandardHttpServletRequestEx;
import org.apache.servicecomb.swagger.invocation.context.ContextUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.ErrorCode.AUTHORITIES_NOT_FOUND;
import static com.lnjoying.justice.schema.common.ErrorLevel.ERROR;
import static com.lnjoying.justice.schema.constant.UserHeadInfo.*;

/**
 * request utils of servicecomb
 *
 * @author merak
 **/

public class ServiceCombRequestUtils
{

    public static HttpServletRequest getHttpServletRequest()
    {
        HttpServletRequest request = null;
        Invocation invocationContext = (Invocation) ContextUtils.getInvocationContext();
        if (Objects.nonNull(invocationContext))
        {
            request = (invocationContext).getRequestEx();
        }

        if (request == null) {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                request = ((ServletRequestAttributes) requestAttributes).getRequest();
            }
        }
        return request;
    }

    public static String getAuthorities()
    {
        String authorities = (String)getHttpServletRequest().getAttribute(AUTHORITIES);
        if (StringUtils.isBlank(authorities))
        {
            throw new WebSystemException(AUTHORITIES_NOT_FOUND, ERROR);
        }
        return authorities;
    }
    
    public static boolean isAdmin()
    {
        HttpServletRequest request = getHttpServletRequest();
        String authorities = (String)request.getAttribute(AUTHORITIES);
        if (StringUtils.isBlank(authorities))
        {
            authorities = request.getHeader(AUTHORITIES);
        }
        boolean hasAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_ADMIN)
                || authorities.contains(RoleConstants.ROLE_ALL_ADMIN));
        return hasAuthorities;
    }

    public static boolean isBpAdmin()
    {
        String authorities = (String)getHttpServletRequest().getAttribute(AUTHORITIES);
        boolean hasAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_ALL_TENANT_ADMIN));
        return hasAuthorities;
    }

    public static boolean isBpUserOrPersonal()
    {
        String authorities = (String)getHttpServletRequest().getAttribute(AUTHORITIES);
        boolean hasAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_TENANT)  || authorities.contains(RoleConstants.ROLE_ALL_TENANT));
        return hasAuthorities;
    }

    public static String getUserId()
    {
        HttpServletRequest request = getHttpServletRequest();
        String userId = (String)request.getAttribute(USERID);
        if (StringUtils.isBlank(userId))
        {
            userId =  request.getHeader(USERID);
        }

        if (StringUtils.isBlank(userId))
        {
            throw new WebSystemException(ErrorCode.USER_ID_NOT_FOUND, ErrorLevel.ERROR);
        }
        return userId;
    }

    public static String getUserName()
    {
        String userName = (String)getHttpServletRequest().getAttribute(USERNAME);
        if (StringUtils.isBlank(userName))
        {
            throw new WebSystemException(ErrorCode.USER_NAME_NOT_FOUND, ErrorLevel.ERROR);
        }
        return userName;
    }

    public static String getBpId()
    {
        String bpId = (String)getHttpServletRequest().getAttribute(BPID);

        // role of tenant_admin must have bpId
        if (isBpAdmin())
        {
            if (StringUtils.isBlank(bpId))
            {
                throw new WebSystemException(ErrorCode.BP_ID_NOT_FOUND, ErrorLevel.ERROR);
            }
        }

        if (StringUtils.isBlank(bpId))
        {
            bpId = null;
        }

        return bpId;
    }

    public static String getBpName()
    {
        String bpName = (String)getHttpServletRequest().getAttribute(BpName);

        if (StringUtils.isBlank(bpName))
        {
            throw new WebSystemException(ErrorCode.BP_NAME_NOT_FOUND, ErrorLevel.ERROR);
        }
        return bpName;
    }

    public static Pair<String, String> getUserAttributes()
    {

        // left bpId, right userId
        Pair<String, String> pair;

        if (isAdmin())
        {
            pair = ImmutablePair.nullPair();
        }
        else if (isBpAdmin())
        {
            pair = ImmutablePair.of(getBpId(), null);
        }
        else
        {
            pair = ImmutablePair.of(getBpId(), getUserId());
        }

        return pair;
    }

    /**
     * just for simplified  query not real user id
     * @return
     */
    public static String queryUserId()
    {
        return getUserAttributes().getRight();
    }

    /**
     * just for simplified query not real bp id
     * @return
     */
    public static String queryBpId()
    {
        return getUserAttributes().getLeft();
    }

    /**
     * get HttpServletRequest from InvocationContext
     *
     * @return
     */
    public static HttpServletRequest httpServletRequest()
    {
        return (((StandardHttpServletRequestEx) ((Invocation) ContextUtils.getInvocationContext()).getLocalContext().get("servicecomb-rest-request")));
    }

    public static void checkRoleAdmin()
    {
        String authorities = getAuthorities();
        boolean hasAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_ADMIN)
                || authorities.contains(RoleConstants.ROLE_ALL_ADMIN));

        if (!hasAuthorities)
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
    }

    public static void checkRoleTenantAdmin()
    {
        String authorities = getAuthorities();
        boolean hasAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_ALL_TENANT_ADMIN));

        if (!hasAuthorities)
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }

    }

    public static void checkRoleTenant()
    {
        String authorities = getAuthorities();
        boolean hasAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_TENANT)
                || authorities.contains(RoleConstants.ROLE_ALL_TENANT));

        if (!hasAuthorities)
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
    }

    public static void checkRoleTenantAdminOrAdmin()
    {
        String authorities = getAuthorities();

        boolean hasTenantAdminAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_ALL_TENANT_ADMIN));

        boolean hasAdminAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_ADMIN)
                || authorities.contains(RoleConstants.ROLE_ALL_ADMIN));

        if (!(hasTenantAdminAuthorities || hasAdminAuthorities))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
    }
    
    public static void checkRoleUmsTenantOrAdmin()
    {
        String authorities = (String)getHttpServletRequest().getAttribute(AUTHORITIES);
        boolean hasTenantAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_TENANT)
                || authorities.contains(RoleConstants.ROLE_ALL_TENANT));

        boolean hasTenantAdminAuthorities = StringUtils.isNotBlank(authorities)
                && authorities.contains(RoleConstants.ROLE_ALL_TENANT_ADMIN);

        boolean hasAdminAuthorities = StringUtils.isNotBlank(authorities)
                && (authorities.contains(RoleConstants.ROLE_UMS_ADMIN)
                || authorities.contains(RoleConstants.ROLE_ALL_ADMIN));

        if (!(hasTenantAuthorities || hasTenantAdminAuthorities || hasAdminAuthorities))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
    }
}
