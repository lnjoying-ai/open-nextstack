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

import com.lnjoying.justice.usermanager.db.model.TblUserRoleInfoExample;
import com.lnjoying.justice.usermanager.db.model.TblUserRoleInfoKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TblUserRoleInfoMapper {
    long countByExample(TblUserRoleInfoExample example);

    int deleteByExample(TblUserRoleInfoExample example);

    int deleteByPrimaryKey(TblUserRoleInfoKey key);

    int insert(TblUserRoleInfoKey record);

    int insertSelective(TblUserRoleInfoKey record);

    List<TblUserRoleInfoKey> selectByExample(TblUserRoleInfoExample example);

    int updateByExampleSelective(@Param("record") TblUserRoleInfoKey record, @Param("example") TblUserRoleInfoExample example);

    int updateByExample(@Param("record") TblUserRoleInfoKey record, @Param("example") TblUserRoleInfoExample example);
}
