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
import java.util.List;

@Data
public class EipPortMapUpdateReqVo
{

    @Data
    public static class portMap {
        Integer globalPort;
        Integer localPort;
        Integer protocol;
        String portMapId;
        String createTime;
        String updateTime;
    }

    Boolean oneToOne;

    List<portMap> portMaps;

    @AssertTrue(message = "port is invalid")
    public boolean isValidPort()
    {
        if (portMaps == null || portMaps.isEmpty())
        {
            if (oneToOne == null)
                return true;
            return oneToOne;
        }
        for (portMap portMap : portMaps)
        {
            if (portMap.getGlobalPort() == null || portMap.getLocalPort() == null || portMap.getProtocol() == null)
                return false;
            if (portMap.getGlobalPort() < 1 || portMap.getGlobalPort() > 65535 || portMap.getLocalPort() < 1 || portMap.getLocalPort() > 65535)
                return false;
        }
        return true;
    }
}
