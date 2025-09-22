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

package com.lnjoying.vm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.lnjoying.vm.domain.dto.response.HypervisorNodeInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface HypervisorNodeInfoMapper extends BaseMapper<HypervisorNodeInfo>
{

    //    @Select("SELECT  DISTINCT tbl_hypervisor_node.node_id,tbl_hypervisor_node.name as node_name,tbl_hypervisor_node.manage_ip, tbl_pci_device_group.device_group_id_from_agent as pci_device_group_id  " +
//            "FROM tbl_hypervisor_node " +
//            " LEFT JOIN tbl_pci_device_group on tbl_hypervisor_node.node_id = tbl_pci_device_group.node_id" +
//            " LEFT JOIN tbl_pci_device on tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id ${ew.customSqlSegment} ")
    @Select("SELECT " +
            "tbl_hypervisor_node.node_id," +
            "tbl_hypervisor_node.NAME AS node_name," +
            "tbl_hypervisor_node.mem_total," +
            "tbl_hypervisor_node.cpu_log_count," +
            "tbl_hypervisor_node.cpu_model," +
            "tbl_hypervisor_node.manage_ip," +
            "count(tbl_pci_device.device_id) as available_gpu_count " +
            "FROM " +
            "tbl_hypervisor_node " +
            "LEFT JOIN tbl_pci_device ON tbl_hypervisor_node.node_id = tbl_pci_device.node_id " +
//            "LEFT JOIN tbl_pci_device ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            " ${ew.customSqlSegment} " +
            " GROUP BY tbl_hypervisor_node.node_id"
    )
    List<HypervisorNodeInfo> selectGpuNodeInfo(IPage<HypervisorNodeInfo> page, @Param(Constants.WRAPPER) Wrapper<HypervisorNodeInfo> queryWrapper);

    @Select("SELECT " +
            "tbl_hypervisor_node.node_id," +
            "tbl_hypervisor_node.NAME AS node_name," +
            "tbl_hypervisor_node.mem_total," +
            "tbl_hypervisor_node.cpu_log_count," +
            "tbl_hypervisor_node.cpu_model," +
            "tbl_hypervisor_node.manage_ip " +
            "FROM " +
            "tbl_hypervisor_node " +
//            "LEFT JOIN tbl_pci_device_group ON tbl_hypervisor_node.node_id = tbl_pci_device_group.node_id " +
            " ${ew.customSqlSegment} "
    )
    List<HypervisorNodeInfo> selectCustomNodeInfo(IPage<HypervisorNodeInfo> page, @Param(Constants.WRAPPER) Wrapper<HypervisorNodeInfo> queryWrapper);
}
