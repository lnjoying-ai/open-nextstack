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

package com.lnjoying.vm.aspect;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.service.VmInstanceService;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Date;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class VmTimeoutCheckAspect
{
    @Autowired
    private VmInstanceService vmService;

    @Autowired
    private LogRpcService logRpcService;

    @Pointcut("@annotation(com.lnjoying.vm.aspect.CheckVmTimeout)")
    public void checkVmTimeoutPointCut()
    {

    }

    @Around("checkVmTimeoutPointCut()")
    public Object checkVmTimeout(ProceedingJoinPoint pjp) throws Throwable
    {
        // 获取方法上的注解信息
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        CheckVmTimeout checkVmTimeout = method.getAnnotation(CheckVmTimeout.class);
        int timeoutMinutes = checkVmTimeout.timeoutMinutes();

        Object[] args = pjp.getArgs();
        for (Object arg : args)
        {
            if (arg instanceof VmInstance)
            {
                VmInstance tblVmInstance = (VmInstance) arg;
                if (VmInstanceStatus.WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME == tblVmInstance.getPhaseStatus())
                {
                    break;
                }
                Date createTime = tblVmInstance.getCreateTime();
                Date now = new Date();
                long diff = now.getTime() - createTime.getTime();
                long diffMinutes = diff / (60 * 1000);
                if (diffMinutes > timeoutMinutes)
                {
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATE_FAILED);
                    tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    boolean ok = vmService.updateById(tblVmInstance);
                    if (ok)
                    {
                        log.error("VM timeout, vmId: {}", tblVmInstance.getVmInstanceId());
                        logRpcService.getLogService().addEvent(tblVmInstance.getUserId(), "创建虚机超时", String.format("请求参数: vmInstanceId:%s", tblVmInstance.getVmInstanceId()), "创建失败");
                    }
                    else
                    {
                        log.error("VM timeout, vmId: {}, update database failed", tblVmInstance.getVmInstanceId());
                    }
                    throw new WebSystemException(ErrorCode.VM_STATUS_ERROR, ErrorLevel.INFO);
                }
            }
        }
        return pjp.proceed();
    }

}
