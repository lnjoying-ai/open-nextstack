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

package com.lnjoying.justice.network.domain.dto.response;

import com.lnjoying.justice.schema.service.compute.ComputeService;
import lombok.Data;

import java.util.List;

@Data
public class TopologyRspVo
{
    private String vpcId;
    private List<SubnetTopology> subnetTopologies;

    @Data
    public static class SubnetTopology{
       private String subnetId;
       private String subnetName;
       private String cidr;
       private List<ComputeService.InstanceInfo> instanceInfos;
    }
}
