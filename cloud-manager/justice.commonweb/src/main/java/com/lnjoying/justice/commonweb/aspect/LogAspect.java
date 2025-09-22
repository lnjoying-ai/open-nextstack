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

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lnjoying.justice.commonweb.biz.LogRpcSerice;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.HttpContextUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @description: TODO   记录平台操作日志
 * @author: LiSen
 * @date: 2023/5/24
 */

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    @Autowired
    private LogRpcSerice logRpcSerice;

    @Pointcut("@annotation(com.lnjoying.justice.commonweb.aspect.LogAnnotation)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {

        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String userId = request.getHeader("X-UserId");
        String userName = request.getHeader("X-UserName");

        // 获取方法上的注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        LogAnnotation logAnnotation = method.getAnnotation(LogAnnotation.class);

        // 获取操作名称
        String obtainParameter = logAnnotation.obtainParameter();
        String description = logAnnotation.description();
        String resource = logAnnotation.resource();
        try {

            if (StrUtil.isNotBlank(obtainParameter)) {
                Object[] args = point.getArgs();
                //判断当前方法入参，是否为空
                if (args != null && args.length > 0) {
                    //解析方法参数
                    List<String> opList = new ArrayList<>(Arrays.asList(obtainParameter.split(",")));
                    //当前方法转为JSON
                    JsonMapper jsonMapper = new JsonMapper();
                    String json = jsonMapper.writeValueAsString(args);
                    JSONArray jsonArray = JSONUtil.parseArray(json);
                    // 创建一个字符串数组
                    String[] arr = new String[opList.size()];
                    //获取参数名
                    String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);

                    for (int j = 0; j < opList.size(); j++) {

                        //不是@RequestBody类型的数据参数
                        int indexOf = this.indexOf(parameterNames, opList.get(j));
                        if (indexOf != -1) {
                            arr[j] = args[indexOf].toString();
                            continue;
                        }

                        //是@RequestBody类型的数据参数
                        for (int i = 0; i < jsonArray.size(); i++) {
                            if (jsonArray.get(i).toString().startsWith("{")) {
                                JSONObject role = jsonArray.getJSONObject(i);
                                //编译需要获取的参数
                                if (role.containsKey(opList.get(j))) {
                                    arr[j] = role.getStr(opList.get(j));
                                }
                            }
                        }
                    }

                    //描述拼接
                    description = StrUtil.format(description, arr);
                }
            }

            log.info("Logging parameters！！resource：{}", resource);
            logRpcSerice.getLogService().addLog(userId, userName, resource, description);

        } catch (Exception e) {
            log.error("log record exception: {}", e.getMessage());
            throw throwWebException(e);
        }

        // 执行目标方法
        Object result = point.proceed();

        return result;
    }


    /**
     * @param: array
     * @param: name
     * @description: TODO   判断值是否存在
     * @return: int
     * @author: LiSen
     * @date: 2023/5/29
     */
    public int indexOf(String[] array, String name) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }


    public WebSystemException throwWebException(Exception e) {
        if (e instanceof WebSystemException) {
            return (WebSystemException) e;
        } else {
            return new WebSystemException(ErrorCode.SystemError, ErrorLevel.CRITICAL);
        }
    }


}
