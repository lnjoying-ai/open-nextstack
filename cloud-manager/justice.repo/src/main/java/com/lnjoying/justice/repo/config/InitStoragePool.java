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

package com.lnjoying.justice.repo.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.common.constant.StoragePoolType;
import com.lnjoying.justice.repo.entity.StoragePool;
import com.lnjoying.justice.repo.service.StoragePoolService;
import com.micro.core.common.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@DependsOn({"repoConfigYaml","repoAgentConfig"})
public class InitStoragePool
{
    @Autowired
    private StoragePoolService storagePoolService;


    @Autowired
    private RepoAgentConfig repoAgentConfig;

    @PostConstruct
    void createStoragePool()
    {
        insertStoragePool();
    }

    private void insertStoragePool()
    {
        int poolType;
//        RepoAgentConfig repoAgentConfig = new RepoAgentConfig();
//        repoAgentConfig.setPoolName("gfsPool");
//        repoAgentConfig.setPoolSid("abc");
//        repoAgentConfig.setPoolType("fs");
//        repoAgentConfig.setPoolParas("dir:/vms");
        if (StoragePoolType.FS.equals(repoAgentConfig.getPoolType()))
        {
            poolType = StoragePoolType.POOL_FS_TYPE;
        }
        else
        {
            poolType = StoragePoolType.POOL_BLOCK_TYPE;
        }
        LambdaQueryWrapper<StoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(StoragePool::getPhaseStatus, REMOVED)
                .eq(StoragePool::getSid,repoAgentConfig.getPoolSid())
                .eq(StoragePool::getParas, repoAgentConfig.getPoolParas())
                .eq(StoragePool::getType, poolType);
        if (0 == storagePoolService.count(queryWrapper))
        {

//            String storagePoolId = Utils.assignUUId();

            System.out.println("======repo.config=====");
            System.out.println(repoAgentConfig);
            StoragePool tblStoragePool = new StoragePool();
//            tblStoragePool.setStoragePoolId(storagePoolId);
            tblStoragePool.setStoragePoolId(Utils.assignUUId());
            tblStoragePool.setPhaseStatus(PhaseStatus.ADDED);
            tblStoragePool.setCreateTime(new Date(System.currentTimeMillis()));
            tblStoragePool.setUpdateTime(tblStoragePool.getCreateTime());
            tblStoragePool.setParas(repoAgentConfig.getPoolParas());
            tblStoragePool.setSid(repoAgentConfig.getPoolSid());

            tblStoragePool.setType(poolType);

            tblStoragePool.setName(repoAgentConfig.getPoolName());
//            int result = storagePoolMapper.insert(tblStoragePool);
            boolean ok = storagePoolService.save(tblStoragePool);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
            }
        }
    }
}
