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

package com.tencent.bk.job.common.iam.constant;

/**
 * IAM v4 预置角色 ID。
 *
 * <p>v4 取消了 v3 的 {@code related_actions}，改由角色聚合操作，用户按角色申请权限。
 * 各角色包含的操作清单是模型注册数据，见 v4 模型 JSON。</p>
 *
 * <p>角色分两类视角：业务 / 平台视角关联 {@code biz} 或不关联资源，横跨多种资源；
 * 单资源视角只关联单一资源类型，用于按实例授权（含资源创建者授权，见
 * {@link com.tencent.bk.job.common.iam.util.IamV4CreatorRoleMapper}）。</p>
 */
public class IamV4RoleId {

    /**
     * 业务只读
     */
    public static final String READ_ONLY = "read_only";
    /**
     * 业务使用者：可执行已有资源，不可创建/编辑
     */
    public static final String USER = "user";
    /**
     * 业务开发：可创建/编辑/执行业务内资源
     */
    public static final String DEVELOPER = "developer";
    /**
     * 业务运维：开发权限 + 账号/凭证/文件源等敏感资源管理
     */
    public static final String OPERATOR = "operator";
    /**
     * 平台公共资源管理员（系统级）
     */
    public static final String PLATFORM_ADMIN = "platform_admin";
    // ---------- 单资源视角：只含所关联资源类型上的操作，按实例授权 ----------

    /**
     * 公共脚本管理员（关联 public_script）
     */
    public static final String PUBLIC_SCRIPT_MANAGER = "public_script_manager";

    /**
     * 业务脚本管理员（关联 script）
     */
    public static final String SCRIPT_MANAGER = "script_manager";
    /**
     * 作业模版管理员（关联 job_template）
     */
    public static final String JOB_TEMPLATE_MANAGER = "job_template_manager";
    /**
     * 执行方案管理员（关联 job_plan）
     */
    public static final String JOB_PLAN_MANAGER = "job_plan_manager";
    /**
     * 标签管理员（关联 tag）
     */
    public static final String TAG_MANAGER = "tag_manager";
    /**
     * 定时任务管理员（关联 cron）
     */
    public static final String CRON_MANAGER = "cron_manager";
    /**
     * 文件源管理员（关联 file_source）
     */
    public static final String FILE_SOURCE_MANAGER = "file_source_manager";
    /**
     * 凭证管理员（关联 ticket）
     */
    public static final String TICKET_MANAGER = "ticket_manager";
    /**
     * 执行账号管理员（关联 account）
     */
    public static final String ACCOUNT_MANAGER = "account_manager";

    private IamV4RoleId() {
    }
}
