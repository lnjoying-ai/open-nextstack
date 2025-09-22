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

package com.lnjoying.justice.network.rpcserviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.entity.network.SecurityGroupRule;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.justice.network.common.AgentConstant;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.common.PortType;
import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.request.MacInfo;
import com.lnjoying.justice.network.domain.backend.request.MergePortFromAgentReq;
import com.lnjoying.justice.network.domain.backend.response.SwitchGetFromAgentRsp;
import com.lnjoying.justice.network.domain.backend.response.SwitchesGetFromAgentRsp;
import com.lnjoying.justice.network.entity.*;
import com.lnjoying.justice.network.service.*;
import com.lnjoying.justice.network.service.biz.CombRpcSerice;
import com.lnjoying.justice.network.service.timer.VpcTimerService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.utils.JsonUtil;
import com.micro.core.utils.UnderlineAndHump;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@RpcSchema(schemaId = "networkService")
@Slf4j
public class NetworkServiceImpl implements NetworkService
{

    @Autowired
    NetworkAgentConfig networkAgentConfig;

    @Autowired
    VpcService vpcService;

    @Autowired
    SubnetService subnetService;

    @Autowired
    EipService eipService;

    @Autowired
    PortService portService;

    @Autowired
    SgVmInstanceService sgVmInstanceService;

    @Autowired
    SecurityGroupService securityGroupService;

    @Autowired
    SecurityGroupRuleService securityGroupRuleService;

    @Autowired
    EipMapService eipMapService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    com.lnjoying.justice.network.service.biz.NetworkService bizNetworkService;

//    private static final Logger log = LoggerFactory.getLogger(NetworkServiceImpl.class);
//    private static final ConcurrentHashMap<String,String> switchMap = new ConcurrentHashMap<>();

    @Override
    public Vpc getVpc(@ApiParam(name = "vpcId") String vpcId) {
//        TblRsVpc RsVpc = networkRepository.getVpcById(vpcId);
        com.lnjoying.justice.network.entity.Vpc tblVpc = vpcService.getById(vpcId);
//        vpcService.getOne(Wrappers.<Vpc>lambdaQuery().eq())
        if (null == tblVpc) {
            return null;
        }
        Vpc vpc = new Vpc();
        vpc.setAddressType(tblVpc.getAddressType());
        vpc.setCidr(tblVpc.getCidr());
        vpc.setCreateTime(tblVpc.getCreateTime());
        vpc.setUpdateTime(tblVpc.getUpdateTime());
        vpc.setName(tblVpc.getName());
        vpc.setVpcId(tblVpc.getVpcId());
        vpc.setPhaseStatus(tblVpc.getPhaseStatus());
        vpc.setPhaseInfo(tblVpc.getPhaseInfo());
        vpc.setUserId(tblVpc.getUserId());
        vpc.setVpcIdFromAgent(tblVpc.getVpcIdFromAgent());
        vpc.setVlanId(tblVpc.getVlanId());
        return vpc;
    }

    @Override
    public List<Vpc> getVpcs(@ApiParam(name = "vpcIds") List<String> vpcIds)
    {
        return vpcIds.stream().map(this::getVpc).collect(Collectors.toList());
    }

    @Override
    public Subnet getSubnet(@ApiParam(name = "subnetId") String subnetId) {
        com.lnjoying.justice.network.entity.Subnet tblSubnet = subnetService.getById(subnetId);
        if (null == tblSubnet) {
            return null;
        }
        Subnet subnet = new Subnet();
        subnet.setSubnetId(tblSubnet.getSubnetId());
        subnet.setSubnetIdFromAgent(tblSubnet.getSubnetIdFromAgent());
        subnet.setUserId(tblSubnet.getUserId());
        subnet.setVpcId(tblSubnet.getVpcId());
        subnet.setGatewayIp(tblSubnet.getGatewayIp());
        subnet.setCidr(tblSubnet.getCidr());
        subnet.setName(tblSubnet.getName());
        subnet.setPhaseStatus(tblSubnet.getPhaseStatus());
        subnet.setAddressType(tblSubnet.getAddressType());
        subnet.setPhaseInfo(tblSubnet.getPhaseInfo());
        subnet.setCreateTime(tblSubnet.getCreateTime());
        subnet.setUpdateTime(tblSubnet.getUpdateTime());
        return subnet;
    }

    @Override
    public List<Subnet> getSubnets(@ApiParam(name = "subnetIds") List<String> subnetIds)
    {
        return subnetIds.stream().map(this::getSubnet).collect(Collectors.toList());
    }

