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

package com.lnjoying.justice.network.domain.dto.response;

import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.network.entity.Backend;
import com.micro.core.common.Utils;
import lombok.Data;

import java.util.List;

@Data
public class BackendDetailInfoRsp
{
    private String backendId;

    private String name;

    private String protocol;

    private String balance;

    private String creatTime;

    private String vpcId;

    private String lbId;

    private String vpcName;

    private List<String> backendServers;

    private Integer phaseStatus;

    public void setBackendDetailInfoRsp(Backend backend)
    {
        this.backendId = backend.getBackendId();
        this.name = backend.getName();
        this.protocol = backend.getProtocol();
        this.balance = backend.getBalance();
        this.creatTime = Utils.formatDate(backend.getCreateTime());
        this.lbId = backend.getLbId();
        this.vpcId = backend.getVpcId();
        this.backendServers = StrUtil.split(backend.getBackendServer(),",");
        this.phaseStatus = backend.getPhaseStatus();
    }
}
