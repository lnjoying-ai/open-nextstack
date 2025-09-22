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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.redis.op.SetOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.foundation.vertx.VertxUtils;
import org.apache.servicecomb.foundation.vertx.client.ClientPoolFactory;
import org.apache.servicecomb.foundation.vertx.client.ClientPoolManager;
import org.apache.servicecomb.foundation.vertx.client.ClientVerticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RedisClientUtils
{
    private static ClientPoolManager<RedisClient> clientMgr;
    static boolean sync = true;
    private static Logger LOGGER = LogManager.getLogger();

    public static void init(String vertxName, String redisHost, int redisPort, String redisPassword, int redisClientCount) throws InterruptedException
    {
        Vertx vertx = VertxUtils.getOrCreateVertxByName(vertxName, null);
        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisHost)
                .setPort(redisPort)
                .setAuth(redisPassword);

        ClientPoolFactory<RedisClient> factory = (ctx) ->
        {
            return RedisClient.create(vertx, redisOptions);
        };

        clientMgr = new ClientPoolManager<>(vertx, factory);

        DeploymentOptions deployOptions = VertxUtils.createClientDeployOptions(clientMgr,redisClientCount);
        VertxUtils.blockDeploy(vertx, ClientVerticle.class, deployOptions);
    }


    public static void set(BufferIO value, int extime)
    {
        SetOptions options = new SetOptions().setEX(extime);
        RedisSession session = getRedisSession();
        Buffer buffer =  Buffer.buffer();
        value.writeToBuffer(buffer);
        CompletableFuture<Void> future = new CompletableFuture<>();
        session.setBinary(value.getKey(), buffer, options, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void set(BufferIO value)
    {
        RedisSession session = getRedisSession();
        Buffer buffer =  Buffer.buffer();
        value.writeToBuffer(buffer);
        CompletableFuture<Void> future = new CompletableFuture<>();
        session.setBinary(value.getKey(), buffer, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void set(String key, String value, int extime)
    {
        SetOptions options = new SetOptions().setEX(extime);
        RedisSession session = getRedisSession();
        CompletableFuture<String> future = new CompletableFuture<>();
        session.set(key, value, options,future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void set(String mkey, String skey, String value, int extime)
    {
        String key = mkey+skey;
        SetOptions options = new SetOptions().setEX(extime);
        RedisSession session = getRedisSession();
        CompletableFuture<String> future = new CompletableFuture<>();
        session.set(key, value, options,future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void oset(String mkey, String skey, Object value)
    {
        String key=mkey+skey;
        Buffer buffer = SerializeUtil.serialize(value);
        CompletableFuture<Void> future = new CompletableFuture<>();
        RedisSession session = getRedisSession();
        session.setBinary(key, buffer, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void oset(String mkey, String skey, Object value, int extime)
    {
        SetOptions options = new SetOptions().setEX(extime);
        String key=mkey+skey;
        Buffer buffer = SerializeUtil.serialize(value);
        CompletableFuture<Void> future = new CompletableFuture<>();
        RedisSession session = getRedisSession();
        session.setBinary(key, buffer, options, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static  Object oget(String mkey, String skey)
    {
        String key = mkey + skey;
        Buffer buffer = getBinary(key);
        if (null == buffer)
        {
            return null;
        }

        return  SerializeUtil.unserialize(buffer.getBytes());
    }

    public static void odel(String mkey, String skey)
    {
        String key = mkey + skey;
        delete(key);
    }

    public static long hset(String mkey, String skey, String value)
    {
        CompletableFuture<Long> future = new CompletableFuture<>();
        getRedisSession().hset(mkey, skey, value, future);
        try
        {
            return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static long hset(String mkey, String skey, String value, int extime)
    {
        SetOptions options = new SetOptions().setEX(extime);
        CompletableFuture<Long> future = new CompletableFuture<>();
        getRedisSession().hset(mkey, skey, value, future);
        try {
            return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void hdel(String mkey, String skey)
    {
        try
        {
            getRedisSession().hdel(mkey, skey);
        }
        catch (Exception e)
        {
            LOGGER.error("hdel " + e);
        }
    }

    public static int delete(String key)
    {
        try
        {
            RedisSession session = getRedisSession();
            session.delete(key);
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            LOGGER.error("delete " + e);
        }
        return 0;
    }

    public static String hget(String mkey, String skey)
    {
        CompletableFuture<String> future = new CompletableFuture<>();

        try
        {
            RedisSession session = getRedisSession();
            session.hget(mkey, skey, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            LOGGER.error("hget  " + e);
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static int delete (List<String> keys)
    {
        try
        {
            RedisSession session = getRedisSession();
            session.delete(keys);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    public static long lpush(String mkey, String skey, String value)
    {
        LOGGER.info("push to {}{} value: {}", mkey, skey, value);
        try
        {
            JsonArray nodes = RedisClientUtils.lrangeAll(mkey, skey);
            LOGGER.info("{}{} have value: {}",mkey, skey, nodes);
            if (null  != nodes  && nodes.size() >= 1)
            {
                for(Object o : nodes)
                {
                    if (value.equals(o.toString()))
                    {
                        LOGGER.info("{}{} !!!!!have value: {}", mkey, skey, nodes);
                        return nodes.size();
                    }
                }
            }
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().lpush(mkey+skey, value, future);
            return future.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.error("lpush " + e);
            return -1;
        }
    }

    public static long rpush(String mkey, String skey, String value)
    {
        LOGGER.info("push to {}{} value: {}", mkey, skey, value);
        try
        {
            JsonArray nodes = RedisClientUtils.lrangeAll(mkey, skey);
            LOGGER.info("{}{} have value: {}",mkey, skey, nodes);
            if (null  != nodes  && nodes.size() >= 1)
            {
                for(Object o : nodes)
                {
                    if (value.equals(o.toString()))
                    {
                        LOGGER.info("{}{} !!!!!have value: {}",mkey, skey, nodes);
                        return nodes.size();
                    }
                }
            }
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().rpush(mkey+skey, value, future);
            return future.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.error("lpush " + e);
            return -1;
        }
    }

    public static Long lrem(String mkey, String skey, String value)
    {
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().lrem(mkey+skey, value, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("lrem " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("lrem " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("lrem " + e);
        }
        return null;
    }

    public static Long scard(String mkey, String skey)
    {
        return scard(mkey+skey);
    }

    public static Long scard(String key)
    {
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().scard(key, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("scard " + e.getMessage());
        }
        catch (ExecutionException e)
        {
            LOGGER.error("scard " + e.getMessage());
        }
        catch (Exception e)
        {
            LOGGER.error("scard " + e.getMessage());
        }
        return null;
    }

    public static long sadd(String mkey, String skey, String value)
    {
        LOGGER.info("set to {}{} value: {}", mkey, skey, value);
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().sadd(mkey+skey, value, future);
            return future.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.error("sadd error " + e.getMessage());
            return -1;
        }
    }

    public static long sismember(String mkey, String skey, String value)
    {
        LOGGER.info("judge setmember key: {}{} value: {}", mkey, skey, value);
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().sismember(mkey+skey, value, future);
            return future.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.error("sismember " + e.getMessage());
            return -1;
        }
    }

    public static long srem(String mkey, String skey, String value)
    {
        LOGGER.info("rem  key: {}{} value: {}", mkey, skey, value);
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().srem(mkey+skey, value, future);
            return future.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.error("srem  " + e);
            return -1;
        }
    }

    public static Long llen(String mkey, String skey)
    {
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().llen(mkey+skey, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
//            e.printStackTrace();
            LOGGER.error("len " + e);
        }
        catch (ExecutionException e)
        {
//            e.printStackTrace();
            LOGGER.error("len " + e);
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            LOGGER.error("len " + e);
        }
        return null;
    }

    public static JsonArray lrange(String mkey, String skey, Long start, Long end)
    {
        try
        {
            CompletableFuture<JsonArray> future = new CompletableFuture<>();
            getRedisSession().lrange(mkey+skey, start, end, future);
            return future.get();
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            LOGGER.error("lrange " + e);
            return null;
        }
    }

    public static JsonArray lrangeAll(String mkey, String skey)
    {
        try
        {
            Long al = llen(mkey,skey);
            if (al==null || al < 1)
            {
                return null;
            }
            CompletableFuture<JsonArray> future = new CompletableFuture<>();
            getRedisSession().lrange(mkey+skey, 0L, al, future);
            return future.get();
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            LOGGER.error("lrangeAll " + e);
            return null;
        }
        finally {
        }
    }

    public static JsonArray smembers(String mkey, String skey)
    {
        try
        {
            CompletableFuture<JsonArray> future = new CompletableFuture<>();
            getRedisSession().smembers(mkey+skey,  future);
            return future.get();
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            LOGGER.error("smembers " + e.getMessage());
            return null;
        }
        finally {
        }
    }

    public static Long sdiffstore(String key, String key1, List<String> keyn)
    {
        try
        {
            CompletableFuture<Long> future = new CompletableFuture<>();
            getRedisSession().sdiffstore(key, key1, keyn, future);
            return future.get();
        }
        catch (Exception e)
        {
            LOGGER.error("sdiffstore " + e.getMessage());
            return null;
        }
        finally {
        }
    }

    public static JsonArray sdiff(String key1, List<String> keyn)
    {
        try
        {
            CompletableFuture<JsonArray> future = new CompletableFuture<>();
            getRedisSession().sdiff(key1, keyn, future);
            return future.get();
        }
        catch (Exception e)
        {
            LOGGER.error("sdiff " + e.getMessage());
            return null;
        }
        finally {
        }
    }


    public static void lpushMany(String mkey, String skey, List<String> value)
    {
        try
        {
            getRedisSession().lpushMany(mkey+skey, value);
        }
        catch (Exception e)
        {
            LOGGER.error("lpushMany " + e);
        }
    }

    public static String lpop(String mkey, String skey)
    {
        CompletableFuture<String> future = new CompletableFuture<>();

        try
        {
            RedisSession session = getRedisSession();
            session.lpop(mkey+skey, future);

            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("lpop " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("lpop " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("lpop " + e);
        }
        return null;
    }

    public static String spop(String mkey, String skey)
    {
        CompletableFuture<String> future = new CompletableFuture<>();

        try
        {
            RedisSession session = getRedisSession();
            session.spop(mkey+skey, future);

            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("spop " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("spop " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("spop " + e);
        }
        return null;
    }

    public static String spop(String key)
    {
        CompletableFuture<String> future = new CompletableFuture<>();

        try
        {
            RedisSession session = getRedisSession();
            session.spop(key, future);

            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("spop " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("spop " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("spop " + e);
        }
        return null;
    }

    public static Buffer getBinary(String key)
    {
        CompletableFuture<Buffer> future = new CompletableFuture<>();


        try
        {
            RedisSession session = getRedisSession();
            session.getBinary(key, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("getBinary " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("getBinary " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("getBinary " + e);
        }
        return null;
    }

    public static JsonArray keys(String master)
    {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();


        try
        {
            RedisSession session = getRedisSession();
            session.keys(master, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("getBinary " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("getBinary " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("getBinary " + e);
        }
        return null;
    }

    public static void get(String key, BufferIO bufferIO)
    {
        bufferIO.readFromBuffer(getBinary(key));
    }

    public static String get(String mkey,String skey)
    {
        String key = mkey + skey;
        return get(key);
    }

    public static String get(String key)
    {
        CompletableFuture<String> future = new CompletableFuture<>();


        try
        {
            RedisSession session = getRedisSession();
            session.get(key, future);
            return future.get();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("get " + e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("get " + e);
        }
        catch (Exception e)
        {
            LOGGER.error("get " + e);
        }
        return null;
    }


    private static RedisSession getRedisSession()
    {
        RedisClient redisClient = clientMgr.findClientPool(sync);
        RedisSession session = new RedisSession(redisClient);
        return session;
    }


    public static void main(String[] args) throws Exception
    {
        TestCase testCase = new TestCase("111131", 123, 9999999);
//        Buffer buffer = SerializeUtil.serialize(testCase);
//        byte [] b = buffer.getBytes();
//        TestCase testCase1  = (TestCase) SerializeUtil.unserialize(buffer.getBytes());
//        System.out.println(testCase1);
        testCase.setTJobType(TJobType.OFFSET);

        RedisClientUtils.init("momo", "127.0.0.1",6379, "hello", 10);
        JsonArray a11 = RedisClientUtils.keys("*");
//        Object object = RedisClientUtils.oget("scene_2_analysis_ret","515B6CA74B79E39F2FF752E4CFD3EBBFB96021C5");
//        JsonArray jsonArray = RedisClientUtils.keys("scene_2_analysis_ret*");
//        CompletableFuture<Integer> ff = (CompletableFuture<Integer> )RedisClientUtils.oget("t981", "");
        System.out.println(RedisClientUtils.sismember("11","","88"));
        System.out.println(RedisClientUtils.sadd("11","","88"));
        System.out.println(RedisClientUtils.sismember("11","","88"));
       while (RedisClientUtils.spop("1111","2222") != null);
        RedisClientUtils.oset("t1", "t2", testCase);
        TestCase testCase1 = (TestCase)RedisClientUtils.oget("t1","t2");
        RedisClientUtils.hset("12334","vvv","ffff");
        Long r1 = RedisClientUtils.lpush("hahad","vvvd","11110");
        Long r2 = RedisClientUtils.lpush("hahad","vvvd","11111");
        Long r3 = RedisClientUtils.lpush("hahae","vvvd","11111");
        JsonArray ss = RedisClientUtils.keys("haha*");
        List<String> as = new ArrayList<>();
        as.add("1");
        as.add("2");
        RedisClientUtils.oset("hhh","jjj",as);
        List<String> bs = (List<String>) RedisClientUtils.oget("hhh","jjj");
        JsonArray ja = RedisClientUtils.lrangeAll("aa","");
        ja.forEach(a ->{
            String b = a.toString();
            System.out.println(b);
        });
        System.out.println(ja);
        RedisClientUtils.lrem("aa","","aa3");

        ja = RedisClientUtils.lrangeAll("aa","");
        String vv = ja.getString(0);
        ja.forEach(a ->{
            String b = a.toString();
            System.out.println(b);
        });
        System.out.println(ja);

        ja = RedisClientUtils.lrange("aaq","",0L,1L);
        if (ja.isEmpty())
        {
            System.out.println("empty");
        }
        ja.forEach(a ->{
            String b = a.toString();
            System.out.println(b);
        });
        System.out.println(ja);
    }
}