    @Override
    public DeployNetworkNic getDeployNetworkNic(@ApiParam(name = "nicIdFromAgent") String nicIdFromAgent) {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getNicUrl() + "/" + nicIdFromAgent;
        String result;
        DeployNetworkNic deployNetworkNic = new DeployNetworkNic();
        try {
            result = HttpActionUtil.get(url);
        } catch (Exception e) {
            log.error("getDeployNetworkNic error: {}", e.getMessage());
            return null;
        }
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap) {
            return null;
        }
        if ("failed".equals(resultMap.get("status"))) {
            return deployNetworkNic;
        }
        deployNetworkNic.setMacAddress((String) resultMap.get("mac"));
        deployNetworkNic.setPhaseStatus(PhaseStatus.getPhaseCode((String) resultMap.get("phase")));
        deployNetworkNic.setNicIdFromAgent(nicIdFromAgent);
        return  deployNetworkNic;
    }

    @Override
    public TenantNetworkPort getTenantNetworkPort(@ApiParam(name = "portId") String portId) {
        Port tblPort = portService.getById(portId);
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus() ) {
            return null;
        }
        TenantNetworkPort tenantNetworkPort = new TenantNetworkPort();
        tenantNetworkPort.setMacAddress(tblPort.getMacAddress());
//                arpingStatus 的状态为 failed、pending、ok
//                .arpingStatus((String) resultMap.get("arping"))
        tenantNetworkPort.setPortId(portId);
        if (null != tblPort.getOfPort())
        {
            tenantNetworkPort.setOfport(tblPort.getOfPort().toString());
        }
        tenantNetworkPort.setIpAddress(tblPort.getIpAddress());
        tenantNetworkPort.setPhaseStatus(tblPort.getPhaseStatus());
        com.lnjoying.justice.network.entity.Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
        tenantNetworkPort.setSubnetCidr(tblSubnet.getCidr());
        tenantNetworkPort.setSubnetName(tblSubnet.getName());
        com.lnjoying.justice.network.entity.Vpc tblVpc = vpcService.getById(tblSubnet.getVpcId());
        tenantNetworkPort.setVpcCidr(tblVpc.getCidr());
        tenantNetworkPort.setVlanId(tblVpc.getVlanId().toString());
        tenantNetworkPort.setVpcName(tblVpc.getName());
        tenantNetworkPort.setPortIdFromAgent(tblPort.getPortIdFromAgent());
         return  tenantNetworkPort;
    }

    @Override
    public List<TenantNetworkPort> getTenantNetworkPorts(@ApiParam(name = "portIds") List<String> portIds)
    {
        return portIds.stream().map(this::getTenantNetworkPort).collect(Collectors.toList());
    }

    @Override
    public String setDeployNetwork(@ApiParam(name = "mac") String mac) {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getNicUrl();
        MacInfo macInfo = new MacInfo(mac);
        String postString = JsonUtil.objectToJson(macInfo);
        String result;
        try {
            result = HttpActionUtil.post(url, postString);
        } catch (Exception e) {
            log.error("setDeployNetwork error: {}", e.getMessage());
            return null;
        }
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap) {
            return null;
        }

        return (String) resultMap.get("uuid");
    }

    @Override
    public String delFromDeployNetwork(@ApiParam(name = "nicIdFromAgent") String nicIdFromAgent) {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getNicUrl() + "/" + nicIdFromAgent;
        String result;
        try {
            result = HttpActionUtil.delete(url);
        } catch (Exception e) {
            log.error("delFromDeployNetwork error: {}", e.getMessage());
            return null;
        }
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap) {
            return null;
        }
        if (Objects.equals(resultMap.get("status"), "failed")) {
            return null;
        }
        return nicIdFromAgent;
    }

    @Override
    public List<NetworkDetailInfo> getBatchNetworkInfos(List<NetworkDetailInfoReq> networkInfoList) {
        return networkInfoList.stream().map(this::getNetworkDetailInfo).collect(Collectors.toList());
    }

    @Override
    public String setTenantNetwork(@ApiParam(name = "mac") String mac,
                                   @ApiParam(name = "subnetId") String subnetId,
                                   @ApiParam(name = "staticIp")String staticIp
                                   )
    {
        CreatePortReq createPortReq = new CreatePortReq();
        createPortReq.setContext(mac);
        createPortReq.setStaticIp(staticIp);
        createPortReq.setSubnetId(subnetId);
        return createPort(createPortReq,false, null);
    }

    @Override
    public String delPortFromTenantNetwork(String portId) {
//        TblRsPort port = networkRepository.getPortById(portId);
        Port tblPort = portService.getById(portId);
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus()){
            return null;
        }
        tblPort.setPhaseStatus(PhaseStatus.DELETING);
        tblPort.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = portService.updateById(tblPort);
