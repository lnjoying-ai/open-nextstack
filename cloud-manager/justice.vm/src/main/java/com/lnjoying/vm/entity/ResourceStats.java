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
@TableName("tbl_resource_stats")
@ApiModel(value = "ResourceStats对象", description = "")
public class ResourceStats extends BaseColumns
{

    private static final long serialVersionUID = 1L;

    @TableId("stats_id")
    private String statsId;

    @TableField("name")
    private String name;

    @TableField("total")
    private Integer total;

    @TableField("used")
    private Integer used;

    @TableField("running")
    private Integer running;

    @TableField("unit")
    private String unit;

    @TableField("phase_status")
    private Integer phaseStatus;

}
