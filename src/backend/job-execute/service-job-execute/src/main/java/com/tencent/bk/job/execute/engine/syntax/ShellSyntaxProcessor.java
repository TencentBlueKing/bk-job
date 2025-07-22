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

package com.tencent.bk.job.execute.engine.syntax;


import org.apache.commons.lang3.StringUtils;

/**
 * shell语法差异处理
 */
public interface ShellSyntaxProcessor {

    /**
     * 声明一个字符串变量
     */
    String declareVariable(String varName, String varValue, boolean appendNewline);

    /**
     * 声明一个关联数组
     */
    String declareIntVariable(String varName, int varValue, boolean appendNewline);


    /**
     * 声明一个关联数组
     */
    String declareAssociativeArray(String varName, String varValue, boolean appendNewline);

    /**
     * 声明一个索引数组
     */
    String declareIndexArray(String varName, String varValue, boolean appendNewline);

    /**
     * 单引号替换成'\''，避免Shell报错
     */
    default String escapeSingleQuote(String value) {
        if (StringUtils.isEmpty(value)) return "";
        return value.replaceAll("'", "'\\\\''");
    }

    /**
     * 如果appendNewline为true，则在content末尾添加换行
     */
    default String withOptionalNewline(String content, boolean appendNewline) {
        return appendNewline ? content + "\n" : content;
    }
}
