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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.common.constant.PhaseStatus;
import com.lnjoying.justice.operation.domain.dto.response.ReceiverBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.ReceiversResp;
import com.lnjoying.justice.operation.entity.TblAlarmRuleReceiver;
import com.lnjoying.justice.operation.entity.TblReceiver;
import com.lnjoying.justice.operation.mapper.TblReceiverDao;
import com.lnjoying.justice.operation.service.TblAlarmRuleReceiverService;
import com.lnjoying.justice.operation.service.TblReceiverService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

/**
 * (TblReceiver)表服务实现类
 *
 * @author Lis
 * @since 2023-05-19 17:48:36
 */
@Slf4j
@Service("tblReceiverService")
public class TblReceiverServiceImpl extends ServiceImpl<TblReceiverDao, TblReceiver> implements TblReceiverService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$");
    @Autowired
    private TblAlarmRuleReceiverService ruleReceiverService;

    @Override
    public ReceiverBaseResp addRec(TblReceiver tblReceiver, String userId) {

        //判断，添加的多个邮箱是否存在重复
        boolean hasDuplicates = tblReceiver.getContactInfos().stream().distinct().count() != tblReceiver.getContactInfos().size();
        if (hasDuplicates) {
            log.info("Multiple mailboxes have duplicate data！");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        //手机号，邮箱过滤验真
        tblReceiver.getContactInfos().forEach(t -> {
            if (tblReceiver.getType() == 0) {
                if (!EMAIL_PATTERN.matcher(t).matches())
                    ExceptionUtil.wrapRuntime(t + "：Please enter the correct email address！");

            } else {
                if (!PHONE_PATTERN.matcher(t).matches())
                    ExceptionUtil.wrapRuntime(t + "：Please enter the correct phone number！");
            }
        });

        tblReceiver.setReceiverId(Utils.assignUUId());
        tblReceiver.setContactCount(tblReceiver.getContactInfos().size());
        tblReceiver.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblReceiver.setUserId(userId);
        tblReceiver.setPhaseStatus(PhaseStatus.CREATED);
        tblReceiver.setContactInfo(String.join(",", tblReceiver.getContactInfos()));

        if (!this.save(tblReceiver)) {
            log.info("create receiver failed!");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        ReceiverBaseResp baseResp = new ReceiverBaseResp();
        baseResp.setReceiverId(tblReceiver.getReceiverId());
        return baseResp;
    }


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

    @Override
    public ReceiversResp selectRecPage(String name, String description, Integer pageSize, Integer pageNum, String userId) {

//        IPage<TblReceiver> page = new Page<>(pageSize, pageNum);

        LambdaQueryWrapper<TblReceiver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(description), TblReceiver::getDescription, description);
        queryWrapper.like(StrUtil.isNotBlank(name), TblReceiver::getName, name);
        queryWrapper.eq(TblReceiver::getUserId, userId);
        queryWrapper.ne(TblReceiver::getPhaseStatus, REMOVED);
        ReceiversResp receiversResp = new ReceiversResp();
        long totalNum = this.count(queryWrapper);
        receiversResp.setTotalNum(totalNum);
        if (0 == totalNum) {
            return receiversResp;
        }
        IPage<TblReceiver> page = new Page<>(pageNum, pageSize);
        queryWrapper.orderByDesc(TblReceiver::getCreateTime);
        // 调用分页查询方法
        IPage<TblReceiver> result = this.page(page, queryWrapper);

        result.getRecords().forEach(d -> {
            if (StrUtil.isNotBlank(d.getContactInfo())) {
                d.setContactInfos(Arrays.asList(d.getContactInfo().split(",")));
            }
        });
        receiversResp.setAlarmRules(result.getRecords());

        return receiversResp;
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
    @Override
    public ReceiverBaseResp updateReceiver(String receiverId, TblReceiver tblReceiver, String userId) {
        TblReceiver receiver = this.getById(receiverId);
        if (null == receiver) {
            throw new WebSystemException(ErrorCode.RECEIVER_NOT_EXIST, ErrorLevel.INFO);
        }

        //判断，添加的多个邮箱是否存在重复
        boolean hasDuplicates = tblReceiver.getContactInfos().stream().distinct().count() != tblReceiver.getContactInfos().size();
        if (hasDuplicates) {
            log.info("Multiple mailboxes have duplicate data！");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        //手机号，邮箱过滤验真
        tblReceiver.getContactInfos().forEach(t -> {
            if (tblReceiver.getType() == 0) {
                if (!EMAIL_PATTERN.matcher(t).matches())
                    ExceptionUtil.wrapRuntime(t + "：Please enter the correct email address！");

            } else {
                if (!PHONE_PATTERN.matcher(t).matches())
                    ExceptionUtil.wrapRuntime(t + "：Please enter the correct phone number！");
            }
        });

        tblReceiver.setContactInfo(String.join(",", tblReceiver.getContactInfos()));
        tblReceiver.setContactCount(tblReceiver.getContactInfos().size());
        tblReceiver.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblReceiver.setReceiverId(receiver.getReceiverId());
        if (!this.updateById(tblReceiver)) {
            log.info("create receiver failed!");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        ReceiverBaseResp baseResp = new ReceiverBaseResp();
        baseResp.setReceiverId(tblReceiver.getReceiverId());

        return baseResp;
    }

    /**
     * @param: receiverId
     * @description: TODO   运维管理--报警设置--通知对象--删除数据
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/26
     */
    @Override
    public ReceiverBaseResp removeReceiver(String receiverId) {
        TblReceiver receiver = this.getById(receiverId);
        if (null == receiver || REMOVED == receiver.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }
        long ruleReceiverCount = ruleReceiverService.count(new LambdaQueryWrapper<TblAlarmRuleReceiver>().eq(TblAlarmRuleReceiver::getReceiverId, receiverId));
        if (ruleReceiverCount > 0) {
            throw new WebSystemException(ErrorCode.RULE_RECEIVER_IS_USING, ErrorLevel.INFO);
        }
        receiver.setPhaseStatus(REMOVED);
        receiver.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));

        if (!this.updateById(receiver)) {
            log.info("del receiver failed!");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        ReceiverBaseResp baseResp = new ReceiverBaseResp();
        baseResp.setReceiverId(receiver.getReceiverId());
        return baseResp;
    }

    /**
     * @param: id
     * @description: TODO   通过主键查询单条数据
     * @return: com.lnjoying.justice.operation.entity.TblReceiver
     * @author: LiSen
     * @date: 2023/6/12
     */
    @Override
    public TblReceiver getSelectOneById(String id, String userId) {
        TblReceiver tblReceiver = this.getOne(new LambdaQueryWrapper<TblReceiver>().eq(TblReceiver::getReceiverId, id).eq(TblReceiver::getUserId, userId).ne(TblReceiver::getPhaseStatus, REMOVED));
        if (null == tblReceiver) {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(tblReceiver.getContactInfo())) {
            tblReceiver.setContactInfos(Arrays.asList(tblReceiver.getContactInfo().split(",")));
        }
        return tblReceiver;
    }
}

