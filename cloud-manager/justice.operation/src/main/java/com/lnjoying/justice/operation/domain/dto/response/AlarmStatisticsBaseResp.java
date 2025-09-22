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

package com.lnjoying.justice.operation.domain.dto.response;

import lombok.Data;

/**
 * @description: TODO   报警消息--近一周报警统计
 * @author: LiSen
 * @date: 2023/6/13
 */
@Data
public class AlarmStatisticsBaseResp {

    private String date;
    private Integer warning = 0;
    private Integer critical = 0;
    private Integer emergency = 0;

}
