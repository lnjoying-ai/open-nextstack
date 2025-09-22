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

package com.lnjoying.justice.operation.rpcserviceimpl;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.schema.service.operation.LogService;
import com.lnjoying.justice.schema.service.ums.UmsService;
import com.lnjoying.justice.operation.common.constant.PhaseStatus;
import com.lnjoying.justice.operation.entity.TblEvent;
import com.lnjoying.justice.operation.entity.TblLog;
import com.lnjoying.justice.operation.mapper.TblEventDao;
import com.lnjoying.justice.operation.mapper.TblLogDao;
import com.lnjoying.justice.operation.service.biz.CombRpcSerice;
import com.micro.core.common.Utils;
import io.swagger.annotations.ApiParam;
import org.apache.servicecomb.provider.pojo.RpcSchema;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

/**
 * Rpc implementation class
 *
 * @author merak
 **/
@RpcSchema(schemaId = "logService")
public class LogServiceImpl implements LogService {

    @Resource
    private TblLogDao tblLogDao;

    @Resource
    private TblEventDao tblEventDao;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Override
    public void addLog(@ApiParam(name = "userId") String userId, @ApiParam(name = "userName") String userName, @ApiParam(name = "resource") String resource, @ApiParam(name = "description") String description) {
        // 记录操作日志
        TblLog tblLog = new TblLog();
        tblLog.setUserId(userId);
        tblLog.setLogId(Utils.assignUUId());
        tblLog.setDescription(description);
        tblLog.setResource(resource);
        tblLog.setOperator(userName);
        tblLog.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblLogDao.insert(tblLog);
        Console.log("操作资源:" + resource + ",记录成功！");
    }

    @Override
    public void addEvent(@ApiParam(name = "userId") String userId,@ApiParam(name = "content") String content,
                         @ApiParam(name = "detailInfo") String detailInfo, @ApiParam(name = "result") String result)
    {
        //记录事件日志
        TblEvent tblEvent = new TblEvent();

        if (!StrUtil.isBlank(userId))
        {
            UmsService.User user = combRpcSerice.getUmsService().getUser(userId);
            if (null != user)
            {
                tblEvent.setUsername(user.getUserName());
            }
        }
        else
        {
            tblEvent.setUsername("admin");
        }
        tblEvent.setEventId(Utils.assignUUId());
        tblEvent.setUserId(userId);
        tblEvent.setContent(content);
        tblEvent.setDetailInfo(detailInfo);
        tblEvent.setResult(result);
        tblEvent.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblEvent.setUpdateTime(tblEvent.getCreateTime());
        tblEvent.setPhaseStatus(PhaseStatus.CREATED);
        tblEventDao.insert(tblEvent);
    }
}
