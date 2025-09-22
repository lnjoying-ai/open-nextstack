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
 * 负载均衡器-监听器
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_frontend")
@ApiModel(value="Frontend对象", description="负载均衡器-监听器")
public class Frontend extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("frontend_id")
    private String frontendId;

    @TableField("frontend_id_from_agent")
    private String frontendIdFromAgent;

    @TableField("lb_id")
    private String lbId;

    @TableField("backend_id")
    private String backendId;

    @TableField("name")
    private String name;

    @TableField("listen_port")
    private String listenPort;

    @TableField("phase_status")
    private Integer phaseStatus;

//    @TableField("port")
//    private Integer port;
}
