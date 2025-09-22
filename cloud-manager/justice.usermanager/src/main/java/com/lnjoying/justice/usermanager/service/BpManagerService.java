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

package com.lnjoying.justice.usermanager.service;

import com.google.gson.Gson;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.common.LJ_Function;
import com.lnjoying.justice.usermanager.common.constant.UMStatus;
import com.lnjoying.justice.usermanager.common.constant.UserKind;
import com.lnjoying.justice.usermanager.common.constant.UserLevel;
import com.lnjoying.justice.usermanager.db.model.TblBpInfo;
import com.lnjoying.justice.usermanager.db.model.TblBpInfoExample;
import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.repo.BpRepository;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import com.lnjoying.justice.usermanager.domain.dto.request.bp.BpRawReq;
import com.lnjoying.justice.usermanager.domain.dto.request.bp.Bp_Contacts_Info;
import com.lnjoying.justice.usermanager.domain.dto.response.bp.BpRsp;
import com.lnjoying.justice.usermanager.domain.dto.response.bp.QueryBpsRsp;
import com.lnjoying.justice.usermanager.domain.dto.response.role.RoleDto;
import com.lnjoying.justice.usermanager.domain.model.search.BpSearchCritical;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("bpManagerService")
@Slf4j
public class BpManagerService {

//    private static final Logger log = LoggerFactory.getLogger(BpManagerService.class);

    @Autowired
    private BpRepository bpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Pbkdf2PasswordEncoder passwordEncoder;

    Gson gson = new Gson();


