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

package com.lnjoying.justice.usermanager.db.mapper;

import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserOperator
{
//    List<UserRole> getUserRolesByUserId(String userId);

    @Results(id = "role", value = {
            @Result(column = "id", property = "roleId"),
            @Result(column = "platform", property = "platform"),
            @Result(column = "role", property = "role")
    })
    @Select("select a.role_id as id, platform, role from tbl_user_role_info a left join tbl_role_info b on a.role_id=b.role_id where a.user_id=#{userId}")
    public List<TblRoleInfo> getUserRolesByUserId(@Param("userId") String userId);
}
