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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class HypervisorNodeAllocationInfo
{
    @TableField("tbl_hypervisor_node.node_id")
    private String nodeId;

    @TableField("tbl_hypervisor_node.name")
    private String name;

    @TableField("tbl_hypervisor_node.manage_ip")
    private String manageIp;

    @TableField("tbl_hypervisor_node.mem_total")
    private Integer memTotal;

    @TableField("tbl_hypervisor_node.cpu_log_count")
    private Integer cpuLogCount;

    @TableField("tbl_hypervisor_node.cpu_model")
    private String cpuModel;

    @JsonProperty("availableGpuCount")
    private Integer gpuCount;

    private String gpuName;

    @JsonProperty("usedCpuSum")
    @TableField("cpu_sum")
    private Integer cpuSum;

    @JsonProperty("usedMemSum")
    @TableField("mem_sum")
    private Integer memSum;

    @JsonIgnore
    @TableField("mem_recycle")
    private Integer memRecycle;

    @JsonIgnore
    @TableField("cpu_recycle")
    private Integer cpuRecycle;

    @TableField("tbl_hypervisor_node.phase_status")
    @JsonProperty("phaseStatus")
//    @JsonIgnore
    private Integer nodePhaseStatus;

    @JsonIgnore
    @TableField("tbl_vm_instance.phase_status")
    private String vmInstancePhaseStatus;

    @TableField("tbl_hypervisor_node.available_ib_count")
    private Integer availableIbCount;

    @TableField("tbl_hypervisor_node.error_count")
    private Integer errorCount;

    @TableField("ib_total")
    private Integer ibTotal;

    private Long gpuTotal;

    @TableField("tbl_hypervisor_node.create_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
