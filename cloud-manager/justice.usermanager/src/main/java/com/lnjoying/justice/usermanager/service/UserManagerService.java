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

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.usermanager.common.constant.UMStatus;
import com.lnjoying.justice.usermanager.common.constant.UserKind;
import com.lnjoying.justice.usermanager.common.constant.UserLevel;
import com.lnjoying.justice.usermanager.config.SmsConfig;
import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfoExample;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import com.lnjoying.justice.usermanager.domain.dto.request.user.*;
import com.lnjoying.justice.usermanager.domain.dto.response.role.ComponentRoleDto;
import com.lnjoying.justice.usermanager.domain.dto.response.role.RoleDto;
import com.lnjoying.justice.usermanager.domain.dto.response.user.ApiKeyRsp;
import com.lnjoying.justice.usermanager.domain.dto.response.user.QueryUsersRsp;
import com.lnjoying.justice.usermanager.domain.dto.response.user.UniquenessRsp;
import com.lnjoying.justice.usermanager.domain.dto.response.user.UserRsp;
import com.lnjoying.justice.usermanager.domain.model.UserContactInfo;
import com.lnjoying.justice.usermanager.domain.model.search.UserSearchCritical;
import com.lnjoying.justice.usermanager.utils.Md5Util;
import com.lnjoying.justice.usermanager.utils.ServiceCombRequestUtils;
import com.micro.core.common.Utils;
import com.micro.core.persistence.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lnjoying.justice.usermanager.common.constant.UserKind.*;

@Service("userManagerService")
@Slf4j
public class UserManagerService {

//    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagerService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Pbkdf2PasswordEncoder passwordEncoder;

    @Autowired
    private SmsConfig smsConfig;

