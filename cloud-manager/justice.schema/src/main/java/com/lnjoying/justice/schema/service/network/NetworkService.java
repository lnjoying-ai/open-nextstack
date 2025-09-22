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

package com.lnjoying.justice.schema.service.network;

import com.lnjoying.justice.schema.entity.network.SecurityGroupRule;
import io.swagger.annotations.ApiParam;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * network rpc service
 *
 * @author bruce
 **/

public interface NetworkService {

    Vpc getVpc(@ApiParam(name = "vpcId") String vpcId);
    List<Vpc> getVpcs(@ApiParam(name = "vpcIds") List<String> vpcIds);

    @Data
    final class Vpc implements Serializable {
        private String vpcId;

        private String name;

        private String userId;

        private Integer vlanId;

        private Integer phaseStatus;

        private String phaseInfo;

        private Integer addressType;

        private String cidr;

        private Date createTime;

        private Date updateTime;

        private String vpcIdFromAgent;
    }

    Subnet getSubnet(@ApiParam(name = "subnetId") String subnetId);
    List<Subnet> getSubnets(@ApiParam(name = "subnetIds") List<String> subnetIds);

    @Data
    final class Subnet implements Serializable {
        private String subnetId;

        private String name;

        private String userId;

        private String vpcId;

        private Integer phaseStatus;

        private String phaseInfo;

        private Integer addressType;

        private String cidr;

        private String gatewayIp;

        private Date createTime;

        private Date updateTime;

        private String subnetIdFromAgent;
    }

    DeployNetworkNic getDeployNetworkNic(@ApiParam(name = "nicIdFromAgent") String nicIdFromAgent);

    @Data
    final class DeployNetworkNic implements Serializable {
        private String nicIdFromAgent;
        private Integer phaseStatus;
        private String macAddress;
    }

    TenantNetworkPort getTenantNetworkPort(@ApiParam(name = "portId") String portId);

    List<TenantNetworkPort> getTenantNetworkPorts(@ApiParam(name = "portIds") List<String> portIds);

    @Data
    final class TenantNetworkPort implements Serializable {
        private String portId;
        private String portIdFromAgent;
        private Integer phaseStatus;
        private String macAddress;
        private String ipAddress;
        private String ofport;
        private String vpcCidr;
        private String subnetCidr;
        private String vlanId;
        private String subnetName;
        private String vpcName;
        //        arpingStatus 的状态为 failed、pending、ok
//        private String arpingStatus;
    }

    String setDeployNetwork(@ApiParam(name = "mac") String mac);

    String setTenantNetwork(@ApiParam(name = "mac") String mac,
                            @ApiParam(name = "subnetId") String subnetId,
                            @ApiParam(name = "staticIp")String staticIp);

    String delPortFromTenantNetwork(@ApiParam(name = "portId") String portId);
    List<String> delPortsFromTenantNetwork(@ApiParam(name = "portIds") List<String> portIds);

    // return ok or failed;
    String notifyIpForPort(@ApiParam(name = "portId") String portId,
                  @ApiParam(name = "ip") String ip);

    String delFromDeployNetwork(@ApiParam(name = "nicIdFromAgent") String nicIdFromAgent);

    // getBatch
    List<NetworkDetailInfo> getBatchNetworkInfos(@ApiParam(name = "networkInfoList") List<NetworkDetailInfoReq> networkInfoList);

    NetworkDetailInfo getNetworkDetailInfo(@ApiParam(name = "networkDetailInfoReq") NetworkDetailInfoReq networkDetailInfoReq);

    List<SgInfo> getSgInfos(@ApiParam(name = "instanceId") String instanceId);

    @Data
    final class NetworkDetailInfoReq implements Serializable {
        String vpcId;
        String subnetId;
        String portId;
        String instanceId;
    }

    @Data
    final class NetworkDetailInfo implements Serializable {
        String vpcId;
        String vpcName;
        String vpcCidr;
        String subnetId;
        String subnetName;
        String subnetCidr;
        String portId;
        String ipAddress;
        Boolean isVip;
        String eipId;
        String eip;
        Integer boundPhaseStatus;
        String boundType;
        String publicIp;
//        List<SgInfo> sgInfos;
    }

