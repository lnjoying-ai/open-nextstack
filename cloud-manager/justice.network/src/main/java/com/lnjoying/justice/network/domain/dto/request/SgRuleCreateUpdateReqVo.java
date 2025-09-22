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

package com.lnjoying.justice.network.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

@Data
public class SgRuleCreateUpdateReqVo
{
//不需要对name做任何的校验
    String name;

    @NotNull
    Integer priority;

//    @NotNull
//    String sgId;

    @NotNull
    Integer direction;

    @NotNull
    Integer protocol;

    //22,44,1024-1400
    @NotNull
    String port;

    @NotNull
    Integer addressType;

    @NotNull
    AddressesRef addressRef;

    @NotNull
    Integer action;

    String description;

    @Data
    public static class AddressesRef
    {
        String cidr;
        String sgId;
        String ipPoolId;
    }

    @AssertTrue(message = "port is invalid")
    private boolean isValidPortRange()
    {
        if (port.isEmpty() || "0".equals(port) || "all".equals(port)) return true;
        String[] ports = port.split(",");
        for (String portStr : ports) {
            if (portStr.contains("-")) {
                // 处理端口范围
                String[] range = portStr.split("-");
                if (range.length != 2) {
                    return false; // 无效的端口范围
                }
                int start, end;
                try {
                    start = Integer.parseInt(range[0].trim());
                    end = Integer.parseInt(range[1].trim());
                } catch (NumberFormatException e) {
                    return false; // 无效的端口号
                }
                if (start < 0 || end < 0 || end > 65535 || start > end) {
                    return false; // 端口号超出范围或范围无效
                }
            } else {
                // 处理单个端口号
                int portNumber;
                try {
                    portNumber = Integer.parseInt(portStr.trim());
                } catch (NumberFormatException e) {
                    return false; // 无效的端口号
                }
                if (portNumber < 0 || portNumber > 65535) {
                    return false; // 端口号超出范围
                }
            }
        }
        return true; // 所有端口范围和端口号都有效
    }
}
