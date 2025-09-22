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

package com.lnjoying.justice.repo.rpcserviceimpl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.schema.service.repo.ImageService;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.entity.*;
import com.lnjoying.justice.repo.service.NodeImageService;
import com.lnjoying.justice.repo.service.NodeStoragePoolService;
import com.lnjoying.justice.repo.service.ShareService;
import com.micro.core.common.Utils;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@RpcSchema(schemaId = "imageService")
@Slf4j
public class RpcImageServiceImpl implements ImageService {
    @Autowired
    private com.lnjoying.justice.repo.service.ImageService imageService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private NodeStoragePoolService nodeStoragePoolService;

    @Autowired
    private NodeImageService nodeImageService;

    
    @Override
    public Image getImage(@ApiParam(value = "imageId", required = true, name = "imageId") String imageId) {
        if (StrUtil.isBlank(imageId)) {
//            log.info("get image: null, imageId: {}",imageId);
            return null;
        }
        com.lnjoying.justice.repo.entity.Image tblImage = imageService.getById(imageId);
        if (tblImage == null) {
            log.info("get image: null, imageId: {}",imageId);
            return null;
        }
        Image image = new Image();
        image.setImageId(tblImage.getImageId());
        String imageName = tblImage.getImageName();
        image.setName(imageName);
        if (imageName.contains("GPU"))
        {
            image.setName(imageName.split("-GPU-")[0]);
        }
        image.setFormat(tblImage.getImageFormat());
        image.setImageOsType(tblImage.getImageOsType());
        image.setImageOsVendor(tblImage.getImageOsVendor());
        image.setImageOsVersion(tblImage.getImageOsVersion());
        image.setVmInstanceId(tblImage.getVmInstanceId());
        image.setImageBase(tblImage.getImageBase());
        return image;
    }

    @Override
    public List<Image> getBatchImages(@ApiParam(name = "imageIdList") List<String> imageIdList) {
        return imageIdList.stream().map(this::getImage).collect(Collectors.toList());
    }

    @Override
    public ImgConnection getImageConnection(@ApiParam(value = "shareId", required = true, name = "shareId")
                                                    String shareId) {

        Share tblShare = shareService.getById(shareId);
        if (tblShare == null || REMOVED == tblShare.getPhaseStatus()){
            log.info("get rsShare: null, shareId: {}", shareId);
            return null;
        }
        ImgConnection imgConnection = new ImgConnection();
        imgConnection.setShareId(shareId);
        imgConnection.setInstanceId(tblShare.getBaremetalId());
        imgConnection.setImageId(tblShare.getImageId());
        imgConnection.setIscsiInitiator(tblShare.getIscsiInitiator());
        imgConnection.setIscsiTarget(tblShare.getIscsiTarget());
        imgConnection.setIscsiIpport(tblShare.getIscsiIpport());
        return imgConnection;
    }

    @Override
    public String createImgConnection(@ApiParam(value = "connectionReq", required = true, name = "connectionReq")
                                              ConnectionReq connectionReq) {

        String shareId = UUID.randomUUID().toString();
        Share tblShare = new Share();


        tblShare.setShareId(shareId);
        tblShare.setImageId(connectionReq.getImageId());
        tblShare.setCreateTime(new Date(System.currentTimeMillis()));
        tblShare.setUpdateTime(new Date(System.currentTimeMillis()));
        tblShare.setPhaseStatus(PhaseStatus.ADDING);
        tblShare.setBaremetalId(connectionReq.getInstanceId());
        boolean ok = shareService.save(tblShare);
        if (!ok) {
            log.info("create share: update database error");
            return null;
        }

        return shareId;
    }

    @Override
    public String deleteImgConnection(@ApiParam(value = "shareId", required = true, name = "shareId")
                                              String shareId) {
        Share tblShare = shareService.getById(shareId);
        if (null == tblShare || REMOVED == tblShare.getPhaseStatus()) {
            log.info("get rsShare: null, shareId: {}", shareId);
            return null;
        }

        tblShare.setPhaseStatus(PhaseStatus.DELETING);
        tblShare.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = shareService.updateById(tblShare);
        if (!ok) {
            log.info("delete share: update database error");
            return null;
        }
        return shareId;
    }

    @Override
    public String createNodeImage(@ApiParam(name="imageId") String imageId, @ApiParam(name="nodeIp") String nodeIp,
                                  @ApiParam(name = "nodeStoragePoolId") String nodeStoragePoolId)
    {
        com.lnjoying.justice.repo.entity.Image tblImage = imageService.getById(imageId);
        if (null == tblImage || REMOVED == tblImage.getPhaseStatus())
        {
            log.info("image does not exist,imageId:{}", imageId);
            return null;
        }
        String poolId = getStoragePoolIdFromAgent(nodeIp, nodeStoragePoolId);
        if (StrUtil.isBlank(poolId)) return  null;
        String nodeImageId = getNodeImage(imageId,nodeIp, poolId);
        if (!StrUtil.isBlank(nodeImageId))
        {
            return nodeImageId;
        }
        NodeImage tblNodeImage = new NodeImage();
        tblNodeImage.setNodeImageId(Utils.assignUUId());
        tblNodeImage.setImageId(imageId);
        tblNodeImage.setStoragePoolIdFromAgent(poolId);
        tblNodeImage.setNodeIp(nodeIp);
        tblNodeImage.setUserId(tblImage.getUserId());
        tblNodeImage.setPhaseStatus(PhaseStatus.ADDING);
        tblNodeImage.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblNodeImage.setUpdateTime(tblNodeImage.getCreateTime());
        boolean ok = nodeImageService.save(tblNodeImage);
        if (!ok)
        {
            log.info("insert database error,{}", tblNodeImage);
            return null;
        }
        return tblNodeImage.getNodeImageId();
    }

    @Override
    public String getNodeImage(@ApiParam(name="nodeImageId") String nodeImageId)
    {
        NodeImage tblNodeImage = nodeImageService.getById(nodeImageId);
        if (null == tblNodeImage||REMOVED == tblNodeImage.getPhaseStatus())
        {
            log.info("NodeImage does not exist, nodeImageId:{}", nodeImageId);
            return null;
        }
        if (PhaseStatus.ADDED == tblNodeImage.getPhaseStatus())
        {
            return tblNodeImage.getNodeImageIdFromAgent();
        }
        log.info("NodeImage :{}", tblNodeImage);
        return null;
    }


    private String getStoragePoolIdFromAgent(String nodeIp, String nodeStoragePoolId)
    {
        NodeStoragePool nodeStoragePool = nodeStoragePoolService.getById(nodeStoragePoolId);
        if (null == nodeStoragePool || REMOVED == nodeStoragePool.getPhaseStatus() )
        {
            return null;
        }

        return nodeStoragePool.getStoragePoolIdFromAgent();

    }


    private String getNodeImage( String imageId, String nodeIp,
                                 String storagePoolIdFromAgent)
    {
        LambdaQueryWrapper<NodeImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NodeImage::getImageId, imageId)
                .eq(NodeImage::getNodeIp, nodeIp)
                .ne(NodeImage::getPhaseStatus, REMOVED)
                .eq(NodeImage::getStoragePoolIdFromAgent,storagePoolIdFromAgent);

        if (nodeImageService.count(queryWrapper) > 0)
        {
            return nodeImageService.getOne(queryWrapper).getNodeImageId();
        }
        return null;
    }

}
