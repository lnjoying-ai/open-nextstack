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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.SetOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RedisSession
{
    RedisClient redis;

    private static Logger LOGGER = LogManager.getLogger();


    public RedisSession(RedisClient redis)
    {
        this.redis = redis;
    }

    public void delete(String key)
    {
        redis.del(key, res->{
            if (res.succeeded())
            {
                LOGGER.debug("success to delete key: {} ", key);
            }
            else
            {
                LOGGER.debug("failed to delete key: {} ", key);
            }
        });
    }

    public void delete(List<String> keys)
    {
        redis.delMany(keys, res->{
            if (res.succeeded())
            {
                LOGGER.debug("success to delete key: {} ", keys.toString());
            }
            else
            {
                LOGGER.debug("failed to delete key: {} ", keys.toString());
            }
        });
    }

    public void set(String key, String value, SetOptions options,CompletableFuture<String> strFuture)
    {
        redis.setWithOptions(key, value, options, res -> {
            if (res.succeeded())
            {
                LOGGER.info("success to set key: {} data: {}", key, value);
                strFuture.complete(res.result());
            }
            else
            {
                LOGGER.info("failed to set key: {} data: {}", key, value);
            }
        });
    }

    public void get( String key, CompletableFuture<String> strFuture)
    {
        redis.get(key, res -> {
            if (res.succeeded())
            {
                LOGGER.info(res.result());
                strFuture.complete(res.result());
                return;
            }

            strFuture.completeExceptionally(res.cause());
        });
    }

    public void setBinary(String key, Buffer buffer, SetOptions options,  CompletableFuture<Void> future)
    {
        redis.setBinaryWithOptions(key, buffer, options, res->{
            if (res.succeeded())
            {
//                LOGGER.info("success to set key: {} " + key + " data:" + buffer.toString());
                LOGGER.info("success to set key: {} ", key );
                future.complete(res.result());
            }
            else
            {
//                LOGGER.info("failed to set key: {} " + key + " data:" + buffer.toString());
                LOGGER.info("failed to set key: {} ", key );
                future.completeExceptionally(res.cause());
            }
        });
    }

    public void setBinary(String key, Buffer buffer, CompletableFuture<Void> future)
    {
        redis.setBinary(key, buffer, res->{
            if (res.succeeded())
            {
//                LOGGER.info("success to set key: " + key + " data:" + buffer.toString());
                LOGGER.info("success to set key: {}", key);
                future.complete(res.result());
            }
            else
            {
//                LOGGER.info("failed to set key: " + key + " data:" + buffer.toString());
                LOGGER.info("failed to set key: {}", key );
                future.completeExceptionally(res.cause());
            }
        });
    }

    public void hset(String mkey, String skey, String value, CompletableFuture<Long> future)
    {
        redis.hset(mkey, skey, value, res -> {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to set key: {} key: {} value: {}", mkey, skey, value);
            }
            else
            {
                LOGGER.info("failed to set key: {} key: {} value: {}", mkey, skey, value);
                future.completeExceptionally(res.cause());
            }
        });
    }

    public void hset(String mkey, String skey, String value, SetOptions options)
    {
        redis.hset(mkey, skey, value, res -> {
            if (res.succeeded())
            {
                LOGGER.info("success to set key: {} key: {} value: {}", mkey, skey, value);
            }
            else
            {
                LOGGER.info("failed to set key: {} key: {} value: {}", mkey, skey, value);
            }
        });
    }


    public void hget(String mkey, String skey, CompletableFuture<String> strFuture)
    {
        redis.hget(mkey, skey, res -> {
            if (res.succeeded())
            {
                LOGGER.info("{}{}:{}", mkey, skey, res.result());
                strFuture.complete(res.result());
                return;
            }

            strFuture.completeExceptionally(res.cause());
        });
    }

    public void hdel(String mkey, String skey)
    {
        redis.hdel(mkey, skey, res -> {
            if (res.succeeded())
            {
                LOGGER.info("success to set key: {} key: {}", mkey, skey);
            }
            else
            {
                LOGGER.info("failed to set key: {} key: {}", mkey, skey);
            }
        });
    }

    public void getBinary(String key, CompletableFuture<Buffer> bufferFuture)
    {
        redis.getBinary(key, res ->
                        {
                            if (res.succeeded())
                            {
//                                LOGGER.info(key + ":" + res.result());
                                LOGGER.info(key);
                                bufferFuture.complete(res.result());
                                return;
                            }

                            bufferFuture.completeExceptionally(res.cause());
                        });
    }

    public synchronized void lpush(String key, String value, CompletableFuture<Long> future)
    {
        redis.lpush(key, value, res ->
        {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to set key: {} value: {}{}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to set key: {} value: {}", key, value);
            }
        });
    }

    public synchronized void rpush(String key, String value, CompletableFuture<Long> future)
    {
        redis.rpush(key, value, res ->
        {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to set key: {} value: {}{}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to set key: {} value: {}", key, value);
            }
        });
    }

    public synchronized void lpushx(String key, String value, CompletableFuture<Long> future)
    {
        redis.lpushx(key, value, res ->
        {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to set key: {} value: {}{}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to set key: {} value: {}", key, value);
            }
        });
    }

    public void sadd(String key, String value, CompletableFuture<Long> future)
    {
        redis.sadd(key, value, res ->
        {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to set key: {} value: {}{}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to set key: {} value: {}",  key, value);
            }
        });
    }

    public void sismember(String key, String value, CompletableFuture<Long> future)
    {
        redis.sismember(key, value, res ->
        {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to get key: {} value: {}{}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to get key: {} value: {}", key, value);
            }
        });
    }

    public void srem(String key, String value, CompletableFuture<Long> future)
    {
        redis.srem(key, value, res ->
        {
            if (res.succeeded())
            {
                future.complete(res.result());
                LOGGER.info("success to rem key: {} value: {} {}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to rem key: {} value: {}", key, value);
            }
        });
    }

    public void lrem(String key, String value, CompletableFuture<Long> strFuture)
    {
        redis.lrem(key, 0, value, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("success to rm key: {} value: {} rm count: {}", key, value, res.result());
            }
            else
            {
                LOGGER.info("failed to set key: {} value: {}", key, value);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void scard(String key, CompletableFuture<Long> strFuture)
    {
        redis.scard(key, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("success to scard key: {} count: {}", key, res.result());
            }
            else
            {
                LOGGER.info("failed to scard key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void llen(String key, CompletableFuture<Long> strFuture)
    {
        redis.llen(key, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("the key : {} len: {}", key, res.result());
            }
            else
            {
                LOGGER.info("failed to get len of  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void lrange(String key, Long start, Long end, CompletableFuture<JsonArray> strFuture)
    {
        redis.lrange(key, start, end, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("get the arrays for the key : {} array: {}", key, res.result().size());
            }
            else
            {
                LOGGER.info("failed to get len of  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void smembers(String key,  CompletableFuture<JsonArray> strFuture)
    {
        redis.smembers(key, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("get the arrays for the key : {} array: {}", key, res.result().size());
            }
            else
            {
                LOGGER.info("failed to get len of  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void sdiffstore(String key,  String key1, List<String> keyn, CompletableFuture<Long> strFuture)
    {
        redis.sdiffstore(key, key1, keyn, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("get the sdiffstore for the key : {} array: {}", key, res.result());
            }
            else
            {
                LOGGER.info("failed to get len of  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void sdiff(String key1, List<String> keyn, CompletableFuture<JsonArray> strFuture)
    {
        redis.sdiff(key1, keyn, res ->
        {
            if (res.succeeded())
            {
                strFuture.complete(res.result());
                LOGGER.info("get the sdiff for the key : {} array: {}", key1, res.result());
            }
            else
            {
                LOGGER.info("failed to sdiff for the key: {}", key1);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void lpushMany(String key, List<String> value)
    {
        redis.lpushMany(key, value, res ->
        {
            if (res.succeeded())
            {
                LOGGER.info("success to set key: {} value: {}", key, value);
            }
            else
            {
                LOGGER.info("failed to set key: {} value: {}", key, value);
            }
        });
    }

    public void lpop(String key, CompletableFuture<String> strFuture)
    {
        redis.lpop(key, res ->
        {
            if (res.succeeded())
            {
                LOGGER.info( "{}:{}", key, res.result());
                strFuture.complete(res.result());
            }
            else
            {
                LOGGER.info("failed to lpop  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }

    public void spop(String key, CompletableFuture<String> strFuture)
    {
        redis.spop(key, res ->
        {
            if (res.succeeded())
            {
                LOGGER.info( "{}:{}", key, res.result());
                strFuture.complete(res.result());
            }
            else
            {
                LOGGER.info("failed to spop  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }


    public void keys(String key, CompletableFuture<JsonArray> strFuture)
    {
        redis.keys(key, res ->
        {
            if (res.succeeded())
            {
                LOGGER.info("{}:{}", key, res.result());
                strFuture.complete(res.result());
            }
            else
            {
                LOGGER.info("failed to lpop  key: {}", key);
                strFuture.completeExceptionally(res.cause());
            }
        });
    }
}
