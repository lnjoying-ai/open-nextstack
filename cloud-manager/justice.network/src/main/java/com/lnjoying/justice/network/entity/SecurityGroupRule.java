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
 * @author george
 * @since 2023-01-04
 */
@Data
@TableName("tbl_security_group_rule")
@ApiModel(value="SecurityGroupRule对象", description="")
public class SecurityGroupRule {

    private static final long serialVersionUID = 1L;

    @TableId("rule_id")
    private String ruleId;

    @TableField("sg_id")
    private String sgId;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("priority")
    private Integer priority;

    @TableField("direction")
    private Integer direction;

    @TableField("protocol")
    private Integer protocol;

    @TableField("address_type")
    private Integer addressType;

    @TableField("port")
    private String port;

    @TableField("cidr")
    private String cidr;

    @TableField("sg_id_reference")
    private String sgIdReference;

    @TableField("description")
    private String description;

    @TableField("pool_id")
    private String poolId;

    @TableField("action")
    private Integer action;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