    /**
     * register user info by telephone, verification code and so on.
     *
     * @param reqParam reqParam
     * @return
     */
    public Object addUser(UserAddReq reqParam) throws WebSystemException
    {
        try
        {
            log.info("Begin register user");
            UserContactInfo userContactInfo = reqParam.getContact_info();
            if (userContactInfo != null )
            {
                if (!StringUtils.isEmpty(userContactInfo.getPhone())
                        && userRepository.getUserByPhone(userContactInfo.getPhone()) != null)
                {
                    throw new WebSystemException(ErrorCode.PhoneOccupied, ErrorLevel.INFO);
                }

                if (!StringUtils.isEmpty(userContactInfo.getEmail())
                        && userRepository.getUserByEmail(userContactInfo.getEmail()) != null)
                {
                    throw new WebSystemException(ErrorCode.EmailOccupied, ErrorLevel.INFO);
                }
            }

            if (StringUtils.isEmpty(reqParam.getName()))
            {
                throw new WebSystemException(ErrorCode.InvalidUsername, ErrorLevel.INFO);
            }

            if (!StringUtils.isEmpty(reqParam.getName())
                    && userRepository.getUserByUserName(reqParam.getName()) != null)
            {
                throw new WebSystemException(ErrorCode.DuplicateUser, ErrorLevel.INFO);
            }

            TblUserInfo tblUserInfo = new TblUserInfo();
            tblUserInfo.setUserId(Utils.assignUUId());
            if (!StringUtils.isEmpty(reqParam.getBp_id()))
            {
                tblUserInfo.setBpId(reqParam.getBp_id());
            }
            tblUserInfo.setUserName(reqParam.getName());
            tblUserInfo.setIsAllowed(reqParam.is_allowed());
            tblUserInfo.setStatus(reqParam.getStatus());
            tblUserInfo.setKind(reqParam.getKind());
            tblUserInfo.setLevel(reqParam.getLevel());


            if (userContactInfo != null)
            {
                if (!StringUtils.isEmpty(userContactInfo.getEmail()))
                {
                    tblUserInfo.setEmail(userContactInfo.getEmail());
                }

                if (!StringUtils.isEmpty(userContactInfo.getPhone()))
                {
                    tblUserInfo.setPhone(userContactInfo.getPhone());
                }

                if (!StringUtils.isEmpty(userContactInfo.getAddress()))
                {
                    tblUserInfo.setAddress(userContactInfo.getAddress());
                }
            }

            int result = 0;

            tblUserInfo.setGender(reqParam.getGender());
            tblUserInfo.setPassword(passwordEncoder.encode(reqParam.getPassword()));
            tblUserInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblUserInfo.setUpdateTime(tblUserInfo.getCreateTime());
            result = userRepository.insertUser(tblUserInfo);

            // init tenant role
            initTenantRole(tblUserInfo);

            if (result > 0)
            {
                Map<String, String> retValue = new HashMap<>();
                retValue.put("id", tblUserInfo.getUserId());
                return retValue;
            }
            else
            {
                log.error("User register failed");
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
        }
        catch (DuplicateKeyException e)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO, e.getMessage());
        }
        catch (Exception e)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO, e.getMessage());
        }
    }

    public Object register(UserRegReq reqParam) throws WebSystemException
    {
        try
        {
            log.info("Begin register user");
            UserContactInfo userContactInfo = reqParam.getContact_info();
            if (userContactInfo != null )
            {
                if (!StringUtils.isEmpty(userContactInfo.getPhone())
                        && userRepository.getUserByPhone(userContactInfo.getPhone()) != null)
                {
                    throw new WebSystemException(ErrorCode.PhoneOccupied, ErrorLevel.INFO);
                }

                if (!StringUtils.isEmpty(userContactInfo.getEmail())
                        && userRepository.getUserByEmail(userContactInfo.getEmail()) != null)
                {
                    throw new WebSystemException(ErrorCode.EmailOccupied, ErrorLevel.INFO);
                }
            }

            if (StringUtils.isEmpty(reqParam.getName()))
            {
                throw new WebSystemException(ErrorCode.InvalidUsername, ErrorLevel.INFO);
            }

            if (!StringUtils.isEmpty(reqParam.getName())
                    && userRepository.getUserByUserName(reqParam.getName()) != null)
            {
                throw new WebSystemException(ErrorCode.DuplicateUser, ErrorLevel.INFO);
            }

            TblUserInfo tblUserInfo = new TblUserInfo();
            tblUserInfo.setUserId(Utils.assignUUId());
            if (!StringUtils.isEmpty(reqParam.getBp_id()))
            {
                tblUserInfo.setBpId(reqParam.getBp_id());
            }
            tblUserInfo.setUserName(reqParam.getName());
            tblUserInfo.setIsAllowed(true);
            tblUserInfo.setStatus(UMStatus.ACTIVE);


            if (userContactInfo != null)
            {
                if (!StringUtils.isEmpty(userContactInfo.getEmail()))
                {
                    tblUserInfo.setEmail(userContactInfo.getEmail());
                }

                if (!StringUtils.isEmpty(userContactInfo.getPhone()))
                {
                    tblUserInfo.setPhone(userContactInfo.getPhone());
                }

                if (!StringUtils.isEmpty(userContactInfo.getAddress()))
                {
                    tblUserInfo.setAddress(userContactInfo.getAddress());
                }
            }

            int result = 0;

            tblUserInfo.setGender(reqParam.getGender());
            if (StringUtils.isEmpty(reqParam.getBp_id()))
            {
                tblUserInfo.setKind(PERSONAL);
            }
            else
            {
                tblUserInfo.setKind(UserKind.BP);
            }

            tblUserInfo.setLevel(UserLevel.LEVEL1);
            tblUserInfo.setPassword(passwordEncoder.encode(reqParam.getPassword()));
            tblUserInfo.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblUserInfo.setUpdateTime(tblUserInfo.getCreateTime());
            result = userRepository.insertUser(tblUserInfo);

            // Initialize user role
            initTenantRole(tblUserInfo);

            if (result > 0)
            {
                Map<String, String> retValue = new HashMap<>();
                retValue.put("id", tblUserInfo.getUserId());
                return retValue;
            }
            else
            {
                log.error("User register failed");
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }

        }
        catch (DuplicateKeyException e)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO, e.getMessage());
        }
        catch (Exception e)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO, e.getMessage());
        }
    }

    /**
     * whether the verification code is correct.
     *
     * @param verificationCode verificationCode
     * @param telephone telephone
     * @return
     */
    private boolean verifyVerificationCode(String verificationCode, String telephone)
    {
        String code = RedisUtil.get(RedisCacheField.AUTH_VER_CODE, telephone);
        if (code != null && verificationCode.equals(code))
        {
            return true;
        }
        return false;
    }


    public void retrievePassword(RetrievePasswordReq retireveRequest) throws IOException
    {
        log.info("Begin retrieve password");
        String telephone = retireveRequest.getPhone();
        String email = retireveRequest.getPhone();
        String verificationCode = retireveRequest.getVerification_code();
        TblUserInfo tblUserInfo = null;
        String method = telephone;
        if (telephone != null)
        {
            //username is not exit
            tblUserInfo = userRepository.getUserByPhone(telephone);

            if (tblUserInfo == null)
            {
                log.error("Telephone not exist");
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);

            }
        }
        else if (email != null)
        {
            method = email;
            tblUserInfo = userRepository.getUserByEmail(email);
            if (tblUserInfo == null)
            {
                log.error("Telephone not exist");
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
            }
        }


        if (!verifyVerificationCode(verificationCode, method))
        {
            log.error("verification code is error ");
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }

        int result = 0;
        try
        {
            tblUserInfo.setPassword(passwordEncoder.encode(retireveRequest.getNew_password()));
            result += userRepository.updateUserInfo(tblUserInfo);
        } catch (Exception e) {
            log.error("Database Operate Exception: {}", e.getMessage());
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }

        if (result > 0)
        {
            log.info("Modify password success");
            return;
        }
        else
        {
            log.error("Modify password failed");
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
    }

    public void updatePassword(String userId, UpdatePasswordReq updatePasswordReq) throws IOException
    {
        log.info("Begin retrieve password");
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

        if (! passwordEncoder.matches(updatePasswordReq.getOld_password(), tblUserInfo.getPassword()))
        {
            throw new WebSystemException(ErrorCode.InvalidOldPasswd, ErrorLevel.INFO);
        }

        int result = 0;
        try
        {
            tblUserInfo.setPassword(passwordEncoder.encode(updatePasswordReq.getNew_password()));
            result += userRepository.updateUserInfo(tblUserInfo);
        } catch (Exception e) {
            log.error("Database Operate Exception: {}", e.getMessage());
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }

        if (result > 0)
        {
            log.info("Modify password success");
            return;
        }
        else
        {
            log.error("Modify password failed");
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
    }

    /**
     * Verify that the username and telephone number and mail address exist.
     *
     * @param uniqueRequest UniqueReq
     * @return
     */
    public UniquenessRsp uniqueness(UniqueReq uniqueRequest)
    {
        String mailAddress = uniqueRequest.getEmail();
        String telephone = uniqueRequest.getPhone();
        String username = uniqueRequest.getUsername();
        UniquenessRsp uniquenessResponse = new UniquenessRsp();

        if (mailAddress != null && mailAddress.length() > 1 && userRepository.getUserByEmail(mailAddress) != null)
        {
            uniquenessResponse.setMailAddress(true);
        }

        if (telephone != null && telephone.length() > 1 && userRepository.getUserByPhone(telephone) != null)
        {
            uniquenessResponse.setTelephone(true);
        }

        if (username != null && username.length() > 1 && userRepository.getUserByUserName(username) != null)
        {
            uniquenessResponse.setUsername(true);
        }

        return uniquenessResponse;
    }

    /**
     * delete user by id.
     *
     * @param userId
     */
    public boolean deleteUser(String userId) throws WebSystemException
    {
        try
        {
            TblUserInfo tblUserInfo = userRepository.getUserById(userId);
            if (tblUserInfo == null)
            {
                throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
            }
            tblUserInfo.setStatus(UMStatus.REMOVED);
            String userName = tblUserInfo.getUserId() + tblUserInfo.getUserName();
            if (userName.length()>63) userName = userName.substring(0,63);
            tblUserInfo.setUserName(userName);

            if (tblUserInfo.getPhone() != null)
            {
                String phone = tblUserInfo.getUserId() + tblUserInfo.getPhone();
                if (phone.length() > 31) phone = phone.substring(0, 31);
                tblUserInfo.setPhone(phone);
            }

            if (tblUserInfo.getEmail() != null)
            {
                String email = tblUserInfo.getUserId() + tblUserInfo.getEmail();
                if (email.length() > 63) email = email.substring(0, 63);
                tblUserInfo.setEmail(email);
            }
            userRepository.updateUserInfo(tblUserInfo);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public int updateUser(String userId, UserUpdateReq req) throws WebSystemException
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

        UserContactInfo userContactInfo = req.getContact_info();

        if (userContactInfo != null )
        {
            TblUserInfo tblUserInfoT = null;
            if (!StringUtils.isEmpty(userContactInfo.getPhone()) && ! StringUtils.equals(tblUserInfo.getPhone(), userContactInfo.getPhone()))
            {
                tblUserInfoT = userRepository.getUserByPhone(userContactInfo.getPhone());
            }

            if (tblUserInfoT != null && ! tblUserInfoT.getUserId().equals(userId))
            {
                throw new WebSystemException(ErrorCode.PhoneOccupied, ErrorLevel.INFO);
            }

            if (!StringUtils.isEmpty(userContactInfo.getEmail()) && ! StringUtils.equals(tblUserInfo.getEmail(), userContactInfo.getEmail()))
            {
                tblUserInfoT = userRepository.getUserByEmail(userContactInfo.getEmail());
            }

            if (tblUserInfoT != null && ! tblUserInfoT.getUserId().equals(userId))
            {
                throw new WebSystemException(ErrorCode.EmailOccupied, ErrorLevel.INFO);
            }
        }

        if (req.getName() != null && ! StringUtils.equals(tblUserInfo.getUserName(), req.getName()))
        {
            TblUserInfo tblUserInfoT2 = userRepository.getUserByUserName(req.getName());
            if (tblUserInfoT2 != null && !tblUserInfoT2.getUserId().equals(userId))
            {
                throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
            }
        }

        tblUserInfo.setBpId(req.getBp_id());
        tblUserInfo.setIsAllowed(req.is_allowed());

        if (userContactInfo != null)
        {
            if (!StringUtils.isEmpty(userContactInfo.getEmail()))
            {
                tblUserInfo.setEmail(userContactInfo.getEmail());
            }

            if (!StringUtils.isEmpty(userContactInfo.getPhone()))
            {
                tblUserInfo.setPhone(userContactInfo.getPhone());
            }

            if (!StringUtils.isEmpty(userContactInfo.getAddress()))
            {
                tblUserInfo.setAddress(userContactInfo.getAddress());
            }
        }

        int result = 0;

        tblUserInfo.setGender(req.getGender());
        tblUserInfo.setKind(req.getKind());
        tblUserInfo.setLevel(req.getLevel());
        tblUserInfo.setStatus(req.getStatus());
        if (! StringUtils.isEmpty(req.getPassword()))
        {
            tblUserInfo.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        result = userRepository.updateUserInfo(tblUserInfo);

        if (result > 0)
        {
            return ErrorCode.SUCCESS.getCode();
        }
        else
        {
            log.error("User register failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

    }

    public int updateCurrentUser(String userId, UserBasicReq req) throws WebSystemException
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

        UserContactInfo userContactInfo = req.getContact_info();

        if (userContactInfo != null )
        {
            TblUserInfo tblUserInfoT = null;
            if (!StringUtils.isEmpty(userContactInfo.getPhone()) && ! StringUtils.equals(tblUserInfo.getPhone(), userContactInfo.getPhone()))
            {
                tblUserInfoT = userRepository.getUserByPhone(userContactInfo.getPhone());
            }

            if (tblUserInfoT != null && ! tblUserInfoT.getUserId().equals(userId))
            {
                throw new WebSystemException(ErrorCode.PhoneOccupied, ErrorLevel.INFO);
            }

            if (!StringUtils.isEmpty(userContactInfo.getEmail()) && ! StringUtils.equals(tblUserInfo.getEmail(), userContactInfo.getEmail()))
            {
                tblUserInfoT = userRepository.getUserByEmail(userContactInfo.getEmail());
            }

            if (tblUserInfoT != null && ! tblUserInfoT.getUserId().equals(userId))
            {
                throw new WebSystemException(ErrorCode.EmailOccupied, ErrorLevel.INFO);
            }
        }

        if (req.getName() != null && ! StringUtils.equals(tblUserInfo.getUserName(), req.getName()))
        {
            TblUserInfo tblUserInfoT2 = userRepository.getUserByUserName(req.getName());
            if (tblUserInfoT2 != null && !tblUserInfoT2.getUserId().equals(userId))
            {
                throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
            }
        }

        tblUserInfo.setBpId(req.getBp_id());

        if (userContactInfo != null)
        {
            if (!StringUtils.isEmpty(userContactInfo.getEmail()))
            {
                tblUserInfo.setEmail(userContactInfo.getEmail());
            }

            if (!StringUtils.isEmpty(userContactInfo.getPhone()))
            {
                tblUserInfo.setPhone(userContactInfo.getPhone());
            }

            if (!StringUtils.isEmpty(userContactInfo.getAddress()))
            {
                tblUserInfo.setAddress(userContactInfo.getAddress());
            }
        }

        int result = 0;

        tblUserInfo.setGender(req.getGender());
        result = userRepository.updateUserInfo(tblUserInfo);

        if (result > 0)
        {
            return ErrorCode.SUCCESS.getCode();
        }
        else
        {
            log.error("User register failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

    }
    /**
     * update user status.
     *
     * @param userId user id
     * @param allowFlag allow flag
     * @return String or FormatRespDto
     */
    public int updateUserStatus(String userId, boolean allowFlag)
    {
        return userRepository.updateUserStatus(userId, allowFlag);
    }

    public UserRsp getUserDtoInfo(String userId) throws WebSystemException
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);

        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

        List<TblRoleInfo> tblRoleInfoList = userRepository.getRolesByUserId(userId);

        UserRsp userRsp = new UserRsp();
        userRsp.setPermission(tblRoleInfoList);
        userRsp.setResponse(tblUserInfo);
        return userRsp;
    }

    TblUserInfoExample setUserInfoExample(UserSearchCritical critical)
    {
        TblUserInfoExample example = new TblUserInfoExample();
        TblUserInfoExample.Criteria criteria = example.createCriteria();
        if (ServiceCombRequestUtils.isAdmin() && StringUtils.isNotBlank(critical.getQueryBpId()))
        {
            criteria.andBpIdEqualTo(critical.getQueryBpId());
        }
        if (critical.getName() != null) criteria.andUserNameEqualTo(critical.getName());
        if (critical.getBpId() != null) criteria.andBpIdEqualTo(critical.getBpId());
        if (critical.getUserId() != null) criteria.andUserIdEqualTo(critical.getUserId());
        criteria.andStatusNotEqualTo(UMStatus.REMOVED);
        return example;
    }

    public Object getUserDtoInfos(UserSearchCritical critical) throws WebSystemException
    {
    	try
		{
			TblUserInfoExample example = setUserInfoExample(critical);
			QueryUsersRsp queryUserRsp = new QueryUsersRsp();

			Long total_num = userRepository.countUsersByExample(example);
			queryUserRsp.setTotal_num(total_num.intValue());
			if (total_num < 1)
			{
				return queryUserRsp;
			}

			int begin = ((critical.getPageNum()-1) * critical.getPageSize());
			example.setOrderByClause("update_time desc");
			example.setStartRow(begin);
			example.setPageSize(critical.getPageSize());

			List<TblUserInfo> userInfoList = userRepository.getUsersByExample(example);
			if (userInfoList == null)
			{
				return queryUserRsp;
			}

			List<UserRsp> userRspList = new ArrayList<>();
			for (TblUserInfo userInfo : userInfoList)
			{
				UserRsp userRsp = new UserRsp();
				userRsp.setResponse(userInfo);
				userRspList.add(userRsp);
			}
			queryUserRsp.setUsers(userRspList);
			return queryUserRsp;
		}
    	catch (Exception e)
		{
			throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
		}

    }

    public List<RoleDto> getRolesByUserId(String userId)
    {
        List<TblRoleInfo> tblRoleInfoList = userRepository.getRolesByUserId(userId);
        List<RoleDto> roleDtoList = new ArrayList<>();
        if (tblRoleInfoList == null || tblRoleInfoList.isEmpty())
        {
            return roleDtoList;
        }

        for (TblRoleInfo tblRoleInfo : tblRoleInfoList)
        {
            RoleDto roleDto = new RoleDto();
            roleDto.setPlatform(tblRoleInfo.getPlatform());
            roleDto.setRole(tblRoleInfo.getRole());
            roleDtoList.add(roleDto);
        }
        return roleDtoList;
    }

    public List<ComponentRoleDto> getRoles()
    {
        List<TblRoleInfo> tblRoleInfoList = userRepository.getWholeRoles();
        List<ComponentRoleDto> roleDtoList = new ArrayList<>();
        if (tblRoleInfoList == null)
        {
            return roleDtoList;
        }

        Map<String, ComponentRoleDto> componentRoleDtoMap = new HashMap<>();

        for (TblRoleInfo tblRoleInfo : tblRoleInfoList)
        {
            ComponentRoleDto componentRoleDto = componentRoleDtoMap.get(tblRoleInfo.getPlatform());
            if (componentRoleDto != null)
            {
                componentRoleDto.getRoles().add(tblRoleInfo.getRole());
                continue;
            }
            componentRoleDto = new ComponentRoleDto();
            componentRoleDto.setPlatform(tblRoleInfo.getPlatform());
            List<String> roles = new ArrayList<>();
            roles.add(tblRoleInfo.getRole());
            componentRoleDto.setRoles(roles);
            componentRoleDtoMap.put(tblRoleInfo.getPlatform(), componentRoleDto);
        }
        return new ArrayList<ComponentRoleDto>(componentRoleDtoMap.values());
    }

    public void updateUserPhone(String userId, PhoneRawInfo phoneRawInfo) throws WebSystemException
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

/*        if (RedisUtil.get(RedisCacheField.PATCH_VER_CODE, phoneRawInfo.getVerification_code()) == null)
        {
            throw new WebSystemException(ErrorCode.Invalid_validateCode, ErrorLevel.INFO);
        }*/

        TblUserInfo tblExistUser = userRepository.getUserByPhone(phoneRawInfo.getPhone());
        if (tblExistUser != null)
        {
            throw new WebSystemException(ErrorCode.PhoneOccupied, ErrorLevel.INFO);
        }
        tblUserInfo.setPhone(phoneRawInfo.getPhone());
        userRepository.updateUserInfo(tblUserInfo);
    }

    public void updateUserEmail(String userId, EmailRawInfo emailRawInfo) throws WebSystemException
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (tblUserInfo == null)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }

/*        if (RedisUtil.get(RedisCacheField.PATCH_VER_CODE, emailRawInfo.getVerification_code()) == null)
        {
            throw new WebSystemException(ErrorCode.Invalid_validateCode, ErrorLevel.INFO);
        }*/

        TblUserInfo tblExistUser = userRepository.getUserByEmail(emailRawInfo.getEmail());
        if (tblExistUser != null)
        {
            throw new WebSystemException(ErrorCode.EmailOccupied, ErrorLevel.INFO);
        }
        tblUserInfo.setEmail(emailRawInfo.getEmail());
        userRepository.updateUserInfo(tblUserInfo);
    }

    public void addRolesByUserId(String userId, List<RoleDto> roleRawReq) throws WebSystemException {
        userRepository.addRolesByUserId(userId, roleRawReq);
    }

    private void initTenantRole(TblUserInfo tblUserInfo) throws WebSystemException {
        List<TblRoleInfo> wholeRoles = userRepository.getWholeRoles();
        if (CollectionUtils.isNotEmpty(wholeRoles)) {
            List<TblRoleInfo> tblRoleInfos = new ArrayList<>();

            String kind = "";
            if (tblUserInfo.getKind().intValue() == PERSONAL) {
               kind = "TENANT";
            } else if (tblUserInfo.getKind().intValue() == BP) {
                kind = "TENANT_ADMIN";
            } else if (tblUserInfo.getKind().intValue() == ADMIN || tblUserInfo.getKind().intValue() == UserKind.SYSTEM) {
                kind = "ADMIN";
            }
            String finalKind = kind;
            tblRoleInfos = wholeRoles.stream().filter(role -> role.getRole().equalsIgnoreCase(finalKind))
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(tblRoleInfos)) {
                List<RoleDto> collect = tblRoleInfos.stream().map(x -> {
                    RoleDto roleDto = new RoleDto();
                    roleDto.setRole(x.getRole());
                    roleDto.setPlatform(x.getPlatform());
                    return roleDto;
                }).collect(Collectors.toList());
                userRepository.addRolesByUserId(tblUserInfo.getUserId(), collect);
            }
        }
    }

    public ApiKeyRsp setApiKey(String userId)
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (null == tblUserInfo)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }
        tblUserInfo.setApiKey(Utils.assignUUId());
        tblUserInfo.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (1 != userRepository.updateUserInfo(tblUserInfo))
        {
            log.error("User set apiKey failed, userId:{}", userId);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        ApiKeyRsp apiKeyRsp = new ApiKeyRsp();
        apiKeyRsp.setUserId(userId);
        apiKeyRsp.setApiKey(tblUserInfo.getApiKey());
        apiKeyRsp.setApiKeySecret(Md5Util.getMd5(tblUserInfo.getApiKey()));
        return apiKeyRsp;
    }

    public String getApiKey(String userId)
    {
        TblUserInfo tblUserInfo = userRepository.getUserById(userId);
        if (null == tblUserInfo)
        {
            throw new WebSystemException(ErrorCode.User_Not_Exist, ErrorLevel.INFO);
        }
        return tblUserInfo.getApiKey();
    }
}
