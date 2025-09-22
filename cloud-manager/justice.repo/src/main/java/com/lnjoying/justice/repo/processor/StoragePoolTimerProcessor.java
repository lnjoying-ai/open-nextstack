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

package com.lnjoying.justice.repo.processor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.common.constant.StoragePoolType;
import com.lnjoying.justice.repo.config.RepoAgentConfig;
import com.lnjoying.justice.repo.domain.backend.request.StoragePoolCreateReqFromAgent;
import com.lnjoying.justice.repo.domain.backend.response.BaseRsp;
import com.lnjoying.justice.repo.domain.backend.response.StoragePoolRspFromAgent;
import com.lnjoying.justice.repo.domain.backend.response.StoragePoolsRspFromAgent;
import com.lnjoying.justice.repo.entity.NodeStoragePool;
import com.lnjoying.justice.repo.service.NodeStoragePoolService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class StoragePoolTimerProcessor extends AbstractRunnableProcessor
{

    @Autowired
    private NodeStoragePoolService nodeStoragePoolService;

    @Autowired
    private RepoAgentConfig repoAgentConfig;

    public StoragePoolTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("storagePool timer processor start");
    }

    @Override
    public void stop()
    {
            log.info("storagePool timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processStoragePools(getMiddleStatusStoragePools());
        }
        catch (Exception e)
        {
            log.error("storagePool timer processor exception: {}", e.getMessage());
        }
    }

    private List<NodeStoragePool> getMiddleStatusStoragePools()
    {
        LambdaQueryWrapper<NodeStoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(NodeStoragePool::getPhaseStatus, PhaseStatus.ADDED )
                .ne(NodeStoragePool::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(NodeStoragePool::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(NodeStoragePool::getPhaseStatus,REMOVED);
        return nodeStoragePoolService.list(queryWrapper);
    }

    private void processStoragePools(List<NodeStoragePool> tblNodeStoragePools)
    {
        try
        {
            log.debug("get tblStoragePools :{}", tblNodeStoragePools);
            for ( NodeStoragePool tblStoragePool: tblNodeStoragePools )
            {
                processStoragePool(tblStoragePool);
            }
        }
        catch (Exception e)
        {
            log.error("storagePool timer processor error:  {}", e.getMessage());
        }
    }

    private void processStoragePool(NodeStoragePool tblNodeStoragePool)
    {
        int phaseStatus = tblNodeStoragePool.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateStoragePool(tblNodeStoragePool);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveStoragePool(tblNodeStoragePool);
                    break;
                default:
                    defaultProcessStoragePool(tblNodeStoragePool);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("storagePool timer processor error: storagePoolId {}, phase status {} , exception {}", tblNodeStoragePool.getNodeStoragePoolId(), phaseStatus, e.getMessage());
        }
    }

    private void processCreateStoragePool(NodeStoragePool tblNodeStoragePool)
    {
        try
        {
            String storagePoolIdFromAgent = null;
            boolean needCreate = true;
            List<String> poolIdFromAgents = getStoragePoolsFromAgent(tblNodeStoragePool);
            if(null != poolIdFromAgents && poolIdFromAgents.size() >0)
            {
                String poolIdFromAgent = findStoragePoolId(poolIdFromAgents, tblNodeStoragePool);
                if (null != poolIdFromAgent)
                {
                    storagePoolIdFromAgent = poolIdFromAgent;
                    needCreate = false;
                }
            }
            if (needCreate)
            {
                storagePoolIdFromAgent = createStoragePoolFromAgent(tblNodeStoragePool);
            }
            tblNodeStoragePool.setStoragePoolIdFromAgent(storagePoolIdFromAgent);
            tblNodeStoragePool.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblNodeStoragePool.setPhaseStatus(PhaseStatus.AGENT_ADDING);
            boolean ok = nodeStoragePoolService.updateById(tblNodeStoragePool);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            log.info("created storagePool:{} storagePoolIdFromAgent: {}", tblNodeStoragePool.getNodeStoragePoolId(), storagePoolIdFromAgent);
        }
        catch (WebSystemException e)
        {
            log.error("create storagePool error: storagePoolId {}, {}", tblNodeStoragePool.getNodeStoragePoolId(), e.getMessage());
        }
    }

    private void processRemoveStoragePool(NodeStoragePool tblNodeStoragePool)
    {
        String storagePoolIdFromAgent = removeStoragePoolFromAgent(tblNodeStoragePool);
        if (null == storagePoolIdFromAgent)
        {
            log.info("removeStoragePoolFromAgent error, storagePoolId:{}", tblNodeStoragePool.getStoragePoolId());
            throw new WebSystemException(ErrorCode.SystemError,ErrorLevel.INFO);
        }
        tblNodeStoragePool.setPhaseStatus(PhaseStatus.AGENT_DELETING);
        tblNodeStoragePool.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        nodeStoragePoolService.updateById(tblNodeStoragePool);
    }

    private void defaultProcessStoragePool(NodeStoragePool tblNodeStoragePool)
    {
        try
        {
            StoragePoolRspFromAgent storagePoolRspFromAgent = getStoragePoolStatusFromAgent(tblNodeStoragePool);

            switch (Objects.requireNonNull(Objects.requireNonNull(storagePoolRspFromAgent).getPhase()))
            {
                case AgentConstant.SUCCESS:
                    tblNodeStoragePool.setPhaseStatus(PhaseStatus.ADDED);
                    tblNodeStoragePool.setStoragePoolIdFromAgent(storagePoolRspFromAgent.getStoragePoolIdFromAgent());
                    break;
                case AgentConstant.POOL_NOT_EXIST:
                    tblNodeStoragePool.setPhaseStatus(REMOVED);
                    break;
                case AgentConstant.FAIL:
                    if (storagePoolRspFromAgent.getType().equals(AgentConstant.ADD))
                    {
                        tblNodeStoragePool.setPhaseStatus(PhaseStatus.ADD_FAILED);
                    }
                    else if (storagePoolRspFromAgent.getType().equals(AgentConstant.DEL))
                    {
                        tblNodeStoragePool.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                    }
                    break;
                default:
                    return;
            }
            tblNodeStoragePool.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = nodeStoragePoolService.updateById(tblNodeStoragePool);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("getStoragePoolCreateStatus error:{}, storagePoolId:{}, ", e.getMessage(), tblNodeStoragePool.getNodeStoragePoolId());
        }
    }

    private String createStoragePoolFromAgent(NodeStoragePool tblNodeStoragePool)
    {
        String nodeIp = tblNodeStoragePool.getNodeIp();
        String url = String.format("http://%s:%s%s", nodeIp,repoAgentConfig.getVmAgentPort() , repoAgentConfig.getPoolUrl());
        StoragePoolCreateReqFromAgent storagePoolCreateReqFromAgent = new StoragePoolCreateReqFromAgent();
        storagePoolCreateReqFromAgent.setParas(tblNodeStoragePool.getParas());
        storagePoolCreateReqFromAgent.setSid(tblNodeStoragePool.getSid());
        storagePoolCreateReqFromAgent.setType(StoragePoolType.FS);
        if (StoragePoolType.POOL_FS_TYPE == tblNodeStoragePool.getType())
        {
            storagePoolCreateReqFromAgent.setType(StoragePoolType.FS);
        }
        String jsonString = JsonUtil.objectToJson(storagePoolCreateReqFromAgent);
        BaseRsp result = HttpActionUtil.post(url,jsonString,BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating storagePool  error,  storagePoolId:{}", tblNodeStoragePool.getNodeStoragePoolId());
            return null;
        }

        if (!AgentConstant.PENDING.equals(result.getStatus()))
        {
            String reason = result.getReason();
            if (Objects.equals(reason, AgentConstant.POOL_ALREADY_EXISTS))
            {
                log.info("storagePool already exist, storagePoolId:{}", tblNodeStoragePool.getNodeStoragePoolId());
                return null;
            }
            log.info("create storagePool error, status:{}, result:{}",result.getStatus(), result.getReason());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return result.getUuid();
    }

    private String removeStoragePoolFromAgent(NodeStoragePool tblNodeStoragePool)
    {
        String url = String.format("http://%s:%s/%s/%s", tblNodeStoragePool.getNodeIp(),repoAgentConfig.getVmAgentPort(),
                repoAgentConfig.getPoolUrl(), tblNodeStoragePool.getStoragePoolIdFromAgent());

        BaseRsp result = HttpActionUtil.delete(url,BaseRsp.class);
        if (null == result)
        {
            log.error("get response of removing storagePool error,  sgId:{}", tblNodeStoragePool.getStoragePoolIdFromAgent());
            return null;
        }


        if (AgentConstant.PENDING.equals(result.getStatus()) ||
                (AgentConstant.FAILED.equals(result.getStatus()) && result.getReason().contains(AgentConstant.NOT_EXIST)))
        {
            return tblNodeStoragePool.getStoragePoolIdFromAgent();
        }

        return null;
    }

    private StoragePoolRspFromAgent getStoragePoolStatusFromAgent(NodeStoragePool tblNodeStoragePool)
    {
        try
        {
            String url = String.format("http://%s:%s/%s/%s", tblNodeStoragePool.getNodeIp(),repoAgentConfig.getVmAgentPort(),
                    repoAgentConfig.getPoolUrl(), tblNodeStoragePool.getStoragePoolIdFromAgent());
            StoragePoolRspFromAgent storagePoolRspFromAgent = HttpActionUtil.getObject(url, StoragePoolRspFromAgent.class);
            log.info("url:{}, get result:{}", url, storagePoolRspFromAgent);
            if (AgentConstant.POOL_NOT_EXIST.equals(storagePoolRspFromAgent.getReason()))
            {
                storagePoolRspFromAgent.setPhase(AgentConstant.POOL_NOT_EXIST);
            }
            return storagePoolRspFromAgent;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private List<String> getStoragePoolsFromAgent(NodeStoragePool tblNodeStoragePool)
    {
        try
        {
            String url = String.format("http://%s:%s/%s", tblNodeStoragePool.getNodeIp(),repoAgentConfig.getVmAgentPort(),
                    repoAgentConfig.getPoolUrl());
            StoragePoolsRspFromAgent storagePoolsRspFromAgent = HttpActionUtil.getObject(url, StoragePoolsRspFromAgent.class);
            log.info("url:{}, get result:{}", url, storagePoolsRspFromAgent);
            if (null == storagePoolsRspFromAgent || 0 == storagePoolsRspFromAgent.getPoolIds().size())
            {
                return null;
            }
            return storagePoolsRspFromAgent.getPoolIds();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String findStoragePoolId(List<String> poolIds, NodeStoragePool tblNodeStoragePool)
    {
        String sid = tblNodeStoragePool.getSid();
        Integer poolType = tblNodeStoragePool.getType();
        String pool;
        if (poolType == StoragePoolType.POOL_FS_TYPE) pool = StoragePoolType.FS;
                else pool = StoragePoolType.BLOCK;
        for(String poolId: poolIds)
        {
            String url = String.format("http://%s:%s/%s/%s", tblNodeStoragePool.getNodeIp(),repoAgentConfig.getVmAgentPort(),
                    repoAgentConfig.getPoolUrl(), poolId);
            StoragePoolRspFromAgent storagePoolRspFromAgent = HttpActionUtil.getObject(url, StoragePoolRspFromAgent.class);
            if (Objects.equals(sid, storagePoolRspFromAgent.getSid())
                && pool.equals(storagePoolRspFromAgent.getType()))
            {
                return poolId;
            }
        }
        return null;
    }
}

