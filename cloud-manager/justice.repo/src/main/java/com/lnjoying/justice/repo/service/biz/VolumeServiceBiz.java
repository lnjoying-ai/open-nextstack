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
import com.lnjoying.justice.commonweb.biz.LogRpcSerice;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.repo.common.constant.*;
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeCreateReq;
import com.lnjoying.justice.repo.domain.dto.request.VolumeExportReq;
import com.lnjoying.justice.repo.domain.dto.response.*;
import com.lnjoying.justice.repo.entity.*;
import com.lnjoying.justice.repo.entity.search.VolumeSearchCritical;
import com.lnjoying.justice.repo.mapper.RootVolumeVoMapper;
import com.lnjoying.justice.repo.mapper.VolumeVoMapper;
import com.lnjoying.justice.repo.service.*;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service("volumeServiceBiz")
@Slf4j
public class VolumeServiceBiz
{
    @Autowired
    VolumeService volumeService;

    @Resource
    VolumeVoMapper volumeVoMapper;

    @Resource
    RootVolumeVoMapper rootVolumeVoMapper;

    @Autowired
    VolumeSnapService volumeSnapService;

    @Autowired
    StoragePoolService storagePoolService;

    @Autowired
    ImageService imageService;

    @Autowired
    CombRpcSerice combRpcService;

    @Autowired
    private LogRpcSerice logRpcSerice;

    @Autowired
    private NodeStoragePoolService nodeStoragePoolService;

    @Autowired
    private NodeImageService nodeImageService;

    public VolumesRsp getVolumes(VolumeSearchCritical volumeSearchCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<VolumesRsp.VolumeVo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VolumesRsp.VolumeVo::getPhaseStatus, REMOVED);
//        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.ne(Volume::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(volumeSearchCritical.getVolumeName()))
        {
            queryWrapper.like(VolumesRsp.VolumeVo::getName, volumeSearchCritical.getVolumeName());
        }
        if (!StrUtil.isBlank(volumeSearchCritical.getPoolId()) )
        {
            queryWrapper.eq(VolumesRsp.VolumeVo::getStoragePoolId, volumeSearchCritical.getPoolId());
        }
        if (null != volumeSearchCritical.getPhaseStatus() && volumeSearchCritical.getIsEqPhaseStatus())
        {
            queryWrapper.eq(VolumesRsp.VolumeVo::getPhaseStatus, volumeSearchCritical.getPhaseStatus());
        }
        else if (null != volumeSearchCritical.getPhaseStatus() && !volumeSearchCritical.getIsEqPhaseStatus() )
        {
            queryWrapper.ne(VolumesRsp.VolumeVo::getPhaseStatus, volumeSearchCritical.getPhaseStatus());
        }
        if (volumeSearchCritical.getIsRoot())
        {
            queryWrapper.eq(VolumesRsp.VolumeVo::getType, VolumeType.ROOT_DISK);
//            queryWrapper.isNotNull(VolumesRsp.VolumeVo::getImageId);
        }
        else
        {
            queryWrapper.eq(VolumesRsp.VolumeVo::getType, VolumeType.DATA_DISK);
        }

        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(VolumesRsp.VolumeVo::getUserId, userId);
        }
//
        VolumesRsp getVolumesRsp = new VolumesRsp();
//
//        //get total number with example condition
        long totalNum = volumeVoMapper.countVolumes(queryWrapper);

        getVolumesRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getVolumesRsp;
        }
//
//        //query with page number and page size
////        int begin = ((volumeSearchCritical.getPageNum() - 1) * volumeSearchCritical.getPageSize());
//
        queryWrapper.orderByDesc(VolumesRsp.VolumeVo::getCreateTime);
//
        Page<VolumesRsp.VolumeVo> page = new Page<>(volumeSearchCritical.getPageNum(),volumeSearchCritical.getPageSize());
//        Page<Volume> volumePage = volumeService.page(page, queryWrapper);
//
//        List<Volume> volumes = volumePage.getRecords();
//        if (null == volumes) {
//            return getVolumesRsp;
//        }
        List<VolumesRsp.VolumeVo> volumes = volumeVoMapper.selectVolumes(page, queryWrapper);
