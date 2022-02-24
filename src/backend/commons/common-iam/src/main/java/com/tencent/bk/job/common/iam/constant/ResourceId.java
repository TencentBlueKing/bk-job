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

package com.tencent.bk.job.common.iam.constant;

/**
 * @since 9/6/2020 15:38
 */
public class ResourceId {
    /**
     * 业务
     */
    public static final String BIZ = "biz";
    /**
     * 业务集
     */
    public static final String BUSINESS_SET = "business_set";
    /**
     * 脚本
     */
    public static final String SCRIPT = "script";
    /**
     * 作业模板
     */
    public static final String TEMPLATE = "job_template";
    /**
     * 作业执行方案
     */
    public static final String PLAN = "job_plan";
    /**
     * 定时任务
     */
    public static final String CRON = "cron";
    /**
     * 执行账号
     */
    public static final String ACCOUNT = "account";
    /**
     * 公共脚本
     */
    public static final String PUBLIC_SCRIPT = "public_script";
    /**
     * 标签
     */
    public static final String TAG = "tag";
    /**
     * 主机
     */
    public static final String HOST = "host";
    /**
     * 云区域
     */
    public static final String CLOUD_AREA = "sys_cloud_area";
    /**
     * 集群
     */
    public static final String SET = "set";
    /**
     * 模块
     */
    public static final String MODULE = "module";
    /**
     * 动态分组
     */
    public static final String DYNAMIC_GROUP = "biz_custom_query";
    /**
     * 运营视图
     */
    public static final String DASHBOARD_VIEW = "dashboard_view";
    /**
     * 文件源
     */
    public static final String FILE_SOURCE = "file_source";
    /**
     * 凭证
     */
    public static final String TICKET = "ticket";

}
