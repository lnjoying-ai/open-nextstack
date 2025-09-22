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
import com.lnjoying.justice.repo.entity.VolumeSnap;
import lombok.Data;

import java.util.Date;

@Data
public class VolumeSnapDetailInfoRsp
{
    @TableField("tbl_volume_snap.volume_snap_id")
    private String volumeSnapId;

    @TableField("tbl_volume_snap.volume_id")
    private String volumeId;

    @TableField("tbl_volume.name")
    private String volumeName;

    @TableField("tbl_volume_snap.name")
    private String name;

    @TableField("tbl_volume_snap.description")
    private String description;

    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
    @TableField("tbl_volume_snap.create_time")
    private Date createTime;

    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
    @TableField("tbl_volume_snap.update_time")
    private Date updateTime;

    @TableField("tbl_volume_snap.phase_status")
    private Integer phaseStatus;

    @TableField("tbl_volume.storage_pool_id")
    private String storagePoolId;

    @TableField("tbl_volume_snap.is_current")
    private Boolean isCurrent;

    @TableField("tbl_volume_snap.user_id")
    private String userId;

    @TableField("tbl_volume_snap.parent_id")
    private String parentId;

    public void setVolumeSnapDetailInfoRsp(VolumeSnap tblVolumeSnap)
    {
        this.volumeId = tblVolumeSnap.getVolumeId();

        this.volumeSnapId = tblVolumeSnap.getVolumeSnapId();

        this.name = tblVolumeSnap.getName();

        this.description = tblVolumeSnap.getDescription();

        this.isCurrent = tblVolumeSnap.getIsCurrent();

        this.phaseStatus = tblVolumeSnap.getPhaseStatus();

        this.createTime = tblVolumeSnap.getCreateTime();

        this.updateTime = tblVolumeSnap.getUpdateTime();

        this.parentId = tblVolumeSnap.getParentId();
    }
}
