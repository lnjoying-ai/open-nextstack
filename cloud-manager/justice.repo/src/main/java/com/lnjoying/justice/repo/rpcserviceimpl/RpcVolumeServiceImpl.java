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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lnjoying.justice.schema.service.repo.VolumeService;
import com.lnjoying.justice.repo.common.constant.AgentConstant;
import com.lnjoying.justice.repo.common.constant.OsType;
import com.lnjoying.justice.repo.common.constant.PhaseStatus;
import com.lnjoying.justice.repo.common.constant.VolumeType;
import com.lnjoying.justice.repo.config.RepoAgentConfig;
import com.lnjoying.justice.repo.domain.dto.request.VolumeCreateReq;
import com.lnjoying.justice.repo.domain.dto.response.VolumeBaseRsp;
import com.lnjoying.justice.repo.entity.Image;
import com.lnjoying.justice.repo.entity.Volume;
import com.lnjoying.justice.repo.service.ImageService;
import com.lnjoying.justice.repo.service.biz.VolumeServiceBiz;
import com.micro.core.common.Utils;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;
import static com.lnjoying.justice.repo.processor.VolumeTimerProcessor.putArgsFromAgent;

@RpcSchema(schemaId = "volumeService")
@Slf4j
public class RpcVolumeServiceImpl implements VolumeService
{
    @Autowired
    VolumeServiceBiz volumeServiceBiz;

    @Autowired
    com.lnjoying.justice.repo.service.VolumeService volumeService;

    @Autowired
    ImageService imageService;

    @Autowired
    private RepoAgentConfig repoAgentConfig;

    @Override
    public String createRootDisk(@ApiParam(name="userId") String userId, @ApiParam(name="size") Integer size, @ApiParam(name = "vmId") String vmId,
                                 @ApiParam(name="storagePoolId") String storagePoolId, @ApiParam(name = "nodeIp") String nodeIp,
                                 @ApiParam(name="imageId") String imageId, @ApiParam(name="vmInstanceName") String vmInstanceName)
    {
        VolumeCreateReq volumeCreateReq = new VolumeCreateReq();
        volumeCreateReq.setSize(size);
        volumeCreateReq.setStoragePoolId(storagePoolId);
        String volumeName = vmInstanceName+"-rootDisk";
        volumeCreateReq.setName(volumeName);
        VolumeBaseRsp baseRsp = volumeServiceBiz.createVolume(volumeCreateReq, userId,true,nodeIp, vmId, imageId );
        if (null == baseRsp) return  null;
        return baseRsp.getVolumeId();
    }

