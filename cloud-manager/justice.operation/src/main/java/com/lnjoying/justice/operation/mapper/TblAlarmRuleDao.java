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
import com.lnjoying.justice.operation.domain.dto.response.AlarmRuleDetailsResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRuleResp;
import com.lnjoying.justice.operation.entity.TblAlarmRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * (TblAlarmRule)表数据库访问层
 *
 * @author Lis
 * @since 2023-05-19 17:47:53
 */
public interface TblAlarmRuleDao extends BaseMapper<TblAlarmRule> {

    List<AlarmRuleResp> selectAlaRuPage(IPage<AlarmRuleResp> page, @Param("name") String name, @Param("resourceType") Integer resourceType, @Param("userId") String userId);

    List<AlarmRuleResp> selectCountAlaRu(@Param("name") String name, @Param("resourceType") Integer resourceType, @Param("userId") String userId);

    List<AlarmRuleDetailsResp> getAlarmRuleById(@Param("ruleId") String ruleId, @Param("userId") String userId);

    //
    //判断是否存在重复数据
    @Select("SELECT COUNT(1) FROM tbl_alarm_rule tar " +
            "LEFT JOIN tbl_alarm_rule_resource tarru ON tarru.rule_id = tar.rule_id " +
            "WHERE tar.expr = #{expr} " +
            "AND tar.user_id = #{userId} " +
            "AND tar.duration_time = #{durationTime} " +
            "AND tar.phase_status != -1 " +
            "AND tarru.phase_status != -1 " +
            "AND tarru.alarm_element = #{alarmElement} " +
            "AND tarru.resource_id = #{resourceId}")
    Integer getSameAlarmRule(@Param("alarmElement") Integer alarmElement, @Param("expr") String expr, @Param("userId") String userId, @Param("resourceId") String resourceId, @Param("durationTime") Integer durationTime);
}

