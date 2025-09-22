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
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.request.MonitorTagsCreateReq;
import com.lnjoying.vm.domain.backend.response.AgentRsp;
import com.lnjoying.vm.domain.backend.response.BaseRsp;
import com.lnjoying.vm.domain.backend.response.MonitorTagRsp;
import com.lnjoying.vm.domain.backend.response.VmInstancesRspFromAgent;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.VmInstanceService;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class VmInstanceHaTimerProcessor extends AbstractRunnableProcessor
{
//    private static final Logger log = LogManager.getLogger();

    int maxErrorCount = 10;

    @Autowired
    private HypervisorNodeService hypervisorNodeService;

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private ComputeConfig computeConfig;

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private VmInstanceTimerProcessor vmInstanceTimerProcessor;

    @Autowired
    private HypervisorNodeTimerProcessor hypervisorNodeTimerProcessor;

    @Override
    public void start()
    {
        log.info("vm instance ha timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("vm instance ha timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            //get middle status instances
            List<HypervisorNode> tblHypervisorNodes = getMiddleStatusInstances();

            //log.info(" vm instance timer processor run, instances size: {}", tblVmInstanceList.size());

            //check each instance and process
            for (HypervisorNode tblHypervisorNode : tblHypervisorNodes)
            {
                processHypervisorNode(tblHypervisorNode);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("VmInstanceHaTimer timer processor exception: {}", e.getMessage());
        }
    }

    public List<HypervisorNode> getMiddleStatusInstances()
    {

        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(HypervisorNode::getPhaseStatus, REMOVED)
                .isNotNull(HypervisorNode::getBackupNodeId);
        return hypervisorNodeService.list(queryWrapper);
    }

    void processHypervisorNode(HypervisorNode tblHypervisorNode)
    {
        String status = healthCheck(tblHypervisorNode);
        if (!Objects.equals(AgentConstant.OK, status))
        {
            logRpcService.getLogService().addEvent("", "健康状态检测中", String.format("hypervisorNodeId:%s 离线", tblHypervisorNode.getNodeId()), "计算节点异常");
            logRpcService.getAlertService().sendAlarmInfo(tblHypervisorNode.getNodeId(), "firing");
            tblHypervisorNode.setPhaseStatus(VmInstanceStatus.HYPERVISOR_NODE_OFFLINE);
            tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            hypervisorNodeService.updateById(tblHypervisorNode);
            if (Objects.equals(AgentConstant.OK, backupNodeIsHealthy(tblHypervisorNode)))
            {
                log.info("hypervisorNode health check error, but backup node is healthy, nodeId:{}", tblHypervisorNode.getNodeId());
                LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(VmInstance::getNodeId, tblHypervisorNode.getNodeId())
                        .ne(VmInstance::getPhaseStatus, REMOVED);
                if (0 == vmInstanceService.count(queryWrapper)) return;
                log.info("phase migrate_clean ,hypervisor nodeId:{}", tblHypervisorNode.getNodeId());
                updateNewHypervisorNode(tblHypervisorNode);
                createMonitorTagsFromNode(tblHypervisorNode);
            }
        }
        else
        {
            updateVmInstancesPowerStatus(tblHypervisorNode.getNodeId());
        }
    }

    private void updateVmInstancesPowerStatus(String nodeId)
    {
        LambdaUpdateWrapper<VmInstance> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(VmInstance::getNodeId, nodeId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        List<VmInstance> tblVmInstanceList = vmInstanceService.list(queryWrapper);
        for (VmInstance tblVmInstance : tblVmInstanceList)
        {
            VmInstanceTimerProcessor.GetVmInstanceRspFromAgent getVmInstanceStatus = null;
            try
            {
                getVmInstanceStatus = vmInstanceTimerProcessor.getVmInstanceStatusFromAgent(tblVmInstance);
                if (null == getVmInstanceStatus) continue;
                if (null == getVmInstanceStatus.getCloudinitMetas()) continue;
            }
            catch (Exception e)
            {
                log.error("get vm instance status from agent error, instanceId:{}", tblVmInstance.getVmInstanceId());
                continue;
            }
            if (getVmInstanceStatus.getCloudinitMetas() != null && getVmInstanceStatus.getCloudinitMetas().getDone())
            {
                updateVmInstancePowerStatus(tblVmInstance, getVmInstanceStatus.getPower());
            }
        }

    }


    private void updateVmInstancePowerStatus(VmInstance tblVmInstance, String powerStatus)
    {
        if (Objects.equals(powerStatus, AgentConstant.SHUT) && VmInstanceStatus.INSTANCE_POWEROFF != tblVmInstance.getPhaseStatus())
        {
            if (VmInstanceStatus.INSTANCE_RUNNING == tblVmInstance.getPhaseStatus() ||
                    VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE == tblVmInstance.getPhaseStatus() ||
                    VmInstanceStatus.INSTANCE_MIGRATE_CLEAN == tblVmInstance.getPhaseStatus())
            {
                log.info("vm instance power off, instanceId:{}, phaseStatus:{}", tblVmInstance.getVmInstanceId(), tblVmInstance.getPhaseStatus());
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_POWEROFF);
                tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                vmInstanceService.updateById(tblVmInstance);
            }
        }
        else if (Objects.equals(powerStatus, AgentConstant.RUNNING) && (VmInstanceStatus.INSTANCE_RUNNING != tblVmInstance.getPhaseStatus()
                || VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE != tblVmInstance.getPhaseStatus()))
        {
            if (VmInstanceStatus.INSTANCE_POWEROFF == tblVmInstance.getPhaseStatus())
            {
                log.info("vm instance power on, instanceId:{}, phaseStatus:{}", tblVmInstance.getVmInstanceId(), tblVmInstance.getPhaseStatus());
                tblVmInstance.setPhaseStatus(VmInstanceStatus.INSTANCE_RUNNING);
                tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                vmInstanceService.updateById(tblVmInstance);
            }
        }
    }


    private String healthCheck(HypervisorNode tblHypervisorNode)
    {
        AgentRsp agentRsp = hypervisorNodeTimerProcessor.getAgent(tblHypervisorNode.getAgentId());
        if (agentRsp == null || StrUtil.isBlank(agentRsp.getAgentIp()))
        {
            return getVmsStatus(tblHypervisorNode);
        }
        if (agentRsp.getMasterL3())
        {
            AgentConstant.L3Ip.set(agentRsp.getAgentIp());
            if (!tblHypervisorNode.getMasterL3())
            {
                tblHypervisorNode.setMasterL3(true);
                hypervisorNodeService.updateById(tblHypervisorNode);
            }
        }
        else
        {
            if (tblHypervisorNode.getMasterL3())
            {
                tblHypervisorNode.setMasterL3(false);
                hypervisorNodeService.updateById(tblHypervisorNode);
            }
        }

        return getVmsStatus(tblHypervisorNode);
    }

    public String backupNodeIsHealthy(HypervisorNode tblHypervisorNode)
    {
        if (StrUtil.isBlank(tblHypervisorNode.getBackupNodeId()) || Objects.equals(tblHypervisorNode.getBackupNodeId(), tblHypervisorNode.getNodeId()))
        {
            return null;
        }
        String backupNodeId = tblHypervisorNode.getBackupNodeId();
        HypervisorNode backupNode = hypervisorNodeService.getById(backupNodeId);
        if (null == backupNode)
        {
            return null;
        }
        if (vmInstancesOnBackupNode(tblHypervisorNode))
        {
            return AgentConstant.OK;
        }
        return null;
    }

    private String getVmsStatus(HypervisorNode tblHypervisorNode)
    {
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        String url = "http://" + tblHypervisorNode.getManageIp() + ":" + vmAgentPort + ComputeUrl.V1_VM_URL;

        BaseRsp result;
        try
        {
            result = HttpActionUtil.get(url, BaseRsp.class);
//            resultMap = JsonUtil.jsonToMap(result, BaseRsp.class);
            assert result != null;
            if (!AgentConstant.OK.equals(result.getStatus()))
            {
                log.info("hypervisorNode health check error:{} nodeIP:{}", result, tblHypervisorNode.getManageIp());
                return handleError(tblHypervisorNode);
            }
//            result = HttpActionUtil.get(networkUrl);
//            resultMap = JsonUtil.jsonToMap(result);
//            assert resultMap != null;
        }
        catch (Exception e)
        {
            log.info("get vm status error:{}", e.getMessage());
            return handleError(tblHypervisorNode);
        }
        if (!AgentConstant.OK.equals(result.getStatus()))
        {
            log.info("hypervisorNode health check error:{} nodeIP:{}", result, tblHypervisorNode.getManageIp());
            return handleError(tblHypervisorNode);
        }
        int errorCount = tblHypervisorNode.getErrorCount();
        if (errorCount > 0)
        {
            errorCount--;
            String status = null;
            if (0 == errorCount)
            {
                logRpcService.getLogService().addEvent("", "健康状态检测中", String.format("hypervisorNodeId:%s 上线", tblHypervisorNode.getNodeId()), "计算节点恢复正常");
                logRpcService.getAlertService().sendAlarmInfo(tblHypervisorNode.getNodeId(), "resolved");
                status = AgentConstant.OK;
                tblHypervisorNode.setPhaseStatus(VmInstanceStatus.HYPERVISOR_NODE_CREATED);
                tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            }
            tblHypervisorNode.setErrorCount(errorCount);
            hypervisorNodeService.updateById(tblHypervisorNode);
            return status;
        }

        return AgentConstant.OK;
    }


    private String handleError(HypervisorNode tblHypervisorNode)
    {
//        boolean ok = ping(tblHypervisorNode.getManageIp(),3,1);
//        if (ok)
//        {
//            return AgentConstant.OK;
//        }
        Integer errCount = tblHypervisorNode.getErrorCount();
        errCount++;
        if (errCount < maxErrorCount)
        {
            tblHypervisorNode.setErrorCount(errCount);
            tblHypervisorNode.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            hypervisorNodeService.updateById(tblHypervisorNode);
            return AgentConstant.OK;
        }
        return null;
    }

    public void updateNewHypervisorNode(HypervisorNode tblHypervisorNode)
    {
        String nodeId = tblHypervisorNode.getNodeId();
        LambdaUpdateWrapper<VmInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VmInstance::getNodeId, nodeId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        List<VmInstance> tblVmInstances = vmInstanceService.list(updateWrapper);
        if (tblVmInstances.size() == 0)
        {
            return;
        }
        VmInstance tblVmInstance = new VmInstance();
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVmInstance.setLastNodeId(nodeId);
        tblVmInstance.setDestNodeId(tblHypervisorNode.getBackupNodeId());
        tblVmInstance.setNodeId(tblHypervisorNode.getBackupNodeId());
        tblVmInstance.setPhaseStatus(VmInstanceStatus.GET_INSTANCE_RESUME_STATUS);
        boolean ok = vmInstanceService.update(tblVmInstance, updateWrapper);
        if (ok)
        {
            for (VmInstance oldTblVmInstance : tblVmInstances)
            {
                logRpcService.getLogService().addEvent(oldTblVmInstance.getUserId(), String.format("vm %s 自动迁移中", oldTblVmInstance.getVmInstanceId()),
                        String.format("请求参数: destNodeId:%s", tblHypervisorNode.getBackupNodeId()), "迁移中");
            }
        }
    }


    // update libvirt-exporter vmName and vmUserId
    public void createMonitorTags(VmInstance tblVmInstance)
    {

        MonitorTagsCreateReq req = new MonitorTagsCreateReq(tblVmInstance.getInstanceIdFromAgent(),
                tblVmInstance.getVmInstanceId(), tblVmInstance.getName(), tblVmInstance.getFlavorId(),
                tblVmInstance.getUserId(), tblVmInstance.getInstanceGroupId(), tblVmInstance.getCmpTenantId(), tblVmInstance.getCmpUserId());
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == tblHypervisorNode)
        {
            log.error("get hypervisorNode :null, vm instanceId:{}", tblVmInstance.getVmInstanceId());
            return;
        }
        String url = "http://" + tblHypervisorNode.getManageIp() + ComputeUrl.LIBVERT_EXPORTER_TAG_PORT_URL;
        String jsonStr = JsonUtil.objectToJson(req);
        try
        {
            String result = HttpActionUtil.post(url, jsonStr);
            MonitorTagRsp getMonitorTagRsp = JsonUtil.jsonToPojo(result, MonitorTagRsp.class);
            assert getMonitorTagRsp != null;
            if (AgentConstant.MONITOR_TAG_OK != getMonitorTagRsp.getCode())
            {
                log.error("get GetMonitorTagRsp : err, vm instanceId:{} msg:{}", tblVmInstance.getVmInstanceId(), getMonitorTagRsp.getMessage());
            }
        }
        catch (Exception exception)
        {
            log.error("post request url:{} , error:{}", url, exception.getMessage());
        }
    }

    public void createGpuMonitorTags(VmInstance tblVmInstance, String url)
    {

    }

    public List<String> getVmInstanceIdsFromNodeAgent(HypervisorNode tblHypervisorNode)
    {
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        String url = "http://" + tblHypervisorNode.getManageIp() + ":" + vmAgentPort + ComputeUrl.V1_VM_URL;
        VmInstancesRspFromAgent vmInstanceIdsRsp = HttpActionUtil.getObject(url, VmInstancesRspFromAgent.class);
        return vmInstanceIdsRsp.getVmIds();
    }

    public Boolean vmInstancesOnBackupNode(HypervisorNode tblHypervisorNode)
    {
        String backupNodeId = tblHypervisorNode.getBackupNodeId();
        HypervisorNode backupNode = hypervisorNodeService.getById(backupNodeId);
        List<VmInstance> tblVmInstances = getVmInstanceByNodeId(tblHypervisorNode.getNodeId());
        Set<String> dbVmInstanceIds = tblVmInstances.stream().map(VmInstance::getInstanceIdFromAgent).collect(Collectors.toSet());
        Set<String> nodeVmInstanceIds = new HashSet<>(getVmInstanceIdsFromNodeAgent(backupNode));
        log.info("nodeVmInstanceIds:{}, dbVmInstanceIds:{}", nodeVmInstanceIds, dbVmInstanceIds);
        return nodeVmInstanceIds.containsAll(dbVmInstanceIds);
    }


    public void createMonitorTagsFromNode(HypervisorNode tblHypervisorNode)
    {
        List<VmInstance> tblVmInstances = getVmInstanceByNodeId(tblHypervisorNode.getNodeId());
        for (VmInstance tblVmInstance : tblVmInstances)
        {
            createMonitorTags(tblVmInstance);
        }
    }

    public List<VmInstance> getVmInstanceByNodeId(String nodeId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmInstance::getPhaseStatus, REMOVED)
                .eq(VmInstance::getNodeId, nodeId);
        return vmInstanceService.list(queryWrapper);
    }


    public static boolean ping(String ipAddress, int pingTimes, int timeOut)
    {
        BufferedReader in = null;
        Runtime r = Runtime.getRuntime();   //  将要执行的ping命令,此命令是linux格式的命令
        String pingCommand = "/bin/ping " + ipAddress + " -n " + pingTimes + " -w " + timeOut;
        try
        {    //  执行命令并获取输出

            Process p = r.exec(pingCommand);
            if (p == null)
            {
                return false;
            }
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));    //  逐行检查输出,计算类似出现=23ms TTL=62字样的次数
            int connectedCount = 0;
            String line = null;
            while ((line = in.readLine()) != null)
            {
                connectedCount += getCheckResult(line);
            }    //  如果出现类似=23ms TTL=62这样的字样,出现的次数=测试次数则返回真
            return connectedCount >= pingTimes - 1;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();    //  出现异常则返回假
            return false;
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                log.info("ping error,{}", e.getMessage());
            }
        }
    }

    private static int getCheckResult(String line)
    {   //  System.out.println("控制台输出的结果为:"+line);
        Pattern pattern = Pattern.compile("(\\d+ms)(\\s+)(TTL=\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find())
        {
            return 1;
        }
        return 0;
    }

}
