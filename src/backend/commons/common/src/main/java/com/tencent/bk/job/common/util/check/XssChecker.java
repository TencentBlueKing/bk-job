/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.common.util.check;

import com.tencent.bk.job.common.util.check.exception.StringCheckException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XSS 攻击字符检查：禁止字符串中包含 HTML 标签相关字符（{@code < > " '}），防止 XSS 注入攻击。
 */
public class XssChecker implements IStringCheckStrategy {

    /**
     * XSS 攻击关键字符：HTML 标签符号 < >，以及属性值引号 " '
     */
    private static final Pattern XSS_PATTERN = Pattern.compile("[<>\"']");

    @Override
    public String checkAndGetResult(String rawStr) {
        if (rawStr == null) {
            return null;
        }
        Matcher m = XSS_PATTERN.matcher(rawStr);
        if (m.find()) {
            throw new StringCheckException(rawStr + " cannot contain HTML special characters < > \" '");
        }
        return rawStr;
    }
}
