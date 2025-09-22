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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 *
 * </p>
 *
 * @author George
 * @since 2023-02-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_vm_snap")
@ApiModel(value = "VmSnap对象", description = "")
public class VmSnap extends BaseColumns
{

    private static final long serialVersionUID = 1L;

    @TableId("snap_id")
    private String snapId;

    @TableField("vm_instance_id")
    private String vmInstanceId;

    @TableField("name")
    private String name;

    @TableField("snap_id_from_agent")
    private String snapIdFromAgent;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("description")
    private String description;

    @TableField("is_current")
    private Boolean isCurrent;

    @TableField("parent_id")
    private String parentId;

}
