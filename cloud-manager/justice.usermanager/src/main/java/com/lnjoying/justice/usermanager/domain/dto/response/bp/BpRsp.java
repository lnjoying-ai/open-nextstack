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

package com.lnjoying.justice.usermanager.domain.dto.response.bp;

import com.lnjoying.justice.usermanager.db.model.TblBpInfo;
import com.lnjoying.justice.usermanager.domain.dto.request.bp.Bp_Contacts_Info;
import com.micro.core.common.Utils;
import lombok.Data;

@Data
public class BpRsp
{
	String id;
	int	status;
	String name;
	String website;
	String license_id;
	String master_user;
	Bp_Contacts_Info contact_info;
	String create_time;
	String update_time;

	public void setBpInfo(TblBpInfo tblBpInfo)
	{
		this.setId(tblBpInfo.getBpId());
		this.setLicense_id(tblBpInfo.getLicenseId());
		this.setMaster_user(tblBpInfo.getMasterUser());
		this.setName(tblBpInfo.getBpName());
		this.setWebsite(tblBpInfo.getWebsite());
		this.setCreate_time(Utils.formatDate(tblBpInfo.getCreateTime()));
		this.setUpdate_time(Utils.formatDate(tblBpInfo.getUpdateTime()));
		if (tblBpInfo.getStatus() != null) this.setStatus(tblBpInfo.getStatus());
	}
}
