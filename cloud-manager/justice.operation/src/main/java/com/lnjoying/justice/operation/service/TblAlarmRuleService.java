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
import com.lnjoying.justice.operation.domain.dto.request.AlarmRuleReq;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRuleBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRuleDetailsResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRulesResp;
import com.lnjoying.justice.operation.entity.TblAlarmRule;

import javax.validation.Valid;

/**
 * (TblAlarmRule)表服务接口
 *
 * @author Lis
 * @since 2023-05-19 17:47:53
 */
public interface TblAlarmRuleService extends IService<TblAlarmRule> {

    //运维管理-报警设置-新增报警器
    AlarmRuleBaseResp addAla(@Valid AlarmRuleReq tblAlarmRuleReq, String userId);

    //运维管理--报警器--分页查询
    AlarmRulesResp selectAlaRuPage(String name, Integer resourceType, Integer pageSize, Integer pageNum, String userId);

    //运维管理--报警设置--编辑报警器
    AlarmRuleBaseResp updateAlarmRule(String ruleId, AlarmRuleReq tblAlarmRuleReq, String userId);

    //运维管理--报警设置--删除报警器
    AlarmRuleBaseResp removeAlarmRule(String ruleId);

    //通过主键查询单条数据
    AlarmRuleDetailsResp getAlarmRuleById(String ruleId, String userId);
}

