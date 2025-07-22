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
 * ksh语法差异实现
 */
public class KshSyntaxProcessor implements ShellSyntaxProcessor {

    @Override
    public String declareVariable(String varName, String varValue, boolean appendNewline) {
        String str = varName + "='" + escapeSingleQuote(varValue) + "'";
        return withOptionalNewline(str, appendNewline);
    }

    @Override
    public String declareIntVariable(String varName, int varValue, boolean appendNewline) {
        String str = varName + "=" + varValue;
        return withOptionalNewline(str, appendNewline);
    }

    @Override
    public String declareAssociativeArray(String varName, String varValue, boolean appendNewline) {
        String str = StringUtils.isNotBlank(varValue)
            ? "typeset -A " + varName + "=" + varValue
            : "typeset -A " + varName;
        return withOptionalNewline(str, appendNewline);
    }

    @Override
    public String declareIndexArray(String varName, String varValue, boolean appendNewline) {
        String str = StringUtils.isNotBlank(varValue)
            ? "typeset -a " + varName + "=" + varValue
            : "typeset -a " + varName;
        return withOptionalNewline(str, appendNewline);
    }
}