//
//        //set response
        getVolumesRsp.setVolumes(volumes);
        return getVolumesRsp;
    }

    public RootVolumesRsp getRootVolumes(VolumeSearchCritical volumeSearchCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<RootVolumesRsp.RootVolumeVo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(RootVolumesRsp.RootVolumeVo::getPhaseStatus, REMOVED)
                .eq(RootVolumesRsp.RootVolumeVo::getType, VolumeType.ROOT_DISK);
//                .isNotNull(RootVolumesRsp.RootVolumeVo::getImageId);
//        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.ne(Volume::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(volumeSearchCritical.getVolumeName()))
        {
            queryWrapper.like(RootVolumesRsp.RootVolumeVo::getName, volumeSearchCritical.getVolumeName());
        }
        if (!StrUtil.isBlank(volumeSearchCritical.getPoolId()) )
        {
            queryWrapper.eq(RootVolumesRsp.RootVolumeVo::getStoragePoolId, volumeSearchCritical.getPoolId());
        }
        if (null != volumeSearchCritical.getPhaseStatus() && volumeSearchCritical.getIsEqPhaseStatus())
        {
            queryWrapper.eq(RootVolumesRsp.RootVolumeVo::getPhaseStatus, volumeSearchCritical.getPhaseStatus());
        }
        else if (null != volumeSearchCritical.getPhaseStatus() && !volumeSearchCritical.getIsEqPhaseStatus() )
        {
            queryWrapper.ne(RootVolumesRsp.RootVolumeVo::getPhaseStatus, volumeSearchCritical.getPhaseStatus());
        }

        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(RootVolumesRsp.RootVolumeVo::getUserId, userId);
        }
//
        RootVolumesRsp getVolumesRsp = new RootVolumesRsp();
//
//        //get total number with example condition
        long totalNum = rootVolumeVoMapper.countRootVolumes(queryWrapper);

        getVolumesRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getVolumesRsp;
        }

        queryWrapper.orderByDesc(RootVolumesRsp.RootVolumeVo::getCreateTime);
//
        Page<RootVolumesRsp.RootVolumeVo> page = new Page<>(volumeSearchCritical.getPageNum(),volumeSearchCritical.getPageSize());

        List<RootVolumesRsp.RootVolumeVo> volumes = rootVolumeVoMapper.selectRootVolumes(page, queryWrapper);
