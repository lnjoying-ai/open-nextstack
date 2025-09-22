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

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lnjoying.justice.repo.entity.Volume;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class VolumesRsp
{
    private long totalNum;

    private List<VolumeVo> volumes;

    @Data
    public static
    class VolumeVo
    {
        @TableField("tbl_volume.volume_id")
        private String volumeId;

        @TableField("tbl_volume.user_id")
        private String userId;

        @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
        @TableField("tbl_volume.create_time")
        private Date createTime;

        @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
        @TableField("tbl_volume.update_time")
        private Date updateTime;

        @TableField("tbl_volume.storage_pool_id")
        private String storagePoolId;

        @TableField("tbl_volume.name")
        private String name;

        @TableField("tbl_volume.size")
        private Integer size;

        @TableField("tbl_volume.type")
        private Integer type;

        @TableField("tbl_vm_instance.vm_instance_id")
        private String vmInstanceId;

        @TableField("tbl_vm_instance.name")
        private String vmName;

        @TableField("tbl_volume.phase_status")
        private Integer phaseStatus;

        @TableField("tbl_volume.image_id")
        private String imageId;

        public void setVolumeVol(Volume tblVolume)
        {

            this.volumeId = tblVolume.getVolumeId();

            this.name = tblVolume.getName();

            this.storagePoolId = tblVolume.getStoragePoolId();

            this.userId = tblVolume.getUserId();

            this.size = tblVolume.getSize();

            this.type = tblVolume.getType();

            this.vmInstanceId = tblVolume.getVmId();

            this.phaseStatus = tblVolume.getPhaseStatus();

            this.imageId = tblVolume.getImageId();
        }
    }


}
