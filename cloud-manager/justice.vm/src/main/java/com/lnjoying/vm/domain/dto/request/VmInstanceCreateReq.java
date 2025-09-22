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
import javax.validation.constraints.*;
import java.util.List;

@Data
public class VmInstanceCreateReq
{

    @Valid
    @NotEmpty(message = "networkInfos is required")
    private List<@NotNull NetworkInfo> networkInfos;
//    String vpcId;

    //    @NotNull
//    String subnetId;

    private List<@NotNull DiskInfo> diskInfos;

    //    @NotBlank(message = "imageId is required")
    private String imageId;

    @Min(0)
    @Max(1)
    private Integer imageOsType;

    @Min(60)
    @NotNull
    private Integer rootDisk;

    @NotBlank(message = "flavorId is required")
    private String flavorId;

    //    @NotBlank(message = "hostname is required")
    private String hostname;

    private String cmpTenantId;

    private String cmpUserId;

//    private Boolean hasGpu;

    @NotEmpty(message = "sgIds is required")
    @Valid
    private List<@NotBlank String> sgIds;

    //    @NotBlank(message = "sysUsername is required")
    private String sysUsername;

    private String sysPassword;

    private String pubkeyId;

    private List<String> gpuIds;

    @NotBlank(message = "name cannot  empty")
    @Length(max = 64, message = "name length must be less than 64")
    private String name;

    private String description;

//    private String staticIp;

    private String eipId;

    private String nodeId;

    @NotBlank(message = "storagePoolId is required")
    private String storagePoolId;

    @Data
    public static class NetworkInfo
    {
        @NotBlank(message = "vpcId is required")
        String vpcId;
        @NotBlank(message = "subnetId is required")
        String subnetId;
        String staticIp;
        Boolean isVip;
    }

    @Data
    public static class DiskInfo
    {
        Integer diskType;
        Integer size;
        String volumeId;
//        String volumeName;
    }

    @AssertTrue(message = "volumeId or size is required")
    private boolean isValidDiskInfos()
    {
        if (diskInfos == null || diskInfos.isEmpty())
        {
            return true;
        }
        for (DiskInfo diskInfo : diskInfos)
        {
            if (diskInfo.getDiskType() == null || diskInfo.getSize() == null)
            {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "sysPassword or pubkeyId is required")
    private boolean isValid()
    {
        if (StrUtil.isNotBlank(imageId))
        {
            return !StrUtil.isBlank(sysPassword) || !StrUtil.isBlank(pubkeyId);
        }
        return true;
    }

//    @AssertTrue(message = "sysUsername or hostname is required")
//    private boolean isValidUserHostname()
//    {
//        if (StrUtil.isNotBlank(imageId))
//        {
//            return !StrUtil.isBlank(sysUsername) || !StrUtil.isBlank(hostname);
//        }
//        return true;
//    }

    // 校验的参数必须为true,否则会报错(message为提示信息)
    //用户名不能是root,并且符合linux的用户名规则
    @AssertTrue(message = "username cannot be root")
    private boolean isValidUsername()
    {
        if (StrUtil.isBlank(sysUsername) && StrUtil.isBlank(imageId) && StrUtil.isBlank(hostname))
        {
            return true;
        }
        return !"root".equals(sysUsername) && sysUsername.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{0,29}[a-zA-Z0-9]$");
    }

    //hostname 不符合linux的hostname规则
    @AssertTrue(message = "hostname is invalid")
    private boolean isValidHostname()
    {
        if (StrUtil.isNotBlank(imageId))
        {
            if (StrUtil.isBlank(hostname)) return true;
            if (hostname.equalsIgnoreCase("com") || hostname.equalsIgnoreCase("org") || hostname.equalsIgnoreCase("net") || hostname.equalsIgnoreCase("gov") || hostname.equalsIgnoreCase("edu") || hostname.equalsIgnoreCase("mil") || hostname.equalsIgnoreCase("arpa") || hostname.equalsIgnoreCase("cloud"))
            {
                return false;
            }
            return hostname.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]$");
        }
        return true;
    }

//    @AssertTrue(message = "name is invalid")
//    private boolean isValidName()
//    {
//        return name.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]$");
//    }

}
