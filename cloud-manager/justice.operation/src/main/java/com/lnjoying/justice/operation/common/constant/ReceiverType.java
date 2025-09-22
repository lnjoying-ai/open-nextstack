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

public enum ReceiverType
{
    EMAIL(0, ModuleConstant.EMAIL),
    SMS(1, ModuleConstant.SMS),
    PHONE(2, ModuleConstant.PHONE);

    private final Integer value;

    private final String notifyType;

    public Integer getValue()
    {
        return value;
    }

    public String getType()
    {
        return notifyType;
    }

    ReceiverType( int value, String notifyType)
    {
        this.value = value;
        this.notifyType = notifyType;
    }

    public static String getNotifyTypeByValue(Integer value)
    {
        switch (value)
        {
            case 0:
                return EMAIL.notifyType;
            case 1:
                return SMS.notifyType;
            case 2:
                return PHONE.notifyType;
            default:
                return null;
        }
    }

    public static Integer getValueByNotifyType(String status)
    {
        switch (status)
        {
            case ModuleConstant.EMAIL:
                return EMAIL.value;
            case ModuleConstant.SMS:
                return SMS.value;
            case ModuleConstant.PHONE:
                return PHONE.value;
            default:
                return null;
        }
    }

}
