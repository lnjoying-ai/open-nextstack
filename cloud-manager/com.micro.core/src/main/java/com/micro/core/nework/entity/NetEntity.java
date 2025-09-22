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

package com.micro.core.nework.entity;

public class NetEntity
{
    static final long serialVersionUID = 42L;
    private String Id;
    private String Desc;
    public enum ChannelType {CLIENT,SERVER,DUPLEX};
    public NetEntity()
    {

    }
    public NetEntity(String host, int port, ChannelType channelType)
    {
        this.host = host;
        this.port = port;
        this.channelType = channelType;
    }

    private ChannelType channelType;
    private String host;
    private int port;

    public void setHost(String host) {this.host = host;}
    public String getHost() {return this.host;}

    public void setPort(Integer port) {this.port = port;}
    public Integer getPort() {return this.port;}

    public void setChannelType(ChannelType chType) {this.channelType = chType;}
    public ChannelType getChannelType() {return this.channelType;}
}
