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

import com.lnjoying.justice.network.entity.EipPool;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class EipPoolDetailInfoRspVo
{
    private String name;

    private String description;

    private String createTime;

    private String updateTime;

    private Integer vlanId;

    private Integer phaseStatus;

    public void setEipPool(EipPool tblEipPool)
    {
        this.name = tblEipPool.getName();
        this.description = tblEipPool.getDescription();
        this.createTime = Utils.formatDate(tblEipPool.getCreateTime());
        this.updateTime = Utils.formatDate(tblEipPool.getUpdateTime());
        this.phaseStatus = tblEipPool.getPhaseStatus();
    }
}
