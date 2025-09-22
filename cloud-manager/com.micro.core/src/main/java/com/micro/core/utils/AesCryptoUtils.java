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

package com.micro.core.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

import java.nio.charset.StandardCharsets;

public class AesCryptoUtils
{
    /**
     * Only 128 bit keys can be used, 256 keys cannot be used,
     * otherwise it may be thrownIllegal key size or default parameters
     */
    private static final String key = "jdjdfajdjdfjk,o!";

    private AesCryptoUtils()
    {
    }

    /**
     * Encryption, using UTF-8 encoding
     *
     * @param content
     * @return Encrypted Hex
     */
    public static String encryptHex(String content)
    {
        return getInstance().encryptHex(content);
    }

    /**
     * Decrypt the string represented by Hex (hexadecimal) or Base64, the default UTF-8 encoding
     *
     * @param encryptHex
     * @return Decrypted String
     */
    public static String decryptStr(String encryptHex)
    {
        return getInstance().decryptStr(encryptHex, CharsetUtil.CHARSET_UTF_8);
    }

    public static SymmetricCrypto getInstance()
    {
        return Holder.INSTANCE;
    }

    private static class Holder
    {
        static SymmetricCrypto INSTANCE = new SymmetricCrypto(SymmetricAlgorithm.AES, key.getBytes(StandardCharsets.UTF_8));
    }
}
