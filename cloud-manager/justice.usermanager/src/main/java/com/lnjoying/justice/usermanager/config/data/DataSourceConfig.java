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

package com.lnjoying.justice.usermanager.config.data;

import com.micro.core.config.RedisConfig;
import com.micro.core.persistence.redis.RedisUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;


@Configuration
@PropertySource(value = "classpath:application.properties")
public class
DataSourceConfig
{
    private static Logger LOGGER = LogManager.getLogger();

    @Autowired
    private RedisConfig redisConfig;

    @Bean
    RedisConfig createRedisConfig()
    {
        return new RedisConfig();
    }

    @PostConstruct
    void start()
    {
        try
        {
            LOGGER.info("connect redis");
            RedisUtil.init(redisConfig);
//            RedisClientUtils.init("user manager", redisConfig.getHost(), redisConfig.getPort(), redisConfig.getPassword(), redisConfig.getClientcount());
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