//
//        //set response
        getVolumesRsp.setVolumes(volumes);
        return getVolumesRsp;

    }


    public VolumeDetailInfoRsp getVolume(String volumeId, String userId) throws WebSystemException {
        Volume volume = getVolumeById(volumeId);
        if (!StrUtil.isBlank(userId) && !userId.equals(volume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        VolumeDetailInfoRsp volumeDetailInfoRsp = new VolumeDetailInfoRsp();
        volumeDetailInfoRsp.setVolumeDetailInfoRsp(volume);
        StoragePool storagePool = storagePoolService.getById(volume.getStoragePoolId());
        if (null != storagePool)
        {
            volumeDetailInfoRsp.setStoragePoolName(storagePool.getName());
            volumeDetailInfoRsp.setStoragePoolId(storagePool.getStoragePoolId());
        }
        if (VolumeType.ROOT_DISK == volume.getType())
        {
            Integer imageOsType = combRpcService.getVmService().getImageOsTypeByVolumeId(volumeId);
            volumeDetailInfoRsp.setImageOsType(imageOsType);
            if (StrUtil.isNotBlank(volume.getImageId()))
            {
                Image image = imageService.getById(volume.getImageId());
                if (null != image)
                {
                    volumeDetailInfoRsp.setImageName(image.getImageName());
                    volumeDetailInfoRsp.setImageOsVendor(image.getImageOsVendor());
                }
            }
        }
        return volumeDetailInfoRsp;
    }

    public VolumeBaseRsp removeVolume(String volumeId, String userId) throws WebSystemException {
        Volume volume = getVolumeById(volumeId);
        if (PhaseStatus.DETACHED != volume.getPhaseStatus() && PhaseStatus.ATTACH_FAILED != volume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NOT_DETACHED, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<VolumeSnap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VolumeSnap::getPhaseStatus, REMOVED)
                .eq(VolumeSnap::getVolumeId, volumeId);
        long volumeSnapTotal = volumeSnapService.count(queryWrapper);
        if (volumeSnapTotal > 0) {
            throw new WebSystemException(ErrorCode.VOLUME_HAS_SNAPS, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(userId) && !userId.equals(volume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        volume.setPhaseStatus(PhaseStatus.DELETING);
        volume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = volumeService.updateById(volume);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VolumeBaseRsp.builder().volumeId(volumeId).build();
    }

    public VolumeBaseRsp createVolume(VolumeCreateReq volumeInfo, String userId, boolean isRootDisk,
                                   String nodeIp, String vmId, String imageId) throws WebSystemException
    {
        String volumeId = Utils.assignUUId();
        Volume tblVolume = new Volume();
        tblVolume.setVolumeId(volumeId);
        tblVolume.setCreateTime(new Date(System.currentTimeMillis()));
        tblVolume.setUpdateTime(tblVolume.getCreateTime());
        tblVolume.setUserId(userId);
        tblVolume.setName(volumeInfo.getName());
        tblVolume.setDescription(volumeInfo.getDescription());
        tblVolume.setStoragePoolId(volumeInfo.getStoragePoolId());
        if (AgentConstant.DISK_MAX_SIZE < volumeInfo.getSize())
        {
            throw new WebSystemException(ErrorCode.VOLUME_SIZE_TOO_LARGE, ErrorLevel.INFO);
        }
        tblVolume.setSize(volumeInfo.getSize());
        if (isRootDisk)
        {
            tblVolume.setType(VolumeType.ROOT_DISK);
            tblVolume.setPhaseStatus(PhaseStatus.ADDING);
            tblVolume.setNodeIp(nodeIp);
            tblVolume.setVmId(vmId);
            tblVolume.setImageId(imageId);
        }
        else
        {
            tblVolume.setType(VolumeType.DATA_DISK);
            if (StrUtil.isBlank(nodeIp))
            {
                tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
            }
            else
            {
                tblVolume.setPhaseStatus(PhaseStatus.ADDING);
                tblVolume.setNodeIp(nodeIp);
                tblVolume.setVmId(vmId);
            }
        }

        boolean ok = volumeService.save(tblVolume);

        if (!ok) {
            log.error("insert tbl_volume error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return VolumeBaseRsp.builder().volumeId(volumeId).build();
    }

    public VolumeBaseRsp updateVolume(String volumeId, CommonReq req, String userId) throws WebSystemException
    {
        Volume volume = getVolumeById(volumeId);
        if (!StrUtil.isBlank(userId) && !userId.equals(volume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(req.getName())) volume.setName(req.getName());
        if (!StrUtil.isBlank(req.getDescription())) volume.setDescription(req.getDescription());
        if (!volumeService.updateById(volume))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return VolumeBaseRsp.builder().volumeId(volumeId).build();
    }

    public VolumeBaseRsp attachVolume(@NotBlank String volumeId, @NotBlank String vmId, String userId) throws WebSystemException
    {
        Boolean isMigrating = combRpcService.getVmService().isMigrating(vmId);
        if (null == isMigrating)
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (isMigrating)
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_IS_MIGRATING, ErrorLevel.INFO);
        }
        Volume volume = getVolumeById(volumeId);
        if (!StrUtil.isBlank(userId) && !userId.equals(volume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        if (PhaseStatus.DETACHED != volume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NOT_DETACHED, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Volume::getVmId, vmId)
                .isNull(Volume::getImageId)
                .ne(Volume::getPhaseStatus, REMOVED);
        if (volumeService.count(queryWrapper) >= AgentConstant.MAX_DATA_DISK_NUM)
        {
            throw new WebSystemException(ErrorCode.VOLUME_ATTACHED_TOO_MANY, ErrorLevel.INFO);
        }
        VmService.UserIdAndVmSnaps userIdAndVmSnaps = combRpcService.getVmService().getUserIdAndVmSnaps(vmId);
        if (userIdAndVmSnaps.getHasVmSnaps())
        {
            throw new WebSystemException(ErrorCode.VOLUME_CANNOT_DETACHED_OR_ATTACHED, ErrorLevel.INFO);
        }

        if (!StrUtil.isBlank(userId) && !userIdAndVmSnaps.getUserId().equals(volume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        volume.setVmId(vmId);
        volume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        volume.setPhaseStatus(PhaseStatus.ATTACHING);
        if (!volumeService.updateById(volume))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        Boolean ok = combRpcService.getVmService().attachVolume(volumeId, vmId);
        if (null == ok)
        {
            throw new WebSystemException(ErrorCode.VOLUME_ALREADY_ATTACHED, ErrorLevel.INFO);
        }
        else if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VolumeBaseRsp.builder().volumeId(volumeId).build();
    }

    public VolumeBaseRsp detachVolume(String volumeId, String userId) throws WebSystemException
    {
        Volume volume = getVolumeById(volumeId);
//        volume.setVmId(vmId);
        if (!StrUtil.isBlank(userId) && !userId.equals(volume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        if (PhaseStatus.ATTACHED != volume.getPhaseStatus() && PhaseStatus.DETACH_FAILED != volume.getPhaseStatus()
            && PhaseStatus.ATTACH_FAILED != volume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NOT_ATTACHED, ErrorLevel.INFO);
        }
        Boolean isMigrating = combRpcService.getVmService().isMigrating(volume.getVmId());
        if (null == isMigrating)
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (isMigrating)
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_IS_MIGRATING, ErrorLevel.INFO);
        }
        VmService.UserIdAndVmSnaps userIdAndVmSnaps = combRpcService.getVmService().getUserIdAndVmSnaps(volume.getVmId());
        if (userIdAndVmSnaps.getHasVmSnaps())
        {
            throw new WebSystemException(ErrorCode.VOLUME_CANNOT_DETACHED_OR_ATTACHED, ErrorLevel.INFO);
        }
        volume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        volume.setPhaseStatus(PhaseStatus.DETACHING);
//        volume.setVmId("");
        if (!volumeService.updateById(volume))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VolumeBaseRsp.builder().volumeId(volumeId).build();
    }

    public Volume getVolumeById(String volumeId) throws WebSystemException
    {
        Volume volume = volumeService.getById(volumeId);
        if (null == volume || REMOVED == volume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_NOT_EXIST, ErrorLevel.INFO);
        }
        return volume;
    }

    @Transactional(rollbackFor = Exception.class)
    public ImageBaseRsp exportVolume(String volumeId, @NotBlank String userId, VolumeExportReq volumeExportReq) throws WebSystemException
    {
        Volume tblVolume = getVolumeById(volumeId);
        if (!StrUtil.isBlank(userId) && !userId.equals(tblVolume.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<Image> imageLambdaQueryWrapper = new LambdaQueryWrapper<>();
        imageLambdaQueryWrapper.eq(Image::getImageName, volumeExportReq.getImageName())
                .ne(Image::getPhaseStatus, REMOVED);
        if (imageService.count(imageLambdaQueryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.IMAGE_NAME_EXIST, ErrorLevel.INFO);
        }
        String vmId = tblVolume.getVmId();
//        if (!combRpcService.getVmService().isVmPowerOff(vmId))
//        {
//            throw new WebSystemException(ErrorCode.INSTANCE_NOT_POWER_OFF, ErrorLevel.INFO);
//        }
        if (PhaseStatus.EXPORTING == tblVolume.getPhaseStatus() || PhaseStatus.AGENT_EXPORTING == tblVolume.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VOLUME_IS_EXPORTING, ErrorLevel.INFO);
        }
        String sourceImageId = tblVolume.getImageId();
        tblVolume.setPhaseStatus(PhaseStatus.EXPORTING);

//        volume.setUserId(userId);

        Image image = new Image();
        String imageId = Utils.assignUUId();
        image.setImageId(imageId);
        image.setImageName(volumeExportReq.getImageName());
        if (StrUtil.isBlank(sourceImageId))
        {
            image.setImageOsType(combRpcService.getVmService().getImageOsTypeByVolumeId(volumeId));
            image.setImageBase(null);
        }
        else
        {
            Image sourceImage = imageService.getById(sourceImageId);
            image.setImageOsType(sourceImage.getImageOsType());
            image.setImageBase(sourceImage.getImageBase());
            image.setImageOsVendor(sourceImage.getImageOsVendor());
            image.setImageOsVersion(sourceImage.getImageOsVersion());
        }
        image.setPhaseStatus(PhaseStatus.IMPORTING);
        image.setImageFormat(ImageType.QCOW2_TYPE);
        image.setIsPublic(volumeExportReq.getIsPublic());
        image.setUserId(userId);

        image.setCreateTime(new Date(System.currentTimeMillis()));
        image.setUpdateTime(image.getCreateTime());
        image.setVmInstanceId(vmId);
        if (!imageService.save(image))
        {
            log.error("save image failed, volumeId:{}", volumeId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVolume.setExportName(image.getImageId());
        boolean ok = volumeService.updateById(tblVolume);
        if (!ok)
        {
            log.error("update volume failed, volumeId:{}", volumeId);
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(userId).getUserName();
        String vmImageId = combRpcService.getVmService().getImageIdByVolumeId(volumeId);
        String desc = StrUtil.format("导出云盘成镜像【id：{}，镜像名称：{}，是否公开：{}，镜像id】", volumeId, volumeExportReq.getImageName(), volumeExportReq.getIsPublic(),vmImageId);
        logRpcSerice.getLogService().addLog(userId, userName, "存储-云盘管理", desc);


        return ImageBaseRsp.builder().imageId(imageId).build();
    }

}
