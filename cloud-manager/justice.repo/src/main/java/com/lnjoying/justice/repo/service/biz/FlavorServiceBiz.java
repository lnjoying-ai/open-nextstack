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


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.FlavorType;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.config.RepoAgentConfig;
import com.lnjoying.justice.repo.domain.dto.request.FlavorCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.FlavorBaseRsp;
import com.lnjoying.justice.repo.domain.dto.response.FlavorDetailInfoRsp;
import com.lnjoying.justice.repo.domain.dto.response.FlavorMaxNumInfoRsp;
import com.lnjoying.justice.repo.domain.dto.response.FlavorsRsp;
import com.lnjoying.justice.repo.entity.Flavor;
import com.lnjoying.justice.repo.entity.search.FlavorSearchCritical;
import com.lnjoying.justice.repo.service.FlavorService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service("flavorServiceBiz")
@Slf4j
public class FlavorServiceBiz
{
    @Autowired
    FlavorService flavorService;

    @Autowired
    CombRpcSerice combRpcSerice;

    @Autowired
    private RepoAgentConfig repoAgentConfig;

    public FlavorsRsp getFlavors(FlavorSearchCritical flavorSearchCritical) throws WebSystemException
    {

        LambdaQueryWrapper<Flavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Flavor::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(flavorSearchCritical.getFlavorName())) {
            queryWrapper.like(Flavor::getName, flavorSearchCritical.getFlavorName());
        }
        if (!StrUtil.isBlank(flavorSearchCritical.getUserId()))
        {
            queryWrapper.eq(Flavor::getUserId, flavorSearchCritical.getUserId());
        }
        if (null == flavorSearchCritical.getFlavorType() || FlavorType.VM_FLAVOR_TYPE == flavorSearchCritical.getFlavorType())
        {
            queryWrapper.eq(Flavor::getType, FlavorType.VM_FLAVOR_TYPE);
            if (null != flavorSearchCritical.getGpu() && flavorSearchCritical.getGpu())
            {
                queryWrapper.gt(Flavor::getGpuCount, 0);
            }
            else if (null != flavorSearchCritical.getGpu() && !flavorSearchCritical.getGpu())
            {
                queryWrapper.and(wrapper -> wrapper.eq(Flavor::getGpuCount, 0).or().isNull(Flavor::getGpuCount));
            }
        }
        else if (FlavorType.BAREMETAL_FLAVOR_TYPE == flavorSearchCritical.getFlavorType())
        {
            queryWrapper.eq(Flavor::getType, FlavorType.BAREMETAL_FLAVOR_TYPE);
        }
        else if (FlavorType.ALL_FLAVOR_TYPE != flavorSearchCritical.getFlavorType() )
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        /*
        if(null!=flavorSearchCritical.getFlavorName() ){
            criteria.andFlavorNameEqualTo(flavorSearchCritical.getFlavorName());
        }else if (null != flavorSearchCritical.getFlavorOsVendor()&& null ==flavorSearchCritical.getFlavorOsType()){
            criteria.andFlavorOsVendorEqualTo(flavorSearchCritical.getFlavorOsVendor());
        }else if (null != flavorSearchCritical.getFlavorOsVendor() && null !=flavorSearchCritical.getFlavorOsType()){
            criteria.andFlavorOsTypeEqualTo(flavorSearchCritical.getFlavorOsType())
                    .andFlavorOsVendorEqualTo(flavorSearchCritical.getFlavorOsVendor());
        }else if  ( null !=flavorSearchCritical.getFlavorOsType() && null == flavorSearchCritical.getFlavorOsVendor()){
            criteria.andFlavorOsTypeEqualTo(flavorSearchCritical.getFlavorOsType());
        }
         */

        FlavorsRsp getFlavorsRsp = new FlavorsRsp();

        //get total number with example condition
        long totalNum = flavorService.count(queryWrapper);

        getFlavorsRsp.setTotalNum(totalNum);
        if (totalNum < 1) {
            return getFlavorsRsp;
        }

