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

package com.lnjoying.justice.network.processor;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.ImmutableMap;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.compute.ComputeService;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.common.Protocols;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.response.BaseRsp;
import com.lnjoying.justice.network.entity.SecurityGroup;
import com.lnjoying.justice.network.entity.SecurityGroupRule;
import com.lnjoying.justice.network.entity.SgVmInstance;
import com.lnjoying.justice.network.mapper.SecurityGroupMapper;
import com.lnjoying.justice.network.service.PortService;
import com.lnjoying.justice.network.service.SecurityGroupRuleService;
import com.lnjoying.justice.network.service.SecurityGroupService;
import com.lnjoying.justice.network.service.SgVmInstanceService;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.lnjoying.justice.network.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Component
@Slf4j
public class SecurityGroupTimerProcessor extends AbstractRunnableProcessor
{


    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    private SecurityGroupService securityGroupService;

    @Autowired
    private SgVmInstanceService sgVmInstanceService;

    @Autowired
    private SecurityGroupRuleService securityGroupRuleService;

    @Autowired
    private PortService portService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private LogRpcService logRpcService;

    @Resource
    SecurityGroupMapper securityGroupMapper;

    private VmService vmService ;

    public static Map<Integer,String> directionMap = ImmutableMap.of(
            0,"in",
            1,"out"
    );
    public static Map<Integer,String> protocolMap = ImmutableMap.of(
            Protocols.TCP,"tcp",
             Protocols.UDP,"udp",
             Protocols.IP,"all",
             Protocols.ICMP,"icmp"
    );

    public static Map<Integer,String> actionMap = ImmutableMap.of(
             0, "drop",
             1, "accept"
    );

    public static Map<String,String> icmpPortMap = new ImmutableMap.Builder<String,String>()
            .put("0", "all")
            .put("1", "echo")
            .put("2", "echo_reply")
            .put("3", "fragment_need_sf_set")
            .put("4", "host_redirect")
            .put("5", "host_tos_redirect")
            .put("6", "host_unreachable")
            .put("7", "information_reply")
            .put("8", "information_request")
            .put("9", "net_redirect")
            .put("10", "net_tos_redirect")
            .put("11", "net_unreachable")
            .put("12", "parameter_problem")
            .put("13", "port_unreachable")
            .put("14", "protocol_unreachable")
            .put("15", "reassembly_timeout")
            .put("16", "source_quench")
            .put("17", "source_route_failed")
            .put("18", "timestamp_reply")
            .put("19", "timestamp_request")
            .put("20", "ttl_exceeded")
            .put("all", "all")
            .build();

    public SecurityGroupTimerProcessor()
    {

    }

