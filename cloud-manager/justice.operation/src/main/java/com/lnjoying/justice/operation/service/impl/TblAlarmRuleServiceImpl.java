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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.common.constant.*;
import com.lnjoying.justice.operation.config.OperationConfig;
import com.lnjoying.justice.operation.domain.dto.request.AlarmRuleReq;
import com.lnjoying.justice.operation.domain.dto.response.*;
import com.lnjoying.justice.operation.entity.TblAlarmRule;
import com.lnjoying.justice.operation.entity.TblAlarmRuleReceiver;
import com.lnjoying.justice.operation.entity.TblAlarmRuleResource;
import com.lnjoying.justice.operation.entity.TblReceiver;
import com.lnjoying.justice.operation.mapper.TblAlarmRuleDao;
import com.lnjoying.justice.operation.service.TblAlarmRuleReceiverService;
import com.lnjoying.justice.operation.service.TblAlarmRuleResourceService;
import com.lnjoying.justice.operation.service.TblAlarmRuleService;
import com.lnjoying.justice.operation.service.TblReceiverService;
import com.lnjoying.justice.operation.service.biz.CombRpcSerice;
import com.lnjoying.justice.operation.utils.AlarmRuleParameters;
import com.lnjoying.justice.operation.utils.TemplateUtils;
import com.lnjoying.justice.operation.utils.TsQueryParameters;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;
import static com.lnjoying.justice.operation.common.constant.PrometheusConfig.RULE_FILE_PATH;

/**
 * (TblAlarmRule)表服务实现类
 *
 * @author Lis
 * @since 2023-05-19 17:47:53
 */
@Slf4j
@Service("tblAlarmRuleService")
public class TblAlarmRuleServiceImpl extends ServiceImpl<TblAlarmRuleDao, TblAlarmRule> implements TblAlarmRuleService {


    @Autowired
    private OperationConfig operationConfig;
    @Autowired
    private TblAlarmRuleReceiverService ruleReceiverService;
    @Autowired
    private TblAlarmRuleResourceService ruleResourceService;
    @Autowired
    private TblReceiverService receiverService;
    @Autowired
    private CombRpcSerice combRpcSerice;
    @Resource
    private TblAlarmRuleDao alarmRuleDao;

