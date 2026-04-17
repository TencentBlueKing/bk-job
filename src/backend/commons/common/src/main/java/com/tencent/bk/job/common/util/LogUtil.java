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

package com.tencent.bk.job.common.util;

import java.util.List;

/**
 * @Description
 * @Date 2019/12/23
 * @Version 1.0
 */
public class LogUtil {

    /**
     * 构建日志中需要打印出的部分列表元素字符串
     *
     * @param list          原始列表
     * @param maxElementNum 打印出的最大元素个数，-1表示无限制
     * @return 日志字符串
     */
    public static <T> String buildListLog(List<T> list, int maxElementNum) {
        if (list == null) {
            return "";
        }
        if (list.isEmpty() || maxElementNum == 0) {
            return "[]";
        } else if (list.size() <= maxElementNum || maxElementNum < 0) {
            return "[" + StringUtil.concatCollection(list) + "]";
        } else {
            return "(" + list.size() + " elements)["
                + StringUtil.concatCollection(list.subList(0, maxElementNum))
                + ",...]";
        }
    }

    private static String joinBySeparator(String separator, Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) {
                builder.append("null");
            } else {
                builder.append(arg.toString());
            }
            builder.append(separator);
        }
        builder.deleteCharAt(builder.lastIndexOf(","));
        return builder.toString();
    }

    public static String getInputLog(String prefix, Object... args) {
        return (prefix + "Input(" + joinBySeparator(",", args) + ")");
    }

    public static String getOutputLog(String prefix, Object... args) {
        return (prefix + "Output(" + joinBySeparator(",", args) + ")");
    }

    /**
     * 截取文本末尾，保留最多 maxChars 个字符；若发生截断，去掉开头的不完整行（即从第一个换行符之后开始）。
     *
     * @param text     原始文本
     * @param maxChars 最大保留字符数，必须 > 0
     * @return 截取后的文本；若未超限则原样返回；输入为 null 则返回 null
     */
    public static String tailLog(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        String tail = text.substring(text.length() - maxChars);
        int firstNewline = tail.indexOf('\n');
        if (firstNewline >= 0 && firstNewline < tail.length() - 1) {
            return tail.substring(firstNewline + 1);
        }
        return tail;
    }
}
