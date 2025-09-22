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

package com.lnjoying.justice.operation.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.domain.dto.request.AlarmMarkResolved;
import com.lnjoying.justice.operation.domain.dto.response.*;
import com.lnjoying.justice.operation.entity.TblAlarmInfo;
import com.lnjoying.justice.operation.mapper.TblAlarmInfoDao;
import com.lnjoying.justice.operation.service.TblAlarmInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

/**
 * (TblAlarmInfo)表服务实现类
 *
 * @author Lis
 * @since 2023-05-19 17:47:45
 */
@Slf4j
@Service("tblAlarmInfoService")
public class TblAlarmInfoServiceImpl extends ServiceImpl<TblAlarmInfoDao, TblAlarmInfo> implements TblAlarmInfoService {

    @Autowired
    private TblAlarmInfoDao alarmInfoDao;


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

    @Override
    public AlarmInfosResp selectAlaPage(String summeryInfo, Integer resourceType, Long startTime, Long endTime, Integer phaseStatus, Integer pageSize, Integer pageNum, String userId) {
        AlarmInfosResp alarmInfosResp = new AlarmInfosResp();
        Page<AlarmInfoResp> page = new Page<AlarmInfoResp>(pageNum, pageSize);
        Date startTimeDate = new Date(), endTimeDate = startTimeDate;
        if (null != startTime) {
            startTimeDate = new Date(startTime);
        }
        if (null != endTime) {
            endTimeDate = new Date(endTime);
        }

        Integer alarmInfoCount = alarmInfoDao.selectAlaCount(summeryInfo, resourceType, startTimeDate, endTimeDate, phaseStatus, userId);
        if (alarmInfoCount == 0) {
            return alarmInfosResp;
        }

        List<AlarmInfoResp> alarmInfoResps = alarmInfoDao.selectAlaPage(page, summeryInfo, resourceType, startTimeDate, endTimeDate, phaseStatus, userId);

        alarmInfosResp.setAlarmInfos(alarmInfoResps);
        alarmInfosResp.setTotalNum(alarmInfoCount);

        return alarmInfosResp;
    }


    /**
     * @param: userId
     * @description: TODO   报警消息--近一周报警统计
     * @return: com.lnjoying.justice.operation.domain.dto.response.AlarmInfoBaseResp
     * @author: LiSen
     * @date: 2023/6/13
     */
    @Override
    public List<AlarmStatisticsBaseResp> getAlarmStatistics(String userId) {

        List<AlarmStatisticsBaseResp> alarmStatisticsBaseResps = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < Calendar.DAY_OF_WEEK; i++) {
            LocalDateTime dateTime = now.minusDays(i);
            String dateStr = dateTime.format(formatter);

            AlarmStatisticsBaseResp alarmStatisticsBaseResp = new AlarmStatisticsBaseResp();
            LocalDateTime beginOfDay = LocalDateTimeUtil.beginOfDay(dateTime);
            LocalDateTime endOfDay = LocalDateTimeUtil.endOfDay(dateTime);

            alarmStatisticsBaseResp = alarmInfoDao.getAlarmStatisticsCount(userId, beginOfDay, endOfDay);
            if (alarmStatisticsBaseResp == null)
                alarmStatisticsBaseResp = new AlarmStatisticsBaseResp();
            alarmStatisticsBaseResp.setDate(dateStr);

            alarmStatisticsBaseResps.add(alarmStatisticsBaseResp);
        }

        //List<AlarmStatisticsBaseResp> asd = alarmInfoDao.getAlarmSt1atistics(userId);

        return alarmStatisticsBaseResps;
    }


    /**
     * @param: userId
     * @description: TODO   报警消息--近一周报警分布
     * @return: com.lnjoying.justice.operation.domain.dto.response.AlarmInfoBaseResp
     * @author: LiSen
     * @date: 2023/6/13
     */
    @Override
    public AlarmDistributionBaseResp getAlarmDistribution(String userId) {

        LocalDateTime endOfDay = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        LocalDateTime beginOfDay = LocalDateTimeUtil.beginOfDay(LocalDateTimeUtil.offset(endOfDay, -6, ChronoUnit.DAYS));
        return alarmInfoDao.getAlarmDistribution(userId, beginOfDay, endOfDay);
    }


    /**
     * @param: alarmMarkResolved
     * @param: userId
     * @description: TODO   运维管理-报警设置-标记解决
     * @return: com.lnjoying.justice.operation.domain.dto.response.AlarmInfoBaseResp
     * @author: LiSen
     * @date: 2023/6/14
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlarmInfoBaseResp updateAlarmInfoStatus(AlarmMarkResolved alarmMarkResolved, String userId) {
        alarmMarkResolved.getInfoIds().forEach(d -> {
            TblAlarmInfo tblAlarmInfo = this.getById(d);
            if (null == tblAlarmInfo || REMOVED == tblAlarmInfo.getPhaseStatus()) {
                throw new WebSystemException(ErrorCode.RULE_NOT_EXIST, ErrorLevel.INFO);
            }
            tblAlarmInfo.setPhaseStatus(alarmMarkResolved.getPhaseStatus());
//            tblAlarmInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            if (!this.updateById(tblAlarmInfo)) {
                log.info("update alarm rules phaseStatus failed!");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        });
        AlarmInfoBaseResp alarmInfoBaseResp = new AlarmInfoBaseResp();
        alarmInfoBaseResp.setAlarmInfoId(String.join(",", alarmMarkResolved.getInfoIds()));
        return alarmInfoBaseResp;
    }
}

