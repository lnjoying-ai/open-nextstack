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

package com.lnjoying.justice.schema.service.compute;

import io.swagger.annotations.ApiParam;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public interface VmService
{

    ComputeService.Instance getVmInstanceFromPortId(@ApiParam(name = "portId") String portId);

    List<GpuFlavorInfo> getGpuFlavorInfos();

    List<GpuFlavorInfo> getGpuFlavorInfosByName(@ApiParam(name = "gpuName") String gpuName);

    String createSecurityGroupAndRules(@ApiParam(name = "rules") List<String> rules, @ApiParam(name = "vmInstanceId") String vmInstanceId);

    String vmBondSecurityGroup(@ApiParam(name = "sgId") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId);

    String vmUnbondSecurityGroup( @ApiParam(name = "vmInstanceId")String vmInstanceId);

    String updateSecurityGroupRules(@ApiParam(name = "rules")List<String> rules,@ApiParam(name = "vmInstanceId") String vmInstanceId, @ApiParam(name = "sgId") String sgId );

    int getSecurityGroupStatus(@ApiParam(name = "sgId")String sgId, @ApiParam(name = "vmInstanceId")String vmInstanceId);

    boolean canCreateEipMap(@ApiParam(name = "portId") String portId);

    int getVmSecurityGroupStatus( @ApiParam(name = "vmInstanceId")String vmInstanceId);

    String removeSecurityGroup(@ApiParam(name = "sgIds")List<String> sgIds, @ApiParam(name = "vmInstanceIds")List<String> vmInstanceIds);

    List<ComputeService.VpcAndPortInfo> getVpcAndPortFromVmInstanceId(@ApiParam(name = "vmInstanceId")String vmInstanceId);

    Boolean isVmInstancePowerOff(@ApiParam(name = "vmInstanceId")String vmInstanceId);

    int getInstanceCountByFlavorId(@ApiParam(name = "flavorId") String flavorId);

    Boolean attachVolume(@ApiParam(name="volumeId") String volumeId, @ApiParam(name="vmInstanceId") String vmInstanceId);

    Boolean isMigrating(@ApiParam(name = "vmInstanceId") String vmInstanceId);
//    String getUserIdByVmId(@ApiParam(name="vmId") String vmId);
    //update tbl_disk_info REMOVED when volume is DETACHED
    Boolean detachVolume(@ApiParam(name="volumeId") String volumeId);

    Boolean isVmOkByVmId(@ApiParam(name="vmId") String vmId);

    List<VmService.InstanceDetailInfo> getVmInstanceDetailInfos(@ApiParam(name = "instanceIdList") List<String> instanceIdList);

    List<ComputeService.InstanceInfo> getInstanceInfosFromSubnetId(@ApiParam(name = "subnetId") String subnetId);

    Map<String,List<ComputeService.InstanceInfo>> getInstanceInfos(@ApiParam(name = "subnetIdList") List<String> subnetIdList);

    String getHypervisorNodeIp(@ApiParam(name = "vmInstanceId")String vmInstanceId);

    List<String> getHypervisorNodeIpPorts(@ApiParam(name = "nodeIds")List<String> nodeIds);

    Boolean isVmPowerOff(@ApiParam(name="vmId") String vmId);

    String getImageIdByVolumeId(@ApiParam(name = "volumeId") String volumeId);

    Integer getImageOsTypeByVolumeId(@ApiParam(name = "volumeId") String volumeId);

    String getNodeIdByIp(@ApiParam(name = "nodeIp") String nodeIp);

    String setVmInstanceEip(@ApiParam(name = "eipId") String eipId, @ApiParam(name = "vmInstanceId") String vmInstanceId);

    Map<String, Object> getResourceIdToName(@ApiParam(name = "resourceType") Integer resourceType,@ApiParam(name = "resourceIdsSet") HashSet<String> resourceIdsSet);

    String getL3IpPort();
    @Data
    class VpcAndPortInfo
    {
        String vpcId;
        String portId;
    }

    @Data
    class InstanceDetailInfo extends ComputeService.InstanceCommonInfo implements Serializable
    {
        private String portId;
        private String flavorId;
    }

    @Data
    class InstanceCommonInfo implements  Serializable
    {
        String instanceId;
        String name;
        Integer phaseStatus;
        String flavorName;
    }

    ComputeService.AgentIpPort getAgentIpPort(@ApiParam(name = "portType") short portType, @ApiParam(name = "vmInstanceId") String vmInstanceId);

    ComputeService.AgentIpPort NfsAgentIpPort(@ApiParam(name = "instanceId") String instanceId);

    @Data
    class AgentIpPort
    {
        String ip;
        String port;
    }

    @Data
    final class Instance implements Serializable
    {
        String instanceId;
        String instanceName;
        String portId;
    }

    @Data
    final class NodeIpAndVmAgentId implements Serializable
    {
        String nodeIp;
        String vmIdFromAgent;
        String status;
    }

    NodeIpAndVmAgentId getNodeIpAndVmAgentIdByVmId(@ApiParam(name = "vmId") String vmId);



    UserIdAndVmSnaps getUserIdAndVmSnaps(@ApiParam(name = "vmId") String vmId);

    @Data
    final class UserIdAndVmSnaps implements Serializable
    {
        String userId;
        Boolean hasVmSnaps;
    }

    @Data
    final class GpuFlavorInfo
    {
        String gpuName;

        Integer gpuCount;
    }
}
