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

package com.lnjoying.justice.network.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lnjoying.justice.network.domain.dto.response.SecurityGroupRspVo;
import com.lnjoying.justice.network.entity.SecurityGroup;
import com.lnjoying.justice.network.mapper.SecurityGroupMapper;
import com.lnjoying.justice.network.service.SecurityGroupService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author george
 * @since 2023-01-04
 */
@Service
public class SecurityGroupServiceImpl extends ServiceImpl<SecurityGroupMapper, SecurityGroup> implements SecurityGroupService {
    @Resource
    private SecurityGroupMapper securityGroupMapper;

    public List<SecurityGroupRspVo> getSecurityGroups(String userId, String name, String sgId, int phaseStatus, boolean phaseStatusIsEqual, Integer pageSize, Integer startRow)
    {
        return securityGroupMapper.selectByUserId(userId, name, sgId, phaseStatus, phaseStatusIsEqual, pageSize, startRow);
    }

    public long countSecurityGroupBySearch(String userId, String name, String sgId, int phaseStatus, boolean phaseStatusIsEqual)
    {
        return securityGroupMapper.countByUserId(userId,name,sgId,phaseStatus, phaseStatusIsEqual);
    }
}
