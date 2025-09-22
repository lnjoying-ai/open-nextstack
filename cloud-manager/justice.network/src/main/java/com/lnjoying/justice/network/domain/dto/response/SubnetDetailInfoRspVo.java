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

import com.lnjoying.justice.network.entity.Subnet;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class SubnetDetailInfoRspVo
{
   private String subnetId;

   private String name;

   private Integer phaseStatus;

   private String phaseInfo;

   private String vpcId;

   private String vpcName;

   private String userId;

   private String userName;

   private Integer addressType;

   private String cidr;

   private String gatewayIp;

   private String createTime;

   private String updateTime;

    public void setSubnetDetailInfoRsp(Subnet tblSubnet)
    {
        this.subnetId = tblSubnet.getSubnetId();
        this.name = tblSubnet.getName();
        this.phaseStatus = tblSubnet.getPhaseStatus();
        this.phaseInfo = tblSubnet.getPhaseInfo();
        this.vpcId = tblSubnet.getVpcId();
        //this.vpc_name
        //this.vlan_name
        this.userId = tblSubnet.getUserId();
        //this.user_name
        this.addressType = tblSubnet.getAddressType();
        this.cidr = tblSubnet.getCidr();
        this.gatewayIp = tblSubnet.getGatewayIp();
        this.createTime = Utils.formatDate(tblSubnet.getCreateTime());
        this.updateTime = Utils.formatDate(tblSubnet.getUpdateTime());
    }
}
