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

package com.lnjoying.justice.repo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.lnjoying.justice.repo.domain.dto.response.VolumesRsp;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VolumeVoMapper extends BaseMapper<VolumesRsp.VolumeVo>
{
    @Select("select tbl_volume.volume_id, tbl_volume.user_id, tbl_volume.create_time, tbl_volume.update_time, tbl_volume.storage_pool_id, tbl_volume.name, tbl_volume.size, tbl_volume.type, tbl_vm_instance.vm_instance_id as vmInstanceId, tbl_vm_instance.name as vmName, tbl_volume.phase_status " +
            "from tbl_volume left join tbl_vm_instance on tbl_volume.vm_id = tbl_vm_instance.vm_instance_id ${ew.customSqlSegment}")
    List<VolumesRsp.VolumeVo> selectVolumes(IPage<VolumesRsp.VolumeVo> page, @Param(Constants.WRAPPER) Wrapper<VolumesRsp.VolumeVo> queryWrapper);

    @Select("select count(*) from tbl_volume left join tbl_vm_instance on tbl_volume.vm_id = tbl_vm_instance.vm_instance_id ${ew.customSqlSegment}")
    Long countVolumes( @Param(Constants.WRAPPER) Wrapper<VolumesRsp.VolumeVo> queryWrapper);
}
