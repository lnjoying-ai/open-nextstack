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

package com.lnjoying.justice.usermanager.domain.dto.response.user;

import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.domain.dto.response.role.RoleDto;
import com.lnjoying.justice.usermanager.domain.model.UserContactInfo;
import com.micro.core.common.Utils;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class UserRsp extends UserBasicRsp {

    @ApiModelProperty(required = true, example = "37423702-051a-46b4-bf2b-f190759cc0b8")
    private String id;

    @ApiModelProperty(required = true)
    private List<RoleDto> permissions;

    private UserContactInfo contact_info;
    /**
     * set tenant response value.
     */
    public void setResponse(TblUserInfo tblUserInfo)
    {
        this.setId(tblUserInfo.getUserId());
        this.setName(tblUserInfo.getUserName());
        UserContactInfo userContactInfo = new UserContactInfo();
        userContactInfo.setEmail(tblUserInfo.getEmail());
        userContactInfo.setPhone(tblUserInfo.getPhone());
        userContactInfo.setAddress(tblUserInfo.getAddress());
        this.setContact_info(userContactInfo);
//        this.set_allowed(tblUserInfo.getIsAllowed());
        this.setIs_allowed(tblUserInfo.getIsAllowed());
        this.setKind(tblUserInfo.getKind()==null?0:tblUserInfo.getKind());
        this.setLevel(tblUserInfo.getLevel()==null?0:tblUserInfo.getLevel());
        this.setStatus(tblUserInfo.getStatus()==null?0:tblUserInfo.getStatus());
        this.setGender(tblUserInfo.getGender()==null?0:tblUserInfo.getGender());
        this.setCreate_time(Utils.formatDate(tblUserInfo.getCreateTime()));
        this.setUpdate_time(Utils.formatDate(tblUserInfo.getUpdateTime()));
        this.setApi_key(tblUserInfo.getApiKey());
    }

    /**
     * set tenant role permission.
     */
    public void setPermission(List<TblRoleInfo> rolePos)
    {
        List<RoleDto> roleDtos = new ArrayList<>();
        for (TblRoleInfo rolePo : rolePos)
        {
            RoleDto dto = new RoleDto();
            dto.setPlatform(rolePo.getPlatform());
            dto.setRole(rolePo.getRole());
            roleDtos.add(dto);
        }
        this.setPermissions(roleDtos);
    }
}
