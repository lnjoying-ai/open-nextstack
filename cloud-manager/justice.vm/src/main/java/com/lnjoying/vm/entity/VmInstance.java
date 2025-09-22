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

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * @since 2023-02-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tbl_vm_instance")
@ApiModel(value = "VmInstance对象", description = "")
public class VmInstance extends BaseColumns
{

    private static final long serialVersionUID = 1L;

    @TableId("vm_instance_id")
    private String vmInstanceId;

    @TableField("instance_id_from_agent")
    private String instanceIdFromAgent;

    @TableField("name")
    private String name;

    @TableField("phase_status")
    private Integer phaseStatus;

    @TableField("node_id")
    private String nodeId;

    @TableField("flavor_id")
    private String flavorId;

    @TableField("image_id")
    private String imageId;

    @TableField("vpc_id")
    private String vpcId;

    @TableField("subnet_id")
    private String subnetId;

    @TableField("port_id")
    private String portId;

    @TableField("static_ip")
    private String staticIp;

    @TableField("host_name")
    private String hostName;

    @TableField("cpu_count")
    private Integer cpuCount;

    @TableField("mem_size")
    private Integer memSize;

    @TableField("sys_username")
    private String sysUsername;

    @TableField("sys_password")
    private String sysPassword;

    @TableField("pubkey_id")
    private String pubkeyId;

    @TableField("description")
    private String description;

    @TableField("last_node_id")
    private String lastNodeId;

    @TableField("dest_node_id")
    private String destNodeId;

    @TableField("volume_id")
    private String volumeId;

    @TableField("storage_pool_id")
    private String storagePoolId;

    @TableField(value = "instance_group_id")
    private String instanceGroupId;

    @TableField(value = "boot_dev")
    private String bootDev;

    @TableField(value = "os_type")
    private String osType;

    @TableField(value = "cmp_tenant_id")
    private String cmpTenantId;

    @TableField(value = "cmp_user_id")
    private String cmpUserId;

    @TableField(value = "eip_id", updateStrategy = FieldStrategy.IGNORED)
    private String eipId;

    @TableField(value = "root_disk")
    private Integer rootDisk;

    //回收的内存
    @TableField(value = "recycle_mem_size")
    private Integer recycleMemSize;

    @TableField(value = "recycle_cpu_count")
    private Integer recycleCpuCount;
}
