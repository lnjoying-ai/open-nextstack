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

package com.micro.core.nework.tcp;

import com.micro.core.common.ChannelList;
import com.micro.core.nework.tcp.method.ITcpProcess;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public abstract  class AbstractITcpProcessor implements ITcpProcess
{
    private static Logger LOGGER = LogManager.getLogger();

    private volatile AtomicInteger conCnt = new AtomicInteger();

    private SslContext sslCtx = null;
    private ChannelList channelList = new ChannelList();


    public AbstractITcpProcessor()
    {
    }


    @Override
    public void close(Channel channel) throws Exception
    {
        try
        {
            if (channel.isOpen())
            {
                channel.close().sync();
            }

        }
        catch (InterruptedException e)
        {
            LOGGER.error("close channel Error ", e);
        }
        removeChannel(channel);
    }


    @Override
    public void close() throws Exception
    {
        Channel ch = channelList.fetch();
        while (ch != null)
        {
            close(ch);
            ch = channelList.fetch();
        }
    }

    @Override
    public Channel fetch()
    {
        Channel ch = channelList.fetch();

        if (ch != null && ch.isActive())
        {
            return ch;
        }
        return null;
    }

    public SslContext getSslCtx() {
        return sslCtx;
    }

    @Override
    public int getConnectionNum()
    {
        return channelList.getCount();
    }

    @Override
    public void addChannel(Channel ch)
    {
        channelList.add(ch);
    }

    @Override
    public void removeChannel(Channel ch)
    {
        channelList.remove(ch);
    }

    @Override
    public Channel[] getallChannel() {
        return channelList.getall();
    }
}
