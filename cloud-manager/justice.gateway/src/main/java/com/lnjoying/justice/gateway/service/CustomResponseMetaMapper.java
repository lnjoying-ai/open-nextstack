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

package com.lnjoying.justice.gateway.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.apache.servicecomb.swagger.invocation.response.ResponseMetaMapper;

import java.util.HashMap;
import java.util.Map;

public class CustomResponseMetaMapper implements ResponseMetaMapper
{
    private final static Map<Integer, JavaType> CODES = new HashMap<>(1);
    static
    {
        CODES.put(500, SimpleType.constructUnsafe(IllegalStateErrorData.class));
    }

    @Override
    public int getOrder()
    {
        return 100;
    }

    @Override
    public Map<Integer, JavaType> getMapper()
    {
        return CODES;
    }
}
