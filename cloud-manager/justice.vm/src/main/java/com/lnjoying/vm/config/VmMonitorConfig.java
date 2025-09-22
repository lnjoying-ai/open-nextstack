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
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VmMonitorConfig extends CommonMonitorConfig
{
    Integer vmState;

    Integer totalVCPUUsage;

    //    Integer vmCPUCount;
    Integer memMaxBytes;

    Integer totalMemUsageBytes;

    Integer interfacePPS;

    Integer interfaceErrorDrop;


    public void setPanels(String title, Integer id)
    {
        title = title.trim();
        super.setPanels(title, id);

        switch (title)
        {
            case MonitorTitle.TotalVCPUUsage:
                totalVCPUUsage = id;
                break;
            case MonitorTitle.VmMemMaxBytes:
                memMaxBytes = id;
                break;
            case MonitorTitle.VmState:
                vmState = id;
                break;
            case MonitorTitle.TotalMemUsageBytes:
                totalMemUsageBytes = id;
                break;
            case MonitorTitle.InterfaceErrorDrop:
                interfaceErrorDrop = id;
                break;
            case MonitorTitle.InterfacePPS:
                interfacePPS = id;
                break;
        }
    }
}
