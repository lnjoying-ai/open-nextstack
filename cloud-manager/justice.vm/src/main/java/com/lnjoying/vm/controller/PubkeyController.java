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

package com.lnjoying.vm.controller;

import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.UploadPubkeyReq;
import com.lnjoying.vm.domain.dto.response.PubkeyBaseRsp;
import com.lnjoying.vm.domain.dto.response.PubkeyDetailInfo;
import com.lnjoying.vm.domain.dto.response.PubkeysRsp;
import com.lnjoying.vm.domain.dto.response.KeyPairInfo;
import com.lnjoying.vm.entity.search.PubkeySearchCritical;
import com.lnjoying.vm.service.biz.PubkeyServiceBiz;
import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestSchema(schemaId = "pubkey")
@RequestMapping("/vm/v1")
@Api(value = "Pubkey Controller",tags = {"Pubkey Controller"})
@Slf4j
public class PubkeyController
{
    @Autowired
    private PubkeyServiceBiz pubkeyServiceBiz;

    @LogAnnotation(resource = "公钥", description = "创建公钥【名称：{}，描述：{}】", obtainParameter = "name,description")
    @PostMapping(value = "/pubkeys", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create key pair", response = Object.class)
    public ResponseEntity<KeyPairInfo> createKeyPair(
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody CommonReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    )  throws WebSystemException
    {
        try
        {
            log.info("create keyPair, request:{}, userId:{}",request,userId);
            KeyPairInfo keyPairInfo = pubkeyServiceBiz.createKeyPair(request, userId);
            return ResponseEntity.ok(keyPairInfo);
        }
        catch (Exception e)
        {
            log.error("create keyPair error: {}", e.getMessage());

            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "公钥", description = "上传公钥【公钥：{}】", obtainParameter = "pubKey")
    @PostMapping(value = "/pubkeys/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "upload pubkey", response = Object.class)
    public ResponseEntity<PubkeyBaseRsp> uploadPubkey(
            @ApiParam(value = "UploadPubkeyReq", required = true, name = "UploadPubkeyReq") @RequestBody @Valid UploadPubkeyReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("upload pubkey, request:{}, userId:{}",request, userId);
            PubkeyBaseRsp pubkeyBaseRsp = pubkeyServiceBiz.uploadPubkey(request, userId);
            return ResponseEntity.ok(pubkeyBaseRsp);
        }
        catch (Exception e)
        {
            log.error("upload pubkey error: {}",e.getMessage());
            e.printStackTrace();
            throw  throwWebException(e);
        }
    }

    @GetMapping(value = "/pubkeys/{pubkeyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get pubkey detail info", response = Object.class)
    public ResponseEntity<PubkeyDetailInfo> getPubkey(
            @ApiParam(value = "pubkeyId", required = true, name = "pubkeyId") @PathVariable("pubkeyId") String pubkeyId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get pubkey, request:{}, userId:{}",pubkeyId, userId);
            PubkeyDetailInfo pubkeyDetailInfo = pubkeyServiceBiz.getPubkey(pubkeyId);
            return ResponseEntity.ok(pubkeyDetailInfo);
        }
        catch (Exception e)
        {
            log.error("get pubkey error: {}, pubkeyId: {}",e.getMessage(), pubkeyId);
            e.printStackTrace();
            throw  throwWebException(e);
        }
    }

    @GetMapping(value = "/pubkeys", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get pubkeys", response = Object.class)
    public ResponseEntity<PubkeysRsp> getPubkeys(
            @ApiParam(name = "name") @RequestParam(required = false) String name,
            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get pubkey list");
            PubkeySearchCritical pageSearchCritical = new PubkeySearchCritical();
            pageSearchCritical.setName(name);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);

            PubkeysRsp getPubkeysRsp = pubkeyServiceBiz.getPubkeys(pageSearchCritical, userId);
            return ResponseEntity.ok(getPubkeysRsp);
        }
        catch (Exception e)
        {
            log.error("get pubkeys error: {}",e.getMessage());
            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "公钥", description = "更新公钥【名称：{}，描述：{}，id：{}】", obtainParameter = "name,description,pubkeyId")
    @PutMapping(value = "/pubkeys/{pubkeyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update pubkey", response = Object.class)
    public ResponseEntity<PubkeyBaseRsp> updatePubkey(
            @ApiParam(value = "pubkeyId", required = true, name = "pubkeyId") @PathVariable("pubkeyId") String pubkeyId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody CommonReq request,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.debug("put pubkey: {}, userId:{}", request, userId);
            PubkeyBaseRsp pubkeyBaseRsp = pubkeyServiceBiz.updatePubkey(request, pubkeyId);
            return ResponseEntity.ok(pubkeyBaseRsp);
        }
        catch (Exception e)
        {
            log.error("update pubkey error: {}, pubkeyId: {}",e.getMessage(), pubkeyId);
            e.printStackTrace();
            throw  throwWebException(e);
        }
    }

    @LogAnnotation(resource = "公钥", description = "删除公钥【id：{}】", obtainParameter = "pubkeyId")
    @DeleteMapping(value = "/pubkeys/{pubkeyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete pubkey", response = Object.class)
    public ResponseEntity<PubkeyBaseRsp> removePubkey(
            @ApiParam(value = "pubkeyId", required = true, name = "pubkeyId") @PathVariable("pubkeyId") String pubkeyId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    )throws WebSystemException
    {
        try
        {
            log.debug("delete pubkeyId: {}, userId: {}", pubkeyId, userId);
            PubkeyBaseRsp pubkeyBaseRsp = pubkeyServiceBiz.removePubkey(pubkeyId);
            return ResponseEntity.ok(pubkeyBaseRsp);
        }
        catch (Exception e)
        {
            log.error("delete pubkey error: {}, pubkeyId: {}",e.getMessage(), pubkeyId);
            e.printStackTrace();
            throw  throwWebException(e);
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
