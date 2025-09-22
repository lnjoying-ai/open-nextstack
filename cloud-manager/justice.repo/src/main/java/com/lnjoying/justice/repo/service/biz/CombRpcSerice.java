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

package com.lnjoying.justice.repo.service.biz;

import com.lnjoying.justice.schema.service.compute.BaremetalService;
import com.lnjoying.justice.schema.service.compute.ComputeService;
import com.lnjoying.justice.schema.service.compute.VmService;
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

//    @RpcReference(microserviceName = "compute", schemaId = "computeService")
    private ComputeService computeService;

    public ComputeService getComputeService() {return computeService;}

    @RpcReference(microserviceName = "vm", schemaId = "computeVmService")
    private VmService vmService;

    public VmService getVmService() {return vmService;}


    @RpcReference(microserviceName = "bm", schemaId = "computeBmService")
    private BaremetalService baremetalService;

    public BaremetalService getBmService() {return baremetalService;}
}
