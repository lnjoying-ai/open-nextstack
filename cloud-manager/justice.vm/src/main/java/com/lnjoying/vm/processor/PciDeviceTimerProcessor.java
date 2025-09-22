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

package com.lnjoying.vm.processor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.common.ComputeUrl;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.backend.request.GPUAttachReq;
import com.lnjoying.vm.domain.backend.response.BaseRsp;
import com.lnjoying.vm.domain.backend.response.GPURspFromAgent;
import com.lnjoying.vm.domain.backend.response.GpusRspFromAgent;
import com.lnjoying.vm.domain.dto.response.VmInstanceAbbrInfo;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.PciDevice;
import com.lnjoying.vm.mapper.DeviceDetailInfoMapper;
import com.lnjoying.vm.mapper.PciDeviceMapper;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.PciDeviceService;
import com.lnjoying.vm.service.biz.LogRpcService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.processor.AbstractRunnableProcessor;
import com.micro.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Slf4j
@Component
public class PciDeviceTimerProcessor extends AbstractRunnableProcessor

{
    public PciDeviceTimerProcessor()
    {
    }

    @Autowired
    private PciDeviceService pciDeviceService;

    @Autowired
    private HypervisorNodeService nodeService;

    @Resource
    private DeviceDetailInfoMapper deviceDetailInfoMapper;

    @Autowired
    private ComputeConfig computeConfig;

    @Autowired
    private LogRpcService logRpcService;

    @Resource
    private PciDeviceMapper pciDeviceMapper;

    @Override
    public void start()
    {
        log.info("pci device timer processor start");
    }

    @Override
    public void stop()
    {
        log.info("pci device timer processor stop");
    }

    @Override
    public void run()
    {
        try
        {
            processPciDevices(getMiddleStatusPciDevices());
            updateRemovedInstancePciDevice();
        }
        catch (Exception e)
        {
            log.error("pci device timer processor exception: {}", e.getMessage());
        }
    }

