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

package com.lnjoying.justice.schema.service.ums;

import io.swagger.annotations.ApiParam;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ums rpc service
 *
 * @author merak
 **/
public interface UmsService {

    /**
     * Get all roles owned by the current user
     * @param userId
     * @return
     */
    List<String> getRolesByUserId(@ApiParam(name = "userId")String userId);

    /**
     * @Title isBpUser
     * @Description  Check whether the user is in BP
     * @Author Perry
     * @Param: bpId
     * @Param: userId
     * @UpdateTime 2021/11/17
     * @Return: boolean
     * @Throws
     */
    boolean isBpUser(@ApiParam(name = "bpId")String bpId, @ApiParam(name = "userId")String userId);

    User getUser(@ApiParam(name = "userId")String userId);

    @Data
    final class User implements Serializable
    {
        private String userName;
    }

    List<String> getUserIds();

    boolean isAdminUser(@ApiParam(name = "userId")String userId);


    boolean checkApiKey(@ApiParam(name = "principal") String principal, @ApiParam(name = "credential") String credential);
}
