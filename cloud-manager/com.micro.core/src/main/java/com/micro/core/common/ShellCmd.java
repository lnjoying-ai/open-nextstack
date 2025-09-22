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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShellCmd
{
    private static Logger LOGGER = LogManager.getLogger();
    public static String runCmd(String [] cmdArray)
    {
        Process pro = null;
        try
        {
            LOGGER.info("run cmd:" + StringUtils.join(cmdArray, " "));
            pro = Runtime.getRuntime().exec(cmdArray);
//            if (pro.waitFor() != 0)
//            {
//                LOGGER.info("Failed to call shell's command ");
//                return null;
//            }

            boolean bl = pro.waitFor(120, TimeUnit.SECONDS);
            LOGGER.info("run cmd over.result="+bl);
            if (!bl)
            {
                LOGGER.error("run cmd failed");
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            StringBuffer strbr = new StringBuffer();
            String line;
            while ((line = br.readLine())!= null)
            {
                strbr.append(line).append("\n");
            }

            String result = strbr.toString();
            return result;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Process runCmd2(String [] cmdArray)
    {
        Process pro = null;
        try
        {
            LOGGER.info("run cmd:" + StringUtils.join(cmdArray, " "));
            pro = Runtime.getRuntime().exec(cmdArray);
            return pro;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static String waitResult(Process pro)
    {
        try
        {
            boolean bl = pro.waitFor(120, TimeUnit.SECONDS);
            LOGGER.info("run cmd over.result="+bl);
            if (!bl)
            {
                LOGGER.error("run cmd failed");
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            StringBuffer strbr = new StringBuffer();
            String line;
            while (true)
            {
                if (!((line = br.readLine())!= null)) break;
                strbr.append(line).append("\n");

            }

            String result = strbr.toString();
            return result;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) throws Exception
    {
        String batPath =  "C:/source/SRending/winbat/analysis.bat";
        File batFile = new File(batPath);
        boolean batFileExist = batFile.exists();

        String [] cmdArray = {batPath,"\\46f15cfae57b4576b647f78222486cc5\\ffff","hh.max","ddddd"};
        String result = ShellCmd.runCmd(cmdArray);
//        ShellCmd.runCmd2(batPath);
//        String [] ttt = {"/Users/regulus/source/lnjoying_src_git/SRending/shell/getResult.sh","/Users/regulus/maya_src"};
//        String result1 = ShellCmd.runCmd(ttt);
//        System.out.println(result);
    }
}
