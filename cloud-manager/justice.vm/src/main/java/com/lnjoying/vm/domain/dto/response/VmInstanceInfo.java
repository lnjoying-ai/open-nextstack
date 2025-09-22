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

import com.lnjoying.vm.entity.VmInstance;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class VmInstanceInfo
{
    private String instanceId;

    private String name;

    private int phaseStatus;

    private String hostname;

    private ImageAbbrInfo imageInfo;

    private VpcAbbrInfo vpcInfo;

    private SubnetAbbrInfo subnetInfo;

    private PortAbbrInfo portInfo;

    private String description;

    private String createTime;

    private String updateTime;

    private String volumeId;

    private String hypervisorNodeId;

    private String eipId;

    private String eip;

    private String publicIp;

    private String boundType;

    private Integer boundPhaseStatus;

    public void setInstanceInfo(VmInstance tblVmInstance)
    {
        this.instanceId = tblVmInstance.getVmInstanceId();
        this.name = tblVmInstance.getName();
        this.phaseStatus = tblVmInstance.getPhaseStatus();
        this.hostname = tblVmInstance.getHostName();
        this.volumeId = tblVmInstance.getVolumeId();
        this.description = tblVmInstance.getDescription();
        this.createTime = Utils.formatDate(tblVmInstance.getCreateTime());
        this.updateTime = Utils.formatDate(tblVmInstance.getUpdateTime());
    }
}
