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

import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfoExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TblUserInfoMapper {
    long countByExample(TblUserInfoExample example);

    int deleteByExample(TblUserInfoExample example);

    int deleteByPrimaryKey(String userId);

    int insert(TblUserInfo record);

    int insertSelective(TblUserInfo record);

    List<TblUserInfo> selectByExample(TblUserInfoExample example);

    TblUserInfo selectByPrimaryKey(String userId);

    int updateByExampleSelective(@Param("record") TblUserInfo record, @Param("example") TblUserInfoExample example);

    int updateByExample(@Param("record") TblUserInfo record, @Param("example") TblUserInfoExample example);

    int updateByPrimaryKeySelective(TblUserInfo record);

    int updateByPrimaryKey(TblUserInfo record);
}
