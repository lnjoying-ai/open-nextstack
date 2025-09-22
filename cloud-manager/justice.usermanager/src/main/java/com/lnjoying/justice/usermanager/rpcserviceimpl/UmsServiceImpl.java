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

package com.lnjoying.justice.usermanager.rpcserviceimpl;

import com.lnjoying.justice.schema.constant.RoleConstants;
import com.lnjoying.justice.schema.service.ums.UmsService;
import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfoExample;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import com.lnjoying.justice.usermanager.domain.dto.response.role.RoleDto;
import com.lnjoying.justice.usermanager.service.UserManagerService;
import io.swagger.annotations.ApiParam;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  Rpc implementation class
 *
 * @author merak
 **/
@RpcSchema(schemaId = "umsService")
public class UmsServiceImpl implements UmsService {

    @Autowired
    private UserManagerService userManagerService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<String> getRolesByUserId(@ApiParam(name = "userId")String userId) {
        List<RoleDto> rolesByUserId = userManagerService.getRolesByUserId(userId);
        if (CollectionUtils.isNotEmpty(rolesByUserId)) {
            List<String> roles = rolesByUserId.stream().map(roleDto ->
                    Strings.toUpperCase("role" + "_" + roleDto.getPlatform() + "_" + roleDto.getRole()))
                    .collect(Collectors.toList());
            return roles;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isBpUser(@ApiParam(name = "bpId")String bpId, @ApiParam(name = "userId")String userId)
    {
        if (null == userId || userId.isEmpty())
        {
            return false;
        }
        if (null == bpId || bpId.isEmpty())
        {
            return false;
        }
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        return null != tblUserInfo && null != tblUserInfo.getBpId() && !tblUserInfo.getBpId().isEmpty() && tblUserInfo.getBpId().equals(bpId);
    }

    @Override
    public User getUser(@ApiParam(name = "userId")String userId)
    {
        if (null == userId || userId.isEmpty())
        {
            return null;
        }

        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (null == tblUserInfo)
        {
            return null;
        }

        User user = new User();
        user.setUserName(tblUserInfo.getUserName());
        return user;
    }

    @Override
    public List<String> getUserIds()
    {
        TblUserInfoExample example = new TblUserInfoExample();
//        TblUserInfoExample.Criteria criteria = example.createCriteria();
        List<TblUserInfo> userInfos = userRepository.getUsersByExample(example);
        return userInfos.stream().map(TblUserInfo::getUserId).collect(Collectors.toList());
    }

    @Override
    public  boolean isAdminUser(@ApiParam(name = "userId") String userId)
    {

        if (null == userId || userId.isEmpty())
        {
            return false;
        }

        for (TblRoleInfo tblRoleInfo :userRepository.getRolesByUserId(userId))
        {
            String userRole = "ROLE_"+ tblRoleInfo.getPlatform() +"_" +tblRoleInfo.getRole();
            if (RoleConstants.ROLE_ALL_ADMIN.equals(userRole))
            {
                return true;
            }
        }

        return false;
    }


//    public CompletableFuture<Boolean> checkApiKeyAsync(@ApiParam(name = "principal")String principal, @ApiParam(name = "credential")String credential)
//    {
//        return CompletableFuture.supplyAsync(()->
//        {
//            return "admin".equals(principal) && "admin".equals(credential);
//        });
//    }

    @Override
    public boolean checkApiKey(@ApiParam(name = "principal") String principal, @ApiParam(name = "credential") String credential)
    {
        return "admin".equals(principal) && "admin".equals(credential);
    }

}
