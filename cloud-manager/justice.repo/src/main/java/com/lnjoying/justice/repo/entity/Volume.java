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

package com.lnjoying.justice.repo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author George
 * @since 2023-03-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_volume")
@ApiModel(value="Volume对象", description="")
public class Volume extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("volume_id")
    private String volumeId;

    @TableField("storage_pool_id")
    private String storagePoolId;

    @TableField("volume_id_from_agent")
    @JsonIgnore
    private String volumeIdFromAgent;

    @TableField("image_id")
    private String imageId;

    @TableField("name")
    private String name;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("type")
    private Integer type;

    @TableField("description")
    private String description;

    @TableField("size")
    private Integer size;

    @TableField(value = "vm_id",updateStrategy = FieldStrategy.IGNORED)
    @JsonIgnore
    private String vmId;

    @TableField("node_ip")
    @JsonIgnore
    private String nodeIp;

    @TableField("last_ip")
    @JsonIgnore
    private String lastIp;

    @TableField("dest_ip")
    @JsonIgnore
    private String destIp;

    @TableField("export_name")
    @JsonIgnore
    private String exportName;
}
