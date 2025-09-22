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

package com.lnjoying.justice.repo.service.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeSnapCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.*;
import com.lnjoying.justice.repo.entity.Volume;
import com.lnjoying.justice.repo.entity.VolumeSnap;
import com.lnjoying.justice.repo.entity.search.VolumeSnapSearchCritical;
import com.lnjoying.justice.repo.mapper.VolumeSnapVoMapper;
import com.lnjoying.justice.repo.service.VolumeService;
import com.lnjoying.justice.repo.service.VolumeSnapService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Service("volumeSnapServiceBiz")
@Slf4j
public class VolumeSnapServiceBiz
{
    @Autowired
    VolumeService volumeService;

    @Autowired
    VolumeSnapService volumeSnapService;

    @Resource
    VolumeSnapVoMapper volumeSnapVoMapper;

    @Autowired
    CombRpcSerice combRpcSerice;


    public Object getVolumeSnaps(VolumeSnapSearchCritical volumeSnapSearchCritical, String userId) throws WebSystemException
    {

        LambdaQueryWrapper<VolumeSnapDetailInfoRsp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VolumeSnapDetailInfoRsp::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(volumeSnapSearchCritical.getVolumeSnapName()))
        {
            queryWrapper.like(VolumeSnapDetailInfoRsp::getName, volumeSnapSearchCritical.getVolumeSnapName());
        }
        if (null != volumeSnapSearchCritical.getVolumeId())
        {
            queryWrapper.eq(VolumeSnapDetailInfoRsp::getVolumeId, volumeSnapSearchCritical.getVolumeId());
        }
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(VolumeSnapDetailInfoRsp::getUserId, userId);
        }

        VolumeSnapsRsp getVolumeSnapsRsp = new VolumeSnapsRsp();

        //get total number with example condition
        long totalNum = volumeSnapVoMapper.countVolumeSnaps(queryWrapper);

        getVolumeSnapsRsp.setTotalNum(totalNum);
        if (totalNum < 1) {
            return getVolumeSnapsRsp;
        }

        //query with page number and page size
//        int begin = ((volumeSnapSearchCritical.getPageNum() - 1) * volumeSnapSearchCritical.getPageSize());

        queryWrapper.orderByDesc(VolumeSnapDetailInfoRsp::getCreateTime);

        Page<VolumeSnapDetailInfoRsp> page = new Page<>(volumeSnapSearchCritical.getPageNum(),volumeSnapSearchCritical.getPageSize());
//        Page<VolumeSnap> volumeSnapPage = volumeSnapService.page(page, queryWrapper);

        List<VolumeSnapDetailInfoRsp> volumeSnapDetailInfos = volumeSnapVoMapper.selectVolumeSnaps(page, queryWrapper);


