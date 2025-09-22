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

package com.lnjoying.vm.domain.dto.response;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class VmInstanceAbbrInfo
{
    @TableField("tbl_vm_instance.node_id")
    private String nodeId;

    @TableField("tbl_vm_instance.instance_id_from_agent")
    private String vmInstanceIdFromAgent;
}
