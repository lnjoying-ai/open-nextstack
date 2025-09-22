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
import org.apache.servicecomb.swagger.invocation.Response;
import org.apache.servicecomb.swagger.invocation.SwaggerInvocation;
import org.apache.servicecomb.swagger.invocation.exception.ExceptionToProducerResponseConverter;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.apache.tomcat.util.buf.StringUtils;

import javax.validation.ConstraintViolationException;

import static com.lnjoying.justice.schema.common.ErrorCode.PARAM_ERROR;


/**
 * BindExceptionHandler for param exception
 *
 * @author merak
 **/

@Slf4j
public class ConstraintViolationExceptionToResponseConverter implements
        ExceptionToProducerResponseConverter<ConstraintViolationException>
{
    @Override
    public Class<ConstraintViolationException> getExceptionClass()
    {
        return ConstraintViolationException.class;
    }

    @Override
    public Response convert(SwaggerInvocation swaggerInvocation, ConstraintViolationException e)
    {
        log.error(e.getMessage());
        ErrorData data = new ErrorData();

        data.setCode(PARAM_ERROR.getCode());

        String[] result = e.getConstraintViolations().stream().map(x -> "tip: " + x.getMessage() + ", invalidValue: " + x.getInvalidValue() + "; ")
                .toArray(String[]::new);
        String join = "param error [" + StringUtils.join(result) + "]";
        data.setMessage(join);
        InvocationException state = new InvocationException(javax.ws.rs.core.Response.Status.BAD_REQUEST, data);

        return Response.failResp(state);
    }

    @Override
    public int getOrder()
    {
        return -200;
    }
}