    @Data
    final class SgInfo implements Serializable
    {
        String sgId;
        String sgName;
        String description;
        List<SecurityGroupRule> rules;
    }

    Boolean isIpInUse(@ApiParam(name = "subnetId") String subnetId,
                      @ApiParam(name = "ip") String ip);

    String createSlot(@ApiParam(name = "addSlotReq") AddSlotReq addSlotReq,
                      @ApiParam(name = "switchId") String switchId);

    String setSlot(@ApiParam(name = "setSlotReq") SetSlotReq setSlotReq,
                   @ApiParam(name = "slotId") String slotId);

    String getSlotPhase(@ApiParam(name = "slotId") String slotId);

    String getSetSlotPhase(@ApiParam(name = "slotId") String slotId);

    String getSwitch(@ApiParam(name = "manageIp")String manageIp);

    @Data
    final class AddSlotReq implements Serializable
    {
        String mac;
        String dev;
    }

    @Data
    final class SetSlotReq implements Serializable
    {
        String vlanids;
        String status;
        String mode;
    }

    @Data
    final class CreatePortReq implements  Serializable
    {
        String subnetId;
        String staticIp;
        String context;
        Boolean isVip;
        String agentId;
    }

    @Data
    final class CreateDnsHostReq implements  Serializable
    {
        String hostname;
        String ip;
    }

    List<String> createPorts(@ApiParam(name = "createPortsReq") List<CreatePortReq> createPortsReqs,
                       @ApiParam(name = "isKvm") boolean isKvm);

    String createPort(@ApiParam(name = "createPortReq") CreatePortReq createPortReq,
                      @ApiParam(name = "isKvm") boolean isKvm, @ApiParam(name = "portType") Integer portType);

    String vmInstanceBoundSgs(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId);

    String vmInstanceApplySgWithSgIds(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId);

    String vmInstanceApplySgs(@ApiParam(name = "vmInstanceId")String vmInstanceId);

    String getApplySgsResult(@ApiParam(name = "vmInstanceId")String vmInstanceId);

    String vmInstanceUpdateSgs(@ApiParam(name = "sgIds") List<String> sgIds, @ApiParam(name = "vmInstanceId")String vmInstanceId);

    String vmInstanceUnBoundSgs(@ApiParam(name = "vmInstanceId")String vmInstanceId);

    List<Long> getEipMapCount(@ApiParam(name = "portIds") List<String> portIds);

    List<String> getPorts(@ApiParam(name = "subnetId") String subnetId);

    List<String> createDnsHosts(@ApiParam(name = "portIds") List<String> portIds,
                                @ApiParam(name = "hostname") String hostname);

//    List<String> updateNodePort(@ApiParam(name = "macs") List<String> macs,
//                                @ApiParam(name = "nodePortId") List<String> nodePortIds);

    List<String> removeDnsHosts(@ApiParam(name = "portIds") List<String> portIds);

    List<String> updateDnsHosts(@ApiParam(name = "portIds") List<String> portIds,
                                @ApiParam(name = "hostname") String hostname);

    long getDnsHosts(@ApiParam(name = "portIds") List<String> portIds);

    NetSummeryInfo  getNatSummery(@ApiParam(name ="userId") String userId, @ApiParam(name="isAdmin") boolean isAdmin);

    @Data
    final class NetSummeryInfo implements  Serializable
    {
        int natCount;
        int eipCount;
        int sgCount;
    }

    String migratePorts(@ApiParam(name = "vmInstanceId") String vmInstanceId,
            @ApiParam(name = "agentIp") String agentIp,
            @ApiParam(name = "agentPort") String agentPort);

    String getMigratePortResult(@ApiParam(name = "vmInstanceId") String vmInstanceId);

    String setEipByPortId(@ApiParam(name = "portId") String portId, @ApiParam(name="eipId") String eipId,
                                 @ApiParam(name = "userId") String userId);
}
