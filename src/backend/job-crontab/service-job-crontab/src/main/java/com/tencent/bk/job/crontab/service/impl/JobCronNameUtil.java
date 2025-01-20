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

package com.tencent.bk.job.crontab.service.impl;

/**
 * Job定时任务名称工具类
 */
public class JobCronNameUtil {

    /**
     * 获取Quartz Job名称
     *
     * @param cronJobId 定时任务ID
     * @return Quartz Job名称
     */
    public static String getJobName(long cronJobId) {
        return "job_" + cronJobId;
    }

    /**
     * 获取Quartz Job分组
     *
     * @param appId Job业务ID
     * @return Quartz Job分组
     */
    public static String getJobGroup(long appId) {
        return "bk_app_" + appId;
    }

    /***
     * 获取通知Job名称
     * @param cronJobId 定时任务ID
     * @return 通知Job名称
     */
    public static String getNotifyJobName(long cronJobId) {
        return getJobName(cronJobId) + "_notify";
    }
}
