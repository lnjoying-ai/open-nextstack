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

package com.lnjoying.justice.operation.entity.sender;

import lombok.Data;

@Data
public class SingleSmsRsp {
    // 成功发送的短信计费条数，
    // 计费规则如下：
    // 70个字一条，超出70个字时按每67字一条计费
    //（英文按字母个数计算）
    String fee;
    String mobile;
    //短信标识符（用于匹配状态报告），一个手机号对应一个sid
    String sid;
}
