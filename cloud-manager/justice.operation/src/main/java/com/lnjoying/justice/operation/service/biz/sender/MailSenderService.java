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

package com.lnjoying.justice.operation.service.biz.sender;

import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.operation.config.ApiConfig;
import com.lnjoying.justice.operation.config.MailConfig;
import com.lnjoying.justice.operation.service.MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service("mailSender")
@Slf4j
public class MailSenderService implements MailSender {
    @Autowired
    ApiConfig apiConfig;
    @Autowired
    private MailConfig mailConfig;
    @Autowired
    private JavaMailSender createJavaMailSender;

    public MailSenderService() {
        log.info("start mail service");
    }


    public Integer sendBatchEmail(String[] address, String template, String subject) {
        if (address == null) {
            return 0;
        }
        log.info("send email. to: {}", (Object) address);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailConfig.getSendFrom());
        message.setTo(address);
        message.setSubject(subject);
        message.setText(template);
        try {
            createJavaMailSender.send(message);
            return ErrorCode.SUCCESS.getCode();
        } catch (Exception e) {
            log.info("send error." + e);
            return ErrorCode.SystemError.getCode();
        }
    }

}
