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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lnjoying.justice.operation.domain.dto.response.EventsResp;
import com.lnjoying.justice.operation.entity.TblEvent;
import com.lnjoying.justice.operation.entity.search.EventSearchCritical;
import com.lnjoying.justice.operation.mapper.TblEventDao;
import com.lnjoying.justice.operation.service.TblEventService;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service("tblEventService")
public class TblEventServiceImpl extends ServiceImpl<TblEventDao, TblEvent> implements TblEventService
{
    @Override
    public EventsResp selectEventPage(String userId, EventSearchCritical eventSearchCritical)
    {
        LambdaQueryWrapper<TblEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(TblEvent::getPhaseStatus,REMOVED);
        if (!StrUtil.isBlank(eventSearchCritical.getContent()))
        {
            queryWrapper.like(TblEvent::getContent, eventSearchCritical.getContent());
        }
        if (!StrUtil.isBlank(eventSearchCritical.getDetailInfo()))
        {
            queryWrapper.like(TblEvent::getDetailInfo, eventSearchCritical.getDetailInfo());
        }
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(TblEvent::getUserId, userId);
        }
//        if (null == eventSearchCritical.getStartTime() || 0 == eventSearchCritical.getStartTime())
//        {
////            queryWrapper.ge(TblEvent::getCreateTime,DateUtil.lastWeek());
//            log.warn("startTime is null");
//        }
//        else
        if (null != eventSearchCritical.getStartTime() && 0 < eventSearchCritical.getStartTime())
        {
            //Long 转换成 Date
            Date startTime = new Date(eventSearchCritical.getStartTime());
            queryWrapper.ge(TblEvent::getCreateTime, startTime);
        }
        if (null == eventSearchCritical.getEndTime() || 0 == eventSearchCritical.getEndTime())
        {
            queryWrapper.le(TblEvent::getCreateTime, new Date(System.currentTimeMillis()));
        }
        else
        {
            Date endTime = new Date(eventSearchCritical.getEndTime());
            queryWrapper.le(TblEvent::getCreateTime, endTime);
        }
        long totalNum = this.count(queryWrapper);
        EventsResp eventsResp = new EventsResp();
        eventsResp.setTotalNum(totalNum);
        if (0 == totalNum)
        {
            return  eventsResp;
        }

        queryWrapper.orderByDesc(TblEvent::getCreateTime);
        IPage<TblEvent> page = new Page<>(eventSearchCritical.getPageNum(), eventSearchCritical.getPageSize());
        queryWrapper.orderByDesc(TblEvent::getCreateTime);
        // 调用分页查询方法
        IPage<TblEvent> result = this.page(page, queryWrapper);
//        List<TblEvent> tblEvents = this.list(queryWrapper);
        eventsResp.setEvents(result.getRecords());
        return eventsResp;
    }
}
