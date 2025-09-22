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

import com.lnjoying.justice.repo.entity.StoragePool;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class StoragePoolDetailInfoRsp
{
    private String poolId;

    private String paras;

    private Integer type;

    private String name;

    private String description;

    private String createTime;

    private String updateTime;

    private String sid;

    public void setStoragePoolDetailInfoRsp(StoragePool storagePool)
    {
        this.poolId = storagePool.getStoragePoolId();

        this.paras = storagePool.getParas();

        this.type = storagePool.getType();

        this.name = storagePool.getName();

        this.description = storagePool.getDescription();

        this.sid = storagePool.getSid();

        this.createTime = Utils.formatDate(storagePool.getCreateTime());

        this.updateTime = Utils.formatDate(storagePool.getUpdateTime());
    }
}
