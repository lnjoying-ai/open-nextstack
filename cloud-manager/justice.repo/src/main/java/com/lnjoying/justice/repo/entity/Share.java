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
@TableName("tbl_share")
@ApiModel(value="Share对象", description="")
public class Share extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("share_id")
    private String shareId;

    @TableField("share_id_from_agent")
    private String shareIdFromAgent;

    @TableField("baremetal_id")
    private String baremetalId;

    @TableField("image_id")
    private String imageId;

    @TableField("iscsi_initiator")
    private String iscsiInitiator;

    @TableField("iscsi_target")
    private String iscsiTarget;

    @TableField("iscsi_ipport")
    private String iscsiIpport;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("phase_info")
    private String phaseInfo;

}
