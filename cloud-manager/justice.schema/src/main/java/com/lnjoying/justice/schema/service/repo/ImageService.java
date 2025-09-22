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

import java.io.Serializable;
import java.util.List;

/**
 * image rpc service
 *
 * @author bruce
 **/
public interface ImageService
{

    Image getImage(@ApiParam(name = "imageId")String imageId);

    @Data
    final class Image implements Serializable
    {
        private String imageId;
        private String name;
        private Integer format;
        private Integer imageOsType;
        private Integer imageOsVendor;
        private String imageOsVersion;
        private String vmInstanceId;
        private String imageBase;
    }

    List<Image> getBatchImages(@ApiParam(name = "imageIdList") List<String> imageIdList);

    ImgConnection getImageConnection(@ApiParam(name = "shareId") String shareId);

    @Data
    final class ImgConnection implements Serializable
    {
        private String shareId;
        private String instanceId;
        private String imageId;
        private String iscsiInitiator;
        private String iscsiTarget;
        private String iscsiIpport;
    }

    String createImgConnection(@ApiParam(name = "connectionReq") ConnectionReq connectionReq);

    @Data
    final class  ConnectionReq implements Serializable
    {
        String instanceId;
        String imageId;
    }

    String deleteImgConnection(@ApiParam(name = "shareId") String shareId);

    String createNodeImage(@ApiParam(name = "imageId") String imageId,
                           @ApiParam(name = "nodeIp") String nodeIp,
                           @ApiParam(name = "nodeStoragePoolId") String nodeStoragePoolId);

    String getNodeImage(@ApiParam(name = "nodeImageId") String nodeImageId);
}
