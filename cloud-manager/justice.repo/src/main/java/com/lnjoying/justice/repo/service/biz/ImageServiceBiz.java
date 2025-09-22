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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.biz.LogRpcSerice;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.domain.dto.request.CommonReq;
import com.lnjoying.justice.repo.domain.dto.request.ImageCreateReq;
import com.lnjoying.justice.repo.domain.dto.request.ShareCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.*;
import com.lnjoying.justice.repo.entity.Image;
import com.lnjoying.justice.repo.entity.NodeImage;
import com.lnjoying.justice.repo.entity.Share;
import com.lnjoying.justice.repo.entity.Volume;
import com.lnjoying.justice.repo.entity.search.ImageSearchCritical;
import com.lnjoying.justice.repo.entity.search.ShareSearchCritical;
import com.lnjoying.justice.repo.service.ImageService;
import com.lnjoying.justice.repo.service.NodeImageService;
import com.lnjoying.justice.repo.service.ShareService;
import com.lnjoying.justice.repo.service.VolumeService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Service("imageServiceBiz")
@Slf4j
public class ImageServiceBiz
{
    @Autowired
    ImageService imageService;

    @Autowired
    ShareService shareService;

    @Autowired
    VolumeService volumeService;

    @Autowired
    NodeImageService nodeImageService;

    @Autowired
    private LogRpcSerice logRpcSerice;


    public ImagesRsp getImages(ImageSearchCritical imageSearchCritical, String userId) throws WebSystemException {

        LambdaQueryWrapper<Image> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Image::getPhaseStatus, REMOVED);
        if (StrUtil.isNotBlank(imageSearchCritical.getImageName()))
        {
            queryWrapper.like(Image::getImageName, imageSearchCritical.getImageName());
        }
        if (null != imageSearchCritical.getImageOsType())
        {
            queryWrapper.eq(Image::getImageOsType, imageSearchCritical.getImageOsType());
        }
        if (null != imageSearchCritical.getIsOk())
        {
            queryWrapper.eq(Image::getPhaseStatus, PhaseStatus.ADDED);
        }
        if (null != imageSearchCritical.getImageOsVendor())
        {
            queryWrapper.eq(Image::getImageOsVendor, imageSearchCritical.getImageOsVendor());
        }
        if (null != imageSearchCritical.getIsVm() && imageSearchCritical.getIsVm())
        {
            queryWrapper.isNull(Image::getFileIdFromAgent);
        }
        else if (null != imageSearchCritical.getIsVm() && !imageSearchCritical.getIsVm())
        {
            queryWrapper.isNotNull(Image::getFileIdFromAgent);
        }
        if (imageSearchCritical.getIsPublic() && !StrUtil.isBlank(userId))
        {
            queryWrapper.eq(Image::getIsPublic,  true)
                    .or()
                    .eq(Image::getUserId, userId);
        }
        else if (!imageSearchCritical.getIsPublic() && !StrUtil.isBlank(userId))
        {
            queryWrapper.eq(Image::getUserId, userId);
        }
        else if (imageSearchCritical.getIsPublic() && StrUtil.isBlank(userId))
        {
            log.info("user admin: get all images");
            //
        }
        else
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        if (null != imageSearchCritical.getIsGpu())
        {
            if (imageSearchCritical.getIsGpu())
            {
                queryWrapper.like(Image::getImageName,"GPU");

            }
            else
            {
                queryWrapper.notLike(Image::getImageName,"GPU");
            }
        }
        /*
        if(null!=imageSearchCritical.getImageName() ){
            criteria.andImageNameEqualTo(imageSearchCritical.getImageName());
        }else if (null != imageSearchCritical.getImageOsVendor()&& null ==imageSearchCritical.getImageOsType()){
            criteria.andImageOsVendorEqualTo(imageSearchCritical.getImageOsVendor());
        }else if (null != imageSearchCritical.getImageOsVendor() && null !=imageSearchCritical.getImageOsType()){
            criteria.andImageOsTypeEqualTo(imageSearchCritical.getImageOsType())
                    .andImageOsVendorEqualTo(imageSearchCritical.getImageOsVendor());
        }else if  ( null !=imageSearchCritical.getImageOsType() && null == imageSearchCritical.getImageOsVendor()){
            criteria.andImageOsTypeEqualTo(imageSearchCritical.getImageOsType());
        }
         */

        ImagesRsp getImagesRsp = new ImagesRsp();

            //get total number with example condition
        long totalNum = imageService.count(queryWrapper);

        getImagesRsp.setTotalNum(totalNum);
        if (totalNum < 1) {
            return getImagesRsp;
        }

