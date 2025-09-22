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

package com.lnjoying.justice.operation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 运维管理--报警设置--报警器(TblAlarmRule)表实体类
 *
 * @author Lis
 * @since 2023-05-22 10:57:02
 */
@Data
public class TblAlarmRule extends Model<TblAlarmRule> {
    //主键
    @TableId("rule_id")
    private String ruleId;
    //名称
    @NotNull(message = "Name cannot be empty！")
    private String name;
    //触发规则cpu > 80%; mem>70%
    @NotNull(message = "Trigger rule cannot be empty！")
    private String expr;
    //持续时间
    private Integer durationTime;
    //是否通知(0,不通知 1，选择已有通知 2，新建通知)
    private Boolean notice;
    //报警间隔
    @Min(value = 1)
    @Max(value = 1440)
    private Integer interval;
    //报警级别
    @Min(value = 0)
    @Max(value = 2)
    private Integer level;
    //用户id 
    private String userId;
    //报警状态（0，未报警 1，已报警）
    private Integer phaseStatus;
    //创建时间
    private Date createTime;
    //修改时间
    private Date updateTime;
    //描述
    private String description;

    @TableField(exist = false)
    private String receiverList;

    // 比较符，如大于、小于、等于
    private Integer comparison;
    //报警值如 使用率90%（0.9）
    private Float alarmValue;
    }

