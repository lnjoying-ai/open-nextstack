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

package com.lnjoying.justice.repo.processor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.config.RepoAgentConfig;
import com.lnjoying.justice.repo.domain.backend.request.NodeImageCreateReqFromAgent;
import com.lnjoying.justice.repo.domain.backend.response.BaseRsp;
import com.lnjoying.justice.repo.domain.backend.response.NodeImageRspFromAgent;
import com.lnjoying.justice.repo.domain.backend.response.NodeImagesRspFromAgent;
import com.lnjoying.justice.repo.entity.Image;
import com.lnjoying.justice.repo.entity.NodeImage;
import com.lnjoying.justice.repo.service.ImageService;
import com.lnjoying.justice.repo.service.NodeImageService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Component
@Slf4j
public class NodeImageTimerProcessor extends AbstractRunnableProcessor
{

    @Autowired
    private NodeImageService nodeNodeImageService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private RepoAgentConfig repoAgentConfig;

    public NodeImageTimerProcessor()
    {
    }

    @Override
    public void start()
    {
        log.info("nodeImage timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("nodeImage timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processNodeImages(getMiddleStatusNodeImages());
        }
        catch (Exception e)
        {
            log.error("nodeImage timer processor exception: {}", e.getMessage());
        }
    }

    private List<NodeImage> getMiddleStatusNodeImages()
    {
        LambdaQueryWrapper<NodeImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(NodeImage::getPhaseStatus, PhaseStatus.ADDED )
                .ne(NodeImage::getPhaseStatus, PhaseStatus.ADD_FAILED)
                .ne(NodeImage::getPhaseStatus, PhaseStatus.DELETE_FAILED)
                .ne(NodeImage::getPhaseStatus,REMOVED);
        return nodeNodeImageService.list(queryWrapper);
    }

    private void processNodeImages(List<NodeImage> tblNodeImages)
    {
        try
        {
            log.debug("get tblNodeImages :{}", tblNodeImages);
            for ( NodeImage tblNodeImage: tblNodeImages )
            {
                processNodeImage(tblNodeImage);
            }
        }
        catch (Exception e)
        {
            log.error("nodeImage timer processor error:  {}", e.getMessage());
        }
    }

