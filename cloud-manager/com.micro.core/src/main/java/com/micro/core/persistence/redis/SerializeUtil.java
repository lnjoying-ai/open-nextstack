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

import com.micro.core.common.Utils;
import io.vertx.core.buffer.Buffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializeUtil
{
    public static Buffer serialize(Object object)
    {
        if (object == null)
        {
            return null;
        }

        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try
        {
            //serialize
            Buffer buffer =  Buffer.buffer();

            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            buffer.appendBytes(bytes);
            return buffer;
        }
        catch (Exception e)
        {
            return null;

        }
    }


    public static String serializeStr(Object object)
    {
        if (object == null)
        {
            return null;
        }

        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try
        {
            //serialize
            Buffer buffer =  Buffer.buffer();

            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return Utils.byteToHexString(bytes);
        }
        catch (Exception e)
        {
            return null;

        }
    }

    public static Object unserializeStr(String str)
    {
        ByteArrayInputStream bais = null;
        try
        {
            byte [] bytes = Utils.hexToByteArray(str);

            bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        }
        catch (Exception e)
        {

        }
        return null;
    }

    public static Object unserialize( byte[] bytes)
    {
        ByteArrayInputStream bais = null;
        try
        {
            bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        }
        catch (Exception e)
        {

        }
        return null;
    }
}
