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

package com.lnjoying.justice.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lnjoying.justice.operation.domain.dto.response.AlarmDistributionBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmInfoResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmStatisticsBaseResp;
import com.lnjoying.justice.operation.entity.TblAlarmInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * (TblAlarmInfo)表数据库访问层
 *
 * @author Lis
 * @since 2023-05-19 17:47:44
 */
@Mapper
public interface TblAlarmInfoDao extends BaseMapper<TblAlarmInfo> {

    List<AlarmInfoResp> selectAlaPage(IPage<AlarmInfoResp> page, @Param("summeryInfo") String summeryInfo, @Param("resourceType") Integer resourceType, @Param("startTimeDate") Date startTimeDate, @Param("endTimeDate") Date endTimeDate, @Param("phaseStatus") Integer phaseStatus, @Param("userId") String userId);

    Integer selectAlaCount(@Param("summeryInfo") String summeryInfo, @Param("resourceType") Integer resourceType, @Param("startTimeDate") Date startTimeDate, @Param("endTimeDate") Date endTimeDate, @Param("phaseStatus") Integer phaseStatus, @Param("userId") String userId);

    List<AlarmStatisticsBaseResp> getAlarmSt1atistics(@Param("userId") String userId);


    AlarmStatisticsBaseResp getAlarmStatisticsCount(@Param("userId") String userId, @Param("beginOfDay") LocalDateTime beginOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    AlarmDistributionBaseResp getAlarmDistribution(@Param("userId") String userId, @Param("beginOfDay") LocalDateTime beginOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}

