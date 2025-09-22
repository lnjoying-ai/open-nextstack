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

import com.lnjoying.justice.network.domain.dto.request.EipPortMapCreateReqVo;
import lombok.Data;

import java.util.List;

@Data
public class EipPortMapRspVo
{
   private String eipMapId;

   private String eipId;

   private String mapName;

   private String userId;

   private String eipAddress;

   private String vpcId;

   private String vpcName;

   private String subnetCidr;

   private String subnetName;

   private String subnetId;

   private String insideIp;

   private List<EipPortMapCreateReqVo.portMap> portMaps;

   private String bandwidth;

   private String instanceName;

   private String instanceId;

   private boolean isVm;

   private String createTime;

   private boolean isOneToOne;

   private Integer phaseStatus;

   private String publicIp;
}
