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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 资源表--》监控报警(TblAlarmRuleResource)表实体类
 *
 * @author Lis
 * @since 2023-05-22 11:44:27
 */
@Data
public class TblAlarmRuleResource extends Model<TblAlarmRuleResource> {

    //主键
    @TableId("alarm_rule_resource_id")
    private String alarmRuleResourceId;
    //资源类型
    @Min(value = -1)
    private Integer resourceType;
    //监控对象
    @NotNull(message = "The monitoring object cannot be empty！")
    private String resourceId;
    //报警状态（0，未报警 1，已报警）
    private Integer phaseStatus;
    //创建时间
    private Date createTime;
    //修改时间
    private Date updateTime;
    //报警器表--关联id 
    private String ruleId;
    //监控类型如CPU、内存、磁盘
    private Integer alarmElement;
    //监控类型对应-->单位
    private String unit;

}

