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

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
public class RequestHolder implements Filter
{
//	private static final Logger LOGGER = LoggerFactory.getLogger(AccessFilter.class);

	private static ThreadLocal<HttpServletRequest> httpServletRequestHolder =
			new ThreadLocal<HttpServletRequest>();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
						 FilterChain chain) throws IOException, ServletException {
		httpServletRequestHolder.set((HttpServletRequest) request); // 绑定到当前线程
		try {
			chain.doFilter(request, response);
		} catch (Exception e) {
			throw e;
		} finally {
			httpServletRequestHolder.remove(); // 清理资源引用
		}
	}

	@Override
	public void destroy() {
	}

	public static HttpServletRequest getHttpServletRequest() {
		return httpServletRequestHolder.get();
	}

}
