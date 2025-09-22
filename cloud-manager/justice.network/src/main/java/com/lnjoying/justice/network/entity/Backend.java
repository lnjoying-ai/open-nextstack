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
 * 负载均衡-后端服务组
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_backend")
@ApiModel(value="Backend对象", description="负载均衡-后端服务组")
public class Backend extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("backend_id")
    private String backendId;

    @TableField("lb_id")
    private String lbId;

    @TableField("name")
    private String name;

    @TableField("protocol")
    private String protocol;

    @TableField("balance")
    private String balance;

    @TableField("backend_server")
    private String backendServer;

    @TableField("vpc_id")
    private String vpcId;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("backend_id_from_agent")
    private String backendIdFromAgent;

}
