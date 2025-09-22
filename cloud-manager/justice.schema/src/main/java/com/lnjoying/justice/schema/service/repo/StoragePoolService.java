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

public interface StoragePoolService
{
    String createNodeStoragePool(@ApiParam(name = "nodeIp") String nodeIp,
                                       @ApiParam(name = "storagePoolId") String storagePoolId);
    String getNodeStoragePoolPhaseStatus(@ApiParam(name="nodeIp") String nodeIp,
                                         @ApiParam(name="storagePoolId") String storagePoolId);
//    @Data
//    final class StoragePoolCreateReq implements Serializable
//    {
//        private Integer type;
//
//        private String sid;
//
//        private String paras;
//    }

//    String getStoragePoolFromAgent(@ApiParam(name = "storagePoolId") String storagePoolId,
//                                   @ApiParam(name = "nodeIp") String nodeIp);
}