//        int count = networkRepository.updatePort(port);
        if (!ok){
            log.info("failed to query portIdFromAgent {} from database", portId);
            return null;
        }
        return portId;
    }

    @Override
    public List<String> delPortsFromTenantNetwork(@ApiParam(name = "portIds") List<String> portIds)
    {
        return portIds.stream().map(this::delPortFromTenantNetwork).collect(Collectors.toList());
    }

    @Override
    public String notifyIpForPort(String portId, String ip) {
//        TblRsPort port = networkRepository.getPortById(portId);
        Port tblPort = portService.getById(portId);
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus()){
            return null;
        }
        if (ip != null && !ip.isEmpty()) {
            tblPort.setIpAddress(ip);
        }
        tblPort.setPhaseStatus(PhaseStatus.ARPING);
        tblPort.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = portService.updateById(tblPort);
        if (!ok){
            log.info("failed to query portIdFromAgent {} from database", portId);
            return null;
        }
        return portId;
    }

    private List<com.lnjoying.justice.network.entity.SecurityGroupRule> getSgRules(String sgId)
    {
        LambdaQueryWrapper<com.lnjoying.justice.network.entity.SecurityGroupRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(com.lnjoying.justice.network.entity.SecurityGroupRule::getSgId, sgId)
                .ne(com.lnjoying.justice.network.entity.SecurityGroupRule::getPhaseStatus, REMOVED);

        return securityGroupRuleService.list(queryWrapper);
    }

    @Override
    public  List<SgInfo> getSgInfos(@ApiParam(name = "instanceId") String instanceId)
    {
        if (null != instanceId && !instanceId.isEmpty())
        {
            LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SgVmInstance::getInstanceId, instanceId)
                    .ne(SgVmInstance::getPhaseStatus, REMOVED)
                    .ne(SgVmInstance::getPhaseStatus, PhaseStatus.UNAPPLIED)
                    .orderByDesc(SgVmInstance::getCreateTime);
            List<SgVmInstance> tblSgVmInstances = sgVmInstanceService.list(queryWrapper);
            List<SecurityGroup> tblSecurityGroups = tblSgVmInstances.stream().map(
                    tblSgVmInstance-> securityGroupService.getById(tblSgVmInstance.getSgId())).collect(Collectors.toList());

            return tblSecurityGroups.stream().map(
                    tblSecurityGroup -> {
                        SgInfo sgInfo = new SgInfo();
                        sgInfo.setDescription(tblSecurityGroup.getDescription());
                        sgInfo.setSgId(tblSecurityGroup.getSgId());
                        sgInfo.setSgName(tblSecurityGroup.getName());
                        List<com.lnjoying.justice.network.entity.SecurityGroupRule> rules = getSgRules(tblSecurityGroup.getSgId());
                        if (rules.size() > 0)
                        {
                            List<SecurityGroupRule> sgRules= rules.stream().map(
                                    tblSecurityGroupRule -> {
                                        SecurityGroupRule sgRule = new SecurityGroupRule();
                                        SecurityGroupRule.AddressesRef addressesRef = new SecurityGroupRule.AddressesRef();
                                        if (null != tblSecurityGroupRule.getCidr() && !tblSecurityGroupRule.getCidr().isEmpty())
                                        {
                                            addressesRef.setCidr(tblSecurityGroupRule.getCidr());
                                            sgRule.setAddressRef(addressesRef);
                                        }
                                        else if (null != tblSecurityGroupRule.getSgIdReference() && !tblSecurityGroupRule.getSgIdReference().isEmpty())
                                        {
                                            addressesRef.setSgId(tblSecurityGroupRule.getSgIdReference());
                                            sgRule.setAddressRef(addressesRef);
                                        }
                                        sgRule.setAction(tblSecurityGroupRule.getAction());
                                        sgRule.setRuleId(tblSecurityGroupRule.getRuleId());
                                        sgRule.setAddressType(tblSecurityGroupRule.getAddressType());
                                        sgRule.setDescription(tblSecurityGroupRule.getDescription());
                                        sgRule.setPort(tblSecurityGroupRule.getPort());
                                        sgRule.setPriority(tblSecurityGroupRule.getPriority());
                                        sgRule.setProtocol(tblSecurityGroupRule.getProtocol());
                                        sgRule.setDirection(tblSecurityGroupRule.getDirection());
                                        sgRule.setUpdateTime(Utils.formatDate(tblSecurityGroupRule.getUpdateTime()));
                                        sgRule.setCreateTime(Utils.formatDate(tblSecurityGroupRule.getCreateTime()));
                                        return sgRule;
                                    }
                            ).collect(Collectors.toList());
                            sgInfo.setRules(sgRules);
                        }
                        return sgInfo;
                    }).collect(Collectors.toList());
        }
        return null;
    }


    @Override
    public NetworkDetailInfo getNetworkDetailInfo(@ApiParam(name = "networkDetailInfoReq") NetworkDetailInfoReq networkDetailInfoReq){
        com.lnjoying.justice.network.entity.Vpc tblVpc = vpcService.getById(networkDetailInfoReq.getVpcId());
        com.lnjoying.justice.network.entity.Subnet tblSubnet = subnetService.getById(networkDetailInfoReq.getSubnetId());
        NetworkDetailInfo networkDetailInfo = new NetworkDetailInfo();
        Port tblPort = portService.getById(networkDetailInfoReq.getPortId());
        if (tblVpc != null && REMOVED != tblVpc.getPhaseStatus() ) {
            networkDetailInfo.setVpcName(tblVpc.getName());
            networkDetailInfo.setVpcId(tblVpc.getVpcId());
            networkDetailInfo.setVpcCidr(tblVpc.getCidr());
        }
        if (tblSubnet != null && REMOVED != tblSubnet.getPhaseStatus()) {
            networkDetailInfo.setSubnetId(tblSubnet.getSubnetId());
            networkDetailInfo.setSubnetCidr(tblSubnet.getCidr());
            networkDetailInfo.setSubnetName(tblSubnet.getName());
        }
        if (tblPort != null && REMOVED != tblPort.getPhaseStatus()) {
            networkDetailInfo.setPortId(tblPort.getPortId());
            networkDetailInfo.setIpAddress(tblPort.getIpAddress());
            networkDetailInfo.setIsVip(false);
            if (PortType.vip == tblPort.getType())
            {
                networkDetailInfo.setIsVip(true);
            }
            if (StringUtils.isNotBlank(tblPort.getEipId()))
            {
                com.lnjoying.justice.network.entity.Eip tblEip = eipService.getById(tblPort.getEipId());
                networkDetailInfo.setEipId(tblPort.getEipId());
                networkDetailInfo.setEip(tblEip.getIpaddr());
                networkDetailInfo.setPublicIp(tblEip.getPublicIp());
                networkDetailInfo.setBoundPhaseStatus(tblPort.getEipPhaseStatus());
                networkDetailInfo.setBoundType(AgentConstant.PORT_BOUND);
            }
            else
            {
                EipMap eipMap = getEipIdFromPortMap(tblPort.getPortId());
                if (null != eipMap)
                {
                    com.lnjoying.justice.network.entity.Eip tblEip = eipService.getById(eipMap.getEipId());
                    networkDetailInfo.setEipId(tblEip.getEipId());
                    networkDetailInfo.setEip(tblEip.getIpaddr());
                    networkDetailInfo.setPublicIp(tblEip.getPublicIp());
                    networkDetailInfo.setBoundPhaseStatus(eipMap.getStatus());
                    networkDetailInfo.setBoundType(AgentConstant.NAT_BOUND);
                }
            }
        }
        return networkDetailInfo;
    }

    EipMap getEipIdFromPortMap(String portId)
    {
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EipMap::getPortId, portId)
                .ne(EipMap::getStatus, REMOVED);
        if (eipMapService.count(queryWrapper) == 0)
        {
            return null;
        }
        return eipMapService.getOne(queryWrapper);
    }

    @Override
    public Boolean isIpInUse(@ApiParam(name = "subnetId") String subnetId,
                      @ApiParam(name = "ip") String ip)
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Port::getSubnetId, subnetId)
                .eq(Port::getIpAddress, ip)
                .ne(Port::getPhaseStatus, REMOVED);
        return portService.count(queryWrapper) > 0;
    }

    @Override
    public String createSlot(@ApiParam(name = "addSlotReq") AddSlotReq addSlotReq, @ApiParam(name = "switchId") String switchId)
    {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSwitchUrl() + "/" + switchId + "/slots";
        log.info("create slot url: {}", url);
        String jsonStr = JsonUtil.objectToJson(addSlotReq);
        String postResult = HttpActionUtil.post(url, jsonStr);
        Map resultMap = JsonUtil.jsonToMap(postResult);
        if (null == resultMap)
        {
            log.info("get slot response error: switchId: {} ",switchId );
            return null;
        }
        String status = (String) resultMap.get("status");
        if (!status.equalsIgnoreCase("pending"))
        {
            log.error("create slot error: switchId {}", switchId);
            return "error";
        }
        //update phase status, device id from agent
        return (String) resultMap.get("uuid");
    }

    @Override
    public String setSlot(@ApiParam(name = "setSlotReq") SetSlotReq setSlotReq,
                   @ApiParam(name = "slotId") String slotId)
    {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSlotUrl() + "/" + slotId + "/set";
        log.info("set slot url: {}", url);
        String jsonStr = JsonUtil.objectToJson(setSlotReq);
        String postResult = HttpActionUtil.put(url, jsonStr);
        Map resultMap = JsonUtil.jsonToMap(postResult);
        if (null == resultMap)
        {
            log.info("get slot response error: slotId: {} setSlotReq: {}", slotId, setSlotReq );
            return null;
        }
        String status = (String) resultMap.get("status");
        if (!status.equalsIgnoreCase("pending"))
        {
            log.error("set slot error: slotId {}", slotId);
            return "error";
        }
        return slotId;
    }

    @Override
    public String getSlotPhase(@ApiParam(name = "slotId") String slotId)
    {
        Map resultMap = getSlotResult(slotId);
        if (null == resultMap)
        {
            return null;
        }
        return (String) resultMap.get("phase");
    }

    @Override
    public String getSetSlotPhase(@ApiParam(name = "slotId") String slotId)
    {
        Map resultMap = getSlotResult(slotId);
        if (null == resultMap)
        {
            return null;
        }
        return (String) resultMap.get("setPhase");
    }


    private Map getSlotResult(@ApiParam(name = "slotId") String slotId)
    {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSlotUrl() + "/" + slotId;
        log.info("get slot url: {}",url);
        //send request to pxe agent
        String result = HttpActionUtil.get(url);
        result = UnderlineAndHump.underlineToHump(result);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.info("get slot from agent error, slotId: {}", slotId);
            return null;
        }
        if(! "ok".equals(resultMap.get("status")))
        {
            return null;
        }
        return resultMap;
    }

    // return switchId
    public String getSwitch(@ApiParam(name = "manageIp")String manageIp)
    {
        String switchId = VpcTimerService.switchMap.get(manageIp);
        if(null == switchId || switchId.isEmpty())
        {
            String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSwitchUrl();
            log.info("get switch url: {}",url);
            String result = HttpActionUtil.get(url);
            result = UnderlineAndHump.underlineToHump(result);
            SwitchesGetFromAgentRsp switchesRsp = JsonUtil.jsonToPojo(result, SwitchesGetFromAgentRsp.class);
            if (null == switchesRsp)
            {
                log.info("get switches error: null");
                return null;
            }
            for(String id : switchesRsp.getSwitchUuids())
            {
                if (null != putDataToSwitchMap(id,manageIp))
                {
                    return id;
                }
            }
        }
        return switchId;
    }

    @Override
    public String createPort(@ApiParam(name = "createPortReq") CreatePortReq createPortReq,
                    @ApiParam(name = "isKvm") boolean isKvm, @ApiParam(name = "portType") Integer portType)
    {
        com.lnjoying.justice.network.entity.Subnet tblSubnet = subnetService.getById(createPortReq.getSubnetId());
        if (null == tblSubnet || REMOVED == tblSubnet.getPhaseStatus())
        {
            return null;
        }
        String uuid = Utils.assignUUId();
        Port tblPort = new Port();
        tblPort.setPortId(uuid);
        tblPort.setPhaseStatus(PhaseStatus.ADDING);
        tblPort.setSubnetId(createPortReq.getSubnetId());
//                .vmInstanceId(vmInstanceId)
        tblPort.setIpAddress(createPortReq.getStaticIp());
        tblPort.setCreateTime(new Date(System.currentTimeMillis()));
        tblPort.setUpdateTime(new Date(System.currentTimeMillis()));
        tblPort.setAgentId(createPortReq.getAgentId());
        if (isKvm)
        {
            if (createPortReq.getIsVip())
            {
                tblPort.setType(PortType.vip);
            }
            else
            {
                tblPort.setType(PortType.vm);
            }
            tblPort.setInstanceId(createPortReq.getContext());
        }
        else if (null != portType)
        {
            tblPort.setType(portType);
            tblPort.setInstanceId(createPortReq.getContext());
        }
        else
        {
            tblPort.setType(PortType.baremetal);
            tblPort.setMacAddress(createPortReq.getContext());
        }

        log.info("port Id:{}, subnetId: {}",uuid, createPortReq.getSubnetId());
        boolean ok = portService.save(tblPort);
        if (!ok)
        {
            log.error("port uuid:{} insert tbl_rs_port error.",uuid);
            return null;
        }
        return uuid;
    }
    @Override
    public List<String> createPorts(@ApiParam(name = "createPortsReq") List<CreatePortReq> createPortsReqs,
                       @ApiParam(name = "isKvm") boolean isKvm)
    {
        return createPortsReqs.stream().map(
                createPortReq -> createPort( createPortReq, true, null)
        ).collect(Collectors.toList());
    }


    //add data to switchMap, and return switchId if the ip is the manageIp list.
    public String putDataToSwitchMap(String switchId, String manageIp)
    {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSwitchUrl() + "/" + switchId;
        String result = HttpActionUtil.get(url);
        result = UnderlineAndHump.underlineToHump(result);
        SwitchGetFromAgentRsp switchRsp = JsonUtil.jsonToPojo(result, SwitchGetFromAgentRsp.class);
        if (null == switchRsp)
        {
            return null;
        }
        for (SwitchGetFromAgentRsp.ipInfo ipInfo : switchRsp.getManageIps())
        {
            VpcTimerService.switchMap.put(ipInfo.getIp(), switchId);
            if (Objects.equals(manageIp, ipInfo.getIp()))
            {
                return switchId;
            }
        }
        return null;
    }

    @Override
    public List<Long> getEipMapCount(@ApiParam(name = "portIds") List<String> portIds)
    {
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        if (1 == portIds.size())
        {
            queryWrapper.eq(EipMap::getPortId, portIds.get(0))
                    .ne(EipMap::getStatus, REMOVED);
        }
        else
        {
            queryWrapper.in(EipMap::getPortId, portIds)
                    .ne(EipMap::getStatus, REMOVED);
        }
        LambdaQueryWrapper<Port> portQueryWrapper = new LambdaQueryWrapper<>();
        portQueryWrapper.in(Port::getPortId, portIds)
                .isNotNull(Port::getEipId);
        long eipPortCount = portService.count(portQueryWrapper);
        long eipMapCount = eipMapService.count(queryWrapper);
        return Arrays.asList(eipPortCount, eipMapCount);
    }

    @Override
    public List<String> getPorts(@ApiParam(name = "subnetId") String subnetId)
    {
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EipMap::getSubnetId, subnetId)
                .select(EipMap::getPortId)
                .isNotNull(EipMap::getPortId)
                .ne(EipMap::getStatus, REMOVED);
        List<String> portIds = eipMapService.listObjs(queryWrapper, Object::toString);
        LambdaQueryWrapper<Port> portQueryWrapper = new LambdaQueryWrapper<>();
        portQueryWrapper.eq(Port::getSubnetId, subnetId)
                .select(Port::getPortId)
                .isNotNull(Port::getEipId)
                .ne(Port::getPhaseStatus, REMOVED);
        portIds.addAll(portService.listObjs(portQueryWrapper, Object::toString));
        return portIds;
