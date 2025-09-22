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

package com.lnjoying.justice.usermanager.rpcserviceimpl;

import com.lnjoying.justice.schema.service.ums.AuthService;
import com.lnjoying.justice.usermanager.db.model.TblRoleInfo;
import com.lnjoying.justice.usermanager.db.model.TblUserInfo;
import com.lnjoying.justice.usermanager.db.repo.UserRepository;
import com.lnjoying.justice.usermanager.utils.Md5Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

//@RpcSchema(schemaId = "authService")
@RestSchema(schemaId = "authService")
@RequestMapping(path = "/AuthImpl")
@Controller
@Slf4j
public class AuthImpl
{
	@Autowired
	private UserRepository userRepository;

	@PostMapping(path = "/auth")
	public AuthService.AuthUser auth(@RequestHeader(name = "principal")String principal, @RequestHeader(name = "credential")String credential)
	{
		AuthService.AuthUser authUser = new AuthService.AuthUser();
		authUser.setSuccess(false);
		if (Md5Util.md5Check(credential, principal))
		{
			TblUserInfo tblUserInfo = userRepository.getUserByApiKey(principal);
			authUser.setUserId(tblUserInfo.getUserId());
			authUser.setUserName(tblUserInfo.getUserName());
			authUser.setSuccess(true);

			List<TblRoleInfo> rolePos = userRepository.getRolesByUserId(tblUserInfo.getUserId());
			List<GrantedAuthority> authorities = new ArrayList<>();
			rolePos.forEach(rolePo -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rolePo.getPlatform() + "_" + rolePo.getRole())));
			authUser.setAuthorities(authorities.toString());
//			rolePos.forEach(
//					rolePo -> roles.add("ROLE_"+rolePo.getPlatform()+"_"+ rolePo.getRole())
//			);
//			List<String> roles =  rolePos.stream().map(
//				rolePo->"ROLE_"+rolePo.getPlatform()+"_"+ rolePo.getRole()
//			).collect(Collectors.toList());
			log.info("check apikey successfully,userId:{}", tblUserInfo.getUserId());
		}
		return authUser;
	}
	//    @ApiOperation(value = "differentName", nickname = "differentName")
//    public int diffNames(@ApiParam(name = "x") int a, @ApiParam(name = "y") int b) {
//        return a * 2 + b;
//    }


//	@ApiOperation(value = "islogout", nickname = "islogout")
//	public Boolean islogout()
//	{
//		return false;
//	}
}
