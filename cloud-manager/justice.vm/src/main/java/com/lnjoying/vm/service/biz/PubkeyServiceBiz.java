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

package com.lnjoying.vm.service.biz;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.vm.domain.dto.request.CommonReq;
import com.lnjoying.vm.domain.dto.request.UploadPubkeyReq;
import com.lnjoying.vm.domain.dto.response.KeyPairInfo;
import com.lnjoying.vm.domain.dto.response.PubkeyBaseRsp;
import com.lnjoying.vm.domain.dto.response.PubkeyDetailInfo;
import com.lnjoying.vm.domain.dto.response.PubkeysRsp;
import com.lnjoying.vm.entity.Pubkey;
import com.lnjoying.vm.entity.search.PubkeySearchCritical;
import com.lnjoying.vm.service.PubkeyService;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
public class PubkeyServiceBiz
{

    @Autowired
    private PubkeyService pubkeyService;

    public KeyPairInfo createKeyPair(CommonReq req, String userId) throws WebSystemException
    {
        KeyPairGenerator keyPairGenerator;
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;
        String publicKeyString;
        String privateKeyString;
        try
        {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            publicKey = (RSAPublicKey) keyPair.getPublic();
            privateKey = (RSAPrivateKey) keyPair.getPrivate();

            byte[] privateBytes = privateKey.getEncoded();
            String privateBase64 = Base64.encode(privateBytes);
            privateKeyString = "-----BEGIN RSA PRIVATE KEY-----\n"
                    + privateBase64 + "\n"
                    + "-----END RSA PRIVATE KEY-----";

            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteOs);
            dos.writeInt("ssh-rsa".getBytes().length);
            dos.write("ssh-rsa".getBytes());
            dos.writeInt(publicKey.getPublicExponent().toByteArray().length);
            dos.write(publicKey.getPublicExponent().toByteArray());
            dos.writeInt(publicKey.getModulus().toByteArray().length);
            dos.write(publicKey.getModulus().toByteArray());
            String enc = Base64.encode(byteOs.toByteArray());
            publicKeyString = "ssh-rsa " + enc + " ";
        }
        catch (IOException | NoSuchAlgorithmException exception)
        {
            exception.printStackTrace();
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        Pubkey tblPubkey = new Pubkey();
        String pubkeyId = Utils.assignUUId();
        tblPubkey.setPubkeyId(pubkeyId);
        tblPubkey.setPubkey(publicKeyString);
        tblPubkey.setName(req.getName());
        tblPubkey.setDescription(req.getDescription());
        tblPubkey.setUserId(userId);
        tblPubkey.setCreateTime(new Date(System.currentTimeMillis()));
        tblPubkey.setUpdateTime(new Date(System.currentTimeMillis()));

        boolean ok = pubkeyService.save(tblPubkey);
        if (!ok)
        {
            log.error("add pubkey failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        KeyPairInfo keyPairInfo = new KeyPairInfo();
        keyPairInfo.setPrivateKey(privateKeyString);
        keyPairInfo.setPubkeyId(pubkeyId);
        return keyPairInfo;
    }

    public PubkeyBaseRsp uploadPubkey(UploadPubkeyReq req, String userId) throws WebSystemException
    {
        // Validate pubkey format first
        String pubKey = req.getPubKey();
        if (pubKey == null || pubKey.isEmpty()) {
            log.error("Public key is empty or null");
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }

        // Validate public key format
        if (!isValidPublicKey(pubKey)) {
            log.error("Invalid public key format: {}", pubKey);
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }


        Pubkey tblPubkey = new Pubkey();
        String pubkeyId = Utils.assignUUId();
        tblPubkey.setPubkeyId(pubkeyId);
        tblPubkey.setPubkey(req.getPubKey());
        tblPubkey.setName(req.getName());
        tblPubkey.setDescription(req.getDescription());
        tblPubkey.setUserId(userId);
        tblPubkey.setCreateTime(new Date(System.currentTimeMillis()));
        tblPubkey.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = pubkeyService.save(tblPubkey);

        if (!ok)
        {
            log.error("add pubkey failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        PubkeyBaseRsp pubkeyBaseRsp = new PubkeyBaseRsp();
        pubkeyBaseRsp.setPubkeyId(pubkeyId);
        return pubkeyBaseRsp;
    }

    private boolean isValidPublicKey(String pubKey) {
        try {
            // Check for SSH format (starts with ssh-rsa, ssh-ed25519, etc.)
            if (pubKey.startsWith("ssh-rsa ") || pubKey.startsWith("ssh-ed25519 ") ||
                    pubKey.startsWith("ecdsa-sha2-") || pubKey.startsWith("ssh-dss ")) {
                // Validate SSH public key format
                String[] parts = pubKey.split(" ");
                return parts.length >= 2 && !parts[1].isEmpty();
            }

            // Check for PEM format (Base64 encoded between headers)
            if (pubKey.contains("-----BEGIN PUBLIC KEY-----") &&
                    pubKey.contains("-----END PUBLIC KEY-----")) {
                // Basic PEM format validation
                String keyContent = pubKey.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
                return !keyContent.isEmpty() && isBase64(keyContent);
            }

            // Check for PGP format
            if (pubKey.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----") &&
                    pubKey.contains("-----END PGP PUBLIC KEY BLOCK-----")) {
                return true;
            }

            // Check if it's a raw Base64 encoded key (common for REST APIs)
            if (isBase64(pubKey)) {
                return pubKey.length() >= 32; // Minimum sensible key length
            }

            // Add more format validations as needed

            return false;
        } catch (Exception e) {
            log.error("Error validating public key", e);
            return false;
        }
    }

    /**
     * Simple validation if a string is Base64 encoded
     *
     * @param str The string to check
     * @return true if string appears to be Base64 encoded
     */
    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            // Try to decode and see if it succeeds
            java.util.Base64.getDecoder().decode(str);
            // Check if the string matches the Base64 pattern
            return str.matches("^[A-Za-z0-9+/]*={0,2}$");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }



    public PubkeyBaseRsp removePubkey(String pubkeyId) throws WebSystemException
    {
        boolean ok = pubkeyService.removeById(pubkeyId);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        PubkeyBaseRsp pubkeyBaseRsp = new PubkeyBaseRsp();
        pubkeyBaseRsp.setPubkeyId(pubkeyId);
        return pubkeyBaseRsp;
    }

    public PubkeyBaseRsp updatePubkey(CommonReq req, String pubkeyId) throws WebSystemException
    {
        Pubkey tblPubkey = pubkeyService.getById(pubkeyId);
        if (null == tblPubkey)
        {
            throw new WebSystemException(ErrorCode.PUBKEY_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!StrUtil.isBlank(req.getName()))
        {
            tblPubkey.setName(req.getName());
        }
        if (!StrUtil.isBlank(req.getDescription()))
        {
            tblPubkey.setDescription(req.getDescription());
        }
        tblPubkey.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = pubkeyService.updateById(tblPubkey);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }

        PubkeyBaseRsp pubkeyBaseRsp = new PubkeyBaseRsp();
        pubkeyBaseRsp.setPubkeyId(pubkeyId);
        return pubkeyBaseRsp;
    }

    public PubkeyDetailInfo getPubkey(String pubkeyId) throws WebSystemException
    {
        Pubkey tblPubkey = pubkeyService.getById(pubkeyId);
        if (null == tblPubkey)
        {
            throw new WebSystemException(ErrorCode.PUBKEY_NOT_EXIST, ErrorLevel.INFO);
        }
        PubkeyDetailInfo pubkeyDetailInfo = new PubkeyDetailInfo();
        pubkeyDetailInfo.setPubkeyDetailInfo(tblPubkey);
        return pubkeyDetailInfo;
    }

    public PubkeysRsp getPubkeys(PubkeySearchCritical critical, String userId) throws WebSystemException
    {

        //set criteria with name
        LambdaQueryWrapper<Pubkey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Pubkey::getUserId, userId);
        if (null != critical.getName())
        {
            queryWrapper.like(Pubkey::getName, critical.getName());
        }

        PubkeysRsp getPubkeysRsp = new PubkeysRsp();

        //get total number with example condition
        long totalNum = pubkeyService.count(queryWrapper);
        getPubkeysRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getPubkeysRsp;
        }
        //query with page number and page size
//        int begin = ((critical.getPageNum() - 1) * critical.getPageSize());
        queryWrapper.orderByDesc(Pubkey::getCreateTime);
        Page<Pubkey> page = new Page<>(critical.getPageNum(), critical.getPageSize());
        Page<Pubkey> pubkeyPage = pubkeyService.page(page, queryWrapper);
        List<Pubkey> tblPubkeyList = pubkeyPage.getRecords();
        if (null == tblPubkeyList)
        {
            return getPubkeysRsp;
        }

        List<PubkeyDetailInfo> pubkeyInfoList = tblPubkeyList.stream().map((tblPubkey) ->
        {
            PubkeyDetailInfo pubkeyInfo = new PubkeyDetailInfo();
            pubkeyInfo.setPubkeyDetailInfo(tblPubkey);
            return pubkeyInfo;
        }).collect(Collectors.toList());

        //set response
        getPubkeysRsp.setPubkeys(pubkeyInfoList);

        return getPubkeysRsp;

    }
}