//        return networkRepository.getEipMaps(example).stream().map(TblRsEipMap::getPortId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String vmInstanceBoundSgs(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId)
    {
        return setSgPhase(sgIds, vmInstanceId, PhaseStatus.ADDED);
    }

    @Transactional(rollbackFor = Exception.class)
    public String setSgPhase(List<String> sgIds, String vmInstanceId, Integer phaseStatus)
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                .ne(SgVmInstance::getPhaseStatus, REMOVED)
                .in(SgVmInstance::getSgId, sgIds);
        long count = sgVmInstanceService.count(queryWrapper);
        if (count > 0)
        {
            log.info("sgVmInstance already exist: vmInstanceId:{}, sgs:{}",vmInstanceId, sgIds);
            return "exist";
        }
        long offset = 0;
        for (int i = sgIds.size()-1;i>=0;i--)
        {
            String sgId = sgIds.get(i);
            SgVmInstance tblSgVmInstance = new SgVmInstance();
            tblSgVmInstance.setSgVmId(Utils.assignUUId());
            tblSgVmInstance.setInstanceId(vmInstanceId);
            tblSgVmInstance.setSgId(sgId);
            tblSgVmInstance.setPhaseStatus(phaseStatus);
            tblSgVmInstance.setCreateTime(Utils.buildDate(System.currentTimeMillis()+offset));
            tblSgVmInstance.setUpdateTime(tblSgVmInstance.getCreateTime());
            boolean ok = sgVmInstanceService.save(tblSgVmInstance);
            if (!ok)
            {
                log.info("vmInstanceuSgs update database error: vmInstanceId:{}",vmInstanceId);
                return null;
            }
            offset++;
        }
        return "ok";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String vmInstanceApplySgWithSgIds(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId)
    {
        return setSgPhase(sgIds, vmInstanceId, PhaseStatus.APPLYING);
    }

    @Override
    public String vmInstanceApplySgs(@ApiParam(name = "vmInstanceId")String vmInstanceId)
    {
        LambdaUpdateWrapper<SgVmInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                .ne(SgVmInstance::getPhaseStatus, REMOVED);
        SgVmInstance tblSgVmInstance = new SgVmInstance();
        tblSgVmInstance.setPhaseStatus(PhaseStatus.APPLYING);
        tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = sgVmInstanceService.update(tblSgVmInstance, updateWrapper);
        if (!ok)
        {
            log.info("vmInstanceuSgs update database error: vmInstanceId:{}",vmInstanceId);
            return null;
        }
        return "ok";
    }

    @Override
    public String getApplySgsResult(@ApiParam(name = "vmInstanceId")String vmInstanceId)
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                .ne(SgVmInstance::getPhaseStatus, REMOVED);
        List<SgVmInstance> sgVmInstances = sgVmInstanceService.list(queryWrapper);
        for (SgVmInstance sgVmInstance : sgVmInstances)
        {
            if (sgVmInstance.getPhaseStatus() != PhaseStatus.APPLIED)
            {
                return null;
            }
        }
        return "ok";
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public String vmInstanceUpdateSgs(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId)
    {
        LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                .ne(SgVmInstance::getPhaseStatus, REMOVED);
        if (sgVmInstanceService.count(queryWrapper) > 0)
        {
            SgVmInstance tblSgVmInstance = new SgVmInstance();
            tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            tblSgVmInstance.setPhaseStatus(REMOVED);
            LambdaUpdateWrapper<SgVmInstance> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                    .ne(SgVmInstance::getPhaseStatus, REMOVED);
            boolean ok = sgVmInstanceService.update(tblSgVmInstance, updateWrapper);
            if (!ok)
            {
                return null;
            }
        }

        return vmInstanceBoundSgs(sgIds,vmInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String vmInstanceUnBoundSgs(@ApiParam(name = "vmInstanceId")String vmInstanceId)
    {
        try
        {
            LambdaQueryWrapper<SgVmInstance> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SgVmInstance::getInstanceId, vmInstanceId)
                    .ne(SgVmInstance::getPhaseStatus, REMOVED);
            List<SgVmInstance> tblSgVmInstances = sgVmInstanceService.list(queryWrapper);
            if (null == tblSgVmInstances || 0 == tblSgVmInstances.size())
            {
                return "ok";
            }
            SgVmInstance tblSgVmInstance = new SgVmInstance();
            tblSgVmInstance.setPhaseStatus(REMOVED);
            tblSgVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = sgVmInstanceService.update(tblSgVmInstance, queryWrapper);
            if (!ok)
            {
                log.info("vmInstanceBoundSgs update database error: vmInstanceId:{}",vmInstanceId);
                return null;
            }

            return "ok";
        }
        catch (Exception e)
        {
            log.error("vm unbound security group error:{}, vmInstanceId:{}", e.getMessage(), vmInstanceId);
            return null;
        }
    }

    public String createDnsHostByPortId(@ApiParam(name = "portId") String portId,
                                @ApiParam(name = "hostname") String hostname)
    {
        Port tblPort = portService.getById(portId);
        if (null == tblPort)
        {
            log.info("port is null, portId: {}",portId);
            return null;
        }
        com.lnjoying.justice.network.entity.Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
        if (null == tblSubnet)
        {
            log.info("subnet is null, subnetId: {}", tblPort.getSubnetId());
            return null;
        }
        CreateDnsHostReq createDnsHostReq = new CreateDnsHostReq();
        createDnsHostReq.setIp(tblPort.getIpAddress());
        createDnsHostReq.setHostname(hostname);

        String vpcIdFromAgent = getVpcIdFromAgent(tblSubnet.getVpcId());
        String hostId =  createDnsHost(vpcIdFromAgent, createDnsHostReq);
        if (null != hostId)
        {
            tblPort.setHostIdFromAgent(hostId);
            boolean ok = portService.updateById(tblPort);
            if (ok)
            {
                return hostId;
            }
        }

        return null;
    }

    public String createDnsHost(@ApiParam(name = "vpcId") String vpcId,
                                @ApiParam(name = "createDnsHostReq") CreateDnsHostReq createDnsHostReq)
    {
        String url =  combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getVpcUrl()+"/"+vpcId+"/hosts";
        String postString = JsonUtil.objectToJson(createDnsHostReq);
        String result;
        try {
            result = HttpActionUtil.post(url, postString);
            log.info("create host result: {}, url:{}, postString:{}", result, url, postString);
        } catch (Exception e) {
            log.error("creat host error: {}", e.getMessage());
            return null;
        }
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap) {
            return null;
        }
        return (String) resultMap.get("uuid");
    }

    @Override
    public List<String> createDnsHosts(@ApiParam(name = "portIds") List<String> portIds,
                                       @ApiParam(name = "hostname") String hostname)

    {

        List<String> result = portIds.stream().map(
                portId -> createDnsHostByPortId(portId, hostname)
        ).collect(Collectors.toList());
        return  result.stream().filter(Objects::nonNull).filter(portId -> !"".equals(portId)).collect(Collectors.toList());
    }

