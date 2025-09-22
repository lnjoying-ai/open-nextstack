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

package com.lnjoying.justice.schema.service.repo;

import io.swagger.annotations.ApiParam;
import lombok.Data;

import java.util.List;

public interface VolumeService
{
    String createRootDisk(@ApiParam(name="userId") String userId, @ApiParam(name="size") Integer size,@ApiParam(name="vmId") String vmId,
                          @ApiParam(name="storagePoolId") String storagePoolId, @ApiParam(name = "nodeIp") String nodeIp,
                          @ApiParam(name="imageId") String imageId, @ApiParam(name="vmInstanceName") String vmInstanceName);

    Boolean canMigrate(@ApiParam(name="vmInstanceId") String vmInstanceId);

    VolumeInfo setRootDiskAttached(@ApiParam(name="userId") String userId,
                         @ApiParam(name="volumeId") String volumeId);

    VolumeInfo setRootDiskDetached(@ApiParam(name="volumeId") String volumeId);

    String createDataDisk(@ApiParam(name="userId") String userId, @ApiParam(name="size") Integer size, @ApiParam(name="volumeName") String volumeName,
                          @ApiParam(name="vmId") String vmId,@ApiParam(name="storagePoolId") String storagePoolId, @ApiParam(name = "nodeIp") String nodeIp);

    String getVolumeIdFromAgent(@ApiParam(name = "volumeId") String volumeId);

    List<String> getVolumeIdFromAgentList(@ApiParam(name = "volumeList") List<String> volumeList);

    Boolean detachVolumesByVmId(@ApiParam(name="vmId") String vmId, @ApiParam(name="removeRootDisk") Boolean removeRootDisk);

    Boolean removeDataVolume(@ApiParam(name="volumeIds") List<String> volumeIds);

    List<VolumeInfo> getVolumeInfosByVmId(@ApiParam(name="vmId") String vmId);

    ImageInfo getImageInfoByRecycleVolumeId(@ApiParam(name="volumeId") String volumeId);

    String attachVolume(@ApiParam(name="volumeId") String volumeId,
                         @ApiParam(name="vmId") String vmId,
                            @ApiParam(name="nodeIp") String nodeIp);

    List<String> attachVolumes(@ApiParam(name="volumeIds") List<String> volumeIds,
                                   @ApiParam(name="vmId") String vmId,
                                   @ApiParam(name="nodeIp") String nodeIp);

    Boolean isDetached(@ApiParam(name="volumeId") String volumeId);

    String setDestIp(@ApiParam(name="vmId") String vmId,
                     @ApiParam(name="destIp") String destIp);

    @Data
    final class ImageInfo
    {
        String imageId;
        String osType;
    }

    @Data
    final class VolumeInfo
    {
        String volumeName;
        String volumeId;
        Integer size;
        Integer type;
        Integer phaseStatus;
    }
}
