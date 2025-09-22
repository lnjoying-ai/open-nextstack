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

package com.micro.core.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class JdbcConfig
{
    /**
     * 数据库用户名
     */
    @Value("${spring.datasource.username}")
    private String userName;
    /**
     * 驱动名称
     */
    @Value("${spring.datasource.driver-class-name}")
    private String driverClass;
    /**
     * 数据库连接url
     */
    @Value("${spring.datasource.url}")
    private String url;
    /**
     * 数据库密码
     */
    @Value("${spring.datasource.password}")
    private String password;

    /**
     * 数据库连接池初始化大小
     */
    @Value("${spring.datasource.initialSize}")
    private int initialSize;

    /**
     * 数据库连接池最小最小连接数
     */
    @Value("${spring.datasource.minIdle}")
    private int minIdle;

    /**
     * 数据库连接池最大连接数
     */
    @Value("${spring.datasource.maxActive}")
    private int maxActive;

    /**
     * 获取连接等待超时的时间
     */
    @Value("${spring.datasource.maxWait}")
    private long maxWait;

    /**
     * 多久检测一次
     */
    @Value("${spring.datasource.timeBetweenEvictionRunsMillis}")
    private long timeBetweenEvictionRunsMillis;

    /**
     * 连接在池中最小生存的时间
     */
    @Value("${spring.datasource.minEvictableIdleTimeMillis}")
    private long minEvictableIdleTimeMillis;

    /**
     * 测试连接是否有效的sql
     */
    @Value("${spring.datasource.validationQuery}")
    private String validationQuery;

    /**
     * 申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，检测连接是否有效
     */
    @Value("${spring.datasource.testWhileIdle}")
    private boolean testWhileIdle;

    /**
     * 申请连接时,检测连接是否有效
     */
    @Value("${spring.datasource.testOnBorrow}")
    private boolean testOnBorrow;

    /**
     * 归还连接时,检测连接是否有效
     */
    @Value("${spring.datasource.testOnReturn}")
    private boolean testOnReturn;

    /**
     * PSCache大小
     */
    @Value("${spring.datasource.maxPoolPreparedStatementPerConnectionSize}")
    private int maxPoolPreparedStatementPerConnectionSize;

    /**
     * 通过别名的方式配置扩展插件
     */
    @Value("${spring.datasource.filters}")
    private String filters;
}
