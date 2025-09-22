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

package com.lnjoying.justice.operation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Slf4j
public class Swagger2Config
{

    @Value(value = "${swagger.enabled}")
    Boolean swaggerEnabled;

    @Bean
    @SuppressWarnings("all")
    public Docket docket() {
        log.info("swagger.enabled: " + swaggerEnabled);
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .enable(swaggerEnabled)
                .groupName("justice")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.lnjoying.justice.operation.controller")
                )
                .paths(PathSelectors.any())
                .build();
    }

    @SuppressWarnings("all")
    public ApiInfo apiInfo() {
        return new ApiInfo(
                "operation api",
                "operation api",
                "V0.1",
                "xx@hxq.com",
                "justice",
                "null",
                "null"
        );
    }
}
