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

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
 * 运维管理--报警(TblAlarmInfo)表实体类
 *
 * @author Lis
 * @since 2023-05-22 11:28:48
 */
@Data
public class TblAlarmInfo extends Model<TblAlarmInfo> {

    //主键
    @TableId("info_id")
    private String infoId;
    //报警器表--主键
    private String ruleId;
    //报警次数
    private Integer alarmCount;

    private String summeryInfo;
    //消息内容
    private String detailInfo;
    //用户id
    private String userId;
    //报警状态（0，未报警 1，已报警,2,）
    private Integer phaseStatus;
    //创建时间
    private Date createTime;
    //修改时间
    private Date updateTime;
    //触发行为
    private String triggerBehavior;
    //确定时间
    private Date confirmTime;
    //报警间隔
    private Integer interval;
    //报警级别
    private Integer level;
    //资源类型
    private Integer resourceType;
    //监控类型如CPU、内存、磁盘
    private Integer alarmElement;


}

