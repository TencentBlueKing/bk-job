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

package com.tencent.bk.job.common.service;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Job 变量解析工具
 */
public class VariableResolver {
    /**
     * 通过 job_import 方式声明导入的变量格式
     * example :
     * # job_import {{var1}}
     */
    private static final String JOB_IMPORT_VARIABLE_PATTERN = "^#\\s+job_import\\s+(\\{\\{[A-Za-z0-9_]+}})";

    /**
     * 脚本内置的变量格式
     * {{var}}
     */
    private static final String SCRIPT_BUILD_IN_VARIABLE_PATTERN = "(\\{\\{[A-Za-z0-9_]+}})";

    /**
     * 解析通过 job_import 方式声明导入的变量
     *
     * @param scriptContent 脚本内容
     * @return 变量名
     */
    public static List<String> resolveJobImportVariables(String scriptContent) {
        String[] lines = scriptContent.split("\n");
        Set<String> variables = new HashSet<>();
        for (String line : lines) {
            if (StringUtils.isEmpty(line) || !line.startsWith("#")) {
                continue;
            }
            Pattern pattern = Pattern.compile(JOB_IMPORT_VARIABLE_PATTERN);

            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                String variable = matcher.group(1).trim();
                variable = variable.substring(2, variable.length() - 2);
                variables.add(variable);
            }
        }
        return new ArrayList<>(variables);
    }

    /**
     * 解析脚本使用的Job内置变量
     *
     * @param content 脚本内容
     * @return 变量列表
     */
    public static Set<String> resolveScriptBuildInVariables(String content) {
        String[] lines = content.split("\n");
        Set<String> variables = new HashSet<>();
        for (String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            Pattern pattern = Pattern.compile(SCRIPT_BUILD_IN_VARIABLE_PATTERN);

            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                String variable = matcher.group(1).trim();
                variable = variable.substring(2, variable.length() - 2);
                variables.add(variable);
            }
        }
        return variables;
    }

    /**
     * 使用job的标准格式解析变量
     *
     * @param content 被解析的内容
     * @return 变量列表
     */
    public static List<String> resolveJobStandardVar(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }

        Matcher m = Pattern.compile("\\$\\{([_a-zA-Z][0-9_a-zA-Z]*)}").matcher(content);
        List<String> varNames = new ArrayList<>();
        while (m.find()) {
            String varName = m.group(1);
            if (!varNames.contains(varName)) {
                varNames.add(varName);
            }
        }
        return varNames;
    }

    /**
     * 从shell脚本解析shell变量
     *
     * @param shellScriptContent shell脚本内容
     * @return 变量列表
     */
    public static List<String> resolveShellScriptVar(String shellScriptContent) {
        if (StringUtils.isBlank(shellScriptContent)) {
            return Collections.emptyList();
        }
        String content = filterCommentLine(shellScriptContent);
        if (StringUtils.isBlank(content)) {
            return Collections.emptyList();
        }
        List<String> varNames = new ArrayList<>();

        Matcher m1 = Pattern.compile("\\$([_a-zA-Z][0-9_a-zA-Z]*)").matcher(content);
        while (m1.find()) {
            String varName = m1.group(1);
            if (!varNames.contains(varName)) {
                varNames.add(varName);
            }
        }
        Matcher m2 = Pattern.compile("\\$\\{[#!]?([_a-zA-Z][0-9_a-zA-Z]*)\\S*}").matcher(content);
        while (m2.find()) {
            String varName = m2.group(1);
            if (!varNames.contains(varName)) {
                varNames.add(varName);
            }
        }

        return varNames;
    }

    private static String filterCommentLine(String shellScriptContent) {
        String[] lines = shellScriptContent.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimLine = line.trim();
            if (StringUtils.isBlank(trimLine) || trimLine.startsWith("#")) {
                continue;
            }
            builder.append(line).append("\n");
        }
        return builder.toString();
    }
}
