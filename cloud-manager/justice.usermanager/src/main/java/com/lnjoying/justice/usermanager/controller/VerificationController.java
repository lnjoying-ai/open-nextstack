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

package com.lnjoying.justice.usermanager.controller;

import com.lnjoying.justice.commonweb.controller.RestWebController;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.common.RedisCacheField;
import com.lnjoying.justice.schema.common.TemplateID;
import com.lnjoying.justice.usermanager.config.DescriptionConfig;
import com.lnjoying.justice.usermanager.config.ServiceConfig;
import com.lnjoying.justice.usermanager.service.CombRpcSerice;
import com.lnjoying.justice.usermanager.service.IdentityService;
import com.lnjoying.justice.usermanager.utils.ValidatorUtil;
import com.micro.core.common.Utils;
import com.micro.core.persistence.redis.RedisUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestSchema(schemaId = "verification")
@RequestMapping("/api/ums/v1/verification")
@Controller
@Slf4j
public class VerificationController extends RestWebController
{

    @Autowired
    private IdentityService identityService;

    int VER_CODE_LEN = 4;

    @Value("${spring.duration.validateCode}")
    Integer duration;

    @Autowired
    CombRpcSerice combRpcSerice;

    @GetMapping(value = "/auth/sms/{phone}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "verification", response = Object.class, notes =
            DescriptionConfig.VERIFICATION_SMS_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void authVerificationSms(
            @ApiParam(value = "phone", required = true, name = "phone") @PathVariable String phone)  throws IOException
    {
        verificationSms(phone, TemplateID.AUTH_VER_CODE, RedisCacheField.AUTH_VER_CODE);
    }



    @GetMapping(value = "/auth/email/{emailAddr}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "verification", response = Object.class, notes =
            DescriptionConfig.VERIFICATION_EMAIL_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void authVerificationEmail(
            @ApiParam(value = "emailAddr", required = true, name = "emailAddr") @PathVariable String emailAddr)  throws IOException
    {
        verificationEmail(emailAddr, TemplateID.AUTH_VER_CODE, RedisCacheField.AUTH_VER_CODE);
    }

    @GetMapping(value = "/registration/sms/{phone}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "registration", response = Object.class, notes =
            DescriptionConfig.SMS_REGISTRATION_MSG)
    public void regVerificationSms(
            @ApiParam(value = "phone", required = true, name = "phone") @PathVariable String phone)  throws IOException
    {
        verificationSms(phone, TemplateID.REG_VER_CODE, RedisCacheField.REG_VER_CODE);
    }

    @GetMapping(value = "/registration/email/{emailAddr}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "registration", response = Object.class, notes =
            DescriptionConfig.EMAIL_REGISTRATION_MSG)
    @ResponseBody    @ResponseStatus(HttpStatus.OK)
    public void regVerificationEmail(
            @ApiParam(value = "emailAddr", required = true, name = "emailAddr") @PathVariable String emailAddr)  throws IOException
    {
        verificationEmail(emailAddr, TemplateID.REG_VER_CODE, RedisCacheField.REG_VER_CODE);
    }

    void verificationSms(String phone, String template, String redisKey) throws WebSystemException
    {
        if (null == phone || phone.isEmpty())
        {
            throw new WebSystemException(ErrorCode.VER_Params_Error, ErrorLevel.ERROR);
        }

        if (! ValidatorUtil.validateStr(ServiceConfig.PATTERN_TELEPHONE, phone))
        {
            throw new WebSystemException(ErrorCode.VER_Params_Error, ErrorLevel.ERROR);
        }

        String verCode = Utils.getRandomStr(VER_CODE_LEN);
        if (null == verCode || verCode.isEmpty())
        {
            log.error("validate code is empty.");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
        try
        {
            String content = verCode+","+duration.toString();
            combRpcSerice.getTipMessageService().sendSingleSms(content, phone, template);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new WebSystemException(ErrorCode.SystemError ,ErrorLevel.CRITICAL);
        }

        RedisUtil.set(redisKey, phone, verCode, duration);
        log.info("log validate code {} to {}", verCode, phone);
    }

    void verificationEmail(String emailAddr, String template, String redisKey) throws WebSystemException
    {
        if (null == emailAddr || emailAddr.isEmpty()) {
            throw new WebSystemException(ErrorCode.VER_Params_Error, ErrorLevel.ERROR);
        }

        if (!ValidatorUtil.validateStr(ServiceConfig.PATTERN_MAILADDRESS, emailAddr)) {
            throw new WebSystemException(ErrorCode.VER_Params_Error, ErrorLevel.ERROR);
        }

        String verCode = Utils.getRandomStr(VER_CODE_LEN);
        if (null == verCode || verCode.isEmpty())
        {
            log.error("validate code is empty.");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.ERROR);
        }
        String content = verCode + "," + duration.toString();
        try
        {
            combRpcSerice.getTipMessageService().sendEmail(emailAddr, content, template);
            RedisUtil.set(redisKey + emailAddr, verCode, duration);
            log.info("log validate code {} to {}", verCode, emailAddr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.CRITICAL);
        }
    }
}
