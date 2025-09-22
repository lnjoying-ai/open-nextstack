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
import lombok.Data;

@Data
public class HypervisorNodeInfo
{

    @TableField("tbl_hypervisor_node.node_id")
    private String nodeId;

    @TableField("tbl_hypervisor_node.name")
    private String nodeName;

    @TableField("tbl_hypervisor_node.manage_ip")
    private String manageIp;

    @TableField("available_gpu_count")
    private Integer availableGpuCount;

    @JsonIgnore
    @TableField("tbl_pci_device.phase_status")
    private Integer pciDevicePhaseStatus;

    @TableField("tbl_hypervisor_node.phase_status")
    @JsonIgnore
    private Integer nodePhaseStatus;

    @TableField("tbl_pci_device.type")
    @JsonIgnore
    private String pciDeviceType;

    @JsonIgnore
    @TableField("tbl_pci_device_group.device_group_id")
    private String pciDeviceGroupId;

    @TableField("tbl_hypervisor_node.mem_total")
    private Integer memTotal;

    @TableField("tbl_hypervisor_node.cpu_log_count")
    private Integer cpuLogCount;

    @TableField("tbl_hypervisor_node.cpu_model")
    private String cpuModel;

    private Integer cpuAllocation;

    private Integer memAllocation;
}
