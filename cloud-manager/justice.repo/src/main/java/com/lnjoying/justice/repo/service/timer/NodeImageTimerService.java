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

package com.lnjoying.justice.repo.service.timer;


import com.lnjoying.justice.repo.processor.NodeImageTimerProcessor;
import com.micro.core.process.service.ScheduleProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service("nodeImageTimerService")
@Slf4j
public class NodeImageTimerService extends ScheduleProcessStrategy {


    @Autowired
    private NodeImageTimerProcessor nodeImageTimerProcessor;

    public NodeImageTimerService()
    {
        super("nodeImage timer service", 1);
    }

    @PostConstruct
    public void start()
    {
        log.info("start nodeImage  timer processor");

        //5s
        int cycle = 5000;
        super.start(() -> nodeImageTimerProcessor, 90000, cycle, null);
    }
}
