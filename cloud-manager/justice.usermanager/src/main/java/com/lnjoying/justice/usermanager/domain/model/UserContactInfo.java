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

package com.lnjoying.justice.usermanager.domain.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class UserContactInfo
{
    @ApiModelProperty(example = "test@edgegallery.org")
    private String email;

    @ApiModelProperty(required = true, example = "15533449966")
    private String phone;

    private String address;

    public void trim()
    {
        this.email    =  StringUtils.trimWhitespace(this.email);
        this.phone    =  StringUtils.trimWhitespace(this.phone);
    }
}