    private void processNodeImage(NodeImage tblNodeImage)
    {
        int phaseStatus = tblNodeImage.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
                case PhaseStatus.ADDING:
                    processCreateNodeImage(tblNodeImage);
                    break;
                case PhaseStatus.DELETING:
                    processRemoveNodeImage(tblNodeImage);
                    break;
                default:
                    defaultProcessNodeImage(tblNodeImage);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("nodeImage timer processor error: nodeImageId {}, phase status {} , exception {}", tblNodeImage.getNodeImageId(), phaseStatus, e.getMessage());
        }
    }

    private void processCreateNodeImage(NodeImage tblNodeImage)
    {
        try
        {
            String nodeImageIdFromAgent = createNodeImageFromAgent(tblNodeImage);
            if(StrUtil.isBlank(nodeImageIdFromAgent))
            {
                tblNodeImage.setPhaseStatus(PhaseStatus.ADD_FAILED);
            }
            else
            {
                tblNodeImage.setPhaseStatus(PhaseStatus.AGENT_ADDING);
                tblNodeImage.setNodeImageIdFromAgent(nodeImageIdFromAgent);
            }
            tblNodeImage.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = nodeNodeImageService.updateById(tblNodeImage);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            log.info("created nodeImage:{} nodeImageIdFromAgent: {}", tblNodeImage.getNodeImageId(), nodeImageIdFromAgent);
        }
        catch (WebSystemException e)
        {
            log.error("create nodeImage error: nodeImageId {}, {}", tblNodeImage.getNodeImageId(), e.getMessage());
        }
    }

    private void processRemoveNodeImage(NodeImage tblNodeImage)
    {
        try
        {
            String nodeImageIdFromAgent = removeNodeImageFromAgent(tblNodeImage);
            if (null == nodeImageIdFromAgent)
            {
                log.info("removeNodeImageFromAgent error, nodeImageId:{}", tblNodeImage.getNodeImageId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            tblNodeImage.setPhaseStatus(PhaseStatus.AGENT_DELETING);
            tblNodeImage.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            nodeNodeImageService.updateById(tblNodeImage);
        }
        catch (WebSystemException e)
        {
            log.error("remove nodeImage error: nodeImageId {}, {}", tblNodeImage.getNodeImageId(), e.getMessage());
        }
    }

    private void defaultProcessNodeImage(NodeImage tblNodeImage)
    {
        try
        {
            NodeImageRspFromAgent nodeImageRspFromAgent = getNodeImageStatusFromAgent(tblNodeImage);

            switch (Objects.requireNonNull(Objects.requireNonNull(nodeImageRspFromAgent).getPhase()))
            {
                case AgentConstant.SUCCESS:
                    if (nodeImageRspFromAgent.getPhaseType().equals(AgentConstant.ADD))
                    {
                        tblNodeImage.setPhaseStatus(PhaseStatus.ADDED);
                        tblNodeImage.setNodeImageIdFromAgent(nodeImageRspFromAgent.getNodeImageIdFromAgent());
                    }
                    break;
                case AgentConstant.IMG_NOT_EXIST:
                    tblNodeImage.setPhaseStatus(REMOVED);
                    break;
                case AgentConstant.FAIL:
                    if (nodeImageRspFromAgent.getPhaseType().equals(AgentConstant.ADD))
                    {
                        tblNodeImage.setPhaseStatus(PhaseStatus.ADD_FAILED);
                    }
                    else if (nodeImageRspFromAgent.getPhaseType().equals(AgentConstant.DEL))
                    {
                        tblNodeImage.setPhaseStatus(PhaseStatus.DELETE_FAILED);
                    }
                    break;
                default:
                    return;
            }
            tblNodeImage.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = nodeNodeImageService.updateById(tblNodeImage);
            if (!ok)
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        catch (Exception e)
        {
            log.error("getNodeImageCreateStatus error:{}, nodeImageId:{}, ", e.getMessage(), tblNodeImage.getNodeImageId());
        }
    }

    private String createNodeImageFromAgent(NodeImage tblNodeImage)
    {
        String nodeIp = tblNodeImage.getNodeIp();
        String url = String.format("http://%s:%s%s/%s/imgs", nodeIp, repoAgentConfig.getVmAgentPort(), repoAgentConfig.getPoolUrl(),tblNodeImage.getStoragePoolIdFromAgent());

        Image tblImage = imageService.getById(tblNodeImage.getImageId());
        String desc = tblImage.getImageName();
        String name = tblImage.getImageName();
        if (!tblImage.getIsPublic())
        {
            name = tblImage.getImageId();
        }
        NodeImageCreateReqFromAgent nodeImageCreateReqFromAgent = new NodeImageCreateReqFromAgent();
        nodeImageCreateReqFromAgent.setName(name);
        nodeImageCreateReqFromAgent.setDesc(desc);
        if(StrUtil.isBlank(desc))
        {
            nodeImageCreateReqFromAgent.setDesc(name);
        }
        ResponseEntity<NodeImageRspFromAgent> response = HttpActionUtil.postForEntity(url, nodeImageCreateReqFromAgent, NodeImageRspFromAgent.class);
        NodeImageRspFromAgent nodeImageRspFromAgent = response.getBody();
        if (null == nodeImageRspFromAgent)
        {
            log.error("get response of creating nodeImage  error,  nodeImageId:{}", tblNodeImage.getNodeImageId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(nodeImageRspFromAgent.getReason()))
        {
            if (nodeImageRspFromAgent.getReason().contains(AgentConstant.ALREADY_EXISTS))
            {
                log.info("nodeImage already exists, nodeImageId:{}", tblNodeImage.getNodeImageId());
                return getNodeImageIdByName(tblNodeImage, name);
            }
            return null;
        }
        else if (StrUtil.isBlank(nodeImageRspFromAgent.getNodeImageIdFromAgent()))
        {
            log.error("nodeImageId is null, nodeImageRspFromAgent: {}", nodeImageCreateReqFromAgent);
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        return nodeImageRspFromAgent.getNodeImageIdFromAgent();
//        String jsonString = JsonUtil.objectToJson(nodeImageCreateReqFromAgent);
//        String result = HttpActionUtil.post(url,jsonString);
//        Map resultMap = JsonUtil.jsonToMap(result);
//        if (null == resultMap)
//        {
//            log.error("get response of creating nodeImage  error,  nodeImageId:{}", tblNodeImage.getNodeImageId());
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//        }
//
//        String status = (String) resultMap.get("status");
//        if (!AgentConstant.PENDING.equals(status))
//        {
//            log.info("create nodeImage error, status:{}, result:{}",status, resultMap);
//            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//        }
//        return (String)resultMap.get("uuid");
//        return null;
    }

    private String removeNodeImageFromAgent(NodeImage tblNodeImage)
    {
        String url = String.format("http://%s:%s/%s/%s", tblNodeImage.getNodeIp(),repoAgentConfig.getVmAgentPort(),
                repoAgentConfig.getImageUrl(), tblNodeImage.getNodeImageIdFromAgent());
//        Image tblImage = imageService.getById(tblNodeImage.getImageId());
//        if (!tblImage.getIsPublic())
//        {
//            url = String.format("http://%s:%s/%s/%s",tblNodeImage.getNodeIp(),repoAgentConfig.getVmAgentPort(),
//                    repoAgentConfig.getImageUrl(), tblNodeImage.getNodeImageIdFromAgent());
//        }
        BaseRsp result = HttpActionUtil.delete(url, BaseRsp.class);

        if (null == result)
        {
            log.error("get response of removing nodeImage error,  sgId:{}", tblNodeImage.getNodeImageIdFromAgent());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        if (AgentConstant.PENDING.equals(result.getStatus()) ||
                (AgentConstant.FAILED.equals(result.getStatus()) && result.getReason().contains(AgentConstant.NOT_EXIST)))
        {
            return tblNodeImage.getNodeImageIdFromAgent();
        }

        return null;
    }

    private NodeImageRspFromAgent getNodeImageStatusFromAgent(NodeImage tblNodeImage)
    {
        try
        {
            String url = String.format("http://%s:%s/%s/%s", tblNodeImage.getNodeIp(),repoAgentConfig.getVmAgentPort(),
                    repoAgentConfig.getImageUrl(), tblNodeImage.getNodeImageIdFromAgent());
            NodeImageRspFromAgent nodeImageRspFromAgent = HttpActionUtil.getObject(url, NodeImageRspFromAgent.class);
            log.info("url:{}, get result:{}", url, nodeImageRspFromAgent);
            if (AgentConstant.IMG_NOT_EXIST.equals(nodeImageRspFromAgent.getReason()))
            {
                nodeImageRspFromAgent.setPhase(AgentConstant.IMG_NOT_EXIST);
            }
            return nodeImageRspFromAgent;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String getNodeImageIdByName(NodeImage tblNodeImage, String imgName)
    {
        String url = String.format("http://%s:%s%s/%s/imgs", tblNodeImage.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getPoolUrl(),tblNodeImage.getStoragePoolIdFromAgent());

        try
        {
            NodeImagesRspFromAgent rsp = HttpActionUtil.getObject(url, NodeImagesRspFromAgent.class);
            if (null == rsp)
            {
                log.error("get response of getting nodeImage error,  nodeImageId:{}", tblNodeImage.getNodeImageId());
                return null;
            }
            if (rsp.getImgCount() > 0)
            {
                for (String imgId : rsp.getImageIds())
                {
                    String imgUrl = String.format("http://%s:%s/%s/%s", tblNodeImage.getNodeIp(), repoAgentConfig.getVmAgentPort(),
                            repoAgentConfig.getImageUrl(), imgId);
                    NodeImageRspFromAgent nodeImageRspFromAgent = HttpActionUtil.getObject(imgUrl, NodeImageRspFromAgent.class);
                    if (null == nodeImageRspFromAgent)
                    {
                        log.error("get response of getting nodeImage error,  nodeImageId:{}", tblNodeImage.getNodeImageId());
                        return null;
                    }
                    if (imgName.equals(nodeImageRspFromAgent.getName()))
                    {
                        return imgId;
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("get response of getting nodeImage error,  nodeImageId:{}", tblNodeImage.getNodeImageId());
            return null;
        }
        return null;
    }
}

