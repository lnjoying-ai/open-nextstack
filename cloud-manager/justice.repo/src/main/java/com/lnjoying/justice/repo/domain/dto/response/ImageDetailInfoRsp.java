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

import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.repo.common.constant.ImageType;
import com.lnjoying.justice.repo.entity.Image;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class ImageDetailInfoRsp {
    String imageId;
    String imageName;
    Integer imageOsType;
//    Integer imageOsVendor;
//    String imageOsVersion;
//    String userId;
//    String userName;
    Integer imageFormat;
    Boolean isPublic;
    String createTime;
    String updateTime;
    Integer phaseStatus;
    Integer imageOsVendor;
    String  imageOsVersion;
    String  gpuDriverVersion;
//    List<ImageIdInfo> imageIdInfos;

    public void setImageDetailInfoRsp(Image tblImage){
        this.imageId = tblImage.getImageId();
        this.imageName = tblImage.getImageName();
        this.imageOsType = tblImage.getImageOsType();
//        this.imageOsVendor = rsImage.getImageOsVendor();
//        this.imageOsVersion = rsImage.getImageOsVersion();
//        this.userId = rsImage.getUserId();
        this.isPublic = tblImage.getIsPublic();
        this.phaseStatus = tblImage.getPhaseStatus();
        if (StrUtil.isBlank(tblImage.getFileIdFromAgent()))
        {
            this.imageFormat = ImageType.VM_IMAGE_TYPE;
        }
        else
        {
            this.imageFormat = ImageType.BAREMETAL_IMAGE_TYPE;
        }
        this.imageOsVendor = tblImage.getImageOsVendor();
//        this.imageFormat = tblImage.getImageFormat();
        this.createTime = Utils.formatDate(tblImage.getCreateTime());
        this.updateTime = Utils.formatDate(tblImage.getUpdateTime());
        if (this.imageName.contains("GPU"))
        {
            this.gpuDriverVersion = this.imageName.split("-GPU-")[1];
        }
        this.imageOsVersion = tblImage.getImageOsVersion();
    }
}
