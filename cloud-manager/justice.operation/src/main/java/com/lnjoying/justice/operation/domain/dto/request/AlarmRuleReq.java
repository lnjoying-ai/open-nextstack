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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 运维管理--报警设置--报警器(TblAlarmRule)表实体类
 *
 * @author Lis
 * @since 2023-05-22 10:57:02
 */
@Data
public class AlarmRuleReq {
    //名称
    @NotBlank(message = "name cannot be empty")
    private String name;
    //描述
    private String description;
    //资源类型 如虚拟机、计算节点、主机组

    @Min(value = 0)
    @Max(value = 2)
    private Integer resourceType;
    //监控对象
    @NotEmpty(message = "resourceId cannot be empty！")
    private List<@NotBlank  String> resourceIds;

    //监控类型如CPU、内存、磁盘
    @Min(value = 0)
    @Max(value = 7)
    private Integer alarmElement;
    // 比较符，如大于、小于、等于

    @Min(value = 0)
    @Max(value = 4)
    private Integer comparison;

    //报警值如 使用率90%（0.9）
    //@Max(value = 100)
    private Float alarmValue;

    //持续时间，单位分钟
    @Min(value = 2)
    @Max(value = 60)
    private Integer durationTime;

    //报警间隔,单位分钟
    @Min(value = 10)
    @Max(value = 1440)
    private Integer interval;

    //报警级别warning、critical和emergency
    @Min(value = 0)
    @Max(value = 2)
    private Integer level;

    //是否通知(0,不通知 1，选择已有通知 2，新建通知)
//    @Min(value = 0)
//    @Max(value = 1)
    private Boolean notice;

    //通知对象
    private List<String> receiverList;

    }

