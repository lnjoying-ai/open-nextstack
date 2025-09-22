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

/**
 * 运维管理--报警(TblAlarmInfo)表实体类
 *
 * @author Lis
 * @since 2023-05-22 11:28:48
 */
@Data
public class AlarmInfoResp {
    private String infoId;
    //消息内容
    private String summeryInfo;
    //消息内容
    private String detailInfo;
    //触发行为
    private String triggerBehavior;

    //报警级别
    private Integer level;
    //资源类型
    private Integer resourceType;
    //报警次数
    private Integer alarmCount;

    //报警间隔
    private Integer interval;

    //报警状态（0，未报警 1，已报警）
    private Integer phaseStatus;
    //确认者
    private String confirmName;
    //确认时间
    private String confirmTime;
    //报警时间
    private String callpoliceTime;

    //创建时间
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd HH:mm:ss",timezone ="GMT+8")
    private Date createTime;

    //监控类型如CPU、内存、磁盘
    @JsonIgnore
    private Integer alarmElement;

}

