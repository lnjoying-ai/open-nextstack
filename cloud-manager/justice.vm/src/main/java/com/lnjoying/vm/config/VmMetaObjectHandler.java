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

package com.lnjoying.vm.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static com.lnjoying.justice.schema.common.ErrorCode.USER_ID_NOT_FOUND;
import static com.lnjoying.justice.schema.common.ErrorLevel.ERROR;
import static com.lnjoying.justice.schema.constant.UserHeadInfo.USERID;


@Component
@Slf4j
public class VmMetaObjectHandler implements MetaObjectHandler
{
    @Autowired
    HttpServletRequest httpServletRequest;

    @Override
    public void insertFill(MetaObject metaObject)
    {
        log.info("start insert fill ...");
        this.setFieldValByName("version", 1, metaObject);
//        String userId = httpServletRequest.getHeader(USERID);
//        String userId = getUserId();
//        Object userId = getFieldValByName("userId",metaObject);
//        SecurityContextHolder.getContext().getAuthentication().getName();
//        strictInsertFill(metaObject,"userId", ()->userId, String.class);
//        strictInsertFill(metaObject, "createTime", ()->new Date(System.currentTimeMillis()), Date.class);
//        strictInsertFill(metaObject, "updateTime", ()->new Date(System.currentTimeMillis()), Date.class);
//        strictInsertFill(metaObject, "status", ()->0, Integer.class);
    }

    @Override
    public void updateFill(MetaObject metaObject)
    {
//        log.info("start update fill ...");
//        String userId = getUserId();
//        strictUpdateFill(metaObject,"updateTime", ()->new Date(System.currentTimeMillis()), Date.class);
//        strictInsertFill(metaObject,"updateBy",()->userId, String.class);
    }

    private String getUserId()
    {
        String userId = httpServletRequest.getHeader(USERID);
        if (StringUtils.isBlank(userId))
        {
            throw new WebSystemException(USER_ID_NOT_FOUND, ERROR);
        }
        return userId;
    }
}

