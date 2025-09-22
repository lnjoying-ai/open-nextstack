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

package com.lnjoying.vm.handler;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler
{
    @ExceptionHandler(value = WebSystemException.class)
    @ResponseBody
    public Object baseErrorHandler(HttpServletRequest req, WebSystemException e)
    {
        ErrorData data = new ErrorData();

        data.setCode(e.getCode().getCode());

        data.setMessage(e.getMessage());
        return data;
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Object defaultErrorHandler(HttpServletRequest req, Exception e)
    {
        log.error("default Exception:{}", e.getMessage());
        ErrorData data = new ErrorData();
//       # ErrorCode.SystemError
        data.setCode(9999);
        data.setMessage("System error.");
        return data;
    }

}
