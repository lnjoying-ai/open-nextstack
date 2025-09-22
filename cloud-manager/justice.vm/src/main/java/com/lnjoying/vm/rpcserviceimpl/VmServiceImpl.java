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

package com.lnjoying.vm.rpcserviceimpl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lnjoying.justice.schema.service.compute.ComputeService;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ImageOsType;
import com.lnjoying.vm.common.ResourceType;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.response.BaseRsp;
import com.lnjoying.vm.entity.*;
import com.lnjoying.vm.mapper.GpuFlavorMapper;
import com.lnjoying.vm.service.*;
import com.lnjoying.vm.service.biz.CombRpcSerice;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.utils.JsonUtil;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;
import static com.lnjoying.vm.common.ComputeUrl.*;
import static com.lnjoying.vm.common.VmInstanceStatus.VM;

@RpcSchema(schemaId = "computeVmService")
@Slf4j
public class VmServiceImpl implements VmService
{
    @Autowired
    ComputeConfig computeConfig;

    @Autowired
    CombRpcSerice combRpcSerice;

    @Autowired
    VmInstanceService vmInstanceService;

    @Autowired
    HypervisorNodeService hypervisorNodeService;

    @Autowired
    NfsService nfsService;

    @Autowired
    InstanceNetworkRefService instanceNetworkRefService;

    @Autowired
    DiskInfoService diskInfoService;

    @Autowired
    VmSnapService vmSnapService;

    @Resource
    GpuFlavorMapper gpuFlavorMapper;

    @Autowired
    InstanceGroupService instanceGroupService;

    public final static short vmType = 0;

    @Override
    public List<GpuFlavorInfo> getGpuFlavorInfos()
    {
        List<GpuFlavorInfo> gpuFlavorInfos = gpuFlavorMapper.selectGpuFlavorInfo();
        if (0 == gpuFlavorInfos.size())
        {
            GpuFlavorInfo gpuFlavorInfo = new GpuFlavorInfo();
            gpuFlavorInfo.setGpuCount(0);
            gpuFlavorInfo.setGpuName("");
            gpuFlavorInfos.add(gpuFlavorInfo);
        }
        return gpuFlavorInfos;
    }

    @Override
    public List<GpuFlavorInfo> getGpuFlavorInfosByName(@ApiParam(name = "gpuName") String gpuName)
    {
        List<GpuFlavorInfo> gpuFlavorInfos = gpuFlavorMapper.selectGpuFlavorInfoByName(gpuName);
        if (0 == gpuFlavorInfos.size())
        {
            GpuFlavorInfo gpuFlavorInfo = new GpuFlavorInfo();
            gpuFlavorInfo.setGpuCount(0);
            gpuFlavorInfo.setGpuName("");
            gpuFlavorInfos.add(gpuFlavorInfo);
        }
        return gpuFlavorInfos;
    }

