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
@TableName("tbl_instance_network_ref")
@ApiModel(value = "InstanceNetworkRef对象", description = "")
public class InstanceNetworkRef
{

    private static final long serialVersionUID = 1L;

    @TableId("instance_network_id")
    private String instanceNetworkId;

    @TableField("instance_id")
    private String instanceId;

    @TableField("vpc_id")
    private String vpcId;

    @TableField("subnet_id")
    private String subnetId;

    @TableField("port_id")
    private String portId;

    @TableField("instance_type")
    private Integer instanceType;

    @TableField("static_ip")
    private String staticIp;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("is_vip")
    private Boolean isVip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