    @Override
    public void start()
    {
        log.info("securityGroup timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("securityGroup timer processor stop");
    }

    @Override
    public void run()
    {
        vmService = combRpcSerice.getVmService();
        CompletableFuture.runAsync(()->processVmInstances(getMiddleStatusVmInstanceIds()));
        CompletableFuture.runAsync(()->processSecurityGroups(getMiddleStatusSecurityGroups()));
//        CompletableFuture.runAsync(this::processUpdateSecurityGroup);
    }

    private List<SecurityGroup> getMiddleStatusSecurityGroups()
    {
        LambdaQueryWrapper<SecurityGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(SecurityGroup::getPhaseStatus, PhaseStatus.ADDED )
                .ne(SecurityGroup::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(SecurityGroup::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(SecurityGroup::getPhaseStatus,PhaseStatus.UPDATED)
                .ne(SecurityGroup::getPhaseStatus,PhaseStatus.UPDATE_FAILED)
                .ne(SecurityGroup::getPhaseStatus,REMOVED);
        return securityGroupService.list(queryWrapper);

    }

    private void processSecurityGroups(List<SecurityGroup> tblSecurityGroups)
    {
        try
        {
            log.debug("get tblSecurityGroups :{}", tblSecurityGroups);
            for ( SecurityGroup tblSecurityGroup: tblSecurityGroups )
            {
                processSecurityGroup(tblSecurityGroup);
            }
        }
        catch (Exception e)
        {
            log.error("security group timer processor error:  {}", e.getMessage());
        }
    }

    private void processSecurityGroup(SecurityGroup tblSecurityGroup)
    {
        int phaseStatus = tblSecurityGroup.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateSecurityGroup(tblSecurityGroup);
                    break;
                case PhaseStatus.UPDATING:
                    processUpdateSecurityGroup(tblSecurityGroup);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveSecurityGroup(tblSecurityGroup);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("security group timer processor error: sgId {}, phase status {} , exception {}", tblSecurityGroup.getSgId(), phaseStatus, e.getMessage());
        }
    }

    private List<List<String>> getMiddleStatusVmInstanceIds()
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(SgVmInstance::getPhaseStatus, PhaseStatus.ADDED)
                .ne(SgVmInstance::getPhaseStatus, PhaseStatus.APPLY_FAILED)
                .ne(SgVmInstance::getPhaseStatus,PhaseStatus.APPLIED)
                .ne(SgVmInstance::getPhaseStatus,REMOVED);

//        criteria.andPhaseStatusNotEqualTo(PhaseStatus.UNAPPLY_FAILED);
        return sgVmInstanceService.list(queryWrapper).stream().
                map(tblSgVmInstance -> {
                    List<String> sgVmList = new ArrayList<>();
                    sgVmList.add(tblSgVmInstance.getInstanceId());
                    sgVmList.add(tblSgVmInstance.getPhaseStatus().toString());
                    return sgVmList;
                }).distinct().collect(Collectors.toList());
    }

    private void processVmInstances(List<List<String>> vmInstanceIdSgs)
    {
            log.debug("get vmInstanceIdSgs :{}", vmInstanceIdSgs);
            for ( List<String> vmInstanceIdSg: vmInstanceIdSgs )
            {
                try
                {
                    processVmInstance(vmInstanceIdSg);
                }
                catch (Exception e)
                {
                    log.error("security group timer processor error:  {}", e.getMessage());
                }
            }

    }

    private void processVmInstance(List<String> vmInstanceIdSg)
    {
        String vmInstanceId = vmInstanceIdSg.get(0);
        int phaseStatus = Integer.parseInt(vmInstanceIdSg.get(1));
        switch (phaseStatus)
        {
            case PhaseStatus.APPLYING:
                processApplyingPhase(vmInstanceId);
                break;
            case PhaseStatus.UNAPPLIED:
                processUnAppliedPhase(vmInstanceId);
                break;
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void processCheckAppliedResult(String vmInstanceId)
    {
        List<ComputeService.VpcAndPortInfo> vpcPortInfos = vmService.getVpcAndPortFromVmInstanceId(vmInstanceId);
        int vmSgPhaseStatus = PhaseStatus.APPLIED;
        for (ComputeService.VpcAndPortInfo vpcPortInfo:vpcPortInfos)
        {
            String portIdFromAgent = portService.getById(vpcPortInfo.getPortId()).getPortIdFromAgent();
            vmSgPhaseStatus = getPortBoundSecurityGroupStatus(portIdFromAgent);
            if (PhaseStatus.APPLYING == vmSgPhaseStatus)
            {
                return;
            }
            else if ( PhaseStatus.APPLY_FAILED == vmSgPhaseStatus)
            {
                break;
            }
        }
        log.info("vmSgPhaseStatus:{}", vmSgPhaseStatus);
        List<SgVmInstance> tblSgVmInstances = getSgVmInstancesByPhase(vmInstanceId, PhaseStatus.CHECK_APPLIED_RESULT);
        int finalVmSgPhaseStatus = vmSgPhaseStatus;
        tblSgVmInstances.forEach(tblSgVmInstance ->
                {
                    tblSgVmInstance.setPhaseStatus(finalVmSgPhaseStatus);
                    sgVmInstanceService.updateById(tblSgVmInstance);
                }
        );

    }

    @Transactional(rollbackFor = Exception.class)
    public void processApplyingPhase(String vmInstanceId)
    {
        SgVmInstance tblWrongSgVmInstance = securityGroupMapper.selectWrongPhaseSgVmInstance(vmInstanceId);
        if (null != tblWrongSgVmInstance)
        {
            tblWrongSgVmInstance.setPhaseStatus(REMOVED);
            tblWrongSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            sgVmInstanceService.updateById(tblWrongSgVmInstance);
        }
        List<SgVmInstance> tblSgVmInstances = getNotUnAppliedSgVmInstances(vmInstanceId);
        log.info("get tblSgVmInstances:{}", tblSgVmInstances);
        List<SgVmInstance>  applyingVmInstances = getSgVmInstancesByPhase(vmInstanceId, PhaseStatus.APPLYING);
        //List<String> sgIds = tblSgVmInstances.stream().map(TblSgVmInstance::getSgIdFromAgent).distinct().collect(Collectors.toList());
        log.info("get applyingVmInstances:{}", applyingVmInstances);
        List<String> sgIds =  tblSgVmInstances.stream().map(
                tblSgVmInstance -> securityGroupService.getById(
                tblSgVmInstance.getSgId()).getSgIdFromAgent()).distinct().collect(Collectors.toList());
        log.info("get sgIds:{}", sgIds);
        List<ComputeService.VpcAndPortInfo> vpcPortInfos = vmService.getVpcAndPortFromVmInstanceId(vmInstanceId);
        if ( null == vpcPortInfos|| vpcPortInfos.size() == 0)
        {
            return;
        }
        log.info("get vpcPortInfos: {}", vpcPortInfos);
        for (ComputeService.VpcAndPortInfo vpcPortInfo:vpcPortInfos)
        {
            if (null != vpcPortInfo.getIsRemoved() && vpcPortInfo.getIsRemoved())
            {
                log.info("vm instance is removed, vmId:{}", vmInstanceId);
                applyingVmInstances.forEach(tblSgVmInstance ->
                {
                    tblSgVmInstance.setPhaseStatus(REMOVED);
                    tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    sgVmInstanceService.updateById(tblSgVmInstance);
                });
                return;
            }
            log.info("get portId:{}", vpcPortInfo.getPortId());
            if (StrUtil.isBlank(vpcPortInfo.getPortId())) return;
            String portIdFromAgent = portService.getById(vpcPortInfo.getPortId()).getPortIdFromAgent();
            log.info("portIdFromAgent :{}", portIdFromAgent);
            if (StrUtil.isBlank(portIdFromAgent) )
            {
                log.info("portIdFromAgent is blank, portId:{}", vpcPortInfo.getPortId());
                return;
            }
            String vmInstanceIdFromAgent = vmBondSecurityGroup(portIdFromAgent, sgIds);
            if (StrUtil.isBlank(vmInstanceIdFromAgent)) {
                log.info("vmInstanceIdFromAgent is blank, portId:{}", vpcPortInfo.getPortId());
                applyingVmInstances.forEach(tblSgVmInstance ->
                {
                    tblSgVmInstance.setPhaseStatus(PhaseStatus.APPLY_FAILED);
                    tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    sgVmInstanceService.updateById(tblSgVmInstance);
                });
                return;
            }
        }
        applyingVmInstances.forEach(tblSgVmInstance ->
        {
            tblSgVmInstance.setPhaseStatus(PhaseStatus.APPLIED);
            tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            sgVmInstanceService.updateById(tblSgVmInstance);
        });
    }


    public void processUnAppliedPhase(String vmInstanceId)
    {
        List<SgVmInstance> tblSgVmInstances = getNotUnAppliedSgVmInstances(vmInstanceId);
//         List<TblSgVmInstance> tblSgVmInstances = getSgVmInstancesByPhase(vmInstanceId, PhaseStatus.UNAPPLIED);
        List<ComputeService.VpcAndPortInfo> vpcPortInfos = vmService.getVpcAndPortFromVmInstanceId(vmInstanceId);
        if ( null == vpcPortInfos|| vpcPortInfos.size() == 0)
        {
            return;
        }
        List<String> sgIds = tblSgVmInstances.stream().map(tblSgVmInstance -> securityGroupService.getById(
                tblSgVmInstance.getSgId()).getSgIdFromAgent()).distinct().collect(Collectors.toList());
        String portIdFromAgent = null;
        String vmInstanceIdFromAgent = null;
        for (ComputeService.VpcAndPortInfo vpcPortInfo:vpcPortInfos)
        {
            portIdFromAgent = portService.getById(vpcPortInfo.getPortId()).getPortIdFromAgent();
            if (null != portIdFromAgent)
            {
                vmInstanceIdFromAgent = vmBondSecurityGroup(portIdFromAgent, sgIds);
                if (null == vmInstanceIdFromAgent) return ;
            }
        }
        log.info("portIdFromAgent:{}, vmInstanceIdFromAgent:{}", portIdFromAgent, vmInstanceIdFromAgent);

        LambdaUpdateWrapper<SgVmInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SgVmInstance::getPhaseStatus, PhaseStatus.UNAPPLIED);

        SgVmInstance tblSgVmInstance = new SgVmInstance();
        tblSgVmInstance.setPhaseStatus(REMOVED);
        tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        sgVmInstanceService.update(tblSgVmInstance, updateWrapper);
//            tblSgVmInstances.forEach(tblSgVmInstance -> {
//                tblSgVmInstance.setPhaseStatus(REMOVED);
////                networkRepository.deleteSgVmInstance(tblSgVmInstance.getSgVmId());
//                networkRepository.updateSgVmInstance(tblSgVmInstance);
//            });

    }

    private List<SgVmInstance> getSgVmInstancesByPhase(String vmInstanceId, int phaseStatus)
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(SgVmInstance::getUpdateTime)
                .eq(SgVmInstance::getInstanceId, vmInstanceId);
        if (-1 != phaseStatus)
        {
            queryWrapper.and(qw -> qw.eq(SgVmInstance::getPhaseStatus, phaseStatus));
        }

        return sgVmInstanceService.list(queryWrapper);
    }

    private void processCreateSecurityGroup(SecurityGroup tblSecurityGroup)
    {
        try
        {
           String sgIdFromAgent = createSecurityGroupFromAgent(tblSecurityGroup.getSgId());
           if (null == sgIdFromAgent)
           {
                tblSecurityGroup.setPhaseStatus(PhaseStatus.ADD_FAILED);
           }
           else
           {
               tblSecurityGroup.setSgIdFromAgent(sgIdFromAgent);
               tblSecurityGroup.setPhaseStatus(PhaseStatus.ADDED);
           }
           tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
           boolean ok = securityGroupService.updateById(tblSecurityGroup);
           if (!ok){
               throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
           }
           if (PhaseStatus.ADD_FAILED == tblSecurityGroup.getPhaseStatus())
           {
               logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(), "Agent 创建安全组时发生异常",
                       String.format("请求参数 name:%s ",tblSecurityGroup.getName()), "发生异常:返回结果为空");
           }
           else if (PhaseStatus.AGENT_ADDING == tblSecurityGroup.getPhaseStatus())
           {
                logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(), "Agent 正在创建安全组",
                          String.format("请求参数 name:%s ",tblSecurityGroup.getName()), "创建完成");
           }
            log.info("created security group:{} sgIdFromAgent: {}", tblSecurityGroup.getSgId(), sgIdFromAgent);
        }
        catch (WebSystemException e)
        {
            log.error("get rules error: sgId {}, {}", tblSecurityGroup.getSgId(), e.getMessage());
        }
    }


    private String updateSGRules(String sgIdFromAgent, List<String> rules)
    {
        try
        {
            return updateSGRulesFromAgent(sgIdFromAgent, rules);
        }
        catch (Exception e)
        {
            log.error("updateSecurityGroupRules error: {}, sgId: {}, rules: {}",e.getMessage(),sgIdFromAgent,rules);
            return null;
        }
    }

    private String updateSGRulesFromAgent(String sgIdFromAgent, List<String> rules)
    {
        String url = String.format("%s%s/%s/update",combRpcSerice.getVmService().getL3IpPort(), networkAgentConfig.getSgUrl(), sgIdFromAgent);
        Rules putRules = new Rules();
        putRules.setRules(rules);
        String jsonStr = JsonUtil.objectToJson(putRules);
        BaseRsp result = HttpActionUtil.put(url, jsonStr, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating security group error, sgId: {}", sgIdFromAgent);
            return null;
        }
        if (Objects.equals(result.getStatus(), AgentConstant.OK) )
        {
            return sgIdFromAgent;
        }

        return null;
    }

    private String createSecurityGroupFromAgent(String sgId)
    {
        String url = String.format("%s%s", combRpcSerice.getVmService().getL3IpPort() , networkAgentConfig.getSgUrl());

        BaseRsp result = HttpActionUtil.post(url,null, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating security group error,  sgId:{}", sgId);
            return null;
        }

        if (Objects.equals(result.getStatus(),AgentConstant.OK) && StrUtil.isNotBlank(result.getUuid()))
        {
            return result.getUuid();
        }
        return null;
    }

    private String removeSecurityGroupFromAgent(String sgIdFromAgent)
    {
        String url = String.format("%s%s/%s", combRpcSerice.getVmService().getL3IpPort() , networkAgentConfig.getSgUrl(),sgIdFromAgent);

        BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of removing security group error,  sgId:{}", sgIdFromAgent);
            return null;
        }
        if (Objects.equals(result.getStatus(),AgentConstant.OK)||
                (Objects.equals(result.getStatus(),AgentConstant.FAILED) && result.getReason().contains(AgentConstant.NOT_FOUND)))
        {
            return sgIdFromAgent;
        }

        return null;
    }


    private void getSecurityGroupStatus(SecurityGroup tblSecurityGroup)
    {
        try
        {

            String phaseStatus = getSecurityGroupStatusFromAgent(tblSecurityGroup.getSgIdFromAgent());
            String result;
            switch (Objects.requireNonNull(phaseStatus))
            {
                case AgentConstant.UPDATED:
                    tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATED);
                    result = "更新成功";
                    break;
                case AgentConstant.UPDATE_FAILED:
                    tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATE_FAILED);
                    result = "更新失败";
                    break;
                case AgentConstant.ADDED:
                    tblSecurityGroup.setPhaseStatus(PhaseStatus.ADDED);
                    result = "创建成功";
                    break;
                case AgentConstant.FAILED:
                    tblSecurityGroup.setPhaseStatus(REMOVED);
                    removeRulesBySgId(tblSecurityGroup.getSgId());
                    result = "删除成功";
                    break;
                default:
                    return;
            }
            tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = securityGroupService.updateById(tblSecurityGroup);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR,ErrorLevel.INFO);
            }
            logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(), "Agent 获取安全组状态",
                    String.format("请求参数 sgId:%s ",tblSecurityGroup.getSgId()), result);
        }
        catch (Exception e)
        {
            log.error("getSecurityGroupCreateStatus error:{}, sgId:{}, ", e.getMessage(), tblSecurityGroup.getSgId());
        }
    }

    private void removeRulesBySgId(String sgId)
    {
        LambdaUpdateWrapper<SecurityGroupRule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SecurityGroupRule::getSgId, sgId)
                .ne(SecurityGroupRule::getPhaseStatus, REMOVED);
        if (0 == securityGroupRuleService.count(updateWrapper))
        {
            return;
        }
        SecurityGroupRule securityGroupRule = new SecurityGroupRule();
        securityGroupRule.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        securityGroupRule.setPhaseStatus(REMOVED);
        boolean ok = securityGroupRuleService.update(securityGroupRule, updateWrapper);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR,ErrorLevel.INFO);
        }
    }

    private String getSecurityGroupStatusFromAgent(String sgIdFromAgent)
    {

        String url = String.format("%s%s/%s", combRpcSerice.getVmService().getL3IpPort(), networkAgentConfig.getSgUrl(), sgIdFromAgent);
        String result = HttpActionUtil.get(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.info("get response of security group status error, sgIdFromAgent: {}", sgIdFromAgent);
            return null;
        }
        String status = (String) resultMap.get("status");
        if ("ok".equals(status))
        {
            return (String)resultMap.get("phase");
        }
        else if (AgentConstant.FAILED.equals(status))
        {
            return AgentConstant.FAILED;
        }
        return null;
    }

    private  String vmBondSecurityGroup(String portIdFromAgent, List<String> sgIds)
    {
        if (StrUtil.isBlank(portIdFromAgent))
        {
            throw  new WebSystemException(ErrorCode.BAD_REQUST, ErrorLevel.INFO);
        }

        try
        {
            return vmBondSecurityGroupFromAgent(portIdFromAgent,sgIds);
        }
        catch (Exception e)
        {
            log.error("vmBondSecurityGroup error:{}, portIdFromAgent:{}, sgIds:{}",e.getMessage(),portIdFromAgent,sgIds);
            return null;
        }
    }


    private  String vmBondSecurityGroupFromAgent( String portIdFromAgent,List<String> sgIds)
    {

//        String url = String.format("%s%s/%s", combRpcSerice.getVmService().getL3IpPort() , networkAgentConfig.getSgUrl(),sgIdFromAgent);
        String url = String.format("%s%s/%s/apply", combRpcSerice.getVmService().getL3IpPort(), networkAgentConfig.getPortUrl(),portIdFromAgent);
        SecurityGroups securityGroups = new SecurityGroups();
        securityGroups.setSgs(sgIds);
        String jsonStr = JsonUtil.objectToJson(securityGroups);
        BaseRsp result = HttpActionUtil.put(url,jsonStr, BaseRsp.class);
        if (null == result)
        {
            log.info("get response of bonding security group error, sgIds: {},portIdFromAgent: {}", sgIds, portIdFromAgent);
            return null;
        }
        if (Objects.equals(result.getStatus(),AgentConstant.OK))
        {
            return portIdFromAgent;
        }
        return null;
    }

    private int getPortBoundSecurityGroupStatus(String portIdFromAgent)
    {
        try
        {
            return getPortBoundSecurityGroupStatusFromAgent(portIdFromAgent);
        }
        catch (Exception e)
        {
            log.error("getPortBoundSecurityGroupStatus error:{}, portIdFromAgent:{}",e.getMessage(), portIdFromAgent);
            return PhaseStatus.APPLY_FAILED;
        }
    }

    private int getPortBoundSecurityGroupStatusFromAgent( String portIdFromAgent)
    {
        String url = String.format("%s%s/%s",combRpcSerice.getVmService().getL3IpPort(),networkAgentConfig.getPortUrl(),portIdFromAgent);
        String result = HttpActionUtil.get(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.info("get response of security group status error, portIdFromAgent: {}", portIdFromAgent);
            return PhaseStatus.APPLY_FAILED;
        }
        String status = (String) resultMap.get("status");
        if ("ok".equals(status))
        {
            String sgPhaseStatus =  (String)resultMap.get("apply_phase");
            switch (sgPhaseStatus)
            {
                case AgentConstant.APPLIED:
                    return PhaseStatus.APPLIED;
                case AgentConstant.APPLY_FAILED:
                    return PhaseStatus.APPLY_FAILED;
                case AgentConstant.APPLYING:
                    return PhaseStatus.APPLYING;
            }
        }
        return PhaseStatus.APPLY_FAILED;
    }



    private List<String> getRules(String sgId) throws WebSystemException
    {
        LambdaQueryWrapper<SecurityGroupRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SecurityGroupRule::getSgId, sgId)
                .ne(SecurityGroupRule::getPhaseStatus, REMOVED);

        List<SecurityGroupRule> tblSecurityGroupRules = securityGroupRuleService.list(queryWrapper);
        List<String> rules = new ArrayList<>();
        for (SecurityGroupRule tblSecurityGroupRule:tblSecurityGroupRules)
        {
            String rule = getRule(tblSecurityGroupRule);
            if (null == rule) throw  new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            rules.add(rule);
        }
        return rules;
