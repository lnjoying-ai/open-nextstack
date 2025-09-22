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

import com.lnjoying.justice.network.config.NetworkAgentConfig;
import com.lnjoying.justice.network.domain.backend.response.SwitchGetFromAgentRsp;
import com.lnjoying.justice.network.domain.backend.response.SwitchesGetFromAgentRsp;
import com.lnjoying.justice.network.processor.VpcTimerProcessor;
import com.micro.core.nework.http.HttpActionUtil;
import com.micro.core.process.service.ScheduleProcessStrategy;
import com.micro.core.utils.JsonUtil;
import com.micro.core.utils.UnderlineAndHump;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service("vpcTimerService")
@Slf4j
public class VpcTimerService extends ScheduleProcessStrategy
{

//    private static final Logger LOGGER = LogManager.getLogger(VpcTimerService.class);

    public static final ConcurrentHashMap<String,String> switchMap = new ConcurrentHashMap<>();

    @Autowired
    private VpcTimerProcessor vpcTimerProcessor;

    @Autowired
    NetworkAgentConfig networkAgentConfig;

    public VpcTimerService()
    {
        super("vpc timer service", 1);
    }

    @PostConstruct
    public void start()
    {
        log.info("start vpc  timer processor");
        CompletableFuture.runAsync(
                ()->{
                    for(int i=0;i<5;i++)
                    {
                        try
                        {
                            Thread.sleep(60000);
                        }
                        catch (InterruptedException e)
                        {
                            log.error(" thread.sleep error:{}", e.getMessage());
                        }
//                        getSwitch();
                    }
                }

        );

        //5s
        int cycle = 5000;
        super.start(() -> vpcTimerProcessor, 90000, cycle, null);
    }

    public void getSwitch()
    {
        try
        {
            String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSwitchUrl();
            log.info("get switch url: {}", url);
            String result = HttpActionUtil.get(url);
            result = UnderlineAndHump.underlineToHump(result);
            SwitchesGetFromAgentRsp switchesRsp = JsonUtil.jsonToPojo(result, SwitchesGetFromAgentRsp.class);
            if (null == switchesRsp)
            {
                log.info("get switches error: null");
                return;
            }
            for (String id : switchesRsp.getSwitchUuids())
            {
                putDataToSwitchMap(id);
            }
        }catch (Exception e)
        {
            log.error("get switches error:{}", e.getMessage());
        }
    }

    //add data to switchMap
    public void putDataToSwitchMap(String switchId)
    {
        String url = networkAgentConfig.getNetworkAgentService() + networkAgentConfig.getSwitchUrl() + "/" + switchId;
        String result = HttpActionUtil.get(url);
        result = UnderlineAndHump.underlineToHump(result);
        SwitchGetFromAgentRsp switchRsp = JsonUtil.jsonToPojo(result, SwitchGetFromAgentRsp.class);
        if (null == switchRsp)
        {
            log.info("get switch is null, switchId:,{}", switchId);
            return;
        }
        for (SwitchGetFromAgentRsp.ipInfo ipInfo : switchRsp.getManageIps())
        {
            log.info("put switchMap, manageIp:{}, switchId:{}",ipInfo.getIp(), switchId);
            switchMap.put(ipInfo.getIp(), switchId);
        }
    }
}
