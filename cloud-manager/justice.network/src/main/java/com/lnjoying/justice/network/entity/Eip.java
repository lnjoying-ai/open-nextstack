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

package com.lnjoying.justice.network.entity;

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
 * @author george
 * @since 2023-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_eip")
@ApiModel(value="Eip对象", description="")
public class Eip extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("eip_id")
    private String eipId;

    @TableField("address_type")
    private Integer addressType;

    @TableField("ipaddr")
    private String ipaddr;

    @TableField("public_ip")
    private String publicIp;

    @TableField("prefix_len")
    private Integer prefixLen;

    @TableField("status")
    private Integer status;

    @TableField("bandwidth")
    private String bandwidth;

    @TableField("pool_id")
    private String poolId;

    @TableField(value = "bound_type", updateStrategy = FieldStrategy.IGNORED)
    private String boundType;

    @TableField(value = "bound_id",updateStrategy = FieldStrategy.IGNORED)
    private String boundId;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @ApiModelProperty(value = "创建者")
    @TableField(value = "user_id", fill = FieldFill.INSERT,  updateStrategy = FieldStrategy.IGNORED)
    private String userId;

}
