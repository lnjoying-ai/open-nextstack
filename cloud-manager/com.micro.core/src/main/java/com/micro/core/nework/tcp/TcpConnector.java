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

import com.micro.core.nework.entity.NetEntity;
import com.micro.core.nework.tcp.method.ITcpAddPipeHandler;
import com.micro.core.nework.tcp.method.ITcpConnectListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.netty.channel.*;

public class TcpConnector extends AbstractITcpProcessor
{
    private static Logger LOGGER = LogManager.getLogger();
    private Bootstrap bootstrap;
    private  EventLoopGroup workerGroup;


    public TcpConnector(ITcpAddPipeHandler tcpAddPipeHandler)
    {
        bootstrap = new Bootstrap();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap.group(workerGroup)
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.TCP_NODELAY, true)
                 .option(ChannelOption.SO_REUSEADDR, true)
                 .option(ChannelOption.SO_RCVBUF, 100 * 1024)
                 .option(ChannelOption.SO_SNDBUF, 100 * 1024)
                 .option(EpollChannelOption.SO_REUSEPORT, true)
                 .handler(initPipeLine(tcpAddPipeHandler));
    }


    public ChannelFuture connect(NetEntity entity, ITcpConnectListener listener) throws Exception
    {
        ChannelFuture future = bootstrap.connect(entity.getHost(), entity.getPort());

        future.addListener(new ChannelFutureListener()
        {
            public void operationComplete(ChannelFuture f) throws Exception
            {
                if (f.isSuccess())
                {
                    LOGGER.info("connect success. Remote: {} Local: {}",
                            f.channel().remoteAddress().toString(), f.channel().localAddress().toString());
                    addChannel(f.channel());
                    listener.addlistener(f.channel());
                }
            }
        });

        try
        {
            future.sync();
        }
        catch (InterruptedException e)
        {

            e.printStackTrace();
        }
        return future;
    }

    private ChannelFutureListener channelStopListener = new ChannelFutureListener()
    {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception
        {
            LOGGER.info("CHANNEL_CLOSED");
        }
    };

    private ChannelInitializer<SocketChannel> initPipeLine(ITcpAddPipeHandler tcpAddPipeHandler)
    {

        return new ChannelInitializer<SocketChannel>()
        {

            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception
            {
                ChannelPipeline pipeline = socketChannel.pipeline();
                tcpAddPipeHandler.addHandler(pipeline);
                addChannel(socketChannel);
            }
        };
    }
}
