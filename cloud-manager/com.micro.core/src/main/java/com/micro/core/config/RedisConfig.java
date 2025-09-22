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

package com.micro.core.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class RedisConfig
{
    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.password}")
    private String password;

    @Value("${redis.maxConnect}")
    private int clientcount;

    @Value("${redis.maxConnect}")
    private int maxConnect;


    @Value("${redis.maxIdle}")
    private Integer maxIdle;

    @Value("${redis.maxWaitMillis}")
    private Integer maxWaitMillis;
}

