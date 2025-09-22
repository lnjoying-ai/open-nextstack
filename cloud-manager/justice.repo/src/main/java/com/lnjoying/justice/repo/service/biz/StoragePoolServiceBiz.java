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
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.common.constant.StoragePoolType;
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.request.StoragePoolCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.StoragePoolBaseRsp;
import com.lnjoying.justice.repo.domain.dto.response.StoragePoolDetailInfoRsp;
import com.lnjoying.justice.repo.domain.dto.response.StoragePoolsRsp;
import com.lnjoying.justice.repo.entity.StoragePool;
import com.lnjoying.justice.repo.entity.search.StoragePoolSearchCritical;
import com.lnjoying.justice.repo.service.StoragePoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service("poolServiceBiz")
@Slf4j
public class StoragePoolServiceBiz
{
    @Autowired
    StoragePoolService storagePoolService;

    public StoragePoolsRsp getStoragePools(StoragePoolSearchCritical storagePoolSearchCritical) throws WebSystemException
    {

        LambdaQueryWrapper<StoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(StoragePool::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(storagePoolSearchCritical.getPoolName())) {
            queryWrapper.like(StoragePool::getName, storagePoolSearchCritical.getPoolName());
        }

        if (null == storagePoolSearchCritical.getPoolType() || StoragePoolType.POOL_FS_TYPE == storagePoolSearchCritical.getPoolType())
        {
            queryWrapper.eq(StoragePool::getType, StoragePoolType.POOL_FS_TYPE);
        }
        else
        {
            queryWrapper.eq(StoragePool::getType, StoragePoolType.POOL_BLOCK_TYPE);
        }
        /*
        if(null!=storagePoolSearchCritical.getStoragePoolName() ){
            criteria.andStoragePoolNameEqualTo(storagePoolSearchCritical.getStoragePoolName());
        }else if (null != storagePoolSearchCritical.getStoragePoolOsVendor()&& null ==storagePoolSearchCritical.getStoragePoolOsType()){
            criteria.andStoragePoolOsVendorEqualTo(storagePoolSearchCritical.getStoragePoolOsVendor());
        }else if (null != storagePoolSearchCritical.getStoragePoolOsVendor() && null !=storagePoolSearchCritical.getStoragePoolOsType()){
            criteria.andStoragePoolOsTypeEqualTo(storagePoolSearchCritical.getStoragePoolOsType())
                    .andStoragePoolOsVendorEqualTo(storagePoolSearchCritical.getStoragePoolOsVendor());
        }else if  ( null !=storagePoolSearchCritical.getStoragePoolOsType() && null == storagePoolSearchCritical.getStoragePoolOsVendor()){
            criteria.andStoragePoolOsTypeEqualTo(storagePoolSearchCritical.getStoragePoolOsType());
        }
         */

        StoragePoolsRsp getStoragePoolsRsp = new StoragePoolsRsp();

        //get total number with example condition
        long totalNum = storagePoolService.count(queryWrapper);

        getStoragePoolsRsp.setTotalNum(totalNum);
        if (totalNum < 1) {
            return getStoragePoolsRsp;
        }

        //query with page number and page size
//        int begin = ((storagePoolSearchCritical.getPageNum() - 1) * storagePoolSearchCritical.getPageSize());

        queryWrapper.orderByDesc(StoragePool::getCreateTime);

        Page<StoragePool> page = new Page<>(storagePoolSearchCritical.getPageNum(),storagePoolSearchCritical.getPageSize());
        Page<StoragePool> storagePoolPage = storagePoolService.page(page, queryWrapper);

        List<StoragePool> storagePools = storagePoolPage.getRecords();
        if (null == storagePools) {
            return getStoragePoolsRsp;
        }

        List<StoragePoolDetailInfoRsp> storagePoolInfos = storagePools.stream().map(tblRsStoragePool -> {
            StoragePoolDetailInfoRsp storagePoolInfo = new StoragePoolDetailInfoRsp();
            storagePoolInfo.setStoragePoolDetailInfoRsp(tblRsStoragePool);
            return storagePoolInfo;
        }).collect(Collectors.toList());

        //set response
        getStoragePoolsRsp.setStoragePools(storagePoolInfos);
        return getStoragePoolsRsp;
    }

    public StoragePoolDetailInfoRsp getStoragePool(String storagePoolId) throws WebSystemException {
        StoragePool storagePool = storagePoolService.getById(storagePoolId);
        if (null == storagePool || REMOVED == storagePool.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }
        StoragePoolDetailInfoRsp storagePoolDetailInfoRsp = new StoragePoolDetailInfoRsp();
        storagePoolDetailInfoRsp.setStoragePoolDetailInfoRsp(storagePool);
        return storagePoolDetailInfoRsp;
    }


    public Object addStoragePool(StoragePoolCreateReq storagePoolInfo) throws WebSystemException {
        String storagePoolId = UUID.randomUUID().toString();
        StoragePool tblStoragePool = new StoragePool();
        tblStoragePool.setStoragePoolId(storagePoolId);
        tblStoragePool.setPhaseStatus(PhaseStatus.ADDED);
        tblStoragePool.setCreateTime(new Date(System.currentTimeMillis()));
        tblStoragePool.setUpdateTime(tblStoragePool.getCreateTime());
        tblStoragePool.setParas(storagePoolInfo.getParas());
        tblStoragePool.setSid(storagePoolInfo.getSid());
        tblStoragePool.setType(storagePoolInfo.getType());
        if (null == storagePoolInfo.getType())
        {
            tblStoragePool.setType(StoragePoolType.POOL_FS_TYPE);
        }
        boolean ok = storagePoolService.save(tblStoragePool);

        if (!ok) {
            log.error("insert tbl_storagePool error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return StoragePoolBaseRsp.builder().storagePoolId(storagePoolId).build();
    }

    public StoragePoolBaseRsp updateStoragePool(String storagePoolId, CommonReq req) throws WebSystemException {
        StoragePool storagePool = storagePoolService.getById(storagePoolId);
        if (null == storagePool || REMOVED == storagePool.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(req.getName())) storagePool.setName(req.getName());
        storagePool.setDescription(req.getDescription());
        if (!storagePoolService.updateById(storagePool))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return StoragePoolBaseRsp.builder().storagePoolId(storagePoolId).build();
    }
}
