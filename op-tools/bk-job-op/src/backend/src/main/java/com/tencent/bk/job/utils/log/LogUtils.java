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

package com.tencent.bk.job.utils.log;

/**
 * 日志工具类
 */
public class LogUtils {

    /**
     * 默认单条日志最大长度：5000字符
     */
    private static final int MAX_LOG_LENGTH = 5_000;

    /**
     * 截断提示后缀
     */
    private static final String TRUNCATE_SUFFIX = "... [TRUNCATED, original length: %d]";

    /**
     * 截断字符串，默认长度 MAX_LOG_LENGTH，默认添加后缀
     *
     * @param str 原始字符串
     * @return 截断后的字符串
     */
    public static String truncate(String str) {
        return truncate(str, MAX_LOG_LENGTH);
    }

    /**
     * 截断字符串，使用自定义最大长度，默认添加后缀
     *
     * @param str       原始字符串
     * @param maxLength 最大长度（截断后的字符串长度，不包含后缀）
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        return truncate(str, maxLength, true);
    }

    /**
     * 截断字符串，使用自定义最大长度
     *
     * @param str        原始字符串
     * @param maxLength  最大长度（截断后的字符串长度，不包含后缀）
     * @param withSuffix 是否添加截断提示后缀
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength, boolean withSuffix) {
        if (str == null) {
            return null;
        }

        if (str.length() <= maxLength) {
            return str;
        }

        String truncated = str.substring(0, maxLength);
        
        if (withSuffix) {
            int originalLength = str.length();
            String suffix = String.format(TRUNCATE_SUFFIX, originalLength);
            return truncated + suffix;
        }
        
        return truncated;
    }
}
