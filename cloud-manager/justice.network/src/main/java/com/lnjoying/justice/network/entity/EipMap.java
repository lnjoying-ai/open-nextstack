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
@TableName("tbl_eip_map")
@ApiModel(value="EipMap对象", description="")
public class EipMap extends BaseColumns{

    private static final long serialVersionUID = 1L;

    @TableId("eip_map_id")
    private String eipMapId;

    @TableField("map_name")
    private String mapName;

    @TableField("eip_id")
    private String eipId;

    @TableField("subnet_id")
    private String subnetId;

    @TableField("port_id")
    private String portId;

    @TableField("is_static_ip")
    private Boolean isStaticIp;

    @TableField("inside_ip")
    private String insideIp;

    @TableField("status")
    private Integer status;

    @TableField("bandwidth")
    private String bandwidth;

    @TableField("is_one_to_one")
    private Boolean isOneToOne;

}
