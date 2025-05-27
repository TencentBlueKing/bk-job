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

package com.tencent.bk.job.logsvr.util;

import com.tencent.bk.job.logsvr.consts.LogTypeEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 集合名称工具类
 */
public class CollectionNameUtil {
    /**
     * 集合名称前缀
     */
    private static final String COLLECTION_PREFIX = "job_log";

    /**
     * 集合名各部分之间的分隔符
     */
    private static final String COLLECTION_NAME_SEPARATOR = "_";

    /**
     * 脚本执行日志类型
     */
    private static final String TYPE_SCRIPT = "script";

    /**
     * 文件分发日志类型
     */
    private static final String TYPE_FILE = "file";

    /**
     * 正则：提取集合名称中的日期部分
     */
    private static final Pattern LOG_COLLECTION_NAME_PATTERN = Pattern.compile(
        COLLECTION_PREFIX + "_(?:script|file)_(\\d{4}_\\d{2}_\\d{2})"
    );


    /**
     * 构建日志集合名
     *
     * @param jobCreateDate 作业创建日期，格式为 yyyy_MM_dd
     * @param logType 日志类型枚举
     * @return 集合名称，如：job_log_script_2025_01_01
     */
    public static String buildLogCollectionName(String jobCreateDate, LogTypeEnum logType) {
        return COLLECTION_PREFIX + COLLECTION_NAME_SEPARATOR +
            getLogTypeName(logType) + COLLECTION_NAME_SEPARATOR +
            jobCreateDate;
    }

    /**
     * 获取类型名称
     */
    public static String getLogTypeName(LogTypeEnum logType) {
        if (logType == LogTypeEnum.SCRIPT) {
            return TYPE_SCRIPT;
        } else if (logType == LogTypeEnum.FILE) {
            return TYPE_FILE;
        } else {
            throw new IllegalArgumentException("Invalid logType: " + logType);
        }
    }

    /**
     * 构建日志集合名称中不带日期的部分（下划线结尾），如：job_log_script_
     *
     * @param logType 日志类型
     * @return 集合名前缀字符串
     */
    public static String buildCollectionNamePrefix(LogTypeEnum logType) {
        return COLLECTION_PREFIX + COLLECTION_NAME_SEPARATOR +
            getLogTypeName(logType) + COLLECTION_NAME_SEPARATOR;
    }

    /**
     * 从日志集合名中提取日期部分
     *
     * @param collectionName MongoDB集合名，如：job_log_script_2025_01_01
     * @return String 日期字符串，如：2025_01_01
     */
    public static String collectionNameToDateStr(String collectionName) {
        Matcher m = LOG_COLLECTION_NAME_PATTERN.matcher(collectionName);
        if (!m.matches()) {
            return null;
        }
        return m.group(1);
    }
}
