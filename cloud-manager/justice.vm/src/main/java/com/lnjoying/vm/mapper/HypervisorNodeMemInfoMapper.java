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
import com.lnjoying.vm.domain.dto.response.HypervisorNodeMemInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface HypervisorNodeMemInfoMapper extends BaseMapper<HypervisorNodeMemInfo>
{
    @Select("SELECT " +
            " tbl_hypervisor_node.mem_total," +
            " tbl_hypervisor_node.node_id," +
            " SUM ( CASE WHEN tbl_vm_instance.mem_size IS NULL THEN tbl_flavor.mem ELSE tbl_vm_instance.mem_size END ) AS mem_used," +
            " SUM ( CASE WHEN tbl_vm_instance.recycle_mem_size IS NULL THEN 0 ELSE tbl_vm_instance.recycle_mem_size END ) AS mem_recycle " +
            " FROM" +
            " tbl_vm_instance," +
            " tbl_hypervisor_node," +
            " tbl_flavor" +
            " WHERE" +
            " tbl_vm_instance.node_id = tbl_hypervisor_node.node_id and tbl_flavor.flavor_id=tbl_vm_instance.flavor_id" +
            " AND tbl_hypervisor_node.node_id = #{nodeId}" +
            " AND tbl_vm_instance.phase_status <> -1" +
            " GROUP BY" +
            " tbl_hypervisor_node.node_id")
    List<HypervisorNodeMemInfo> selectNodeMemInfo(@Param("nodeId") String nodeId);
}
