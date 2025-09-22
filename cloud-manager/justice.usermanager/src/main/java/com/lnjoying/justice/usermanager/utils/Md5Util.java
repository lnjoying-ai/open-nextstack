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

package com.lnjoying.justice.usermanager.utils;

import org.springframework.util.DigestUtils;

import javax.validation.constraints.NotEmpty;

public class Md5Util
{
    private static final String salt = "x2W";

    private static final String password = "!QWE@#95";

    public static String getMd5(@NotEmpty String source)
    {
        String encStr =DESUtil.encrypt(password, source);
        encStr = encStr+salt;
        return DigestUtils.md5DigestAsHex(encStr.getBytes());
    }

    public static Boolean md5Check(@NotEmpty String encStr, @NotEmpty String source)
    {
        return getMd5(source).equals(encStr);
    }
}
