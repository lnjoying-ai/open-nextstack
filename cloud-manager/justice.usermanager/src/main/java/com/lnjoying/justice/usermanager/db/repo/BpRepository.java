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

import com.lnjoying.justice.usermanager.db.mapper.TblBpInfoMapper;
import com.lnjoying.justice.usermanager.db.model.TblBpInfo;
import com.lnjoying.justice.usermanager.db.model.TblBpInfoExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(rollbackFor = {Exception.class})
public class BpRepository
{
    @Autowired
    TblBpInfoMapper tblBpInfoMapper;

    public int insertBp(TblBpInfo tblBpInfo)
    {
        return tblBpInfoMapper.insert(tblBpInfo);
    }

    public int deleteBp(String bpId)
    {
        return tblBpInfoMapper.deleteByPrimaryKey(bpId);
    }

    public int updateBp(TblBpInfo tblBpInfo)
    {
        return tblBpInfoMapper.updateByPrimaryKeySelective(tblBpInfo);
    }

    public TblBpInfo getBpInfo(String bpId)
    {
        return tblBpInfoMapper.selectByPrimaryKey(bpId);
    }


    public List<TblBpInfo> getBpsByExample(TblBpInfoExample example)
    {
        return tblBpInfoMapper.selectByExample(example);
    }

    public long countBpsByExample(TblBpInfoExample example)
    {
        return tblBpInfoMapper.countByExample(example);
    }
}
