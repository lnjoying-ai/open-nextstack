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

package com.lnjoying.vm.processor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.response.BaseRsp;
import com.lnjoying.vm.domain.backend.response.VmSnapRspFromAgent;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.entity.VmSnap;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.VmInstanceService;
import com.lnjoying.vm.service.VmSnapService;
import com.lnjoying.vm.service.biz.CombRpcSerice;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class VmSnapTimerProcessor extends AbstractRunnableProcessor
{

    @Autowired
    ComputeConfig computeConfig;

    @Autowired
    private VmSnapService vmSnapService;

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    HypervisorNodeService hypervisorNodeService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    LogRpcService logRpcService;

    @Autowired
    VmInstanceTimerProcessor vmInstanceTimerProcessor;


    public VmSnapTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("vm snap timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("vm snap timer processor stop");
    }

    @Override
    public void run()
    {

        try
        {
            //get middle status instances
            List<VmSnap> tblVmSnapList = getMiddleStatusInstances();

            //log.info(" vm instance timer processor run, instances size: {}", tblVmInstanceList.size());

            //check each instance and process
            for (VmSnap tblVmSnap : tblVmSnapList)
            {
                processVmSnap(tblVmSnap);
            }
        }
        catch (Exception e)
        {
            log.error("vm snap timer processor exception: {}", e.getMessage());
        }
    }

    //get middle status vm snap (not final status)
    private List<VmSnap> getMiddleStatusInstances()
    {
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_CREATE_FAILED)
                .ne(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_REMOVE_FAILED)
                .ne(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_CREATED)
                .ne(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_SWITCHED)
                .ne(VmSnap::getPhaseStatus, REMOVED)
                .ne(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_SWITCH_FAILED);


        return vmSnapService.list(queryWrapper);
    }

    private void processVmSnap(VmSnap tblVmSnap)
    {
        int phaseStatus = tblVmSnap.getPhaseStatus();

        try
        {
            switch (phaseStatus)
            {
                case VmInstanceStatus.SNAP_INIT:
                    createSnapFromAgent(tblVmSnap);
                    break;
                case VmInstanceStatus.SNAP_CREATING:
                    checkSnapCreateResult(tblVmSnap);
                    break;
                case VmInstanceStatus.SNAP_REMOVING:
                    removeSnap(tblVmSnap);
                    break;
                case VmInstanceStatus.GET_SNAP_REMOVED_STATUS:
                    checkSnapRemoveResult(tblVmSnap);
                    break;
                case VmInstanceStatus.SNAP_SWITCHING:
                    switchSnapFromAgent(tblVmSnap);
                    break;
                case VmInstanceStatus.GET_SNAP_SWITCHED_STATUS:
                    checkSnapSwitchResult(tblVmSnap);
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("vm snap timer processor error: snap id {}, instance id {}, phase status {} , exception {}", tblVmSnap.getSnapId(), tblVmSnap.getVmInstanceId(), phaseStatus, e.getMessage());
        }
    }

    private void createSnapFromAgent(VmSnap tblVmSnap)
    {
        try
        {
            VmInstance tblVmInstance = vmInstanceService.getById(tblVmSnap.getVmInstanceId());
            String instanceId = tblVmInstance.getInstanceIdFromAgent();
            String nodeIp = getManageIp(tblVmSnap);
            String url = nodeIp + "/" + ComputeUrl.V1_VM_URL + "/" + instanceId + "/cps";
            BaseRsp result = HttpActionUtil.post(url, "", BaseRsp.class);
            if (null == result)
            {
                log.error("get response of creating vm snap :null,  vm instance id {}", tblVmSnap.getSnapId());
                return;
            }

            String status = result.getStatus();

            //not pending status -> error
            if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
            {
                log.error("create vm snap error:vm snap id {}, vm instance id {}, result:{}", tblVmSnap.getSnapId(), tblVmInstance.getVmInstanceId(), result);
            }
            else
            {
                //update phase status, instance id from agent
                String uuid = result.getUuid();
                tblVmSnap.setSnapIdFromAgent(uuid);
                boolean ok = updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.SNAP_CREATING);
                if (!ok)
                {
                    log.info("update database tbl_vm_snap error, vmSnapId: {}", tblVmSnap.getSnapId());
                    return;
                }
                log.info("create vm snap, creating status: vm snap id {}, vm snap id from agent {}, vm instance id {}",
                        tblVmSnap.getSnapId(), uuid, tblVmInstance.getVmInstanceId());
                logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "Agent 正在创建虚机快照",
                        String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "创建中");
            }
        }
        catch (Exception e)
        {
            log.error("create snap from agent ,id {}  ,error: {}", tblVmSnap.getSnapId(), e.getMessage());
        }
    }

    private void switchSnapFromAgent(VmSnap tblVmSnap)
    {
        try
        {
            String nodeIp = getManageIp(tblVmSnap);
            String url = nodeIp + ComputeUrl.V1_SNAP_URL + "/" + tblVmSnap.getSnapIdFromAgent() + "/switch";
            BaseRsp result = HttpActionUtil.put(url, null, BaseRsp.class);
            if (null == result)
            {
                log.error("get response of switching vm snap :null,  vm snap id {}", tblVmSnap.getSnapId());
                return;
            }
            boolean ok = true;
            String status = result.getStatus();
            //not pending status -> error
            if (status.equalsIgnoreCase(AgentConstant.FAILED))
            {
                log.error("switch vm snap error:vm snap id {} vm instance id {}", tblVmSnap.getSnapId(), tblVmSnap.getVmInstanceId());
                ok = updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.SNAP_SWITCH_FAILED);
            }
            else if (status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
            {
                log.info("switch vm snap, switching status: vm snap id {}, vm snap id from agent {}",
                        tblVmSnap.getSnapId(), tblVmSnap.getSnapIdFromAgent());
                //update phase status, instance id from agent
                ok = updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.GET_SNAP_SWITCHED_STATUS);

            }
            if (!ok)
            {
                log.info("update database tbl_vm_snap error, vmSnapId: {}", tblVmSnap.getSnapId());
                return;
            }
            if (VmInstanceStatus.GET_SNAP_SWITCHED_STATUS == tblVmSnap.getPhaseStatus())
            {
                logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "Agent 正在切换虚机快照",
                        String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "切换中");
            }
            else if (VmInstanceStatus.SNAP_SWITCH_FAILED == tblVmSnap.getPhaseStatus())
            {
                logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "Agent 切换虚机快照失败",
                        String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "切换失败");
            }

        }
        catch (Exception e)
        {
            log.error("switch snap from agent ,id {}  ,error: {}", tblVmSnap.getSnapId(), e.getMessage());
        }
    }

    private void removeSnap(VmSnap tblVmSnap)
    {
        try
        {
            String nodeIp = getManageIp(tblVmSnap);
            String url = nodeIp + "/" + ComputeUrl.V1_SNAP_URL + "/" + tblVmSnap.getSnapIdFromAgent();
            BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);
            if (null == result)
            {
                log.error("get response of removing vm snap :null,vm snap id {}  vm instance id {}", tblVmSnap.getSnapId(), tblVmSnap.getVmInstanceId());
                return;
            }

            String status = result.getStatus();
            String reason = result.getReason();
            //not pending status -> error
            boolean ok = false;
            if (!StrUtil.isBlank(reason) && reason.contains(AgentConstant.NOT_EXIST))
            {
                ok = updateVmSnapPhaseStatus(tblVmSnap, REMOVED);
                if (ok)
                {
                    List<VmSnap> vmSnaps = getVmSnapByVmInstanceId(tblVmSnap.getVmInstanceId());
                    VmSnap currentSnap = getCurrentSnap(vmSnaps, tblVmSnap.getVmInstanceId());
                    if (null != currentSnap)
                    {
                        currentSnap.setIsCurrent(true);
                        currentSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                        ok = vmSnapService.updateById(currentSnap);
                    }
                    setParentSnapId(vmSnaps, tblVmSnap.getVmInstanceId());
                }
            }
            else if (!status.equalsIgnoreCase(AgentConstant.PENDING_STATUS))
            {
                log.error("remove vm error: vm snap id {}", tblVmSnap.getSnapId());
            }
            else
            {
                ok = updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.GET_SNAP_REMOVED_STATUS);
            }
            if (!ok)
            {
                log.info("update database tbl_vm_snap error, vmInstanceId: {}", tblVmSnap.getSnapId());
            }
        }
        catch (Exception e)
        {
            log.error("remove snap from agent, id {}  ,error: {}", tblVmSnap.getSnapId(), e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkSnapCreateResult(VmSnap tblVmSnap)
    {
        try
        {
            boolean ok = false;
            VmSnapRspFromAgent rsp = getVmSnapResultFromAgent(tblVmSnap);
            ;
//            String phaseStatus = getVmSnapStatusFromAgent(tblVmSnap,false);
            if (rsp == null) return;
            if (!rsp.getPhaseType().equals(AgentConstant.ADD)) return;
            if (rsp.getPhase().equals(AgentConstant.SUCCESS))
            {
                tblVmSnap.setPhaseStatus(VmInstanceStatus.SNAP_CREATED);
                if (rsp.getCurrent()) tblVmSnap.setIsCurrent(true);
                LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(VmSnap::getSnapIdFromAgent, rsp.getParentId())
                        .ne(VmSnap::getPhaseStatus, REMOVED);
                if (vmSnapService.count(queryWrapper) > 0)
                {
                    String parentId = vmSnapService.getOne(queryWrapper).getSnapId();
                    tblVmSnap.setParentId(parentId);
                }
                if (StrUtil.isBlank(rsp.getParentId())) tblVmSnap.setParentId("");
                tblVmSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                ok = vmSnapService.updateById(tblVmSnap);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "创建快照成功",
                            String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "创建成功");
                }
            }
            else if (rsp.getPhase().equals(AgentConstant.FAIL))
            {
                ok = updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.SNAP_CREATE_FAILED);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "创建快照失败",
                            String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "创建失败");
                }
            }
            if (ok && rsp.getCurrent())
            {
                updateVmSnapCurrentArg(tblVmSnap);
            }
        }
        catch (Exception e)
        {
            log.error("get the result of creating snap error, snapId: {}", tblVmSnap.getSnapId());
        }
    }

    private void updateVmSnapCurrentArg(VmSnap tblVmSnap)
    {
        LambdaUpdateWrapper<VmSnap> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VmSnap::getIsCurrent, true)
                .eq(VmSnap::getVmInstanceId, tblVmSnap.getVmInstanceId())
                .ne(VmSnap::getPhaseStatus, REMOVED)
                .ne(VmSnap::getSnapId, tblVmSnap.getSnapId());

        VmSnap updateSnap = new VmSnap();
        updateSnap.setIsCurrent(false);
        updateSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        vmSnapService.update(updateSnap, updateWrapper);
    }


    public void checkSnapRemoveResult(VmSnap tblVmSnap)
    {
        TransactionStatus transactionStatus = null;
        try
        {
            String getStatus = getVmSnapStatusFromAgent(tblVmSnap);
            log.info("get snap remove status from agent, vmSnapId: {}, status: {}", tblVmSnap.getSnapId(), getStatus);
            if (!AgentConstant.GET_STATUS_FAILED.equals(getStatus) && !AgentConstant.NOT_EXIST.equals(getStatus))
            {
                log.info("get snap remove status from agent failed, vmSnapId: {}", tblVmSnap.getSnapId());
                return;
            }

            boolean isCurrent = tblVmSnap.getIsCurrent();
            boolean ok = false;
            transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            if (AgentConstant.NOT_EXIST.equalsIgnoreCase(getStatus))
            {
                ok = updateVmSnapPhaseStatus(tblVmSnap, REMOVED);
            }
            else
            {
                ok = updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.SNAP_REMOVE_FAILED);
            }
            if (!ok)
            {
                dataSourceTransactionManager.commit(transactionStatus);
                log.info("update database tbl_vm_snap error, vmSnapId: {}", tblVmSnap.getSnapId());
                return;
            }
            if (REMOVED == tblVmSnap.getPhaseStatus())
            {
                log.info("update current snap, vmInstanceId: {}", tblVmSnap.getVmInstanceId());
                List<VmSnap> tblVmSnaps = getVmSnapByVmInstanceId(tblVmSnap.getVmInstanceId());
                if (null != tblVmSnaps && !tblVmSnaps.isEmpty())
                {
                    VmSnap currentSnap = getCurrentSnap(tblVmSnaps, tblVmSnap.getVmInstanceId());
                    if (null != currentSnap)
                    {
                        currentSnap.setIsCurrent(true);
                        currentSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                        vmSnapService.updateById(currentSnap);
                    }
                }
                setParentSnapId(tblVmSnaps, tblVmSnap.getVmInstanceId());
            }
            dataSourceTransactionManager.commit(transactionStatus);
            if (REMOVED == tblVmSnap.getPhaseStatus())
            {
                logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "删除快照成功",
                        String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "删除成功");
            }
            else
            {
                logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "删除快照失败",
                        String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "删除失败");
            }
        }
        catch (Exception e)
        {
            log.error("get the result of creating snap error, snapId: {}", tblVmSnap.getSnapId());
            if (null != transactionStatus)
            {
                dataSourceTransactionManager.rollback(transactionStatus);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkSnapSwitchResult(VmSnap tblVmSnap)
    {
        try
        {
            VmSnapRspFromAgent getSnapInfoFromAgentRsp = getVmSnapResultFromAgent(tblVmSnap);
            if (null == getSnapInfoFromAgentRsp) return;
            if (!getSnapInfoFromAgentRsp.getPhaseType().equals("switch")) return;
            if (AgentConstant.SUCCESS.equals(getSnapInfoFromAgentRsp.getPhase()))
            {
                VmInstance tblVmInstance = vmInstanceService.getById(tblVmSnap.getVmInstanceId());
                Boolean isShut = isVmShutDown(tblVmInstance);
                if (null != isShut && isShut)
                {
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWEROFF);
                }
                else if (null != isShut)
                {
                    tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RUNNING);
                }
                if (null != isShut)
                {
                    tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    vmInstanceService.updateById(tblVmInstance);
                }
                tblVmSnap.setPhaseStatus(VmInstanceStatus.SNAP_SWITCHED);
                tblVmSnap.setIsCurrent(getSnapInfoFromAgentRsp.getCurrent());
                boolean ok = vmSnapService.updateById(tblVmSnap);
                if (ok && tblVmSnap.getIsCurrent())
                {
                    updateVmSnapCurrentArg(tblVmSnap);
                }
                List<VmSnap> tblVmSnaps = getVmSnapByVmInstanceId(tblVmSnap.getVmInstanceId());
                setParentSnapId(tblVmSnaps, tblVmSnap.getVmInstanceId());
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "切换快照成功",
                            String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "切换成功");
                }
            }
            else if (AgentConstant.FAIL.equals(getSnapInfoFromAgentRsp.getPhase()))
            {
                updateVmSnapPhaseStatus(tblVmSnap, VmInstanceStatus.SNAP_SWITCH_FAILED);
                logRpcService.getLogService().addEvent(tblVmSnap.getUserId(), "切换快照失败",
                        String.format("请求参数: vmSnapId:%s", tblVmSnap.getSnapId()), "切换失败");
            }

        }
        catch (Exception e)
        {
            log.error("get the result of switching snap error, snapId: {}", tblVmSnap.getSnapId());
        }
    }

    private void setParentSnapId(List<VmSnap> vmSnaps, String vmInstanceId)
    {
        if (null == vmSnaps || vmSnaps.isEmpty()) return;
        for (VmSnap tblVmSnap : vmSnaps)
        {
            VmSnapRspFromAgent rsp = getVmSnapResultFromAgent(tblVmSnap);
            if (null == rsp) continue;
            if (null == rsp.getParentId())
            {
                tblVmSnap.setParentId("");
                vmSnapService.updateById(tblVmSnap);
            }
            else
            {
                LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(VmSnap::getSnapIdFromAgent, rsp.getParentId())
                        .eq(VmSnap::getVmInstanceId, vmInstanceId)
                        .ne(VmSnap::getPhaseStatus, REMOVED);
                if (vmSnapService.count(queryWrapper) > 0)
                {
                    String parentId = vmSnapService.getOne(queryWrapper).getSnapId();
                    tblVmSnap.setParentId(parentId);
                    vmSnapService.updateById(tblVmSnap);
                }
            }
        }
    }

    private List<VmSnap> getVmSnapByVmInstanceId(String vmInstanceId)
    {
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmSnap::getPhaseStatus, REMOVED)
                .eq(VmSnap::getVmInstanceId, vmInstanceId)
