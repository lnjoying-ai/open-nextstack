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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_nfs")
@ApiModel(value = "NFS对象", description = "")
public class Nfs extends BaseColumns
{
    private static final long serialVersionUID = 1L;

    @TableId("nfs_id")
    private String nfsId;

    @TableField("name")
    private String name;

    @TableField("vpc_id")
    private String vpcId;

    @TableField("subnet_id")
    private String subnetId;

    @TableField("port_id")
    private String portId;

    @TableField("size")
    private Integer size;

    @TableField("nfs_id_from_agent")
    private String nfsIdFromAgent;

    @TableField("description")
    private String description;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("node_ip")
    private String nodeIp;
}
