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

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.micro.core.persistence.redis.RedisUtil;

import java.io.IOException;
import java.util.regex.Pattern;

public class ValidatorUtil
{
    public static boolean validateStr(String regx, String input)
    {
        return Pattern.matches(regx, input);
    }

    public static void checkValidateCode(String verCode,String redisMKey, String key) throws IOException
    {
        if (null == verCode || verCode.isEmpty())
        {
            throw new WebSystemException(ErrorCode.Invalid_validateCode, ErrorLevel.ERROR);
        }

        String cacheValidateCode = RedisUtil.get(redisMKey+key);

        if (! verCode.equals(cacheValidateCode))
        {
            throw new WebSystemException(ErrorCode.Invalid_validateCode, ErrorLevel.ERROR);
        }

        RedisUtil.delete(redisMKey+key);
    }
}