//        return tblSecurityGroupRules.stream().map(
//                tblSecurityGroupRule -> getRule(tblSecurityGroupRule,vcpIdFromAgent)
//        ).collect(Collectors.toList());
    }

    private String getRule(SecurityGroupRule tblSecurityGroupRule)
    {
        String address = null;
        if(null != tblSecurityGroupRule.getCidr() && !tblSecurityGroupRule.getCidr().isEmpty())
        {
            address = tblSecurityGroupRule.getCidr();
        }
        else if (null != tblSecurityGroupRule.getSgIdReference() && !tblSecurityGroupRule.getSgIdReference().isEmpty())
        {
            if (Objects.equals(tblSecurityGroupRule.getSgIdReference(), tblSecurityGroupRule.getSgId()))
            {
                address = "default";
            }
            else
            {
                address = securityGroupService.getById(tblSecurityGroupRule.getSgIdReference()).getSgIdFromAgent();
            }
        }
        else
        {
            address = AgentConstant.ALL_IP;
        }
        String port = tblSecurityGroupRule.getPort();
        if (Protocols.ICMP == tblSecurityGroupRule.getProtocol())
        {
            List<String> portList = Arrays.stream(tblSecurityGroupRule.getPort().split(",")).map(key ->
                icmpPortMap.get(key)).collect(Collectors.toList());
            port = String.join("+",portList);
        }
        // port 把,号全部替换成+号
        port = StrUtil.replace(port,",","+");
        //TODO ip pool
        return String.format("dir:%s,priority:%d,protocol:%s,port:%s,addr:%s,action:%s",
                directionMap.get(tblSecurityGroupRule.getDirection()),tblSecurityGroupRule.getPriority(),
                protocolMap.get(tblSecurityGroupRule.getProtocol()),port,address,actionMap.get(tblSecurityGroupRule.getAction()));

    }


    //update sg rules(add/delete/update)
    public void processUpdateSecurityGroup(SecurityGroup tblSecurityGroup)
    {

        String sgId = tblSecurityGroup.getSgId();
        if (null == tblSecurityGroup.getSgIdFromAgent())
        {
            String sgIdFromAgent = createSecurityGroupFromAgent(sgId);
            tblSecurityGroup.setSgIdFromAgent(sgIdFromAgent);
            tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = securityGroupService.updateById(tblSecurityGroup);
            if (!ok)
            {
                log.info("processUpdateSecurityGroup, update sgIdFromAgent error, sgId:{}",sgId);
                return;
            }
        }
        List<String> rules = null;
        try
        {
            rules = getRules(sgId);
        }
        catch (WebSystemException e)
        {
            log.error("get rules error: sgId {}", tblSecurityGroup.getSgId());
        }
        log.info("update security group :{}, rules:{} ",tblSecurityGroup.getSgId(),rules);
        String sgIdFromAgent = updateSGRules(tblSecurityGroup.getSgIdFromAgent(), rules);
        if (null != sgIdFromAgent)
        {
            tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATED);
            tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            securityGroupService.updateById(tblSecurityGroup);
            logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(),"Agent 正在更新安全组",
                    String.format("请求参数: sgId:%s rules:%s",tblSecurityGroup.getSgId(), rules),"更新完成");
        }
    }

    public void processRemoveSecurityGroup(SecurityGroup tblSecurityGroup) throws WebSystemException
    {
        if (StrUtil.isBlank(tblSecurityGroup.getSgIdFromAgent()))
        {
            tblSecurityGroup.setPhaseStatus(REMOVED);
            boolean ok = securityGroupService.updateById(tblSecurityGroup);
            if (!ok)
            {
                log.info("processRemoveSecurityGroup, remove tblSecurityGroup error, sgId:{}",tblSecurityGroup.getSgId());
                return;
            }
            logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(),"Agent 正在删除安全组",
                    String.format("请求参数: %s", tblSecurityGroup.getSgId()),"删除成功");
        }
        String sgIdFromAgent = removeSecurityGroupFromAgent(tblSecurityGroup.getSgIdFromAgent());
        if (null == sgIdFromAgent)
        {
            log.info("removeSecurityGroupFromAgent error, sgId:{}", tblSecurityGroup.getSgId());
            throw new WebSystemException(ErrorCode.SystemError,ErrorLevel.INFO);
        }
        tblSecurityGroup.setPhaseStatus(REMOVED);
        tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        securityGroupService.updateById(tblSecurityGroup);
        LambdaUpdateWrapper<SgVmInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SgVmInstance::getSgId, tblSecurityGroup.getSgId())
                .ne(SgVmInstance::getPhaseStatus, REMOVED);
        if(sgVmInstanceService.count(updateWrapper) > 0)
        {
            log.info("sgVmInstanceService.count > 0, sgId:{}", tblSecurityGroup.getSgId());
            SgVmInstance tblSgVmInstance = new SgVmInstance();
            tblSgVmInstance.setPhaseStatus(PhaseStatus.DELETING);
            tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            sgVmInstanceService.update(tblSgVmInstance, updateWrapper);
            logRpcService.getLogService().addEvent(tblSecurityGroup.getUserId(),"Agent 正在删除安全组",
                    String.format("请求参数: %s", tblSecurityGroup.getSgId()),"删除完成");
        }

    }

    private List<SgVmInstance> getNotUnAppliedSgVmInstances(String vmInstanceId)
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                .ne(SgVmInstance::getPhaseStatus, PhaseStatus.UNAPPLIED)
                .ne(SgVmInstance::getPhaseStatus, REMOVED)
                .orderByDesc(SgVmInstance::getUpdateTime);

        return sgVmInstanceService.list(queryWrapper);

