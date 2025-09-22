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

import com.lnjoying.justice.operation.entity.TblLog;
import lombok.Data;

import java.util.List;

/**
 * 运维管理--日志(TblLog)表实体类
 *
 * @author Lis
 * @since 2023-05-23 21:01:03
 */
@Data
public class LogsResp {

    private long totalNum;
    private List<TblLog> alarmRules;

}

