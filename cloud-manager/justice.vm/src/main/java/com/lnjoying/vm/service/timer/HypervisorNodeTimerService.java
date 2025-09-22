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

import com.lnjoying.vm.processor.HypervisorNodeTimerProcessor;
import com.micro.core.process.service.ScheduleProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;


@Service("hypervisorNodeTimerService")
@Slf4j
@EnableTransactionManagement
public class HypervisorNodeTimerService extends ScheduleProcessStrategy
{
    @Autowired
    private HypervisorNodeTimerProcessor hypervisorNodeTimerProcessor;

    public HypervisorNodeTimerService()
    {
        super("hypervisorNodeTimerService timer service", 1);
    }

    @PostConstruct
    public void start()
    {
        log.info("start hypervisorNode  timer processor");

        //5s
        int cycle = 5000;
        super.start(() -> hypervisorNodeTimerProcessor, 90000, cycle, null);
    }
}
