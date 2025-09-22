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

package com.micro.core.persistence.redis;

import com.micro.core.config.RedisConfig;
import com.micro.core.exception.BaseException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Slf4j
public class RedisPoolUtil
{

//    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPoolUtil.class);

    static GenericObjectPool<StatefulRedisConnection<String, String>> instance;

    public static void init(RedisConfig redisConfig)
    {
        RedisURI redisUri = RedisURI.builder().withHost(redisConfig.getHost()).withPort(redisConfig.getPort())
                .withPassword(redisConfig.getPassword()).build();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(redisConfig.getMaxConnect());
        poolConfig.setMaxIdle(redisConfig.getMaxIdle());
        poolConfig.setMaxWaitMillis(redisConfig.getMaxWaitMillis());
        poolConfig.setTestOnBorrow(true);
        RedisClient redisClient = RedisClient.create(redisUri);
        instance = ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, poolConfig);
    }

    private static GenericObjectPool<StatefulRedisConnection<String, String>> getPoolInstance()
    {
        return instance;
    }

    /**
     * statefull redis connection.
     *
     * @return
     */
    public static StatefulRedisConnection<String, String> getConnection() throws BaseException
    {
        try
        {
            return getPoolInstance().borrowObject();
        }
        catch (Exception e)
        {
            log.error("can not get redis connection.");
            throw new BaseException("can not get redis connection.");
        }
    }

//    static class RedisPoolUtilHandler
//    {
//
//        static GenericObjectPool<StatefulRedisConnection<String, String>> instance;
//
//        private RedisPoolUtilHandler()
//        {
//        }
//
//        static
//        {
//            RedisURI redisUri = RedisURI.builder().withHost(redisConfig.getIp()).withPort(redisConfig.getPort())
//                .withPassword(redisConfig.getPassword()).build();
//            GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
//            poolConfig.setMaxTotal(redisConfig.getMaxTotal());
//            poolConfig.setMaxIdle(redisConfig.getMaxIdle());
//            poolConfig.setMaxWaitMillis(redisConfig.getMaxWaitMillis());
//            poolConfig.setTestOnBorrow(true);
//            RedisClient redisClient = RedisClient.create(redisUri);
//            instance = ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, poolConfig);
//        }
//    }
}
