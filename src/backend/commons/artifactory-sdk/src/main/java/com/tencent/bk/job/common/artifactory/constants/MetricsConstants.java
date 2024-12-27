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

package com.tencent.bk.job.common.artifactory.constants;

public class MetricsConstants {

    /**
     * 仅统计调用制品库 API的HTTP请求过程
     */
    public static final String METRICS_NAME_BKREPO_API_HTTP = "job.client.bkrepo.api.http";
    /**
     * 统计调用制品库 API整个过程，含反序列化
     */
    public static final String METRICS_NAME_BKREPO_API = "job.client.bkrepo.api";

    public static final String TAG_KEY_API_NAME = "api_name";
    public static final String TAG_KEY_STATUS = "status";

    public static final String TAG_VALUE_OK = "ok";
    public static final String TAG_VALUE_ERROR = "error";
    public static final String TAG_VALUE_NONE = "none";
    /**
     * 用户请求参数错误：请求的节点路径在制品库中不存在
     */
    public static final String TAG_VALUE_CLIENT_ERROR_NODE_NOT_FOUND = "client.nodeNotFound";
}
