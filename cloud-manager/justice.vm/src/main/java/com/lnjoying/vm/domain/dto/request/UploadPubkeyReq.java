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

package com.lnjoying.vm.domain.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.AssertTrue;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class UploadPubkeyReq extends CommonReq
{
    //    @NotBlank(message = "pubKey is required")
    String pubKey;

    @AssertTrue(message = "pubKey is invalid")
    public boolean isValidPubKey()
    {
        String SSH_KEY_PATTERN = "^(ssh-rsa|ssh-dss|ecdsa-sha2-nistp256|ecdsa-sha2-nistp384|ecdsa-sha2-nistp521)\\s+([a-zA-Z0-9+/]+={0,3})\\s*(\\S+)?\\s*$";
        Pattern pattern = Pattern.compile(SSH_KEY_PATTERN);
        return pattern.matcher(pubKey).matches();
    }
}
