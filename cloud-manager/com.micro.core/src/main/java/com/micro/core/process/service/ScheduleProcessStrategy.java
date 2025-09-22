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
import java.util.concurrent.*;

/**
 * base class for process at a period
 */
public class ScheduleProcessStrategy
{
    private static Logger LOGGER = LogManager.getLogger();
    private ScheduledExecutorService scheduExecPool;
    private int threadNum = 5;
    private int start = 0;
    private int period = 1;
    private List<AbstractRunnableProcessor> processors = null;
    private List<ScheduledFuture<?>> scheduledFutures;


    public ScheduleProcessStrategy(String desc, int threadNum)
    {
        this.threadNum = threadNum;
        processors = new ArrayList<AbstractRunnableProcessor>();
        scheduExecPool = Executors.newScheduledThreadPool(2, new ProcessorsThreadFactory(desc));
        scheduledFutures = new ArrayList<>();
    };


    public void start(NewProcessorInterface processorInterface, int delay, int period, int num, BlockingQueue<Object> queue)
    {
        if (period < 0 || delay < 0 || processorInterface == null)
        {
            LOGGER.error("input params error. pls check");
            return;
        }


        for (int i=0; i< num; i++)
        {
            start(processorInterface, delay, period, queue);
        }
    }


    /**
     *
     * @param processorInterface: impl process for task, which will called
     * @param delay: delay time for exec task
     * @param period: period time
     */
    public void start(NewProcessorInterface processorInterface, int delay, int period, BlockingQueue<Object> queue)
    {
        if (period < 0 || delay < 0 || processorInterface == null)
        {
            LOGGER.error("input params error. pls check");
            return;
        }
        AbstractRunnableProcessor processor = processorInterface.newProcessor();
        if (queue != null)
        {
            processor.setBlockQueue(queue);
        }
        processor.start();
        ScheduledFuture<?> scheduledFuture;
        if (period == 0)
        {
            scheduledFuture = scheduExecPool.schedule(processor, delay, TimeUnit.MILLISECONDS);
        }
        else
        {
            scheduledFuture = scheduExecPool.scheduleAtFixedRate(processor, delay, period, TimeUnit.MILLISECONDS);
        }
        scheduledFutures.add(scheduledFuture);
        processors.add(processor);

        LOGGER.info("Started processing thread");
    }


    public void stop()
    {
        scheduledFutures.forEach(scheduledFuture -> {
            if (scheduledFuture != null && !scheduledFuture.isDone()) {
                scheduledFuture.cancel(true);
            }
        });

        scheduExecPool.shutdownNow();

        processors.forEach(processor -> processor.stop());
    }
}