    public Object addBp(BpRawReq bpRawReq) throws WebSystemException
    {
        try
        {
            log.info("add bp {}", bpRawReq);
            if (StringUtils.isEmpty(bpRawReq.getName()))
            {
                throw new WebSystemException(ErrorCode.BP_NAME_INVALID, ErrorLevel.INFO);
            }

            if (StringUtils.isEmpty(bpRawReq.getLicense_id()))
            {
                throw new WebSystemException(ErrorCode.BP_LIC_INVALID, ErrorLevel.INFO);
            }

            TblBpInfo tblBpInfo = new TblBpInfo();
            tblBpInfo.setBpId(bpRawReq.getLicense_id());
            tblBpInfo.setBpName(bpRawReq.getName());
            tblBpInfo.setLicenseId(bpRawReq.getLicense_id());
            tblBpInfo.setStatus(bpRawReq.getStatus());

            TblUserInfo tblUserInfo = null;
            LJ_Function<TblUserInfo> lj_function = null;
            if (! StringUtils.isEmpty(bpRawReq.getMaster_user()))
            {
                tblUserInfo = userRepository.getUserByUserName(bpRawReq.getMaster_user());

                if (tblUserInfo == null)
                {
                    tblUserInfo = new TblUserInfo();
                    tblUserInfo.setUserId(Utils.assignUUId());
                    tblUserInfo.setUserName(bpRawReq.getMaster_user());
                    tblUserInfo.setPassword(passwordEncoder.encode(bpRawReq.getLicense_id()));
                    tblUserInfo.setStatus(UMStatus.ACTIVE);
                    lj_function = tbl -> {
                        userRepository.insertUser(tbl);
                        // Initialize bp role
                        initBpRole(tbl);
                    };
                }
                else
                {
                    lj_function = tbl -> userRepository.updateUserInfo(tbl);
                }

                if (tblUserInfo != null)
                {
                    tblBpInfo.setMasterUser(tblUserInfo.getUserName());
                    tblUserInfo.setBpId(tblBpInfo.getBpId());
                }
            }


            tblBpInfo.setStatus(bpRawReq.getStatus());
            tblBpInfo.setWebsite(bpRawReq.getWebsite());
            tblBpInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblBpInfo.setUpdateTime(tblBpInfo.getCreateTime());
            if (bpRawReq.getContact_info() != null)
            {
                tblBpInfo.setContactInfo(gson.toJson(bpRawReq.getContact_info()));
            }

            int result = bpRepository.insertBp(tblBpInfo);


            if (result > 0 && tblUserInfo != null && lj_function != null)
            {
                lj_function.operator(tblUserInfo);
            }

            if (result > 0)
            {
                Map<String, String> retValue = new HashMap<>();
                retValue.put("id", tblBpInfo.getBpId());
                return retValue;
            }
            else
            {
                log.error("User register failed");
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
        }
        catch (DuplicateKeyException e)
        {
            e.printStackTrace();
            throw new WebSystemException(ErrorCode.BP_KEY_Occupied, ErrorLevel.INFO, e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO, e.getMessage());
        }
    }

    public long deleteBp(String bpId) throws WebSystemException
    {
        log.info("del bp {}", bpId);
        TblBpInfo tblBpInfo = bpRepository.getBpInfo(bpId);
        if (tblBpInfo == null)
        {
            throw new WebSystemException(ErrorCode.BP_NOT_EXIST, ErrorLevel.INFO);
        }

        tblBpInfo.setStatus(UMStatus.REMOVED);
        return bpRepository.updateBp(tblBpInfo);
    }

    public Object getBpDtoInfo(String bpId) throws WebSystemException
    {
        log.info("get bp {}", bpId);

        TblBpInfo tblBpInfo = bpRepository.getBpInfo(bpId);

        if (tblBpInfo == null)
        {
            throw new WebSystemException(ErrorCode.BP_NOT_EXIST, ErrorLevel.INFO);
        }

        BpRsp bpRsp = new BpRsp();
        bpRsp.setBpInfo(tblBpInfo);
        if (! StringUtils.isEmpty(tblBpInfo.getContactInfo()))
        {
            bpRsp.setContact_info(gson.fromJson(tblBpInfo.getContactInfo(), Bp_Contacts_Info.class));
        }

        return bpRsp;
    }

    public long updateBpInfo(String bpId, BpRawReq bpRawReq) throws WebSystemException
    {
        log.info("update bp {} raw info: {}", bpId, bpRawReq);
        TblBpInfo tblBpInfo = bpRepository.getBpInfo(bpId);
        if (tblBpInfo == null)
        {
            throw new WebSystemException(ErrorCode.BP_NOT_EXIST, ErrorLevel.INFO);
        }

        tblBpInfo.setBpName(bpRawReq.getName());
//        tblBpInfo.setLicenseId(bpRawReq.getLicense_id());
        tblBpInfo.setMasterUser(bpRawReq.getMaster_user());
        tblBpInfo.setStatus(bpRawReq.getStatus());
        tblBpInfo.setWebsite(bpRawReq.getWebsite());
        tblBpInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (bpRawReq.getContact_info() != null)
        {
            tblBpInfo.setContactInfo(gson.toJson(bpRawReq.getContact_info()));
        }

        TblUserInfo tblUserInfo = null;
        LJ_Function<TblUserInfo> lj_function = null;
        if (! StringUtils.isEmpty(bpRawReq.getMaster_user()))
        {
            tblUserInfo = userRepository.getUserByUserName(bpRawReq.getMaster_user());

            if (tblUserInfo == null)
            {
                tblUserInfo = new TblUserInfo();
                tblUserInfo.setUserId(Utils.assignUUId());
                tblUserInfo.setPassword(passwordEncoder.encode(bpRawReq.getLicense_id()));
                tblUserInfo.setStatus(UMStatus.ACTIVE);
                tblUserInfo.setLevel(UserLevel.LEVEL1);
                tblUserInfo.setIsAllowed(true);
                lj_function = tbl -> userRepository.insertUser(tbl);
            }
            else
            {
                lj_function = tbl -> userRepository.updateUserInfo(tbl);
            }

            if (tblUserInfo != null)
            {
                tblUserInfo.setKind(UserKind.BP);
                tblUserInfo.setBpId(tblBpInfo.getBpId());
                tblUserInfo.setUserName(bpRawReq.getMaster_user());
                tblBpInfo.setMasterUser(bpRawReq.getMaster_user());
            }
        }

        int ret =  bpRepository.updateBp(tblBpInfo);
        if (ret > 0)
        {
            lj_function.operator(tblUserInfo);
        }

        return ret;
    }

    TblBpInfoExample setBpInfoExample(BpSearchCritical critical)
    {
        TblBpInfoExample example = new TblBpInfoExample();
        TblBpInfoExample.Criteria criteria = example.createCriteria();
        criteria.andStatusNotEqualTo(UMStatus.REMOVED);
        if (critical.getName() != null) criteria.andBpNameEqualTo(critical.getName());
        return example;
    }

    public Object getBpDtoInfos(BpSearchCritical critical) throws WebSystemException
    {
        try
        {
            TblBpInfoExample example = setBpInfoExample(critical);
            QueryBpsRsp queryBpsRsp = new QueryBpsRsp();

            Long total_num = bpRepository.countBpsByExample(example);
            queryBpsRsp.setTotal_num(total_num.intValue());
            if (total_num < 1)
            {
                return queryBpsRsp;
            }

            int begin = ((critical.getPageNum()-1) * critical.getPageSize());
            example.setOrderByClause("update_time desc");
            example.setStartRow(begin);
            example.setPageSize(critical.getPageSize());


            List<TblBpInfo> bpInfoList = bpRepository.getBpsByExample(example);
            if (bpInfoList == null)
            {
                return queryBpsRsp;
            }

            List<BpRsp> bpRspList = new ArrayList<>();
            for (TblBpInfo tblBpInfo : bpInfoList)
            {
                BpRsp bpRsp = new BpRsp();
                bpRsp.setBpInfo(tblBpInfo);
                if (! StringUtils.isEmpty(tblBpInfo.getContactInfo()))
                {
                    bpRsp.setContact_info(gson.fromJson(tblBpInfo.getContactInfo(), Bp_Contacts_Info.class));
                }
                bpRspList.add(bpRsp);
            }
            queryBpsRsp.setBps(bpRspList);
            return queryBpsRsp;
        }
        catch (Exception e)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
    }

    private void initBpRole(TblUserInfo tbl) {
        List<TblRoleInfo> wholeRoles = userRepository.getWholeRoles();
        if (CollectionUtils.isNotEmpty(wholeRoles)) {
            List<TblRoleInfo> tenantAdminRoles = wholeRoles.stream().filter(role -> role.getRole().equalsIgnoreCase("TENANT_ADMIN"))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(tenantAdminRoles)) {
                List<RoleDto> collect = tenantAdminRoles.stream().map(x -> {
                    RoleDto roleDto = new RoleDto();
                    roleDto.setRole(x.getRole());
                    roleDto.setPlatform(x.getPlatform());
                    return roleDto;
                }).collect(Collectors.toList());
                try {
                    userRepository.addRolesByUserId(tbl.getUserId(), collect);
                } catch (WebSystemException e) {
                    // Initial authorization failed, manual authorization
                   log.error("init bp role failed, need manual authorization");
                }
            }
        }
    }
}
