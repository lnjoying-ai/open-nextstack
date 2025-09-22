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

import com.lnjoying.justice.network.entity.Vpc;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class VpcDetailInfoRspVo
{
    private String vpcId;

    private String name;

    private Integer phaseStatus;

    private String phaseInfo;

    private String userId;

    private String userName;
    //    Short vlanId;
    private Integer count;

    private String createTime;

    private String updateTime;

    private String cidr;

    public void setVpcDetailInfoRsp(Vpc tblVpc)
    {
        this.vpcId = tblVpc.getVpcId();

        this.name = tblVpc.getName();

        this.phaseStatus = tblVpc.getPhaseStatus();

        this.userId = tblVpc.getUserId();

        this.phaseInfo = tblVpc.getPhaseInfo();

        this.cidr = tblVpc.getCidr();
        //this.vlan_name = tblRsVpc.getVlanId();

        this.createTime = Utils.formatDate(tblVpc.getCreateTime());

        this.updateTime = Utils.formatDate(tblVpc.getUpdateTime());
    }
}
