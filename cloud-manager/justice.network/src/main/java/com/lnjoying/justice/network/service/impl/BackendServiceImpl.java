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

package com.lnjoying.justice.network.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.HttpContextUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.network.common.PhaseStatus;
import com.lnjoying.justice.network.domain.dto.request.BackendCreateReq;
import com.lnjoying.justice.network.domain.dto.response.BackendBaseRsp;
import com.lnjoying.justice.network.domain.dto.response.BackendDetailInfoRsp;
import com.lnjoying.justice.network.domain.dto.response.BackendsRsp;
import com.lnjoying.justice.network.entity.Backend;
import com.lnjoying.justice.network.entity.Loadbalancer;
import com.lnjoying.justice.network.entity.search.BackendSearchCritical;
import com.lnjoying.justice.network.mapper.BackendMapper;
import com.lnjoying.justice.network.service.BackendService;
import com.lnjoying.justice.network.service.LoadbalancerService;
import com.lnjoying.justice.network.service.VpcService;
import com.lnjoying.justice.network.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

/**
 * <p>
 * 负载均衡-后端服务组 服务实现类
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@Service
public class BackendServiceImpl extends ServiceImpl<BackendMapper, Backend> implements BackendService {

    @Autowired
    private VpcService vpcService;

    @Autowired
    private LoadbalancerService loadbalancerService;

    @Autowired
    private LogRpcService logRpcService;

    @Override
    public BackendsRsp getBackendServices(BackendSearchCritical backendCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<Backend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Backend::getPhaseStatus, REMOVED);
        if (StrUtil.isNotBlank(backendCritical.getName()))
        {
            queryWrapper.like(Backend::getName, backendCritical.getName());
        }

        if (StrUtil.isNotBlank(userId))
        {
            queryWrapper.eq(Backend::getUserId, userId);
        }

        if (StrUtil.isNotBlank(backendCritical.getLbId()))
        {
            queryWrapper.eq(Backend::getLbId, backendCritical.getLbId());
        }

        BackendsRsp getBackendsRsp = new BackendsRsp();

        long totalNum = this.count(queryWrapper);
        getBackendsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getBackendsRsp;
        }
        queryWrapper.orderByDesc(Backend::getCreateTime);
//        int current = backendCritical.getPageSize() * (backendCritical.getPageNum()-1);
        Page<Backend> page = new Page<>(backendCritical.getPageNum(), backendCritical.getPageSize());
        Page<Backend> backendPage = this.page(page, queryWrapper);
        if (backendPage.getTotal() < 1)
        {
            return getBackendsRsp;
        }
        List<Backend> backends = backendPage.getRecords();
        List<BackendDetailInfoRsp> backendDetailInfoRsps = backends.stream().map(backend ->
        {
            BackendDetailInfoRsp backendDetailInfoRsp = new BackendDetailInfoRsp();
            backendDetailInfoRsp.setBackendDetailInfoRsp(backend);
            backendDetailInfoRsp.setVpcName(vpcService.getById(backend.getVpcId()).getName());
            return backendDetailInfoRsp;
        }).collect(Collectors.toList());
        getBackendsRsp.setBackends(backendDetailInfoRsps);
        return getBackendsRsp;
    }

    @Override
    public BackendBaseRsp addBackend(BackendCreateReq backendInfo, @NotBlank String userId)
    {
        Backend tblBackend = new Backend();
        String backendId = Utils.assignUUId();
        BeanUtils.copyProperties(backendInfo, tblBackend);
        tblBackend.setBackendId(backendId);
        //list to String
        tblBackend.setBackendServer(StrUtil.join(",", backendInfo.getBackendServers()));
        tblBackend.setPhaseStatus(PhaseStatus.ADDING);
        tblBackend.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblBackend.setUpdateTime(tblBackend.getCreateTime());
        tblBackend.setUserId(userId);

        boolean ok = this.save(tblBackend);
        if (!ok)
        {
            log.error("insert tbl_backend error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"创建后端服务组", "后端服务组名称：" + backendInfo.getName());
        return BackendBaseRsp.builder().backendId(backendId).build();
    }

    @Override
    public BackendBaseRsp delBackend(@NotBlank String backendId, String userId)
    {
        Backend tblBackend = getBackend(backendId,userId);

        tblBackend.setPhaseStatus(PhaseStatus.DELETING);
        tblBackend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = this.updateById(tblBackend);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"删除后端服务组", "后端服务组Id：" + tblBackend.getBackendId());
        return BackendBaseRsp.builder().backendId(backendId).build();
    }

    @Override
    public BackendBaseRsp updateBackend(String backendId,BackendCreateReq backendInfo, String userId)
    {
        Loadbalancer tblLoadbalancer = loadbalancerService.getById(backendInfo.getLbId());
        if (null == tblLoadbalancer || REMOVED == tblLoadbalancer.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.LOADBALANCER_NOT_EXIST, ErrorLevel.INFO);
        }

        Backend tblBackend = getBackend(backendId,userId);
        BeanUtils.copyProperties(backendInfo, tblBackend);
        //list to String
        tblBackend.setBackendServer(StrUtil.join(",", backendInfo.getBackendServers()));
        tblBackend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));

        boolean ok = this.updateById(tblBackend);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"更新后端服务组", "后端服务组Id：" + tblBackend.getBackendId());
        return BackendBaseRsp.builder().backendId(backendId).build();
    }

    private Backend getBackend(String backendId, String userId)
    {
        Backend tblBackend = this.getById(backendId);
        if (null == tblBackend || Objects.equals(tblBackend.getPhaseStatus(), REMOVED))
        {
            throw new WebSystemException(ErrorCode.BACKEND_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblBackend.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        return tblBackend;
    }
}
