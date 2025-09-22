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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Utils
{
    public static String assignUUId()
    {
        return UUID.randomUUID().toString().replace("-","");
    }

    public static  Long getLongUUID() {return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;}

    public static String getRandomStr(Integer txtLength)
    {
        return RandomStringUtils.randomNumeric(txtLength);
    }
    public static Long getRandomNum() { return (long)(1+Math.random()*(1000000000)); }

    public static String formatDate(Long timestamp)
    {
        Date date = new Date(timestamp);
        return formatDate(date);
    }

    public static Date buildDate(Long timestamp)
    {
        return new Date(timestamp);
    }

    public static String getDate(Long timestamp)
    {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public static String formatDate(Date d)
    {
        if (d == null)
        {
            return null;
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(d);
    }

    public static Timestamp parseTime(String inTime)
    {
        String ftime = inTime.replace("(中国标准时间)","");
        ftime = ftime.trim();
        ftime = ftime.replace("GMT 0800","GMT+08:00");

        DateTimeFormatter dTF = DateTimeFormatter.ofPattern("EEE MMM dd uuuu HH:mm:ss z");
        LocalDateTime lds = LocalDateTime.parse(ftime, dTF);
        return Timestamp.valueOf(lds);
    }

    public static Timestamp parseStTime(String inTime)
    {
        DateTimeFormatter dTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime lds = LocalDateTime.parse(inTime, dTF);
        return Timestamp.valueOf(lds);
    }

    public static Timestamp parseSSStTime(String inTime)
    {
        DateTimeFormatter dTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime lds = LocalDateTime.parse(inTime, dTF);
        return Timestamp.valueOf(lds);
    }

    public static String int2String(Integer value)
    {
        if (value != null)
        {
            return value.toString();
        }
        return null;
    }

    public static String long2String(Long value)
    {
        if (value != null)
        {
            return value.toString();
        }
        return null;
    }


    public static String bool2String(Boolean value)
    {
        if (value != null)
        {
            return value.toString();
        }
        return null;
    }

    public static String getEnv(String key)
    {
        Map envMap = System.getenv();
        return (String) envMap.get(key);
    }

    public static Integer getRandomByRange(Integer min, Integer max)
    {
        return new Random().nextInt(max-min)+min;
    }

    public static Integer getRandomByRangeEx(Integer min, Integer max)
    {
        int ret = new Random().nextInt(max-min)+min;
        if (ret > min && ret == max.intValue())
        {
            ret -= 1;
        }

        return ret;
    }

    public static Long getRandomByRangeL(long min, long max)
    {
        return new Random().nextLong();
    }

    public static Map<String, String> getUriParams(String url)
    {
        Map<String, String> map = new HashMap<String, String>();
        int start = url.indexOf("?");
        if (start >= 0) {
            String str = url.substring(start + 1);
            System.out.println(str);
            String[] paramsArr = str.split("&");
            for (String param : paramsArr) {
                String[] temp = param.split("=");
                map.put(temp[0], temp[1]);
            }
        }
        return map;
    }

    public static String getEncoding(String str)
    {
        String encode;

        encode = "UTF-16";
        try
        {
            if(str.equals(new String(str.getBytes(), encode)))
            {
                return encode;
            }
        }
        catch(Exception ex) {}

        encode = "ASCII";
        try
        {
            if(str.equals(new String(str.getBytes(), encode)))
            {
                return "字符串<< " + str + " >>中仅由数字和英文字母组成，无法识别其编码格式";
            }
        }
        catch(Exception ex) {}

        encode = "ISO-8859-1";
        try
        {
            if(str.equals(new String(str.getBytes(), encode)))
            {
                return encode;
            }
        }
        catch(Exception ex) {}

        encode = "GB2312";
        try
        {
            if(str.equals(new String(str.getBytes(), encode)))
            {
                return encode;
            }
        }
        catch(Exception ex) {}

        encode = "UTF-8";
        try
        {
            if(str.equals(new String(str.getBytes(), encode)))
            {
                return encode;
            }
        }
        catch(Exception ex) {}

        /*
         *......待完善
         */

        return "未识别编码格式";
    }


    public static byte[] stringToBytes(String str)
    {
        return str.getBytes();
//        try
//        {
//            // 使用指定的字符集将此字符串编码为byte序列并存到一个byte数组中
//            return str.getBytes();
//        }
//        catch (UnsupportedEncodingException e)
//        {
//            e.printStackTrace();
//        }
//        return null;
    }

    public static String bytesToString(byte[] bs)
    {
        return new String(bs);

//        try
//        {
//            // 通过指定的字符集解码指定的byte数组并构造一个新的字符串
//            return new String(bs);
//        }
//        catch (UnsupportedEncodingException e)
//        {
//            e.printStackTrace();
//        }
//        return null;
    }

    public static String byteToHexString(byte[] byteArray)
    {
        if (byteArray == null || byteArray.length < 1)
        {
            throw new IllegalArgumentException("this byteArray must not be null or empty");
        }

        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++)
        {
            if ((byteArray[i] & 0xff) < 0x10)//0~F前面不零
            {
                hexString.append("0");
            }

            hexString.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return hexString.toString().toLowerCase();
    }

    public static byte[] hexToByteArray(String hexString)
    {
        if (StringUtils.isEmpty(hexString))
        {
            throw new IllegalArgumentException("this hexString must not be empty");
        }


        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++)
        {
            //因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    public static String toStringHex2(String s)
    {
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(
                        i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "utf-8");// UTF-16le:Not
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }

    public static Integer getValue(Integer input)
    {
        return (null == input?0:input);
    }

    public static long getLongValue(Long value)
    {
        return value==null?0:value;
    }

    public static Long getLongValue(Integer value)
    {
        return value == null ? 0L:value.longValue();
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();


//    public static String byteArrToHex(byte[] bytes) {
//        char[] hexChars = new char[bytes.length * 2];
//        for (int j = 0; j < bytes.length; j++) {
//            int v = bytes[j] & 0xFF;
//            hexChars[j * 2] = hexArray[v >>> 4];
//            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//        }
//        return new String(hexChars);
//    }

    public static float div(Integer a, Integer b)
    {
        return (float)a/b;
    }

    public static String getMD5(File file)
    {
        FileInputStream fis = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length = -1;
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            return byteToHexString(md.digest());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static String getSHA(File file)
    {
        FileInputStream fis = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length = -1;
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            return byteToHexString(md.digest());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static String getSHA(String context)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] buffer = context.getBytes();
            int length = buffer.length;
            md.update(buffer, 0, length);

            return byteToHexString(md.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static void zipFile(String compose, ZipOutputStream zos, String name) throws IOException
    {
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(compose))
        {
            byte[] content = compose.getBytes("UTF-8");
            zos.putNextEntry(new ZipEntry(name));
            zos.write(content);
            zos.closeEntry();
        }
    }

    public static String buildStr(String ... var)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i <  var.length; i++)
        {
            stringBuilder.append(var[i]);
        }
        return stringBuilder.toString();
    }

    public static void main(String[] args) throws Exception
    {
        String s = buildStr("s1","_","s2");
        System.out.println(s);
    }
}
