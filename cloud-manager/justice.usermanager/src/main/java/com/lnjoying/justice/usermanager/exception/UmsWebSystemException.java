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

package com.lnjoying.justice.usermanager.exception;

import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

/**
 * ums web exception
 * Refer to WebSystemException, inherit RuntimeException, not IOException
 *
 * @author merak
 **/

public class UmsWebSystemException extends RuntimeException
{

    /**
     * Error code
     */
    private ErrorCode code;

    /**
     * Error level
     */
    private ErrorLevel level;

    private String detailMsg;

    /**
     * HTTP response code (optional)
     */
    private int httpResponseCode = -1;

    public UmsWebSystemException(ErrorCode code, ErrorLevel level)
    {
        this(code, null, level);
    }

    public UmsWebSystemException(ErrorCode code, ErrorLevel level, String detailMsg)
    {
        this(code, null, level);
        this.detailMsg = detailMsg;
    }

    public UmsWebSystemException(ErrorCode code, ErrorLevel level, int httpResponseCode)
    {
        this(code, null, level);
        setHttpResponseCode(httpResponseCode);
    }

    public UmsWebSystemException(ErrorCode code, Throwable cause, ErrorLevel level)
    {
        super(code.getMessage(), cause);
        this.code = code;
        this.level = level;
    }

    public ErrorCode getCode()
    {
        return code;
    }

    public ErrorLevel getLevel()
    {
        return level;
    }

    public String getDetailMsg()
    {
        return detailMsg;
    }

    public int getHttpResponseCode()
    {
        return httpResponseCode;
    }

    public void setCode(ErrorCode code)
    {
        this.code = code;
    }

    public void setLevel(ErrorLevel level)
    {
        this.level = level;
    }

    public void setDetailMsg(String detailMsg)
    {
        this.detailMsg = detailMsg;
    }

    public void setHttpResponseCode(int httpResponseCode)
    {
        this.httpResponseCode = httpResponseCode;
    }

    public static Response.Status getHeadStatus(ErrorCode status)
    {
        switch (status)
        {
            case DuplicateUser:
            case CreateUserError:
            case EmailOccupied:
            case PhoneOccupied:
                return Response.Status.CONFLICT;
            case Project_Not_Exist:
            case User_Not_Exist:
            case InvalidReq:
                return Response.Status.NOT_FOUND;
            case SystemError:
            case SQL_ERROR:
                return Response.Status.INTERNAL_SERVER_ERROR;
            case Redirect:
                return Response.Status.FOUND;
            case InvalidAuthority:
                return Response.Status.UNAUTHORIZED;
            default:
                return Response.Status.BAD_REQUEST;
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
