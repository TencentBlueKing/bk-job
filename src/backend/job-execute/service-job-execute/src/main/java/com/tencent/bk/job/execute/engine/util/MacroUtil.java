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

package com.tencent.bk.job.execute.engine.util;

import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.engine.gse.Strftime;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 处理宏工具类
 *
 * @version 1.0
 * @date 2018/3/27
 */
public class MacroUtil {

    private static final String FILE_SRC_IP = "[FILESRCIP]";

    /**
     * 解析文件路径中的日期
     */
    public static String resolveDate(String str, long timestamp) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int startIndex = 0;
        while (true) {
            int matchStart = str.indexOf("[DATE:", startIndex);
            if (matchStart != -1) {
                int matchEnd = str.indexOf(']', (matchStart + 1) + "[DATE:".length());
                if (matchEnd != -1) {
                    String pattern = str.substring(matchStart + "[DATE:".length(), matchEnd);
                    String formatDate = "";
                    boolean isValidDatePattern = true;
                    try {
                        formatDate = DateUtils.formatUnixTimestamp(timestamp, ChronoUnit.MILLIS, pattern,
                            ZoneId.systemDefault());
                    } catch (Throwable e) {
                        isValidDatePattern = false;
                    }
                    sb.append(str.subSequence(startIndex, matchStart));
                    if (isValidDatePattern) {
                        sb.append(formatDate);
                    } else {
                        sb.append(str.subSequence(matchStart, matchEnd + 1));
                    }
                    startIndex = matchEnd + 1;
                    continue;
                }
            }
            sb.append(str.substring(startIndex));
            break;
        }

        return sb.toString();
    }

    public static String resolveDateWithStrfTime(String str) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        Date date = new Date();
        int startIndex = 0;
        while (true) {
            int matchStart = str.indexOf("[DATEFMT:", startIndex);
            if (matchStart != -1) {
                int matchEnd = str.indexOf(']', (matchStart + 1) + "[DATEFMT:".length());
                if (matchEnd != -1) {
                    String dateStr = str.substring(matchStart + "[DATEFMT:".length(), matchEnd);
                    sb.append(str.subSequence(startIndex, matchStart));
                    sb.append(strfdateFormat(dateStr, date));

                    startIndex = matchEnd + 1;
                    continue;
                }
            }
            sb.append(str.substring(startIndex));
            break;
        }

        return sb.toString();
    }

    /**
     * 解析源文件IP地址
     */
    public static String resolveFileSrcIpMacro(String filePath, String fileSrcIp) {
        fileSrcIp = fileSrcIp.replace(':', '_');
        return filePath.replace(FILE_SRC_IP, fileSrcIp);
    }

    private static String strfdateFormat(String pattern, Date date) {
        try {
            return new Strftime(pattern).format(date);
        } catch (Exception e) {
            return pattern;
        }
    }
}
