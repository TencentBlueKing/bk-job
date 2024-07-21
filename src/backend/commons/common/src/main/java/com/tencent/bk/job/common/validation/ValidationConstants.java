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

package com.tencent.bk.job.common.validation;

/**
 * 参数校验用到常量
 */
public class ValidationConstants {
    // 脚本版本正则表达式
    public static final String SCRIPT_VERSION_PATTERN = "^[A-Za-z0-9_\\-#@.]+$";
    // 文件源code正则表达式，由英文字符开头，1-32位英文字符、下划线、数字组成
    public static final String FILE_SOURCE_CODE_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{0,31}$";
    // ip地址正则表达式
    public static final String IP_PATTERN = "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)" +
        "\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)" +
        "\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";

    // 校验常用的最大值
    public static final int COMMON_MAX_2 = 2;
    public static final int COMMON_MAX_3 = 3;
    public static final int COMMON_MAX_60 = 60;
    public static final int COMMON_MAX_32 = 32;

    // 校验常用的最小值
    public static final int COMMON_MIN_0 = 0;
    public static final int COMMON_MIN_1 = 1;

    // 查询作业历史的最大时间跨度（ms）
    public static final long MAX_SEARCH_TASK_HISTORY_RANGE_MILLS = 30 * 24 * 60 * 60 * 1000L;

    // 批量查询作业执行日志, 主机数限制
    public static final int MAX_BATCH_SEARCH_LOGS_HOSTS_LIMIT = 500;
}
