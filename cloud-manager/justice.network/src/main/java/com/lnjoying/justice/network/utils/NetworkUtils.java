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

package com.lnjoying.justice.network.utils;

import cn.hutool.core.util.StrUtil;
import org.jruby.ext.socket.SubnetUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.hutool.core.text.CharSequenceUtil.isNumeric;

public class NetworkUtils
{
    public static boolean isValidCidrIp(String cidrIp)
    {
        boolean isValid = true;
        try
        {
            new SubnetUtils((cidrIp));
        }
        catch (IllegalArgumentException e)
        {
            isValid = false;
        }
        return isValid;
    }

    public static boolean isValidSubnetCidr(String cidrIp)
    {
//        boolean isValid = true;
        try
        {
            SubnetUtils subnetUtils = new SubnetUtils(cidrIp);
            return subnetUtils.getInfo().getNetworkAddress().equals(subnetUtils.getInfo().getAddress());
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    public static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    public static boolean isValidInet4Address(String ip)
    {
        if (ip == null) {
            return false;
        }

        Matcher matcher = IPv4_PATTERN.matcher(ip);

        return matcher.matches();
    }

    public static int maskToPrefix(String netmask)
    {
        String[] data = netmask.split("\\.");
        int len = 0;
        for (String n : data) {
            len += (8 - Math.log(256-Integer.parseInt(n)) / Math.log(2));
        }
        return len;
    }

    public static String getCidr(String networkAddress, String netmask)
    {
        StringBuilder cidr = new StringBuilder(networkAddress);
        cidr.append("/");
        cidr.append(maskToPrefix(netmask));
        return cidr.toString();
    }

    //ip address to long
    public static long ipToLong(String ipAddress)
    {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            result |= ip << (i * 8);
        }
        return result;
    }

    //long to ip address
    public static String longToIp(long i)
    {
        return ((i >> 24) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                (i & 0xFF);
    }

    //check string is ip address
    public static boolean isIpV4Address(String ipAddress)
    {
        if (ipAddress.isEmpty())
        {
            return false;
        }
        ipAddress = ipAddress.trim();
        try
        {
            String[] ipAddressInArray = ipAddress.split("\\.");
            if (ipAddressInArray.length != 4)
            {
                return false;
            }
            for (String str : ipAddressInArray)
            {
                int i = Integer.parseInt(str);
                if ((i < 0) || (i > 255))
                {
                    return false;
                }
            }
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    //check subnet overlap
    public static boolean isCidrOverlap(String subnetCidr, String otherCidr)
    {
        try
        {
            SubnetUtils subnet = new SubnetUtils(subnetCidr);
            SubnetUtils.SubnetInfo subnetInfo = subnet.getInfo();
            long subnetInfoMin = ipToLong(subnetInfo.getLowAddress());
            long subnetInfoMax = ipToLong(subnetInfo.getHighAddress());

            SubnetUtils otherSubnet = new SubnetUtils(otherCidr);
            SubnetUtils.SubnetInfo otherSubnetInfo = otherSubnet.getInfo();
            long otherSubnetInfoMin = ipToLong(otherSubnetInfo.getLowAddress());
            long otherSubnetInfoMax = ipToLong(otherSubnetInfo.getHighAddress());

            if (subnetInfoMin <= otherSubnetInfoMax && otherSubnetInfoMin <= subnetInfoMax) {
                return true;
            }

            return false;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }

    }

    public static boolean isOverlap(long[] range, long[] otherRange) {
        return range[0]<=otherRange[1] && otherRange[0] <= range[1] ;
    }

    //check subnet overlap
    public static boolean isCidrOverlap(String subnetCidr, List<String> otherCidrs)
    {
        try
        {
            for (String otherCidr : otherCidrs)
            {
                if (isCidrOverlap(subnetCidr, otherCidr))
                {
                    return true;
                }
            }

            return false;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }

    }

    public static boolean isValidatePort(String port)
    {
        if (isNumeric(port))
        {
            int portInt = Integer.parseInt(port);
            return portInt >= 0 && portInt <= 65535;
        }
        return false;
    }

    /** * 判断是否为合法IP:Port * @return the boolean*/
    public static boolean isValidIpPort(String ipAddress)
    {
        if (!StrUtil.isEmpty(ipAddress))
        {
            String[] ipPort = ipAddress.split(":");
            if (ipPort.length == 2)
            {
                String ip = ipPort[0];
                String port = ipPort[1];
                return isIpV4Address(ip) && isValidatePort(port);
            }
        }
        return false;
    }
}
