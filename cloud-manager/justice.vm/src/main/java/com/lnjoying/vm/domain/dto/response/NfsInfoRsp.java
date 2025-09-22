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

import com.lnjoying.vm.entity.Nfs;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class NfsInfoRsp
{
    private String nfsId;

    private String name;

    private String createTime;

    private String updateTime;

    private String vpcId;

    private String subnetId;

    private String vpcCidr;

    private String subnetCidr;

    private Integer size;

    private Integer phaseStatus;

    private String servicePath;

    private String description;

    private String vpcName;

    private String subnetName;

    private String portId;

    public void setNfsInfo(Nfs tblNfs)
    {
        this.nfsId = tblNfs.getNfsId();
        this.name = tblNfs.getName();
        this.createTime = Utils.formatDate(tblNfs.getCreateTime());
        this.updateTime = Utils.formatDate(tblNfs.getUpdateTime());
        this.vpcId = tblNfs.getVpcId();
        this.subnetId = tblNfs.getSubnetId();
        this.size = tblNfs.getSize();
        this.phaseStatus = tblNfs.getPhaseStatus();
        this.description = tblNfs.getDescription();
        this.portId = tblNfs.getPortId();
    }
}
