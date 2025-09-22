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

package com.lnjoying.vm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author George
 * @since 2023-02-07
 */
@Data
@TableName("tbl_hypervisor_node")
@ApiModel(value = "HypervisorNode对象", description = "")
public class HypervisorNode
{

    private static final long serialVersionUID = 1L;

    @TableId("node_id")
    private String nodeId;

    @TableField("instance_id")
    private String instanceId;

    @TableField("name")
    private String name;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("agent_id")
    private String agentId;

    @TableField("master_l3")
    private Boolean masterL3;

    @TableField("manage_ip")
    private String manageIp;

    @TableField("host_name")
    private String hostName;

    @TableField("sys_username")
    private String sysUsername;

    @TableField("sys_password")
    private String sysPassword;

    @TableField("pubkey_id")
    private String pubkeyId;

    @TableField("description")
    private String description;

    @TableField("backup_node_id")
    private String backupNodeId;

    @TableField("error_count")
    private Integer errorCount;

    @TableField("cpu_log_count")
    private Integer cpuLogCount;

    @TableField("cpu_phy_count")
    private Integer cpuPhyCount;

    @TableField("mem_total")
    private Integer memTotal;

    @TableField("cpu_model")
    private String cpuModel;

    @TableField("available_ib_count")
    private Integer availableIbCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