//    @Override
//    public List<String> updateNodePort(@ApiParam(name = "macs") List<String> macs,
//                                @ApiParam(name = "nodePortId") List<String> nodePortIds)
//    {
//        if(macs.size() != nodePortIds.size() || macs.size() == 0)
//        {
//            log.info("macs size is not equal nodePortIds size or macs size is 0");
//            return null;
//        }
//        List<String> portIds = new ArrayList<>();
//        for(int i=0;i<macs.size();i++)
//        {
//            LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(Port::getMacAddress, macs.get(i))
//                    .ne(Port::getPhaseStatus, REMOVED);
//            Port tblPort = portService.getOne(queryWrapper);
//            if (null == tblPort)
//            {
//                log.info("port is null, mac: {}",macs.get(i));
//                continue;
//            }
//            tblPort.setNodePortId(nodePortIds.get(i));
//            tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//            portService.updateById(tblPort);
//            portIds.add(tblPort.getPortId());
//        }
//        return portIds;
//    }

    public String removeDnsHost(@ApiParam(name = "portId") String portId)
    {
//        TblRsPort tblRsPort = networkRepository.getPortById(portId);
        Port tblPort = portService.getById(portId);
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus())
        {
            return null;
        }
        String hostId = tblPort.getHostIdFromAgent();
        String url = combRpcSerice.getVmService().getL3IpPort() + networkAgentConfig.getHostUrl()+"/"+hostId;
        String result;
        try {
            result = HttpActionUtil.delete(url);
        } catch (Exception e) {
            log.error("delete host error: {}, url:{}", e.getMessage(), url);
            return null;
        }
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap) {
            return null;
        }
        if (Objects.equals(resultMap.get("status"), "failed")) {
            return null;
        }
        tblPort.setHostIdFromAgent("");
        boolean ok = portService.updateById(tblPort);
        if (ok) return hostId;
        return null;
    }

    @Override
    public List<String> removeDnsHosts(@ApiParam(name = "portIds") List<String> portIds)
    {
        List<String> result = portIds.stream().map(
                this::removeDnsHost
        ).collect(Collectors.toList());
        return  result.stream().filter(Objects::nonNull).filter(portId -> !"".equals(portId)).collect(Collectors.toList());
    }

    public String updateDnsHost(@ApiParam(name = "portId") String portId,
                                @ApiParam(name = "hostname") String hostname)
    {
        Port tblPort = portService.getById(portId);
        if (null == tblPort)
        {
            log.info("port is null, portId: {}",portId);
            return null;
        }
        com.lnjoying.justice.network.entity.Subnet tblSubnet = subnetService.getById(tblPort.getSubnetId());
        if (null == tblSubnet)
        {
            log.info("subnet is null, subnetId: {}", tblPort.getSubnetId());
            return null;
        }
        CreateDnsHostReq createDnsHostReq = new CreateDnsHostReq();
        createDnsHostReq.setHostname(hostname);
        createDnsHostReq.setIp(tblPort.getIpAddress());

        String hostId = tblPort.getHostIdFromAgent();
        String result = removeDnsHost(portId);
        if (hostId.equals(result))
        {
            String vpcIdFromAgent = getVpcIdFromAgent(tblSubnet.getVpcId());
            hostId = createDnsHost(vpcIdFromAgent, createDnsHostReq);
            tblPort.setHostIdFromAgent(hostId);
            boolean ok = portService.updateById(tblPort);
            if (ok) return hostId;
        }
        return null;
    }

    @Override
    public List<String> updateDnsHosts(@ApiParam(name = "portIds") List<String> portIds,
                                       @ApiParam(name = "hostname") String hostname)
    {
        List<String> result =  portIds.stream().map(
                portId-> updateDnsHost(portId, hostname)
        ).collect(Collectors.toList());

        return  result.stream().filter(Objects::nonNull).filter(portId -> !"".equals(portId)).collect(Collectors.toList());
    }

    @Override
    public long getDnsHosts(@ApiParam(name = "portIds") List<String> portIds)
    {
        List<String> hostIds = portIds.stream().map(
               portId -> portService.getById(portId).getHostIdFromAgent()
        ).collect(Collectors.toList());
        return hostIds.stream().filter(Objects::nonNull).filter(portId -> !"".equals(portId)).count();
    }

    String getVpcIdFromAgent (@ApiParam(name = "vpcId")String vpcId)
    {
        com.lnjoying.justice.network.entity.Vpc tblVpc = vpcService.getById(vpcId);
        if (null == tblVpc) return  null;
        return tblVpc.getVpcIdFromAgent();
    }

    @Override
    public NetSummeryInfo  getNatSummery(@ApiParam(name ="userId") String userId,
                                         @ApiParam(name="isAdmin") boolean isAdmin)
    {
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(EipMap::getStatus, REMOVED);
        if (!isAdmin)
        {
            queryWrapper.and(qw -> qw.eq(EipMap::getUserId, userId));
        }
        NetSummeryInfo netSummeryInfo = new NetSummeryInfo();
        netSummeryInfo.setNatCount((int)eipMapService.count(queryWrapper));
        return netSummeryInfo;
    }

    @Override
    public String migratePorts(@ApiParam(name = "vmInstanceId") String vmInstanceId,
                     @ApiParam(name = "agentIp") String agentIp,
                     @ApiParam(name = "agentPort") String agentPort)
    {
        List<Port> ports = getPortsFromVmInstanceId(vmInstanceId);
        assert ports != null;
        ports.forEach(
                port -> migratePortFromAgent(agentIp,agentPort, port.getPortIdFromAgent())
        );
        return vmInstanceId;
    }

    @Override
    public String getMigratePortResult(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        List<Port> ports = getPortsFromVmInstanceId(vmInstanceId);
        assert ports != null;
        for (Port port: ports)
        {
            try
            {
                boolean result = isMigratePhaseFromAgent(port.getPortIdFromAgent());
                if (!result)
                {
                    log.info("vmInstanceId :{}, isMigratePhaseFromAgent is false", vmInstanceId);
                    return null;
                }
            }
            catch (WebSystemException e)
            {
                log.info("isMigratePhaseFromAgent error: {}", e.getMessage());
                return null;
            }
        }
        return vmInstanceId;
    }

    @Override
    public String setEipByPortId(@ApiParam(name = "portId") String portId, @ApiParam(name="eipId") String eipId,
                                 @ApiParam(name = "userId") String userId)
    {
        return bizNetworkService.attachEip(eipId, portId, userId);
    }

    private List<Port> getPortsFromVmInstanceId(String vmInstanceId)
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Port::getInstanceId,vmInstanceId)
                .ne(Port::getPhaseStatus, REMOVED);
        if (portService.count(queryWrapper) < 1)
        {
            log.info("the count of port is 0, vmInstanceId :{}", vmInstanceId);
            return null;
        }
        return portService.list(queryWrapper);
    }


