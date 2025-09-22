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
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import io.lettuce.core.api.sync.*;
@Slf4j
public class RedisUtil
{
//    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtil.class);

    private RedisUtil()
    {
    }

    public static void init(RedisConfig redisConfig) throws InterruptedException
    {
        RedisPoolUtil.init(redisConfig);
    }

    public static void set(String key, String value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            commands.set(key, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static void set(String mkey, String skey, String value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            commands.set(mkey + skey, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static void set(String key, String value, int extime)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            commands.set(key, value);
            commands.expire(key, extime);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static void set(String mkey, String skey, String value, int extime)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            commands.set(mkey + skey, value);
            commands.expire(mkey + skey, extime);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static void oset(String mkey, String skey, Object value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            commands.set(mkey + skey,SerializeUtil.serializeStr(value));
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static void oset(String mkey, String skey, Object value, int extime)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            commands.set(mkey + skey,SerializeUtil.serializeStr(value));
            commands.expire(mkey + skey, extime);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }


    public static  Object oget(String mkey, String skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            String value = commands.get(mkey + skey);
            return SerializeUtil.unserializeStr(value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static void delete( String key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            connection.sync().del( key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static void odel(String mkey, String skey)
    {
        String key = mkey + skey;
        delete(key);
    }

    public static void hset(String mkey, String skey, String value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisHashCommands<String, String> commands = connection.sync();
            commands.hset(mkey,skey,value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }


    public static void hdel(String mkey, String ...skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisHashCommands<String, String> commands = connection.sync();
            commands.hdel(mkey,skey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
        }
    }

    public static String hget(String mkey, String skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisHashCommands<String, String> commands = connection.sync();
            return commands.hget(mkey,skey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static void delete (List<String> keys)
    {
        for (String key : keys)
        {
            delete(key);
        }
    }

    public static Long llen(String mkey, String skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisListCommands<String, String> commands = connection.sync();
            return commands.llen(mkey+skey);
//            return commands.get(mkey + skey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
//            return null;
        }
        return 0L;
    }

    public static List<String> lrange(String mkey, String skey, Long start, Long end)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisListCommands<String, String> commands = connection.sync();

            return commands.lrange(mkey+skey, start, end);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static List<String> lrangeAll(String mkey, String skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisListCommands<String, String> commands = connection.sync();
            Long al = llen(mkey, skey);
            if (al == null || al < 1)
            {
                return null;
            }

           return commands.lrange(mkey+skey, 0L, al);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static long lpush(String mkey, String skey, String value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisListCommands<String, String> commands = connection.sync();
            List<String> values = lrangeAll(mkey, skey);
            if (values != null && ! values.isEmpty())
            {
                for (String v : values)
                {
                    if (v.equals(value))
                    {
                        log.info("{}{} !!!!!have value:{}", mkey, skey, value);
                        return values.size();
                    }
                }
            }
            return  commands.lpush(mkey, skey, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1;
        }
    }

    public static Long scard(String mkey, String skey)
    {
        return scard(mkey+skey);
    }

    public static Long scard(String key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.scard(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static long sadd(String mkey, String skey, String ... value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sadd(mkey + skey, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1;
        }
    }

    public static boolean sismember(String mkey, String skey, String value)
    {
        log.info("judge setmember key: {}{} value: {}", mkey, skey, value);
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sismember(mkey+skey, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return false;
        }
    }

    public static long srem(String mkey, String skey, String value)
    {
        log.info("judge setmember key: {}{} value: {}", mkey, skey, value);
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.srem(mkey+skey, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1;
        }
    }


    public static Set<String> smembers(String mkey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.smembers(mkey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static Set<String> smembers(String mkey, String skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.smembers(mkey+skey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static Long sdiffstore(String key, String ... key1)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sdiffstore(key, key1);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1L;
        }
    }

    public static Set<String> sdiff(String ... key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sdiff(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }


    public static long lpushMany(String mkey, String skey, String ...value)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisListCommands<String, String> commands = connection.sync();
            return commands.lpush(mkey + skey, value);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1;
        }
    }

    public static String lpop(String mkey, String skey)
    {

        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisListCommands<String, String> commands = connection.sync();
            return commands.lpop(mkey + skey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static String spop(String mkey, String skey)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();

            return commands.spop(mkey + skey);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static String spop(String key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();

            return commands.spop(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }


    public static List<String> keys(String master)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            return commands.keys(master);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static String get(String mkey,String skey)
    {
        String key = mkey + skey;
        return get(key);
    }

    public static String get(String key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            return commands.get(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static Long exists(String key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisCommands<String, String> commands = connection.sync();
            return commands.exists(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static Long sunionstore(String key, String ... key1)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sunionstore(key, key1);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1L;
        }
    }

    public static Set<String> sunion(String ... key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sunion(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static Long sinterstore(String key, String ... key1)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sinterstore(key, key1);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return -1L;
        }
    }

    public static Set<String> sinter(String ... key)
    {
        try (StatefulRedisConnection<String, String> connection = RedisPoolUtil.getConnection())
        {
            RedisSetCommands<String, String> commands = connection.sync();
            return commands.sinter(key);
        }
        catch (BaseException e)
        {
            log.error("failed to connect redis.");
            return null;
        }
    }

    public static void main(String[] args) throws Exception
    {
        RedisConfig redisConfig = new RedisConfig();
        redisConfig.setHost("127.0.0.1");
        redisConfig.setPort(6379);
        redisConfig.setPassword("hello");
        redisConfig.setMaxConnect(10);
        redisConfig.setMaxIdle(10);
        redisConfig.setMaxWaitMillis(100000);
        RedisUtil.init(redisConfig);
        RedisUtil.set("123","456",5);
        String value = RedisUtil.get("123");
        System.out.println(value);

        TestCase testCase = new TestCase("111131", 123, 9999999);
        RedisUtil.oset("1234","5566", testCase);

        TestCase t2 = (TestCase) RedisUtil.oget("1234", "5566");
        System.out.println(t2);

        RedisUtil.sadd("1295","11122", "668788");
        RedisUtil.sadd("1295","1133","668789");
		RedisUtil.sadd("1295","1133","6687889");
        Set<String> abc = RedisUtil.smembers("1295", "1133");
        System.out.println(abc);
        RedisUtil.srem("1295","1133","6687889");
		System.out.println(RedisUtil.smembers("1295", "1133"));

    }
}
