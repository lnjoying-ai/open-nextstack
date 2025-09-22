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
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.NfsStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.request.NfsCreateReqFromAgent;
import com.lnjoying.vm.domain.backend.response.NfsRspFromAgent;
import com.lnjoying.vm.domain.backend.response.ResultFromAgentRsp;
import com.lnjoying.vm.entity.Nfs;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.NfsService;
import com.lnjoying.vm.service.biz.CombRpcSerice;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Slf4j
@Component
public class NfsTimerProcessor extends AbstractRunnableProcessor
{
    public NfsTimerProcessor()
    {
    }

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private ComputeConfig computeConfig;

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private HypervisorNodeService nodeService;

    @Autowired
    private VmScheduler vmScheduler;

    @Autowired
    private NfsService nfsService;

    @Override
    public void start()
    {
        log.info("nfs timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("nfs timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            List<Nfs> nfsList = getMiddleStatusInstances();
            for (Nfs tblNfs : nfsList)
            {
                processNfs(tblNfs);
            }
        }
        catch (Exception e)
        {
            log.error("nfs timer processor exception: {}", e.getMessage());
        }
    }

    private List<Nfs> getMiddleStatusInstances()
    {
        LambdaQueryWrapper<Nfs> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Nfs::getPhaseStatus, NfsStatus.NFS_CREATED)
                .ne(Nfs::getPhaseStatus, REMOVED)
                .ne(Nfs::getPhaseStatus, NfsStatus.NFS_REMOVE_FAILED)
                .ne(Nfs::getPhaseStatus, NfsStatus.NFS_CREATE_FAILED);
        return nfsService.list(queryWrapper);
    }

    private void processNfs(Nfs tblNfs)
    {
        int phaseStatus = tblNfs.getPhaseStatus();
        switch (phaseStatus)
        {
            case NfsStatus.NFS_INIT:
                processCreatingPort(tblNfs);
                break;
            case NfsStatus.NFS_CREATING:
                processCreating(tblNfs);
                break;
            case NfsStatus.NFS_REMOVING:
                processRemoving(tblNfs);
                break;
            default:
                defaultProcess(tblNfs);
                break;
        }
    }

    public String scheduleNode(Nfs tblNfs)
    {
        if (StrUtil.isNotBlank(tblNfs.getNodeIp())) return tblNfs.getNodeIp();
        List<String> nodeIps = vmScheduler.getBottom5VmsNode();
        Random rand = new Random();
        String nodeIp = nodeIps.get(rand.nextInt(nodeIps.size()));
        tblNfs.setNodeIp(nodeIp);
        tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = nfsService.updateById(tblNfs);
        if (!ok)
        {
            log.error("update node_ip failed, nfsId:{}", tblNfs.getNfsId());
            return null;
        }
        return nodeIp;
    }

    public void defaultProcess(Nfs tblNfs)
    {
        log.info("default process, nfsId:{}", tblNfs.getNfsId());
        NfsRspFromAgent resp = getNfsStatusFromAgent(tblNfs);
        if (resp == null)
        {
            log.error("get nfs status from agent failed, nfsId:{}", tblNfs.getNfsId());
            return;
        }
        boolean ok;
        String status = Objects.requireNonNull(resp.getStatus());
        if (status.equals(AgentConstant.FAILED))
        {
            if (null != resp.getReason() && resp.getReason().equals(AgentConstant.NFS_NOT_EXIST))
            {
                tblNfs.setPhaseStatus(REMOVED);
                tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                ok = nfsService.updateById(tblNfs);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent删除NFS服务",
                            String.format("请求参数: nfsId:%s, size: %d", tblNfs.getNfsId(), tblNfs.getSize()), "删除成功");
                }
                return;
            }
        }
        switch (Objects.requireNonNull(Objects.requireNonNull(resp).getPhase()))
        {
            case AgentConstant.ADDED:
                tblNfs.setPhaseStatus(NfsStatus.NFS_CREATED);
                tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                ok = nfsService.updateById(tblNfs);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent创建NFS服务",
                            String.format("请求参数: nfsId:%s, size: %d", tblNfs.getNfsId(), tblNfs.getSize()), "创建成功");
                }
                break;
            case AgentConstant.ADD_FAILED:
                tblNfs.setPhaseStatus(NfsStatus.NFS_CREATE_FAILED);
                tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                ok = nfsService.updateById(tblNfs);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent创建NFS服务",
                            String.format("请求参数: nfsId:%s, size: %d", tblNfs.getNfsId(), tblNfs.getSize()), "创建失败");
                }
                break;
            case AgentConstant.DELETE_FAILED:
                tblNfs.setPhaseStatus(NfsStatus.NFS_REMOVE_FAILED);
                tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                ok = nfsService.updateById(tblNfs);
                if (ok)
                {
                    logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent删除NFS服务",
                            String.format("请求参数: nfsId:%s, size: %d", tblNfs.getNfsId(), tblNfs.getSize()), "删除失败");
                }
                break;
            default:
                break;
        }
    }


    public void processCreatingPort(Nfs tblNfs)
    {
        log.info("processCreatingPort: nfsId={}", tblNfs.getNfsId());
        String nodeIp = scheduleNode(tblNfs);
        if (StrUtil.isBlank(nodeIp))
        {
            log.error("schedule node failed, nfsId:{}", tblNfs.getNfsId());
            return;
        }
        NetworkService.CreatePortReq createPortReq = new NetworkService.CreatePortReq();
        createPortReq.setStaticIp("");
        createPortReq.setSubnetId(tblNfs.getSubnetId());
        createPortReq.setContext(tblNfs.getNfsId());
        createPortReq.setIsVip(false);
        String portId = combRpcSerice.getNetworkService().createPort(createPortReq, false, AgentConstant.NFS_PORT);
        if (StrUtil.isBlank(portId))
        {
            log.error("get portId null, nfsId:{}", tblNfs.getPortId());
            return;
        }
        tblNfs.setPortId(portId);
        tblNfs.setPhaseStatus(NfsStatus.NFS_CREATING);
        boolean ok = nfsService.updateById(tblNfs);
        if (!ok)
        {
            log.error("update nfs failed, nfsId:{}", tblNfs.getNfsId());
        }
    }

    public void processCreating(Nfs tblNfs)
    {
        log.info("processCreating nfsId:{}", tblNfs.getNfsId());
        NetworkService.TenantNetworkPort port = combRpcSerice.getNetworkService().getTenantNetworkPort(tblNfs.getPortId());
        if (StrUtil.isBlank(port.getIpAddress()) || StrUtil.isBlank(port.getMacAddress()) ||
                StrUtil.isBlank(port.getOfport()))
        {
            log.info("get port ip null, nfsId:{}", tblNfs.getNfsId());
            return;
        }
        NfsCreateReqFromAgent req = new NfsCreateReqFromAgent();
        req.setVpcCidr(new ArrayList<String>()
        {{
            add(port.getVpcCidr());
        }});
        req.setSubnetCidr(new ArrayList<String>()
        {{
            add(port.getSubnetCidr());
        }});
        req.setIp(new ArrayList<String>()
        {{
            add(port.getIpAddress());
        }});
        req.setMac(new ArrayList<String>()
        {{
            add(port.getMacAddress());
        }});
        req.setSize(tblNfs.getSize().toString());
        //随机获取长度为8的字符串
        req.setName(Utils.assignUUId().substring(0, 9));
        req.setOfport(new ArrayList<String>()
        {{
            add(port.getOfport());
        }});
        req.setVlanId(new ArrayList<String>()
        {{
            add(port.getVlanId());
        }});
        String nfsIdFromAgent = createNfsFromAgent(tblNfs, req);
        if (StrUtil.isBlank(nfsIdFromAgent))
        {
            log.error("create nfs failed, nfsId:{}", tblNfs.getNfsId());
            return;
        }
        tblNfs.setNfsIdFromAgent(nfsIdFromAgent);
        tblNfs.setPhaseStatus(NfsStatus.GET_NFS_CREATED_STATUS);
        tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = nfsService.updateById(tblNfs);
        if (!ok)
        {
            log.error("update nfs failed, nfsId:{}", tblNfs.getNfsId());
        }
        if (ok)
        {
            logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent创建NFS服务",
                    String.format("请求参数: nfsId:%s, size: %d", tblNfs.getNfsId(), tblNfs.getSize()), "创建中");
        }
    }

    public void processRemoving(Nfs tblNfs)
    {
        log.info("processRemoving nfsId:{}", tblNfs.getNfsId());
        String nfsId = removeNfsFromAgent(tblNfs);
        if (StrUtil.isBlank(nfsId))
        {
            log.error("remove nfs from agent failed, nfsId:{}", tblNfs.getNfsId());
            tblNfs.setPhaseStatus(NfsStatus.NFS_REMOVE_FAILED);
            logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent删除NFS服务",
                    String.format("请求参数: nfsId:%s", tblNfs.getNfsId()), "删除失败");
        }
        else
        {
            tblNfs.setPhaseStatus(NfsStatus.GET_NFS_REMOVED_STATUS);
            logRpcService.getLogService().addEvent(tblNfs.getUserId(), "Agent删除NFS服务",
                    String.format("请求参数: nfsId:%s", tblNfs.getNfsId()), "删除中");
        }
        tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = nfsService.updateById(tblNfs);
        if (!ok)
        {
            log.error("update nfs database failed, nfsId:{}", tblNfs.getNfsId());
        }
    }

    public String getUrl(Nfs tblNfs)
    {
        Integer vmAgentPort = computeConfig.getVmAgentPort();
        if (null == vmAgentPort || 0 == vmAgentPort)
        {
            vmAgentPort = ComputeUrl.VM_AGENT_PORT;
        }
        String managerIp = tblNfs.getNodeIp();
        return "http://" + managerIp + ":" + vmAgentPort + ComputeUrl.V1_NFS_URL;
    }

    public String createNfsFromAgent(Nfs tblNfs, NfsCreateReqFromAgent req)
    {
        String url = getUrl(tblNfs);
        log.info("create nfs from agent, req: {}, nfsId: {}", req, tblNfs.getNfsId());
        try
        {
            ResponseEntity<ResultFromAgentRsp> result = HttpActionUtil.postForEntity(url, req, ResultFromAgentRsp.class);
            ResultFromAgentRsp resultFromAgentRsp = result.getBody();
            if (null == resultFromAgentRsp)
            {
                log.error("get response of creating nfs error,  nfsId:{}", tblNfs.getNfsId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            if (StrUtil.isBlank(resultFromAgentRsp.getUuid()))
            {
                log.error("nfs is null, nfsRspFromAgent: {}", resultFromAgentRsp);
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            return resultFromAgentRsp.getUuid();
        }
        catch (Exception e)
        {
            log.error("create nfs from agent error, nfsId:{}", tblNfs.getNfsId(), e);
            return null;
        }
    }

    public String removeNfsFromAgent(Nfs tblNfs)
    {
        String url = getUrl(tblNfs) + "/" + tblNfs.getNfsIdFromAgent();
        log.info("remove nfs from agent, nfsId:{}", tblNfs.getNfsId());
        try
        {
            String result = HttpActionUtil.delete(url);
            Map resultMap = JsonUtil.jsonToMap(result);
            if (null == resultMap)
            {
                log.error("get response of removing nfs error,  lbId:{}", tblNfs.getNfsId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }

            String status = (String) resultMap.get("status");
            if (AgentConstant.PENDING_STATUS.equals(status) ||
                    (AgentConstant.FAILED.equals(status) && result.contains(AgentConstant.NOT_EXIST)))
            {
                return tblNfs.getNfsId();
            }

            return tblNfs.getNfsId();
        }
        catch (Exception e)
        {
            log.error("remove nfs from agent error, nfsId:{}", tblNfs.getNfsId(), e);
            return null;
        }
    }

    public NfsRspFromAgent getNfsStatusFromAgent(Nfs tblNfs)
    {
        String url = getUrl(tblNfs) + "/" + tblNfs.getNfsIdFromAgent();
        try
        {
            return HttpActionUtil.getObject(url, NfsRspFromAgent.class);
        }
        catch (Exception e)
        {
            log.error("get nfs status from agent error: {}", e.getMessage());
            return null;
        }
    }

}
