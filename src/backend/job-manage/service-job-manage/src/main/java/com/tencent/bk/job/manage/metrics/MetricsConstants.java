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

package com.tencent.bk.job.manage.metrics;

public class MetricsConstants {
    // metric name
    // 后台指标
    public static final String NAME_NOTIFY_POOL_SIZE = "notify.pool.size";
    public static final String NAME_NOTIFY_QUEUE_SIZE = "notify.queue.size";
    public static final String NAME_CMDB_QUERY_POOL_SIZE = "cmdb.query.pool.size";
    public static final String NAME_CMDB_QUERY_QUEUE_SIZE = "cmdb.query.queue.size";
    public static final String NAME_CMDB_RESOURCE_LIMIT = "cmdb.resource.limit";
    public static final String NAME_CMDB_RESOURCE_RATE = "cmdb.resource.rate";
    public static final String NAME_ANALYSIS_TASK_SCHEDULE_POOL_SIZE = "analysisTask.schedule.pool.size";
    public static final String NAME_ANALYSIS_TASK_SCHEDULE_QUEUE_SIZE = "analysisTask.schedule.queue.size";
    public static final String NAME_SYNC_APP_EXECUTOR_POOL_SIZE = "sync.app.executor.pool.size";
    public static final String NAME_SYNC_APP_EXECUTOR_QUEUE_SIZE = "sync.app.executor.queue.size";
    public static final String NAME_SYNC_HOST_EXECUTOR_POOL_SIZE = "sync.host.executor.pool.size";
    public static final String NAME_SYNC_HOST_EXECUTOR_QUEUE_SIZE = "sync.host.executor.queue.size";
    public static final String NAME_SYNC_AGENT_STATUS_EXECUTOR_POOL_SIZE = "sync.agentStatus.executor.pool.size";
    public static final String NAME_SYNC_AGENT_STATUS_EXECUTOR_QUEUE_SIZE = "sync.agentStatus.executor.queue.size";
    // 运营指标：业务类
    // 业务总量
    public static final String NAME_APPLICATION_COUNT = "application.count";
    // 上一次业务同步后经过的时间(s)
    public static final String NAME_APPLICATION_SYNC_AFTER_LAST_SECONDS = "application.sync.afterLast.seconds";
    // 主机总量
    public static final String NAME_HOST_COUNT = "host.count";
    // 上一次主机同步后经过的时间(s)
    public static final String NAME_HOST_SYNC_AFTER_LAST_SECONDS = "host.sync.afterLast.seconds";
    // 上一次主机状态同步后经过的时间(s)
    public static final String NAME_HOST_AGENT_STATUS_SYNC_AFTER_LAST_SECONDS = "host.agentStatus.sync.afterLast" +
        ".seconds";
    // Linux主机总量
    public static final String NAME_HOST_LINUX_COUNT = "host.linux.count";
    // Windows主机总量
    public static final String NAME_HOST_WINDOWS_COUNT = "host.windows.count";
    // AIX主机总量
    public static final String NAME_HOST_AIX_COUNT = "host.aix.count";
    // 运营指标：资源类
    // 白名单IP总量
    public static final String NAME_WHITE_IP_COUNT_ALL = "whiteIp.count.all";
    // 模板总量
    public static final String NAME_TEMPLATE_COUNT_ALL = "template.count.all";
    // 执行方案总量
    public static final String NAME_TASK_PLAN_COUNT_ALL = "taskPlan.count.all";
    // 脚本总量
    public static final String NAME_SCRIPT_COUNT_ALL = "script.count.all";
    // 脚本类型前缀
    public static final String NAME_SCRIPT_COUNT_TYPE_PREFIX = "script.count.type.";
    // 脚本状态前缀
    public static final String NAME_SCRIPT_COUNT_STATUS_PREFIX = "script.count.status.";
    // 脚本版本总量
    public static final String NAME_SCRIPT_VERSION_COUNT_ALL = "script.version.count.all";
    // 账号总量
    public static final String NAME_ACCOUNT_COUNT_ALL = "account.count.all";
    // 业务模板量
    public static final String NAME_TEMPLATE_COUNT_APP = "template.count.app";
    // 业务执行方案量
    public static final String NAME_TASK_PLAN_COUNT_APP = "template.count.app";
    // 业务脚本量
    public static final String NAME_SCRIPT_COUNT_APP = "script.count.app";
    // 业务脚本版本量
    public static final String NAME_SCRIPT_VERSION_COUNT_APP = "script.version.count.app";
    // 业务账号量
    public static final String NAME_ACCOUNT_COUNT_APP = "account.count.app";


    // tag
    public static final String TAG_MODULE = "module";
    public static final String TAG_ASPECT = "aspect";
    public static final String TAG_RESOURCE_ID = "resourceId";


    // value
    public static final String VALUE_MODULE_APPLICATION = "application";
    public static final String VALUE_MODULE_HOST = "host";
    public static final String VALUE_MODULE_RESOURCE = "resource";
    public static final String VALUE_MODULE_TEMPLATE = "template";
    public static final String VALUE_MODULE_TASK_PLAN = "taskPlan";
    public static final String VALUE_MODULE_WHITE_IP = "whiteIp";
    public static final String VALUE_MODULE_SCRIPT = "script";
    public static final String VALUE_MODULE_ACCOUNT = "account";
    public static final String VALUE_MODULE_NOTIFY = "notify";
    public static final String VALUE_MODULE_CMDB = "cmdb";
    public static final String VALUE_MODULE_ANALYSIS_TASK = "analysisTask";
    public static final String VALUE_MODULE_SYNC = "sync";

    public static final String VALUE_ASPECT_FLOW_CONTROL = "flowControl";
}
