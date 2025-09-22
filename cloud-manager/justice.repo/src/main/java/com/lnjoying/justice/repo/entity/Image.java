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
 * @since 2023-03-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_image")
@ApiModel(value="Image对象", description="")
public class Image extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("image_id")
    private String imageId;

    @TableField("file_id_from_agent")
    private String fileIdFromAgent;

    @TableField("image_os_type")
    private Integer imageOsType;

    @TableField("image_os_vendor")
    private Integer imageOsVendor;

    @TableField("image_os_version")
    private String imageOsVersion;

    @TableField("image_name")
    private String imageName;

    @TableField("image_format")
    private Integer imageFormat;

    @TableField("agent_ip")
    private String agentIp;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("phase_info")
    private String phaseInfo;

    @TableField("is_public")
    private Boolean isPublic;

    @TableField("description")
    private String description;

    @TableField("vm_instance_id")
    private String vmInstanceId;

    @TableField("image_base")
    private String imageBase;
}
