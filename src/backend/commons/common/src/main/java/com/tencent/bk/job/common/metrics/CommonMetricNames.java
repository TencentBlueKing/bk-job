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

package com.tencent.bk.job.common.metrics;

/**
 * 公共监控指标
 */
public class CommonMetricNames {
    /**
     * 仅统计调用ESB BK-LOGIN API的HTTP请求过程
     */
    public static final String BK_LOGIN_API_HTTP = "job.client.bk.login.api.http";
    /**
     * 仅统计调用ESB BK-LOGIN API的整个过程，含反序列化
     */
    public static final String BK_LOGIN_API = "job.client.bk.login.api";
    /**
     * 仅统计调用ESB USER-MANAGE API的HTTP请求过程
     */
    public static final String USER_MANAGE_API_HTTP = "job.client.user.manage.api.http";
    /**
     * 统计调用ESB 用户管理 API的整个过程，含反序列化
     */
    public static final String USER_MANAGE_API = "job.client.user.manage.api";
    /**
     * 仅统计调用ESB CMSI API的HTTP请求过程
     */
    public static final String ESB_CMSI_API_HTTP = "job.client.cmsi.api.http";
    /**
     * 统计调用ESB CMSI API的整个过程，含反序列化
     */
    public static final String ESB_CMSI_API = "job.client.cmsi.api";
    /**
     * 仅统计调用ESB CMDB API的HTTP请求过程
     */
    public static final String ESB_CMDB_API_HTTP = "job.client.cmdb.api.http";

    /**
     * 仅统计调用权限中心后台 API的HTTP请求过程
     */
    public static final String IAM_API_HTTP = "job.client.iam.api.http";
    /**
     * 统计调用权限中心后台 API整个过程，含反序列化
     */
    public static final String IAM_API = "job.client.iam.api";

    /**
     * 仅统计调用消息通知中心后台 API的HTTP请求过程
     */
    public static final String BK_NOTICE_API_HTTP = "job.client.bknotice.api.http";
    /**
     * 统计调用消息通知中心后台 API整个过程，含反序列化
     */
    public static final String BK_NOTICE_API = "job.client.bknotice.api";

    /**
     * 仅统计调用AIDev后台 API的HTTP请求过程
     */
    public static final String BK_AI_DEV_API_HTTP = "job.client.bkAIDev.api.http";
    /**
     * 统计调用AIDev后台 API整个过程，含反序列化
     */
    public static final String BK_AI_DEV_API = "job.client.bkAIDev.api";


    /**
     * 被调用ESB API
     */
    public static final String ESB_API = "job.server.esb.api";
    /**
     * 被调用WEB API
     */
    public static final String WEB_API = "job.server.web.api";

    /**
     * 仅统计调用ESB GSE API的HTTP请求过程
     */
    public static final String ESB_GSE_API_HTTP = "job.client.gse.api.http";
    /**
     * 统计调用 GSE API整个过程，含反序列化
     */
    public static final String ESB_GSE_API = "job.client.gse.api";

}
