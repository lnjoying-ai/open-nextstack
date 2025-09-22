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

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author George
 * @since 2023-03-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_pci_device")
@ApiModel(value = "PciDevice对象", description = "")
public class PciDevice extends BaseColumns
{

    private static final long serialVersionUID = 1L;

    @TableId("device_id")
    private String deviceId;

    @TableField("device_group_id")
    private String deviceGroupId;

    @TableField("type")
    private String type;

    @TableField("name")
    private String name;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("device_id_from_agent")
    private String deviceIdFromAgent;

    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @ApiModelProperty(value = "创建者")
    @TableField(value = "user_id", fill = FieldFill.INSERT, updateStrategy = FieldStrategy.IGNORED)
    private String userId;

    @TableField(value = "vm_instance_id", updateStrategy = FieldStrategy.IGNORED)
    private String vmInstanceId;

    @TableField(value = "partition_id", updateStrategy = FieldStrategy.IGNORED)
    private String partitionId;

    @TableField(value = "node_id")
    private String nodeId;
}
