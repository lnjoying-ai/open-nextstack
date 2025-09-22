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

package com.lnjoying.justice.usermanager;

//import org.apache.servicecomb.springboot2.starter.EnableServiceComb;
//import org.apache.servicecomb.springboot2.starter.EnableServiceComb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.springboot2.starter.EnableServiceComb;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.request.RequestContextListener;

@SpringBootApplication(scanBasePackages = "com.lnjoying.justice.usermanager", exclude = {SecurityAutoConfiguration.class})
@MapperScan(basePackages = {"com.lnjoying.justice.usermanager.db.mapper"})
@EnableScheduling
@EnableServiceComb
public class UserApplication
{
    private static Logger LOGGER = LogManager.getLogger();

    @Bean
    public RequestContextListener requestContextListener(){
        return new RequestContextListener();
    }


    /**
     * Main.
     */
    public static void main(String[] args)
    {
        LOGGER.info("ums starting");
        SpringApplication.run(UserApplication.class, args);
    }
}
