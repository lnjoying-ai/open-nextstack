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
import com.lnjoying.justice.usermanager.db.model.TblRoleInfoExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TblRoleInfoMapper {
    long countByExample(TblRoleInfoExample example);

    int deleteByExample(TblRoleInfoExample example);

    int deleteByPrimaryKey(Long roleId);

    int insert(TblRoleInfo record);

    int insertSelective(TblRoleInfo record);

    List<TblRoleInfo> selectByExample(TblRoleInfoExample example);

    TblRoleInfo selectByPrimaryKey(Long roleId);

    int updateByExampleSelective(@Param("record") TblRoleInfo record, @Param("example") TblRoleInfoExample example);

    int updateByExample(@Param("record") TblRoleInfo record, @Param("example") TblRoleInfoExample example);

    int updateByPrimaryKeySelective(TblRoleInfo record);

    int updateByPrimaryKey(TblRoleInfo record);
}
