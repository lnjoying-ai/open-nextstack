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

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lnjoying.vm.entity.PciDevice;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class PciDeviceInfo
{
//    @TableField("tbl_pci_device_group.device_group_id_from_agent")
//    @JsonProperty("pciDeviceGroupId")
//    private String pciDeviceGroupIdFromAgent;

//    @TableField("tbl_pci_device_group.device_group_id")
//    @JsonIgnore
//    private String pciDeviceGroupId;

    @TableField("tbl_pci_device.name")
    private String pciDeviceName;

    @TableField("tbl_pci_device.type")
    private String pciDeviceType;

    @TableField("tbl_pci_device.user_id")
    @JsonIgnore
    private String userId;

    @TableField("tbl_pci_device.phase_status")
    private Integer phaseStatus;

    @TableField("tbl_pci_device.device_id")
    private String deviceId;

    @TableField("tbl_pci_device.create_time")
    private String createTime;

    @TableField("tbl_pci_device.update_time")
    private String updateTime;

    public void setPciDevice(PciDevice tblPciDevice)
    {
        this.createTime = Utils.formatDate(tblPciDevice.getCreateTime());
        this.updateTime = Utils.formatDate(tblPciDevice.getUpdateTime());

        this.deviceId = tblPciDevice.getDeviceId();
        this.phaseStatus = tblPciDevice.getPhaseStatus();

        this.userId = tblPciDevice.getUserId();

        this.pciDeviceName = tblPciDevice.getName();
        this.pciDeviceType = tblPciDevice.getType();
    }

}
