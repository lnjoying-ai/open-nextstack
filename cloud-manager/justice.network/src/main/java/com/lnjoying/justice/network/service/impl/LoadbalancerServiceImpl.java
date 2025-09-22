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
import com.lnjoying.justice.network.domain.dto.request.LoadbalancerCreateReq;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancerBaseRsp;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancerDetailInfoRsp;
import com.lnjoying.justice.network.domain.dto.response.LoadbalancersRsp;
import com.lnjoying.justice.network.entity.Frontend;
import com.lnjoying.justice.network.entity.Loadbalancer;
import com.lnjoying.justice.network.entity.Subnet;
import com.lnjoying.justice.network.entity.search.LoadbalancerSearchCritical;
import com.lnjoying.justice.network.mapper.LoadbalancerMapper;
import com.lnjoying.justice.network.service.FrontendService;
import com.lnjoying.justice.network.service.LoadbalancerService;
import com.lnjoying.justice.network.service.SubnetService;
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
 * 负载均衡器实例 服务实现类
 * </p>
 *
 * @author George
 * @since 2023-07-13
 */
@Service
public class LoadbalancerServiceImpl extends ServiceImpl<LoadbalancerMapper, Loadbalancer> implements LoadbalancerService {

    @Autowired
    private LogRpcService logRpcService;

    @Autowired
    private SubnetService subnetService;

    @Autowired
    private VpcService vpcService;

    @Autowired
    private FrontendService frontendService;

    @Override
    public LoadbalancersRsp getLoadbalancers(LoadbalancerSearchCritical loadBalancerSearchCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<Loadbalancer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Loadbalancer::getPhaseStatus, REMOVED);
        if (StrUtil.isNotBlank(loadBalancerSearchCritical.getName()))
        {
            queryWrapper.like(Loadbalancer::getName, loadBalancerSearchCritical.getName());
        }

        if (StrUtil.isNotBlank(userId))
        {
            queryWrapper.eq(Loadbalancer::getUserId, userId);
        }

        LoadbalancersRsp getLoadbalancersRsp = new LoadbalancersRsp();

        long totalNum = this.count(queryWrapper);
        getLoadbalancersRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getLoadbalancersRsp;
        }
        queryWrapper.orderByDesc(Loadbalancer::getCreateTime);
