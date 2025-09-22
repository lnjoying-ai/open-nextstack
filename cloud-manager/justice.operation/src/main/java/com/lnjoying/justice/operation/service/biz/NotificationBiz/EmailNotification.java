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

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.lnjoying.justice.operation.service.MailSender;
import com.lnjoying.justice.operation.service.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class EmailNotification implements Notification {

    @Autowired
    private MailSender mailSender;

    @Override
    public void send(String[] address, String detailInfo, String subject) {

        log.info("Email send to " + new Gson().toJson(address) + " with content: " + detailInfo);

        //简易内容格式
        String template = StrUtil.format("详细资料：@{}", detailInfo).replace("@", "\n");
        Integer code = mailSender.sendBatchEmail(address, template, subject);

        if (code == 0) {
            log.info("Email send to " + Arrays.asList(address) + " Success！");
        } else {
            log.info("Email send to " + Arrays.asList(address) + " Fail！");
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}