        //query with page number and page size
//        int begin = ((flavorSearchCritical.getPageNum() - 1) * flavorSearchCritical.getPageSize());

        queryWrapper.orderByDesc(Flavor::getCreateTime);

        Page<Flavor> page = new Page<>(flavorSearchCritical.getPageNum(),flavorSearchCritical.getPageSize());
        Page<Flavor> flavorPage = flavorService.page(page, queryWrapper);

        List<Flavor> flavors = flavorPage.getRecords();
        if (null == flavors) {
            return getFlavorsRsp;
        }

        List<FlavorDetailInfoRsp> flavorInfos = flavors.stream().map(tblRsFlavor -> {
            FlavorDetailInfoRsp flavorInfo = new FlavorDetailInfoRsp();
            flavorInfo.setFlavorDetailInfoRsp(tblRsFlavor);
            return flavorInfo;
        }).collect(Collectors.toList());

        //set response
        getFlavorsRsp.setFlavors(flavorInfos);
        return getFlavorsRsp;
    }

    public FlavorDetailInfoRsp getFlavor(String flavorId) throws WebSystemException {
        Flavor flavor = flavorService.getById(flavorId);
        if (null == flavor || REMOVED == flavor.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }
        FlavorDetailInfoRsp flavorDetailInfoRsp = new FlavorDetailInfoRsp();
        flavorDetailInfoRsp.setFlavorDetailInfoRsp(flavor);
        return flavorDetailInfoRsp;
    }

    public FlavorBaseRsp removeFlavor(String flavorId) throws WebSystemException {

        Flavor flavor = flavorService.getById(flavorId);
        if (null == flavor || REMOVED == flavor.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }
        int instanceTotal = 0;
        if (FlavorType.BAREMETAL_FLAVOR_TYPE == flavor.getType())
        {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }
        else
        {
             instanceTotal = combRpcSerice.getVmService().getInstanceCountByFlavorId(flavorId);

        }
//        int instanceTotal = combRpcSerice.getVmService().getInstanceCountByFlavorId(flavorId);
        if (instanceTotal > 0)
        {
            throw new WebSystemException(ErrorCode.FLAVOR_IS_USING, ErrorLevel.INFO);
        }
        flavor.setPhaseStatus(REMOVED);
        flavor.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = flavorService.updateById(flavor);
        if (!ok) {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return FlavorBaseRsp.builder().flavorId(flavorId).build();
    }

    public FlavorBaseRsp updateFlavor(String flavorId, String name) throws WebSystemException
    {
        Flavor flavor = flavorService.getById(flavorId);
        if (null == flavor || REMOVED == flavor.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.FLAVOR_NOT_EXIST, ErrorLevel.INFO);
        }
        flavor.setName(name);
        flavor.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = flavorService.updateById(flavor);
        if (!ok) {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return FlavorBaseRsp.builder().flavorId(flavorId).build();
    }

    public List<VmService.GpuFlavorInfo>  getAvailableGpu()
    {
        List<VmService.GpuFlavorInfo> gpuFlavorInfos = combRpcSerice.getVmService().getGpuFlavorInfos();
        if (null == gpuFlavorInfos || gpuFlavorInfos.size() < 1)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        Set<VmService.GpuFlavorInfo> setWithoutDuplicates = new HashSet<>(gpuFlavorInfos);
        List<VmService.GpuFlavorInfo> gpuInfos = setWithoutDuplicates.stream().map(gpuFlavorInfo -> {
            VmService.GpuFlavorInfo gpuInfo = new VmService.GpuFlavorInfo();
            gpuInfo.setGpuName(gpuFlavorInfo.getGpuName());
            gpuInfo.setGpuCount(gpuFlavorInfo.getGpuCount());
            return gpuInfo;
        }).collect(Collectors.toList());
        return gpuInfos;
    }

    public FlavorBaseRsp createFlavor(FlavorCreateReq flavorInfo,String userId) throws WebSystemException {
        if (!checkFlavorInfo(flavorInfo))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        Flavor tblFlavor = new Flavor();
        if (null != flavorInfo.getGpuCount() && flavorInfo.getGpuCount()>0 && StrUtil.isNotBlank(flavorInfo.getGpuName()))
        {
            List<VmService.GpuFlavorInfo> gpuFlavorInfos = combRpcSerice.getVmService().getGpuFlavorInfosByName(flavorInfo.getGpuName());
            if (null == gpuFlavorInfos || gpuFlavorInfos.size() < 1)
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            VmService.GpuFlavorInfo gpuFlavorInfo = gpuFlavorInfos.get(0);
            if (gpuFlavorInfo.getGpuCount() < flavorInfo.getGpuCount())
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            if (!Objects.equals(gpuFlavorInfo.getGpuName(), flavorInfo.getGpuName()))
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            tblFlavor.setGpuCount(flavorInfo.getGpuCount());
            tblFlavor.setGpuName(flavorInfo.getGpuName());
        }
        String flavorId = Utils.assignUUId();
        tblFlavor.setFlavorId(flavorId);
        tblFlavor.setPhaseStatus(PhaseStatus.ADDED);
        tblFlavor.setCreateTime(new Date(System.currentTimeMillis()));
        tblFlavor.setUpdateTime(tblFlavor.getCreateTime());
        if (!StrUtil.isBlank(userId))
        {
            tblFlavor.setUserId(userId);
        }
        tblFlavor.setCpu(flavorInfo.getCpu());
        tblFlavor.setMem(flavorInfo.getMem());
        tblFlavor.setName(flavorInfo.getName());
//        tblFlavor.setRootDisk(flavorInfo.getRootDisk());
        tblFlavor.setType(flavorInfo.getType());
        if (null == flavorInfo.getNeedIb())
        {
            tblFlavor.setNeedIb(false);
        }
        else
        {
            tblFlavor.setNeedIb(flavorInfo.getNeedIb());
        }
        if (null == flavorInfo.getType())
        {
            tblFlavor.setType(FlavorType.VM_FLAVOR_TYPE);
        }
        boolean ok = flavorService.save(tblFlavor);

        if (!ok) {
            log.error("insert tbl_rs_flavor error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return FlavorBaseRsp.builder().flavorId(flavorId).build();
    }

    public FlavorMaxNumInfoRsp getFlavorMaxNumInfo() throws WebSystemException
    {
        FlavorMaxNumInfoRsp flavorMaxNumInfoRsp = new FlavorMaxNumInfoRsp();
        flavorMaxNumInfoRsp.setMaxVcpu(null == repoAgentConfig.getMaxVcpu()? AgentConstant.maxVcpu:repoAgentConfig.getMaxVcpu());
        flavorMaxNumInfoRsp.setMaxMemory(null == repoAgentConfig.getMaxMemory()? AgentConstant.maxVmMemory:repoAgentConfig.getMaxMemory());
//        flavorMaxNumInfoRsp.setMaxRootDisk(null == repoAgentConfig.getMaxRootDisk()? AgentConstant.maxRootDisk:repoAgentConfig.getMaxRootDisk());
        return flavorMaxNumInfoRsp;
    }

    public boolean checkFlavorInfo(FlavorCreateReq flavorInfo)
    {
        int maxVcpu = null == repoAgentConfig.getMaxVcpu()? AgentConstant.maxVcpu :repoAgentConfig.getMaxVcpu();
        int maxMemory = null == repoAgentConfig.getMaxMemory()? AgentConstant.maxVmMemory :repoAgentConfig.getMaxMemory();
        int maxRootDisk = null == repoAgentConfig.getMaxRootDisk()? AgentConstant.maxRootDisk :repoAgentConfig.getMaxRootDisk();
        if (flavorInfo.getCpu() > maxVcpu)
        {
            return false;
        }

        return flavorInfo.getMem() <= maxMemory;
//        return flavorInfo.getRootDisk() <= maxRootDisk;
    }
}
