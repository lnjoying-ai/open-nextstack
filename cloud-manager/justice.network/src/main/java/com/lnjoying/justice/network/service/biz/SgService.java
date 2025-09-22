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

package com.lnjoying.justice.network.service.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lnjoying.justice.commonweb.biz.LogRpcSerice;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.common.Protocols;
import com.lnjoying.justice.network.domain.dto.request.CommonReq;
import com.lnjoying.justice.network.domain.dto.request.SecurityGroupCreateReqVo;
import com.lnjoying.justice.network.domain.dto.request.SgRuleCreateUpdateReqVo;
import com.lnjoying.justice.network.domain.dto.response.*;
import com.lnjoying.justice.network.entity.SecurityGroup;
import com.lnjoying.justice.network.entity.SecurityGroupRule;
import com.lnjoying.justice.network.entity.SgVmInstance;
import com.lnjoying.justice.network.entity.search.SecurityGroupSearchCritical;
import com.lnjoying.justice.network.service.PortService;
import com.lnjoying.justice.network.service.SecurityGroupRuleService;
import com.lnjoying.justice.network.service.SecurityGroupService;
import com.lnjoying.justice.network.service.SgVmInstanceService;
import com.lnjoying.justice.network.utils.NetworkUtils;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service("securityGroupService")
@Slf4j
public class SgService
{
    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private SecurityGroupService securityGroupService;

    @Autowired
    private SecurityGroupRuleService securityGroupRuleService;
    
    @Autowired
    private SgVmInstanceService sgVmInstanceService;
    
    @Autowired
    private PortService portService;

    @Autowired
    private LogRpcSerice logRpcSerice;


    //security group
    public SecurityGroupsRspVo getSecurityGroups(SecurityGroupSearchCritical securityGroupSearchCritical, String userId)
    {
            String sgId = securityGroupSearchCritical.getSgId();
            String name = securityGroupSearchCritical.getName();
            long totalNum = securityGroupService.countSecurityGroupBySearch(userId, name, sgId, REMOVED, false);
            SecurityGroupsRspVo getSecurityGroupsRsp = new SecurityGroupsRspVo();
            getSecurityGroupsRsp.setTotalNum(totalNum);
            if (totalNum < 1)
            {
                return getSecurityGroupsRsp;
            }

            int pageSize = securityGroupSearchCritical.getPageSize();
            int pageNum = securityGroupSearchCritical.getPageNum();
            //query with page number and page size
            int begin = ((pageNum - 1) * pageSize);

            List<SecurityGroupRspVo> securityGroups = securityGroupService.getSecurityGroups(userId, name, sgId, REMOVED, false, pageSize, begin);
            getSecurityGroupsRsp.setSecurityGroups(securityGroups);

            return getSecurityGroupsRsp;
    }

