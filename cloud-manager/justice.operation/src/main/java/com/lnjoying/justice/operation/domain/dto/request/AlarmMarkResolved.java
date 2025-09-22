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

package com.lnjoying.justice.operation.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @description: TODO   标记解决 入参
 * @author: LiSen
 * @date: 2023/6/14
 */
@Data
public class AlarmMarkResolved {

    @NotEmpty(message = "infoIds cannot be empty！")
    private List<@NotBlank  String> infoIds;

    //报警状态（0，未报警 1，已报警 2，隐藏）
    @Min(value = 0)
    @Max(value = 2)
    private Integer phaseStatus;

    }

