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

import com.lnjoying.vm.entity.Pubkey;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class PubkeyDetailInfo
{
    private String pubkeyId;
    private String name;
    private String pubkey;
    private String description;
    private String createTime;
    private String updateTime;

    public void setPubkeyDetailInfo(Pubkey tblPubkey)
    {
        this.pubkeyId = tblPubkey.getPubkeyId();
        this.pubkey = tblPubkey.getPubkey();
        this.name = tblPubkey.getName();
        this.description = tblPubkey.getDescription();
        this.createTime = Utils.formatDate(tblPubkey.getCreateTime());
        this.updateTime = Utils.formatDate(tblPubkey.getUpdateTime());
    }
}
