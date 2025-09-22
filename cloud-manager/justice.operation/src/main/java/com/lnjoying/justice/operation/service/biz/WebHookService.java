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

package com.lnjoying.justice.operation.service.biz;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.HttpContextUtils;
import com.lnjoying.justice.operation.common.constant.AlarmElementUnitOperator;
import com.lnjoying.justice.operation.common.constant.AlertStatus;
import com.lnjoying.justice.operation.domain.dto.request.WebhookInfoReq;
import com.lnjoying.justice.operation.entity.*;
import com.lnjoying.justice.operation.service.*;
import com.lnjoying.justice.operation.service.biz.NotificationBiz.NotificationFactory;
import com.micro.core.common.Utils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service
@Slf4j
public class WebHookService {

    @Autowired
    private TblAlarmInfoService tblAlarmInfoService;

    @Autowired
    private TblAlarmRuleReceiverService tblAlarmRuleReceiverService;

    @Autowired
    private TblAlarmRuleResourceService tblAlarmRuleResourceService;

    @Autowired
    private TblAlarmRuleService tblAlarmRuleService;

    @Autowired
    private TblReceiverService tblReceiverService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private NotificationFactory notificationFactory;

    public void handleInstanceAlarm(WebhookInfoReq req) throws WebSystemException {
        if (req.getAlerts().isEmpty()) {
            log.info("instance webhook, no alerts");
            return;
        }
        WebhookInfoReq.Alert alert = req.getAlerts().get(0);
        handleAlert(alert);
    }

    private Message handleAlert(WebhookInfoReq.Alert alert) {
        String alarmRuleId = alert.getAnnotations().getRuleId();
        TblAlarmRule tblAlarmRule = tblAlarmRuleService.getById(alarmRuleId);
        if (null == tblAlarmRule || REMOVED == tblAlarmRule.getPhaseStatus()) {
            log.info("instance webhook, no alarm rule, ruleId:{}", alarmRuleId);
            return null;
        }

        //用于资源类型+监控类型如CPU
        TblAlarmRuleResource tblAlarmRuleResource = tblAlarmRuleResourceService.getOne(new LambdaQueryWrapper<TblAlarmRuleResource>().eq(TblAlarmRuleResource::getRuleId, alarmRuleId).ne(TblAlarmRuleResource::getPhaseStatus, REMOVED).last("limit 1"));

        //当前用户
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userId = request.getHeader("X-UserId");

        LambdaQueryWrapper<TblAlarmInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TblAlarmInfo::getRuleId, alarmRuleId)
                .eq(TblAlarmInfo::getSummeryInfo, alert.getAnnotations().getSummary());

