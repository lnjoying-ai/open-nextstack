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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * (TblReceiver)表实体类
 *
 * @author Lis
 * @since 2023-05-22 10:57:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TblReceiver extends Model<TblReceiver> {
    //主键
    @TableId("receiver_id")
    private String receiverId;
    //报警表关联id
//    private String ruleReceiverId;
    //类型（0，邮箱 1，短信 2，电话）
    private Integer type;
    //邮箱地址或手机号--多个
    private String contactInfo;
    //邮箱地址或手机号--数量
    private Integer contactCount;
    //名称
    @NotNull(message = "Name cannot be empty！")
    private String name;
    //描述
    private String description;
    //用户id
    private String userId;
    //报警状态（0，未报警 1，已报警）
    @JsonIgnore
    private Integer phaseStatus;
    //创建时间
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createTime;
    //修改时间
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date updateTime;

    @TableField(exist = false)
    private List<@NotBlank String> contactInfos;

    }

