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

package com.lnjoying.justice.operation.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Date;

/**
 * (TblReceiver)表实体类
 *
 * @author Lis
 * @since 2023-05-22 10:57:18
 */
@Data
public class ReceiverReq {
    //主键
    private String receiverId;
    //报警表关联id
    private String ruleReceiverId;
    //类型（0，邮箱 1，手机号）
    @Min(value = 0)
    @Max(value = 1)
    private Integer trType;
    //邮箱地址或手机号--多个
    private String contactInfo;
    //邮箱地址或手机号--数量
    private Integer contactCount;
    //名称
    private String trName;
    //描述
    private String description;
    //报警状态（0，未报警 1，已报警）
    private Integer phaseStatus;
    //创建时间
    private Date createTime;
    //修改时间
    private Date updateTime;

    }

