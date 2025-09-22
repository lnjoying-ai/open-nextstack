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

package com.micro.core.process.processor;

import java.beans.ExceptionListener;
import java.util.concurrent.BlockingQueue;

public abstract  class AbstractRunnableProcessor implements Runnable, ExceptionListener
{
    private BlockingQueue<Object> queue;
    public AbstractRunnableProcessor()
    {

    }

    public void start()
    {

    }

    public void stop()
    {

    }



    @Override
    public void run()
    {

    }

    @Override
    public void exceptionThrown(Exception e) {

    }

    public void setBlockQueue(BlockingQueue<Object> queue)
    {
        this.queue = queue;
    }

    public BlockingQueue<Object> getBlockQueue()
    {
        return queue;
    }

}
