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

package com.lnjoying.vm.domain.dto.response;

import com.lnjoying.vm.entity.VmSnap;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class VmSnapInfo
{
    private String snapId;

    private String name;

    private String description;

    private String vmInstanceId;

    private String createTime;

    private String updateTime;

    private int phaseStatus;

    private String vmInstanceName;

    private boolean isCurrent;

    private String parentId;

    public void setSnapInfo(VmSnap tblVmSnap)
    {
        this.snapId = tblVmSnap.getSnapId();
        this.name = tblVmSnap.getName();
        this.vmInstanceId = tblVmSnap.getVmInstanceId();
        this.phaseStatus = tblVmSnap.getPhaseStatus();
        this.createTime = Utils.formatDate(tblVmSnap.getCreateTime());
        this.updateTime = Utils.formatDate(tblVmSnap.getUpdateTime());
        this.isCurrent = tblVmSnap.getIsCurrent();
        this.description = tblVmSnap.getDescription();
        this.parentId = tblVmSnap.getParentId();
    }
}