//        criteria.andPhaseStatusNotEqualTo(REMOVED);
//        criteria.andSgIdEqualTo(sgId);
    }

    private List<String> getUpdateSecurityGroupIds()
    {
        LambdaQueryWrapper<SecurityGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SecurityGroup::getPhaseStatus, PhaseStatus.UPDATING);
        List<SecurityGroup> tblSecurityGroups = securityGroupService.list(queryWrapper);

        List<String> agentNeedUpdateSgs = new ArrayList<>();
        for(SecurityGroup tblSecurityGroup: tblSecurityGroups)
        {
            if (isSgBondPhase(tblSecurityGroup.getSgId()))
            {
                agentNeedUpdateSgs.add(tblSecurityGroup.getSgId());
            }
            else
            {
                log.info("update security group: {} ,change from UPDATING to: UPDATED",tblSecurityGroup.getSgId());
                tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATED);
                boolean ok = securityGroupService.updateById(tblSecurityGroup);
                if (!ok)
                {
                    log.error("update security group error");
                }
            }
        }

        return agentNeedUpdateSgs;
    }

    private boolean isSgBondPhase(String sgId)
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getSgId, sgId)
                .ne(SgVmInstance::getPhaseStatus, REMOVED)
                .ne(SgVmInstance::getPhaseStatus, PhaseStatus.UNAPPLIED);
        return sgVmInstanceService.count(queryWrapper) > 1;
    }


    @Data
    final static class Rules
    {
        List<String> rules;
    }

    @Data
    final static class SecurityGroups
    {
        List<String> sgs;
    }

}
