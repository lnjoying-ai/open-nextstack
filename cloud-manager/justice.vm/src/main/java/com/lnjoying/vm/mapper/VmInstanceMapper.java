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
import com.lnjoying.vm.domain.dto.response.HypervisorNodeVmsInfo;
import com.lnjoying.vm.entity.VmInstance;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author George
 * @since 2023-02-07
 */
public interface VmInstanceMapper extends BaseMapper<VmInstance>
{

    long sumRootDiskSizeByUserId(@Param("userId") String userId);

    long sumDataDiskSizeByUserId(@Param("userId") String userId);

    long sumCpusByUserId(@Param("userId") String userId);

    long sumMemByUserId(@Param("userId") String userId);

    List<HypervisorNodeVmsInfo> selectNodeVmInfo(@Param("topK") Integer topK);
}
