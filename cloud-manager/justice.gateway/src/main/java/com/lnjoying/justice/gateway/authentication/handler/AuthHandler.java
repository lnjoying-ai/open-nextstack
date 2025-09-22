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

package com.lnjoying.justice.gateway.authentication.handler;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.JwtUtils;
import com.lnjoying.justice.gateway.config.DataSourceConfig;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorData;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.schema.constant.UserHeadInfo;
import com.lnjoying.justice.schema.constant.WebConstants;
import com.lnjoying.justice.schema.service.ums.AuthService;
import com.micro.core.common.JwtTools;
import com.micro.core.common.Utils;
import com.micro.core.persistence.redis.RedisClientUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.impl.JwtMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.definition.MicroserviceMeta;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.foundation.vertx.http.VertxServerRequestToHttpServletRequest;
import org.apache.servicecomb.provider.pojo.Invoker;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.apache.servicecomb.swagger.invocation.InvocationType;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AuthHandler implements Handler
{
//	private static Logger LOGGER = LoggerFactory.getLogger(AuthHandler.class);

	String jwtkey = null;

	private static AuthService authService;

	static {
		authService = Invoker.createProxy("ums","authService", AuthService.class);
	}

//	@Autowired
//	private CombRpcSerice combRpcSerice;

	public AuthHandler()
	{
		DataSourceConfig dataSourceConfig = BeanUtils.getBean("dataSourceConfig");
		if (dataSourceConfig != null)
		{
			jwtkey = dataSourceConfig.getJwtkey();
		}
//
	}

	@Override
	public void init(MicroserviceMeta microserviceMeta, InvocationType invocationType)
	{

	}

	@Override
	public void handle(Invocation invocation, AsyncResponse asyncResp) throws Exception
	{
		VertxServerRequestToHttpServletRequest requestEx = (VertxServerRequestToHttpServletRequest)invocation.getRequestEx();
		AtomicReference<Boolean> authSucc = new AtomicReference<>(false);
		if (null == requestEx)
		{
			invocation.next(asyncResp);
			return;
		}

		Boolean isUseApiKey = checkUseApiKey(requestEx);
		if (isUseApiKey)
		{
			checkApiKey(requestEx).whenComplete((authUser, e)->
			{
				if (authUser.isSuccess())
				{
					//39937079-99fe-4cd8-881f-04ca8c4fe09d
					requestEx.getContext().request().headers().set(UserHeadInfo.USERID, authUser.getUserId());
					requestEx.getContext().request().headers().set(UserHeadInfo.USERNAME, authUser.getUserName());
					requestEx.getContext().request().headers().set(UserHeadInfo.AUTHORITIES, authUser.getAuthorities());
					requestEx.getContext().request().headers().set(UserHeadInfo.BPID, "");
					requestEx.getContext().request().headers().set(UserHeadInfo.BpName, "");
					doHandle(invocation, asyncResp, true, e);

				}
				else
				{
					sendErrorResp(asyncResp, ErrorCode.InvalidAuthority);
				}
			});
			return;
		}
		else
		{
			String accessToken = requestEx.getHeader(WebConstants.HEADER_ACCESS_TOKEN_NAME);
			if (accessToken == null)
			{
				try
				{
					Cookie[] cookies = requestEx.getCookies();

					if (cookies != null)
					{
						for (Cookie k : cookies)
						{
							if (k.getName().equals(WebConstants.ACCESS_TOKEN_NAME) && k.getValue().startsWith(JwtUtils.getAuthorizationHeaderPrefix()))
							{
								accessToken = k.getValue();
								break;
							}
						}
					}
				}
				catch (Exception e)
				{
					sendErrorResp(asyncResp, ErrorCode.InvalidAuthority);
					return;
				}
			}


			if (accessToken == null)
			{
				sendErrorResp(asyncResp, ErrorCode.InvalidAuthority);
				return;
			}
			authSucc.set(checkJwt(accessToken, requestEx));
		}

		Throwable authException = null;
		doHandle(invocation, asyncResp, authSucc.get(), authException);
		
	}

	protected void doHandle(Invocation invocation, AsyncResponse asyncResp, Boolean authSucc, Throwable authException)
	{
		if (authException != null)
		{
			sendErrorResp(asyncResp, ErrorCode.InvalidAuthority);
			return;
		}

		if (!authSucc)
		{
			sendErrorResp(asyncResp, ErrorCode.InvalidAuthority);
		}

		log.debug("auth success.");
		try
		{
			invocation.next(asyncResp);
		}
		catch (Throwable e)
		{
			asyncResp.consumerFail(e);
		}
	}

	private Boolean checkJwt(String accessToken, VertxServerRequestToHttpServletRequest request)
	{
		try
		{
			JwtMap jwtInfo = JwtTools.getJwtInfo(accessToken, jwtkey, JwtUtils.getAuthorizationHeaderPrefix());
			log.info("jwtInfo: {}", jwtInfo);

			if (null == jwtInfo || jwtInfo.size() < 4)
			{
				return false;
			}

			String agent = request.getHeader("user-agent");
			String remote = request.getRemoteHost();
			Collection<? extends GrantedAuthority> role = (Collection<? extends GrantedAuthority>) jwtInfo.get(UserHeadInfo.AUTHORITIES);

			if (!agent.equals(jwtInfo.get("user-agent")))
			{
				log.info("user-agent: {} is not match jwt: {}", agent, jwtInfo.get("user-agent"));
				return false;
			}

			if (!remote.equals(jwtInfo.get("remote")))
			{
				log.info("remote: {} is not match jwt: {}", remote, jwtInfo.get("remote"));
				//return false;
			}

			Long validBegin = Long.parseLong(jwtInfo.get("begin").toString());

			Long curTime = System.currentTimeMillis();

			Long spanTime = curTime - validBegin;
			if (spanTime > (WebConstants.LNJOYING_TOKEN_INDATE * 1000)) {
				log.info("jwt over time");
				return false;
			}

			String accessHash = Utils.getSHA(accessToken);
			if (checkIsLogout(accessHash))
			{
				log.info("jwt check have been dropped for {}", jwtInfo.get(UserHeadInfo.USERNAME));
				return false;
			}

			request.getContext().request().headers().set(UserHeadInfo.USERID, (String)jwtInfo.get(UserHeadInfo.USERID));
			request.getContext().request().headers().set(UserHeadInfo.USERNAME,    (String)jwtInfo.get(UserHeadInfo.USERNAME));
			request.getContext().request().headers().set(UserHeadInfo.AUTHORITIES, role.toString());
			request.getContext().request().headers().set(UserHeadInfo.BPID, (String)jwtInfo.get(UserHeadInfo.BPID));
			request.getContext().request().headers().set(UserHeadInfo.BpName,    (String)jwtInfo.get(UserHeadInfo.BpName));
			return true;
		}
		catch (ExpiredJwtException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private Boolean checkUseApiKey(VertxServerRequestToHttpServletRequest request)
	{
		String principal = request.getHeader(UserHeadInfo.ApiKeyHeaderName);

		String credential = request.getHeader(UserHeadInfo.ApiKeySecretHeaderName);
		return null != principal && null != credential;
	}

	private Boolean getResult(Invocation invocation,
							  VertxServerRequestToHttpServletRequest request
							  )
	{
			try
			{
				invocation.next(response ->
				{
					Boolean result = response.getResult();
					if (!result)
					{
						log.info("get reuslt :{}", result);
						throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
					}
				});
				return true;
			}
			catch (Exception e)
			{
				return false;
			}

	}

	private CompletableFuture<AuthService.AuthUser> checkApiKey(VertxServerRequestToHttpServletRequest request)
	{

		String principal = request.getHeader(UserHeadInfo.ApiKeyHeaderName);

		String credential = request.getHeader(UserHeadInfo.ApiKeySecretHeaderName);
		CompletableFuture<AuthService.AuthUser> future = authService.auth(principal, credential);
//			CombRpcSerice combRpcSerice =  BeanUtils.getBean(CombRpcSerice.class);
//			CombRpcSerice combRpcSerice = BeanUtils.getBean(CombRpcSerice.class);
//			boolean ok = combRpcSerice.getUmsService().checkApiKey(principal, credential);
//		UmsService ums = Invoker.createProxy("ums","umsService", UmsService.class);
//		CompletableFuture<Boolean>future = ums.checkApiKeyAsync(principal, credential);
		return future;
	}

	public boolean checkIsLogout(String accessHash)
	{
		try
		{
			if (RedisClientUtils.get(RedisCacheField.ACCESS_TOKEN_EXPIRE, accessHash) != null)
			{
				log.error("token have been deleted ", accessHash);
				return true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	void sendErrorResp(AsyncResponse asyncResp, ErrorCode errorCode)
	{
		ErrorData errorData = new ErrorData();
		errorData.setCode(errorCode.getCode());
		errorData.setMessage(errorCode.getMessage());
		asyncResp.consumerFail(new InvocationException(Status.UNAUTHORIZED, errorData));
	}
}
