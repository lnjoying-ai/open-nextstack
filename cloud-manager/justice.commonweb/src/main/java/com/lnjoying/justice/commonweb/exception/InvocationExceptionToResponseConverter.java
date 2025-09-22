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

package com.lnjoying.justice.commonweb.exception;

import com.lnjoying.justice.schema.common.ErrorData;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.swagger.invocation.Response;
import org.apache.servicecomb.swagger.invocation.SwaggerInvocation;
import org.apache.servicecomb.swagger.invocation.exception.ExceptionToProducerResponseConverter;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;

import java.util.Optional;

import static com.lnjoying.justice.schema.common.ErrorCode.SystemError;


/**
 * handle invocation and log
 *
 * @author merak
 **/

@Slf4j
public class InvocationExceptionToResponseConverter implements
        ExceptionToProducerResponseConverter<InvocationException>
{
    @Override
    public Class<InvocationException> getExceptionClass()
    {
        return InvocationException.class;
    }

    @Override
    public Response convert(SwaggerInvocation swaggerInvocation, InvocationException e)
    {
        String summary = ((Invocation) swaggerInvocation).getOperationMeta().getSwaggerOperation().getSummary();
        log.error(summary + " error: {}", e);

        ErrorData data = new ErrorData();
        data.setCode(SystemError.getCode());
        data.setMessage(Optional.ofNullable(e.getMessage()).isPresent() ? e.getMessage() : "");
        InvocationException state = new InvocationException(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR, data);
        return Response.failResp(state);
    }

    @Override
    public int getOrder()
    {
        return -50;
    }
}
