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
import com.lnjoying.justice.network.domain.dto.request.LoadbalancerCreateReq;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancerBaseRsp;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancerDetailInfoRsp;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancersRsp;
import com.lnjoying.justice.network.entity.Loadbalancer;
import com.lnjoying.justice.network.entity.search.LoadbalancerSearchCritical;

import javax.validation.constraints.NotBlank;

/**
 * <p>
 * 负载均衡器实例 服务类
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
public interface LoadbalancerService extends IService<Loadbalancer> {
    LoadbalancersRsp getLoadbalancers(LoadbalancerSearchCritical loadBalancerSearchCritical, String userId);

    LoadbalancerDetailInfoRsp getLoadbalancerDetailInfo(String loadbalancerId, String userId);

    LoadbalancerBaseRsp addLoadbalancer(LoadbalancerCreateReq loadbalancerInfo, @NotBlank String userId);

    LoadbalancerBaseRsp updateLoadbalancer(String loadbalancerId,LoadbalancerCreateReq loadbalancerInfo, String userId);

    LoadbalancerBaseRsp delLoadbalancer(@NotBlank String loadbalancerId, String userId);
}
