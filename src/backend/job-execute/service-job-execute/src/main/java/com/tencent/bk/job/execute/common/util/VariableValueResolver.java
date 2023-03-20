/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量值解析
 */
@Slf4j
public class VariableValueResolver {
    /**
     * 解析字符串中的变量值
     *
     * @param param       字符串参数
     * @param variableMap 变量KV
     * @return 解析后的字符串
     */
    public static String resolve(String param, Map<String, String> variableMap) {
        if (StringUtils.isBlank(param) || variableMap == null || variableMap.isEmpty()) {
            return param;
        }

        Matcher m = Pattern.compile("\\$\\{\\w+}").matcher(param);
        StringBuilder resolvedParam = new StringBuilder();
        boolean hasMatched = false;
        int notVarPartStart = 0;
        while (m.find()) {
            String paramTemplate = m.group();
            String paramName = paramTemplate.substring(2, paramTemplate.length() - 1);
            int varStart = m.start();
            resolvedParam.append(param, notVarPartStart, varStart);
            if (variableMap.containsKey(paramName)) {
                resolvedParam.append(variableMap.get(paramName) == null ? "" : variableMap.get(paramName));
            } else {
                resolvedParam.append(paramTemplate);
            }
            notVarPartStart = m.end();
            hasMatched = true;
        }
        if (hasMatched) {
            resolvedParam.append(param.substring(notVarPartStart));
        } else {
            resolvedParam.append(param);
        }
        return resolvedParam.toString();
    }
}
