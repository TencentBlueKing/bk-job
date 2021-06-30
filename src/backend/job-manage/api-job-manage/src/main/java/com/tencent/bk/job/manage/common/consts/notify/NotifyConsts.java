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

package com.tencent.bk.job.manage.common.consts.notify;

/**
 * @Description
 * @Date 2020/1/5
 * @Version 1.0
 */
public class NotifyConsts {
    // 作业平台超级管理员账号
    public static final String JOB_ADMINISTRATOR_NAME = "admin";

    // 传参未指定APP_ID时采取的默认通知策略
    public static final long DEFAULT_APP_ID = -1L;

    // 传参未指定RESOURCE_ID时采取的默认通知策略
    public static final String DEFAULT_RESOURCE_ID = "-1";

    // 传参未指定TRIGGER_USER时采取的默认通知策略
    public static final String DEFAULT_TRIGGER_USER = JOB_ADMINISTRATOR_NAME;

    public static final String SEPERATOR_COMMA = ",";
    public static final String SEPERATOR_ENTER = "\n";

    public static final String NOTIFY_CHANNEL_CODE_COMMON = "common";

    public static final String NOTIFY_TEMPLATE_CODE_CONFIRMATION = "confirmation";
    public static final String NOTIFY_TEMPLATE_CODE_EXECUTE_SUCCESS = "executeSuccess";
    public static final String NOTIFY_TEMPLATE_CODE_EXECUTE_FAILURE = "executeFailure";
    public static final String NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_EXECUTE = "beforeCronJobExecute";
    public static final String NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_END = "beforeCronJobEnd";
    public static final String NOTIFY_TEMPLATE_CODE_CRON_EXECUTE_FAILED = "cronJobFailed";

    public static final String NOTIFY_TEMPLATE_NAME_PREFIX = "job.manage.notify.template.name.";
    public static final String NOTIFY_TEMPLATE_NAME_CONFIRMATION =
        NOTIFY_TEMPLATE_NAME_PREFIX + NOTIFY_TEMPLATE_CODE_CONFIRMATION;
    public static final String NOTIFY_TEMPLATE_NAME_EXECUTE_SUCCESS =
        NOTIFY_TEMPLATE_NAME_PREFIX + NOTIFY_TEMPLATE_CODE_EXECUTE_SUCCESS;
    public static final String NOTIFY_TEMPLATE_NAME_EXECUTE_FAILURE =
        NOTIFY_TEMPLATE_NAME_PREFIX + NOTIFY_TEMPLATE_CODE_EXECUTE_FAILURE;
    public static final String NOTIFY_TEMPLATE_NAME_BEFORE_CRON_JOB_EXECUTE =
        NOTIFY_TEMPLATE_NAME_PREFIX + NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_EXECUTE;
    public static final String NOTIFY_TEMPLATE_NAME_BEFORE_CRON_JOB_END =
        NOTIFY_TEMPLATE_NAME_PREFIX + NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_END;
}
