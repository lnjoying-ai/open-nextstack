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

package com.micro.core.common;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChannelList
{
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
//    private ReentrantLock reentrantLock = new ReentrantLock();

    private List<Channel> collection = new ArrayList<Channel>();

    public Channel[] getall()
    {
        return collection.toArray(new Channel[0]);
    }

    public Channel fetch()
    {

        try
        {
//            lock.readLock().lock();
//            reentrantLock.lock();
            readLock.lock();
            int size = collection.size();
            if (size == 0)
            {
                return null;
            }

            int idx = (int) getNextAtomicValue(indexSeq, Limited);
            Channel ret = collection.get(idx % size);
            // 超过65535归0
            return ret;
        }
        finally
        {
            readLock.unlock();
//            lock.readLock().unlock();
//            reentrantLock.unlock();
        }
    }

    public boolean add(Channel ele)
    {

        boolean r = false;
        try
        {
            writeLock.lock();
//            lock.writeLock().lock();
//            reentrantLock.lock();
            r = collection.add(ele);
        }
        finally
        {
            writeLock.unlock();
//            lock.writeLock().unlock();
//            reentrantLock.unlock();
        }
        return r;
    }

    public boolean remove(Channel ele)
    {

        boolean r = false;
        try
        {
            writeLock.lock();
//            lock.writeLock().lock();
//            reentrantLock.lock();
            r = collection.remove(ele);
        }
        finally
        {
            writeLock.unlock();
//            lock.writeLock().unlock();
//            reentrantLock.unlock();
        }
        return r;
    }

    public int  getCount()
    {
        writeLock.lock();
//        lock.writeLock().lock();
//        reentrantLock.lock();
        try
        {
            return collection.size();
        }
        finally
        {
            writeLock.unlock();
//            lock.writeLock().unlock();
//            reentrantLock.unlock();
        }

    }

    private  long getNextAtomicValue(AtomicLong atomicObj,long limited)
    {
        long ret = atomicObj.getAndIncrement();

        if (ret > limited)
        {
            synchronized (atomicObj)
            {
                //双重判断，只能有一个线程更新值
                if (atomicObj.get() > limited)
                {
                    atomicObj.set(0);
                    return 0;
                }
                else
                {
                    return atomicObj.getAndIncrement();
                }
            }
        }
        else
        {
            return ret;
        }
    }

    private final static long Limited = 65535L;
    private AtomicLong indexSeq = new AtomicLong();
}
