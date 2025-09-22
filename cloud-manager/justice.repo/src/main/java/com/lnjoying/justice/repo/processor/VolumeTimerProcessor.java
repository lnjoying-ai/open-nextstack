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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.common.constant.VolumeType;
import com.lnjoying.justice.repo.config.RepoAgentConfig;
import com.lnjoying.justice.repo.domain.backend.request.VolumeAttachReqFromAgent;
import com.lnjoying.justice.repo.domain.backend.request.VolumeCreateReqFromAgent;
import com.lnjoying.justice.repo.domain.backend.request.VolumeExportReqFromAgent;
import com.lnjoying.justice.repo.domain.backend.response.BaseRsp;
import com.lnjoying.justice.repo.domain.backend.response.VolumeRspFromAgent;
import com.lnjoying.justice.repo.entity.*;
import com.lnjoying.justice.repo.service.*;
import com.lnjoying.justice.repo.service.biz.CombRpcSerice;
import com.lnjoying.justice.repo.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class VolumeTimerProcessor extends AbstractRunnableProcessor
{
    @Autowired
    private VolumeService volumeService;

    @Autowired
    private StoragePoolService storagePoolService;

    @Autowired
    private RepoAgentConfig repoAgentConfig;

    @Autowired
    private NodeStoragePoolService nodeStoragePoolService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private NodeImageService nodeImageService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    public VolumeTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("volume timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("volume timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processVolumes(getMiddleStatusSecurityGroups());
        }
        catch (Exception e)
        {
            log.error("volume timer processor exception: {}", e.getMessage());
        }
    }

    private List<Volume> getMiddleStatusSecurityGroups()
    {
        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Volume::getPhaseStatus, PhaseStatus.ATTACHED )
                .ne(Volume::getPhaseStatus, PhaseStatus.ATTACH_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.DETACHED)
                .ne(Volume::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.UPDATED)
                .ne(Volume::getPhaseStatus,PhaseStatus.UPDATE_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.EXPORT_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.SUSPEND_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.DETACH_FAILED)
                .ne(Volume::getPhaseStatus, PhaseStatus.RESUME_FAILED)
                .ne(Volume::getPhaseStatus,REMOVED);
        return volumeService.list(queryWrapper);
    }

    private void processVolumes(List<Volume> tblVolumes)
    {
        try
        {
            log.debug("get tblVolumes :{}", tblVolumes);
            for ( Volume tblVolume: tblVolumes )
            {
                processVolume(tblVolume);
            }
        }
        catch (Exception e)
        {
            log.error("volumes timer processor error:  {}", e.getMessage());
        }
    }
    // volume 分为 rootDisk 和 dataDisk。
    // 如果为dataDisk 又分2种情况：1. 先建volume，再挂载 2. 新建虚机的同时，也创建volume
    // 第2种情况，需要在processAttachVolume时，先进行attachPhase状态的判断，如果为Attached,直接设置为Attached状态。

    private void processVolume(Volume tblVolume)
    {
        int phaseStatus = tblVolume.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateVolume(tblVolume);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveVolume(tblVolume);
                    break;
                case PhaseStatus.ATTACHING:
                    processAttachVolume(tblVolume);
                    break;
                case PhaseStatus.DETACHING:
                    processDetachVolume(tblVolume);
                    break;
                case PhaseStatus.AGENT_ATTACHING:
                    getAttachProcessVolume(tblVolume);
                    break;
                case PhaseStatus.AGENT_DETACHING:
                    getDetachProcessVolume(tblVolume);
                    break;
                case PhaseStatus.SUSPENDING:
                    processSuspendVolume(tblVolume);
                    break;
                case PhaseStatus.PRE_DEST_RESUMING:
                    processPreDestResumeVolume(tblVolume);
                    break;
                case PhaseStatus.RESUMING:
                    processResumeVolume(tblVolume);
                    break;
                case PhaseStatus.EXPORTING:
                    processExportVolume(tblVolume);
                    break;
                case PhaseStatus.AGENT_EXPORTING:
                    processGetExportVolume(tblVolume);
                    break;
                default:
                    defaultProcessVolume(tblVolume);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("volume timer processor error: volumeId {}, phase status {} , exception {}", tblVolume.getVolumeId(), phaseStatus, e.getMessage());
        }
    }

    private void processExportVolume(Volume tblVolume)
    {
        try
        {
            String ok = exportVolumeFromAgent(tblVolume);
            assert ok != null;
            if (ok.equals(AgentConstant.FAILED))
            {
                log.info("suspendVolume error, volumeId:{}", tblVolume.getVolumeId());
                tblVolume.setPhaseStatus(PhaseStatus.EXPORT_FAILED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                volumeService.updateById(tblVolume);
                return;
            }
            if (ok.equals(AgentConstant.OK))
            {
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                tblVolume.setPhaseStatus(PhaseStatus.AGENT_EXPORTING);
                volumeService.updateById(tblVolume);
            }
        }
        catch (Exception e)
        {
            log.error("export volume error: volumeId {}, imageName {},{}", tblVolume.getVolumeId(), tblVolume.getExportName(), e.getMessage());
        }
    }

     void processGetExportVolume(Volume tblVolume)
    {
        VolumeRspFromAgent volumeRspFromAgent = getVolumeStatusFromAgent(tblVolume);
        assert volumeRspFromAgent != null;
        Image tblImage = new Image();
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        if (volumeRspFromAgent.getPhase().equals(AgentConstant.EXPORT_FAILED))
        {
               tblImage.setPhaseStatus(PhaseStatus.ADD_FAILED);
        }
        else if (volumeRspFromAgent.getPhase().equals(AgentConstant.EXPORTED))
        {
            tblImage.setPhaseStatus(PhaseStatus.ADDED);

//            tblImage.setUserId(tblVolume.getUserId());
        }
        else
        {
            dataSourceTransactionManager.commit(transactionStatus);
            return;
        }
        LambdaQueryWrapper<Image> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.ne(Image::getPhaseStatus, REMOVED)
                .eq(Image::getImageId, tblVolume.getExportName());
        tblVolume.setPhaseStatus(PhaseStatus.ATTACHED);
        tblImage.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = volumeService.updateById(tblVolume);
        if (ok)
        {
            Image imageInfo = imageService.getOne(imageWrapper);
            if (null == imageInfo || REMOVED == imageInfo.getPhaseStatus())
            {
                log.error("get image info failed, imageId:{}", tblVolume.getExportName());
                dataSourceTransactionManager.rollback(transactionStatus);
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            ok = imageService.update(tblImage, imageWrapper);
            if (ok)
            {
                // nodeImage 的处理
                if (null == createNodeImage(tblVolume, imageInfo.getImageId()))
                {
                    log.error("create node image failed, volumeId:{}", tblVolume.getVolumeId());
                    dataSourceTransactionManager.rollback(transactionStatus);
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
                dataSourceTransactionManager.commit(transactionStatus);
            }
            else
            {
                dataSourceTransactionManager.rollback(transactionStatus);
            }
        }
        else
        {
            dataSourceTransactionManager.rollback(transactionStatus);
        }
    }

    private void processCreateVolume(Volume tblVolume)
    {
        try
        {
            String volumeIdFromAgent = createVolumeFromAgent(tblVolume);
            if (null == volumeIdFromAgent)
            {
                return;
            }
            tblVolume.setVolumeIdFromAgent(volumeIdFromAgent);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblVolume.setPhaseStatus(PhaseStatus.AGENT_ADDING);
            boolean ok = volumeService.updateById(tblVolume);
            log.info("created volume:{} volumeIdFromAgent: {}", tblVolume.getVolumeId(), volumeIdFromAgent);
            if (ok)
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在创建云盘", String.format("请求参数: storagePoolId:%s volumeId:%s 大小:%d GB", tblVolume.getStoragePoolId(),tblVolume.getVolumeId(), tblVolume.getSize()), "创建中");
            }
        }
        catch (WebSystemException e)
        {
            log.error("create volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());
        }
    }

    private void processRemoveVolume(Volume tblVolume)
    {
        try
        {
            Boolean ok;
            if (VolumeType.DATA_DISK ==tblVolume.getType())
            {
                ok = combRpcSerice.getVmService().detachVolume(tblVolume.getVolumeId());
                if (!ok)
                {
                    log.info("detachVolume from vm service error, volumeId:{}", tblVolume.getVolumeId());
                    return;
                }
            }
            if (StrUtil.isBlank(tblVolume.getVolumeIdFromAgent()))
            {
                tblVolume.setPhaseStatus(REMOVED);
                volumeService.updateById(tblVolume);
                return ;
            }
            String volumeIdFromAgent = removeVolumeFromAgent(tblVolume);
            if (null == volumeIdFromAgent)
            {
                log.info("removeVolumeFromAgent error, volumeId:{}", tblVolume.getVolumeId());
                return;
            }
            tblVolume.setPhaseStatus(PhaseStatus.AGENT_DELETING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            ok = volumeService.updateById(tblVolume);
            if (ok)
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在删除云盘", String.format("请求参数: volumeId:%s", tblVolume.getVolumeId()), "删除中");
            }
        }
        catch (WebSystemException e)
        {
            log.error("remove volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());
        }
    }

    private void processAttachVolume(Volume tblVolume)
    {
        try
        {
            VmService.NodeIpAndVmAgentId nodeIpAndVmAgentId = combRpcSerice.getVmService().getNodeIpAndVmAgentIdByVmId(tblVolume.getVmId());
            if (null == nodeIpAndVmAgentId)
            {
                log.info("getNodeIpAndVmAgentIdByVmId null, volumeId:{}", tblVolume.getVolumeId());
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
                volumeService.updateById(tblVolume);
                return;
            }
            String nodeIp = nodeIpAndVmAgentId.getNodeIp();
            String vmIdFromAgent = nodeIpAndVmAgentId.getVmIdFromAgent();
            if (StrUtil.isBlank(nodeIp)) return;
            if (StrUtil.isBlank(tblVolume.getNodeIp()))
            {
                setNodeIpPhase(tblVolume, nodeIp);
                return;
            }
            else if ( !nodeIp.equals(tblVolume.getNodeIp()))
            {
                setDestIpPhase(tblVolume, nodeIp);
                return;
            }
            //  从Agent获取状态，如果已经是Attached 状态，直接设置为Attached, 不用走下面的流程
            // 新建虚机时，同时指定了数据盘就会出现Attached 状态
            VolumeRspFromAgent volumeRspFromAgent = getVolumeStatusFromAgent(tblVolume);
            log.info("attach volume, volumeRspFromAgent: {}, volumeId:{}", volumeRspFromAgent, tblVolume.getVolumeId());
            if (null != volumeRspFromAgent && volumeRspFromAgent.getPhaseType().equals(AgentConstant.ADD) &&
                    AgentConstant.ADDED.equals(volumeRspFromAgent.getPhase())
                     && !StrUtil.isBlank(volumeRspFromAgent.getVmIdFromAgent()))
            {
                tblVolume.setPhaseStatus(PhaseStatus.ATTACHED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                volumeService.updateById(tblVolume);
                return;
            }
            if (StrUtil.isBlank(vmIdFromAgent) && AgentConstant.FAILED.equals(nodeIpAndVmAgentId.getStatus()))
            {
                tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                volumeService.updateById(tblVolume);
                return;
            }
            if (StrUtil.isBlank(vmIdFromAgent)) return;

            String ok = attachVolumeFromAgent(tblVolume,vmIdFromAgent);
            assert ok != null;
            if (ok.equals(AgentConstant.FAILED))
            {
                log.info("attachVolumeFromAgent error, volumeId:{}, status:{}", tblVolume.getVolumeId(), ok);
                tblVolume.setPhaseStatus(PhaseStatus.ATTACH_FAILED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                if (volumeService.updateById(tblVolume))
                {
                    logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 挂载云盘失败", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), "挂载失败");
                }
                return;
                //throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            else if (ok.equals(AgentConstant.PENDING))
            {
                return;
            }
            else if (!ok.equals(AgentConstant.OK))
            {
                log.info("attachVolumeFromAgent error, volumeId:{}, status:{}", tblVolume.getVolumeId(), ok);
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            tblVolume.setPhaseStatus(PhaseStatus.AGENT_ATTACHING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean dbOk = volumeService.updateById(tblVolume);
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在挂载云盘", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), "挂载中");
            }

        }
        catch (WebSystemException e)
        {
            log.error("attach volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());
        }
    }

    private void processPreDestResumeVolume(Volume tblVolume)
    {
        try
        {
            VolumeRspFromAgent volumeRspFromAgent = getVolumeStatusFromAgent(tblVolume);
            assert volumeRspFromAgent != null;
            if (AgentConstant.SUSPENDED.equals(volumeRspFromAgent.getPhase()))
            {
                tblVolume.setNodeIp(tblVolume.getDestIp());
//                resumeVolumeFromAgent(tblVolume);
                tblVolume.setPhaseStatus(PhaseStatus.RESUMING);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                volumeService.updateById(tblVolume);
            }
            else if (AgentConstant.SUSPEND_FAILED.equals(volumeRspFromAgent.getPhase()))
            {
                tblVolume.setPhaseStatus(PhaseStatus.SUSPEND_FAILED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                volumeService.updateById(tblVolume);
            }
        }
        catch (WebSystemException e)
        {
            log.error("PreDestResume volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());
        }
    }

    private void processSuspendVolume(Volume tblVolume)
    {
        try
        {
            String ok = suspendVolumeFromAgent(tblVolume);
            assert ok != null;
            if (ok.equals(AgentConstant.FAILED))
            {
                log.info("suspendVolume error, volumeId:{}", tblVolume.getVolumeId());
                tblVolume.setPhaseStatus(PhaseStatus.SUSPEND_FAILED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                boolean dbOk = volumeService.updateById(tblVolume);
                if (dbOk)
                {
                    if (StrUtil.isBlank(tblVolume.getVmId()))
                    {
                        logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 挂起云盘失败", String.format("请求参数: volumeId:%s", tblVolume.getVolumeId()), "挂起失败");
                    }
                    else
                    {
                        logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 挂起虚机失败", String.format("请求参数: vmId:%s", tblVolume.getVmId()), "挂起失败");
                    }
                }
                return;
                //throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            else if (!ok.equals(AgentConstant.OK))
            {
                log.info("suspend Volume error, volumeId:{}", tblVolume.getVolumeId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            tblVolume.setPhaseStatus(PhaseStatus.AGENT_SUSPENDING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean dbOk = volumeService.updateById(tblVolume);
            if (dbOk)
            {
                if(StrUtil.isBlank(tblVolume.getVmId()))
                {
                    logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在挂起云盘", String.format("请求参数: volumeId:%s", tblVolume.getVolumeId()), "挂起中");
                }
                else
                {
                    logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在挂起虚机", String.format("请求参数: vmInstanceId:%s", tblVolume.getVmId()), "挂起中");
                }
            }
        }
        catch (Exception e)
        {
            log.error("suspend volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());

        }
    }


    private void processResumeVolume(Volume tblVolume)
    {
        try
        {
            String ok = resumeVolumeFromAgent(tblVolume);
            assert ok != null;
            if (ok.equals(AgentConstant.FAILED))
            {
                log.info("resumeVolume error, volumeId:{}", tblVolume.getVolumeId());
                tblVolume.setPhaseStatus(PhaseStatus.RESUME_FAILED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                boolean dbOk = volumeService.updateById(tblVolume);
                if (dbOk)
                {
                    if(StrUtil.isBlank(tblVolume.getVmId()))
                    {
                        logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 恢复云盘失败", String.format("请求参数: volumeId:%s", tblVolume.getVolumeId()), "恢复失败");
                    }
                    else
                    {
                        logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 恢复虚机失败",String.format("请求参数: vmId:%s", tblVolume.getVmId()), "恢复失败");
                    }
                }
                return;
                //throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            else if (!ok.equals(AgentConstant.OK))
            {
                log.info("resumeVolume error, volumeId:{}", tblVolume.getVolumeId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            tblVolume.setPhaseStatus(PhaseStatus.AGENT_RESUMING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean dbOk = volumeService.updateById(tblVolume);
            if (dbOk)
            {
                if(StrUtil.isBlank(tblVolume.getVmId()))
                {
                    logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在恢复云盘", String.format("请求参数: volumeId:%s", tblVolume.getVolumeId()), "恢复中");
                }
                else
                {
                    logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在恢复虚机", String.format("请求参数: vmId:%s", tblVolume.getVmId()), "恢复中");
                }
            }
        }
        catch (WebSystemException e)
        {
            log.error("resume volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());
        }
    }

    private void processDetachVolume(Volume tblVolume)
    {
        try
        {
            String ok = detachVolumeFromAgent(tblVolume);
            assert ok != null;
            if(ok.equals(AgentConstant.FAILED))
            {
                log.info("detachVolumeFromAgent error, volumeId:{}", tblVolume.getVolumeId());
                tblVolume.setPhaseStatus(PhaseStatus.DETACH_FAILED);
                tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                boolean dbOk = volumeService.updateById(tblVolume);
                if (dbOk)
                {
                    logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 卸载云盘失败", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), "卸载失败");
                }
                return;
            }
            else if (!ok.equals(AgentConstant.OK))
            {
                log.info("detachVolumeFromAgent error, volumeId:{}", tblVolume.getVolumeId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            tblVolume.setPhaseStatus(PhaseStatus.AGENT_DETACHING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean dbOk = volumeService.updateById(tblVolume);
            if (dbOk)
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在卸载云盘", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), "卸载中");
            }
        }
        catch (WebSystemException e)
        {
            log.error("detach volume error: volumeId {}, {}", tblVolume.getVolumeId(), e.getMessage());
        }
    }

    private void defaultProcessVolume(Volume tblVolume)
    {
        try
        {
            VolumeRspFromAgent volumeRspFromAgent = getVolumeStatusFromAgent(tblVolume);
            String result = null;
            switch (Objects.requireNonNull(Objects.requireNonNull(volumeRspFromAgent).getPhase()))
            {
                case AgentConstant.ADDED:
                    if (VolumeType.DATA_DISK == tblVolume.getType())
                    {
                        //created new data disk;
                        if (combRpcSerice.getVmService().isVmOkByVmId(tblVolume.getVmId()))
                        {
                            tblVolume.setPhaseStatus(PhaseStatus.ATTACHING);
                            result = "挂载中";
                        }
                        else
                        {
                            tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
                            result = "未挂载";
                        }
                    }
                    else
                    {
                        tblVolume.setPhaseStatus(PhaseStatus.ATTACHED);
                        result = "已挂载";
                    }
                    break;
                case AgentConstant.VOLUME_NOT_EXIST:
                    tblVolume.setPhaseStatus(REMOVED);
                    result = "已删除";
                    break;
                case AgentConstant.ADD_FAILED:
                    tblVolume.setPhaseStatus(PhaseStatus.ADD_FAILED);
                    result = "创建失败";
                    break;
                case AgentConstant.DELETE_FAILED:
                    tblVolume.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                    result = "删除失败";
                    break;
                case AgentConstant.SUSPENDED:
                    tblVolume.setPhaseStatus(PhaseStatus.SUSPEND);
                    result = "已暂停";
                    break;
                case AgentConstant.RESUMED:
//                    tblVolume.setVolumeIdFromAgent(volumeRspFromAgent.getVolumeIdFromAgent());
                    tblVolume.setPhaseStatus(PhaseStatus.ATTACHING);
                    result = "挂载中";
                    break;
                default:
                    return;
            }
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = volumeService.updateById(tblVolume);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 正在获取云盘状态", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), result);
        }
        catch (Exception e)
        {
            log.error("getVolumeStatus error:{}, volumeId:{}, ", e.getMessage(), tblVolume.getVolumeId());
        }
    }

    private void getAttachProcessVolume(Volume tblVolume)
    {
        try
        {
            VolumeRspFromAgent volumeRspFromAgent = getVolumeStatusFromAgent(tblVolume);
            switch (Objects.requireNonNull(Objects.requireNonNull(volumeRspFromAgent).getPhase()))
            {
                case AgentConstant.ATTACHED:
                    tblVolume.setPhaseStatus(PhaseStatus.ATTACHED);
                    break;
                case AgentConstant.ATTACH_FAILED:
                    tblVolume.setPhaseStatus(PhaseStatus.ATTACH_FAILED);
                    break;
                default: return;
            }
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = volumeService.updateById(tblVolume);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            if (PhaseStatus.ATTACHED == tblVolume.getPhaseStatus())
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 挂载云盘成功", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), "挂载成功");
            }
            else
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 挂载云盘失败", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), tblVolume.getVmId()), "挂载失败");
            }
        }
        catch (WebSystemException e)
        {
            log.error("getVolumeAttachStatus error:{}, volumeId:{}, ", e.getMessage(), tblVolume.getVolumeId());
        }
    }

    private void getDetachProcessVolume(Volume tblVolume)
    {
        try
        {
            String vmInstanceId = tblVolume.getVmId();
            VolumeRspFromAgent volumeRspFromAgent = getVolumeStatusFromAgent(tblVolume);
            switch (Objects.requireNonNull(Objects.requireNonNull(volumeRspFromAgent).getPhase()))
            {
                case AgentConstant.DETACHED:
                    Boolean ok = combRpcSerice.getVmService().detachVolume(tblVolume.getVolumeId());
                    if (!ok)
                    {
                        log.info("detachVolume from vm service error, volumeId:{}", tblVolume.getVolumeId());
                        return;
                    }
                    tblVolume.setVmId(null);
                    tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
                    tblVolume.setLastIp(tblVolume.getNodeIp());
                    tblVolume.setDestIp("");
                    break;
                case AgentConstant.DETACH_FAILED:
                    tblVolume.setPhaseStatus(PhaseStatus.DETACH_FAILED);
                    break;
                default:
                    if (StrUtil.isBlank(volumeRspFromAgent.getVmIdFromAgent()))
                    {
                        tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
                    }
            }
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = volumeService.updateById(tblVolume);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            if (PhaseStatus.DETACHED == tblVolume.getPhaseStatus())
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 卸载云盘成功", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), vmInstanceId), "卸载成功");
            }
            else
            {
                logRpcService.getLogService().addEvent(tblVolume.getUserId(), "Agent 卸载云盘失败", String.format("请求参数: volumeId:%s vmInstanceId:%s", tblVolume.getVolumeId(), vmInstanceId), "卸载失败");
            }
        }
        catch (WebSystemException e)
        {
            log.error("getVolumeDetachStatus error:{}, volumeId:{}, ", e.getMessage(), tblVolume.getVolumeId());
        }
    }

    // in order to create a new data disk successfully,you must set nodeIp.
    private void setNodeIpPhase(Volume tblVolume, String nodeIp)
    {
        tblVolume.setNodeIp(nodeIp);
        tblVolume.setPhaseStatus(PhaseStatus.ADDING);
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        volumeService.updateById(tblVolume);
    }

    private void setDestIpPhase(Volume tblVolume, String nodeIp)
    {
        String ok = suspendVolumeFromAgent(tblVolume);
        if (AgentConstant.OK.equals(ok))
        {
            tblVolume.setDestIp(nodeIp);
            tblVolume.setPhaseStatus(PhaseStatus.PRE_DEST_RESUMING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            volumeService.updateById(tblVolume);
        }
    }



    private String createVolumeFromAgent(Volume tblVolume)
    {
        if (StrUtil.isBlank(tblVolume.getNodeIp()))
        {
            log.info("wait for updating nodeIp volumeId:{}", tblVolume.getVolumeId());
            return null;
        }

        StoragePool tblPool = storagePoolService.getById(tblVolume.getStoragePoolId());
        if (null == tblPool) throw  new WebSystemException(ErrorCode.STORAGE_POOL_NOT_EXIST,ErrorLevel.INFO);
        LambdaQueryWrapper<NodeStoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NodeStoragePool::getNodeIp, tblVolume.getNodeIp())
                .eq(NodeStoragePool::getStoragePoolId, tblVolume.getStoragePoolId())
                .ne(NodeStoragePool::getPhaseStatus, REMOVED);
        NodeStoragePool nodeStoragePool = nodeStoragePoolService.getOne(queryWrapper);
        if (StrUtil.isBlank(nodeStoragePool.getStoragePoolIdFromAgent())) return null;
        String url = String.format("http://%s:%s%s/%s/vols", tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort() , repoAgentConfig.getPoolUrl(),
                nodeStoragePool.getStoragePoolIdFromAgent());
        VolumeCreateReqFromAgent volumeCreateReqFromAgent = new VolumeCreateReqFromAgent();
        volumeCreateReqFromAgent.setSize(tblVolume.getSize());
        if(VolumeType.ROOT_DISK == tblVolume.getType())
        {
            volumeCreateReqFromAgent.setIsRoot(true);
        }
        else
        {
            volumeCreateReqFromAgent.setIsRoot(false);
        }
        if (!StrUtil.isBlank(tblVolume.getImageId()))
        {
            LambdaQueryWrapper<NodeImage> nodeImageLambdaQueryWrapper = new LambdaQueryWrapper<>();
            nodeImageLambdaQueryWrapper.eq(NodeImage::getImageId, tblVolume.getImageId())
                            .eq(NodeImage::getNodeIp, tblVolume.getNodeIp())
                                    .ne(NodeImage::getPhaseStatus, REMOVED);
            NodeImage nodeImage = nodeImageService.getOne(nodeImageLambdaQueryWrapper);
            if (null == nodeImage || StrUtil.isBlank(nodeImage.getNodeImageIdFromAgent())) return null;
            volumeCreateReqFromAgent.setImageId(nodeImage.getNodeImageIdFromAgent());
//            volumeCreateReqFromAgent.setIsRoot(true);
        }
        String jsonString = JsonUtil.objectToJson(volumeCreateReqFromAgent);
        BaseRsp result = HttpActionUtil.post(url,jsonString, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating volume  error,  volumeId:{}", tblVolume.getVolumeId());
            return null;
        }

        if (!AgentConstant.PENDING.equals(result.getStatus()))
        {
            log.info("create volume error, status:{}, result:{}",result.getStatus(), result);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return result.getUuid();
    }

    private String removeVolumeFromAgent(Volume tblVolume)
    {
        String url = String.format("http://%s:%s%s/%s", tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(), tblVolume.getVolumeIdFromAgent());

        BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of removing volume error,  sgId:{}", tblVolume.getVolumeIdFromAgent());
            return null;
        }


        if (AgentConstant.PENDING.equals(result.getStatus()) ||
                (AgentConstant.FAILED.equals(result.getStatus()) && result.getReason().contains(AgentConstant.NOT_EXIST)))
        {
            return tblVolume.getVolumeIdFromAgent();
        }

        return null;
    }

    private VolumeRspFromAgent getVolumeStatusFromAgent(Volume tblVolume)
    {
        try
        {
            String url = String.format("http://%s:%s%s/%s", tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(), tblVolume.getVolumeIdFromAgent());
            VolumeRspFromAgent volumeRspFromAgent = HttpActionUtil.getObject(url, VolumeRspFromAgent.class);
            log.info("url:{}, get result:{}", url, volumeRspFromAgent);
            if (AgentConstant.VOLUME_NOT_EXIST.equals(volumeRspFromAgent.getReason()))
            {
                volumeRspFromAgent.setPhase(AgentConstant.VOLUME_NOT_EXIST);
                return  volumeRspFromAgent;
            }
            String phaseType = volumeRspFromAgent.getPhaseType();
            switch (phaseType)
            {
                case "add":
                    if (AgentConstant.SUCCESS.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.ADDED);
                    }
                    else if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.ADD_FAILED);
                    }
                    break;
                case "del":
                    if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.DELETE_FAILED);
                    }
                    break;
                case "attach":
                    if (AgentConstant.SUCCESS.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.ATTACHED);
                    }
                    else if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.ATTACH_FAILED);
                    }
                case "detach":
                    if (AgentConstant.SUCCESS.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.DETACHED);
                    }
                    else if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        if (StrUtil.isBlank(volumeRspFromAgent.getVmIdFromAgent()))
                        {
                            volumeRspFromAgent.setPhase(AgentConstant.DETACHED);
                        }
                        else
                        {
                            volumeRspFromAgent.setPhase(AgentConstant.DETACHING);
                        }
                        volumeRspFromAgent.setPhase(AgentConstant.DETACH_FAILED);
                    }
                    break;
                case "export":
                    if (AgentConstant.SUCCESS.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.EXPORTED);
                    }
                    else if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.EXPORT_FAILED);
                    }
                    break;
                case  "suspend":
                    if (AgentConstant.SUCCESS.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.SUSPENDED);
                    }
                    else if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.SUSPEND_FAILED);
                    }
                    break;
                case "resume":
                    if (AgentConstant.SUCCESS.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.RESUMED);
                    }
                    else if (AgentConstant.FAIL.equals(volumeRspFromAgent.getPhase()))
                    {
                        volumeRspFromAgent.setPhase(AgentConstant.RESUME_FAILED);
                    }
                    break;
            }
            return volumeRspFromAgent;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String attachVolumeFromAgent(Volume tblVolume, String vmIdFromAgent)
    {
        String url = String.format("http://%s:%s%s/%s/attach",
                tblVolume.getNodeIp(),repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        VolumeAttachReqFromAgent volumeAttachReqFromAgent = new VolumeAttachReqFromAgent();
        volumeAttachReqFromAgent.setVmId(vmIdFromAgent);
        log.info("url:{}, req:{}", url, volumeAttachReqFromAgent);
        String jsonString = JsonUtil.objectToJson(volumeAttachReqFromAgent);
        try
        {
            return putArgsFromAgent(url, jsonString);
        }
        catch (Exception e)
        {
            log.error("attach volume error:{}", e.getMessage());
            return null;
        }
    }

    private String detachVolumeFromAgent(Volume tblVolume)
    {
        String url = String.format("http://%s:%s%s/%s/detach",
                tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        try
        {
            return putArgsFromAgent(url, null);
        }
        catch (Exception e)
        {
            log.error("detach volume error:{}", e.getMessage());
            return null;
        }
    }

    private String suspendVolumeFromAgent(Volume tblVolume)
    {
        String url = String.format("http://%s:%s%s/%s/suspend",
                tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        try
        {
            return putArgsFromAgent(url, null);
        }
        catch (Exception e)
        {
            log.error("suspend volume error:{}", e.getMessage());
            return null;
        }
    }

    private String resumeVolumeFromAgent(Volume tblVolume)
    {
        String url = String.format("http://%s:%s%s/%s/resume",
                tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        try
        {
            return putArgsFromAgent(url, null);
        }
        catch (Exception e)
        {
            log.error("resume volume error:{}", e.getMessage());
            return null;
        }
    }

    private String exportVolumeFromAgent(Volume tblVolume)
    {
        String url = String.format("http://%s:%s%s/%s/export",
                tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        VolumeExportReqFromAgent volumeExportReqFromAgent = new VolumeExportReqFromAgent();
//        volumeExportReqFromAgent.setImageName(tblVolume.get);
        volumeExportReqFromAgent.setImageName(tblVolume.getExportName());
        String jsonString = JsonUtil.objectToJson(volumeExportReqFromAgent);
        try
        {
            return putArgsFromAgent(url, jsonString);
        }
        catch (Exception e)
        {
            log.error("export volume error:{}", e.getMessage());
            return null;
        }
    }

    public static String putArgsFromAgent(String url, String jsonString)
    {
        BaseRsp result = HttpActionUtil.put(url,jsonString, BaseRsp.class);
        if (null == result)
        {
            log.error("get response error,  url:{}, httpAction: put, jsonStr:{}", url, jsonString);
            return null;
        }

        String status = result.getStatus();
        if (AgentConstant.FAILED.equals(status) && (AgentConstant.VOLUME_NOT_ATTACHED.equals(result.getReason())
        ||AgentConstant.VOLUME_ALREADY_ATTACHED.equals(result.getReason())))
        {
            return AgentConstant.OK;
        }
        else if (AgentConstant.FAILED.equals(status) &&
                result.getReason().contains(AgentConstant.NOT_READY))
        {
            return AgentConstant.PENDING;
        }
        else if (AgentConstant.FAILED.equals(status) &&
                StrUtil.isNotBlank(result.getReason()))
        {
            log.info("get response error, url:{}, httpAction: put, jsonStr:{},status:{}, result:{}",url, jsonString,status, result);
            String reason = result.getReason();
            if (AgentConstant.VOLUME_ATTACHING.equals(reason)) return AgentConstant.OK;

            return AgentConstant.FAILED;
        }

        if (!AgentConstant.PENDING.equals(status))
        {

            log.info("get response error, url:{}, httpAction: put, jsonStr:{},status:{}, result:{}",url, jsonString,status, result);
            return null;
        }
        return AgentConstant.OK;
    }

    public String createNodeImage(Volume tblVolume, String imageId)
    {
        String poolId = getStoragePoolIdFromAgent(tblVolume.getNodeIp());
        if (StrUtil.isBlank(poolId)) return  null;
        LambdaQueryWrapper<NodeImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NodeImage::getImageId, imageId)
                .eq(NodeImage::getNodeIp, tblVolume.getNodeIp())
                .ne(NodeImage::getPhaseStatus, REMOVED)
                .eq(NodeImage::getStoragePoolIdFromAgent,poolId);

        if (nodeImageService.count(queryWrapper) > 0)
        {
            return nodeImageService.getOne(queryWrapper).getNodeImageId();
        }
        NodeImage tblNodeImage = new NodeImage();
        tblNodeImage.setNodeImageId(Utils.assignUUId());
        tblNodeImage.setImageId(imageId);
        tblNodeImage.setStoragePoolIdFromAgent(poolId);
        tblNodeImage.setNodeIp(tblVolume.getNodeIp());
        tblNodeImage.setUserId(tblVolume.getUserId());
        tblNodeImage.setPhaseStatus(PhaseStatus.ADDING);
        tblNodeImage.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblNodeImage.setUpdateTime(tblNodeImage.getCreateTime());
        boolean ok = nodeImageService.save(tblNodeImage);
        if (!ok)
        {
            log.info("insert database error,{}", tblNodeImage);
            return null;
        }
        return  tblNodeImage.getNodeImageId();
    }

    public String getStoragePoolIdFromAgent( String nodeIp)
    {
        LambdaQueryWrapper<NodeStoragePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NodeStoragePool::getNodeIp, nodeIp)
                .ne(NodeStoragePool::getPhaseStatus, REMOVED);
        NodeStoragePool nodeStoragePool = nodeStoragePoolService.getOne(queryWrapper);
        if (null == nodeStoragePool || REMOVED == nodeStoragePool.getPhaseStatus() )
        {
            return null;
        }

        return nodeStoragePool.getStoragePoolIdFromAgent();
    }
}
