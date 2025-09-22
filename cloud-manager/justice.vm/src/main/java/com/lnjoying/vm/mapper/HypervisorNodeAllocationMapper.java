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
import com.lnjoying.vm.domain.dto.response.HypervisorNodeAllocationInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface HypervisorNodeAllocationMapper extends BaseMapper<HypervisorNodeAllocationInfo>
{
    @Select("SELECT " +
            " tbl_hypervisor_node.NAME," +
            " tbl_hypervisor_node.manage_ip," +
            " tbl_hypervisor_node.mem_total," +
            " tbl_hypervisor_node.cpu_log_count," +
            " tbl_hypervisor_node.cpu_model," +
            " tbl_hypervisor_node.node_id," +
            " tbl_hypervisor_node.available_ib_count," +
            " tbl_hypervisor_node.phase_status as node_phase_status," +
            " tbl_hypervisor_node.error_count," +
            " tbl_hypervisor_node.create_time," +
            " #{ibTotal} AS ib_total," +
            " COALESCE(sub.CPU_SUM,0) as CPU_SUM," +
            " COALESCE(sub.mem_recycle,0) as mem_recycle," +
            " COALESCE(sub.cpu_recycle,0) as cpu_recycle," +
            " COALESCE(sub.MEM_SUM,0) as MEM_SUM FROM tbl_hypervisor_node left join " +
            "(SELECT " +
            " tbl_hypervisor_node.name, tbl_hypervisor_node.manage_ip, tbl_hypervisor_node.error_count, tbl_hypervisor_node.create_time," +
            " tbl_hypervisor_node.mem_total, tbl_hypervisor_node.cpu_log_count, tbl_hypervisor_node.cpu_model," +
            " tbl_hypervisor_node.node_id, tbl_hypervisor_node.available_ib_count,tbl_hypervisor_node.phase_status," +
//                    " SUM ( tbl_flavor.cpu ) as CPU_SUM," +
//                    " SUM ( tbl_flavor.mem ) as MEM_SUM " +
            "SUM ( CASE WHEN tbl_vm_instance.cpu_count is  NULL THEN tbl_flavor.cpu ELSE tbl_vm_instance.cpu_count END ) as CPU_SUM," +
            "SUM ( CASE WHEN tbl_vm_instance.mem_size  is NULL THEN tbl_flavor.mem ELSE tbl_vm_instance.mem_size END ) as MEM_SUM, " +
            "SUM ( CASE WHEN tbl_vm_instance.recycle_mem_size IS NULL THEN 0 ELSE tbl_vm_instance.recycle_mem_size END ) AS mem_recycle, " +
            "SUM ( CASE WHEN tbl_vm_instance.recycle_cpu_count IS NULL THEN 0 ELSE tbl_vm_instance.recycle_cpu_count END ) AS cpu_recycle " +
            " FROM " +
            " tbl_hypervisor_node " +
            " LEFT JOIN tbl_vm_instance ON tbl_hypervisor_node.node_id = tbl_vm_instance.node_id " +
            " LEFT JOIN tbl_flavor ON tbl_vm_instance.flavor_id = tbl_flavor.flavor_id " +
            " where tbl_vm_instance.phase_status <> -1 " +
//                    " ${ew.customSqlSegment} " +
            " GROUP BY" +
            " tbl_hypervisor_node.node_id) sub" +
            " on tbl_hypervisor_node.node_id = sub.node_id " +
            " ${ew.customSqlSegment} "
//            " where tbl_hypervisor_node.phase_status = 40  and tbl_hypervisor_node.error_count = 0"
    )
    List<HypervisorNodeAllocationInfo> selectNodeAllocationInfo(IPage<HypervisorNodeAllocationInfo> page, @Param(Constants.WRAPPER) Wrapper<HypervisorNodeAllocationInfo> queryWrapper, Integer ibTotal);


    @Select("SELECT  * from " +
            "(SELECT tbl_pci_device.NAME," +
            " COUNT ( tbl_pci_device.device_id ) as gpuCount," +
            " tbl_hypervisor_node.node_id" +
            " FROM tbl_pci_device" +
//            " LEFT JOIN tbl_pci_device_group ON tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            " LEFT JOIN tbl_hypervisor_node ON tbl_pci_device.node_id = tbl_hypervisor_node.node_id " +
            " WHERE " +
            " tbl_hypervisor_node.phase_status = 40 and tbl_pci_device.name = #{gpuName}  and  tbl_pci_device.vm_instance_id is null" +
            "GROUP BY (tbl_pci_device.NAME, tbl_hypervisor_node.node_id )) as gpuNode where gpuNode.gpuCount >= #{gpuCount}"
    )
    List<HypervisorNodeAllocationInfo> selectGpuNodeAllocationInfo(String gpuName, Integer gpuCount);

}