//                    .ne(VmSnap::getPhaseStatus,VmInstanceStatus.SNAP_REMOVE_FAILED)
//                    .ne(VmSnap::getPhaseStatus,VmInstanceStatus.SNAP_REMOVING)
//                    .ne(VmSnap::getPhaseStatus,VmInstanceStatus.SNAP_INIT)
                .ne(VmSnap::getPhaseStatus, VmInstanceStatus.SNAP_CREATE_FAILED);
        if (vmSnapService.count(queryWrapper) > 0)
        {
            List<VmSnap> tblVmSnaps = vmSnapService.list(queryWrapper);
            return tblVmSnaps;
        }
        return null;
    }

    private VmSnap getCurrentSnap(List<VmSnap> tblVmSnaps, String vmInstanceId)
    {
        try
        {
            for (VmSnap tblVmSnap : tblVmSnaps)
            {
                VmSnapRspFromAgent rsp = getVmSnapResultFromAgent(tblVmSnap);
                if (null == rsp) continue;
                if (rsp.getCurrent())
                {
                    return tblVmSnap;
                }
            }

            return null;
        }
        catch (Exception e)
        {
            log.info("get currentSnap error: {}", e.getMessage());
            return null;
        }
    }


    //get vm instance status
    private String getVmSnapStatusFromAgent(VmSnap tblVmSnap) throws WebSystemException
    {
        try
        {
            String snapIdFromAgent = tblVmSnap.getSnapIdFromAgent();
            //todo get hypervisor node ip
            String nodeIp = getManageIp(tblVmSnap);
            String url = nodeIp + "/" + ComputeUrl.V1_SNAP_URL + "/" + snapIdFromAgent;
            VmSnapRspFromAgent result = HttpActionUtil.getObject(url, VmSnapRspFromAgent.class);
            if (null == result)
            {
                log.error("get response of creating vm snap :null,  vm instance id {}", tblVmSnap.getSnapId());
                return null;
            }
            String status = result.getStatus();
            if (AgentConstant.GET_STATUS_FAILED.equalsIgnoreCase(status) && result.getReason().contains(AgentConstant.NOT_EXIST))
            {
                return AgentConstant.NOT_EXIST;
            }
            return status;
        }
        catch (Exception e)
        {
            log.info("get snap id: {} error {}", tblVmSnap.getSnapId(), e.getMessage());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
    }

    private VmSnapRspFromAgent getVmSnapResultFromAgent(VmSnap tblVmSnap) throws WebSystemException
    {
        try
        {
            String snapIdFromAgent = tblVmSnap.getSnapIdFromAgent();
            //todo get hypervisor node ip
            String nodeIp = getManageIp(tblVmSnap);
            String url = nodeIp + "/" + ComputeUrl.V1_SNAP_URL + "/" + snapIdFromAgent;
            VmSnapRspFromAgent getSnapInfoFromAgentRsp = HttpActionUtil.getObject(url, VmSnapRspFromAgent.class);
//            String result = HttpActionUtil.get(url);
//            result = UnderlineAndHump.underlineToHump(result);

            if (null == getSnapInfoFromAgentRsp)
            {
                log.error("get response of creating vm snap :null,  vm instance id {}", tblVmSnap.getSnapId());
                return null;
            }
            return getSnapInfoFromAgentRsp;
        }
        catch (Exception e)
        {
            log.info("get snap id: {} error {}", tblVmSnap.getSnapId(), e.getMessage());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
    }


    private boolean updateVmSnapPhaseStatus(VmSnap tblVmSnap, int phaseStatus)
    {
        tblVmSnap.setPhaseStatus(phaseStatus);
        tblVmSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        log.info("update vm snap phase status to db, snapId: {}, phaseStatus: {}, tblVmSnap:{}", tblVmSnap.getSnapId(), phaseStatus, tblVmSnap);
        //update instance deploying phase status to db
        return vmSnapService.updateById(tblVmSnap);
    }

    String getManageIp(VmSnap tblVmSnap)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(tblVmSnap.getVmInstanceId());
        if (null == tblVmInstance)
        {
            return null;
        }
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == tblHypervisorNode)
        {
            return null;
        }
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        if (null == vmAgentPort || 0 == vmAgentPort)
        {
            vmAgentPort = ComputeUrl.VM_AGENT_PORT;
        }
        return "http://" + tblHypervisorNode.getManageIp() + ":" + vmAgentPort;
    }

//    "children":"0","current":"no","domain":"4d720c8c-58aa-4695-b669-289d28ac0a86","name":"1661502464","parent":"1661501594"

    private Boolean isVmShutDown(VmInstance tblVmInstance)
    {
        //VmInstance tblVmInstance = vmInstanceService.getById(tblVmSnap.getVmInstanceId());
        String power = vmInstanceTimerProcessor.getPowerStatus(tblVmInstance);
        if (AgentConstant.POWER_ON.equals(power))
        {
            return false;
        }
        else if (AgentConstant.POWER_OFF.equals(power))
        {
            return true;
        }

        return null;
    }

}
