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

package com.lnjoying.justice.schema.service.ims;

import io.swagger.annotations.ApiParam;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * ims registry service
 *
 * @author merak
 **/
public interface ImsRegistryService {

    /**
     * query by registryId and userId
     * @param registryId
     * @param bpId
     * @param userId
     * @return
     */
    Registry getRegistry(@ApiParam(name = "registryId")@NotNull String registryId, @ApiParam(name = "bpId") String bpId, @ApiParam(name = "userId") String userId);

    final class Registry implements Serializable {

        private static final long serialVersionUID = -5970373514918069294L;

        private String registryId;

        private String registryUrl;

        private String registryUserName;

        /**
         * Use base 64 encoding
         */
        private String registryPassword;

        public String getRegistryId() {
            return registryId;
        }

        public void setRegistryId(String registryId) {
            this.registryId = registryId;
        }

        public String getRegistryUrl() {
            return registryUrl;
        }

        public void setRegistryUrl(String registryUrl) {
            this.registryUrl = registryUrl;
        }

        public String getRegistryUserName() {
            return registryUserName;
        }

        public void setRegistryUserName(String registryUserName) {
            this.registryUserName = registryUserName;
        }

        public String getRegistryPassword() {
            return registryPassword;
        }

        public void setRegistryPassword(String registryPassword) {
            this.registryPassword = registryPassword;
        }
    }
}
