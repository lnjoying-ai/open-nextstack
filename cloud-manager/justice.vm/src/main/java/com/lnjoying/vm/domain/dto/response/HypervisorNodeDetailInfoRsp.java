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

@Data
public class HypervisorNodeDetailInfoRsp
{
    private String nodeId;

    private String name;

    private String manageIp;

    private String hostname;

    private String description;

    private String sysUsername;

    private String pubkeyId;

//    private List<VmInfo> vmInfos;

    private String createTime;

    private String updateTime;

    private boolean hasGpu;

//    @Data
//    public static class VmInfo
//    {
//        String vmInstanceId;
//        String vmName;
//    }

    public void setHypervisorNodeDetailInfo(HypervisorNode tblHypervisorNode)
    {
        nodeId = tblHypervisorNode.getNodeId();

        name = tblHypervisorNode.getName();

        manageIp = tblHypervisorNode.getManageIp();

        hostname = tblHypervisorNode.getHostName();

        description = tblHypervisorNode.getDescription();

        sysUsername = tblHypervisorNode.getSysUsername();

        pubkeyId = tblHypervisorNode.getPubkeyId();

        createTime = Utils.formatDate(tblHypervisorNode.getCreateTime());

        updateTime = Utils.formatDate(tblHypervisorNode.getUpdateTime());
    }
}
