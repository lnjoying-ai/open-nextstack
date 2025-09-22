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

public interface BaremetalService
{
    ComputeService.Instance getBaremetalInstanceFromPortId(@ApiParam(name = "portId") String portId);

    ComputeService.AgentIpPort getAgentIpPort(@ApiParam(name = "portType") short portType, @ApiParam(name = "vmInstanceId") String vmInstanceId);

    List<ComputeService.InstanceInfo> getInstanceInfosFromSubnetId(@ApiParam(name = "subnetId") String subnetId);

    Map<String, List<ComputeService.InstanceInfo>> getInstanceInfos(@ApiParam(name = "subnetIdList") List<String> subnetIdList);

}
