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
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.config.RepoAgentConfig;
import com.lnjoying.justice.repo.domain.backend.response.BaseRsp;
import com.lnjoying.justice.repo.domain.backend.response.VolumeSnapRspFromAgent;
import com.lnjoying.justice.repo.entity.Volume;
import com.lnjoying.justice.repo.entity.VolumeSnap;
import com.lnjoying.justice.repo.service.VolumeSnapService;
import com.lnjoying.justice.repo.service.biz.LogRpcService;
import com.lnjoying.justice.repo.service.biz.VolumeServiceBiz;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class VolumeSnapTimerProcessor extends AbstractRunnableProcessor
{


    @Autowired
    private VolumeSnapService volumeSnapService;

    @Autowired
    private RepoAgentConfig repoAgentConfig;

    @Autowired
    private VolumeServiceBiz volumeServiceBiz;

    @Autowired
    private LogRpcService logRpcService;

    public VolumeSnapTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("volumeSnapTimer timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("volumeSnapTimer timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processVolumeSnaps(getMiddleStatusVolumeSnaps());
        }
        catch (Exception e)
        {
            log.error("volumeSnap timer processor exception: {}", e.getMessage());
        }
    }

    private List<VolumeSnap> getMiddleStatusVolumeSnaps()
    {
        LambdaQueryWrapper<VolumeSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VolumeSnap::getPhaseStatus, PhaseStatus.ADDED )
                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.SNAP_SWITCH_FAILED)
                .ne(VolumeSnap::getPhaseStatus,REMOVED);
        return volumeSnapService.list(queryWrapper);
    }

    private void processVolumeSnaps(List<VolumeSnap> tblVolumeSnaps)
    {
        try
        {
            log.debug("get tblStoragePools :{}", tblVolumeSnaps);
            for ( VolumeSnap tblVolumeSnap: tblVolumeSnaps )
            {
                processVolumeSnap(tblVolumeSnap);
            }
        }
        catch (Exception e)
        {
            log.error("volumeSnap timer processor error:  {}", e.getMessage());
        }
    }

    private void processVolumeSnap(VolumeSnap tblVolumeSnap)
    {
        int phaseStatus = tblVolumeSnap.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateVolumeSnap(tblVolumeSnap);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveVolumeSnap(tblVolumeSnap);
                    break;
                case PhaseStatus.SNAP_SWITCHING:
                    processSwitchVolumeSnap(tblVolumeSnap);
                    break;
                case PhaseStatus.AGENT_SWITCHING:
                    processGetSwitchVolumeSnap(tblVolumeSnap);
                    break;
                default:
                    defaultProcessVolumeSnap(tblVolumeSnap);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("volumeSnap timer processor error: volumeSnapId {}, phase status {} , exception {}", tblVolumeSnap.getVolumeSnapId(), phaseStatus, e.getMessage());
        }
    }

    private void processCreateVolumeSnap(VolumeSnap tblVolumeSnap)
    {
        try
        {
            String volumeSnapIdFromAgent = createVolumeSnapFromAgent(tblVolumeSnap);
            if (null == volumeSnapIdFromAgent)
            {
                return;
            }

            tblVolumeSnap.setVolumeSnapIdFromAgent(volumeSnapIdFromAgent);
            tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblVolumeSnap.setPhaseStatus(PhaseStatus.AGENT_ADDING);
        }
        catch (WebSystemException e)
        {
            tblVolumeSnap.setPhaseStatus(PhaseStatus.ADD_FAILED);
            log.error("create volumeSnap error: volumeSnapId {}, {}", tblVolumeSnap.getVolumeSnapId(), e.getMessage());
        }
        boolean ok = volumeSnapService.updateById(tblVolumeSnap);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        log.info("created volumeSnap:{} volumeSnapIdFromAgent: {}", tblVolumeSnap.getVolumeSnapId(), tblVolumeSnap.getVolumeSnapIdFromAgent());
        if (tblVolumeSnap.getPhaseStatus() == PhaseStatus.AGENT_ADDING)
        {
            logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(),"Agent 正在创建云盘快照", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "创建中");
        }
        else
        {
            logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(), "创建云盘快照失败", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "创建失败");
        }
    }


    private void processRemoveVolumeSnap(VolumeSnap tblVolumeSnap)
    {
        if (StrUtil.isBlank(tblVolumeSnap.getVolumeSnapIdFromAgent()))
        {
            tblVolumeSnap.setPhaseStatus(PhaseStatus.AGENT_DELETING);
            tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            volumeSnapService.updateById(tblVolumeSnap);
            return;
        }
        String volumeSnapIdFromAgent = removeVolumeSnapFromAgent(tblVolumeSnap);
        if (null == volumeSnapIdFromAgent)
        {
            log.info("removeVolumeSnapFromAgent error, volumeSnapId:{}", tblVolumeSnap.getVolumeSnapId());
            throw new WebSystemException(ErrorCode.SystemError,ErrorLevel.INFO);
        }
        tblVolumeSnap.setPhaseStatus(PhaseStatus.AGENT_DELETING);
        tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = volumeSnapService.updateById(tblVolumeSnap);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        log.info("removed volumeSnap:{} volumeSnapIdFromAgent: {}", tblVolumeSnap.getVolumeSnapId(), tblVolumeSnap.getVolumeSnapIdFromAgent());
        logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(),"Agent 正在删除云盘快照", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "删除中");

    }

    @Transactional(rollbackFor = Exception.class)
    void processGetSwitchVolumeSnap(VolumeSnap tblVolumeSnap)
    {
        try
        {
            VolumeSnapRspFromAgent volumeSnapRspFromAgent = getVolumeSnapStatusFromAgent(tblVolumeSnap);
            assert volumeSnapRspFromAgent != null;
            String logMessage;
            if (!Objects.equals(volumeSnapRspFromAgent.getPhaseType(), AgentConstant.SWITCH))
            {
                return;
            }
            switch (volumeSnapRspFromAgent.getPhase())
            {
                case AgentConstant.SUCCESS:
                    tblVolumeSnap.setPhaseStatus(PhaseStatus.ADDED);
//                    if (volumeSnapRspFromAgent.getCurrent())
//                    {
//                        tblVolumeSnap.setIsCurrent(true);
//                        updateIsCurrentToFalse(tblVolumeSnap);
//                    }
                    VolumeSnap currentVolumeSnap = getCurrentSnap(tblVolumeSnap.getVolumeId());
                    if (null != currentVolumeSnap)
                    {
                        currentVolumeSnap.setIsCurrent(true);
                        currentVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                        volumeSnapService.updateById(currentVolumeSnap);
                        updateIsCurrentToFalse(currentVolumeSnap);
                        if (currentVolumeSnap.getVolumeSnapId().equals(tblVolumeSnap.getVolumeSnapId())) tblVolumeSnap.setIsCurrent(true);
                        log.info("set current volumeSnap:{} volumeSnapIdFromAgent: {}", currentVolumeSnap.getVolumeSnapId(), currentVolumeSnap.getVolumeSnapIdFromAgent());
                    }
                    logMessage = AgentConstant.SUCCESS;
                    setParentSnapId(tblVolumeSnap.getVolumeId());

                    break;
                case AgentConstant.FAIL:
                    logMessage = AgentConstant.FAIL;
                    tblVolumeSnap.setPhaseStatus(PhaseStatus.SNAP_SWITCH_FAILED);
                    break;
                default: return;
            }
            tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = volumeSnapService.updateById(tblVolumeSnap);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            log.info("switched volumeSnap:{} volumeSnapIdFromAgent: {}", tblVolumeSnap.getVolumeSnapId(), tblVolumeSnap.getVolumeSnapIdFromAgent());
            if (logMessage.equals(AgentConstant.SUCCESS))
            {
                logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(),"Agent 正在切换云盘快照", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "切换成功");
            }
            else
            {
                logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(),"Agent 正在切换云盘快照", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "切换失败");
            }
        }
        catch (Exception e)
        {
            log.error("getVolumeSnapStatus error:{}, volumeSnapId:{}, ", e.getMessage(), tblVolumeSnap.getVolumeSnapId());

        }

    }

    void processSwitchVolumeSnap(VolumeSnap tblVolumeSnap)
    {
        try
        {
            String ok = switchVolumeSnapFromAgent(tblVolumeSnap);
            assert ok != null;
            if (ok.equals(AgentConstant.FAILED))
            {
                tblVolumeSnap.setPhaseStatus(PhaseStatus.SNAP_SWITCH_FAILED);
            }
            else if (ok.equals(AgentConstant.OK))
            {
                tblVolumeSnap.setPhaseStatus(PhaseStatus.AGENT_SWITCHING);
            }
            else
            {
                return;
            }
            tblVolumeSnap.setUpdateTime(tblVolumeSnap.getUpdateTime());
            boolean dbOk = volumeSnapService.updateById(tblVolumeSnap);
            if (dbOk)
            {
                log.info("switch volumeSnap:{} volumeSnapIdFromAgent: {}", tblVolumeSnap.getVolumeSnapId(), tblVolumeSnap.getVolumeSnapIdFromAgent());
                if (PhaseStatus.SNAP_SWITCH_FAILED == tblVolumeSnap.getPhaseStatus())
                {
                    logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(),"Agent 正在切换云盘快照", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "切换失败");
                }
                else
                {
                    logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(), "Agent 正在切换云盘快照", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), "切换中");
                }
            }

        }
        catch (Exception e)
        {
            log.error("switch volumeSnap error:{}, volumeSnapId:{}, ", e.getMessage(), tblVolumeSnap.getVolumeSnapId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    void defaultProcessVolumeSnap(VolumeSnap tblVolumeSnap)
    {
        try
        {
            VolumeSnapRspFromAgent volumeSnapRspFromAgent = getVolumeSnapStatusFromAgent(tblVolumeSnap);
            String result = null;
            switch (Objects.requireNonNull(Objects.requireNonNull(volumeSnapRspFromAgent).getPhase()))
            {
                case AgentConstant.SUCCESS:
                    if (volumeSnapRspFromAgent.getPhaseType().equals(AgentConstant.ADD))
                    {
                        tblVolumeSnap.setPhaseStatus(PhaseStatus.ADDED);
                        tblVolumeSnap.setVolumeSnapIdFromAgent(volumeSnapRspFromAgent.getVolumeSnapIdFromAgent());
                        if (volumeSnapRspFromAgent.getCurrent())
                        {
                            updateIsCurrentToFalse(tblVolumeSnap);
                            tblVolumeSnap.setIsCurrent(true);
                        }
                        tblVolumeSnap.setParentId("");
                        if (StrUtil.isNotBlank(volumeSnapRspFromAgent.getParentId()))
                        {
                            LambdaQueryWrapper<VolumeSnap> snapIdQueryWrapper = new LambdaQueryWrapper<>();
                            snapIdQueryWrapper.eq(VolumeSnap::getVolumeSnapIdFromAgent, volumeSnapRspFromAgent.getParentId())
                                    .eq(VolumeSnap::getVolumeId, tblVolumeSnap.getVolumeId())
                                    .ne(VolumeSnap::getPhaseStatus, REMOVED);
                            VolumeSnap parentSnap = volumeSnapService.getOne(snapIdQueryWrapper);
                            if (null != parentSnap)
                            {
                                tblVolumeSnap.setParentId(parentSnap.getVolumeSnapId());
                            }
                        }
                        result = "创建成功";
                    }
                    break;
                case AgentConstant.SNAP_NOT_EXIST:
                    tblVolumeSnap.setPhaseStatus(REMOVED);
                    VolumeSnap currentVolumeSnap = getCurrentSnap(tblVolumeSnap.getVolumeId());
                    if (null != currentVolumeSnap)
                    {
                        currentVolumeSnap.setIsCurrent(true);
                        currentVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                        volumeSnapService.updateById(currentVolumeSnap);
                    }
                    setParentSnapId(tblVolumeSnap.getVolumeId());
                    result = "删除成功";
                    break;
                case AgentConstant.FAIL:
                    if (volumeSnapRspFromAgent.getPhaseType().equals(AgentConstant.ADD))
                    {
                        tblVolumeSnap.setPhaseStatus(PhaseStatus.ADD_FAILED);
                        result = "创建失败";
                    }
                    else if (volumeSnapRspFromAgent.getPhaseType().equals(AgentConstant.DEL))
                    {
                        tblVolumeSnap.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                        result = "删除失败";
                    }
                    break;
                default:
                    return;
            }
            tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = volumeSnapService.updateById(tblVolumeSnap);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            logRpcService.getLogService().addEvent(tblVolumeSnap.getUserId(),"Agent 正在获取云盘快照状态", String.format("请求参数: volumeSnapId:%s", tblVolumeSnap.getVolumeSnapId()), result);
        }
        catch (Exception e)
        {
            log.error("getVolumeSnapStatus error:{}, volumeSnapId:{}, ", e.getMessage(), tblVolumeSnap.getVolumeSnapId());
        }
    }

    private String createVolumeSnapFromAgent(VolumeSnap tblVolumeSnap)
    {
        Volume tblVolume = volumeServiceBiz.getVolumeById(tblVolumeSnap.getVolumeId());
        String nodeIp = tblVolume.getNodeIp();
        if (StrUtil.isBlank(nodeIp))
        {
            return null;
        }
        String url = String.format("http://%s:%s/%s/%s/snaps", nodeIp,repoAgentConfig.getVmAgentPort() , repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        BaseRsp result = HttpActionUtil.post(url, null, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating volumeSnap  error,  volumeSnap:{}", tblVolumeSnap.getVolumeSnapId());
            return null;
        }

        if (!AgentConstant.PENDING.equals(result.getStatus()))
        {
            log.info("create volumeSnap error, status:{}, result:{}",result.getStatus(), result);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return result.getUuid();
    }

    private String removeVolumeSnapFromAgent(VolumeSnap tblVolumeSnap)
    {
        Volume tblVolume = volumeServiceBiz.getVolumeById(tblVolumeSnap.getVolumeId());
        String nodeIp = tblVolume.getNodeIp();
        String url = String.format("http://%s:%s%s/%s", nodeIp,repoAgentConfig.getVmAgentPort(),
                repoAgentConfig.getVolumeSnapUrl(), tblVolumeSnap.getVolumeSnapIdFromAgent());

        BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of removing volumeSnap error,  volumeSnapId:{}", tblVolumeSnap.getVolumeSnapId());
            return null;
        }

        if (AgentConstant.PENDING.equals(result.getStatus()) ||
                (AgentConstant.FAILED.equals(result.getStatus()) && result.getReason().contains(AgentConstant.NOT_EXIST)))
        {
            return tblVolumeSnap.getVolumeSnapIdFromAgent();
        }

        return null;
    }

    private String switchVolumeSnapFromAgent(VolumeSnap tblVolumeSnap)
    {
        Volume tblVolume = volumeServiceBiz.getVolumeById(tblVolumeSnap.getVolumeId());
        String nodeIp = tblVolume.getNodeIp();
        String url = String.format("http://%s:%s/%s/%s/switch", nodeIp,repoAgentConfig.getVmAgentPort(),
                repoAgentConfig.getVolumeSnapUrl(), tblVolumeSnap.getVolumeSnapIdFromAgent());
        BaseRsp result = HttpActionUtil.put(url, null, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of switch volumeSnap error,  volumeSnapId:{}", tblVolumeSnap.getVolumeSnapId());
            return null;
        }

        if (AgentConstant.PENDING.equals(result.getStatus()))
        {
            return AgentConstant.OK;
        }
        else if (AgentConstant.FAILED.equals(result.getStatus()))
        {
            return AgentConstant.FAILED;
        }

        return null;
    }

    private VolumeSnapRspFromAgent getVolumeSnapStatusFromAgent(VolumeSnap tblVolumeSnap)
    {
        try
        {
            Volume tblVolume = volumeServiceBiz.getVolumeById(tblVolumeSnap.getVolumeId());
            String nodeIp = tblVolume.getNodeIp();
            String url = String.format("http://%s:%s/%s/%s", nodeIp,repoAgentConfig.getVmAgentPort(),
                    repoAgentConfig.getVolumeSnapUrl(), tblVolumeSnap.getVolumeSnapIdFromAgent());
            VolumeSnapRspFromAgent volumeSnapRspFromAgent = HttpActionUtil.getObject(url, VolumeSnapRspFromAgent.class);
            log.info("url:{}, get result:{}", url, volumeSnapRspFromAgent);
            if (AgentConstant.SNAP_NOT_EXIST.equals(volumeSnapRspFromAgent.getReason()))
            {
                volumeSnapRspFromAgent.setPhase(AgentConstant.SNAP_NOT_EXIST);
            }
            return volumeSnapRspFromAgent;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void setParentSnapId(String volumeId)
    {
        LambdaQueryWrapper<VolumeSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VolumeSnap::getVolumeId,volumeId)
                .ne(VolumeSnap::getPhaseStatus,REMOVED);
        List<VolumeSnap> volumeSnaps = volumeSnapService.list(queryWrapper);
        for (VolumeSnap tblVolumeSnap : volumeSnaps)
        {
            VolumeSnapRspFromAgent rsp = getVolumeSnapStatusFromAgent(tblVolumeSnap);
            if (null == rsp) continue;
            if (null == rsp.getParentId())
            {
                tblVolumeSnap.setParentId("");
                volumeSnapService.updateById(tblVolumeSnap);
            }
            else
            {
                LambdaQueryWrapper<VolumeSnap> snapIdQueryWrapper = new LambdaQueryWrapper<>();
                snapIdQueryWrapper.eq(VolumeSnap::getVolumeSnapIdFromAgent,rsp.getParentId())
                        .eq(VolumeSnap::getVolumeId,volumeId)
                        .ne(VolumeSnap::getPhaseStatus,REMOVED);
                if (volumeSnapService.count(snapIdQueryWrapper) > 0)
                {
                    String parentId = volumeSnapService.getOne(snapIdQueryWrapper).getVolumeSnapId();
                    tblVolumeSnap.setParentId(parentId);
                    volumeSnapService.updateById(tblVolumeSnap);
                }
            }
        }
    }

    private VolumeSnap getCurrentSnap(String volumeId)
    {
        LambdaQueryWrapper<VolumeSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VolumeSnap::getPhaseStatus, REMOVED)
                .eq(VolumeSnap::getVolumeId, volumeId)
//                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.DELETING)
//                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.ADDING)
                .ne(VolumeSnap::getPhaseStatus,PhaseStatus.ADD_FAILED);
        if (volumeSnapService.count(queryWrapper) > 0)
        {
            List<VolumeSnap> tblVolumeSnaps = volumeSnapService.list(queryWrapper);
            for (VolumeSnap tblVolumeSnap : tblVolumeSnaps)
            {
                VolumeSnapRspFromAgent rsp = getVolumeSnapStatusFromAgent(tblVolumeSnap);
                if (null == rsp) continue;
                if (!StrUtil.isBlank(rsp.getReason()))
                {
                    continue;
                }
                if (rsp.getCurrent())
                {
                    return tblVolumeSnap;
                }
            }
        }
        return null;
//                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.DELETE_FAILED)
//                .ne(VolumeSnap::getPhaseStatus,PhaseStatus.ADDING)
//                .ne(VolumeSnap::getPhaseStatus, PhaseStatus.DELETING);

    }

    private void updateIsCurrentToFalse(VolumeSnap tblVolumeSnap)
    {
        LambdaQueryWrapper<VolumeSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VolumeSnap::getVolumeId, tblVolumeSnap.getVolumeId())
                .ne(VolumeSnap::getPhaseStatus, REMOVED)
                .ne(VolumeSnap::getVolumeSnapId, tblVolumeSnap.getVolumeSnapId())
                .eq(VolumeSnap::getIsCurrent, true);
        VolumeSnap needUpdateTblVolumeSnap = new VolumeSnap();
        needUpdateTblVolumeSnap.setIsCurrent(false);
        needUpdateTblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        volumeSnapService.update(needUpdateTblVolumeSnap, queryWrapper);
    }
}
