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

package com.lnjoying.vm.service.timer;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lnjoying.vm.common.AgentConstant;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.entity.HypervisorNode;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.processor.VmInstanceHaTimerProcessor;
import com.lnjoying.vm.processor.VmInstanceTimerProcessor;
import com.lnjoying.vm.service.HypervisorNodeService;
import com.lnjoying.vm.service.VmInstanceService;
import com.micro.core.process.service.ScheduleProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;

@Service("vmInstanceHaTimerService")
@Slf4j
public class VmInstanceHaTimerService extends ScheduleProcessStrategy
{


    @Autowired
    private VmInstanceHaTimerProcessor vmInstanceHaTimerProcessor;

    @Autowired
    private VmInstanceTimerProcessor vmInstanceTimerProcessor;

    @Autowired
    private ComputeConfig ComputeConfig;

    @Autowired
    private VmInstanceService vmInstanceService;

    @Autowired
    private HypervisorNodeService hypervisorNodeService;

    public VmInstanceHaTimerService()
    {
        super("vmInstanceHa timer service", 1);
    }

    @PostConstruct
    public void start()
    {
        log.info("start vmInstanceHa  timer processor");

        CompletableFuture.runAsync(
                this::checkLocalNodeVmInstances
        );

        //15s
        int cycle = 15000;
        super.start(() -> vmInstanceHaTimerProcessor, 90000, cycle, null);
    }


    public void checkLocalNodeVmInstances()
    {
        log.info("checkLocalVmInstance start...");
        LambdaQueryWrapper<HypervisorNode> nodeQueryWrapper = new LambdaQueryWrapper<>();
        nodeQueryWrapper.eq(HypervisorNode::getManageIp, ComputeConfig.getNodeIp())
                .ne(HypervisorNode::getPhaseStatus, REMOVED);
        if (hypervisorNodeService.count(nodeQueryWrapper) > 0)
        {
            HypervisorNode tblHypervisorNode = hypervisorNodeService.getOne(nodeQueryWrapper);
            String nodeId = tblHypervisorNode.getNodeId();
            LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VmInstance::getNodeId, nodeId)
                    .ne(VmInstance::getPhaseStatus, REMOVED);
            long vmCount = vmInstanceService.count(queryWrapper);
            if (vmCount > 0)
            {
                if (Objects.equals(AgentConstant.OK, vmInstanceHaTimerProcessor.backupNodeIsHealthy(tblHypervisorNode)))
                {
                    log.info("hypervisorNode health check error, but backup node is healthy, nodeId:{}", tblHypervisorNode.getNodeId());
                    log.info("phase migrate_clean ,hypervisor nodeId:{}", tblHypervisorNode.getNodeId());
                    vmInstanceHaTimerProcessor.updateNewHypervisorNode(tblHypervisorNode);
                    vmInstanceHaTimerProcessor.createMonitorTagsFromNode(tblHypervisorNode);
                }
            }
        }

    }
}
