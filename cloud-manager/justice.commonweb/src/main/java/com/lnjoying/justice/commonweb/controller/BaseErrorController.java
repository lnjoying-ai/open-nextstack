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

package com.lnjoying.justice.commonweb.controller;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

//@Controller
//@RequestMapping("/error")
public class BaseErrorController extends RestWebController
{
    private static Logger LOGGER = LogManager.getLogger();
    @PostMapping(value = "/400")
    public String error_400() throws IOException
    {
        throw new WebSystemException(ErrorCode.BAD_REQUST, ErrorLevel.INFO);
    }

    @PostMapping(value = "/401")
    public String error_401() throws IOException
    {
        throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
    }

    @PostMapping(value = "/402")
    public String error_402() throws IOException
    {
        throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
    }

    @PostMapping(value = "/403")
    public void error_403() throws IOException
    {
        throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
    }

    @PostMapping(value = "/404")
    public String error_404() throws IOException
    {
        throw new WebSystemException(ErrorCode.UNKNOW_SERVICE, ErrorLevel.INFO);
    }

    @PostMapping(value = "/405")
    public String error_405() throws IOException
    {
        throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
    }

    @PostMapping(value = "/500")
    public String error_500() throws IOException
    {
        throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
    }
}
