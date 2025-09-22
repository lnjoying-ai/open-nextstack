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

import io.vertx.core.buffer.Buffer;
import lombok.Data;

import java.io.Serializable;
import java.nio.charset.Charset;

//implements BufferIO
@Data
public class TestCase  implements Serializable
{
    private String id;
    private int num;
    private long age;
    private TJobType tJobType;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public TestCase(String id, int num, long age)
    {
        this.id  = id;
        this.num = num;
        this.age = age;
    }
    public TestCase() {};

//    @Override
    public void writeToBuffer(Buffer buff)
    {
        byte[] bytes = this.id.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        buff.appendInt(num);
        buff.appendLong(age);
    }

//    @Override
    public int readFromBuffer(Buffer buffer)
    {
        if (buffer == null)
        {
            return 0;
        }
        int pos = 0;
        int len = buffer.getInt(pos);
        pos += 4;
        byte[] bytes = buffer.getBytes(pos, pos + len);
        pos += len;
        id = new String(bytes, UTF8);
        num = buffer.getInt(pos);
        pos += 4;
        age = buffer.getLong(pos);
        pos +=8;
        return pos;
    }

    public String toString()
    {
        return new StringBuilder(100).append("id:").append(id).append(" num:").append(num).append(" age:").append(age).toString();
    }

    public String getKey() {return  id;}
}
