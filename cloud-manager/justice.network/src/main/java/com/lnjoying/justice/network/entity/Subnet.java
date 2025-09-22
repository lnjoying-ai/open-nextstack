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
 * @author george
 * @since 2023-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_subnet")
@ApiModel(value="Subnet对象", description="")
public class Subnet extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("subnet_id")
    private String subnetId;

    @TableField("subnet_id_from_agent")
    private String subnetIdFromAgent;

    @TableField("name")
    private String name;

    @TableField("vpc_id")
    private String vpcId;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("phase_info")
    private String phaseInfo;

    @TableField("address_type")
    private Integer addressType;

    @TableField("subnet_cidr")
    private String cidr;

    @TableField("gateway_ip")
    private String gatewayIp;

    @TableField("description")
    private String description;

}