    private List<PciDevice> getMiddleStatusPciDevices()
    {
        LambdaQueryWrapper<PciDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_DETACHED)
                .ne(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_ATTACHED)
                .ne(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_ATTACH_FAILED)
                .ne(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_DETACH_FAILED)
                .ne(PciDevice::getPhaseStatus, VmInstanceStatus.DEVICE_INIT_CREATE)
                .ne(PciDevice::getPhaseStatus, REMOVED);
        return pciDeviceService.list(queryWrapper);
    }

    private List<PciDevice> getRemovedInstancePciDevices()
    {
        return pciDeviceMapper.selectRemovedPciDevice();
    }

    private void updateRemovedInstancePciDevice()
    {
        List<PciDevice> removedInstancePciDevices = getRemovedInstancePciDevices();
        if (null == removedInstancePciDevices || removedInstancePciDevices.isEmpty())
        {
            return;
        }
        for (PciDevice tblPciDevice : removedInstancePciDevices)
        {
            try
            {
                tblPciDevice.setVmInstanceId(null);
                tblPciDevice.setUserId(null);
                tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHED);
                pciDeviceService.updateById(tblPciDevice);
            }
            catch (Exception e)
            {
                log.error("update removed instance pci device  error: {}", e.getMessage());
            }
        }
    }

    private void processPciDevices(List<PciDevice> tblPciDevices)
    {
        try
        {
            log.debug("get tblPciDevices :{}", tblPciDevices);
            for (PciDevice tblPciDevice : tblPciDevices)
            {
                processPciDevice(tblPciDevice);
            }
        }
        catch (Exception e)
        {
            log.error("pci devices timer processor error:  {}", e.getMessage());
        }
    }

    private void processPciDevice(PciDevice tblPciDevice)
    {
        int phaseStatus = tblPciDevice.getPhaseStatus();
        try
        {
            switch (phaseStatus)
            {
//                case VmInstanceStatus.INSTANCE_INIT:
//                    processInitDevice(tblPciDevice);
//                    break;
                case VmInstanceStatus.DEVICE_ATTACHING:
                    processAttachDevice(tblPciDevice);
                    break;
                case VmInstanceStatus.DEVICE_DETACHING:
                    processDetachDevice(tblPciDevice);
                    break;
                case VmInstanceStatus.DEVICE_AGENT_ATTACHING:
                    getProcessDeviceResult(tblPciDevice, VmInstanceStatus.DEVICE_AGENT_ATTACHING);
                    break;
                case VmInstanceStatus.DEVICE_AGENT_DETACHING:
                    getProcessDeviceResult(tblPciDevice, VmInstanceStatus.DEVICE_AGENT_DETACHING);
                    break;
            }
        }
        catch (Exception e)
        {
//            e.printStackTrace();
            log.error("pci device timer processor error: deviceId {}, phase status {} , exception {}", tblPciDevice.getDeviceId(), phaseStatus, e.getMessage());
        }
    }

    private void processInitDevice(PciDevice tblPciDevice)
    {
        // 获取创建时间与当前时间的差值，超过15分钟，再进行处理
        long createTime = tblPciDevice.getCreateTime().getTime();
        long currentTime = System.currentTimeMillis();
        if (currentTime - createTime > 15 * 60 * 1000)
        {
            GPURspFromAgent gpuRspFromAgent = getDeviceStatusFromAgent(tblPciDevice);
            if (Objects.equals(gpuRspFromAgent.getPhaseType(), "attach"))
            {
                if (Objects.equals(gpuRspFromAgent.getPhase(), AgentConstant.SUCCESS))
                {
                    tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACHED);
                }
                else if (Objects.equals(gpuRspFromAgent.getPhase(), "attach_failed"))
                {
                    tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACH_FAILED);
                }
                if (StrUtil.isNotBlank(gpuRspFromAgent.getVmId()))
                {
                    tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                    pciDeviceService.updateById(tblPciDevice);
                }
            }
        }
    }

    private void processAttachDevice(PciDevice tblPciDevice)
    {
        try
        {
            String ok = attachDeviceFromAgent(tblPciDevice);
            assert ok != null;
            if (ok.equals(AgentConstant.FAILED))
            {
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACH_FAILED);
            }
            else if (!ok.equals(AgentConstant.OK))
            {
                log.info("attachDeviceFromAgent error, deviceId:{}", tblPciDevice.getDeviceId());
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
            }
            else
            {
                tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_AGENT_ATTACHING);
            }
            tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean dbOk = pciDeviceService.updateById(tblPciDevice);
            if (dbOk && ok.equals(AgentConstant.OK))
            {
                logRpcService.getLogService().addEvent(tblPciDevice.getUserId(), "挂载PCI设备",
                        String.format("请求参数: deviceId:%s", tblPciDevice.getDeviceId()), "挂载中");
                //            logRpcService.getLogService().addEvent(tblPciDevice.getUserId(),);
            }
            if (dbOk && ok.equals(AgentConstant.FAILED))
            {
                logRpcService.getLogService().addEvent(tblPciDevice.getUserId(), "挂载PCI设备",
                        String.format("请求参数: deviceId:%s", tblPciDevice.getDeviceId()), "挂载失败");
            }
        }
        catch (WebSystemException e)
        {
            log.error("attach device error: deviceId {}, {}", tblPciDevice.getDeviceId(), e.getMessage());
        }
    }

    private void processDetachDevice(PciDevice tblPciDevice)
    {
        String ok = detachDeviceFromAgent(tblPciDevice);
        assert ok != null;
        if (ok.equals(AgentConstant.FAILED))
        {
            tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACH_FAILED);
        }
        else if (!ok.equals(AgentConstant.OK))
        {
            log.info("detachDeviceFromAgent error, deviceId:{}", tblPciDevice.getDeviceId());
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        else
        {
            tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_AGENT_DETACHING);
        }


