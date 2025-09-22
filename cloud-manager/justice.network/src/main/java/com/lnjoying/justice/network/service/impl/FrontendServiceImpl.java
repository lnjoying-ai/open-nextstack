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
import com.lnjoying.justice.network.domain.dto.request.FrontendCreateReq;
import com.lnjoying.justice.network.domain.dto.response.FrontendBaseRsp;
import com.lnjoying.justice.network.domain.dto.response.FrontendDetailInfoRsp;
import com.lnjoying.justice.network.domain.dto.response.FrontendsRsp;
import com.lnjoying.justice.network.entity.Backend;
import com.lnjoying.justice.network.entity.Frontend;
import com.lnjoying.justice.network.entity.Loadbalancer;
import com.lnjoying.justice.network.entity.search.FrontendSearchCritical;
import com.lnjoying.justice.network.mapper.FrontendMapper;
import com.lnjoying.justice.network.service.BackendService;
import com.lnjoying.justice.network.service.FrontendService;
import com.lnjoying.justice.network.service.LoadbalancerService;
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
 * 负载均衡器-监听器 服务实现类
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@Service
public class FrontendServiceImpl extends ServiceImpl<FrontendMapper, Frontend> implements FrontendService
{

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private LoadbalancerService loadbalancerService;

    @Autowired
    private BackendService backendService;

    @Override
    public FrontendsRsp getFrontendServices(FrontendSearchCritical frontendCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<Frontend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Frontend::getPhaseStatus, REMOVED);
        if (StrUtil.isNotBlank(frontendCritical.getName()))
        {
            queryWrapper.like(Frontend::getName, frontendCritical.getName());
        }

        if (StrUtil.isNotBlank(userId))
        {
            queryWrapper.eq(Frontend::getUserId, userId);
        }

        if (StrUtil.isNotBlank(frontendCritical.getLbId()))
        {
            queryWrapper.eq(Frontend::getLbId, frontendCritical.getLbId());
        }

        FrontendsRsp getFrontendsRsp = new FrontendsRsp();

        long totalNum = this.count(queryWrapper);
        getFrontendsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getFrontendsRsp;
        }
        queryWrapper.orderByDesc(Frontend::getCreateTime);
//        int current = frontendCritical.getPageSize() * (frontendCritical.getPageNum()-1);
        Page<Frontend> page = new Page<>(frontendCritical.getPageNum(), frontendCritical.getPageSize());
        Page<Frontend> frontendPage = this.page(page, queryWrapper);
        if (frontendPage.getTotal() < 1)
        {
            return getFrontendsRsp;
        }
        List<Frontend> frontends = frontendPage.getRecords();
        List<FrontendDetailInfoRsp> frontendDetailInfoRsps = frontends.stream().map(frontend ->
        {
            FrontendDetailInfoRsp frontendDetailInfoRsp = new FrontendDetailInfoRsp();
            frontendDetailInfoRsp.setFrontendDetailInfoRsp(frontend);
            Backend tblBackend = backendService.getById(frontend.getBackendId());
            frontendDetailInfoRsp.setBackendName(backendService.getById(frontend.getBackendId()).getName());
            frontendDetailInfoRsp.setProtocol(tblBackend.getProtocol());
            return frontendDetailInfoRsp;
        }).collect(Collectors.toList());
        getFrontendsRsp.setFrontends(frontendDetailInfoRsps);
        return getFrontendsRsp;
    }

    @Override
    public FrontendBaseRsp addFrontend(FrontendCreateReq frontendInfo, @NotBlank String userId)
    {
        Frontend tblFrontend = new Frontend();
        String frontendId = Utils.assignUUId();
        BeanUtils.copyProperties(frontendInfo, tblFrontend);
        tblFrontend.setFrontendId(frontendId);
        //list to String

        tblFrontend.setPhaseStatus(PhaseStatus.ADDING);
        tblFrontend.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblFrontend.setUpdateTime(tblFrontend.getCreateTime());
        tblFrontend.setUserId(userId);
        tblFrontend.setListenPort(frontendInfo.getPort().toString());

        boolean ok = this.save(tblFrontend);
        if (!ok)
        {
            log.error("insert tbl_frontend error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"创建监听器", "监听器名称：" + frontendInfo.getName());
        return FrontendBaseRsp.builder().frontendId(frontendId).build();
    }

    @Override
    public FrontendBaseRsp delFrontend(@NotBlank String frontendId, String userId)
    {
        Frontend tblFrontend = getFrontend(frontendId,userId);

        tblFrontend.setPhaseStatus(PhaseStatus.DELETING);
        boolean ok = this.updateById(tblFrontend);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"删除后端服务组", "后端服务组Id：" + tblFrontend.getFrontendId());
        return FrontendBaseRsp.builder().frontendId(frontendId).build();
    }

    @Override
    public FrontendBaseRsp updateFrontend(String frontendId,FrontendCreateReq frontendInfo, String userId)
    {
        Loadbalancer tblLoadbalancer  = loadbalancerService.getById(frontendInfo.getLbId());
        if (null == tblLoadbalancer || REMOVED == tblLoadbalancer.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.LOADBALANCER_NOT_EXIST, ErrorLevel.INFO);
        }
        Backend tblBackend = backendService.getById(frontendInfo.getBackendId());
        if (null == tblBackend || REMOVED == tblLoadbalancer.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.BACKEND_NOT_EXIST, ErrorLevel.INFO);
        }
        Frontend tblFrontend = getFrontend(frontendId,userId);
        BeanUtils.copyProperties(frontendInfo, tblFrontend);
        //list to String
        tblFrontend.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));

        boolean ok = this.updateById(tblFrontend);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"更新后端服务组", "后端服务组Id：" + tblFrontend.getFrontendId());
        return FrontendBaseRsp.builder().frontendId(frontendId).build();
    }

    private Frontend getFrontend(String frontendId, String userId)
    {
        Frontend tblFrontend = this.getById(frontendId);
        if (null == tblFrontend || Objects.equals(tblFrontend.getPhaseStatus(), REMOVED))
        {
            throw new WebSystemException(ErrorCode.BACKEND_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblFrontend.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        return tblFrontend;
    }
    
}
