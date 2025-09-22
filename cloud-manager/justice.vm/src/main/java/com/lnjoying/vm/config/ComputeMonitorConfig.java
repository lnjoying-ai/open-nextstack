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
public class ComputeMonitorConfig extends CommonMonitorConfig
{
//    Integer cpuCores;
//
//    Integer cpuUsed;
//
//    Integer memoryUsed;
//
//    Integer fileSystemUsed;

//    Integer networkTraffic;

//    Integer diskIos;
//
//    Integer diskThroughput;

    Integer load;

    Integer contextSwitches;

    Integer tcpStats;

    Integer udpStats;

    Integer conntrack;


    public void setPanels(String title, Integer id)
    {
        title = title.trim();
        super.setPanels(title, id);

        switch (title)
        {
            case MonitorTitle.Load:
                load = id;
                break;
            case MonitorTitle.ContextSwitches:
                contextSwitches = id;
                break;
            case MonitorTitle.TcpStats:
                tcpStats = id;
                break;
            case MonitorTitle.UdpStats:
                udpStats = id;
                break;
            case MonitorTitle.Conntrack:
                conntrack = id;
                break;
        }
    }
}
