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

package com.lnjoying.justice.operation.service.biz.NotificationBiz;

import com.google.gson.Gson;
import com.lnjoying.justice.operation.entity.sender.BatchSmsRsp;
import com.lnjoying.justice.operation.service.Notification;
import com.lnjoying.justice.operation.service.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsNotification implements Notification {

    @Autowired
    private SmsSender smsSender;

    @Override
    public void send(String[] address, String detailInfo, String subject) {

        //多个手机号，分割
        String joined = String.join(",", address);

        //组装多个短信中的参数及格式
        detailInfo = subject + ";" + detailInfo.replaceAll("@", ",");

        //指定模版及地址
        String template = "operation_code", url = "/templatesms";

        //发送短信
        BatchSmsRsp batchSmsRsp = smsSender.sendBatchSms(detailInfo, joined, template, url);

        //判断成功与否
        log.info("Sms send to " + new Gson().toJson(address) + " with content: " + detailInfo);
        if (null != batchSmsRsp && batchSmsRsp.getCode().equals("0")) {
            log.info("Sms send to " + joined + " Success！");
        } else {
            log.info("Sms send to " + joined + " Fail！异常信息：" + batchSmsRsp.getMsg());
        }

    }


    @Override
    public void afterPropertiesSet() throws Exception {

    }
}

