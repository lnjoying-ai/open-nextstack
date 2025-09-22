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

package com.lnjoying.vm.service.biz;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.common.CommonPhaseStatus;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.config.ComputeMonitorConfig;
import com.lnjoying.vm.config.GpuMonitorConfig;
import com.lnjoying.vm.config.VmMonitorConfig;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.service.VmInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service("monitorService")
@Slf4j
public class MonitorService
{

    @Autowired
    ComputeConfig computeConfig;

    @Autowired
    VmMonitorConfig vmMonitorConfig;

    @Autowired
    GpuMonitorConfig gpuMonitorConfig;

    @Autowired
    ComputeMonitorConfig computeMonitorConfig;

    @Autowired
    VmInstanceService vmInstanceService;


    public VmMonitorConfig getVmAllPanels()
    {
        return vmMonitorConfig;
    }

    public ComputeMonitorConfig getComputeAllPanels()
    {
        return computeMonitorConfig;
    }

    public GpuMonitorConfig getGpuAllPanels()
    {
        return gpuMonitorConfig;
    }

    public boolean hasPermission(String vmInstanceId, String userId, String dashboardId, String queryUserId, String nodeId)
    {
        log.info("get monitor info : vmInstanceId: {}, userId: {}, dashboardId: {}", vmInstanceId, userId, dashboardId);
        if (Objects.equals(computeMonitorConfig.getDashboardId(), dashboardId))
        {
            return ServiceCombRequestUtils.isAdmin();
        }
        if (Objects.equals(gpuMonitorConfig.getDashboardId(), dashboardId) && !Strings.isBlank(nodeId))
        {
            return ServiceCombRequestUtils.isAdmin();
        }
        if (null != queryUserId)
        {
            return ServiceCombRequestUtils.isAdmin() || Objects.equals(userId, queryUserId);
        }

        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance)
        {
            LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ne(VmInstance::getPhaseStatus, CommonPhaseStatus.REMOVED)
                    .eq(VmInstance::getVmInstanceId, vmInstanceId);
            if (vmInstanceService.count(queryWrapper) < 1)
            {
                log.info("the vm does not exist, vmInstanceId: {}", vmInstanceId);
                throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
            }
            tblVmInstance = vmInstanceService.getOne(queryWrapper);

        }
        if (!Objects.equals(userId, tblVmInstance.getUserId()))
        {
            log.info("The VM is not owned by the user, vmInstanceId:{} userId:{}", vmInstanceId, userId);
            return false;
        }
        return true;
    }

}
