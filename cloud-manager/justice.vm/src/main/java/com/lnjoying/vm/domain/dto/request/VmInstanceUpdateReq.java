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

package com.lnjoying.vm.domain.dto.request;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import javax.validation.constraints.AssertTrue;

@Data
public class VmInstanceUpdateReq
{
    private String name;

    private String description;

    private String flavorId;

    private String bootDev;

    private Integer cpuCount;

    private Integer memorySize;

    @AssertTrue(message = "name,description,flavorId,bootDev is required")
    private boolean isValid()
    {
        return !StrUtil.isBlank(name) || !StrUtil.isBlank(description) ||
                !StrUtil.isBlank(flavorId) || !StrUtil.isBlank(bootDev) || cpuCount != null || memorySize != null;
    }
}
