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

package com.micro.core.nework.tcp.method;

import io.netty.channel.Channel;

public interface ITcpProcess
{
    //close channel
    public void close(Channel channel) throws Exception;

    //close all channel
    public void close()throws Exception;

    //fetch a channel from channel pool
    Channel fetch();

    //get connection Num
    int getConnectionNum();

    public Channel[] getallChannel();
    public void addChannel(Channel ch);
    public void removeChannel(Channel ch);
}
