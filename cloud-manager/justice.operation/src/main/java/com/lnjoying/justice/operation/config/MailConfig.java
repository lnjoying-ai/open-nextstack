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

package com.lnjoying.justice.operation.config;

import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Data
@Configuration
@PropertySource(value = "classpath:application.properties", encoding="utf-8")
public class MailConfig
{
    private static Logger LOGGER = LogManager.getLogger();
    public MailConfig()
    {
        LOGGER.info("config email");
    }
    @Value("${spring.mail.sendfrom}")
    private String sendFrom;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String endableStarttls;
    @Value("${spring.mail.properties.mail.smtp.starttls.required}")
    private String requiredStarttls;

//    @Value("${spring.mail.template.subject}")
//    private String templateSubject;

    @Bean(name = "createJavaMailSender")
    JavaMailSender createJavaMailSender()
    {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(this.getHost());
        mailSender.setUsername(this.getUsername());
        mailSender.setPassword(this.getPassword());
        Properties props = System.getProperties();
        props.setProperty("mail.smtp.auth", this.getAuth());
        props.setProperty("smtp.starttls.enable", this.getEndableStarttls());
        props.setProperty("smtp.starttls.required", this.getRequiredStarttls());
        mailSender.setJavaMailProperties(props);
        return mailSender;
    }
}
