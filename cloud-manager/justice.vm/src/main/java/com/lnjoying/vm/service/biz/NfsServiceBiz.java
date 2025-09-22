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

package com.lnjoying.vm.service.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.vm.common.NfsStatus;
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.NfsCreateReq;
import com.lnjoying.vm.domain.dto.response.NfsBaseRsp;
import com.lnjoying.vm.domain.dto.response.NfsInfoRsp;
import com.lnjoying.vm.domain.dto.response.NfsInfosRsp;
import com.lnjoying.vm.entity.Nfs;
import com.lnjoying.vm.entity.search.NfsSearchCritical;
import com.lnjoying.vm.service.NfsService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service
@Slf4j
public class NfsServiceBiz
{
    @Autowired
    private NfsService nfsService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private LogRpcService logRpcService;

    public NfsBaseRsp createNfs(NfsCreateReq req, String userId) throws WebSystemException
    {
        NetworkService.Vpc vpcInfo = combRpcSerice.getNetworkService().getVpc(req.getVpcId());
        if (null == vpcInfo)
        {
            log.error("create nfs failed, vpc not exist, vpcId:{}", req.getVpcId());
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST, ErrorLevel.ERROR);
        }
        Nfs tblNfs = new Nfs();
        tblNfs.setNfsId(Utils.assignUUId());
        tblNfs.setUserId(userId);
        tblNfs.setName(req.getName());
        tblNfs.setDescription(req.getDescription());
        tblNfs.setSize(req.getSize());
        tblNfs.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblNfs.setUpdateTime(tblNfs.getCreateTime());
        tblNfs.setVpcId(req.getVpcId());
        tblNfs.setSubnetId(req.getSubnetId());
        tblNfs.setUserId(userId);
        tblNfs.setPhaseStatus(NfsStatus.NFS_INIT);
        boolean ok = nfsService.save(tblNfs);
        if (!ok)
        {
            log.error("create nfs failed, tblNfs:{}", tblNfs);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.ERROR);
        }
        NfsBaseRsp rsp = new NfsBaseRsp();
        rsp.setNfsId(tblNfs.getNfsId());
        String userName = logRpcService.getUmsService().getUser(userId).getUserName();
        String desc = StrUtil.format("创建Nfs【id：{}，名称：{},vpcId：{}，subnetId:{}，大小：{}】", tblNfs.getNfsId(), tblNfs.getName(), tblNfs.getVpcId(), tblNfs.getSubnetId(), tblNfs.getSize());
        logRpcService.getLogService().addLog(userId, userName, "计算-NFS", desc);
        return rsp;
    }

    public NfsBaseRsp removeNfs(String nfsId, String userId)
    {
        Nfs tblNfs = nfsService.getById(nfsId);
        if (tblNfs == null || REMOVED == tblNfs.getPhaseStatus())
        {
            log.error("remove nfs failed, nfsId:{}", nfsId);
            throw new WebSystemException(ErrorCode.NFS_NOT_EXIST, ErrorLevel.ERROR);
        }
        if (!tblNfs.getUserId().equals(userId))
        {
            log.error("remove nfs failed, nfsId:{}, userId:{}", nfsId, userId);
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.ERROR);
        }
        tblNfs.setPhaseStatus(NfsStatus.NFS_REMOVING);
        tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = nfsService.updateById(tblNfs);
        if (!ok)
        {
            log.error("remove nfs failed, nfsId:{}", nfsId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.ERROR);
        }
        NfsBaseRsp rsp = new NfsBaseRsp();
        rsp.setNfsId(nfsId);
        return rsp;
    }

    public NfsBaseRsp updateNfs(String nfsId, CommonReq req, String userId)
    {
        Nfs tblNfs = nfsService.getById(nfsId);
        if (tblNfs == null || REMOVED == tblNfs.getPhaseStatus())
        {
            log.error("update nfs failed, nfsId:{}", nfsId);
            throw new WebSystemException(ErrorCode.NFS_NOT_EXIST, ErrorLevel.ERROR);
        }
        if (!tblNfs.getUserId().equals(userId))
        {
            log.error("update nfs failed, nfsId:{}, userId:{}", nfsId, userId);
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.ERROR);
        }
        if (StrUtil.isNotBlank(req.getName())) tblNfs.setName(req.getName());
        if (StrUtil.isNotBlank(req.getDescription())) tblNfs.setDescription(req.getDescription());
        tblNfs.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = nfsService.updateById(tblNfs);
        if (!ok)
        {
            log.error("update nfs failed, tblNfs:{}", tblNfs);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.ERROR);
        }
        NfsBaseRsp rsp = new NfsBaseRsp();
        rsp.setNfsId(nfsId);
        return rsp;
    }

    public NfsInfoRsp getNfs(String nfsId, String userId) throws WebSystemException
    {
        Nfs tblNfs = nfsService.getById(nfsId);
        if (tblNfs == null || REMOVED == tblNfs.getPhaseStatus())
        {
            log.error("get nfs failed, nfsId:{}", nfsId);
            throw new WebSystemException(ErrorCode.NFS_NOT_EXIST, ErrorLevel.ERROR);
        }
        if (!tblNfs.getUserId().equals(userId))
        {
            log.error("get nfs failed, nfsId:{}, userId:{}", nfsId, userId);
            throw new WebSystemException(ErrorCode.No_Permission, ErrorLevel.ERROR);
        }
        NfsInfoRsp rsp = new NfsInfoRsp();
        rsp.setNfsInfo(tblNfs);
        NetworkService.TenantNetworkPort port = combRpcSerice.getNetworkService().getTenantNetworkPort(tblNfs.getPortId());
        if (null != port)
        {
            if (StrUtil.isNotBlank(port.getSubnetCidr())) rsp.setSubnetCidr(port.getSubnetCidr());
            if (StrUtil.isNotBlank(port.getVpcCidr())) rsp.setVpcCidr(port.getVpcCidr());
            if (StrUtil.isNotBlank(port.getIpAddress())) rsp.setServicePath(port.getIpAddress() + ":/");
        }
        return rsp;
    }

    // NFS列表
    public NfsInfosRsp listNfs(NfsSearchCritical nfsSearchCritical, String usrId) throws WebSystemException
    {
        LambdaQueryWrapper<Nfs> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Nfs::getPhaseStatus, REMOVED);
        if (StrUtil.isNotBlank(nfsSearchCritical.getName()))
            queryWrapper.like(Nfs::getName, nfsSearchCritical.getName());
        if (StrUtil.isNotBlank(usrId)) queryWrapper.eq(Nfs::getUserId, usrId);
        long count = nfsService.count(queryWrapper);
        NfsInfosRsp rsp = new NfsInfosRsp();
        rsp.setTotalNum(count);
        if (count == 0) return rsp;
        Page<Nfs> page = new Page<>(nfsSearchCritical.getPageNum(), nfsSearchCritical.getPageSize());
        Page<Nfs> nfsIPage = nfsService.page(page, queryWrapper);
        if (nfsIPage.getTotal() < 1)
        {
            return rsp;
        }
        List<Nfs> nfsList = nfsIPage.getRecords();
        List<NfsInfoRsp> nfsInfoList = new ArrayList<>();
        for (Nfs tblNfs : nfsList)
        {
            NfsInfoRsp nfsRsp = new NfsInfoRsp();
            nfsRsp.setNfsInfo(tblNfs);
            NetworkService.TenantNetworkPort port = combRpcSerice.getNetworkService().getTenantNetworkPort(tblNfs.getPortId());
            if (null != port)
            {
                if (StrUtil.isNotBlank(port.getSubnetCidr())) nfsRsp.setSubnetCidr(port.getSubnetCidr());
                if (StrUtil.isNotBlank(port.getVpcCidr())) nfsRsp.setVpcCidr(port.getVpcCidr());
                if (StrUtil.isNotBlank(port.getIpAddress())) nfsRsp.setServicePath(port.getIpAddress() + ":/");
                if (StrUtil.isNotBlank(port.getSubnetName())) nfsRsp.setSubnetName(port.getSubnetName());
                if (StrUtil.isNotBlank(port.getVpcName())) nfsRsp.setVpcName(port.getVpcName());
            }
            nfsInfoList.add(nfsRsp);
        }
        rsp.setNfsInfos(nfsInfoList);
        return rsp;
    }
}
