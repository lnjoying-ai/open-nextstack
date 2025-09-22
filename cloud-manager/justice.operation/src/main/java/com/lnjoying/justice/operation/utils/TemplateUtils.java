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

package com.lnjoying.justice.operation.utils;

//import org.apache.commons.io.FileUtils;
//import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.bean.BeanUtil;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.util.Map;

//import org.apache.commons.io.FileUtils;
@Slf4j
public class TemplateUtils
{
    public static String parseThymeleafTemplate(final String template, final Object params)
    {
        return parseThymeleafTemplate(template, BeanUtil.beanToMap(params));
    }

    public static String parseThymeleafTemplate(final String template, final Map<String, Object> params)
    {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".yml");
        templateResolver.setPrefix("templates/"); // templates is the folder name under resources
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCharacterEncoding("UTF-8");
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
//        context.setVariable("group","test");
        context.setVariables(params);
//        context.setVariable("to", "Baeldung");

        return templateEngine.process(template, context);
    }

    public static void generateYmlFile(final String template, final Map<String, Object> params, final String filePath)
    {
        String ymlContent = parseThymeleafTemplate(template, params);
        try
        {
//            FileUtil.writeAsString()FileUtil.getFile(filePath), ymlContent, "UTF-8"
            FileUtils.writeStringToFile(FileUtils.getFile(filePath), ymlContent, "UTF-8");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void generateYmlFile(final String template, final Object params, final String filePath)
    {
        generateYmlFile(template, BeanUtil.beanToMap(params), filePath);
    }

    public static void removeYmlFile(String filePath)
    {
        File file = FileUtils.getFile(filePath);
        if (!file.exists())
        {
            log.info("The current detection file does not exist or has been cleaned up！");
            return;
            //throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
        if (!FileUtils.deleteQuietly(file))
        {
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }


}
