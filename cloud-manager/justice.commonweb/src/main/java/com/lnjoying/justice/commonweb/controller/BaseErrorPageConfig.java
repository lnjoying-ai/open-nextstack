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

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.http.HttpStatus;

//@Component
public class BaseErrorPageConfig implements ErrorPageRegistrar
{
    @Override
    public void registerErrorPages(ErrorPageRegistry registry)
    {
        ErrorPage[] errorPages = new ErrorPage[]
                                {
                                        new ErrorPage(HttpStatus.BAD_REQUEST, "/error/400"),
                                        new ErrorPage(HttpStatus.UNAUTHORIZED, "/error/401"),
                                        new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"),
                                        new ErrorPage(HttpStatus.FORBIDDEN, "/error/403"),
                                        new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500"),
                                        new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/error/405"),
                                        new ErrorPage(Throwable.class, "/error/500")
                                };
        registry.addErrorPages(errorPages);

    }

}
