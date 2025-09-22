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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.schema.service.repo.StoragePoolService;
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.entity.NodeStoragePool;
import com.lnjoying.justice.repo.entity.StoragePool;
import com.lnjoying.justice.repo.service.NodeStoragePoolService;
import com.micro.core.common.Utils;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@RpcSchema(schemaId = "storagePoolService")
@Slf4j
public class RpcStoragePoolServiceImpl implements StoragePoolService
{
    @Autowired
    private com.lnjoying.justice.repo.service.StoragePoolService storagePoolService;

    @Autowired
    private NodeStoragePoolService nodeStoragePoolService;

    @Override
    public String createNodeStoragePool(@ApiParam(name="nodeIp") String nodeIp,
                                              @ApiParam(name="storagePoolId") String storagePoolId)
    {
        StoragePool tblStoragePool = storagePoolService.getById(storagePoolId);
        if (null == tblStoragePool || REMOVED == tblStoragePool.getPhaseStatus()) return null;
        LambdaQueryWrapper<NodeStoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NodeStoragePool::getStoragePoolId, storagePoolId)
                .eq(NodeStoragePool::getNodeIp, nodeIp)
                .ne(NodeStoragePool::getPhaseStatus, REMOVED);
        if (nodeStoragePoolService.count(queryWrapper) > 0)
        {
            return nodeStoragePoolService.getOne(queryWrapper).getNodeStoragePoolId();
        }

        String nodeStoragePoolId = Utils.assignUUId();
        NodeStoragePool tblNodeStoragePool = new NodeStoragePool();
        tblNodeStoragePool.setNodeStoragePoolId(nodeStoragePoolId);
        tblNodeStoragePool.setStoragePoolId(tblStoragePool.getStoragePoolId());
        tblNodeStoragePool.setNodeIp(nodeIp);
        tblNodeStoragePool.setPhaseStatus(PhaseStatus.ADDING);
        tblNodeStoragePool.setSid(tblStoragePool.getSid());
        tblNodeStoragePool.setType(tblStoragePool.getType());
        tblNodeStoragePool.setParas(tblStoragePool.getParas());
//            tblNodeStoragePool.setUserId(tblStoragePool.getUserId());
        tblNodeStoragePool.setCreateTime(new Date(System.currentTimeMillis()));
        tblNodeStoragePool.setUpdateTime(tblNodeStoragePool.getCreateTime());
        boolean ok = nodeStoragePoolService.save(tblNodeStoragePool);
        if(!ok)
        {
            log.info("update database error, nodeStoragePoolId:{}",nodeStoragePoolId);
            return null;
        }
        return tblNodeStoragePool.getNodeStoragePoolId();
    }

    @Override
    public String getNodeStoragePoolPhaseStatus(@ApiParam(name="nodeIp") String nodeIp,
                                        @ApiParam(name="storagePoolId") String storagePoolId)
    {
        LambdaQueryWrapper<NodeStoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NodeStoragePool::getStoragePoolId, storagePoolId)
                .eq(NodeStoragePool::getNodeIp, nodeIp)
                .ne(NodeStoragePool::getPhaseStatus, REMOVED)
                .ne(NodeStoragePool::getPhaseStatus, PhaseStatus.DELETING)
                .ne(NodeStoragePool::getPhaseStatus, PhaseStatus.AGENT_DELETING);
        if (nodeStoragePoolService.count(queryWrapper) > 0)
        {
            Integer phaseStatus = nodeStoragePoolService.getOne(queryWrapper).getPhaseStatus();
            if (PhaseStatus.ADDED == phaseStatus)
            {
                return AgentConstant.OK;
            }
            else if (PhaseStatus.ADD_FAILED == phaseStatus || PhaseStatus.DELETE_FAILED == phaseStatus)
            {
                return AgentConstant.FAILED;
            }
            else
            {
                return null;
            }
        }
        return AgentConstant.REMOVED;
    }
}
