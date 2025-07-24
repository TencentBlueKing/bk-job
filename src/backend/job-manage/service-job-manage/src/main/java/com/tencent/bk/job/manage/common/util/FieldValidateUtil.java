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

package com.tencent.bk.job.manage.common.util;

import org.jooq.tools.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段校验
 */
public class FieldValidateUtil {
    private static final Pattern SCRIPT_NAME_PATTERN = Pattern.compile("^[^./\\\\'\":;|=," +
        "+*?<>\\[\\]{}\\s]{1}[^/\\\\'\":;|=,+*?<>\\[\\]{}\\s]{0,99}");

    /**
     * 脚本脚本名称
     *
     * @param scriptName
     * @return
     */
    public static boolean validateScriptName(String scriptName) {
        if (StringUtils.isBlank(scriptName)) {
            return false;
        }
        Matcher matcher = SCRIPT_NAME_PATTERN.matcher(scriptName);
        return matcher.matches();
    }
}