//        List<VolumeSnap> volumeSnaps = volumeSnapPage.getRecords();
//        if (null == volumeSnaps)
//        {
//            return getVolumeSnapsRsp;
//        }
//
//        List<VolumeSnapDetailInfoRsp> volumeSnapDetailInfos = volumeSnaps.stream().map(tblVolumeSnap -> {
//            VolumeSnapDetailInfoRsp volumeSnapDetailInfoRsp = new VolumeSnapDetailInfoRsp();
//            volumeSnapDetailInfoRsp.setVolumeSnapDetailInfoRsp(tblVolumeSnap);
//            return volumeSnapDetailInfoRsp;
//        }).collect(Collectors.toList());

        //set response
        getVolumeSnapsRsp.setVolumeSnaps(volumeSnapDetailInfos);
        return getVolumeSnapsRsp;
    }

    public Object getVolumeSnap(String volumeSnapId, String userId) throws WebSystemException {
        VolumeSnap volumeSnap = getVolumeSnapById(volumeSnapId);
        if (!StrUtil.isBlank(userId) && !userId.equals(volumeSnap.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }

        Volume tblVolume = volumeService.getById(volumeSnap.getVolumeId());

        VolumeSnapDetailInfoRsp volumeSnapDetailInfoRsp = new VolumeSnapDetailInfoRsp();
        volumeSnapDetailInfoRsp.setVolumeSnapDetailInfoRsp(volumeSnap);
        volumeSnapDetailInfoRsp.setVolumeName(tblVolume.getName());
        volumeSnapDetailInfoRsp.setStoragePoolId(tblVolume.getStoragePoolId());
        return volumeSnapDetailInfoRsp;
    }

    public Object removeVolumeSnap(String volumeSnapId, String userId) throws WebSystemException
    {

        VolumeSnap tblVolumeSnap = getVolumeSnapById(volumeSnapId);
        if (null == tblVolumeSnap || REMOVED == tblVolumeSnap.getPhaseStatus() )
        {
            throw new WebSystemException(ErrorCode.VOLUME_SNAP_NOT_EXIST, ErrorLevel.INFO);
        }

        if (!StrUtil.isBlank(userId) && !userId.equals(tblVolumeSnap.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        Volume tblVolume = volumeService.getById(tblVolumeSnap.getVolumeId());

//        isVmInstancePowerOff(tblVolume);

        tblVolumeSnap.setPhaseStatus(PhaseStatus.DELETING);
        tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = volumeSnapService.updateById(tblVolumeSnap);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VolumeSnapBaseRsp.builder().volumeSnapId(volumeSnapId).build();
    }

    public void isVmInstancePowerOff(Volume tblVolume) throws WebSystemException
    {
        if (null == tblVolume || REMOVED == tblVolume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(tblVolume.getVmId()))
        {
            boolean isPowerOff = combRpcSerice.getVmService().isVmPowerOff(tblVolume.getVmId());
            if (!isPowerOff)
            {
                throw new WebSystemException(ErrorCode.INSTANCE_NOT_POWER_OFF, ErrorLevel.INFO);
            }
        }
    }

    public Object createVolumeSnap(VolumeSnapCreateReq volumeSnapInfo, String userId) throws WebSystemException
    {
        Volume tblVolume = volumeService.getById(volumeSnapInfo.getVolumeId());
//        isVmInstancePowerOff(tblVolume);
        if (StrUtil.isBlank(tblVolume.getVolumeIdFromAgent()) && PhaseStatus.DETACHED==tblVolume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NEVER_USED, ErrorLevel.INFO);
        }
//        String volumeSnapId = UUID.randomUUID().toString();
        String volumeSnapId = Utils.assignUUId();
        VolumeSnap tblVolumeSnap = new VolumeSnap();
        tblVolumeSnap.setVolumeSnapId(volumeSnapId);
        tblVolumeSnap.setVolumeId(volumeSnapInfo.getVolumeId());
        tblVolumeSnap.setPhaseStatus(PhaseStatus.ADDING);
        tblVolumeSnap.setCreateTime(new Date(System.currentTimeMillis()));
        tblVolumeSnap.setUpdateTime(tblVolumeSnap.getCreateTime());
        tblVolumeSnap.setUserId(userId);
        tblVolumeSnap.setName(volumeSnapInfo.getName());
        tblVolumeSnap.setDescription(volumeSnapInfo.getDescription());

        boolean ok = volumeSnapService.save(tblVolumeSnap);

        if (!ok) {
            log.error("insert tbl_volume_snap error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return VolumeSnapBaseRsp.builder().volumeSnapId(volumeSnapId).build();
    }

    public VolumeSnapBaseRsp updateVolumeSnap(String volumeSnapId, CommonReq req, String userId) throws WebSystemException
    {
        VolumeSnap volumeSnap = getVolumeSnapById(volumeSnapId);
        if (!StrUtil.isBlank(userId) && !userId.equals(volumeSnap.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(req.getName())) volumeSnap.setName(req.getName());
        if (!StrUtil.isBlank(req.getDescription())) volumeSnap.setDescription(req.getDescription());
        if (!volumeSnapService.updateById(volumeSnap))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VolumeSnapBaseRsp.builder().volumeSnapId(volumeSnapId).build();
    }

    public VolumeSnapBaseRsp switchSnapVolume( String volumeSnapId, String userId) throws WebSystemException
    {
        VolumeSnap tblVolumeSnap = getVolumeSnapById(volumeSnapId);
        if (null == tblVolumeSnap || REMOVED == tblVolumeSnap.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_SNAP_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(userId) && !Objects.equals(tblVolumeSnap.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        Volume tblVolume = volumeService.getById(tblVolumeSnap.getVolumeId());

        isVmInstancePowerOff(tblVolume);
        tblVolumeSnap.setPhaseStatus(PhaseStatus.SNAP_SWITCHING);
        tblVolumeSnap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (!volumeSnapService.updateById(tblVolumeSnap))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VolumeSnapBaseRsp.builder().volumeSnapId(volumeSnapId).build();
    }

    private VolumeSnap getVolumeSnapById(String volumeSnapId)
    {
        VolumeSnap volumeSnap = volumeSnapService.getById(volumeSnapId);
        if (null == volumeSnap || REMOVED == volumeSnap.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.VOLUME_SNAP_NOT_EXIST, ErrorLevel.INFO);
        }
        return volumeSnap;
    }

    public List<SnapsTreeRsp> getSnapsTree(String volumeId, String userId)
    {
        Volume tblVolume = volumeService.getById(volumeId);
        if (null == tblVolume || REMOVED == tblVolume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(userId) && !tblVolume.getUserId().equals(userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VolumeSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VolumeSnap::getVolumeId, volumeId)
                .ne(VolumeSnap::getPhaseStatus, REMOVED);
        List<VolumeSnap> tblVolumeSnaps = volumeSnapService.list(queryWrapper);
        if (null == tblVolumeSnaps || tblVolumeSnaps.isEmpty())
        {
            return new ArrayList<>();
        }
        List<SnapsTreeRsp> snapsTreeRspList = new ArrayList<>();
        for (VolumeSnap tblVolumeSnap : tblVolumeSnaps)
        {
            SnapsTreeRsp snapsTreeRsp = new SnapsTreeRsp();
//            snapsTreeRsp.setVolumeSnapId(tblVolumeSnap.getVolumeSnapId());
//            snapsTreeRsp.setParentId(tblVolumeSnap.getParentId());
            snapsTreeRsp.setVolumeSnapDetailInfoRsp(tblVolumeSnap);
            snapsTreeRspList.add(snapsTreeRsp);
        }
        Map<String, List<SnapsTreeRsp>> parentMap = snapsTreeRspList.stream().collect(Collectors.groupingBy(SnapsTreeRsp::getParentId));
        for(SnapsTreeRsp snapsTreeRsp : snapsTreeRspList)
        {
            if (parentMap.containsKey(snapsTreeRsp.getVolumeSnapId()))
            {
                snapsTreeRsp.setChildren(parentMap.get(snapsTreeRsp.getVolumeSnapId()));
            }
        }
//        snapsTreeRspList.forEach(item -> item.setChildren(parentMap.get(item.getSnapId())));
        return snapsTreeRspList.stream().filter(item -> "".equals(item.getParentId())).collect(Collectors.toList());
    }
}
