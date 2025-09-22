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

package com.lnjoying.justice.operation.controller;


import com.lnjoying.justice.operation.domain.dto.response.LogsResp;
import com.lnjoying.justice.operation.service.TblLogService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * (TblAlarmInfo)表控制层
 * 运维管理--日志
 *
 * @author Lis
 * @since 2023-05-26 17:47:43
 */
@RestController
@RequestMapping("/operation/v1")
public class TblLogController {
    /**
     * 服务对象
     */
    @Resource
    private TblLogService tblLogService;


    /**
     * @param: time_range
     * @param: pageSize
     * @param: pageNum
     * @param: userId
     * @description: TODO   运维管理--日志--分页查询
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/22
     */
    @ApiOperation(value = "运维管理--日志--分页查询", response = Object.class)
    @GetMapping("/logs")
    public LogsResp selectLogPage(@ApiParam(name = "start_time") @RequestParam(required = false, value = "start_time", defaultValue = "0") Long startTime,
                                  @ApiParam(name = "end_time") @RequestParam(required = false, value = "end_time", defaultValue = "0") Long endTime,
                                  @ApiParam(name = "description") @RequestParam(required = false, value = "description") String description,
                                  @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size", defaultValue = "100") Integer pageSize,
                                  @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num", defaultValue = "1") Integer pageNum,
                                  @RequestHeader(name = "X-UserId", required = false) String userId
    ) {
        return tblLogService.selectLogPage(startTime, endTime, description, pageSize, pageNum, userId);
    }


}

