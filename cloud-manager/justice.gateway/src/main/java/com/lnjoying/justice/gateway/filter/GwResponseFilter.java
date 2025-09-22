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

package com.lnjoying.justice.gateway.filter;

import com.micro.core.common.Utils;
import io.vertx.core.buffer.Buffer;
import org.apache.servicecomb.common.rest.filter.HttpServerFilter;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
import org.apache.servicecomb.swagger.invocation.Response;

public class GwResponseFilter implements HttpServerFilter
{
	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public Response afterReceiveRequest(Invocation invocation, HttpServletRequestEx requestEx)
	{
		System.out.println("after received request");
		return null;
	}

	@Override
	public void beforeSendResponse(Invocation invocation, HttpServletResponseEx responseEx)
	{
		System.out.println("after received response");
		String trans = responseEx.getHeader("trans");
		if (trans != null && trans.equals("convert"))
		{
			String content = responseEx.getBodyBuffer().toString();
			responseEx.setBodyBuffer(Buffer.buffer(Utils.hexToByteArray(content)));
			responseEx.setHeader("Content-Type","application/zip");
		}
	}
}