    /**
     * @param: tblAlarmRuleReq
     * @param: userId
     * @description: TODO   运维管理--报警器--新增数据
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/23
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlarmRuleBaseResp addAla(@Valid AlarmRuleReq tblAlarmRuleReq, String userId) {

        //监控 计算节点 判断当前用户权限
        if (tblAlarmRuleReq.getResourceType() == ResourceType.HYPERVISOR_NODE && !combRpcSerice.getUmsService().isAdminUser(userId)){
            log.error("current user is not admin，userId：" + userId);
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.INFO);
        }

        //赋值
        TblAlarmRule tblAlarmRule = new TblAlarmRule();
        TblAlarmRuleResource alarmRuleResource = new TblAlarmRuleResource();
        BeanUtils.copyProperties(tblAlarmRuleReq, tblAlarmRule);
        BeanUtils.copyProperties(tblAlarmRuleReq, alarmRuleResource);

        //判断是否存在重复--》报警器
        if (AlarmElementType.INSTANCE_HEALTH_STATUS ==tblAlarmRuleReq.getAlarmElement())
        {
            tblAlarmRule.setExpr("-");
            tblAlarmRule.setComparison(null);
            tblAlarmRule.setAlarmValue(null);
        }
        else
        {
            tblAlarmRule.setExpr(ComparisonOperator.getSymbolByValue(tblAlarmRuleReq.getComparison()) + tblAlarmRuleReq.getAlarmValue());
        }

        tblAlarmRuleReq.getResourceIds().forEach(resourceId -> {
            Integer tblAlarmRules = alarmRuleDao.getSameAlarmRule(tblAlarmRuleReq.getAlarmElement(), tblAlarmRule.getExpr(), userId, resourceId, tblAlarmRule.getDurationTime());
            if (tblAlarmRules > 0) {
                log.info("current alarm is present，resourceId：" + resourceId);
                throw new WebSystemException(ErrorCode.CURRENT_ALARM_IS_PRESENT, ErrorLevel.INFO);
            }
        });
//        if (ResourceType.HYPERVISOR_NODE == tblAlarmRuleReq.getResourceType())
//        {
//            List<String> nodeIpPorts = combRpcSerice.getVmService().getHypervisorNodeIpPorts(tblAlarmRuleReq.getResourceIds());
//            tblAlarmRuleReq.setResourceIds(nodeIpPorts);
//        }

        //入库报警器设置
        tblAlarmRule.setRuleId(Utils.assignUUId());
        tblAlarmRule.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblAlarmRule.setUserId(userId);
        tblAlarmRule.setPhaseStatus(PhaseStatus.CREATED);
        tblAlarmRule.setUpdateTime(tblAlarmRule.getCreateTime());

        if (!this.save(tblAlarmRule)) {
            log.info("create alarm rule failed!");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        //入库被控制对象与告警器的对应关系

        if (tblAlarmRuleReq.getResourceIds().size() > 0) {
            tblAlarmRuleReq.getResourceIds().forEach(d -> {
                alarmRuleResource.setAlarmRuleResourceId(Utils.assignUUId());
                alarmRuleResource.setResourceId(d);
                alarmRuleResource.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                alarmRuleResource.setRuleId(tblAlarmRule.getRuleId());
                alarmRuleResource.setPhaseStatus(PhaseStatus.CREATED);
                alarmRuleResource.setUnit(AlarmElementUnitOperator.getUnitByValue(tblAlarmRuleReq.getAlarmElement()));
                if (!ruleResourceService.save(alarmRuleResource)) {
                    log.info("create alarm rule resource failed!");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            });

        }

        //是否触发，通知对象--进行关联
        if (null != tblAlarmRuleReq.getNotice()) {
            if (tblAlarmRuleReq.getNotice()) {
                if (tblAlarmRuleReq.getReceiverList().size() == 0) {
                    log.info("receiverList cannot be empty！");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
                //这部分指的是-->选择现有的通知对象，进行关联入库
                tblAlarmRuleReq.getReceiverList().forEach(t -> {
                    TblAlarmRuleReceiver tblAlarmRuleReceiver = new TblAlarmRuleReceiver();
                    tblAlarmRuleReceiver.setRuleReceiverId(Utils.assignUUId());
                    tblAlarmRuleReceiver.setReceiverId(t);
                    tblAlarmRuleReceiver.setRuleId(tblAlarmRule.getRuleId());
                    tblAlarmRuleReceiver.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                    tblAlarmRuleReceiver.setPhaseStatus(PhaseStatus.CREATED);
                    if (!ruleReceiverService.save(tblAlarmRuleReceiver)) {
                        log.info("create alarm rule receiver failed!");
                        throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                    }
                });
            }
        }

        AlarmRuleParameters alarmRuleParameters = new AlarmRuleParameters();
        alarmRuleParameters.setRuleId(tblAlarmRule.getRuleId());
        alarmRuleParameters.setThreshold(tblAlarmRule.getExpr());
        alarmRuleParameters.setResourceIds(tblAlarmRuleReq.getResourceIds());
        if (ResourceType.HYPERVISOR_NODE == tblAlarmRuleReq.getResourceType())
        {
            List<String> resourceIds = combRpcSerice.getVmService().getHypervisorNodeIpPorts(tblAlarmRuleReq.getResourceIds());
            alarmRuleParameters.setResourceIds(resourceIds);
        }
        alarmRuleParameters.setForTime(tblAlarmRuleReq.getDurationTime() + "m");
        alarmRuleParameters.setAlarmElementType(tblAlarmRuleReq.getAlarmElement());
        alarmRuleParameters.setResourceType(tblAlarmRuleReq.getResourceType());
        TsQueryParameters.generateRuleFile(alarmRuleParameters);
        reloadPrometheus();

        AlarmRuleBaseResp alarmRuleBaseResp = new AlarmRuleBaseResp();
        alarmRuleBaseResp.setAlramRuleId(tblAlarmRule.getRuleId());
        return alarmRuleBaseResp;
    }


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

    @Override
    public AlarmRulesResp selectAlaRuPage(String name, Integer resourceType, Integer pageSize, Integer pageNum, String userId) {
        AlarmRulesResp alarmRulesResp = new AlarmRulesResp();
        List<AlarmRuleResp> alarmRuleCount = alarmRuleDao.selectCountAlaRu(name, resourceType, userId);
        alarmRulesResp.setTotalNum(alarmRuleCount.size());
        if (alarmRuleCount.size() == 0) {
            return alarmRulesResp;
        }

        Page<AlarmRuleResp> page = new Page<AlarmRuleResp>(pageNum, pageSize);
        List<AlarmRuleResp> alarmRuleList = alarmRuleDao.selectAlaRuPage(page, name, resourceType, userId);

        //将多个通知对象，转换成list
        alarmRuleList.forEach(t -> {
            if (StrUtil.isNotBlank(t.getContactInfo()))
                t.setContactInfoList(Arrays.asList(t.getContactInfo().split(",")));
        });

        alarmRulesResp.setAlarmRules(alarmRuleList);

        return alarmRulesResp;
    }

    /**
     * @param: ruleId
     * @param: tblAlarmRule
     * @param: userId
     * @description: TODO   运维管理--报警设置--编辑报警器
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/26
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlarmRuleBaseResp updateAlarmRule(String ruleId, AlarmRuleReq tblAlarmRuleReq, String userId) {

        //监控 计算节点 判断当前用户权限
        if (tblAlarmRuleReq.getResourceType() == ResourceType.HYPERVISOR_NODE && !combRpcSerice.getUmsService().isAdminUser(userId)){
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.INFO);
        }

        TblAlarmRule tblAlarmRule = this.getById(ruleId);
        if (null == tblAlarmRule || REMOVED == tblAlarmRule.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.RULE_NOT_EXIST, ErrorLevel.INFO);
        }


        //移除历史的关联【关系和通知】的数据
        List<TblAlarmRuleResource> ruleResources = ruleResourceService.list(new LambdaQueryWrapper<TblAlarmRuleResource>().eq(TblAlarmRuleResource::getRuleId, ruleId).ne(TblAlarmRuleResource::getPhaseStatus, REMOVED));
        List<TblAlarmRuleReceiver> ruleReceivers = ruleReceiverService.list(new LambdaQueryWrapper<TblAlarmRuleReceiver>().eq(TblAlarmRuleReceiver::getRuleId, ruleId));
        ruleResources.forEach(d -> {
            d.setPhaseStatus(REMOVED);
            d.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            if (!ruleResourceService.updateById(d)) {
                log.info("del rule resources failed!");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        });
        ruleReceivers.forEach(d -> {
            d.setPhaseStatus(REMOVED);
            d.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            if (!ruleReceiverService.updateById(d)) {
                log.info("del rule receiver failed!");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        });

        //
        //判断是否存在重复--》报警器
        tblAlarmRuleReq.getResourceIds().forEach(resourceId -> {
            Integer tblAlarmRules = alarmRuleDao.getSameAlarmRule(tblAlarmRuleReq.getAlarmElement(), tblAlarmRule.getExpr(), userId, resourceId, tblAlarmRuleReq.getDurationTime());
            if (tblAlarmRules > 0) {
                log.info("current alarm is present，resourceId：" + resourceId);
                throw new WebSystemException(ErrorCode.CURRENT_ALARM_IS_PRESENT, ErrorLevel.INFO);
            }
        });

        TblAlarmRuleResource alarmRuleResource = new TblAlarmRuleResource();
        BeanUtils.copyProperties(tblAlarmRuleReq, alarmRuleResource);
        BeanUtils.copyProperties(tblAlarmRuleReq, tblAlarmRule);
        if (AlarmElementType.INSTANCE_HEALTH_STATUS ==tblAlarmRuleReq.getAlarmElement())
        {
            tblAlarmRule.setExpr("-");
            tblAlarmRule.setComparison(null);
            tblAlarmRule.setAlarmValue(null);
        }
        else
        {
            tblAlarmRule.setExpr(ComparisonOperator.getSymbolByValue(tblAlarmRuleReq.getComparison()) + tblAlarmRuleReq.getAlarmValue());
        }
        // 1.修改报警器设置 2. 修改入库被控制对象与告警器的对应关系 3.是否触发，通知对象--进行关联
        tblAlarmRule.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblAlarmRule.setUserId(userId);
        tblAlarmRule.setRuleId(ruleId);

        if (!this.updateById(tblAlarmRule)) {
            log.info("update alarm rule failed!");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }


        //在去修改入库被控制对象与告警器的对应关系
        if (tblAlarmRuleReq.getResourceIds().size() > 0) {
            tblAlarmRuleReq.getResourceIds().forEach(d -> {
                //----默认创建时候的【资源类型-报警条目】------
                //alarmRuleResource.setAlarmElement(ruleResources.get(0).getAlarmElement());
                //alarmRuleResource.setResourceType(ruleResources.get(0).getResourceType());
                //------------------------------------------

                alarmRuleResource.setAlarmRuleResourceId(Utils.assignUUId());
                alarmRuleResource.setResourceId(d);
                alarmRuleResource.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                alarmRuleResource.setRuleId(tblAlarmRule.getRuleId());
                alarmRuleResource.setPhaseStatus(PhaseStatus.CREATED);
                alarmRuleResource.setUnit(AlarmElementUnitOperator.getUnitByValue(tblAlarmRuleReq.getAlarmElement()));
                if (!ruleResourceService.save(alarmRuleResource)) {
                    log.info("add rule resources failed!");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            });
        }

        //是否触发，通知对象--进行关联
        if (null != tblAlarmRuleReq.getNotice()) {
            if (tblAlarmRuleReq.getNotice()) {
                if (tblAlarmRuleReq.getReceiverList().size() == 0) {
                    log.info("receiverList cannot be empty！");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
                //这部分指的是-->选择现有的通知对象，进行关联入库
                tblAlarmRuleReq.getReceiverList().forEach(t -> {
                    TblAlarmRuleReceiver tblAlarmRuleReceiver = new TblAlarmRuleReceiver();
                    tblAlarmRuleReceiver.setRuleReceiverId(Utils.assignUUId());
                    tblAlarmRuleReceiver.setReceiverId(t);
                    tblAlarmRuleReceiver.setRuleId(tblAlarmRule.getRuleId());
                    tblAlarmRuleReceiver.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
                    tblAlarmRuleReceiver.setPhaseStatus(PhaseStatus.CREATED);
                    if (!ruleReceiverService.save(tblAlarmRuleReceiver)) {
                        log.info("add rule receiver failed!");
                        throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                    }
                });
            }
        }

        //重新生成报警规则文件，并重载prometheus
        AlarmRuleParameters alarmRuleParameters = new AlarmRuleParameters();
        alarmRuleParameters.setRuleId(ruleId);
        if (AlarmElementType.INSTANCE_HEALTH_STATUS !=tblAlarmRuleReq.getAlarmElement())
        {
            alarmRuleParameters.setThreshold(ComparisonOperator.getSymbolByValue(tblAlarmRuleReq.getComparison()) + tblAlarmRuleReq.getAlarmValue());
        }
        alarmRuleParameters.setResourceIds(tblAlarmRuleReq.getResourceIds());
        if (ResourceType.HYPERVISOR_NODE == tblAlarmRuleReq.getResourceType())
        {
            List<String> resourceIds = combRpcSerice.getVmService().getHypervisorNodeIpPorts(tblAlarmRuleReq.getResourceIds());
            alarmRuleParameters.setResourceIds(resourceIds);
        }
        alarmRuleParameters.setForTime(tblAlarmRuleReq.getDurationTime() + "m");
        alarmRuleParameters.setAlarmElementType(tblAlarmRuleReq.getAlarmElement());
        alarmRuleParameters.setResourceType(tblAlarmRuleReq.getResourceType());
        TsQueryParameters.generateRuleFile(alarmRuleParameters);
        reloadPrometheus();
        AlarmRuleBaseResp alarmRuleBaseResp = new AlarmRuleBaseResp();
        alarmRuleBaseResp.setAlramRuleId(tblAlarmRule.getRuleId());
        return alarmRuleBaseResp;
    }

    /**
     * @param: ruleId
     * @description: TODO   运维管理--报警设置--删除报警器
     * @return: com.baomidou.mybatisplus.extension.api.R
     * @author: LiSen
     * @date: 2023/5/29
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlarmRuleBaseResp removeAlarmRule(String ruleId) {
        TblAlarmRule tblAlarmRule = this.getById(ruleId);
        if (null == tblAlarmRule || REMOVED == tblAlarmRule.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.RULE_NOT_EXIST, ErrorLevel.INFO);
        }
        tblAlarmRule.setPhaseStatus(REMOVED);
        tblAlarmRule.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (!this.updateById(tblAlarmRule)) {
            log.info("del alarm rule failed!");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        //移除历史的关联【关系和通知】的数据
        List<TblAlarmRuleResource> ruleResources = ruleResourceService.list(new LambdaQueryWrapper<TblAlarmRuleResource>().eq(TblAlarmRuleResource::getRuleId, ruleId).ne(TblAlarmRuleResource::getPhaseStatus, REMOVED));
        List<TblAlarmRuleReceiver> ruleReceivers = ruleReceiverService.list(new LambdaQueryWrapper<TblAlarmRuleReceiver>().eq(TblAlarmRuleReceiver::getRuleId, ruleId).ne(TblAlarmRuleReceiver::getPhaseStatus, REMOVED));
        if (ruleResources.size() > 0) {
            ruleResources.forEach(d -> {
                d.setPhaseStatus(REMOVED);
                d.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                if (!ruleResourceService.updateById(d)) {
                    log.info("del rule resources failed!");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            });
        }
        if (ruleReceivers.size() > 0) {
            ruleReceivers.forEach(d -> {
                d.setPhaseStatus(REMOVED);
                d.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                if (!ruleReceiverService.updateById(d)) {
                    log.info("del rule receiver failed!");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            });
        }

        String ruleFilePath = RULE_FILE_PATH + "/" + ruleId + ".yml";
        TemplateUtils.removeYmlFile(ruleFilePath);
        reloadPrometheus();
        AlarmRuleBaseResp alarmRuleBaseResp = new AlarmRuleBaseResp();
        alarmRuleBaseResp.setAlramRuleId(tblAlarmRule.getRuleId());
        return alarmRuleBaseResp;
    }


    /**
     * @param: ruleId
     * @description: TODO   通过主键查询单条数据
     * @return: com.lnjoying.justice.operation.domain.dto.response.AlarmRuleResp
     * @author: LiSen
     * @date: 2023/6/8
     */
    @Override
    public AlarmRuleDetailsResp getAlarmRuleById(String ruleId, String userId) {

        TblAlarmRule tblAlarmRule = this.getById(ruleId);
        if (null == tblAlarmRule || REMOVED == tblAlarmRule.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.RULE_NOT_EXIST, ErrorLevel.INFO);
        }
        List<AlarmRuleDetailsResp> alarmRuleResp = alarmRuleDao.getAlarmRuleById(ruleId, userId);
        AlarmRuleDetailsResp alarmRuleDetailsResp = new AlarmRuleDetailsResp();
        if (alarmRuleResp.size() > 0) {
            alarmRuleDetailsResp = alarmRuleResp.get(0);

            //================ 虚拟机/虚拟机组 和 通知对象id 合并+去重 ========================================
            //================ 虚拟机/虚拟机组 和 通知对象id 合并+去重 ========================================
            HashSet<String> resourceIdsSet = new HashSet<>();
            HashSet<String> receiverIdsSet = new HashSet<>();
            alarmRuleResp.forEach(d -> {
                resourceIdsSet.add(d.getResourceId());
                receiverIdsSet.add(d.getReceiverId());
            });

            //=============== 去调用 VM服务 查询出关联的虚拟机/虚拟机组的 名称 ==================================
            //=============== 去调用 VM服务 查询出关联的虚拟机/虚拟机组的 名称 ==================================
            Integer resourceType = alarmRuleResp.get(0).getResourceType();
            Map<String, Object> map = combRpcSerice.getVmService().getResourceIdToName(resourceType, resourceIdsSet);
            if (!map.isEmpty()) {
                List<ResourceDetailsResp> resourceDetailsResps = new ArrayList<>();
                for (String key : resourceIdsSet) {
                    ResourceDetailsResp resourceDetailsResp = new ResourceDetailsResp();
                    resourceDetailsResp.setResourceId(key);
                    resourceDetailsResp.setName(map.get(key).toString());
                    resourceDetailsResps.add(resourceDetailsResp);
                }
                alarmRuleDetailsResp.setResourceDetailsResps(resourceDetailsResps);
            }

            //=============== 查询关联的通知对象的 名称和邮箱/电话 ================================================
            //=============== 查询关联的通知对象的 名称和邮箱/电话 ================================================
            if (alarmRuleDetailsResp.getNotice()) {
                List<ReceiverDetailsResp> receiverDetailsResps = new ArrayList<>();
                List<TblReceiver> tblReceiver = receiverService.list(new LambdaQueryWrapper<TblReceiver>().select(TblReceiver::getReceiverId, TblReceiver::getName, TblReceiver::getContactInfo).in(TblReceiver::getReceiverId, receiverIdsSet).ne(TblReceiver::getPhaseStatus, REMOVED));
                tblReceiver.forEach(d -> {
                    ReceiverDetailsResp receiverDetailsResp = new ReceiverDetailsResp();
                    receiverDetailsResp.setReceiverId(d.getReceiverId());
                    receiverDetailsResp.setName(d.getName());
                    receiverDetailsResp.setContactInfo(d.getContactInfo());
                    receiverDetailsResps.add(receiverDetailsResp);
                });
                alarmRuleDetailsResp.setReceiverDetailsResps(receiverDetailsResps);
            }
            //==========================================================================================
            //==========================================================================================
        }
        return alarmRuleDetailsResp;
    }

    //
    // reload prometheus
    public void reloadPrometheus() {
        String prometheusUrl = operationConfig.getPrometheusServer() + "/-/reload";
        try {
            String result = HttpActionUtil.post(prometheusUrl, null);
            log.info("reload prometheus log: {}", result);
            //Map resultMap = JsonUtil.jsonToMap(result);
            //String status = (String) resultMap.get("status");
            if (StrUtil.isNotBlank(result))
            {
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
        } catch (Exception e) {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//            throw new RuntimeException("reload prometheus failed");
        }

    }
}

