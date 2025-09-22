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
import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.domain.dto.response.ReceiverBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.ReceiversResp;
import com.lnjoying.justice.operation.entity.TblReceiver;
import com.lnjoying.justice.operation.service.TblReceiverService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * (TblReceiver)表控制层
 * 运维管理--报警设置--通知对象
 *
 * @author Lis
 * @since 2023-05-19 17:48:36
 */
@Slf4j
@RestController
@RequestMapping("/operation/v1")
public class TblReceiverController {
    /**
     * 服务对象
     */
    @Resource
    private TblReceiverService tblReceiverService;


    /**
     * @param: description
     * @param: pageSize
     * @param: pageNum
     * @param: userId
     * @description: TODO   运维管理--报警设置--通知对象--分页查询
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/22
     */
    @ApiOperation(value = "运维管理--报警设置--通知对象--分页查询", response = Object.class)
    @GetMapping("/receivers")
    @ResponseBody
    public ReceiversResp selectReceiverPage(@ApiParam(name = "name") @RequestParam(required = false, value = "name") String name,
                                            @ApiParam(name = "description") @RequestParam(required = false, value = "description") String description,
                                            @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size", defaultValue = "100") Integer pageSize,
                                            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num", defaultValue = "1") Integer pageNum,
                                            @RequestHeader(name = "X-UserId", required = false) String userId
    ) {

        try {
            return tblReceiverService.selectRecPage(name, description, pageSize, pageNum, userId);
        } catch (Exception e) {
            log.error("get receivers failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    /**
     * @param: tblReceiver
     * @description: TODO   运维管理--报警设置--通知对象--新增数据
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/22
     */
    @ApiOperation(value = "运维管理--报警设置--通知对象--新增数据", response = Object.class)
    @PostMapping("/receivers")
    @LogAnnotation(description = "新增通知对象【名称：{}，配置：{}】", resource = "运维管理-报警设置", obtainParameter = "name,contactInfos")
    public ReceiverBaseResp insert(@ApiParam(value = "tblReceiver", required = true, name = "tblReceiver") @RequestBody @Valid TblReceiver tblReceiver,
                                   @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            Console.log("create receivers device: {}, userId:{}", tblReceiver, userId);
            return this.tblReceiverService.addRec(tblReceiver, userId);
        } catch (Exception e) {
            log.error("create receivers failed: {}", e.getMessage());
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
    @ApiOperation(value = "运维管理-报警设置--通知对象-通过主键查询单条数据", response = Object.class)
    @GetMapping("/receivers/{id}")
    public TblReceiver selectOne(@ApiParam(value = "id", required = true, name = "id") @PathVariable("id") String id,
                                 @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            return this.tblReceiverService.getSelectOneById(id, userId);
        } catch (Exception e) {
            log.error("get receivers failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    /**
     * @param: receiverId
     * @param: tblReceiver
     * @param: userId
     * @description: TODO   运维管理--报警设置--通知对象--编辑数据
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/26
     */

    @ApiOperation(value = "运维管理--报警设置--通知对象--编辑数据", response = Object.class)
    @PutMapping("/receivers/{receiverId}")
    @LogAnnotation(description = "编辑通知对象【名称：{}，配置：{}】", resource = "运维管理-报警设置", obtainParameter = "name,contactInfos")
    public ReceiverBaseResp updateReceiver(@ApiParam(value = "receiverId", required = true, name = "receiverId") @PathVariable("receiverId") String receiverId,
                                           @ApiParam(value = "tblReceiver", required = true, name = "tblReceiver") @RequestBody @Valid TblReceiver tblReceiver,
                                           @RequestHeader(name = "X-UserId", required = false) String userId) {

        try {
            Console.log("put receivers device: {}, userId:{}", tblReceiver, userId);
            return this.tblReceiverService.updateReceiver(receiverId, tblReceiver, userId);
        } catch (Exception e) {
            log.error("receivers failed: {}", e.getMessage());
            throw throwWebException(e);
        }


    }


    @ApiOperation(value = "运维管理--报警设置--通知对象--删除数据", response = Object.class)
    @DeleteMapping("/receivers/{receiverId}")
    @LogAnnotation(description = "删除通知对象【id：{}】", resource = "运维管理-报警设置", obtainParameter = "receiverId")
    public ReceiverBaseResp removeReceiver(@ApiParam(value = "receiverId", required = true, name = "receiverId") @PathVariable("receiverId") String receiverId) {
        try {
            return this.tblReceiverService.removeReceiver(receiverId);
        } catch (Exception e) {
            log.error("receivers failed: {}", e.getMessage());
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

