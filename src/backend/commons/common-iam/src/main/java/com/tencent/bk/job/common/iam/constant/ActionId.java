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
 * @since 9/6/2020 15:20
 */
public class ActionId {
    // 脚本
    public static final String CREATE_SCRIPT = "create_script";
    public static final String VIEW_SCRIPT = "view_script";
    public static final String MANAGE_SCRIPT = "manage_script";
    public static final String EXECUTE_SCRIPT = "execute_script";
    public static final String VIEW_JOB_TEMPLATE = "view_job_template";
    public static final String EDIT_JOB_TEMPLATE = "edit_job_template";
    public static final String DELETE_JOB_TEMPLATE = "delete_job_template";
    public static final String DEBUG_JOB_TEMPLATE = "debug_job_template";
    // 方案
    public static final String CREATE_JOB_PLAN = "create_job_plan";
    public static final String VIEW_JOB_PLAN = "view_job_plan";
    public static final String EDIT_JOB_PLAN = "edit_job_plan";
    public static final String DELETE_JOB_PLAN = "delete_job_plan";
    public static final String LAUNCH_JOB_PLAN = "launch_job_plan";
    public static final String SYNC_JOB_PLAN = "sync_job_plan";
    // 定时任务
    public static final String CREATE_CRON = "create_cron";
    public static final String MANAGE_CRON = "manage_cron";
    // 执行历史
    public static final String VIEW_HISTORY = "view_history";
    // 账号
    public static final String CREATE_ACCOUNT = "create_account";
    public static final String MANAGE_ACCOUNT = "manage_account";
    public static final String USE_ACCOUNT = "use_account";
    // 标签
    public static final String CREATE_TAG = "create_tag";
    public static final String MANAGE_TAG = "manage_tag";
    // 白名单
    public static final String CREATE_WHITELIST = "create_whitelist";
    public static final String MANAGE_WHITELIST = "manage_whitelist";
    // 公共脚本
    public static final String CREATE_PUBLIC_SCRIPT = "create_public_script";
    public static final String EXECUTE_PUBLIC_SCRIPT = "execute_public_script";
    public static final String MANAGE_PUBLIC_SCRIPT_INSTANCE = "manage_public_script_instance";
    // 快速脚本执行
    public static final String QUICK_EXECUTE_SCRIPT = "quick_execute_script";
    // 快速文件分发
    public static final String QUICK_TRANSFER_FILE = "quick_transfer_file";
    // 通知设置
    public static final String NOTIFICATION_SETTING = "notification_setting";
    // 全局设置
    public static final String GLOBAL_SETTINGS = "global_settings";
    // 运营视图查看
    public static final String DASHBOARD_VIEW = "dashboard_view";
    // 服务状态查看
    public static final String SERVICE_STATE_ACCESS = "service_state_access";
    // 作业
    public static final String CREATE_JOB_TEMPLATE = "create_job_template";
    // 业务查看
    public static final String LIST_BUSINESS = "access_business";
    // 文件源
    public static final String VIEW_FILE_SOURCE = "view_file_source";
    public static final String CREATE_FILE_SOURCE = "create_file_source";
    public static final String MANAGE_FILE_SOURCE = "manage_file_source";
    // 凭证
    public static final String USE_TICKET = "use_ticket";
    public static final String CREATE_TICKET = "create_ticket";
    public static final String MANAGE_TICKET = "manage_ticket";
}