    @Override
    public ComputeService.Instance getVmInstanceFromPortId(@ApiParam(name = "portId") String portId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getPortId, portId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        List<VmInstance> tblVmInstances = vmInstanceService.list(queryWrapper);

        VmInstance tblVmInstance;
        if (null == tblVmInstances || 0 == tblVmInstances.size())
        {
            LambdaQueryWrapper<InstanceNetworkRef> networkRefLambdaQueryWrapper = new LambdaQueryWrapper<>();
            networkRefLambdaQueryWrapper.eq(InstanceNetworkRef::getPortId, portId)
                    .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);

            if (instanceNetworkRefService.count(networkRefLambdaQueryWrapper) == 0)
            {
                log.info("get instance: null, portId: {}", portId);
                return null;
            }
            InstanceNetworkRef instanceNetworkRef = instanceNetworkRefService.getOne(networkRefLambdaQueryWrapper);
            String instanceId = instanceNetworkRef.getInstanceId();
            tblVmInstance = vmInstanceService.getById(instanceId);
        }
        else
        {
            tblVmInstance = tblVmInstances.get(0);
        }
        ComputeService.Instance instance = new ComputeService.Instance();
        instance.setInstanceId(tblVmInstance.getVmInstanceId());
        instance.setInstanceName(tblVmInstance.getName());
        instance.setPortId(portId);
        return instance;
    }

    @Override
    public String createSecurityGroupAndRules(@ApiParam(name = "rules") List<String> rules, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        try
        {
            String manageIp = getHypervisorNodeIp(vmInstanceId);
            String sgIdFromAgent = createSecurityGroup(manageIp);
            if (null != sgIdFromAgent)
            {
                CompletableFuture.runAsync(() -> updateSGRules(rules, manageIp, sgIdFromAgent));
            }
            return sgIdFromAgent;
        }
        catch (Exception e)
        {
            log.error("createSecurityGroupAndRules error: {}, vmInstanceId:{}, rules:{}", e.getMessage(), vmInstanceId, rules);
            return null;
        }
    }

    @Override
    public String vmBondSecurityGroup(@ApiParam(name = "sgId") List<String> sgIds, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        try
        {
            String vmInstanceIdFromAgent = vmInstanceService.getById(vmInstanceId).getInstanceIdFromAgent();
            String manageIp = getHypervisorNodeIp(vmInstanceId);
            return vmBondSecurityGroup(manageIp, vmInstanceIdFromAgent, sgIds);
        }
        catch (Exception e)
        {
            log.error("vmBondSecurityGroup error:{}, vmInstanceId:{}, sgIds:{}", e.getMessage(), vmInstanceId, sgIds);
            return null;
        }
    }

    @Override
    public String vmUnbondSecurityGroup(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        return vmBondSecurityGroup(null, vmInstanceId);
    }

    @Override
    public String updateSecurityGroupRules(@ApiParam(name = "rules") List<String> rules, @ApiParam(name = "vmInstanceId") String vmInstanceId, @ApiParam(name = "sgId") String sgId)
    {
        try
        {
            String manageIp = getHypervisorNodeIp(vmInstanceId);
            String phaseStatus = getSecurityGroup(manageIp, sgId);
            if (phaseStatus == null)
            {
                return null;
            }
            return updateSGRules(rules, manageIp, sgId);
        }
        catch (Exception e)
        {
            log.error("updateSecurityGroupRules error: {}, vmInstanceId:{}, rules:{}", e.getMessage(), vmInstanceId, rules);
            return null;
        }
    }

    @Override
    public int getSecurityGroupStatus(@ApiParam(name = "sgId") String sgId, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        try
        {
            String manageIp = getHypervisorNodeIp(vmInstanceId);
            String phaseStatus = getSecurityGroup(manageIp, sgId);
            switch (Objects.requireNonNull(phaseStatus))
            {
                case AgentConstant.UPDATED:
                    return VmInstanceStatus.SG_UPDATED;
                case AgentConstant.UPDATE_FAILED:
                    return VmInstanceStatus.SG_UPDATE_FAILED;
                case AgentConstant.ADDED:
                    return VmInstanceStatus.SG_ADDED;
            }
            return 0;
        }
        catch (Exception e)
        {
            log.error("getSecurityGroupCreateStatus error:{}, sgId:{}, vmInstanceId:{}", e.getMessage(), sgId, vmInstanceId);
            return 0;
        }
    }

    @Override
    public boolean canCreateEipMap(@ApiParam(name = "portId") String portId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getPortId, portId)
                .and(wrapper -> wrapper.ge(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_MIGRATE_INIT)
                        .lt(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_RESUMED)
                        .or().eq(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_MIGRATE_FAILED)
                        .or().eq(VmInstance::getPhaseStatus, VmInstanceStatus.INSTANCE_CREATE_FAILED));
        return vmInstanceService.count(queryWrapper) == 0;
    }

    @Override
    public int getVmSecurityGroupStatus(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        try
        {
            String manageIp = getHypervisorNodeIp(vmInstanceId);
            return getVmSecurityGroupStatusFromAgent(manageIp, vmInstanceId);
        }
        catch (Exception e)
        {
            log.error("getVmSecurityGroupStatus error:{}, vmInstanceId:{}", e.getMessage(), vmInstanceId);
            return -1;
        }


    }

    @Override
    public String removeSecurityGroup(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceIds") List<String> vmInstanceIds)
    {
        try
        {
            if (sgIds.size() != vmInstanceIds.size())
            {
                return null;
            }
            for (int i = 0; i < sgIds.size(); i++)
            {
                String manageIp = getHypervisorNodeIp(vmInstanceIds.get(i));
                String removeStatus = removeSecurityGroup(manageIp, sgIds.get(i));
                if (null == removeStatus)
                {
                    return null;
                }
            }
            return "ok";
        }
        catch (Exception e)
        {
            log.error("removeSecurityGroup error: {}, sgIds:{}, vmInstanceIds:{}", e.getMessage(), sgIds, vmInstanceIds);
            return null;
        }
    }

    @Override
    public Boolean isVmInstancePowerOff(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus()) return false;
        return tblVmInstance.getPhaseStatus().equals(VmInstanceStatus.INSTANCE_POWEROFF);
    }


    @Override
    public List<ComputeService.VpcAndPortInfo> getVpcAndPortFromVmInstanceId(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        List<ComputeService.VpcAndPortInfo> vpcAndPortInfos = new ArrayList<>();
        ComputeService.VpcAndPortInfo vpcAndPortInfo = new ComputeService.VpcAndPortInfo();

        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            vpcAndPortInfo.setIsRemoved(true);
            vpcAndPortInfos.add(vpcAndPortInfo);
            return vpcAndPortInfos;
        }
        vpcAndPortInfo.setPortId(tblVmInstance.getPortId());
        vpcAndPortInfo.setVpcId(tblVmInstance.getVpcId());

        vpcAndPortInfos.add(vpcAndPortInfo);
        LambdaQueryWrapper<InstanceNetworkRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(InstanceNetworkRef::getPhaseStatus, REMOVED)
                .eq(InstanceNetworkRef::getInstanceId, tblVmInstance.getVmInstanceId());

        if (instanceNetworkRefService.count(queryWrapper) > 0)
        {

            List<InstanceNetworkRef> tblInstanceNetworkRefs = instanceNetworkRefService.list(queryWrapper);
            List<ComputeService.VpcAndPortInfo> instanceNetworkRefs = tblInstanceNetworkRefs.stream().map(tblInstanceNetworkRef ->
            {
                ComputeService.VpcAndPortInfo req = new ComputeService.VpcAndPortInfo();
                req.setVpcId(tblInstanceNetworkRef.getVpcId());
                req.setPortId(tblInstanceNetworkRef.getPortId());
                return req;
            }).collect(Collectors.toList());
            vpcAndPortInfos.addAll(instanceNetworkRefs);
        }
        log.info("get vpcAndPortInfo :{}", vpcAndPortInfo);
        return vpcAndPortInfos;
    }

    @Override
    public List<VmService.InstanceDetailInfo> getVmInstanceDetailInfos(@ApiParam(name = "instanceIdList") List<String> instanceIdList)
    {
        return instanceIdList.stream().map(this::getVmInstanceDetailInfo).collect(Collectors.toList());
    }

    @Override
    public ComputeService.AgentIpPort getAgentIpPort(@ApiParam(name = "portType") short portType, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        ComputeService.AgentIpPort agentIpPort = new ComputeService.AgentIpPort();
        Integer vmPhoneHomePort = computeConfig.getVmPhoneHomePort();
        if (null == vmPhoneHomePort)
        {
            vmPhoneHomePort = VM_PHONE_HOME_PORT;
        }

        if (vmType == portType)
        {
            agentIpPort.setPort(vmPhoneHomePort.toString());
            agentIpPort.setIp(hypervisorNodeService.getById(vmInstanceService.getById(vmInstanceId).getNodeId()).getManageIp());
        }

        return agentIpPort;
    }


    @Override
    public ComputeService.AgentIpPort NfsAgentIpPort(@ApiParam(name = "instanceId") String instanceId)
    {
        Nfs tblNfs = nfsService.getById(instanceId);
        if (null == tblNfs || REMOVED == tblNfs.getPhaseStatus())
        {
            return null;
        }
        ComputeService.AgentIpPort agentIpPort = new ComputeService.AgentIpPort();
        Integer vmPhoneHomePort = computeConfig.getVmPhoneHomePort();
        if (null == vmPhoneHomePort)
        {
            vmPhoneHomePort = VM_PHONE_HOME_PORT;
        }
        agentIpPort.setIp(tblNfs.getNodeIp());
        agentIpPort.setPort(vmPhoneHomePort.toString());
        return agentIpPort;
    }


    @Transactional(rollbackFor = Exception.class)
    public VmService.InstanceDetailInfo getVmInstanceDetailInfo(@ApiParam(name = "instanceId") String instanceId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getVmInstanceId, instanceId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        List<VmInstance> vmInstances = vmInstanceService.list(queryWrapper);
        if (null == vmInstances || 0 == vmInstances.size())
        {
            log.info("get vm instance: null, instanceId: {}", instanceId);
            return null;
        }
        VmInstance tblVmInstance = vmInstances.get(0);
        VmService.InstanceDetailInfo instanceDetailInfo = new VmService.InstanceDetailInfo();
        instanceDetailInfo.setInstanceId(instanceId);
        instanceDetailInfo.setName(tblVmInstance.getName());
        instanceDetailInfo.setPortId(tblVmInstance.getPortId());
        instanceDetailInfo.setPhaseStatus(tblVmInstance.getPhaseStatus());
        instanceDetailInfo.setFlavorId(tblVmInstance.getFlavorId());
        FlavorService.FlavorInfo flavorInfo = combRpcSerice.getFlavorService().getFlavorInfo(tblVmInstance.getFlavorId());
        instanceDetailInfo.setFlavorName(flavorInfo.getName());
        return instanceDetailInfo;
    }

    @Override
    public int getInstanceCountByFlavorId(@ApiParam(name = "flavorId") String flavorId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getFlavorId, flavorId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        return (int) vmInstanceService.count(queryWrapper);
    }

    @Override
    public Boolean isMigrating(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus()) return null;
        return VmInstanceStatus.INSTANCE_MIGRATE_INIT <= tblVmInstance.getPhaseStatus() && VmInstanceStatus.INSTANCE_RESUMED >= tblVmInstance.getPhaseStatus();
    }

    @Override
    public Boolean attachVolume(@ApiParam(name = "volumeId") String volumeId, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        LambdaUpdateWrapper<DiskInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DiskInfo::getVolumeId, volumeId)
                .ne(DiskInfo::getPhaseStatus, REMOVED);

        if (0 == diskInfoService.count(updateWrapper))
        {
            DiskInfo tblDiskInfo = new DiskInfo();
            tblDiskInfo.setDiskId(Utils.assignUUId());
            tblDiskInfo.setIsNew(false);
            tblDiskInfo.setVmInstanceId(vmInstanceId);
            tblDiskInfo.setVolumeId(volumeId);
            tblDiskInfo.setPhaseStatus(VmInstanceStatus.INSTANCE_CREATED);
            tblDiskInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblDiskInfo.setUpdateTime(tblDiskInfo.getCreateTime());
            return diskInfoService.save(tblDiskInfo);
        }

        return null;
    }


    @Override
    public Boolean detachVolume(@ApiParam(name = "volumeId") String volumeId)
    {
        LambdaUpdateWrapper<DiskInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DiskInfo::getVolumeId, volumeId)
                .ne(DiskInfo::getPhaseStatus, REMOVED);

        if (0 == diskInfoService.count(updateWrapper))
        {
            return true;
        }
        DiskInfo tblDiskInfo = new DiskInfo();
        tblDiskInfo.setPhaseStatus(REMOVED);
        tblDiskInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));

        return diskInfoService.update(tblDiskInfo, updateWrapper);
    }

