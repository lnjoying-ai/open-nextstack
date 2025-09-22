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

package com.lnjoying.justice.repo.domain.dto.response;

import com.lnjoying.justice.repo.entity.Share;
import lombok.Data;

import java.util.Date;

@Data
public class ShareDetailInfoRsp {
    private String shareId;

    private String userId;

    private String baremetalId;

    private String imageId;

    private Date createTime;

    private Date updateTime;

    private String iscsiInitiator;

    private String iscsiTarget;

    private String iscsiIpport;

    private Integer phase_status;

    public void setShareDetailInfoRsp(Share tblShare){
        this.shareId = tblShare.getShareId();
        this.updateTime = tblShare.getUpdateTime();
        this.createTime = tblShare.getCreateTime();
        this.imageId = tblShare.getImageId();
        this.baremetalId = tblShare.getBaremetalId();
        this.iscsiInitiator = tblShare.getIscsiInitiator();
        this.iscsiTarget = tblShare.getIscsiTarget();
        this.iscsiIpport = tblShare.getIscsiIpport();
        this.userId = tblShare.getUserId();
        this.phase_status = tblShare.getPhaseStatus();
    }
}
