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

package com.lnjoying.vm;

import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.springboot2.starter.EnableServiceComb;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestContextListener;

@SpringBootApplication(scanBasePackages = "com.lnjoying.vm", exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@MapperScan(basePackages = {"com.lnjoying.vm.mapper"})
@EnableScheduling
@EnableServiceComb
@EnableTransactionManagement
@ComponentScan("com.lnjoying")
@EnableAspectJAutoProxy(exposeProxy = true)
@Slf4j
public class VmApplication
{
//    private static final Logger LOGGER = LogManager.getLogger();

    @Bean
    public RequestContextListener requestContextListener()
    {
        return new RequestContextListener();
    }

    /**
     * Main.
     */
    public static void main(String[] args)
    {
        log.info("vm manager starting");
        SpringApplication.run(VmApplication.class, args);
    }
}