        if (AlertStatus.RESOLVED.getStatus().equals(alert.getStatus())) {
            if (alert.getAnnotations().getDescription().contains(":9100"))
            {
                String newDetailInfo = alert.getAnnotations().getDescription().replace(":9100","");
                alert.getAnnotations().setDescription(newDetailInfo);
//                tblAlarmInfo.setDetailInfo(newDetailInfo);
            }
            queryWrapper.eq(TblAlarmInfo::getPhaseStatus, AlertStatus.RESOLVED.getValue());
            queryWrapper.eq(TblAlarmInfo::getDetailInfo, alert.getAnnotations().getDescription());
            queryWrapper.last("limit 1");
            TblAlarmInfo tblAlarmInfo = tblAlarmInfoService.getOne(queryWrapper);

            if (null == tblAlarmInfo){
                tblAlarmInfo = new TblAlarmInfo();
                tblAlarmInfo.setInfoId(Utils.assignUUId());
                tblAlarmInfo.setDetailInfo(alert.getAnnotations().getDescription());
                tblAlarmInfo.setAlarmCount(1);
                tblAlarmInfo.setAlarmElement(tblAlarmRuleResource.getAlarmElement());
                tblAlarmInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                tblAlarmInfo.setUpdateTime(tblAlarmInfo.getCreateTime());
                tblAlarmInfo.setInterval(tblAlarmRule.getInterval());
                tblAlarmInfo.setRuleId(alarmRuleId);
                tblAlarmInfo.setPhaseStatus(AlertStatus.RESOLVED.getValue());
                tblAlarmInfo.setSummeryInfo(alert.getAnnotations().getSummary());
                tblAlarmInfo.setTriggerBehavior( alert.getAnnotations().getThreshold() + tblAlarmRuleResource.getUnit());
                tblAlarmInfo.setLevel(0);
                tblAlarmInfo.setResourceType(tblAlarmRuleResource.getResourceType());
                tblAlarmInfo.setUserId(userId);
                tblAlarmInfoService.save(tblAlarmInfo);
            }else {
                //用当前时间与上次回复时间做比较，！
                long trigger = DateUtil.between(tblAlarmInfo.getUpdateTime(), new Date(), DateUnit.MINUTE, false);
                //下次恢复时间
                if (trigger <= 20) {
                    log.info("Not reaching the recovery time！Current ruleid:{},Differences from last time:{} minutes,", tblAlarmInfo.getRuleId(), trigger);
                    return null;
                }

                tblAlarmInfo.setTriggerBehavior(alert.getAnnotations().getThreshold() + tblAlarmRuleResource.getUnit());
                tblAlarmInfo.setInterval(tblAlarmRule.getInterval());
                tblAlarmInfo.setLevel(tblAlarmRule.getLevel());
                tblAlarmInfo.setResourceType(tblAlarmRuleResource.getResourceType());
                tblAlarmInfo.setAlarmElement(tblAlarmRuleResource.getAlarmElement());
                tblAlarmInfo.setAlarmCount(tblAlarmInfo.getAlarmCount() + 1);
                tblAlarmInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                tblAlarmInfoService.updateById(tblAlarmInfo);
            }

            return null;
        } else {
            //公用部分
            TblAlarmInfo newTblAlarmInfo = new TblAlarmInfo();
            newTblAlarmInfo.setRuleId(alarmRuleId);
            newTblAlarmInfo.setSummeryInfo(alert.getAnnotations().getSummary());
            newTblAlarmInfo.setAlarmCount(1);
            newTblAlarmInfo.setDetailInfo(alert.getAnnotations().getDescription());
            if (alert.getAnnotations().getDescription().contains(":9100"))
            {
                String newDetailInfo = alert.getAnnotations().getDescription().replace(":9100","");
                alert.getAnnotations().setDescription(newDetailInfo);
                newTblAlarmInfo.setDetailInfo(newDetailInfo);
            }
            newTblAlarmInfo.setPhaseStatus(AlertStatus.FIRING.getValue());
            newTblAlarmInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            newTblAlarmInfo.setUpdateTime(newTblAlarmInfo.getCreateTime());
            newTblAlarmInfo.setTriggerBehavior(alert.getAnnotations().getThreshold() + tblAlarmRuleResource.getUnit());
            newTblAlarmInfo.setInterval(tblAlarmRule.getInterval());
            newTblAlarmInfo.setLevel(tblAlarmRule.getLevel());
            newTblAlarmInfo.setResourceType(tblAlarmRuleResource.getResourceType());
            newTblAlarmInfo.setAlarmElement(tblAlarmRuleResource.getAlarmElement());
            newTblAlarmInfo.setUserId(userId);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime beginOfDay = LocalDateTimeUtil.beginOfDay(now);
            LocalDateTime endOfDay = LocalDateTimeUtil.endOfDay(now);
            queryWrapper.ne(TblAlarmInfo::getPhaseStatus, AlertStatus.RESOLVED.getValue());

//            queryWrapper.eq(TblAlarmInfo::getPhaseStatus, AlertStatus.FIRING.getValue());
            String pattern = "(instance: .*)(\\s+.*)";
            Matcher matcher = Pattern.compile(pattern).matcher(alert.getAnnotations().getDescription());
            if (matcher.find( ))
            {
                queryWrapper.likeRight(TblAlarmInfo::getDetailInfo,matcher.group(1));
                log.info("instance webhook, matcher:{}", matcher.group(1));
            }
            //
            if (tblAlarmInfoService.count(queryWrapper) > 0) {
                log.info("instance webhook, alarm info already exists, ruleId:{}", alarmRuleId);
                queryWrapper.orderByDesc(TblAlarmInfo::getUpdateTime);
                queryWrapper.last("limit 1");
                TblAlarmInfo tblAlarmInfo = tblAlarmInfoService.getOne(queryWrapper);
                //=============================================================================
                //=============================================================================
                //用报警间隔和第一次创建时间进行比较，获得下一次触发【告警时间】
                DateTime notificationTime = DateUtil.offsetMinute(tblAlarmInfo.getUpdateTime(), tblAlarmRule.getInterval());
                //判断这次的通知，用【当前时间】去比较上面【告警时间】是否到达触发的间隔！
                long trigger = DateUtil.between(notificationTime, new Date(), DateUnit.MINUTE, false);

                //没到下次触发时间【误差4分钟】
//                if (trigger < -4) {
                if (trigger < 0) {
                    log.info("Not reaching the alarm time！Current ruleid:{},Next Trigger Time:{},", tblAlarmInfo.getRuleId(), notificationTime);
                    return null;
                }
                //=============================================================================
                //=============================================================================
                //判断相同ruleid的告警数据，是否相同
                queryWrapper.clear();
                queryWrapper.eq(TblAlarmInfo::getRuleId, alarmRuleId)
                        .ne(TblAlarmInfo::getPhaseStatus, AlertStatus.RESOLVED.getValue())
                        .eq(TblAlarmInfo::getSummeryInfo, alert.getAnnotations().getSummary())
                        .eq(TblAlarmInfo::getDetailInfo, alert.getAnnotations().getDescription())
                        .ge(TblAlarmInfo::getUpdateTime, beginOfDay)
                        .le(TblAlarmInfo::getUpdateTime, endOfDay);
                if (tblAlarmInfoService.count(queryWrapper) > 0) {
                    log.info("instance webhook, detail_info already exists:{}", alert.getAnnotations().getDescription());
                    queryWrapper.last("limit 1");
                    queryWrapper.orderByDesc(TblAlarmInfo::getUpdateTime);
                    tblAlarmInfo = tblAlarmInfoService.getOne(queryWrapper);
                    tblAlarmInfo.setTriggerBehavior(alert.getAnnotations().getThreshold() + tblAlarmRuleResource.getUnit());
                    tblAlarmInfo.setAlarmCount(tblAlarmInfo.getAlarmCount() + 1);
                    tblAlarmInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    tblAlarmInfo.setInterval(tblAlarmRule.getInterval());
                    tblAlarmInfo.setLevel(tblAlarmRule.getLevel());
                    tblAlarmInfo.setResourceType(tblAlarmRuleResource.getResourceType());
                    tblAlarmInfo.setAlarmElement(tblAlarmRuleResource.getAlarmElement());
                    tblAlarmInfoService.updateById(tblAlarmInfo);
                    Message message = new Message();
                    message.setSummery(tblAlarmInfo.getSummeryInfo());
                    message.setDescription(tblAlarmInfo.getDetailInfo());
                    message.setRuleId(tblAlarmInfo.getRuleId());
                    message.setNotice(tblAlarmRule.getNotice());
                    return message;
                }
               // if (alert.getAnnotations().getDescription().equals(tblAlarmInfo.getDetailInfo()))
            }
            tblAlarmInfoService.save(newTblAlarmInfo);
            Message message = new Message();
            message.setSummery(newTblAlarmInfo.getSummeryInfo());
            message.setDescription(newTblAlarmInfo.getDetailInfo());
            message.setRuleId(newTblAlarmInfo.getRuleId());
            message.setNotice(tblAlarmRule.getNotice());
            return message;
        }
    }

    @SneakyThrows
    public void handleInstanceGroupAlarm(WebhookInfoReq req) throws WebSystemException {
        if (req.getAlerts().isEmpty()) {
            log.info("instance webhook, no alerts");
            return;
        }
//        Message message = new Message();
        Map<String, Object> summery = new HashMap<>(3);
        Map<String, Object> description = new HashMap<>(3);
        HashSet<String> ruleSet = new HashSet<>();
        Boolean notice = null;
        for (WebhookInfoReq.Alert alert : req.getAlerts()) {
            Message tmp = handleAlert(alert);
            if (null == tmp) continue;
            if (description.containsKey(tmp.getRuleId())) {
                //summery.put(tmp.getRuleId(), summery.get(tmp.getRuleId()) + "@" + tmp.getSummery());
                description.put(tmp.getRuleId(), description.get(tmp.getRuleId()) + "@" + tmp.getDescription());
            } else {
                //summery.put(tmp.getRuleId(), tmp.getSummery());
                description.put(tmp.getRuleId(), tmp.getDescription());
            }
            if (tmp.getNotice())
                ruleSet.add(tmp.getRuleId());
        }

        if (ruleSet.isEmpty()) {
            return;
        } else {
            sendAlarmInfos(summery, description, ruleSet);
            description.clear();
            ruleSet.clear();
        }

    }

    public void sendAlarmInfos( Map<String, Object> summeryInfo, Map<String, Object> detailInfo, HashSet<String> ruleSet) {
        if (ruleSet.size() <= 0) return;
        LambdaQueryWrapper<TblAlarmRuleReceiver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(TblAlarmRuleReceiver::getReceiverId, TblAlarmRuleReceiver::getRuleId)
                .in(TblAlarmRuleReceiver::getRuleId, ruleSet)
                .ne(TblAlarmRuleReceiver::getPhaseStatus, REMOVED);

        List<TblAlarmRuleReceiver> receiverIds = tblAlarmRuleReceiverService.list(queryWrapper);
        if (receiverIds.isEmpty()) {
            log.info("instance webhook, no alarm rule receiver, ruleId:{}", ruleSet);
            return;
        }
        receiverIds.forEach(d -> {
            TblReceiver tblReceiver = tblReceiverService.getById(d.getReceiverId());
            TblAlarmRuleResource tblAlarmRuleResource = tblAlarmRuleResourceService.getOne(new LambdaQueryWrapper<TblAlarmRuleResource>().eq(TblAlarmRuleResource::getRuleId, d.getRuleId()).ne(TblAlarmRuleResource::getPhaseStatus, REMOVED).last("limit 1"));
            Notification notification = notificationFactory.createNotificationService(tblReceiver.getType());
            String[] contacts = tblReceiver.getContactInfo().split(",");
            notification.send(contacts, detailInfo.get(d.getRuleId()).toString(), AlarmElementUnitOperator.getSubjectByValue(tblAlarmRuleResource.getAlarmElement()));
            //notification.send(contacts, summeryInfo.get(d.getRuleId()).toString(), detailInfo.get(d.getRuleId()).toString(), AlarmElementUnitOperator.getSubjectByValue(tblAlarmRuleResource.getAlarmElement()));
        });
    }

    // 两分钟的误差内，收到了alertmanager 的信息，都会返回true
    public boolean twoMinuteError(long interval, long firstTime) {
        long current = System.currentTimeMillis();
        long difference = current - firstTime;
        if (difference % interval <= 2 * 60 * 1000) {
            return true;
        } else return difference % interval >= (interval - 2 * 60 * 1000);
    }

    //

    @Data
    class Message {
        String summery;
        String description;
        String ruleId;
        Boolean notice;
    }

}
