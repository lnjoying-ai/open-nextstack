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

import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.common.SystemException;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

public class WebSystemException extends SystemException
{
    private static final long serialVersionUID = -89334987778999224L;
    public WebSystemException(ErrorCode code, ErrorLevel level) {
        super(code, level);
    }

    public WebSystemException(ErrorCode code, ErrorLevel level, String detailMsg)
    {
        super(code, level, detailMsg);
    }

    public WebSystemException(ErrorCode code, ErrorLevel level, int httpResponseCode)
    {
        super(code, level, httpResponseCode);
    }

    public WebSystemException(ErrorCode code, Throwable cause, ErrorLevel level) {
        super(code, cause, level);
    }


    public static Status getHeadStatus(ErrorCode status)
    {
        switch (status)
        {
            case DuplicateUser:
            case CreateUserError:
            case EmailOccupied:
            case PhoneOccupied:
                return Status.CONFLICT;
            case Project_Not_Exist:
            case User_Not_Exist:
            case InvalidReq:
                return Status.NOT_FOUND;
            case SystemError:
            case SQL_ERROR:
                return Status.INTERNAL_SERVER_ERROR;
            case Redirect:
                return Status.FOUND;
            case InvalidAuthority:
                return Status.UNAUTHORIZED;
            case No_Permission:
                return Status.FORBIDDEN;
            default:
                return Status.BAD_REQUEST;
        }
    }

    public static void setResponseStatus(ErrorCode status, HttpServletResponse servletResponse)
    {
        servletResponse.setContentType("application/json;charset=utf-8");

        if (servletResponse.getStatus() >= HttpStatus.BAD_REQUEST.value())
        {
            return;
        }
        servletResponse.setStatus(getHeadStatus(status).getStatusCode());
    }
}