    @Override
    public Boolean canMigrate(@ApiParam(name="vmInstanceId") String vmInstanceId)
    {
        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Volume::getVmId, vmInstanceId)
                .isNull(Volume::getImageId)
                .and((wrapper)-> wrapper.ge(Volume::getPhaseStatus, PhaseStatus.ATTACHING)
                        .le(Volume::getPhaseStatus, PhaseStatus.AGENT_DETACHING)
                        .or().eq(Volume::getPhaseStatus,PhaseStatus.ADDING)
                        .or().eq(Volume::getPhaseStatus,PhaseStatus.DELETING)
                        .or().eq(Volume::getPhaseStatus,PhaseStatus.AGENT_ADDING)
                        .or().eq(Volume::getPhaseStatus, PhaseStatus.AGENT_DELETING));
        return volumeService.count(queryWrapper) == 0;
    }

    @Override
    public VolumeInfo setRootDiskAttached(@ApiParam(name="userId") String userId,
                                @ApiParam(name="volumeId") String volumeId)
    {
        return setRootDiskPhaseStatus(volumeId, PhaseStatus.ATTACHED);
    }

    @Override
    public VolumeInfo setRootDiskDetached(@ApiParam(name="volumeId") String volumeId)
    {
        return setRootDiskPhaseStatus(volumeId, PhaseStatus.DETACHED);
    }

    private VolumeInfo setRootDiskPhaseStatus(String volumeId, Integer phaseStatus)
    {
        Volume tblVolume = volumeService.getById(volumeId);
        if (Objects.isNull(tblVolume)) {
            return null;
        }
        tblVolume.setPhaseStatus(phaseStatus);
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = volumeService.updateById(tblVolume);
        if (!ok) return null;
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(volumeId);
        volumeInfo.setSize(tblVolume.getSize());
        return volumeInfo;
    }


    @Override
    public String createDataDisk(@ApiParam(name="userId") String userId, @ApiParam(name="size") Integer size, @ApiParam(name="volumeName") String volumeName,
                          @ApiParam(name="vmId") String vmId,@ApiParam(name="storagePoolId") String storagePoolId, @ApiParam(name = "nodeIp") String nodeIp)
    {
        VolumeCreateReq volumeCreateReq = new VolumeCreateReq();
        volumeCreateReq.setSize(size);
        volumeCreateReq.setName(volumeName);
        volumeCreateReq.setStoragePoolId(storagePoolId);
        VolumeBaseRsp baseRsp = volumeServiceBiz.createVolume(volumeCreateReq, userId,false,nodeIp, vmId, null );
        if (null == baseRsp) return  null;
        return baseRsp.getVolumeId();
    }


    @Override
    public String getVolumeIdFromAgent(@ApiParam(name = "volumeId") String volumeId)
    {
        Volume tblVolume = volumeServiceBiz.getVolumeById(volumeId);
        Integer volumePhase = tblVolume.getPhaseStatus();
        if (!StrUtil.isBlank(tblVolume.getVolumeIdFromAgent()) &&
                (PhaseStatus.ATTACHING == volumePhase || PhaseStatus.ADDED == volumePhase
                || PhaseStatus.RESUMED == volumePhase || PhaseStatus.ATTACHED == volumePhase))
        {
            return tblVolume.getVolumeIdFromAgent();
        }
        return null;
    }

    @Override
    public List<String> getVolumeIdFromAgentList(@ApiParam(name = "volumeList") List<String> volumeList)
    {
        return volumeList.stream().map(this::getVolumeIdFromAgent).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean detachVolumesByVmId(@ApiParam(name="vmId") String vmId, @ApiParam(name="removeRootDisk") Boolean removeRootDisk)
    {
        LambdaUpdateWrapper<Volume> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Volume::getVmId, vmId)
                .ne(Volume::getPhaseStatus, REMOVED);
        List<Volume> removeVolumes = volumeService.list(updateWrapper);
        Volume tblVolume = new Volume();
        tblVolume.setPhaseStatus(PhaseStatus.DETACHED);
        tblVolume.setVmId(null);
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = volumeService.update(tblVolume, updateWrapper);
        if (ok && removeRootDisk)
        {
            for(Volume removeVolume : removeVolumes)
            {
                if (VolumeType.ROOT_DISK == removeVolume.getType())
                {
                    removeVolume.setPhaseStatus(PhaseStatus.DELETING);
                    removeVolume.setVmId(null);
                    removeVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    ok = volumeService.updateById(removeVolume);
                }
            }
        }
        return ok;
    }

    @Override
    public Boolean removeDataVolume(@ApiParam(name="volumeIds") List<String> volumeIds)
    {
        LambdaUpdateWrapper<Volume> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Volume::getVolumeId, volumeIds)
                .ne(Volume::getPhaseStatus, REMOVED);
        Volume tblVolume = new Volume();
        tblVolume.setPhaseStatus(PhaseStatus.DELETING);
        tblVolume.setVmId(null);
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        return volumeService.update(tblVolume, updateWrapper);
    }

    @Override
    public String attachVolume(@ApiParam(name="volumeId") String volumeId,
                                  @ApiParam(name="vmId") String vmId,
                                   @ApiParam(name="nodeIp") String nodeIp)
    {
        Volume tblVolume = volumeService.getById(volumeId);
        if (null == tblVolume || REMOVED == tblVolume.getPhaseStatus())
        {
            return null;
        }
        if (StrUtil.isBlank(tblVolume.getNodeIp()))
        {
            setNodeIpPhase(tblVolume,nodeIp, vmId);
        }
        else if (!Objects.equals(nodeIp, tblVolume.getNodeIp()))
        {
            if (setDestIpPhase(tblVolume, nodeIp, vmId,0))
            {
                return volumeId;
            }
            else
            {
                return "";
            }
        }
        else if(!StrUtil.isBlank(tblVolume.getVolumeIdFromAgent())&&
                tblVolume.getPhaseStatus() == PhaseStatus.DETACHED)
        {
            log.info("set vmId: {}, volumeId:{}", vmId, volumeId);
            tblVolume.setVmId(vmId);
            tblVolume.setPhaseStatus(PhaseStatus.ATTACHING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            volumeService.updateById(tblVolume);
        }
        if (PhaseStatus.ATTACHED == tblVolume.getPhaseStatus())
        {
            tblVolume.setVmId(vmId);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            volumeService.updateById(tblVolume);
        }
        return volumeId;
    }

    @Override
    public List<String> attachVolumes(@ApiParam(name="volumeIds") List<String> volumeIds,
                                   @ApiParam(name="vmId") String vmId,
                                          @ApiParam(name="nodeIp") String nodeIp)
    {
        return volumeIds.stream().map(volumeId->attachVolume(volumeId,vmId,nodeIp)).collect(Collectors.toList());
    }

    @Override
    public ImageInfo getImageInfoByRecycleVolumeId(@ApiParam(name="volumeId") String volumeId)
    {
        Volume tblVolume = volumeService.getById(volumeId);
        if (null == tblVolume || REMOVED == tblVolume.getPhaseStatus())
        {
            return null;
        }
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setImageId(tblVolume.getImageId());
        if (StrUtil.isBlank(tblVolume.getImageId()))
        {
            return imageInfo;
        }
        Image tblImage = imageService.getById(tblVolume.getImageId());
        imageInfo.setOsType(OsType.LINUX == tblImage.getImageOsType()? "linux":"windows");
        return imageInfo;
    }

    @Override
    public List<VolumeInfo>  getVolumeInfosByVmId(@ApiParam(name="vmId") String vmId)
    {
        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Volume::getVmId, vmId)
                .isNull(Volume::getImageId)
                .ne(Volume::getPhaseStatus, REMOVED)
                .orderByDesc(Volume::getCreateTime);
        List<Volume> volumes = volumeService.list(queryWrapper);
        if (volumes.size() ==0 ) return  null;
        return volumes.stream().map(this::setVolumeInfo).collect(Collectors.toList());
    }


    public String getImageIdByRecycleVolumeId(@ApiParam(name="volumeId") String volumeId)
    {
        Volume tblVolume = volumeService.getById(volumeId);
        if (null == tblVolume || REMOVED == tblVolume.getPhaseStatus())
        {
            return null;
        }
        return tblVolume.getImageId();
    }



    private VolumeInfo setVolumeInfo(Volume tblVolume)
    {
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(tblVolume.getVolumeId());
        volumeInfo.setVolumeName(tblVolume.getName());
        volumeInfo.setSize(tblVolume.getSize());
        volumeInfo.setPhaseStatus(tblVolume.getPhaseStatus());
        volumeInfo.setType(tblVolume.getType());
        return volumeInfo;
    }

    private boolean setDestIpPhase(Volume tblVolume, String nodeIp, String vmId, int retry)
    {

        if (3 == retry)
        {
            return false;
        }
        String ok = suspendVolumeFromAgent(tblVolume);
        if (AgentConstant.OK.equals(ok))
        {
            tblVolume.setDestIp(nodeIp);
            tblVolume.setVmId(vmId);
//            tblVolume.setVolumeIdFromAgent("");
//            tblVolume.setNodeIp(nodeIp);
            tblVolume.setPhaseStatus(PhaseStatus.PRE_DEST_RESUMING);
            tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            return volumeService.updateById(tblVolume);
        }
        else
        {
            setDestIpPhase(tblVolume, nodeIp, vmId, retry+1);
        }
        return false;
    }

    private void setNodeIpPhase(Volume tblVolume, String nodeIp, String vmId)
    {
        tblVolume.setVmId(vmId);
        tblVolume.setNodeIp(nodeIp);
        tblVolume.setPhaseStatus(PhaseStatus.ADDING);
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        volumeService.updateById(tblVolume);
    }

    private String suspendVolumeFromAgent(Volume tblVolume)
    {
        String url = String.format("http://%s:%s%s/%s/suspend",
                tblVolume.getNodeIp(), repoAgentConfig.getVmAgentPort(), repoAgentConfig.getVolumeUrl(),
                tblVolume.getVolumeIdFromAgent());
        try
        {
            return putArgsFromAgent(url, null);
        }
        catch (Exception e)
        {
            log.error("suspend volume error:{}", e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean isDetached(@ApiParam(name="volumeId") String volumeId)
    {
        LambdaQueryWrapper<Volume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Volume::getVolumeId, volumeId)
                .isNull(Volume::getVmId)
                .eq(Volume::getPhaseStatus, PhaseStatus.DETACHED);
        return volumeService.count(queryWrapper) != 0;
    }

    @Override
    public String setDestIp(@ApiParam(name="vmId") String vmId,
                            @ApiParam(name="destIp") String destIp)
    {
        LambdaUpdateWrapper<Volume> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Volume::getVmId, vmId)
                .ne(Volume::getPhaseStatus, REMOVED);
        if (volumeService.count(updateWrapper) == 0)
        {
            return vmId;
        }
//        updateWrapper.orderByAsc(Volume::getCreateTime);
        Volume rootVolume =  volumeService.list(updateWrapper).get(0);
        if (destIp.equals(rootVolume.getNodeIp())) return vmId;
        String nodeIp = rootVolume.getNodeIp();
        Volume tblVolume = new Volume();
        tblVolume.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        tblVolume.setNodeIp(destIp);
        tblVolume.setDestIp(destIp);
        tblVolume.setVmId(vmId);
        tblVolume.setLastIp(nodeIp);
        if (!volumeService.update(tblVolume, updateWrapper))
        {
            return null;
        }
        return vmId;
    }

}
