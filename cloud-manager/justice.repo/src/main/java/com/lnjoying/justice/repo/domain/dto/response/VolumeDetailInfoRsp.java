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

import com.lnjoying.justice.repo.entity.Volume;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class VolumeDetailInfoRsp
{
    private String volumeId;

    private String name;

    private String storagePoolId;

    private String storagePoolName;

    private String description;

    private String createTime;

    private String updateTime;

    private Integer size;

    private Integer type;

    private Integer imageOsType;

    private Integer imageOsVendor;

    private String imageName;

    private String imageId;

    private Integer phaseStatus;

    public void setVolumeDetailInfoRsp(Volume volume)
    {
        this.volumeId = volume.getVolumeId();
        this.size = volume.getSize();
        this.storagePoolId = volume.getStoragePoolId();
        this.name = volume.getName();
        this.description = volume.getDescription();
        this.createTime = Utils.formatDate(volume.getCreateTime());
        this.updateTime = Utils.formatDate(volume.getUpdateTime());
        this.type = volume.getType();
        this.imageId = volume.getImageId();
        this.phaseStatus = volume.getPhaseStatus();
    }

}
