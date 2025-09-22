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

package com.lnjoying.justice.schema.entity.network;

import lombok.Data;

@Data
public class SecurityGroupRule
{
    String ruleId;
    Integer priority;
    Integer direction;
    Integer protocol;
    Integer addressType;
    Integer action;
    String description;
    String port;
    AddressesRef addressRef;
    String createTime;
    String updateTime;

    @Data
    public static class AddressesRef
    {
        String cidr;
        String sgId;
        String ipPoolId;
    }
}

