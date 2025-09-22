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
import com.lnjoying.vm.domain.dto.response.PciDeviceInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DeviceInfoMapper extends BaseMapper<PciDeviceInfo>
{
    @Select("select tbl_pci_device.device_id, tbl_pci_device.type as pci_device_type, tbl_pci_device.name as pci_device_name," +
            "tbl_pci_device.phase_status, tbl_pci_device.create_time, tbl_pci_device.update_time from tbl_pci_device " +
            "  ${ew.customSqlSegment}"
    )
    List<PciDeviceInfo> selectPCIDevices(IPage<PciDeviceInfo> page, @Param(Constants.WRAPPER) Wrapper<PciDeviceInfo> queryWrapper);
}
