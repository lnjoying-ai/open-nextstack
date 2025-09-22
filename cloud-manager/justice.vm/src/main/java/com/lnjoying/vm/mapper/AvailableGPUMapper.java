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
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.lnjoying.vm.domain.dto.response.AvailableGPURsp;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AvailableGPUMapper extends BaseMapper<AvailableGPURsp>
{
    @Select("SELECT tbl_pci_device.NAME," +
            "COUNT (tbl_pci_device.device_id) as gpuCount," +
//            " COUNT ( tbl_pci_device.device_group_id ) as gpuCount," +
            " tbl_hypervisor_node.node_id" +
            " FROM tbl_pci_device" +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " WHERE " +
            "  tbl_hypervisor_node.phase_status = 40  and tbl_hypervisor_node.error_count=0 and tbl_pci_device.name = #{gpuName}  and  tbl_pci_device.vm_instance_id is null" +
            " and tbl_hypervisor_node.available_ib_count >= #{ibCount} " +
            " GROUP BY (tbl_pci_device.NAME, tbl_hypervisor_node.node_id )"
    )
    List<AvailableGPURsp> selectAvailableGPURspByName(String gpuName, Integer ibCount);


    @Select("SELECT  * from  " +
            "(SELECT tbl_pci_device.name as gpuName," +
            " COUNT ( tbl_pci_device.device_id ) as gpuCount," +
            " tbl_hypervisor_node.node_id" +
            " FROM tbl_pci_device" +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " WHERE " +
            "  tbl_hypervisor_node.phase_status = 40  and tbl_hypervisor_node.error_count=0 and tbl_pci_device.name = #{gpuName}  and  tbl_pci_device.vm_instance_id is null " +
            " and tbl_hypervisor_node.available_ib_count >= #{ibCount} " +
            " GROUP BY (tbl_pci_device.NAME, tbl_hypervisor_node.node_id )) as gpuNode where gpuNode.gpuCount >= #{gpuCount} limit  #{size} offset #{index}"
    )
    List<AvailableGPURsp> selectAvailableGPURspByNameAndCount(String gpuName, Integer gpuCount, Integer index, Integer size, Integer ibCount);


    @Select("SELECT  count(*) from  " +
            "(SELECT tbl_pci_device.NAME," +
            " COUNT ( tbl_pci_device.device_id ) as gpuCount," +
            " tbl_hypervisor_node.node_id" +
            " FROM tbl_pci_device" +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " WHERE " +
            "  tbl_hypervisor_node.phase_status = 40  and tbl_hypervisor_node.error_count=0 and tbl_pci_device.name = #{gpuName}  and  tbl_pci_device.vm_instance_id is null" +
            " GROUP BY (tbl_pci_device.NAME, tbl_hypervisor_node.node_id )) as gpuNode where gpuNode.gpuCount >= #{gpuCount}"
    )
    long selectTotalAvailableGPURspByNameAndCount(String gpuName, Integer gpuCount);

    @Select("SELECT tbl_pci_device.name as gpuName," +
            " COUNT ( CASE WHEN tbl_pci_device.vm_instance_id IS NULL THEN 1 END) as gpuCount," +
            " tbl_hypervisor_node.node_id" +
            " FROM tbl_pci_device" +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " ${ew.customSqlSegment} and  tbl_hypervisor_node.phase_status = 40  and tbl_hypervisor_node.error_count=0 " +
            " GROUP BY (tbl_pci_device.NAME, tbl_hypervisor_node.node_id )"
    )
    List<AvailableGPURsp> selectAvailableGPURsp(@Param(Constants.WRAPPER) Wrapper<AvailableGPURsp> queryWrapper);


    @Select("SELECT tbl_pci_device.name as gpuName," +
            " COUNT ( tbl_pci_device.device_id ) as gpuCount," +
            " tbl_hypervisor_node.node_id" +
            " FROM tbl_pci_device" +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " ${ew.customSqlSegment}" +
            " GROUP BY (tbl_pci_device.NAME, tbl_hypervisor_node.node_id )"
    )
    List<AvailableGPURsp> selectAvailableGPURspWithAllNode(@Param(Constants.WRAPPER) Wrapper<AvailableGPURsp> queryWrapper);

}
