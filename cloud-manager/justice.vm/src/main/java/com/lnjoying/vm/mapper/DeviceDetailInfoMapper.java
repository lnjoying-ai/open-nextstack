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
import com.lnjoying.vm.domain.dto.response.PciDeviceDetailInfo;
import com.lnjoying.vm.domain.dto.response.VmInstanceAbbrInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DeviceDetailInfoMapper extends BaseMapper<PciDeviceDetailInfo>
{
    @Select("select tbl_hypervisor_node.node_id, tbl_hypervisor_node.name as node_name, " +
            "tbl_pci_device.name as pci_device_name,tbl_pci_device.type as pci_device_type, tbl_pci_device.user_id, tbl_pci_device.phase_status, tbl_pci_device.device_id," +
            "tbl_pci_device.create_time, tbl_pci_device.update_time, tbl_vm_instance.vm_instance_id, tbl_vm_instance.name as vm_instance_name  " +
            "from tbl_hypervisor_node LEFT JOIN tbl_pci_device on tbl_hypervisor_node.node_id = tbl_pci_device.node_id  " +
//            "LEFT JOIN tbl_pci_device on tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id " +
            "LEFT JOIN tbl_vm_instance on tbl_pci_device.vm_instance_id=tbl_vm_instance.vm_instance_id ${ew.customSqlSegment}")
    List<PciDeviceDetailInfo> selectPCIDevices(IPage<PciDeviceDetailInfo> page, @Param(Constants.WRAPPER) Wrapper<PciDeviceDetailInfo> queryWrapper);

    @Select("select tbl_hypervisor_node.node_id, tbl_hypervisor_node.name as node_name, " +
            "tbl_pci_device.name as pci_device_name,tbl_pci_device.type as pci_device_type,  tbl_pci_device.phase_status, tbl_pci_device.device_id " +
            "from tbl_hypervisor_node LEFT JOIN tbl_pci_device on tbl_hypervisor_node.node_id = tbl_pci_device.node_id  ${ew.customSqlSegment}")
//            "LEFT JOIN tbl_pci_device on tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id ${ew.customSqlSegment}")
    List<PciDeviceDetailInfo> selectPCIDevicesWithNoInstance(IPage<PciDeviceDetailInfo> page, @Param(Constants.WRAPPER) Wrapper<PciDeviceDetailInfo> queryWrapper);


    @Select("select count(*) from tbl_hypervisor_node" +
            " LEFT JOIN tbl_pci_device on tbl_hypervisor_node.node_id = tbl_pci_device.node_id  " +
//            "LEFT JOIN tbl_pci_device on tbl_pci_device_group.device_group_id = tbl_pci_device.device_group_id  " +
            "LEFT JOIN tbl_vm_instance on tbl_pci_device.vm_instance_id=tbl_vm_instance.vm_instance_id ${ew.customSqlSegment}")
    Long countPCIDevices(@Param(Constants.WRAPPER) Wrapper<PciDeviceDetailInfo> queryWrapper);

    @Select("select distinct tbl_pci_device.vm_instance_id from tbl_pci_device " +
            "where tbl_pci_device.device_id = #{device_id}"
    )
    String selectVmInstanceIdByDeviceId(@Param("device_id") String device_id);

    @Select("select distinct tbl_vm_instance.phase_status from tbl_pci_device " +
            "LEFT JOIN tbl_vm_instance on tbl_vm_instance.vm_instance_id = tbl_pci_device.vm_instance_id  " +
            "where tbl_pci_device.device_id = #{device_id}")
    Integer selectVmInstancePhaseStatusByDeviceId(@Param("device_id") String device_id);


    @Select("select distinct tbl_vm_instance.node_id, tbl_vm_instance.instance_id_from_agent as vm_instance_id_from_agent from tbl_pci_device " +
            "LEFT JOIN tbl_vm_instance on tbl_vm_instance.vm_instance_id = tbl_pci_device.vm_instance_id  " +
            "where tbl_pci_device.device_id = #{device_id}")
    VmInstanceAbbrInfo selectVmInstanceAbbrInfoByDeviceId(@Param("device_id") String device_id);

    @Select("select distinct tbl_pci_device.device_id_from_agent from  tbl_pci_device " +
            "where(tbl_pci_device.phase_status = 88 and tbl_pci_device.device_id_from_agent is not null and tbl_pci_device.vm_instance_id = #{vm_instance_id})")
    List<String> selectDeviceIdFromAgentByVmId(@Param("vm_instance_id") String vmInstanceId);

}
