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
import com.lnjoying.justice.schema.constant.UserHeadInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.swagger.invocation.context.ContextUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * request context tool
 *
 */
public class HttpContextUtils
{

	private static Logger LOGGER = LogManager.getLogger();
	public static HttpServletRequest getHttpServletRequest()
	{
		HttpServletRequest request = null;
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (null != attributes)
		{
			request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		}
		if (request == null)
		{
			request = (((HttpServletRequest)(ContextUtils.getInvocationContext()).getLocalContext().get("servicecomb-rest-request")));
		}
		return request;
	}

	public static String getDomain()
	{
		HttpServletRequest request = getHttpServletRequest();
		StringBuffer url = request.getRequestURL();
		return url.delete(url.length() - request.getRequestURI().length(), url.length()).toString();
	}

	public static String getOrigin()
	{
		HttpServletRequest request = getHttpServletRequest();
		return request.getHeader("Origin");
	}

	public static boolean hasAttributeRoles(HttpServletRequest request, String role)
	{
		String roles = (String)request.getAttribute(UserHeadInfo.AUTHORITIES);
		if (roles == null || ! roles.contains(role))
		{
			return false;
		}

		return true;

//		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//		if (authentication == null)
//		{
//			return false;
//		}
//		if (authentication.getAuthorities() == null || ! authentication.getAuthorities().contains(role))
//		{
//			return false;
//		}
//		return true;
	}

	public static boolean isAttributeOwner(HttpServletRequest request, String userId)
	{
		String userIdAttr = (String) request.getAttribute(UserHeadInfo.USERID);
		if (userIdAttr == null || ! userIdAttr.equals(userId))
		{
			return false;
		}

		return true;
	}

	public static boolean canOperator(String userId, String role)
	{
		HttpServletRequest httpReq = getHttpServletRequest();
		if (! HttpContextUtils.hasAttributeRoles(httpReq, role) && ! HttpContextUtils.isAttributeOwner(httpReq, userId))
		{
			return false;
		}
		return true;
	}

	public static String getStrAttribute(String key)
	{
		HttpServletRequest request = getHttpServletRequest();
		return (String)request.getAttribute(key);
	}

	public static String getStrHead(String key)
	{
		HttpServletRequest request = getHttpServletRequest();
		return request.getHeader(key);
	}

	public static Integer getIntAttribute(String key)
	{
		HttpServletRequest request = getHttpServletRequest();
		return (Integer)request.getAttribute(key);
	}

	public static boolean haveHeaderRole(String targetRole) throws WebSystemException
	{
		HttpServletRequest request = getHttpServletRequest();
		String authorities = request.getHeader(UserHeadInfo.AUTHORITIES);
		LOGGER.info("target role: {} user role:{}", targetRole, authorities);
		if (StringUtils.isEmpty(authorities) || ! authorities.contains(targetRole))
		{
			throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
		}

		return true;
	}
}

