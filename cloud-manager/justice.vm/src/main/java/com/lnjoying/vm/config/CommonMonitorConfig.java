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


import com.lnjoying.vm.common.MonitorTitle;
import lombok.Data;

@Data
public class CommonMonitorConfig
{
    String dashboardId;

    String dashboardName;

    Integer cpuCount;

    Integer cpuUsed;

    Integer memUsed;

    Integer fileSystemUsed;

    Integer blockRWBytes;

    Integer blockRWRequest;

    Integer interfaceRXTXBytes;

    //    public GetDashboardRsp[] getDashboards()
//    {
//        try
//        {
//            String url = computeConfig.getMonitorServer() + "/api/search?limit=1000&sort=name_sort";
//            return HttpActionUtil.getObject(url, GetDashboardRsp[].class);
//        }
//        catch (Exception e)
//        {
//            LOGGER.error("GetDashboardRsp error:{}", e.getMessage());
//            return  null;
//        }
//    }
//
    public void setPanels(String title, Integer id)
    {
        title = title.trim();
        switch (title)
        {
            case MonitorTitle.BlockRWBytes:
                blockRWBytes = id;
                break;
            case MonitorTitle.BlockRWRequest:
                blockRWRequest = id;
                break;
            case MonitorTitle.InterfaceRXTXBytes:
                interfaceRXTXBytes = id;
                break;
            case MonitorTitle.CpuCount:
                cpuCount = id;
                break;
            case MonitorTitle.CpuUsed:
                cpuUsed = id;
                break;
            case MonitorTitle.FileSystemUsed:
                fileSystemUsed = id;
                break;
            case MonitorTitle.MemUsed:
                memUsed = id;
                break;
        }
    }


}
