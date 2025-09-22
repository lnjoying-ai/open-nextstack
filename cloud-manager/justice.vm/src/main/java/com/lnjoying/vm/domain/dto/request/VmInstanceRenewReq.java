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

package com.lnjoying.vm.domain.dto.request;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class VmInstanceRenewReq
{
    @Valid
    @NotEmpty(message = "networkInfos is required")
    private List<VmInstanceCreateReq.NetworkInfo> networkInfos;
//    String vpcId;

    //    @NotNull
//    String subnetId;

    private List<VmInstanceCreateReq.@NotNull DiskInfo> diskInfos;

    @NotBlank(message = "flavorId is required")
    private String flavorId;

    //    @NotBlank(message = "hostname is required")
    private String hostname;

    @NotEmpty(message = "sgIds is required")
    @Valid
    private List<String> sgIds;

    @NotBlank(message = "sysUsername is required")
    private String sysUsername;

    private String sysPassword;

    private String pubkeyId;

    @NotBlank(message = "name cannot be empty")
    @Length(max = 64, message = "name length must be less than 64")
    private String name;

    private String description;

//    private String staticIp;

    private String nodeId;

    @NotBlank(message = "storagePoolId is required")
    private String storagePoolId;


    @AssertTrue(message = "volumeId or size is required")
    private boolean isValidDiskInfos()
    {
        if (diskInfos == null || diskInfos.isEmpty())
        {
            return false;
        }
        String volumeId = diskInfos.get(0).getVolumeId();
        if (StrUtil.isBlank(volumeId))
        {
            return false;
        }
        if (diskInfos.size() > 1)
        {
            for (int i = 1; i < diskInfos.size(); i++)
            {
                VmInstanceCreateReq.DiskInfo diskInfo = diskInfos.get(i);
                if (StrUtil.isBlank(diskInfo.getVolumeId()) && null == diskInfo.getSize())
                {
                    return false;
                }
            }
        }
        return true;
    }

    //hostname 不符合linux的hostname规则
    @AssertTrue(message = "hostname is invalid")
    private boolean isValidHostname()
    {
        return StrUtil.isBlank(hostname) || hostname.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]$");
    }

    @AssertTrue(message = "name is invalid")
    private boolean isValidName()
    {
        return name.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]$");
    }
}
