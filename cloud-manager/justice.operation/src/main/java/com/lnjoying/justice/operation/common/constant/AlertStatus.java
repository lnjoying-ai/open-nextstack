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

package com.lnjoying.justice.operation.common.constant;

public enum AlertStatus
{
    FIRING(0, ModuleConstant.FIRING),
    RESOLVED(1, ModuleConstant.RESOLVED);

    private final Integer value;

    private final String status;

    public Integer getValue()
    {
        return value;
    }

    public String getStatus()
    {
        return status;
    }

    AlertStatus( int value, String status)
    {
        this.value = value;
        this.status = status;
    }

    public static String getStatusByValue(Integer value)
    {
        switch (value)
        {
            case 0:
                return FIRING.status;
            case 1:
                return RESOLVED.status;
            default:
                return null;
        }
    }

    public static Integer getValueByStatus(String status)
    {
        switch (status)
        {
            case ModuleConstant.FIRING:
                return FIRING.value;
            case ModuleConstant.RESOLVED:
                return RESOLVED.value;
            default:
                return null;
        }
    }
}
