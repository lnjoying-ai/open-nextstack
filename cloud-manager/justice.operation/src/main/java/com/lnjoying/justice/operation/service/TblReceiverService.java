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

package com.lnjoying.justice.operation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lnjoying.justice.operation.domain.dto.response.ReceiverBaseResp;
import com.lnjoying.justice.operation.domain.dto.response.ReceiversResp;
import com.lnjoying.justice.operation.entity.TblReceiver;

/**
 * (TblReceiver)表服务接口
 *
 * @author Lis
 * @since 2023-05-19 17:48:36
 */
public interface TblReceiverService extends IService<TblReceiver> {

    //运维管理--报警设置--通知对象--新增数据
    ReceiverBaseResp addRec(TblReceiver tblReceiver, String userId);

    //运维管理--报警设置--通知对象--分页查询
    ReceiversResp selectRecPage(String name, String description, Integer pageSize, Integer pageNum, String userId);

    //运维管理--报警设置--通知对象--编辑数据
    ReceiverBaseResp updateReceiver(String receiverId, TblReceiver tblReceiver, String userId);

    //运维管理--报警设置--通知对象--删除数据
    ReceiverBaseResp removeReceiver(String receiverId);

    //通过主键查询单条数据
    TblReceiver getSelectOneById(String id, String userId);
}

