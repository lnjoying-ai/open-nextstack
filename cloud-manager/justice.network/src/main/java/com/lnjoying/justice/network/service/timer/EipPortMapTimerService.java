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

package com.lnjoying.justice.network.service.timer;

import com.lnjoying.justice.network.processor.EipPortMapTimerProcessor;
import com.micro.core.process.service.ScheduleProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service("eipPortMapTimerService")
@Slf4j
public class EipPortMapTimerService extends ScheduleProcessStrategy {

    @Autowired
    private EipPortMapTimerProcessor eipPortMapTimerProcessor;

    public EipPortMapTimerService()
    {
        super("eipPortMap timer service", 2);
    }

    @PostConstruct
    public void start()
    {
        log.info("start eipPortMap  timer processor");

        //5s
        int cycle = 5000;
        super.start(() -> eipPortMapTimerProcessor, 90000, cycle, null);
    }
}
