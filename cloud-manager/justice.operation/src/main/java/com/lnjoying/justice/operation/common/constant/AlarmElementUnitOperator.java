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

package com.lnjoying.justice.operation.common.constant;

/**
 * @description: TODO   监控类型及单位枚举类
 * @author: LiSen
 */

public enum AlarmElementUnitOperator {

    CPU_USAGE(0, "%", "CPU使用率过高"),
    MEM_USAGE(1, "%", "内存使用率过高"),
    FILESYSTEM_USAGE(3, "%", "文件系统使用率过高"),
    NETWORK_THROUGHPUT(4, "MB/s", "网卡吞吐量超过阈值"),
    DISK_IOPS(5, "req/s", "磁盘IOPS超出阈值"),
    DISK_THROUGHPUT(6, "MB/s", "磁盘吞吐率超出阈值"),
    INSTANCE_HEALTH_STATUS(7, "", "实例健康状态异常");

    private final int value;
    private final String unit;
    private final String subject;

    AlarmElementUnitOperator(int value, String unit, String subject) {
        this.value = value;
        this.unit = unit;
        this.subject = subject;
    }

    //根据value获取单位
    public static String getUnitByValue(int value) {
        for (AlarmElementUnitOperator operator : AlarmElementUnitOperator.values()) {
            if (operator.getValue() == value) {
                return operator.getUnit();
            }
        }
        return null;
    }

    //根据value获取告警标题
    public static String getSubjectByValue(int value) {
        for (AlarmElementUnitOperator operator : AlarmElementUnitOperator.values()) {
            if (operator.getValue() == value) {
                return operator.getSubject();
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getSubject() {
        return subject;
    }


}
