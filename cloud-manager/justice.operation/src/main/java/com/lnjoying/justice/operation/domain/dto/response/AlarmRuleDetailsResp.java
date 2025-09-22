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

package com.lnjoying.justice.operation.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 运维管理--报警设置--报警器--单条详情
 *
 * @author Lis
 */
@Data
public class AlarmRuleDetailsResp
{

    private String ruleId;
    //名称
    private String name;
    //触发规则
    private String expr;
    //报警级别
    private Integer level;

    //资源类型
    private Integer resourceType;
    //监控对象id
    @JsonIgnore
    private String resourceId;
    //监控对象list
    private List<ResourceDetailsResp> resourceDetailsResps;

    //报警间隔
    private Integer interval;

    //通知对象id
    @JsonIgnore
    private String receiverId;
    //通知对象List
    private List<ReceiverDetailsResp> receiverDetailsResps;

    //创建时间
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
    private Date createTime;

    //修改时间
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
    private Date updateTime;

    //监控类型如CPU、内存、磁盘
    private Integer alarmElement;

    //是否通知(0,不通知 1，选择已有通知 2，新建通知)
    private Boolean notice;

    //描述
    private String description;
    // 比较符，如大于、小于、等于
    private Integer comparison;
    //报警值如 使用率90%（0.9）
    private Float alarmValue;
    //持续时间，单位分钟
    private Integer durationTime;

    }

