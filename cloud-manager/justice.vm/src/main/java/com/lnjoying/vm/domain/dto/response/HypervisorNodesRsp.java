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

package com.lnjoying.vm.domain.dto.response;

import com.lnjoying.vm.entity.HypervisorNode;
import com.micro.core.common.Utils;
import lombok.Data;

import java.util.List;

@Data
public class HypervisorNodesRsp
{
    private long totalNum;

    private List<HypervisorNodeRsp> hypervisorNodes;

    @Data
    public static class HypervisorNodeRsp
    {
        private String nodeId;

        private String name;

        private String manageIp;

        private String hostname;

        private String description;

        private Integer phaseStatus;

        private String createTime;

        private String updateTime;

        private Integer cpuLogCount;

        private Integer memTotal;

        public void setHypervisorNode(HypervisorNode tblHypervisorNode)
        {
            nodeId = tblHypervisorNode.getNodeId();
            name = tblHypervisorNode.getName();
            manageIp = tblHypervisorNode.getManageIp();
            hostname = tblHypervisorNode.getHostName();
            description = tblHypervisorNode.getDescription();
            memTotal = tblHypervisorNode.getMemTotal();
            cpuLogCount = tblHypervisorNode.getCpuLogCount();
            createTime = Utils.formatDate(tblHypervisorNode.getCreateTime());
            updateTime = Utils.formatDate(tblHypervisorNode.getUpdateTime());
            phaseStatus = tblHypervisorNode.getPhaseStatus();
        }
    }
}
