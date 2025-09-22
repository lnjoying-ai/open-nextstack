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

package com.lnjoying.justice.usermanager.db.repo;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.usermanager.db.mapper.TblRoleInfoMapper;
import com.lnjoying.justice.usermanager.db.mapper.TblUserInfoMapper;
import com.lnjoying.justice.usermanager.db.mapper.TblUserRoleInfoMapper;
import com.lnjoying.justice.usermanager.db.mapper.UserOperator;
import com.lnjoying.justice.usermanager.db.model.*;
import com.lnjoying.justice.usermanager.domain.dto.response.role.RoleDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(rollbackFor = {Exception.class})
public class UserRepository
{
    @Autowired
    TblUserInfoMapper tblUserInfoMapper;

    @Autowired
    TblRoleInfoMapper tblRoleInfoMapper;

    @Autowired
    TblUserRoleInfoMapper tblUserRoleInfoMapper;

    @Autowired
    UserOperator userOperator;

    public int insertUser(TblUserInfo tblUserInfo)
    {

        return tblUserInfoMapper.insert(tblUserInfo);
    }

    public TblUserInfo getUserById(String userId)
    {
        return tblUserInfoMapper.selectByPrimaryKey(userId);
    }


    public TblUserInfo getUserByPhone(String phone)
    {
        TblUserInfoExample example = new TblUserInfoExample();
        TblUserInfoExample.Criteria criteria = example.createCriteria();
        criteria.andPhoneEqualTo(phone);
        return getUserByExample(example);
    }


    public TblUserInfo getUserByEmail(String email)
    {
        TblUserInfoExample example = new TblUserInfoExample();
        TblUserInfoExample.Criteria criteria = example.createCriteria();
        criteria.andEmailEqualTo(email);
        return getUserByExample(example);
    }

    public TblUserInfo getUserByExample(TblUserInfoExample example)
    {
        List<TblUserInfo> tblUserInfos = tblUserInfoMapper.selectByExample(example);
        if (tblUserInfos != null && !tblUserInfos.isEmpty()) {
            return tblUserInfos.get(0);
        }

        return null;
    }

    public TblUserInfo getUserByKey(String key)
    {
        TblUserInfo tblUserInfo = getUserByUserName(key);
        if (tblUserInfo != null)
        {
            return tblUserInfo;
        }

        tblUserInfo = getUserByPhone(key);
        if (tblUserInfo != null)
        {
            return tblUserInfo;
        }

        tblUserInfo = getUserByEmail(key);
        if (tblUserInfo != null)
        {
            return tblUserInfo;
        }
        return tblUserInfo;
    }

    public TblUserInfo getUserByUserName(String userName)
    {
        TblUserInfoExample example = new TblUserInfoExample();
        TblUserInfoExample.Criteria criteria = example.createCriteria();
        criteria.andUserNameEqualTo(userName);
        return getUserByExample(example);
    }

    public List<TblRoleInfo> getRolesByUserId(String userId)
    {
        return userOperator.getUserRolesByUserId(userId);
    }

    public int updateUserInfo(TblUserInfo tblUserInfo)
    {
        return tblUserInfoMapper.updateByPrimaryKey(tblUserInfo);
    }

    public int deleteUserInfo(String userId)
    {
        return tblUserInfoMapper.deleteByPrimaryKey(userId);
    }

    public int updateUserStatus(String userId, boolean isAllowed)
    {
        TblUserInfo tblUserInfo = new TblUserInfo();
        tblUserInfo.setUserId(userId);
        tblUserInfo.setIsAllowed(isAllowed);
        return tblUserInfoMapper.updateByPrimaryKeySelective(tblUserInfo);
    }

    public List<TblUserInfo> getUsersByExample(TblUserInfoExample example)
    {
        return tblUserInfoMapper.selectByExample(example);
    }

    public long countUsersByExample(TblUserInfoExample example)
    {
        return tblUserInfoMapper.countByExample(example);
    }

    public List<TblRoleInfo> getWholeRoles()
    {
        TblRoleInfoExample example = new TblRoleInfoExample();
        return tblRoleInfoMapper.selectByExample(example);
    }

    public TblRoleInfo getRoleInfo(String platform, String role)
    {
        TblRoleInfoExample example = new TblRoleInfoExample();
        TblRoleInfoExample.Criteria criteria = example.createCriteria();
        criteria.andPlatformEqualTo(platform);
        criteria.andRoleEqualTo(role);
        List<TblRoleInfo> roleInfoList = tblRoleInfoMapper.selectByExample(example);
        if (roleInfoList == null) return null;
        return roleInfoList.get(0);
    }

    public int insertUserRoleInfo(TblUserRoleInfoKey tblUserRoleInfoKey)
    {
        return tblUserRoleInfoMapper.insert(tblUserRoleInfoKey);
    }

    public void addRolesByUserId(String userId, List<RoleDto> roleDtoList) throws WebSystemException
    {
        TblUserInfo tblUserInfo = getUserById(userId);
        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

        for (RoleDto roleDto : roleDtoList)
        {
            try
            {
                TblRoleInfo tblRoleInfo = getRoleInfo(roleDto.getPlatform(), roleDto.getRole());
                if (tblRoleInfo == null) continue;
                TblUserRoleInfoKey tblUserRoleInfoKey = new TblUserRoleInfoKey();
                tblUserRoleInfoKey.setRoleId(tblRoleInfo.getRoleId());
                tblUserRoleInfoKey.setUserId(tblUserInfo.getUserId());
                insertUserRoleInfo(tblUserRoleInfoKey);
            }
            catch (DuplicateKeyException e)
            {
                continue;
            }
        }
    }

    public TblUserInfo getUserByApiKey(String apiKey)
    {
        TblUserInfoExample example = new TblUserInfoExample();
        TblUserInfoExample.Criteria criteria = example.createCriteria();
        criteria.andApiKeyEqualTo(apiKey);
        return getUserByExample(example);
    }
}
