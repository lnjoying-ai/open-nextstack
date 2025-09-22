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
import com.lnjoying.vm.entity.PciDevice;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author George
 * @since 2023-03-25
 */
public interface PciDeviceMapper extends BaseMapper<PciDevice>
{

    @Select("select tbl_pci_device.*  from tbl_pci_device, tbl_vm_instance  " +
            "where tbl_pci_device.vm_instance_id = tbl_vm_instance.vm_instance_id  " +
            "and tbl_vm_instance.phase_status = -1")
    List<PciDevice> selectRemovedPciDevice();

    @Select("SELECT COALESCE((SELECT COUNT(*) FROM tbl_pci_device WHERE phase_status <> -1  GROUP BY node_id LIMIT 1),0) as count")
    Long selectPciDeviceCount();
}
