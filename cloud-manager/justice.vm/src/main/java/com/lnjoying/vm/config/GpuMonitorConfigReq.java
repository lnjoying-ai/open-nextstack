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

package com.lnjoying.vm.config;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.common.MonitorTitle;
import com.lnjoying.vm.domain.backend.response.DashboardRsp;
import com.micro.core.nework.http.HttpActionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@DependsOn("computeConfig")
@Configuration
@Slf4j
public class GpuMonitorConfigReq
{

    private final GpuMonitorConfig gpuMonitorConfig = new GpuMonitorConfig();

    private static final String dashboardName = "nvidia-gpu-metrics";

    @Resource
    ComputeConfig computeConfig;


    @Bean("gpuMonitorConfig")
    public GpuMonitorConfig getVmMonitorConfig()
    {
        return gpuMonitorConfig;
    }

    @PostConstruct
    void createVmMonitorConfigBean()
    {
        try
        {
            moreLoadConfig(10);
        }
        catch (Exception e)
        {
            log.error("load gpuMonitor config error:{}", e.getMessage());
        }
    }

    private void moreLoadConfig(int attempts) throws InterruptedException
    {
        int again = 0;
        int sleepTime = 1000;
        while (again < attempts)
        {
            try
            {
                loadGpuMonitorConfig();
                break;
            }
            catch (Exception e)
            {
                Thread.sleep(sleepTime);
                sleepTime = sleepTime * 2;
                again++;
            }
        }
    }

    private void loadGpuMonitorConfig()
    {
        String url = computeConfig.getMonitorServer() + "/api/search?limit=1000&sort=name_sort";
        DashboardRsp[] getDashboardsRsp = HttpActionUtil.getObject(url, DashboardRsp[].class);
        String dashboardId = null;
        for (DashboardRsp dashboardRsp : getDashboardsRsp)
        {
            if (MonitorTitle.GPUDashboard.equals(dashboardRsp.getTitle()))
            {
                dashboardId = dashboardRsp.getUid();
                gpuMonitorConfig.setDashboardId(dashboardId);
                gpuMonitorConfig.setDashboardName(dashboardName);
                break;
            }
        }
        if (null == dashboardId)
        {
            log.error("could not get gpu dashboard id");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
    }

}
