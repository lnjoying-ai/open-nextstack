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

package com.lnjoying.justice.usermanager.domain.dto.request.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.lnjoying.justice.usermanager.config.ServiceConfig;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "userRegisterRequest")
public class UserRegReq extends UserBasicReq
{

    @ApiModelProperty(required = true, example = "TestPassword1")
    @Pattern(regexp = ServiceConfig.PATTERN_PASSWORD)
    private String password;

    @SerializedName("verification_code")
    @JsonProperty("verification_code")
    @ApiModelProperty(example = "123456")
    @Pattern(regexp = ServiceConfig.PATTERN_VERIFICATION_CODE)
    private String verification_code;

    /**
     * check basic data by trim.
     */
    public void trim() {
        super.trim();
        this.password = StringUtils.trimWhitespace(this.password);
        this.verification_code = StringUtils.trimWhitespace(this.verification_code);
    }
}
