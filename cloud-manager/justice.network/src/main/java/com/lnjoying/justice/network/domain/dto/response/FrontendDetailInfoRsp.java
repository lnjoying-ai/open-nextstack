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

import com.lnjoying.justice.network.entity.Frontend;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class FrontendDetailInfoRsp
{
    private String frontendId;

    private String name;

    private String listenPort;

    private String protocol;

    private String lbId;

    private String backendId;

    private String backendName;

    private String createTime;

    private String updateTime;

    private Integer phaseStatus;

    public void setFrontendDetailInfoRsp(Frontend tblFrontend)
    {
        this.frontendId = tblFrontend.getFrontendId();
        this.name = tblFrontend.getName();
        this.listenPort = tblFrontend.getListenPort();
//        this.protocol = tblFrontend.getProtocol();
        this.lbId = tblFrontend.getLbId();
        this.backendId = tblFrontend.getBackendId();
//        this.backendName = backendName;
        this.createTime = Utils.formatDate(tblFrontend.getCreateTime());
        this.updateTime = Utils.formatDate(tblFrontend.getUpdateTime());
        this.phaseStatus = tblFrontend.getPhaseStatus();
    }
}