//        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_AGENT_DETACHING);
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean dbOk = pciDeviceService.updateById(tblPciDevice);
        if (dbOk && ok.equals(AgentConstant.OK))
        {
            logRpcService.getLogService().addEvent(tblPciDevice.getUserId(), "卸载PCI设备",
                    String.format("请求参数: deviceId:%s", tblPciDevice.getDeviceId()), "卸载中");
            //            logRpcService.getLogService().addEvent(tblPciDevice.getUserId(),);
        }
        if (dbOk && ok.equals(AgentConstant.FAILED))
        {
            logRpcService.getLogService().addEvent(tblPciDevice.getUserId(), "卸载PCI设备",
                    String.format("请求参数: deviceId:%s", tblPciDevice.getDeviceId()), "卸载失败");
        }
    }


    private void getProcessDeviceResult(PciDevice tblPciDevice, Integer phase)
    {
        GPURspFromAgent gpuRspFromAgent = getDeviceStatusFromAgent(tblPciDevice);
        String result = null;
        if (phase == VmInstanceStatus.DEVICE_AGENT_ATTACHING)
        {
            if (!Objects.equals(gpuRspFromAgent.getPhaseType(), "attach"))
            {
                return;
            }
            switch (Objects.requireNonNull(gpuRspFromAgent.getPhase()))
            {
                case AgentConstant.FAIL:
                    if (StrUtil.isNotBlank(gpuRspFromAgent.getVmId()))
                    {
                        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACHED);
                        result = "挂载成功";
                        break;
                    }
                    tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACH_FAILED);
                    result = "挂载失败";
                    break;
                case AgentConstant.SUCCESS:
                    tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_ATTACHED);
                    result = "挂载成功";
                    break;
                default:
                    return;
            }
        }

        if (phase == VmInstanceStatus.DEVICE_AGENT_DETACHING)
        {
            String detachPhase = "";
            if (Objects.equals(gpuRspFromAgent.getPhaseType(), "detach"))
            {
                detachPhase = gpuRspFromAgent.getPhase();
            }
            String vmInstanceId = gpuRspFromAgent.getVmId();

            switch (detachPhase)
            {
                case AgentConstant.SUCCESS:
                    if (StrUtil.isNotBlank(vmInstanceId))
                    {
                        log.error("detach device error: deviceId {}, vmInstanceId {}", tblPciDevice.getDeviceId(), vmInstanceId);
                        tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACH_FAILED);
                        result = "卸载失败";
                        break;
                    }
                    tblPciDevice.setVmInstanceId(null);
                    tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACHED);
                    tblPciDevice.setUserId(null);
                    result = "未挂载(卸载成功)";
                    break;
                case AgentConstant.FAIL:
                    tblPciDevice.setPhaseStatus(VmInstanceStatus.DEVICE_DETACH_FAILED);
                    result = "卸载失败";
                    break;
                default:
                    return;
            }
        }
        tblPciDevice.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean dbOk = pciDeviceService.updateById(tblPciDevice);
        if (dbOk)
        {
            logRpcService.getLogService().addEvent(tblPciDevice.getUserId(), "获取PCI设备状态",
                    String.format("请求参数: deviceId:%s", tblPciDevice.getDeviceId()), result);
        }
    }

    public GPURspFromAgent getDeviceStatusFromAgent(PciDevice tblPciDevice)
    {
        try
        {
            String nodeIp = getNodeIp(tblPciDevice);
            if (StrUtil.isBlank(nodeIp))
            {
                return null;
            }
            String url = String.format("http://%s:%s%s/%s", nodeIp, computeConfig.getVmAgentPort(), ComputeUrl.V1_GPU_URL, tblPciDevice.getDeviceIdFromAgent());
            GPURspFromAgent gpuRspFromAgent = HttpActionUtil.getObject(url, GPURspFromAgent.class);
            log.info("url:{}, get result:{}", url, gpuRspFromAgent);
            if (!StrUtil.isBlank(gpuRspFromAgent.getReason()) && gpuRspFromAgent.getReason().contains(AgentConstant.NOT_EXIST))
            {
                gpuRspFromAgent.setPhase(AgentConstant.NOT_EXIST);
            }
            return gpuRspFromAgent;
        }
        catch (Exception e)
        {
            log.error("getDeviceStatusFromAgent error:{}", e.getMessage());
            return null;
        }
    }

    public String getNodeIp(PciDevice tblPciDevice)
    {
        String nodeId = tblPciDevice.getNodeId();
        if (StrUtil.isBlank(nodeId))
        {
            log.error("get nodeId null, pciDeviceId:{}", tblPciDevice.getDeviceId());
            return null;
        }
        HypervisorNode node = nodeService.getById(nodeId);
        if (null == node || REMOVED == node.getPhaseStatus())
        {
            log.error("get node null, nodeId:{}", nodeId);
            return null;
        }
        return node.getManageIp();
    }

    public String attachDeviceFromAgent(PciDevice tblPciDevice)
    {
        VmInstanceAbbrInfo vmInstanceAbbrInfo = deviceDetailInfoMapper.selectVmInstanceAbbrInfoByDeviceId(tblPciDevice.getDeviceId());
        log.info("get vmInstanceAbbrInfo:{}", vmInstanceAbbrInfo);
        if (StrUtil.isBlank(vmInstanceAbbrInfo.getVmInstanceIdFromAgent()))
        {
            return null;
        }

        HypervisorNode node = nodeService.getById(vmInstanceAbbrInfo.getNodeId());
        if (null == node || StrUtil.isBlank(node.getManageIp()))
        {
            return null;
        }

        String url = String.format("http://%s:%s%s/%s/attach", node.getManageIp(), computeConfig.getVmAgentPort(), ComputeUrl.V1_GPU_URL, tblPciDevice.getDeviceIdFromAgent());
        GPUAttachReq gpuAttachReq = new GPUAttachReq();
        gpuAttachReq.setVmInstanceId(vmInstanceAbbrInfo.getVmInstanceIdFromAgent());
        String jsonString = JsonUtil.objectToJson(gpuAttachReq);
        try
        {
            return putArgsFromAgent(url, jsonString);
        }
        catch (Exception e)
        {
            log.error("attach device error:{}", e.getMessage());
            return null;
        }
    }

    public String detachDeviceFromAgent(PciDevice tblPciDevice)
    {
        String nodeIp = getNodeIp(tblPciDevice);
        if (StrUtil.isBlank(nodeIp))
        {
            return null;
        }
        String url = String.format("http://%s:%s%s/%s/detach", nodeIp, computeConfig.getVmAgentPort(), ComputeUrl.V1_GPU_URL, tblPciDevice.getDeviceIdFromAgent());

        try
        {
            return putArgsFromAgent(url, null);
        }
        catch (Exception e)
        {
            log.error("detach device error:{}", e.getMessage());
            return null;
        }
    }

    public static String putArgsFromAgent(String url, String jsonString)
    {
        BaseRsp result = HttpActionUtil.put(url, jsonString, BaseRsp.class);

        if (null == result)
        {
            log.error("get response error,  url:{}, httpAction: put, jsonStr:{}", url, jsonString);
            return null;
        }

        String status = result.getStatus();
        if (AgentConstant.FAILED.equals(status) && AgentConstant.GPU_NOT_ATTACHED.equals(result.getReason()))
        {
            return AgentConstant.OK;
        }
        else if (AgentConstant.FAILED.equals(status))
        {
            log.info("get response error, url:{}, httpAction: put, jsonStr:{},status:{}, result:{}", url, jsonString, status, result);
            return AgentConstant.FAILED;
        }
        if (!AgentConstant.PENDING_STATUS.equals(status))
        {
            log.info("get response error, url:{}, httpAction: put, jsonStr:{},status:{}, result:{}", url, jsonString, status, result);
            return null;
        }
        return AgentConstant.OK;
    }

    public List<String> getGpusFromAgent(HypervisorNode tblHypervisorNode)
    {
        int vmAgentPort = ComputeUrl.VM_AGENT_PORT;
        String url = "http://" + tblHypervisorNode.getManageIp() + ":" + vmAgentPort + ComputeUrl.V1_GPU_URL;
        GpusRspFromAgent gpusRspFromAgent = HttpActionUtil.getObject(url, GpusRspFromAgent.class);
        return gpusRspFromAgent.getGpuIds();
    }
}
