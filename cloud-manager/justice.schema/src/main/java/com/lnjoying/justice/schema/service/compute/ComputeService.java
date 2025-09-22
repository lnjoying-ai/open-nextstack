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

package com.lnjoying.justice.schema.service.compute;


import io.swagger.annotations.ApiParam;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * compute rpc service
 *
 * @author George
 **/

public interface ComputeService {

    @Data
    final class Instance implements Serializable {
        String instanceId;
        String instanceName;
        String portId;
    }

    @Data
    final class InstanceInfo implements Serializable {
        String instanceId;
        String name;
        String portId;
        String ip;
        boolean isVm;
    }

    @Data
    class VpcAndPortInfo
    {
        String vpcId;
        String portId;
        Boolean isRemoved;
    }

    @Data
    class InstanceDetailInfo extends InstanceCommonInfo  implements  Serializable
    {
        private String portId;
        private String flavorId;
    }

    @Data
    class InstanceCommonInfo implements  Serializable
    {
        String instanceId;
        String name;
        Integer phaseStatus;
        String flavorName;
    }

    @Data
    class AgentIpPort
    {
        String ip;
        String port;
    }
}
