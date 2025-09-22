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

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.vm.rpcserviceimpl.VmServiceImpl;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GpuFlavorMapper extends BaseMapper<VmService.GpuFlavorInfo>
{
    @Select("SELECT" +
            " tbl_pci_device.NAME AS gpuName, " +
            " COUNT ( tbl_pci_device.device_id ) AS gpuCount " +
            " FROM " +
            " tbl_pci_device " +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id" +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " WHERE " +
            " tbl_hypervisor_node.phase_status = 40  and tbl_hypervisor_node.error_count = 0  " +
            " GROUP BY" +
            " ( tbl_pci_device.NAME, tbl_hypervisor_node.node_id )")
    List<VmServiceImpl.GpuFlavorInfo> selectGpuFlavorInfo();

    @Select("SELECT" +
            " tbl_pci_device.NAME AS gpuName, " +
            " COUNT ( tbl_pci_device.device_id ) AS gpuCount " +
            " FROM " +
            " tbl_pci_device " +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id" +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " WHERE " +
            " tbl_hypervisor_node.phase_status = 40 and tbl_hypervisor_node.error_count = 0 and tbl_pci_device.name= #{gpuName}" +
            " GROUP BY" +
            " ( tbl_pci_device.NAME, tbl_hypervisor_node.node_id )")
    List<VmServiceImpl.GpuFlavorInfo> selectGpuFlavorInfoByName(@Param("gpuName") String gpuName);


}
