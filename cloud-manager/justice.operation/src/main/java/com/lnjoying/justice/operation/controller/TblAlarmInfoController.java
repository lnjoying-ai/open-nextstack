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


import cn.hutool.core.lang.Console;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.domain.dto.request.AlarmMarkResolved;
import com.lnjoying.justice.operation.domain.dto.response.AlarmDistributionBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmInfoBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmInfosResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmStatisticsBaseResp;
import com.lnjoying.justice.operation.service.TblAlarmInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * (TblAlarmInfo)表控制层
 * 运维管理--报警
 *
 * @author Lis
 * @since 2023-05-19 17:47:43
 */
@RestController
@RestSchema(schemaId = "operation")
@Api(value = "AlarmInfo Controller", tags = {"AlarmInfo Controller"})
@RequestMapping("/operation/v1")
@Slf4j
public class TblAlarmInfoController {
    /**
     * 服务对象
     */
    @Resource
    private TblAlarmInfoService tblAlarmInfoService;


    /**
     * @param: tblAlarmRule
     * @param: pageSize
     * @param: pageNum
     * @param: userId
     * @description: TODO   运维管理--报警--分页查询
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/22
     */
    @ApiOperation(value = "运维管理--报警--分页查询", response = Object.class)
    @GetMapping("/alarm-infos")
    public AlarmInfosResp selectAlarmInfoPage(@ApiParam(name = "summery_info") @RequestParam(required = false, value = "summery_info") String summeryInfo,
                                              @ApiParam(name = "resource_type") @RequestParam(required = false, value = "resource_type") Integer resourceType,
                                              @ApiParam(name = "start_time") @RequestParam(required = false, value = "start_time") Long startTime,
                                              @ApiParam(name = "end_time") @RequestParam(required = false, value = "end_time") Long endTime,
                                              @ApiParam(name = "phase_status") @RequestParam(required = false, value = "phase_status") Integer phaseStatus,
                                              @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size", defaultValue = "100") Integer pageSize,
                                              @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num", defaultValue = "1") Integer pageNum,
                                              @RequestHeader(name = "X-UserId", required = false) String userId
    ) {

        try {
            return tblAlarmInfoService.selectAlaPage(summeryInfo, resourceType, startTime, endTime, phaseStatus, pageSize, pageNum, userId);
        } catch (Exception e) {
            log.error("get alarm infos failed: {}", e.getMessage());
            throw throwWebException(e);
        }

    }


    @ApiOperation(value = "运维管理--报警--标记解决", response = Object.class)
    @PutMapping("/alarm-mark-resolved")
//    @LogAnnotation(description = "标记【标记id：{}，标记状态：{}】", resource = "运维管理-报警设置-标记解决", obtainParameter = "infoIds,phaseStatus")
    public AlarmInfoBaseResp updateAlarmInfoStatus(@ApiParam(value = "AlarmMarkResolved", required = true, name = "AlarmMarkResolved") @RequestBody AlarmMarkResolved alarmMarkResolved,
                                                   @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            Console.log("update alarm infos alarmInfoReq: {}", alarmMarkResolved);
            return this.tblAlarmInfoService.updateAlarmInfoStatus(alarmMarkResolved, userId);
        } catch (Exception e) {
            log.error("update alarm rules failed: {}", e.getMessage());
            throw throwWebException(e);
        }

    }


    @ApiOperation(value = "报警消息--近一周报警统计", response = Object.class)
    @GetMapping("/alarm-statistics")
    public List<AlarmStatisticsBaseResp> getAlarmStatistics(@RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            return this.tblAlarmInfoService.getAlarmStatistics(userId);
        } catch (Exception e) {
            log.error("alarm statistics failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @ApiOperation(value = "报警消息--近一周报警分布", response = Object.class)
    @GetMapping("/alarm-distribution")
    public AlarmDistributionBaseResp getAlarmDistribution(@RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            return this.tblAlarmInfoService.getAlarmDistribution(userId);
        } catch (Exception e) {
            log.error("alarm distribution failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    public WebSystemException throwWebException(Exception e) {
        if (e instanceof WebSystemException) {
            return (WebSystemException) e;
        } else {
            return new WebSystemException(ErrorCode.SystemError, ErrorLevel.CRITICAL);
        }
    }

}

