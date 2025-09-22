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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.JwtMap;

import java.util.Date;
import java.util.Map;

public class JwtTools
{

    public static String getNewJwtToken(String subject, Map<String, Object> headerInfo, int expireTime, String privateSecret)
    {
        String token = Jwts.builder()
                .setSubject(subject)
                .setHeader(headerInfo)
                .setExpiration(new Date(System.currentTimeMillis() + expireTime * 1000))
                .signWith(SignatureAlgorithm.HS512, privateSecret)
                .compact();
        return token;
    }

    public static JwtMap getJwtInfo(String token, String jwtkey, String predix)
    {
        Jws<Claims> claimsJws = Jwts.parser()
                .setSigningKey(jwtkey)
                .parseClaimsJws(token.replace(predix, ""));
        String user =  claimsJws.getBody().getSubject();

        JwtMap jwtInfo = (JwtMap) claimsJws.getHeader();

        return jwtInfo;
    }

    public static void main(String[] args) throws Exception
    {

    }
}
