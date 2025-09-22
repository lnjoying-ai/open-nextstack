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

import com.lnjoying.justice.network.entity.Loadbalancer;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class LoadbalancerDetailInfoRsp
{
    private String lbId;

    private String name;

    private String description;

    private Integer phaseStatus;

    private String createTime;

    private String updateTime;

    private String subnetId;

    private String subnetName;

    private String vpcId;

    private String vpcName;

    private Long frontendCount;

    public  void  setLoadbalancerDetailInfoRsp(Loadbalancer loadbalancer)
    {
        this.lbId = loadbalancer.getLbId();
        this.name = loadbalancer.getName();
        this.description = loadbalancer.getDescription();
        this.createTime = Utils.formatDate(loadbalancer.getCreateTime());
        this.updateTime = Utils.formatDate(loadbalancer.getUpdateTime());
        this.phaseStatus = loadbalancer.getPhaseStatus();
        this.subnetId = loadbalancer.getSubnetId();
    }
}