//    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
    private void migratePortFromAgent(String agentIp, String agentPort, String portId)
    {
        MergePortFromAgentReq req = new MergePortFromAgentReq();
        req.setAgentPort(agentPort);
        req.setAgentIp(agentIp);
        String reqString = JsonUtil.objectToJson(req);
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getPortUrl()
                + "/" + portId + "/migrate";
        String result = HttpActionUtil.put(url, reqString);
        Map resultMap= JsonUtil.jsonToMap(result);
        assert resultMap != null;
        if (!AgentConstant.PENDING.equals(resultMap.get("status")))
        {
            log.error("url: {}, put:{} err",url, reqString);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
    }

//    @Retryable(value = Exception.class, maxAttempts = 3,
//            backoff = @Backoff(delay = 1000, multiplier = 1))
    private boolean isMigratePhaseFromAgent(String portId)
    {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getPortUrl()
                + "/" + portId;
        String result = HttpActionUtil.get(url);
        Map resultMap= JsonUtil.jsonToMap(result);
        assert resultMap != null;
        if (Objects.equals(AgentConstant.MIGRATE_FAILED,resultMap.get("migrate_phase")))
        {
            return false;
        }
        else if (Objects.equals(AgentConstant.MIGRATED,resultMap.get("migrate_phase")))
        {
            return true;
        }
        else
        {
            log.info("url: {}, result:{}, portId: {}", url, result, portId);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
    }
}
