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

package com.lnjoying.justice.operation.controller;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.domain.dto.request.WebhookInfoReq;
import com.lnjoying.justice.operation.service.biz.WebHookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RestSchema(schemaId = "webhook")
@Api(value = "WebHook Controller", tags = {"WebHook Controller"})
@RequestMapping("/operation/v1")
@Slf4j
public class WebHookController
{

    @Autowired
    private WebHookService webHookService;


    @PostMapping(value = "/instance-alarm", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "instance webhook", response = Object.class)
    public void instanceAlter(
            @ApiParam(value = "WebhookInfoReq", required = true, name = "WebhookInfoReq") @RequestBody WebhookInfoReq request
    ) throws WebSystemException
    {
        try
        {
            log.info("post, request:{}", request);
            webHookService.handleInstanceAlarm(request);
        }
        catch (Exception e)
        {
            log.error("instance webhook error: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @PostMapping(value = "/group-alarm", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "instance group webhook", response = Object.class)
    public void groupAlter(
//            @ApiParam(value = "CreateUpdateSgRulesReq", required = true, name = "CreateUpdateSgRulesReq") @RequestBody SgRulesCreateUpdateReqVo request

            @ApiParam(value = "WebhookInfoReq", required = true, name = "WebhookInfoReq") @RequestBody WebhookInfoReq request
    ) throws WebSystemException
    {
        try
        {
            log.info("post, request:{}", request);
            webHookService.handleInstanceGroupAlarm(request);
        }
        catch (Exception e)
        {
            log.error("instance group webhook error: {}", e.getMessage());

            throw throwWebException(e);
        }
    }


    public WebSystemException throwWebException(Exception e)
    {
        if (e instanceof WebSystemException)
        {
            return  (WebSystemException)e;
        }
        else
        {
            return new WebSystemException(ErrorCode.SystemError , ErrorLevel.CRITICAL);
        }
    }
}