//    @Override
//    public String getUserIdByVmId(@ApiParam(name="vmId") String vmId)
//    {
//        VmInstance vmInstance = vmInstanceService.getById(vmId);
//        if (null == vmInstance || REMOVED == vmInstance.getPhaseStatus())
//        {
//            return null;
//        }
//        return vmInstance.getUserId();
//    }

    @Override
    public Boolean isVmOkByVmId(@ApiParam(name = "vmId") String vmId)
    {
        VmInstance vmInstance = vmInstanceService.getById(vmId);
        int phaseStatus = vmInstance.getPhaseStatus();
        return VmInstanceStatus.INSTANCE_CREATE_FAILED != phaseStatus &&
                REMOVED != phaseStatus;
    }

    @Override
    public NodeIpAndVmAgentId getNodeIpAndVmAgentIdByVmId(@ApiParam(name = "vmId") String vmId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            return null;
        }
        NodeIpAndVmAgentId nodeIpAndVmAgentId = new NodeIpAndVmAgentId();

        if (VmInstanceStatus.INSTANCE_CREATE_FAILED == tblVmInstance.getPhaseStatus())
        {
            nodeIpAndVmAgentId.setStatus(AgentConstant.GET_STATUS_FAILED);
        }
        HypervisorNode hypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());

        nodeIpAndVmAgentId.setNodeIp(hypervisorNode.getManageIp());
        nodeIpAndVmAgentId.setVmIdFromAgent(tblVmInstance.getInstanceIdFromAgent());
        return nodeIpAndVmAgentId;
    }


    @Override
    public List<ComputeService.InstanceInfo> getInstanceInfosFromSubnetId(@ApiParam(name = "subnetId") String subnetId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getSubnetId, subnetId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        List<VmInstance> tblVmInstances = vmInstanceService.list(queryWrapper);

        // vm portId
        LambdaQueryWrapper<InstanceNetworkRef> networkRefLambdaQueryWrapper = new LambdaQueryWrapper<>();
        networkRefLambdaQueryWrapper.eq(InstanceNetworkRef::getSubnetId, subnetId)
                .ne(InstanceNetworkRef::getPhaseStatus, REMOVED);
        List<InstanceNetworkRef> tblInstanceNetworkRefs = instanceNetworkRefService.list(networkRefLambdaQueryWrapper);
        List<ComputeService.InstanceInfo> vmInstances = new ArrayList<>();
        if (tblInstanceNetworkRefs.size() > 0)
        {
            vmInstances.addAll(tblInstanceNetworkRefs.stream().map(
                    tblInstanceNetworkRef ->
                    {
                        ComputeService.InstanceInfo instanceInfo = new ComputeService.InstanceInfo();
                        instanceInfo.setInstanceId(tblInstanceNetworkRef.getInstanceId());
                        instanceInfo.setPortId(tblInstanceNetworkRef.getPortId());
                        instanceInfo.setVm(tblInstanceNetworkRef.getInstanceType() == VM);
                        instanceInfo.setName(vmInstanceService.getById(tblInstanceNetworkRef.getInstanceId()).getName());
                        return instanceInfo;
                    }).collect(Collectors.toList()));

        }
        if (tblVmInstances.size() > 0)
        {
            vmInstances.addAll(tblVmInstances.stream().map(
                    tblVmInstance ->
                    {
                        ComputeService.InstanceInfo instanceInfo = new ComputeService.InstanceInfo();
                        instanceInfo.setInstanceId(tblVmInstance.getVmInstanceId());
                        instanceInfo.setName(tblVmInstance.getName());
                        instanceInfo.setPortId(tblVmInstance.getPortId());
                        instanceInfo.setVm(true);
                        return instanceInfo;
                    }
            ).collect(Collectors.toList()));
        }
        if (0 == vmInstances.size())
        {
            log.info("get instance: null, subnetId: {}", subnetId);
            return new ArrayList<>();
        }
        return vmInstances;
    }

    @Override
    public Map<String, List<ComputeService.InstanceInfo>> getInstanceInfos(@ApiParam(name = "subnetIdList") List<String> subnetIdList)
    {
        HashSet<String> set = new HashSet<>(subnetIdList);
        if (set.size() != subnetIdList.size())
        {
            log.info("there are duplicate elements in subnetIdList");
            return null;
        }
        return subnetIdList.stream().collect(Collectors.toMap(
                Function.identity(), this::getInstanceInfosFromSubnetId
        ));
    }

    public Boolean isVmPowerOff(@ApiParam(name = "vmId") String vmId)
    {
        VmInstance vmInstance = vmInstanceService.getById(vmId);
        if (VmInstanceStatus.INSTANCE_POWEROFF == vmInstance.getPhaseStatus() ||
                VmInstanceStatus.INSTANCE_POWERED_OFF_DETACH_PCI == vmInstance.getPhaseStatus())
        {
            return true;
        }
        return false;
    }

    @Override
    public String getHypervisorNodeIp(@ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        HypervisorNode tblHypervisorNode = hypervisorNodeService.getById(tblVmInstance.getNodeId());
        if (null == tblHypervisorNode)
        {
            return null;
        }
        return tblHypervisorNode.getManageIp();
    }

    public List<String> getHypervisorNodeIpPorts(@ApiParam(name = "nodeIds") List<String> nodeIds)
    {
        Set<String> nodeIdSet = new HashSet<>(nodeIds);
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(HypervisorNode::getManageIp)
                .in(HypervisorNode::getNodeId, nodeIdSet)
                .ne(HypervisorNode::getPhaseStatus, REMOVED);
        List<String> nodeIps = hypervisorNodeService.listObjs(queryWrapper, Object::toString);
        return nodeIps.stream().map(nodeIp -> nodeIp + AgentConstant.HYPERVISOR_NODE_EXPORTER_PORT).distinct().collect(Collectors.toList());
    }

    private String createSecurityGroup(@ApiParam(name = "hypervisorManagerIp") String hypervisorManagerIp)
    {
        String url = "http://" + hypervisorManagerIp + ":" + VM_AGENT_PORT + V1_SG_URL;
        BaseRsp result = HttpActionUtil.post(url, null, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating security group error, mangerIp: {}", hypervisorManagerIp);
            return null;
        }

        String status = result.getStatus();
        if ("ok".equals(status))
        {
            return result.getUuid();
        }
        return null;
    }

    private String updateSGRules(@ApiParam(name = "rules") List<String> rules, @ApiParam(name = "hypervisorManagerIp") String hypervisorManagerIp, @ApiParam(name = "sgId") String sgId)
    {
        String url = "http://" + hypervisorManagerIp + ":" + VM_AGENT_PORT + V1_SG_URL + "/" + sgId + "/update";
        Rules putRules = new Rules();
        putRules.setRules(rules);
        String jsonStr = JsonUtil.objectToJson(putRules);
        BaseRsp result = HttpActionUtil.put(url, jsonStr, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of creating security group error, mangerIp: {}", hypervisorManagerIp);
            return null;
        }

        String status = result.getStatus()  ;
        if ("ok".equals(status) || "pending".equals(status))
        {
            return sgId;
        }
        return null;
    }

    private String removeSecurityGroup(@ApiParam(name = "hypervisorManagerIp") String hypervisorManagerIp, @ApiParam(name = "sgId") String sgId)
    {
        String url = "http://" + hypervisorManagerIp + ":" + VM_AGENT_PORT + V1_SG_URL + "/" + sgId;
        String result = HttpActionUtil.delete(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.error("get response of removing security group error, mangerIp: {}", hypervisorManagerIp);
            return null;
        }

        String status = (String) resultMap.get("status");
        if ("ok".equals(status))
        {
            return sgId;
        }
        return null;
    }

    private String getSecurityGroup(@ApiParam(name = "hypervisorManagerIp") String hypervisorManagerIp, @ApiParam(name = "sgId") String sgId)
    {
        String url = "http://" + hypervisorManagerIp + ":" + VM_AGENT_PORT + V1_SG_URL + "/" + sgId;
        String result = HttpActionUtil.get(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.error("get response of security group status error, mangerIp: {}", hypervisorManagerIp);
            return null;
        }
        String status = (String) resultMap.get("status");
        if ("ok".equals(status))
        {
            return (String) resultMap.get("phase");
        }
        return null;
    }

    public static String vmBondSecurityGroup(@ApiParam(name = "hypervisorManagerIp") String hypervisorManagerIp, @ApiParam(name = "vmInstanceId") String vmInstanceId, @ApiParam(name = "sgIds") List<String> sgIds)
    {

        String url = String.format("http://%s:%d/%s/%s/apply", hypervisorManagerIp, VM_AGENT_PORT, V1_VM_URL, vmInstanceId);
        SecurityGroups securityGroups = new SecurityGroups();
        securityGroups.setSgs(sgIds);
        String jsonStr = JsonUtil.objectToJson(securityGroups);
        BaseRsp result = HttpActionUtil.put(url, jsonStr, BaseRsp.class);
        if (null == result)
        {
            log.error("get response of bonding security group error, mangerIp: {},vmInstanceId: {}", hypervisorManagerIp, vmInstanceId);
            return null;
        }

        String status = result.getStatus();
        if ("ok".equals(status) || "pending".equals(status))
        {
            return vmInstanceId;
        }
        return null;
    }

    @Override
    public UserIdAndVmSnaps getUserIdAndVmSnaps(@ApiParam(name = "vmId") String vmId)
    {
        UserIdAndVmSnaps userIdAndVmSnaps = new UserIdAndVmSnaps();
        LambdaQueryWrapper<VmSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmSnap::getVmInstanceId, vmId)
                .ne(VmSnap::getPhaseStatus, REMOVED);

        if (vmSnapService.count(queryWrapper) > 0)
        {
            userIdAndVmSnaps.setHasVmSnaps(true);
        }
        else
        {
            userIdAndVmSnaps.setHasVmSnaps(false);
        }
        VmInstance vmInstance = vmInstanceService.getById(vmId);
        if (null == vmInstance || REMOVED == vmInstance.getPhaseStatus())
        {
            return null;
        }
        userIdAndVmSnaps.setUserId(vmInstance.getUserId());
        return userIdAndVmSnaps;
    }

    @Override
    public String getImageIdByVolumeId(@ApiParam(name = "volumeId") String volumeId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getVolumeId, volumeId)
                .ne(VmInstance::getPhaseStatus, REMOVED);
        VmInstance vmInstance = vmInstanceService.getOne(queryWrapper);
        if (vmInstance == null)
        {
            return null;
        }
        else
        {
            return vmInstance.getImageId();
        }
    }

    @Override
    public Integer getImageOsTypeByVolumeId(@ApiParam(name = "volumeId") String volumeId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VmInstance::getVolumeId, volumeId)
                .orderByDesc(VmInstance::getCreateTime)
                .last("limit 1");
        VmInstance vmInstance = vmInstanceService.getOne(queryWrapper);
        if (vmInstance == null)
        {
            return null;
        }
        else
        {
            return "linux".equals(vmInstance.getOsType()) ? ImageOsType.LINUX : ImageOsType.WINDOWS;
        }
    }

    @Override
    public String getNodeIdByIp(@ApiParam(name = "nodeIp") String nodeIp)
    {
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNode::getManageIp, nodeIp)
                .ne(HypervisorNode::getPhaseStatus, REMOVED);
        if (hypervisorNodeService.count(queryWrapper) == 0)
        {
            return null;
        }
        HypervisorNode hypervisorNode = hypervisorNodeService.getOne(queryWrapper);
        return hypervisorNode.getNodeId();
    }

    @Override
    public String setVmInstanceEip(@ApiParam(name = "eipId") String eipId, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {

        VmInstance tblVmInstance = vmInstanceService.getById(vmInstanceId);
        if (null == tblVmInstance || REMOVED == tblVmInstance.getPhaseStatus())
        {
            return null;
        }
        if (StrUtil.isBlank(eipId))
        {
            tblVmInstance.setEipId(null);
        }
        else
        {
            tblVmInstance.setEipId(eipId);
        }
        tblVmInstance.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (!vmInstanceService.updateById(tblVmInstance))
        {
            return null;
        }
        return vmInstanceId;
    }


    /**
     * @param: volumeId
     * @description: TODO
     * @return: java.lang.String
     * @author: LiSen
     * @date: 2023/6/9
     */
    @Override
    public Map<String, Object> getResourceIdToName(Integer resourceType, HashSet<String> resourceIdsSet)
    {
        Map<String, Object> objectMap = new HashMap<>(6);
        if (resourceType == ResourceType.VM_INSTANCE)
        {
            List<VmInstance> vmInstances = vmInstanceService.list(new LambdaQueryWrapper<VmInstance>().in(VmInstance::getVmInstanceId, resourceIdsSet).ne(VmInstance::getPhaseStatus, REMOVED));
            objectMap = vmInstances.stream()
                    .collect(Collectors.toMap(VmInstance::getVmInstanceId, VmInstance::getName));
        }
        else if (resourceType == ResourceType.INSTANCE_GROUP)
        {
            //instanceGroupService
            List<InstanceGroup> instanceGroups = instanceGroupService.list(new LambdaQueryWrapper<InstanceGroup>().in(InstanceGroup::getInstanceGroupId, resourceIdsSet).ne(InstanceGroup::getPhaseStatus, REMOVED));
            objectMap = instanceGroups.stream()
                    .collect(Collectors.toMap(InstanceGroup::getInstanceGroupId, InstanceGroup::getName));
        }
        else if (resourceType == ResourceType.HYPERVISOR_NODE)
        {

            //去除 计算节点 的 端口
//            resourceIdsSet = resourceIdsSet.stream()
//                    .map(resourceId -> resourceId.replace(":" + AgentConstant.HYPERVISOR_NODE_MANAGE_PORT, ""))
//                    .collect(Collectors.toCollection(HashSet::new));

            //hypervisorNodeService
            List<HypervisorNode> instanceGroups = hypervisorNodeService.list(new LambdaQueryWrapper<HypervisorNode>().in(HypervisorNode::getNodeId, resourceIdsSet).ne(HypervisorNode::getPhaseStatus, REMOVED));
            objectMap = instanceGroups.stream()
                    .collect(Collectors.toMap(
                            HypervisorNode::getNodeId,
                            HypervisorNode::getName
                    ));

        }
        return objectMap;
    }


    private int getVmSecurityGroupStatusFromAgent(@ApiParam(name = "hypervisorManagerIp") String hypervisorManagerIp, @ApiParam(name = "vmInstanceId") String vmInstanceId)
    {
        String url = String.format("http://%s:%d/%s/%s", hypervisorManagerIp, VM_AGENT_PORT, V1_VM_URL, vmInstanceId);
        String result = HttpActionUtil.get(url);
        Map resultMap = JsonUtil.jsonToMap(result);
        if (null == resultMap)
        {
            log.error("get response of security group status error, mangerIp: {}", hypervisorManagerIp);
            return -1;
        }
        String status = (String) resultMap.get("status");
        if ("ok".equals(status))
        {
            String sgPhaseStatus = (String) resultMap.get("sg_phase");
            switch (sgPhaseStatus)
            {
                case AgentConstant.APPLIED:
                    return VmInstanceStatus.APPLIED;
                case AgentConstant.APPLY_FAILED:
                    return VmInstanceStatus.APPLY_FAILED;
                case AgentConstant.UNAPPLIED:
                    return VmInstanceStatus.UNAPPLIED;
                case AgentConstant.UNAPPLY_FAILED:
                    return VmInstanceStatus.UNAPPLY_FAILED;
                case AgentConstant.APPLYING:
                    return VmInstanceStatus.APPLYING;
                case AgentConstant.UNAPPLYING:
                    return VmInstanceStatus.UNAPPLYING;
            }
        }
        return -1;
    }

    @Override
    public String getL3IpPort()
    {
        LambdaQueryWrapper<HypervisorNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HypervisorNode::getPhaseStatus, VmInstanceStatus.HYPERVISOR_NODE_CREATED)
                .eq(HypervisorNode::getMasterL3, true)
                .last("limit 1");
        HypervisorNode hypervisorNode = hypervisorNodeService.getOne(queryWrapper);
        if (null == hypervisorNode)
        {
            return null;
        }
        return "http://" + hypervisorNode.getManageIp() + ":" + computeConfig.getVmAgentPort();
//        return "http://"+AgentConstant.L3Ip.get()+":"+computeConfig.getVmAgentPort();
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

    @Data
    final static class AgentIpPort
    {
        String ip;
        String port;
    }
}
