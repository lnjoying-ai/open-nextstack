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

package com.lnjoying.justice.repo.domain.dto.response;

import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.justice.repo.entity.Flavor;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class FlavorDetailInfoRsp extends FlavorService.FlavorInfo
{

//    private Integer phaseStatus;
    private String name;
    private String createTime;
    private String updateTime;
    private Integer phaseStatus;

    public void setFlavorDetailInfoRsp(Flavor tblFlavor)
    {
        this.flavorId = tblFlavor.getFlavorId();
        this.name = tblFlavor.getName();
        this.type = tblFlavor.getType();
        this.cpu = tblFlavor.getCpu();
        this.mem = tblFlavor.getMem();
//        this.rootDisk = tblFlavor.getRootDisk();
        this.createTime =  Utils.formatDate(tblFlavor.getCreateTime());
        this.updateTime =  Utils.formatDate(tblFlavor.getUpdateTime());
        this.gpuCount = tblFlavor.getGpuCount();
        this.gpuName = tblFlavor.getGpuName();
        this.phaseStatus = tblFlavor.getPhaseStatus();
        this.needIb = tblFlavor.getNeedIb();
    }
}
