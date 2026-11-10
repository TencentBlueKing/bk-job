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
 * IAM v4 资源类型 ID。
 *
 * <p>独立于 {@link ResourceTypeId} 维护：部分资源类型在 v4 模型中使用了不同的 ID（业务集、动态分组），
 * 且 v4 新增了若干仅用于实例视图的资源类型。两套 ID 在灰度期同时生效，转换见
 * {@link com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper}。</p>
 */
public class IamV4ResourceTypeId {

    // ---------- 业务级首段 ----------
    // 逐段匹配要求不同实例视图的首段互不相同，因此「业务」在不同视图下有多个变体，
    // 它们都代理到 CMDB 的同一个业务资源。

    /**
     * 业务（业务 → 资源 视图首段）
     */
    public static final String BIZ = "biz";
    /**
     * 业务集（业务集 → 资源 视图首段）
     */
    public static final String BIZ_SET = "biz_set";
    /**
     * 租户集（租户集 → 资源 视图首段，仅多租户）
     */
    public static final String TENANT_SET = "tenant_set";
    /**
     * 业务（业务 → 动态分组 视图首段）
     */
    public static final String BIZ_DYNAMIC_GROUP = "biz_dynamic_group";
    /**
     * 业务（业务 → 集群 → 命名空间 视图首段）
     */
    public static final String BIZ_CONTAINER = "biz_container";

    // ---------- 主机与拓扑 ----------

    /**
     * 主机
     */
    public static final String HOST = "host";
    /**
     * 集群（CMDB 业务拓扑中的 set）
     */
    public static final String SET = "set";
    /**
     * 模块
     */
    public static final String MODULE = "module";
    /**
     * 动态分组（动态分组视图叶子）
     */
    public static final String DYNAMIC_GROUP = "dynamic_group";

    // ---------- 容器 ----------

    /**
     * K8s 集群（容器视图中间段）
     */
    public static final String CLUSTER = "cluster";
    /**
     * K8s 命名空间（容器视图叶子）
     */
    public static final String NAMESPACE = "namespace";

    // ---------- Job 自有资源 ----------

    /**
     * 脚本
     */
    public static final String SCRIPT = "script";
    /**
     * 公共脚本
     */
    public static final String PUBLIC_SCRIPT = "public_script";
    /**
     * 作业模板
     */
    public static final String TEMPLATE = "job_template";
    /**
     * 执行方案
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
     * 标签
     */
    public static final String TAG = "tag";
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

    private IamV4ResourceTypeId() {
    }
}
