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

import com.lnjoying.justice.network.utils.NetworkUtils;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BackendCreateReq
{

    @NotEmpty(message = "name不能为空")
    private String name;

    @NotEmpty(message = "protocol不能为空")
    private String protocol;

    private List<String> backendServers;

    @NotEmpty(message = "balance不能为空")
    private String balance;

    @NotEmpty(message = "vpcId不能为空")
    private String vpcId;

    @NotEmpty(message = "lbId不能为空")
    private String lbId;

    @AssertTrue(message = "backendServer is not valid")
    private boolean isValid()
    {
        if (backendServers.isEmpty()) return false;
        for (String backendServer : backendServers)
        {
            if (!NetworkUtils.isValidIpPort(backendServer))
            {
                return false;
            }
        }
        return true;
    }
}
