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

package com.lnjoying.justice.schema.service.operation;

import io.swagger.annotations.ApiParam;

public interface LogService {

    //记录平台操作日志
    void addLog(@ApiParam(name = "userId") String userId, @ApiParam(name = "userName") String userName, @ApiParam(name = "resource") String resource, @ApiParam(name = "description") String description);
    //记录事件日志
    void addEvent(@ApiParam(name = "userId") String userId,@ApiParam(name = "content") String content,
                  @ApiParam(name = "detailInfo") String detailInfo, @ApiParam(name = "result") String result);
}