    //security group detail info
    @Transactional(rollbackFor = Exception.class)
    public SecurityGroupDetailInfoRspVo getSecurityGroup(String sgId, String userId)
    {
        SecurityGroup tblSecurityGroup = securityGroupService.getById(sgId);
        if (null == tblSecurityGroup || REMOVED == tblSecurityGroup.getPhaseStatus())
        {
            log.info("security group  not exists, sgId: {}", sgId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (null == userId)
        {
            log.info("user admin, get securityGroupId: {}",tblSecurityGroup.getSgId());
        }
        else if (checkUserIdSgIdInValid(userId,sgId))
        {
            log.info("check userId and sgId error, userId:{}, sgId:{}", userId, sgId);
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        SecurityGroupDetailInfoRspVo getSecurityGroupDetailInfoRsp = new SecurityGroupDetailInfoRspVo();
        getSecurityGroupDetailInfoRsp.setName(tblSecurityGroup.getName());
        getSecurityGroupDetailInfoRsp.setDescription(tblSecurityGroup.getDescription());
        getSecurityGroupDetailInfoRsp.setSgId(tblSecurityGroup.getSgId());
        LambdaQueryWrapper<SecurityGroupRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SecurityGroupRule::getSgId,sgId)
                .ne(SecurityGroupRule::getPhaseStatus, PhaseStatus.UNAPPLIED)
                .ne(SecurityGroupRule::getPhaseStatus, REMOVED);
        List<SecurityGroupRule> sgRules = securityGroupRuleService.list(queryWrapper);
//            long totalNum = networkRepository.countSecurityGroupRuleByExample(example);
//            if (totalNum < 1)
//            {
//                return getSecurityGroupDetailInfoRsp;
//            }
        if (sgRules.size() > 0)
        {
            List<SecurityGroupRuleRspVo> securityGroupRules = sgRules.stream().map(tblSecurityGroupRule ->
            {
                SecurityGroupRuleRspVo securityGroupRule = new SecurityGroupRuleRspVo();
                securityGroupRule.setSecurityGroupRule(tblSecurityGroupRule);
                return securityGroupRule;
            }).collect(Collectors.toList());
            getSecurityGroupDetailInfoRsp.setRules(securityGroupRules);
        }
        // vm instances
        LambdaQueryWrapper<SgVmInstance> sgVmInstanceQueryWrapper = new LambdaQueryWrapper<>();
        sgVmInstanceQueryWrapper.select(SgVmInstance::getInstanceId)
                .eq(SgVmInstance::getSgId, sgId)
                .ne(SgVmInstance::getPhaseStatus, PhaseStatus.UNAPPLIED)
                .ne(SgVmInstance::getPhaseStatus, REMOVED);
//        List<SgVmInstance> tblSgVmInstances = sgVmInstanceService.list(sgVmInstanceQueryWrapper);
        List<String> instanceIds = sgVmInstanceService.listObjs(sgVmInstanceQueryWrapper,Object::toString);       
//        List<String> instanceIds = tblSgVmInstances.stream().map(TblSgVmInstance::getInstanceId).collect(Collectors.toList());
        List<VmService.InstanceDetailInfo> vmInstanceDetailInfos = combRpcSerice.getVmService().getVmInstanceDetailInfos(instanceIds);
        vmInstanceDetailInfos.removeIf(Objects::isNull);
        getSecurityGroupDetailInfoRsp.setPhaseStatus(tblSecurityGroup.getPhaseStatus());
        getSecurityGroupDetailInfoRsp.setVmInstances(vmInstanceDetailInfos.stream().map(
                vmInstanceDetailInfo->
                {
                    if (null == vmInstanceDetailInfo)
                    {
                        return null;
                    }
                    SecurityGroupDetailInfoRspVo.VmInstance vmInstance = new SecurityGroupDetailInfoRspVo.VmInstance();
                    vmInstance.setName(vmInstanceDetailInfo.getName());
                    if (StrUtil.isBlank(vmInstanceDetailInfo.getPortId()) || portService.getById(vmInstanceDetailInfo.getPortId()) == null)
                    {
                        vmInstance.setIp(null);
                    }
                    else
                    {
                        vmInstance.setIp(portService.getById(vmInstanceDetailInfo.getPortId()).getIpAddress());
                    }
                    vmInstance.setInstanceId(vmInstanceDetailInfo.getInstanceId());
                    vmInstance.setFlavorName(vmInstanceDetailInfo.getFlavorName());
                    vmInstance.setPhaseStatus(vmInstanceDetailInfo.getPhaseStatus());
                    return vmInstance;
                }
        ).collect(Collectors.toList()));
        return getSecurityGroupDetailInfoRsp;
    }

    //create a new security group
    @Transactional(rollbackFor = Exception.class)
    public SecurityGroupBaseRspVo createSecurityGroup(SecurityGroupCreateReqVo req, String userId) throws WebSystemException
    {
        SecurityGroup tblSecurityGroup = new SecurityGroup();
        tblSecurityGroup.setSgId(Utils.assignUUId());
        if(AgentConstant.DEFAULT.equals(req.getName()))
        {
            LambdaQueryWrapper<SecurityGroup> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SecurityGroup::getName, AgentConstant.DEFAULT)
                    .eq(SecurityGroup::getUserId, userId)
                    .ne(SecurityGroup::getPhaseStatus, REMOVED);
            if (securityGroupService.count(queryWrapper) > 0)
            {
                log.info("can't use `default` as a security group name");
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
        }
        tblSecurityGroup.setName(req.getName());
        tblSecurityGroup.setDescription(req.getDescription());
        tblSecurityGroup.setPhaseStatus(PhaseStatus.ADDING);
        tblSecurityGroup.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblSecurityGroup.setUpdateTime(tblSecurityGroup.getCreateTime());
        tblSecurityGroup.setUserId(userId);
        boolean ok = securityGroupService.save(tblSecurityGroup);
        if (!ok)
        {
            log.info("create security group error :{} ", req);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return SecurityGroupBaseRspVo.builder().sgId(tblSecurityGroup.getSgId()).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDefaultSg(String userId) throws WebSystemException
    {
        LambdaQueryWrapper<SecurityGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SecurityGroup::getUserId, userId)
                .eq(SecurityGroup::getName, AgentConstant.DEFAULT)
                .ne(SecurityGroup::getPhaseStatus, REMOVED);
        if (securityGroupService.count(queryWrapper)>0)
        {
            return ;
        }
        log.info("create default security group, userId:{}",userId);
        SecurityGroupCreateReqVo defaultSg = new SecurityGroupCreateReqVo();
        defaultSg.setName(AgentConstant.DEFAULT);
        defaultSg.setDescription("default security group");
        SecurityGroupBaseRspVo baseRsp = createSecurityGroup(defaultSg,userId);

        // inbound rule
        SgRuleCreateUpdateReqVo defaultInRule = new SgRuleCreateUpdateReqVo();
        SgRuleCreateUpdateReqVo.AddressesRef addressesRef = new SgRuleCreateUpdateReqVo.AddressesRef();
        addressesRef.setSgId(baseRsp.getSgId());

        defaultInRule.setDirection(AgentConstant.IN);
        defaultInRule.setPriority(AgentConstant.MAX_PRIORITY);
        defaultInRule.setAddressType(AgentConstant.IPV4);
        defaultInRule.setName("default inbound rule");
        defaultInRule.setAddressRef(addressesRef);
        defaultInRule.setPort("");
        defaultInRule.setDescription("the same security group traffic is allowed");
        defaultInRule.setProtocol(Protocols.IP);
        defaultInRule.setAction(AgentConstant.ACCEPT);

        createSgRule(defaultInRule, baseRsp.getSgId(), userId);

        addressesRef.setSgId(null);
        addressesRef.setCidr(AgentConstant.ALL_IP);
        defaultInRule.setPort("22,3389");
        defaultInRule.setProtocol(Protocols.TCP);
        defaultInRule.setName("remote connection rules");
        defaultInRule.setDescription("ssh service and windows remote desktop service");
        defaultInRule.setPriority(AgentConstant.MIN_PRIORITY);
        defaultInRule.setAddressRef(addressesRef);

        createSgRule(defaultInRule, baseRsp.getSgId(), userId);

        // outbound rule
        SgRuleCreateUpdateReqVo defaultOutRule = new SgRuleCreateUpdateReqVo();
        addressesRef.setCidr(AgentConstant.ALL_IP);
        addressesRef.setSgId(null);
        addressesRef.setIpPoolId(null);

        defaultOutRule.setDirection(AgentConstant.OUT);
        defaultOutRule.setPriority(AgentConstant.MAX_PRIORITY);
        defaultOutRule.setAddressType(AgentConstant.IPV4);
        defaultOutRule.setName("default outbound rule");
        defaultOutRule.setAddressRef(addressesRef);
        defaultOutRule.setPort("");
        defaultOutRule.setDescription("all traffic is allowed");
        defaultOutRule.setProtocol(Protocols.IP);
        defaultOutRule.setAction(AgentConstant.ACCEPT);

        createSgRule(defaultOutRule, baseRsp.getSgId(),userId);
    }

    public SecurityGroupBaseRspVo updateSecurityGroup(String sgId, String userId, CommonReq commonReq) throws WebSystemException
    {
        SecurityGroup tblSecurityGroup = securityGroupService.getById(sgId);
        if (null == tblSecurityGroup || REMOVED == tblSecurityGroup.getPhaseStatus())
        {
            log.info("security group does not exist, sgId:{}", sgId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (AgentConstant.DEFAULT.equals(tblSecurityGroup.getName()))
        {
            log.info("default security group can't update");
            throw new WebSystemException(ErrorCode.DEFAULT_SECURITY_GROUP_NOT_UPDATE,ErrorLevel.INFO);
        }

        if (checkUserIdSgIdInValid(userId, sgId))
        {
            log.info("security group exists, but user not exists, sgId: {}, userId: {}", sgId, userId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (PhaseStatus.AGENT_DELETING == tblSecurityGroup.getPhaseStatus() ||
        PhaseStatus.DELETING == tblSecurityGroup.getPhaseStatus())
        {
            log.info("security group is deleting, sgId: {}", sgId);
            throw new WebSystemException(ErrorCode.OBJECT_IS_DELETING, ErrorLevel.INFO);
        }
        tblSecurityGroup.setName(commonReq.getName());
        tblSecurityGroup.setDescription(commonReq.getDescription());
        tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = securityGroupService.updateById(tblSecurityGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return SecurityGroupBaseRspVo.builder().sgId(tblSecurityGroup.getSgId()).build();

    }

    //remove security group
    @Transactional(rollbackFor = Exception.class)
    public SecurityGroupBaseRspVo removeSecurityGroup(String sgId, String userId) throws WebSystemException
    {
        SecurityGroup tblSecurityGroup = securityGroupService.getById(sgId);
        if (null == tblSecurityGroup || REMOVED == tblSecurityGroup.getPhaseStatus())
        {
            log.info("security group  not exists, sgId: {}", sgId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (AgentConstant.DEFAULT.equals(tblSecurityGroup.getName()))
        {
            log.info("default security group can't remove");
            throw new WebSystemException(ErrorCode.DEFAULT_SECURITY_GROUP_NOT_REMOVE,ErrorLevel.INFO);
        }

        if (checkUserIdSgIdInValid(userId, sgId))
        {
            log.info("security group exists, but user not exists, sgId: {}, userId: {}", sgId, userId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }

        // get bounded vm instances
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getSgId,sgId)
                .ne(SgVmInstance::getPhaseStatus,PhaseStatus.UNAPPLIED)
                .ne(SgVmInstance::getPhaseStatus,REMOVED);
        long totalNum = sgVmInstanceService.count(queryWrapper);

        if (totalNum > 0 )
        {
            log.info("vm instance exists with the security group ,can't remove, sgId: {}, userId: {}", sgId, userId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_IS_BOUND, ErrorLevel.INFO);
        }

        // get sg_id_reference
        LambdaQueryWrapper<SecurityGroupRule> ruleQueryWrapper = new LambdaQueryWrapper<>();
        ruleQueryWrapper.ne(SecurityGroupRule::getSgId, sgId)
                .eq(SecurityGroupRule::getSgIdReference, sgId)
                .ne(SecurityGroupRule::getPhaseStatus, REMOVED);
        totalNum = securityGroupRuleService.count(ruleQueryWrapper);
        if (totalNum > 0)
        {
            log.info("the security group is used by other security group rules,  sgId {}", sgId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_USED_BY_RULE,ErrorLevel.INFO);
        }

        tblSecurityGroup.setPhaseStatus(PhaseStatus.DELETING);
        tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = securityGroupService.updateById(tblSecurityGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return SecurityGroupBaseRspVo.builder().sgId(tblSecurityGroup.getSgId()).build();
    }

    // create a new security group rule
    @Transactional(rollbackFor = Exception.class)
    public SgRuleBaseRspVo createSgRule(SgRuleCreateUpdateReqVo createSgRuleReq, String sgId, String userId) throws WebSystemException
    {
        SecurityGroupRule tblSecurityGroupRule = createOrUpdateSgRule(createSgRuleReq,sgId,null,userId);
        tblSecurityGroupRule.setPhaseStatus(PhaseStatus.ADDED);
        boolean ok = securityGroupRuleService.save(tblSecurityGroupRule);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        SecurityGroup tblSecurityGroup = securityGroupService.getById(tblSecurityGroupRule.getSgId());
        tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATING);
        ok = securityGroupService.updateById(tblSecurityGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }


        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(userId).getUserName();
        String directionStr = createSgRuleReq.getDirection() == 0 ? "入方向规则" : "出方向规则";
        String actionStr = createSgRuleReq.getAction() == 1 ? "允许" : "拒绝";
        String desc = StrUtil.format("添加安全规则【方向：{}，优先级：{}，策略：{}，协议端口：{}】", directionStr, createSgRuleReq.getPriority(), actionStr, createSgRuleReq.getPort());
        logRpcSerice.getLogService().addLog(userId, userName, "网络-安全组", desc);

        return SgRuleBaseRspVo.builder().ruleId(tblSecurityGroupRule.getRuleId()).build();
    }

    //update rule
    @Transactional(rollbackFor = Exception.class)
    public SgRuleBaseRspVo updateSgRule(SgRuleCreateUpdateReqVo updateSgRuleReq, String sgId, String sgRuleId, String userId) throws WebSystemException
    {
        SecurityGroupRule tblSecurityGroupRule = createOrUpdateSgRule(updateSgRuleReq,sgId,sgRuleId,userId);
        boolean ok = securityGroupRuleService.updateById(tblSecurityGroupRule);
        if (!ok)
        {
            log.error("update security group rule error, sgRuleId:{}, userId:{}",sgRuleId, userId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        SecurityGroup tblSecurityGroup = securityGroupService.getById(tblSecurityGroupRule.getSgId());
        tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATING);
        tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        ok = securityGroupService.updateById(tblSecurityGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        //
        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(userId).getUserName();
        String directionStr = updateSgRuleReq.getDirection() == 0 ? "入方向规则" : "出方向规则";
        String desc = StrUtil.format("编辑安全规则【名称：{}，方向：{}，优先级：{}，策略：{}，协议端口：{}】", updateSgRuleReq.getName(), directionStr, updateSgRuleReq.getPriority(), updateSgRuleReq.getDirection(), updateSgRuleReq.getPort());
        logRpcSerice.getLogService().addLog(userId, userName, "网络-安全组", desc);


        return SgRuleBaseRspVo.builder().ruleId(tblSecurityGroupRule.getRuleId()).build();
    }

    //remove rule
    @Transactional(rollbackFor = Exception.class)
    public SgRuleBaseRspVo removeSgRule( String sgRuleId, String userId) throws WebSystemException
    {

        SecurityGroupRule tblSecurityGroupRule = securityGroupRuleService.getById(sgRuleId);
        if (null == tblSecurityGroupRule || REMOVED == tblSecurityGroupRule.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_RULE_NOT_EXISTS, ErrorLevel.INFO);
        }

        if (checkUserIdSgIdInValid(userId, tblSecurityGroupRule.getSgId()))
        {
            log.info("check userId and sgId error, userId:{}, sgId:{}", userId, tblSecurityGroupRule.getSgId());
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        tblSecurityGroupRule.setPhaseStatus(REMOVED);
        tblSecurityGroupRule.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//            int count = networkRepository.deleteSecurityGroupRule(sgRuleId);
        boolean ok = securityGroupRuleService.updateById(tblSecurityGroupRule);
        if (!ok)
        {
            log.info("update security group rule error,sgRuleId:{}, userId:{}", sgRuleId, userId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        SecurityGroup tblSecurityGroup = securityGroupService.getById(tblSecurityGroupRule.getSgId());
        if (null == tblSecurityGroup || REMOVED == tblSecurityGroup.getPhaseStatus())
        {
           log.info("security group not exists, sgId:{}",tblSecurityGroupRule.getSgId());
           throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (PhaseStatus.DELETING == tblSecurityGroup.getPhaseStatus() || PhaseStatus.AGENT_DELETING == tblSecurityGroup.getPhaseStatus()
        )
        {
            log.info("security group is deleting, sgId: {}", tblSecurityGroupRule.getSgId());
            throw new WebSystemException(ErrorCode.OBJECT_IS_DELETING, ErrorLevel.INFO);
        }
        tblSecurityGroup.setPhaseStatus(PhaseStatus.UPDATING);
        tblSecurityGroup.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        ok = securityGroupService.updateById(tblSecurityGroup);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return SgRuleBaseRspVo.builder().ruleId(tblSecurityGroupRule.getRuleId()).build();
    }

    // sg bound vm instance
    @Transactional(rollbackFor = Exception.class)
    public BaseRsp vmInstanceBoundSg(String vmInstanceId, String sgId, String userId) throws WebSystemException
    {
        checkSgIdInstanceIdExists(userId, sgId, vmInstanceId);
        SgVmInstance tblSgVmInstance = new SgVmInstance();
        tblSgVmInstance.setPhaseStatus(PhaseStatus.APPLYING);
        tblSgVmInstance.setSgVmId(Utils.assignUUId());
        tblSgVmInstance.setInstanceId(vmInstanceId);
        tblSgVmInstance.setSgId(sgId);
        tblSgVmInstance.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblSgVmInstance.setUpdateTime(tblSgVmInstance.getCreateTime());
//            String vpcId = combRpcSerice.getComputeService().getVpcAndPortFromVmInstanceId(vmInstanceId).getVpcId();
//            tblSgVmInstance.setVpcIdFromAgent(networkRepository.getVpcById(vpcId).getVpcIdFromAgent());
        boolean ok = sgVmInstanceService.save(tblSgVmInstance);
        if (!ok)
        {
            log.info("create sgVmInstance error, sgId:{},vmInstanceId:{}, userId:{}", sgId,vmInstanceId, userId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return BaseRsp.builder().uuid(vmInstanceId).build();

    }

    //sg unbound vm instance
    @Transactional(rollbackFor = Exception.class)
    public BaseRsp vmInstanceUnBoundSg(String vmInstanceId, String sgId, String userId) throws WebSystemException
    {
        checkSgIdExists(userId, sgId);
        LambdaUpdateWrapper<SgVmInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SgVmInstance::getSgId, sgId)
                .eq(SgVmInstance::getInstanceId, vmInstanceId);
//        TblSgVmInstanceExample example = new TblSgVmInstanceExample();
//        TblSgVmInstanceExample.Criteria criteria = example.createCriteria();
//        criteria.andSgIdEqualTo(sgId);
//        criteria.andInstanceIdEqualTo(vmInstanceId);
        SgVmInstance tblSgVmInstance = new SgVmInstance();
        tblSgVmInstance.setPhaseStatus(PhaseStatus.UNAPPLIED);
        tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = sgVmInstanceService.update(tblSgVmInstance,updateWrapper);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return BaseRsp.builder().uuid(vmInstanceId).build();
    }


    private SecurityGroupRule createOrUpdateSgRule(SgRuleCreateUpdateReqVo createUpdateSgRuleReq, String sgId, String sgRuleId, String userId) throws WebSystemException
    {
        SecurityGroup tblSecurityGroup = securityGroupService.getById(sgId);
        if (null == tblSecurityGroup || REMOVED == tblSecurityGroup.getPhaseStatus())
        {
            log.info("security group  not exists, sgId: {}", sgId);
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }

        if (PhaseStatus.DELETING == tblSecurityGroup.getPhaseStatus() || PhaseStatus.AGENT_DELETING == tblSecurityGroup.getPhaseStatus())
        {
            log.info("security group is deleting, sgId: {}", sgId);
            throw new WebSystemException(ErrorCode.OBJECT_IS_DELETING, ErrorLevel.INFO);
        }

        if (checkUserIdSgIdInValid(userId, sgId))
        {
            log.info("check userId and sgId error, userId:{}, sgId:{}", userId, sgId);
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        checkCreateRuleParameters(createUpdateSgRuleReq);
        SecurityGroupRule tblSecurityGroupRule;
        if (null == sgRuleId)
        {
            tblSecurityGroupRule = new SecurityGroupRule();
            tblSecurityGroupRule.setRuleId(Utils.assignUUId());
            tblSecurityGroupRule.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblSecurityGroupRule.setUpdateTime(tblSecurityGroupRule.getCreateTime());
        }
        else
        {
            tblSecurityGroupRule = securityGroupRuleService.getById(sgRuleId);
            tblSecurityGroupRule.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        }
        SgRuleCreateUpdateReqVo.AddressesRef addressRef = createUpdateSgRuleReq.getAddressRef();
        // cidr;sgId;ipPool;
        if (!StrUtil.isBlank(addressRef.getCidr()) && (AgentConstant.ALL_IP.equals(addressRef.getCidr()) || NetworkUtils.isValidSubnetCidr(addressRef.getCidr())))
        {
            tblSecurityGroupRule.setCidr(addressRef.getCidr());
            tblSecurityGroupRule.setSgIdReference("");
        }
        else if (!StrUtil.isBlank(addressRef.getSgId()) )
        {
            SecurityGroup tblSg = securityGroupService.getById(addressRef.getSgId());
            if (null ==tblSg || REMOVED == tblSg.getPhaseStatus())
            {
                log.info("security group  not exists, sgId: {}", addressRef.getSgId());
                throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
            }
            tblSecurityGroupRule.setSgIdReference(addressRef.getSgId());
            tblSecurityGroupRule.setCidr("");
        }
//        else if (null != addressRef.getIpPoolId() && !addressRef.getIpPoolId().isEmpty() && null != networkRepository.getIpPoolById(addressRef.getIpPoolId()))
//        {
//            tblSecurityGroupRule.setPoolId(addressRef.getIpPoolId());
//        }
        else
        {
            throw new WebSystemException(ErrorCode.CIDR_OVERLAP, ErrorLevel.INFO);
        }
//        tblSecurityGroupRule.setRuleId(Utils.assignUUId());
        tblSecurityGroupRule.setSgId(sgId);
        tblSecurityGroupRule.setPriority(createUpdateSgRuleReq.getPriority());
        tblSecurityGroupRule.setProtocol(createUpdateSgRuleReq.getProtocol());
        tblSecurityGroupRule.setAddressType(createUpdateSgRuleReq.getAddressType());
//        String port = createUpdateSgRuleReq.getPort();
        tblSecurityGroupRule.setPort(createUpdateSgRuleReq.getPort());
        tblSecurityGroupRule.setDirection(createUpdateSgRuleReq.getDirection());
        tblSecurityGroupRule.setAction(createUpdateSgRuleReq.getAction());
        tblSecurityGroupRule.setDescription(createUpdateSgRuleReq.getDescription());

        return  tblSecurityGroupRule;
    }


    // check sgId and userId
    public boolean checkUserIdSgIdInValid(String userId,String sgId)
    {
        LambdaQueryWrapper<SecurityGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SecurityGroup::getSgId, sgId)
                .eq(SecurityGroup::getUserId, userId)
                .ne(SecurityGroup::getPhaseStatus, REMOVED);
        long totalNum = securityGroupService.count(queryWrapper);
        return totalNum < 1;
    }

    public void checkSgIdExists(String userId, String sgId)
    {
        SecurityGroup tblSecurityGroup = securityGroupService.getById(sgId);
        if (null == tblSecurityGroup || REMOVED == tblSecurityGroup.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.SECURITY_GROUP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblSecurityGroup.getUserId(), userId))
        {
            log.info("check userId and sgId error, userId:{}, sgId:{}", userId, tblSecurityGroup.getSgId());
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
    }

    public void checkSgIdInstanceIdExists(String userId,String sgId, String instanceId) throws WebSystemException
    {
        checkSgIdExists(userId, sgId);
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getSgId, sgId)
                .eq(SgVmInstance::getInstanceId, instanceId)
                .ne(SgVmInstance::getPhaseStatus, REMOVED);
        long totalNum = sgVmInstanceService.count(queryWrapper);
        if (totalNum > 0)
        {
            throw new WebSystemException(ErrorCode.SG_INSTANCE_EXISTS, ErrorLevel.INFO);
        }
    }

    // check rule parameters
    public void checkCreateRuleParameters(SgRuleCreateUpdateReqVo req) throws WebSystemException
    {
        checkIntParameter(req.getPriority(),AgentConstant.MIN_PRIORITY, AgentConstant.MAX_PRIORITY);
        checkIntParameter(req.getDirection(),0,1);
        checkIntParameter(req.getAddressType(),0,1);
        checkIntParameter(req.getAction(),0,1);
        checkIntParameter(req.getProtocol(),Protocols.MIN,Protocols.MAX);
        //
        if(req.getPort().isEmpty() || "0".equals(req.getPort()) || "all".equals(req.getPort()))
        {
            req.setPort("all");
        }
        else
        {
            String rangePattern = "(\\d)+-(\\d)+";
            String numberPattern = "(\\d)+";
            String[] portList = req.getPort().split(",");
            for (String port : portList)
            {
                if (Pattern.matches(numberPattern, port.trim()))
                {
                    int portInt = Integer.parseInt(port.trim());
                    if ( portInt > 65535 || portInt < 1)
                    {
                        throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
                    }
                }
                else  if (Pattern.matches(rangePattern, port.trim()))
                {
                    List<String> nums = Arrays.asList(port.trim().split("-"));
                    if (Integer.parseInt(nums.get(0)) < 1 || Integer.parseInt(nums.get(1)) > 65535)

//                    Matcher matcher = Pattern.compile(rangePattern).matcher(port.trim());
//                    if (Integer.parseInt(matcher.group(1)) < 1 || Integer.parseInt(matcher.group(2)) > 65535)
                    {
                        throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
                    }
                }
            }
        }

    }

    private void checkIntParameter(int parameter,int start, int end)
    {
        if (parameter<start || parameter>end)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
    }
}
