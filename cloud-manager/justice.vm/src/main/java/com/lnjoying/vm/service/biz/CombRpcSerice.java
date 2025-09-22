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

package com.lnjoying.vm.service.biz;

import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.justice.schema.service.repo.ImageService;
import com.lnjoying.justice.schema.service.repo.StoragePoolService;
import com.lnjoying.justice.schema.service.repo.VolumeService;
import com.lnjoying.justice.schema.service.ums.UmsService;
import org.apache.servicecomb.provider.pojo.RpcReference;
import org.springframework.stereotype.Component;

@Component("combRpcService")
public class CombRpcSerice
{
    @RpcReference(microserviceName = "ums", schemaId = "umsService")
    private UmsService umsService;

    public UmsService getUmsService()
    {
        return umsService;
    }

    @RpcReference(microserviceName = "network", schemaId = "networkService")
    private NetworkService networkService;

    public NetworkService getNetworkService()
    {
        return networkService;
    }

    @RpcReference(microserviceName = "repo", schemaId = "imageService")
    private ImageService imageService;

    public ImageService getImageService()
    {
        return imageService;
    }

    @RpcReference(microserviceName = "repo", schemaId = "volumeService")
    private VolumeService volumeService;

    public VolumeService getVolumeService()
    {
        return volumeService;
    }

    @RpcReference(microserviceName = "repo", schemaId = "storagePoolService")
    private StoragePoolService storagePoolService;

    public StoragePoolService getStoragePoolService()
    {
        return storagePoolService;
    }

    @RpcReference(microserviceName = "repo", schemaId = "flavorService")
    private FlavorService flavorService;

    public FlavorService getFlavorService()
    {
        return flavorService;
    }
}
