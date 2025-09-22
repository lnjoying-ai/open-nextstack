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

import com.lnjoying.justice.network.domain.dto.request.SgRuleCreateUpdateReqVo;
import com.lnjoying.justice.network.entity.SecurityGroupRule;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class SecurityGroupRuleRspVo
{
    private String ruleId;

    private Integer priority;

    private Integer direction;

    private Integer protocol;

    private Integer addressType;

    private Integer action;

    private String description;

    private String port;

    private SgRuleCreateUpdateReqVo.AddressesRef addressRef;

    private String createTime;

    private String updateTime;

    public void setSecurityGroupRule(SecurityGroupRule tblSecurityGroupRule)
    {
        ruleId = tblSecurityGroupRule.getRuleId();
        priority = tblSecurityGroupRule.getPriority();
        direction = tblSecurityGroupRule.getDirection();
        protocol = tblSecurityGroupRule.getProtocol();
        addressType = tblSecurityGroupRule.getAddressType();
        port = tblSecurityGroupRule.getPort();
        addressRef = new SgRuleCreateUpdateReqVo.AddressesRef();
        action = tblSecurityGroupRule.getAction();
        description = tblSecurityGroupRule.getDescription();
        createTime = Utils.formatDate(tblSecurityGroupRule.getCreateTime());
        updateTime = Utils.formatDate(tblSecurityGroupRule.getUpdateTime());
        if (null != tblSecurityGroupRule.getCidr() && !tblSecurityGroupRule.getCidr().isEmpty())
        {
            addressRef.setCidr(tblSecurityGroupRule.getCidr());
        }
        else if (null != tblSecurityGroupRule.getSgIdReference() && !tblSecurityGroupRule.getSgIdReference().isEmpty())
        {
            addressRef.setSgId(tblSecurityGroupRule.getSgIdReference());
        }
        else if (null != tblSecurityGroupRule.getPoolId() && !tblSecurityGroupRule.getPoolId().isEmpty())
        {
            addressRef.setIpPoolId(tblSecurityGroupRule.getPoolId());
        }

    }
}
