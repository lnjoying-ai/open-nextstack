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

package com.lnjoying.justice.repo.rpcserviceimpl;

import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.justice.repo.entity.Flavor;
import com.lnjoying.justice.repo.service.biz.FlavorServiceBiz;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Slf4j
@RpcSchema(schemaId = "flavorService")
public class RpcFlavorServiceImpl implements FlavorService
{
    @Autowired
    com.lnjoying.justice.repo.service.FlavorService flavorService;

    @Autowired
    FlavorServiceBiz flavorServiceBiz;

    @Override
    public FlavorInfo getFlavorInfo(@ApiParam(name = "flavorId") String flavorId)
    {
        FlavorInfo flavorInfo = new FlavorInfo();

        Flavor tblFlavor = flavorService.getById(flavorId);
        if (null == tblFlavor || REMOVED == tblFlavor.getPhaseStatus())
        {
            log.error("get flavor null, flavorId:{}", flavorId);
            return null;
        }
        flavorInfo.setFlavorId(flavorId);
        flavorInfo.setCpu(tblFlavor.getCpu());
        flavorInfo.setMem(tblFlavor.getMem());
//        flavorInfo.setRootDisk(tblFlavor.getRootDisk());
        flavorInfo.setType(tblFlavor.getType());
        flavorInfo.setName(tblFlavor.getName());
        flavorInfo.setGpuCount(null == tblFlavor.getGpuCount()?0:tblFlavor.getGpuCount());
        flavorInfo.setGpuName(null == tblFlavor.getGpuName()?"":tblFlavor.getGpuName());
        flavorInfo.setNeedIb(null != tblFlavor.getNeedIb() && tblFlavor.getNeedIb());
        return flavorInfo;
    }

    @Override
    public Integer getMemSizeTotalByFlavorIds(@ApiParam(name = "flavorIds") List<String> flavorIds)
    {
        int total = 0;
        for (String flavorId:flavorIds)
        {
            total += getMemByFlavorId(flavorId);
        }

        return total;
    }

    private int getMemByFlavorId(String flavorId)
    {
        if (StrUtil.isBlank(flavorId)) return 0;
        Flavor flavor = flavorService.getById(flavorId);
        if (null == flavor || REMOVED == flavor.getPhaseStatus()) {
            return 0;
        }
        return flavor.getMem();
    }

}