//        int current = loadbalancerCritical.getPageSize() * (loadbalancerCritical.getPageNum()-1);
        Page<Loadbalancer> page = new Page<>(loadBalancerSearchCritical.getPageNum(), loadBalancerSearchCritical.getPageSize());
        Page<Loadbalancer> loadbalancerPage = this.page(page, queryWrapper);
        if (loadbalancerPage.getTotal() < 1)
        {
            return getLoadbalancersRsp;
        }
        List<Loadbalancer> loadbalancers = loadbalancerPage.getRecords();
        List<LoadbalancerDetailInfoRsp> loadbalancerDetailInfoRsps = loadbalancers.stream().map(loadbalancer ->
        {
            LoadbalancerDetailInfoRsp loadbalancerDetailInfoRsp = new LoadbalancerDetailInfoRsp();
            setDetailInfo(loadbalancerDetailInfoRsp, loadbalancer);
            return loadbalancerDetailInfoRsp;
        }).collect(Collectors.toList());
        getLoadbalancersRsp.setLoadbalancers(loadbalancerDetailInfoRsps);
        return getLoadbalancersRsp;
    }

    @Override
    public LoadbalancerDetailInfoRsp getLoadbalancerDetailInfo(String loadbalancerId, String userId) throws WebSystemException
    {
        Loadbalancer loadbalancer = getLoadbalancer(loadbalancerId, userId);
        LoadbalancerDetailInfoRsp loadbalancerDetailInfoRsp = new LoadbalancerDetailInfoRsp();
        setDetailInfo(loadbalancerDetailInfoRsp, loadbalancer);
        return loadbalancerDetailInfoRsp;
    }

    private void setDetailInfo(LoadbalancerDetailInfoRsp loadbalancerDetailInfoRsp, Loadbalancer loadbalancer)
    {
        loadbalancerDetailInfoRsp.setLoadbalancerDetailInfoRsp(loadbalancer);
        Subnet tblSubnet = subnetService.getById(loadbalancer.getSubnetId());
        loadbalancerDetailInfoRsp.setSubnetName(tblSubnet.getName());
        loadbalancerDetailInfoRsp.setVpcId(tblSubnet.getVpcId());
        loadbalancerDetailInfoRsp.setVpcName(vpcService.getById(tblSubnet.getVpcId()).getName());
        LambdaQueryWrapper<Frontend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Frontend::getLbId, loadbalancer.getLbId())
                .ne(Frontend::getPhaseStatus, REMOVED);
        long frontendCount = frontendService.count(queryWrapper);
        loadbalancerDetailInfoRsp.setFrontendCount(frontendCount);
    }

    @Override
    public LoadbalancerBaseRsp addLoadbalancer(LoadbalancerCreateReq loadbalancerInfo, @NotBlank String userId)
    {
        Loadbalancer tblLoadbalancer = new Loadbalancer();
        String loadbalancerId = Utils.assignUUId();
        BeanUtils.copyProperties(loadbalancerInfo, tblLoadbalancer);
        tblLoadbalancer.setLbId(loadbalancerId);
        tblLoadbalancer.setUserId(userId);
        //list to String
        tblLoadbalancer.setPhaseStatus(PhaseStatus.ADDING);
        tblLoadbalancer.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblLoadbalancer.setUpdateTime(tblLoadbalancer.getCreateTime());

        boolean ok = this.save(tblLoadbalancer);
        if (!ok)
        {
            log.error("insert tbl_loadbalancer error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName, "创建负载均衡器", "负载均衡器名称：" + tblLoadbalancer.getName());
        return LoadbalancerBaseRsp.builder().lbId(loadbalancerId).build();
    }

    @Override
    public LoadbalancerBaseRsp delLoadbalancer(@NotBlank String loadbalancerId, String userId)
    {
        Loadbalancer tblLoadbalancer = getLoadbalancer(loadbalancerId,userId);

        tblLoadbalancer.setPhaseStatus(PhaseStatus.DELETING);
        boolean ok = this.updateById(tblLoadbalancer);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"删除负载均衡器", "负载均衡器Id：" + tblLoadbalancer.getLbId());
        return LoadbalancerBaseRsp.builder().lbId(loadbalancerId).build();
    }


    @Override
    public LoadbalancerBaseRsp updateLoadbalancer(String loadbalancerId,LoadbalancerCreateReq loadbalancerInfo, String userId)
    {
        Loadbalancer tblLoadbalancer = getLoadbalancer(loadbalancerId,userId);
        BeanUtils.copyProperties(loadbalancerInfo, tblLoadbalancer);
        tblLoadbalancer.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));

        boolean ok = this.updateById(tblLoadbalancer);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userName = request.getHeader("X-UserName");
        logRpcService.getLogService().addLog(userId, userName,"更新后端服务组", "后端服务组Id：" + tblLoadbalancer.getLbId());
        return LoadbalancerBaseRsp.builder().lbId(loadbalancerId).build();
    }

    private Loadbalancer getLoadbalancer(String loadbalancerId, String userId)
    {
        Loadbalancer tblLoadbalancer = this.getById(loadbalancerId);
        if (null == tblLoadbalancer || Objects.equals(tblLoadbalancer.getPhaseStatus(), REMOVED))
        {
            throw new WebSystemException(ErrorCode.LOADBALANCER_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(userId))
        {
            if (!Objects.equals(tblLoadbalancer.getUserId(), userId))
            {
                throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
            }
        }
        return tblLoadbalancer;
    }
}
