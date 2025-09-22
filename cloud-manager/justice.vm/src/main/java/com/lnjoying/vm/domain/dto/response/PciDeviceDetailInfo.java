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
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PciDeviceDetailInfo
{
    @TableField("tbl_hypervisor_node.node_id")
    private String nodeId;

    @TableField("tbl_hypervisor_node.name")
    private String nodeName;

    @TableField("tbl_pci_device_group.device_group_id_from_agent")
    private String pciDeviceGroupId;

    @TableField("tbl_pci_device.name")
    private String pciDeviceName;

    @TableField("tbl_pci_device.type")
    private String pciDeviceType;

    @TableField("tbl_pci_device.user_id")
    private String userId;

    @TableField("tbl_pci_device.phase_status")
    private Integer phaseStatus;

    @TableField("tbl_pci_device.device_id")
    private String deviceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("tbl_pci_device.create_time")
    private Date createTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("tbl_pci_device.update_time")
    private Date updateTime;

    @TableField("tbl_vm_instance.vm_instance_id")
    private String vmInstanceId;

    @TableField("tbl_vm_instance.name")
    private String vmInstanceName;
}
