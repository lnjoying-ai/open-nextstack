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

package com.micro.core.process.service;

import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.process.processor.NewProcessorInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * base class for process service
 */
public class ProcessStrategy
{
    private static Logger LOGGER = LogManager.getLogger();
    private ExecutorService processorPool;
    private int threadNum = 5;
    private int maxQueueSize = 10000;
    BlockingQueue<Object> queue = null;
    private List<AbstractRunnableProcessor> processors = null;

    public ProcessStrategy(String desc, int threadNum, int maxQueueSize)
    {
        this.threadNum = threadNum;
        this.maxQueueSize = maxQueueSize;
        processors = new ArrayList<AbstractRunnableProcessor>();
        if (maxQueueSize > 0)
        {
            queue = new LinkedBlockingQueue<>(maxQueueSize);
        }

        processorPool = Executors.newFixedThreadPool(threadNum, new ProcessorsThreadFactory(desc));
    };


    public void start(NewProcessorInterface processorInterface, int num)
    {
        for (int i=0; i< num; i++)
        {
            start(processorInterface);
        }
    }

    /**
     *
     * @param processorInterface: impl process for task, which will called
     */
    public void start(NewProcessorInterface processorInterface)
    {
        AbstractRunnableProcessor processor = processorInterface.newProcessor();
        if (queue != null)
        {
            processor.setBlockQueue(queue);
        }

        processor.start();
        processorPool.execute(processor);
        processors.add(processor);

        LOGGER.info("Started processing thread with queue size of {} and {} threads.", maxQueueSize, threadNum);
    }

    public void stop()
    {

        processorPool.shutdownNow();

        processors.forEach(processor -> processor.stop());
    }

    public void sendMessage(Object obj)
    {
        try
        {
            queue.put(obj);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public int getTaskQueueLength()
    {
        return queue.size();
    }

}
