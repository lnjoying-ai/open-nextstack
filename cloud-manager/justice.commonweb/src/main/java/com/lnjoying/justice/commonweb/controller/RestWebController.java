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
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;

import java.lang.reflect.InvocationTargetException;

public class RestWebController
{
    private static Logger LOGGER = LogManager.getLogger();

//    @ExceptionHandler
//    public void handleSystemException(WebSystemException e,
//                                         HttpServletResponse response) throws IOException
//    {
//        WebSystemException.setResponseStatus(e.getCode(), response);
//
//        JsonObject jsonObject = new JsonObject();
//        jsonObject.put("code", e.getCode().getCode());
//        jsonObject.put("message", e.getCode().getMessage() + e.getDetailMsg());
//
//        PrintWriter out = response.getWriter();
//        out.print(jsonObject.toString());
//    }
//
//    protected void handleSystemException(WebSystemException e, String detailMsg,
//                                      HttpServletResponse response) throws IOException
//    {
//        WebSystemException.setResponseStatus(e.getCode(), response);
//
//        JsonObject jsonObject = new JsonObject();
//        jsonObject.put("code", e.getCode().getCode());
//        jsonObject.put("message", e.getCode().getMessage() + " " + detailMsg);
//
//        PrintWriter out = response.getWriter();
//        out.print(jsonObject.toString());
//    }
//
////    @ExceptionHandler
////    public void handleWebSystemException(WebSystemException e) throws IOException
////    {
////        System.out.println("ssss");
////    }
//
//    @ExceptionHandler
//    public void handleServletException(ServletException e) throws IOException
//    {
//        System.out.println("ssss");
//    }
//
//
//
//    @ExceptionHandler
//    public void handleException(Exception e) throws IOException
//    {
//        System.out.println("ssss");
//    }
//
//    @ExceptionHandler
//    public void handleRuntimeException(RuntimeException e, HttpServletResponse response)
//    {
//        LOGGER.error("Unhandled runtime exception.", e);
//        e.printStackTrace();
//        try
//        {
//            if (e instanceof AuthenticationException)
//            {
//                WebSystemException re = new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
//                handleSystemException(re, e.getMessage(), response);
//                return;
//            }
//
//            if (e instanceof AccessDeniedException)
//            {
//                WebSystemException re = new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
//                handleSystemException(re, e.getMessage(), response);
//                return;
//            }
//
//            if (e instanceof HttpMessageNotReadableException)
//            {
//                WebSystemException re = new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
//                handleSystemException(re, e.getMessage(), response);
//                return;
//            }
//
//            WebSystemException re = new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//            handleSystemException(re, e.getMessage(), response);
//            return;
//        }
//        catch (IOException e1)
//        {
//            e1.printStackTrace();
//        }
//    }

    public WebSystemException throwWebException(Exception e)
    {
        if (e instanceof WebSystemException)
        {
            return  (WebSystemException)e;
        }
        else if ( e instanceof InvocationException || e instanceof InvocationTargetException)
        {
            LOGGER.info("InvocationException", e);
            return null;
        }
        else
        {
            return new WebSystemException(ErrorCode.SystemError , ErrorLevel.CRITICAL);
        }
    }
}
