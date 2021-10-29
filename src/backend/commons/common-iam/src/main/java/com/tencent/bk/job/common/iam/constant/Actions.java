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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Actions {
    // 脚本
    public static final ActionInfo CREATE_SCRIPT = new ActionInfo(ActionId.CREATE_SCRIPT, ResourceTypeEnum.BUSINESS);
    public static final ActionInfo VIEW_SCRIPT = new ActionInfo(ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT);
    public static final ActionInfo MANAGE_SCRIPT = new ActionInfo(ActionId.MANAGE_SCRIPT, ResourceTypeEnum.SCRIPT);
    public static final ActionInfo EXECUTE_SCRIPT = new ActionInfo(ActionId.EXECUTE_SCRIPT,
        Arrays.asList(ResourceTypeEnum.SCRIPT, ResourceTypeEnum.HOST));

    //模板
    public static final ActionInfo CREATE_JOB_TEMPLATE = new ActionInfo(ActionId.CREATE_JOB_TEMPLATE,
        ResourceTypeEnum.BUSINESS);
    public static final ActionInfo VIEW_JOB_TEMPLATE = new ActionInfo(ActionId.VIEW_JOB_TEMPLATE,
        ResourceTypeEnum.TEMPLATE);
    public static final ActionInfo EDIT_JOB_TEMPLATE = new ActionInfo(ActionId.EDIT_JOB_TEMPLATE,
        ResourceTypeEnum.TEMPLATE);
    public static final ActionInfo DELETE_JOB_TEMPLATE = new ActionInfo(ActionId.DELETE_JOB_TEMPLATE,
        ResourceTypeEnum.TEMPLATE);
    public static final ActionInfo DEBUG_JOB_TEMPLATE = new ActionInfo(ActionId.DEBUG_JOB_TEMPLATE,
        Arrays.asList(ResourceTypeEnum.TEMPLATE, ResourceTypeEnum.HOST));

    // 方案
    public static final ActionInfo CREATE_JOB_PLAN = new ActionInfo(ActionId.CREATE_JOB_PLAN,
        ResourceTypeEnum.TEMPLATE);
    public static final ActionInfo VIEW_JOB_PLAN = new ActionInfo(ActionId.VIEW_JOB_PLAN,
        ResourceTypeEnum.PLAN);
    public static final ActionInfo EDIT_JOB_PLAN = new ActionInfo(ActionId.EDIT_JOB_PLAN,
        ResourceTypeEnum.PLAN);
    public static final ActionInfo DELETE_JOB_PLAN = new ActionInfo(ActionId.DELETE_JOB_PLAN,
        ResourceTypeEnum.PLAN);
    public static final ActionInfo LAUNCH_JOB_PLAN = new ActionInfo(ActionId.LAUNCH_JOB_PLAN,
        Arrays.asList(ResourceTypeEnum.PLAN, ResourceTypeEnum.HOST));
    public static final ActionInfo SYNC_JOB_PLAN = new ActionInfo(ActionId.SYNC_JOB_PLAN,
        ResourceTypeEnum.PLAN);

    // 定时任务
    public static final ActionInfo CREATE_CRON = new ActionInfo(ActionId.CREATE_CRON,
        ResourceTypeEnum.BUSINESS);
    public static final ActionInfo MANAGE_CRON = new ActionInfo(ActionId.MANAGE_CRON,
        ResourceTypeEnum.CRON);

    // 执行历史
    public static final ActionInfo VIEW_HISTORY = new ActionInfo(ActionId.VIEW_HISTORY, ResourceTypeEnum.BUSINESS);

    // 账号
    public static final ActionInfo CREATE_ACCOUNT = new ActionInfo(ActionId.CREATE_ACCOUNT,
        ResourceTypeEnum.BUSINESS);
    public static final ActionInfo MANAGE_ACCOUNT = new ActionInfo(ActionId.MANAGE_ACCOUNT,
        ResourceTypeEnum.ACCOUNT);
    public static final ActionInfo USE_ACCOUNT = new ActionInfo(ActionId.USE_ACCOUNT,
        ResourceTypeEnum.ACCOUNT);

    // 标签
    public static final ActionInfo CREATE_TAG = new ActionInfo(ActionId.CREATE_TAG,
        ResourceTypeEnum.BUSINESS);
    public static final ActionInfo MANAGE_TAG = new ActionInfo(ActionId.MANAGE_TAG,
        ResourceTypeEnum.TAG);

    // 白名单
    public static final ActionInfo CREATE_WHITELIST = new ActionInfo(ActionId.CREATE_WHITELIST);
    public static final ActionInfo MANAGE_WHITELIST = new ActionInfo(ActionId.MANAGE_WHITELIST);

    // 公共脚本
    public static final ActionInfo CREATE_PUBLIC_SCRIPT = new ActionInfo(ActionId.CREATE_PUBLIC_SCRIPT);
    public static final ActionInfo EXECUTE_PUBLIC_SCRIPT = new ActionInfo(ActionId.EXECUTE_PUBLIC_SCRIPT,
        Arrays.asList(ResourceTypeEnum.PUBLIC_SCRIPT, ResourceTypeEnum.HOST));
    public static final ActionInfo MANAGE_PUBLIC_SCRIPT_INSTANCE =
        new ActionInfo(ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, ResourceTypeEnum.PUBLIC_SCRIPT);

    // 快速脚本执行
    public static final ActionInfo QUICK_EXECUTE_SCRIPT = new ActionInfo(ActionId.QUICK_EXECUTE_SCRIPT,
        ResourceTypeEnum.HOST);
    // 快速文件分发
    public static final ActionInfo QUICK_TRANSFER_FILE = new ActionInfo(ActionId.QUICK_TRANSFER_FILE,
        ResourceTypeEnum.HOST);
    // 通知设置
    public static final ActionInfo NOTIFICATION_SETTING = new ActionInfo(ActionId.NOTIFICATION_SETTING);
    // 全局设置
    public static final ActionInfo GLOBAL_SETTINGS = new ActionInfo(ActionId.GLOBAL_SETTINGS);
    // 运营视图查看
    public static final ActionInfo DASHBOARD_VIEW = new ActionInfo(ActionId.DASHBOARD_VIEW);
    // 服务状态查看
    public static final ActionInfo SERVICE_STATE_ACCESS = new ActionInfo(ActionId.SERVICE_STATE_ACCESS);
    // 业务查看
    public static final ActionInfo LIST_BUSINESS = new ActionInfo(ActionId.LIST_BUSINESS, ResourceTypeEnum.BUSINESS);
    // 文件源
    public static final ActionInfo CREATE_FILE_SOURCE = new ActionInfo(ActionId.CREATE_FILE_SOURCE,
        ResourceTypeEnum.BUSINESS);
    public static final ActionInfo VIEW_FILE_SOURCE = new ActionInfo(ActionId.VIEW_FILE_SOURCE,
        ResourceTypeEnum.FILE_SOURCE);
    public static final ActionInfo MANAGE_FILE_SOURCE = new ActionInfo(ActionId.MANAGE_FILE_SOURCE,
        ResourceTypeEnum.FILE_SOURCE);
    // 凭证
    public static final ActionInfo CREATE_TICKET = new ActionInfo(ActionId.CREATE_TICKET, ResourceTypeEnum.BUSINESS);
    public static final ActionInfo USE_TICKET = new ActionInfo(ActionId.USE_TICKET, ResourceTypeEnum.TICKET);
    public static final ActionInfo MANAGE_TICKET = new ActionInfo(ActionId.MANAGE_TICKET, ResourceTypeEnum.TICKET);
    // 高危语句
    // 高危语句规则管理
    public static final ActionInfo HIGH_RISK_DETECT_RULE = new ActionInfo(ActionId.HIGH_RISK_DETECT_RULE);
    // 高危语句拦截记录查看
    public static final ActionInfo HIGH_RISK_DETECT_RECORD = new ActionInfo(ActionId.HIGH_RISK_DETECT_RECORD);

    private static Map<String, ActionInfo> actions = new HashMap<>();

    static {
        actions.put(CREATE_SCRIPT.getActionId(), CREATE_SCRIPT);
        actions.put(VIEW_SCRIPT.getActionId(), VIEW_SCRIPT);
        actions.put(MANAGE_SCRIPT.getActionId(), MANAGE_SCRIPT);
        actions.put(EXECUTE_SCRIPT.getActionId(), EXECUTE_SCRIPT);

        actions.put(CREATE_JOB_TEMPLATE.getActionId(), CREATE_JOB_TEMPLATE);
        actions.put(VIEW_JOB_TEMPLATE.getActionId(), VIEW_JOB_TEMPLATE);
        actions.put(EDIT_JOB_TEMPLATE.getActionId(), EDIT_JOB_TEMPLATE);
        actions.put(DELETE_JOB_TEMPLATE.getActionId(), DELETE_JOB_TEMPLATE);
        actions.put(DEBUG_JOB_TEMPLATE.getActionId(), DEBUG_JOB_TEMPLATE);

        actions.put(CREATE_JOB_PLAN.getActionId(), CREATE_JOB_PLAN);
        actions.put(VIEW_JOB_PLAN.getActionId(), VIEW_JOB_PLAN);
        actions.put(EDIT_JOB_PLAN.getActionId(), EDIT_JOB_PLAN);
        actions.put(DELETE_JOB_PLAN.getActionId(), DELETE_JOB_PLAN);
        actions.put(LAUNCH_JOB_PLAN.getActionId(), LAUNCH_JOB_PLAN);
        actions.put(SYNC_JOB_PLAN.getActionId(), SYNC_JOB_PLAN);

        actions.put(CREATE_CRON.getActionId(), CREATE_CRON);
        actions.put(MANAGE_CRON.getActionId(), MANAGE_CRON);

        actions.put(VIEW_HISTORY.getActionId(), VIEW_HISTORY);

        actions.put(CREATE_ACCOUNT.getActionId(), CREATE_ACCOUNT);
        actions.put(MANAGE_ACCOUNT.getActionId(), MANAGE_ACCOUNT);
        actions.put(USE_ACCOUNT.getActionId(), USE_ACCOUNT);

        actions.put(CREATE_TAG.getActionId(), CREATE_TAG);
        actions.put(MANAGE_TAG.getActionId(), MANAGE_TAG);

        actions.put(CREATE_WHITELIST.getActionId(), CREATE_WHITELIST);
        actions.put(MANAGE_WHITELIST.getActionId(), MANAGE_WHITELIST);

        actions.put(CREATE_PUBLIC_SCRIPT.getActionId(), CREATE_PUBLIC_SCRIPT);
        actions.put(EXECUTE_PUBLIC_SCRIPT.getActionId(), EXECUTE_PUBLIC_SCRIPT);
        actions.put(MANAGE_PUBLIC_SCRIPT_INSTANCE.getActionId(), MANAGE_PUBLIC_SCRIPT_INSTANCE);

        actions.put(QUICK_EXECUTE_SCRIPT.getActionId(), QUICK_EXECUTE_SCRIPT);
        actions.put(QUICK_TRANSFER_FILE.getActionId(), QUICK_TRANSFER_FILE);

        actions.put(NOTIFICATION_SETTING.getActionId(), NOTIFICATION_SETTING);
        actions.put(GLOBAL_SETTINGS.getActionId(), GLOBAL_SETTINGS);
        actions.put(DASHBOARD_VIEW.getActionId(), DASHBOARD_VIEW);
        actions.put(SERVICE_STATE_ACCESS.getActionId(), SERVICE_STATE_ACCESS);
        actions.put(LIST_BUSINESS.getActionId(), LIST_BUSINESS);

        actions.put(CREATE_FILE_SOURCE.getActionId(), CREATE_FILE_SOURCE);
        actions.put(VIEW_FILE_SOURCE.getActionId(), VIEW_FILE_SOURCE);
        actions.put(MANAGE_FILE_SOURCE.getActionId(), MANAGE_FILE_SOURCE);

        actions.put(CREATE_TICKET.getActionId(), CREATE_TICKET);
        actions.put(USE_TICKET.getActionId(), USE_TICKET);
        actions.put(MANAGE_TICKET.getActionId(), MANAGE_TICKET);

        actions.put(HIGH_RISK_DETECT_RULE.getActionId(), HIGH_RISK_DETECT_RULE);
        actions.put(HIGH_RISK_DETECT_RECORD.getActionId(), HIGH_RISK_DETECT_RECORD);
    }

    public static ActionInfo getActionInfo(String actionId) {
        return actions.get(actionId);
    }
}
