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

package com.lnjoying.justice.network.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lnjoying.justice.network.domain.dto.request.BackendCreateReq;
import com.lnjoying.justice.network.domain.dto.response.BackendBaseRsp;
import com.lnjoying.justice.network.domain.dto.response.BackendsRsp;
import com.lnjoying.justice.network.entity.Backend;
import com.lnjoying.justice.network.entity.search.BackendSearchCritical;

import javax.validation.constraints.NotBlank;

/**
 * <p>
 * 负载均衡-后端服务组 服务类
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
public interface BackendService extends IService<Backend> {

    BackendsRsp getBackendServices(BackendSearchCritical backendCritical, String userId);

    BackendBaseRsp addBackend(BackendCreateReq backendInfo, @NotBlank String userId);

    BackendBaseRsp delBackend(@NotBlank String backendId, String userId);

    BackendBaseRsp updateBackend(String backendId,BackendCreateReq backendInfo, String userId);

}
