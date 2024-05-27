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

package com.tencent.bk.job.common.util.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FileSizeUtil {

    /**
     * 根据字节数获取易于读取的带单位的文件大小描述
     *
     * @param byteNum 字节数
     * @return 易于读取的带单位的文件大小描述
     */
    public static String getFileSizeStr(Long byteNum) {
        if (byteNum == null) return "--";
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};
        int i = 0;
        double value = (float) byteNum;
        while (value >= 1024 && i < units.length - 1) {
            value = value / 1024.;
            i += 1;
        }
        return String.format("%.2f", value) + units[i];
    }

    /**
     * 从带单位的文件大小描述中解析出真实表示的字节数
     *
     * @param fileSizeStr 带单位的文件大小描述
     * @return 真实表示的字节数
     */
    public static long parseFileSizeBytes(String fileSizeStr) {
        if (StringUtils.isBlank(fileSizeStr)) {
            throw new IllegalArgumentException("Invalid fileSize : " + fileSizeStr);
        }
        long factor = 1L;
        fileSizeStr = fileSizeStr.trim().toUpperCase();
        if (fileSizeStr.endsWith("B")) {
            fileSizeStr = fileSizeStr.replace("B", "");
        }
        if (fileSizeStr.endsWith("P")) {
            fileSizeStr = fileSizeStr.replace("P", "");
            factor = 1024L * 1024L * 1024L * 1024L * 1024L;
        }
        if (fileSizeStr.endsWith("T")) {
            fileSizeStr = fileSizeStr.replace("T", "");
            factor = 1024L * 1024L * 1024L * 1024L;
        }
        if (fileSizeStr.endsWith("G")) {
            fileSizeStr = fileSizeStr.replace("G", "");
            factor = 1024L * 1024L * 1024L;
        }
        if (fileSizeStr.endsWith("M")) {
            fileSizeStr = fileSizeStr.replace("M", "");
            factor = 1024L * 1024L;
        }
        if (fileSizeStr.endsWith("K")) {
            fileSizeStr = fileSizeStr.replace("K", "");
            factor = 1024L;
        }
        return Math.round(Double.parseDouble(fileSizeStr) * factor);
    }
}
