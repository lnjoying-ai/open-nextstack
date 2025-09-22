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
@TableName("tbl_port")
@ApiModel(value="Port对象", description="")
public class Port
{

    private static final long serialVersionUID = 1L;

    @TableId("port_id")
    private String portId;

    @TableField("subnet_id")
    private String subnetId;

    @TableField("port_id_from_agent")
    private String portIdFromAgent;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("mac_address")
    private String macAddress;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("type")
    private Integer type;

    @TableField("instance_id")
    private String instanceId;

    @TableField("of_port")
    private Integer ofPort;

    @TableField("host_id_from_agent")
    private String hostIdFromAgent;

    @TableField("agent_id")
    private String agentId;

    @TableField("speed")
    private Integer speed;

    @TableField(value = "eip_id", updateStrategy = FieldStrategy.IGNORED)
    private String eipId;

    @TableField("eip_phase_status")
    private Integer eipPhaseStatus;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
