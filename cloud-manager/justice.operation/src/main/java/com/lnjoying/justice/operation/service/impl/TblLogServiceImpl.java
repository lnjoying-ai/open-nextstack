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
import com.lnjoying.justice.operation.domain.dto.response.LogsResp;
import com.lnjoying.justice.operation.entity.TblLog;
import com.lnjoying.justice.operation.mapper.TblLogDao;
import com.lnjoying.justice.operation.service.TblLogService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 运维管理--日志(TblLog)表服务实现类
 *
 * @author Lis
 * @since 2023-05-23 21:01:04
 */
@Service("tblLogService")
public class TblLogServiceImpl extends ServiceImpl<TblLogDao, TblLog> implements TblLogService {

    /**
     * @param: timeRange
     * @param: pageSize
     * @param: pageNum
     * @param: userId
     * @description: TODO   运维管理--日志--分页查询
     * @return: org.springframework.http.ResponseEntity
     * @author: LiSen
     * @date: 2023/5/26
     */
    @Override
    public LogsResp selectLogPage(Long startTime, Long endTime, String description, Integer pageSize, Integer pageNum, String userId) {
        IPage<TblLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TblLog> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(TblLog::getUserId, userId);
        if (!StrUtil.isBlank(description))
        {
            queryWrapper.like(TblLog::getDescription, description);
        }
        queryWrapper.ge(0 != startTime, TblLog::getCreateTime, new Date(startTime));
        queryWrapper.le(0 != endTime, TblLog::getCreateTime, new Date(endTime));

        LogsResp logsResp = new LogsResp();
        long totalNum = this.count(queryWrapper);
        queryWrapper.orderByDesc(TblLog::getCreateTime);
        logsResp.setTotalNum(totalNum);
        if (0 == totalNum)
        {
            return  logsResp;
        }

        // 调用分页查询方法
        IPage<TblLog> result = this.page(page, queryWrapper);
        logsResp.setAlarmRules(result.getRecords());

        return logsResp;
    }
}

