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

package com.lnjoying.justice.repo.service.biz;//package com.lnjoying.justice.repo.service.biz;
//
//
//import cn.hutool.core.util.StrUtil;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
//import com.lnjoying.justice.commonweb.exception.WebSystemException;
//import com.lnjoying.justice.schema.common.ErrorCode;
//import com.lnjoying.justice.schema.common.ErrorLevel;
//import com.lnjoying.justice.repo.common.constant.PhaseStatus;
//import com.lnjoying.justice.repo.domain.dto.request.NodeImageInfoReq;
//import com.lnjoying.justice.repo.domain.dto.request.VolumeSnapCreateReq;
//import com.lnjoying.justice.repo.domain.dto.response.NodeImageBaseRsp;
//import com.lnjoying.justice.repo.domain.dto.response.VolumeSnapBaseRsp;
//import com.lnjoying.justice.repo.entity.NodeImage;
//import com.lnjoying.justice.repo.entity.Volume;
//import com.lnjoying.justice.repo.entity.VolumeSnap;
//import com.lnjoying.justice.repo.service.ImageService;
//import com.lnjoying.justice.repo.service.NodeImageService;
//import com.lnjoying.justice.repo.service.StoragePoolService;
//import com.micro.core.common.Utils;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import javax.validation.constraints.NotEmpty;
//import javax.validation.constraints.NotNull;
//import java.util.Date;
//import java.util.UUID;
//
//import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;
//
//@Service("nodeImageServiceBiz")
//@Slf4j
//public class NodeImageServiceBiz
//{
//    @Autowired
//    NodeImageService nodeImageService;
//
//    @Autowired
//    ImageService imageService;
//
//    @Autowired
//    StoragePoolService storagePoolService;
//
//    public NodeImageBaseRsp addNodeImage(@NotNull NodeImageInfoReq nodeImageInfo, @NotEmpty String nodeId, String userId) throws WebSystemException
//    {
//        String nodeImageId = UUID.randomUUID().toString();
//        NodeImage tblNodeImage = new NodeImage();
//        tblNodeImage.setNodeImageId(nodeImageId);
//        tblNodeImage.setImageId(nodeImageInfo.getImageId());
//        tblNodeImage.setStoragePoolId(nodeImageInfo.getStoragePoolId());
//        tblNodeImage.setPhaseStatus(PhaseStatus.ADDING);
//        tblNodeImage.setCreateTime(new Date(System.currentTimeMillis()));
//        tblNodeImage.setUpdateTime(tblNodeImage.getCreateTime());
//        tblNodeImage.setUserId(userId);
//
//        boolean ok = nodeImageService.save(tblNodeImage);
//
//        if (!ok) {
//            log.error("insert tbl_node_image error");
//            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
//        }
//        NodeImageBaseRsp nodeImageBaseRsp = new NodeImageBaseRsp();
//        nodeImageBaseRsp.setNodeImageId(tblNodeImage.getNodeImageId());
//
//        return nodeImageBaseRsp;
//    }
//
//    public NodeImageBaseRsp getNodeImage(@NotNull NodeImageInfoReq nodeImageInfo, @NotEmpty String nodeId, String userId) throws WebSystemException
//    {
//        LambdaQueryWrapper<NodeImage> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(NodeImage::getNodeId, nodeId)
//                .eq(NodeImage::getImageId, nodeImageInfo.getImageId())
//                .eq(NodeImage::getStoragePoolId, nodeImageInfo.getStoragePoolId())
//                .ne(NodeImage::getPhaseStatus, REMOVED);
//        if(!StrUtil.isBlank(userId))
//        {
//            queryWrapper.eq(NodeImage::getUserId, userId);
//        }
//        NodeImage nodeImage = nodeImageService.getOne(queryWrapper);
//        NodeImageBaseRsp nodeImageBaseRsp = new NodeImageBaseRsp();
//        if (null == nodeImage) return  nodeImageBaseRsp;
//        nodeImageBaseRsp.setNodeImageId(nodeImage.getNodeImageId());
//        nodeImageBaseRsp.setAgentNodeImageId(nodeImage.getNodeImageIdFromAgent());
//        nodeImageBaseRsp.setPhaseStatus(nodeImage.getPhaseStatus());
//        return nodeImageBaseRsp;
//    }
//
//    public NodeImageBaseRsp getNodeImageById(@NotEmpty String nodeImageId) throws WebSystemException
//    {
//        NodeImage nodeImage = nodeImageService.getById(nodeImageId);
//        NodeImageBaseRsp nodeImageBaseRsp = new NodeImageBaseRsp();
//        if (null == nodeImage) return  nodeImageBaseRsp;
//        nodeImageBaseRsp.setNodeImageId(nodeImage.getNodeImageId());
//        nodeImageBaseRsp.setAgentNodeImageId(nodeImage.getNodeImageIdFromAgent());
//        nodeImageBaseRsp.setPhaseStatus(nodeImage.getPhaseStatus());
//        return nodeImageBaseRsp;
//    }
//
//    public NodeImageBaseRsp getOrCreateNodeImage(@NotNull NodeImageInfoReq nodeImageInfo, @NotEmpty String nodeId, String userId)throws WebSystemException
//    {
//        NodeImageBaseRsp nodeImageBaseRsp = getNodeImage(nodeImageInfo, nodeId, userId);
//        if (null == nodeImageBaseRsp.getNodeImageId())
//        {
//            return addNodeImage(nodeImageInfo, nodeId, userId);
//        }
//        return nodeImageBaseRsp;
//    }
//
//    public NodeImageBaseRsp delNodeImage(@NotNull NodeImageInfoReq nodeImageInfo, @NotEmpty String userId)
//    {
//        NodeImageBaseRsp nodeImageBaseRsp = new NodeImageBaseRsp();
//        LambdaUpdateWrapper<NodeImage> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.ne(NodeImage::getPhaseStatus, REMOVED)
//                .eq(NodeImage::getImageId, nodeImageInfo.getImageId())
//                .eq(NodeImage::getStoragePoolId, nodeImageInfo.getStoragePoolId())
//                .eq(NodeImage::getUserId, userId);
//        NodeImage nodeImage = new NodeImage();
//        nodeImage.setPhaseStatus(PhaseStatus.DELETING);
//        nodeImage.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
//        boolean ok = nodeImageService.update(nodeImage, updateWrapper);
//
//        if (!ok)
//        {
//            log.error("update tbl_node_image error");
//            return nodeImageBaseRsp;
//        }
//        nodeImageBaseRsp.setPhaseStatus(PhaseStatus.DELETING);
//        return nodeImageBaseRsp;
//    }
//}