        //query with page number and page size
//        int begin = ((imageSearchCritical.getPageNum() - 1) * imageSearchCritical.getPageSize());

        queryWrapper.orderByDesc(Image::getCreateTime);

        Page<Image> page = new Page<>(imageSearchCritical.getPageNum(),imageSearchCritical.getPageSize());
        Page<Image> imagePage = imageService.page(page, queryWrapper);

        List<Image> images = imagePage.getRecords();
        if (null == images) {
            return getImagesRsp;
        }

        List<ImageDetailInfoRsp> imageInfos = images.stream().map(tblRsImage -> {
            ImageDetailInfoRsp imageInfo = new ImageDetailInfoRsp();
            imageInfo.setImageDetailInfoRsp(tblRsImage);
            return imageInfo;
        }).collect(Collectors.toList());

        //set response
        getImagesRsp.setImages(imageInfos);
        return getImagesRsp;
    }


    public ImageDetailInfoRsp getImage(String imageId, String userId) throws WebSystemException {
        Image image = imageService.getById(imageId);
        if (null == image || REMOVED == image.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(userId)&&!image.getIsPublic() &&
                !userId.equals(image.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        ImageDetailInfoRsp imageDetailInfoRsp = new ImageDetailInfoRsp();
        imageDetailInfoRsp.setImageDetailInfoRsp(image);
        return imageDetailInfoRsp;
    }

    @Transactional(rollbackFor = Exception.class)
    public Object removeImage(String imageId, String userId) throws WebSystemException {
        LambdaQueryWrapper<Share> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Share::getPhaseStatus, REMOVED)
                .eq(Share::getImageId, imageId);
        LambdaQueryWrapper<Volume> volumeQueryWrapper = new LambdaQueryWrapper<>();
        volumeQueryWrapper.ne(Volume::getPhaseStatus, REMOVED)
                .eq(Volume::getImageId, imageId);
        long shareTotal = shareService.count(queryWrapper);
        long volumeTotal = volumeService.count(volumeQueryWrapper);
        if (shareTotal > 0 || volumeTotal > 0)
        {
            throw new WebSystemException(ErrorCode.IMAGE_IS_USING, ErrorLevel.INFO);
        }
        Image image = imageService.getById(imageId);

        if (null == image || REMOVED == image.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (image.getIsPublic())
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        // if userId is null,  it means the default image and cannot be deleted
        if (null == image.getUserId())
        {
            throw new WebSystemException(ErrorCode.IMAGE_SYSTEM_DEFAULT, ErrorLevel.INFO);
        }

        if (!StrUtil.isBlank(userId) && !userId.equals(image.getUserId()))
        {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }

        //process nodeImage
        if (!image.getIsPublic())
        {
            LambdaUpdateWrapper<NodeImage> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.ne(NodeImage::getPhaseStatus, REMOVED)
                    .eq(NodeImage::getImageId, imageId);

//                    .eq(NodeImage::getStoragePoolId, nodeImageInfo.getStoragePoolId
            NodeImage nodeImage = new NodeImage();
            nodeImage.setPhaseStatus(PhaseStatus.DELETING);
            nodeImage.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            if (nodeImageService.count(updateWrapper) > 0)
            {
                boolean ok = nodeImageService.update(nodeImage, updateWrapper);
                if (!ok)
                {
                    log.error("update tbl_node_image error");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            }
        }
        image.setPhaseStatus(REMOVED);
        image.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = imageService.updateById(image);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return ImageBaseRsp.builder().imageId(imageId).build();
    }

    public ImageBaseRsp createImage(ImageCreateReq imageInfo) throws WebSystemException {
        String imageId = UUID.randomUUID().toString();
        Image tblImage = new Image();
        tblImage.setImageId(imageId);
        tblImage.setPhaseStatus(PhaseStatus.ADDED);
        tblImage.setCreateTime(new Date(System.currentTimeMillis()));
        tblImage.setUpdateTime(tblImage.getCreateTime());
        tblImage.setUserId(imageInfo.getUserId());
        tblImage.setImageName(imageInfo.getImageName());
        tblImage.setImageFormat(imageInfo.getImageFormat());
        tblImage.setImageOsType(imageInfo.getImageOsType());
        tblImage.setImageOsVendor(imageInfo.getImageOsVendor());
        tblImage.setImageOsVersion(imageInfo.getImageOsVersion());
        tblImage.setIsPublic(true);

        boolean ok = imageService.save(tblImage);

        if (!ok) {
            log.error("insert tbl_rs_image error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(ServiceCombRequestUtils.getUserId()).getUserName();
        String desc = StrUtil.format("添加镜像【镜像名称：{}，镜像类型：{}】", imageInfo.getImageName(), imageInfo.getImageOsType() == 1 ? "WINDOWS" : "LINUX");
        logRpcSerice.getLogService().addLog(ServiceCombRequestUtils.getUserId(), userName, "镜像", desc);

        return ImageBaseRsp.builder().imageId(imageId).build();
    }

    // 更新镜像
    public ImageBaseRsp updateImage(String imageId, CommonReq req,  String userId) throws WebSystemException {
        Image image = imageService.getById(imageId);
        if (null == image || REMOVED == image.getPhaseStatus()) {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(userId) && !userId.equals(image.getUserId())) {
            throw new WebSystemException(ErrorCode.InvalidAuthority, ErrorLevel.INFO);
        }
        if (image.getIsPublic()) {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        image.setImageName(req.getName());
        image.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = imageService.updateById(image);
        if (!ok) {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return ImageBaseRsp.builder().imageId(imageId).build();
    }

    //share
    public Object getShares(ShareSearchCritical searchCritical) throws WebSystemException {

        LambdaQueryWrapper<Share> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Share::getPhaseStatus, REMOVED);
        if (!(StrUtil.isBlank(searchCritical.getImageId())))
        {
            queryWrapper.eq(Share::getImageId, searchCritical.getImageId());
        }
        if (!(StrUtil.isBlank(searchCritical.getUserId())))
        {
            queryWrapper.eq(Share::getUserId, searchCritical.getUserId());
        }
        SharesRsp getSharesRsp = new SharesRsp();

        //get total number with example condition
        long totalNum = shareService.count(queryWrapper);
        log.info("get shares totalNum: {}, imageId:{}", totalNum,searchCritical.getImageId());
        getSharesRsp.setTotalNum(totalNum);
        if (totalNum < 1) {
            return getSharesRsp;
        }

        //query with page number and page size
        int begin = ((searchCritical.getPageNum() - 1) * searchCritical.getPageSize());
        queryWrapper.orderByDesc(Share::getCreateTime);

        Page<Share> page = new Page<>(begin, searchCritical.getPageNum());
        Page<Share> sharePage = shareService.page(page, queryWrapper);

        List<Share> shares = sharePage.getRecords();

        if (null == shares) {
            return getSharesRsp;
        }
        List<ShareDetailInfoRsp> shareInfoList = shares.stream().map(tblRsShare->{
            ShareDetailInfoRsp shareInfo = new ShareDetailInfoRsp();
            shareInfo.setShareDetailInfoRsp(tblRsShare);
            return shareInfo;
        }).collect(Collectors.toList());
        //set response
        getSharesRsp.setShares(shareInfoList);
        return getSharesRsp;
    }


    public Object addShare(ShareCreateReq shareInfo) throws WebSystemException {
        String shareId = UUID.randomUUID().toString();
        Share tblShare = new Share();
        tblShare.setShareId(shareId);
        tblShare.setImageId(shareInfo.getImageId());
        tblShare.setCreateTime(new Date(System.currentTimeMillis()));
        tblShare.setUpdateTime(tblShare.getCreateTime());
//      tblShare.set .shareIdFromAgent(shareIdFromAgent)
        tblShare.setPhaseStatus(PhaseStatus.ADDING);
        tblShare.setUserId(shareInfo.getUserId());
        tblShare.setBaremetalId(shareInfo.getBaremetalId());

        boolean ok = shareService.save(tblShare);
        if (!ok) {
            log.error("insert tbl_rs_share error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return ShareBaseRsp.builder().shareId(shareId).build();
    }

    public Object delShare(String shareId) throws WebSystemException {
        Share tblShare = shareService.getById(shareId);
        if (null == tblShare|| REMOVED == tblShare.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.SHARE_NOT_EXIST, ErrorLevel.INFO);
        }

        tblShare.setPhaseStatus(PhaseStatus.DELETING);
        tblShare.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = shareService.updateById(tblShare);

        if (!ok) {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return ShareBaseRsp.builder().shareId(shareId).build();
    }

    public Object getShare(String shareId)
    {
        Share tblShare = shareService.getById(shareId);
        if (null == tblShare || REMOVED == tblShare.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.SHARE_NOT_EXIST, ErrorLevel.INFO);
        }
        Image image = imageService.getById(tblShare.getImageId());
        if (null == image || REMOVED == image.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.IMAGE_NOT_EXIST, ErrorLevel.INFO);
        }

        ShareDetailInfoRsp shareInfo = new ShareDetailInfoRsp();
        shareInfo.setShareDetailInfoRsp(tblShare);
        return shareInfo;
    }
}
