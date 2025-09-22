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

package com.lnjoying.justice.operation.rpcserviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.schema.service.operation.AlertService;
import com.lnjoying.justice.operation.common.constant.AlarmElementType;
import com.lnjoying.justice.operation.common.constant.ModuleConstant;
import com.lnjoying.justice.operation.common.constant.ResourceType;
import com.lnjoying.justice.operation.domain.dto.request.WebhookInfoReq;
import com.lnjoying.justice.operation.entity.TblAlarmRuleResource;
import com.lnjoying.justice.operation.service.TblAlarmRuleResourceService;
import com.lnjoying.justice.operation.service.biz.WebHookService;
import io.swagger.annotations.ApiParam;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@RpcSchema(schemaId = "alarmService")
public class AlarmServiceImpl implements AlertService
{

    @Autowired
    private TblAlarmRuleResourceService tblAlarmRuleResourceService;

    @Autowired
    private WebHookService webHookService;

    @Override
    public void sendAlarmInfo(@ApiParam(name = "resourceId") String resourceId, @ApiParam(name = "alertStatus") String alertStatus)
    {
       LambdaQueryWrapper<TblAlarmRuleResource> queryWrapper = new LambdaQueryWrapper<>();
       queryWrapper.eq(TblAlarmRuleResource::getResourceId, resourceId)
               .eq(TblAlarmRuleResource::getAlarmElement, AlarmElementType.INSTANCE_HEALTH_STATUS)
               .eq(TblAlarmRuleResource::getResourceType, ResourceType.HYPERVISOR_NODE)
               .ne(TblAlarmRuleResource::getPhaseStatus, REMOVED);
       if (0 == tblAlarmRuleResourceService.count(queryWrapper))
       {
           return;
       }

       queryWrapper.last("limit 1");
       TblAlarmRuleResource tblAlarmRuleResource = tblAlarmRuleResourceService.getOne(queryWrapper);
       String alarmRuleId = tblAlarmRuleResource.getRuleId();

        WebhookInfoReq webhookInfoReq = new WebhookInfoReq();
        WebhookInfoReq.Alert alert = new WebhookInfoReq.Alert();
        WebhookInfoReq.Annotations annotations = new WebhookInfoReq.Annotations();
        annotations.setRuleId(alarmRuleId);
        if (ModuleConstant.FIRING.equals(alertStatus))
        {
            annotations.setDescription(String.format("Instance %s 已离线", resourceId));
            annotations.setSummary("主机异常");
            annotations.setThreshold("-");
            alert.setStatus(ModuleConstant.FIRING);
        }
        else
        {
            annotations.setDescription(String.format("Instance %s 已恢复", resourceId));
            annotations.setSummary("主机恢复");
            annotations.setThreshold("-");
            alert.setStatus(ModuleConstant.RESOLVED);
        }
        alert.setAnnotations(annotations);
        List<WebhookInfoReq.Alert> alerts = Collections.singletonList(alert);
        webhookInfoReq.setAlerts(alerts);

        webHookService.handleInstanceGroupAlarm(webhookInfoReq);
    }
}
