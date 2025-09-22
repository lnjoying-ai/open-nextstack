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
import com.lnjoying.vm.domain.backend.response.PanelsRsp;
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
public class ComputeMonitorConfigReq
{

    private final ComputeMonitorConfig computeMonitorConfig = new ComputeMonitorConfig();

    private static final String dashboardName = "node-exporter-server-metrics";

    @Resource
    ComputeConfig computeConfig;


    @Bean("computeMonitorConfig")
    public ComputeMonitorConfig getComputeMonitorConfig()
    {
        return computeMonitorConfig;
    }

    @PostConstruct
    void createComputeMonitorConfigBean()
    {
        try
        {
            moreLoadConfig(10);
        }
        catch (Exception e)
        {
            log.error("load computeMonitor config error:{}", e.getMessage());
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
                loadComputeMonitorConfig();
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


    private void loadComputeMonitorConfig()
    {
        String url = computeConfig.getMonitorServer() + "/api/search?limit=1000&sort=name_sort";
        DashboardRsp[] getDashboardsRsp = HttpActionUtil.getObject(url, DashboardRsp[].class);
        String dashboardId = null;
        for (DashboardRsp dashboardRsp : getDashboardsRsp)
        {
            if (MonitorTitle.NodeDashboard.equals(dashboardRsp.getTitle()))
            {
                dashboardId = dashboardRsp.getUid();
                computeMonitorConfig.setDashboardId(dashboardId);
                computeMonitorConfig.setDashboardName(dashboardName);
                break;
            }
        }
        if (null == dashboardId)
        {
            log.error("could not get node dashboard id");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }

        String panelUrl = computeConfig.getMonitorServer() + "/api/dashboards/uid/" + dashboardId;
        PanelsRsp getPanelsRsp = HttpActionUtil.getObject(panelUrl, PanelsRsp.class);

        for (PanelsRsp.Panels panels : getPanelsRsp.getDashboard().getPanels())
        {
            if (null == panels.getPanels() || 0 == panels.getPanels().length)
            {
                computeMonitorConfig.setPanels(panels.getTitle(), panels.getId());
            }
            else
            {
                for (PanelsRsp.Panel panel : panels.getPanels())
                {
                    computeMonitorConfig.setPanels(panel.getTitle(), panel.getId());
                }
            }
        }
    }


}
