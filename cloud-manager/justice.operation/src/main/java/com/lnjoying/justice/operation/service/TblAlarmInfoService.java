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

package com.lnjoying.justice.operation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lnjoying.justice.operation.domain.dto.request.AlarmMarkResolved;
import com.lnjoying.justice.operation.domain.dto.response.AlarmDistributionBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmInfoBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmInfosResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmStatisticsBaseResp;
import com.lnjoying.justice.operation.entity.TblAlarmInfo;

import java.util.List;

/**
 * (TblAlarmInfo)表服务接口
 *
 * @author Lis
 * @since 2023-05-19 17:47:45
 */
public interface TblAlarmInfoService extends IService<TblAlarmInfo> {

    //运维管理--报警--分页查询
    AlarmInfosResp selectAlaPage(String summeryInfo, Integer resourceType, Long startTime, Long endTime, Integer phaseStatus, Integer pageSize, Integer pageNum, String userId);

    //报警消息--近一周报警统计
    List<AlarmStatisticsBaseResp> getAlarmStatistics(String userId);

    //报警消息--近一周报警分布
    AlarmDistributionBaseResp getAlarmDistribution(String userId);

    //运维管理-报警设置-标记解决
    AlarmInfoBaseResp updateAlarmInfoStatus(AlarmMarkResolved alarmMarkResolved, String userId);
}

