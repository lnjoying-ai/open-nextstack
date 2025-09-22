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

package com.lnjoying.justice.commonweb.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description: TODO   记录平台操作日志
 * @author: LiSen
 * @date: 2023/5/24
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAnnotation {

    //操作资源
    String resource() default "";

    //操作描述--》如需记录详细描述，可通过模板方式写入！--》请于下方参数对应
    // 例如：description = "新增报警器【名称：{}，邮件：{}】"
    String description() default "";

    //填写需要获取的入参，多个请根据逗号分割--》请于上方模版对应
    // 例如：obtainParameter = "name,contactInfo"
    String obtainParameter() default "";


}
