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


import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.domain.dto.request.AlarmRuleReq;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRuleBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRuleDetailsResp;
import com.lnjoying.justice.operation.domain.dto.response.AlarmRulesResp;
import com.lnjoying.justice.operation.service.TblAlarmRuleService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * (TblAlarmRule)表控制层
 * 运维管理--报警设置--报警器
 *
 * @author Lis
 * @since 2023-05-19 17:47:53
 */
@Slf4j
@RestController
@RequestMapping("/operation/v1")
public class TblAlarmRuleController {
    /**
     * 服务对象
     */
    @Resource
    private TblAlarmRuleService tblAlarmRuleService;

    /**
     * @param: description
     * @param: pageSize
     * @param: pageNum
     * @param: userId
     * @description: TODO   运维管理--报警器--分页查询
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/22
     */
    @ApiOperation(value = "运维管理--报警器--分页查询", response = Object.class)
    @GetMapping("/alarm-rules")
    public AlarmRulesResp selectAlarmRulePage(@ApiParam(name = "name") @RequestParam(required = false, value = "name") String name,
                                              @ApiParam(name = "resource_type") @RequestParam(required = false, value = "resource_type") Integer resourceType,
                                              @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size", defaultValue = "100") Integer pageSize,
                                              @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num", defaultValue = "1") Integer pageNum,
                                              @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            return tblAlarmRuleService.selectAlaRuPage(name, resourceType, pageSize, pageNum, userId);
        } catch (Exception e) {
            log.error("get alarm rules failed: {}", e.getMessage());
            throw throwWebException(e);
        }

    }


    /**
     * @param: tblAlarmRuleReq
     * @param: userId
     * @description: TODO   运维管理--报警器--新增数据
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/23
     */
    @LogAnnotation(description = "新增报警器【名称：{}】", resource = "运维管理--报警设置", obtainParameter = "name")
    @ApiOperation(value = "运维管理-报警设置-新增报警器", response = Object.class)
    @PostMapping("/alarm-rules")
    public AlarmRuleBaseResp addAlarmRule(@ApiParam(value = "tblAlarmRule", required = true, name = "tblAlarmRule") @RequestBody @Valid AlarmRuleReq tblAlarmRuleReq,
                                          @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            return this.tblAlarmRuleService.addAla(tblAlarmRuleReq, userId);
        } catch (Exception e) {
            log.error("create alarm rules failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    /**
     * @param: id
     * @description: TODO   通过主键查询单条数据
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/24
     */
    @ApiOperation(value = "运维管理-报警设置-通过主键查询单条数据", response = Object.class)
    @GetMapping("/alarm-rules/{ruleId}")
    public AlarmRuleDetailsResp getAlarmRuleById(@ApiParam(value = "ruleId", required = true, name = "ruleId") @PathVariable("ruleId") String ruleId,
                                                 @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            return this.tblAlarmRuleService.getAlarmRuleById(ruleId, userId);
        } catch (Exception e) {
            log.error("get alarm rules failed: {}", e.getMessage());
            throw throwWebException(e);
        }

    }


    @ApiOperation(value = "运维管理--报警设置--编辑报警器", response = Object.class)
    @PutMapping("/alarm-rules/{ruleId}")
    @LogAnnotation(description = "编辑报警器【id：{}】", resource = "运维管理-报警设置", obtainParameter = "ruleId")
    public AlarmRuleBaseResp updateAlarmRule(@ApiParam(value = "ruleId", required = true, name = "ruleId") @PathVariable("ruleId") String ruleId,
                                             @ApiParam(value = "tblAlarmRule", required = true, name = "tblAlarmRule") @RequestBody @Valid AlarmRuleReq tblAlarmRuleReq,
                                             @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            log.info("update alarm rules tblAlarmRule: {}, userId:{}", tblAlarmRuleReq, userId);
            return this.tblAlarmRuleService.updateAlarmRule(ruleId, tblAlarmRuleReq, userId);
        } catch (Exception e) {
            log.error("update alarm rules failed: {}", e.getMessage());
            throw throwWebException(e);
        }

    }

    @ApiOperation(value = "运维管理--报警设置--删除报警器", response = Object.class)
    @DeleteMapping("/alarm-rules/{ruleId}")
    @LogAnnotation(description = "删除报警器【id：{}】", resource = "运维管理-报警设置", obtainParameter = "ruleId")
    public AlarmRuleBaseResp removeAlarmRule(@ApiParam(value = "ruleId", required = true, name = "ruleId") @PathVariable("ruleId") String ruleId) {

        try {
            log.info("del alarm rules ruleId: {}", ruleId);
            return this.tblAlarmRuleService.removeAlarmRule(ruleId);
        } catch (Exception e) {
            log.error("del alarm rules failed: {}", e.getMessage());
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

