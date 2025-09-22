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

package com.lnjoying.vm.domain.dto.response;

import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.justice.schema.service.repo.FlavorService;
import com.lnjoying.justice.schema.service.repo.VolumeService;
import com.lnjoying.vm.entity.VmInstance;
import com.micro.core.common.Utils;
import lombok.Data;

import java.util.List;

@Data
public class VmInstanceDetailInfoRsp
{
    private String instanceId;

    private String name;

    private Integer phaseStatus;

    private String imageId;

    private String imageName;

    private Integer imageOsType;

    private String vpcId;

    private String subnetId;

    private String description;

    private String hostname;

    private String sysUsername;

    private String pubkeyId;

    private String hypervisorNodeId;

    private String hypervisorNodeName;

    private String hypervisorNodeIp;

    private String userId;

    private String flavorId;

    private String flavorName;

    private String volumeId;

    private int cpu;

    private int mem;

    private int rootDisk;

    private List<NetworkService.SgInfo> sgInfos;

    private List<SnapInfo> snapInfos;

    private List<NetworkService.NetworkDetailInfo> networkDetailInfos;

    private List<VolumeService.VolumeInfo> diskInfos;

    private String createTime;

    private String updateTime;

    private List<PciDeviceInfo> pciInfos;

    private String bootDev;

    private Boolean ib;

    @Data
    public static class SnapInfo
    {
        String snapName;
        String snapId;
        Integer phaseStatus;
        String createTime;
        String updateTime;
        Boolean isCurrent;
    }


    public void setInstanceDetailInfo(VmInstance tblVmInstance, FlavorService.FlavorInfo tblFlavor)
    {
        if (null == tblVmInstance || null == tblFlavor)
        {
            return;
        }
        this.ib = null != tblFlavor.getNeedIb() && tblFlavor.getNeedIb();
        this.instanceId = tblVmInstance.getVmInstanceId();
        this.name = tblVmInstance.getName();
        this.hypervisorNodeId = tblVmInstance.getNodeId();
        this.phaseStatus = tblVmInstance.getPhaseStatus();
        this.imageId = tblVmInstance.getImageId();
        this.volumeId = tblVmInstance.getVolumeId();
        //this.image_name
        this.vpcId = tblVmInstance.getVpcId();
        //this.vpc_name
        this.subnetId = tblVmInstance.getSubnetId();
        //this.subnet_name
        //this.nic_infos
        this.bootDev = tblVmInstance.getBootDev();
        if (StrUtil.isBlank(bootDev) || bootDev.equals("hd"))
        {
            this.bootDev = "hd";
        }
        this.userId = tblVmInstance.getUserId();
        this.description = tblVmInstance.getDescription();
        this.hostname = tblVmInstance.getHostName();
        this.sysUsername = tblVmInstance.getSysUsername();
        this.pubkeyId = tblVmInstance.getPubkeyId();
        // flavor
        this.flavorId = tblFlavor.getFlavorId();
        this.flavorName = tblFlavor.getName();
        if (null != tblVmInstance.getCpuCount() && tblVmInstance.getCpuCount() > 0)
        {
            this.cpu = tblVmInstance.getCpuCount();
        }
        else
        {
            this.cpu = tblFlavor.getCpu();
        }
        if (null != tblVmInstance.getMemSize() && tblVmInstance.getMemSize() > 0)
        {
            this.mem = tblVmInstance.getMemSize();
        }
        else
        {
            this.mem = tblFlavor.getMem();
        }
        this.rootDisk = tblVmInstance.getRootDisk();
        this.createTime = Utils.formatDate(tblVmInstance.getCreateTime());
        this.updateTime = Utils.formatDate(tblVmInstance.getUpdateTime());
    }
}
