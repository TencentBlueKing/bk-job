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

package com.tencent.bk.job.common.esb.metrics;

public class EsbMetricTags {
    /**
     * API名称
     */
    public static final String KEY_API_NAME = "api_name";
    /**
     * 接口调用状态
     */
    public static final String KEY_STATUS = "status";
    /**
     * 接口调用状态：初始值
     */
    public static final String VALUE_STATUS_NONE = "none";
    /**
     * 接口调用状态：成功
     */
    public static final String VALUE_STATUS_SUCCESS = "success";
    /**
     * 接口调用状态：失败
     */
    public static final String VALUE_STATUS_FAIL = "fail";
    /**
     * 接口调用状态：超频
     */
    public static final String VALUE_STATUS_OVER_RATE = "over_rate";
    /**
     * 接口调用状态：错误
     */
    public static final String VALUE_STATUS_ERROR = "error";
}
