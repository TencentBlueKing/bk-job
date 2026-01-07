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

package com.tencent.bk.job.common.retry.metrics;

/**
 * 外部系统重试指标常量
 */
public class RetryMetricsConstants {

    // ==================== 指标名称 ====================
    /**
     * 外部系统重试指标名
     */
    public static final String NAME_EXTERNAL_SYSTEM_RETRY = "external.system.retry";

    // ==================== Tag Keys ====================
    /**
     * 外部系统名称
     */
    public static final String TAG_KEY_SYSTEM = "system";

    /**
     * API 名称/方法名
     */
    public static final String TAG_KEY_API = "api";

    /**
     * 重试次数
     */
    public static final String TAG_KEY_RETRY_COUNT = "retry_count";

    /**
     * 最终结果
     */
    public static final String TAG_KEY_FINAL_RESULT = "final_result";

    // ==================== Tag Values - System ====================
    /**
     * GSE 系统
     */
    public static final String TAG_VALUE_SYSTEM_GSE = "gse";

    /**
     * CMDB 系统
     */
    public static final String TAG_VALUE_SYSTEM_CMDB = "cmdb";

    /**
     * IAM 系统
     */
    public static final String TAG_VALUE_SYSTEM_IAM = "iam";

    /**
     * BK-Login 系统
     */
    public static final String TAG_VALUE_SYSTEM_BK_LOGIN = "bk-login";

    /**
     * BK-User 系统
     */
    public static final String TAG_VALUE_SYSTEM_BK_USER = "bk-user";

    // ==================== Tag Values - Final Result ====================
    /**
     * 最终成功
     */
    public static final String TAG_VALUE_RESULT_SUCCESS = "success";

    /**
     * 最终失败
     */
    public static final String TAG_VALUE_RESULT_FAILURE = "failure";
}
