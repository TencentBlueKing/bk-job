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
    public static final String NAME_NOTIFY_DELAY = "job.notify.delay";
    public static final String NAME_NOTIFY_POOL_SIZE = "job.notify.pool.size";
    public static final String NAME_NOTIFY_QUEUE_SIZE = "job.notify.queue.size";
    public static final String NAME_CMDB_QUERY_POOL_SIZE = "job.cmdb.query.pool.size";
    public static final String NAME_CMDB_QUERY_QUEUE_SIZE = "job.cmdb.query.queue.size";
    public static final String NAME_CMDB_RESOURCE_LIMIT = "job.cmdb.resource.limit";
    public static final String NAME_CMDB_RESOURCE_RATE = "job.cmdb.resource.rate";
    public static final String NAME_SYNC_APP_EXECUTOR_POOL_SIZE = "job.sync.app.executor.pool.size";
    public static final String NAME_SYNC_APP_EXECUTOR_QUEUE_SIZE = "job.sync.app.executor.queue.size";
    public static final String NAME_SYNC_HOST_EXECUTOR_POOL_SIZE = "job.sync.host.executor.pool.size";
    public static final String NAME_SYNC_HOST_EXECUTOR_QUEUE_SIZE = "job.sync.host.executor.queue.size";
    // 运营指标：业务类
    // 业务总量
    public static final String NAME_APPLICATION_COUNT = "job.application.count";
    // 上一次业务同步后经过的时间(s)
    public static final String NAME_APPLICATION_SYNC_AFTER_LAST_SECONDS = "job.application.sync.afterLast.seconds";
    // 主机总量
    public static final String NAME_HOST_COUNT = "job.host.count";
    // 上一次主机同步后经过的时间(s)
    public static final String NAME_HOST_SYNC_AFTER_LAST_SECONDS = "job.host.sync.afterLast.seconds";
    // 上一次主机状态同步后经过的时间(s)
    public static final String NAME_HOST_AGENT_STATUS_SYNC_AFTER_LAST_SECONDS = "job.host.agentStatus.sync.afterLast" +
        ".seconds";
    // 运营指标：资源类
    // 白名单IP总量
    public static final String NAME_WHITE_IP_COUNT_ALL = "job.whiteIp.count.all";
    // 模板总量
    public static final String NAME_TEMPLATE_COUNT_ALL = "job.template.count.all";
    // 执行方案总量
    public static final String NAME_TASK_PLAN_COUNT_ALL = "job.taskPlan.count.all";
    // 脚本总量
    public static final String NAME_SCRIPT_COUNT_ALL = "job.script.count.all";
    // 账号总量
    public static final String NAME_ACCOUNT_COUNT_ALL = "job.account.count.all";


    // tag key
    public static final String TAG_KEY_MODULE = "module";
    public static final String TAG_KEY_ASPECT = "aspect";
    public static final String TAG_KEY_RESOURCE_ID = "resourceId";
    public static final String TAG_KEY_APP_ID = "appId";
    public static final String TAG_KEY_MSG_TYPE = "msgType";
    public static final String TAG_KEY_SEND_STATUS = "sendStatus";


    // tag value
    public static final String TAG_VALUE_MODULE_APPLICATION = "application";
    public static final String TAG_VALUE_MODULE_HOST = "host";
    public static final String TAG_VALUE_MODULE_RESOURCE = "resource";
    public static final String TAG_VALUE_MODULE_TEMPLATE = "template";
    public static final String TAG_VALUE_MODULE_TASK_PLAN = "taskPlan";
    public static final String TAG_VALUE_MODULE_WHITE_IP = "whiteIp";
    public static final String TAG_VALUE_MODULE_SCRIPT = "script";
    public static final String TAG_VALUE_MODULE_ACCOUNT = "account";
    public static final String TAG_VALUE_MODULE_NOTIFY = "notify";
    public static final String TAG_VALUE_MODULE_CMDB = "cmdb";
    public static final String TAG_VALUE_MODULE_SYNC = "sync";
    public static final String TAG_VALUE_ASPECT_FLOW_CONTROL = "flowControl";
    public static final String TAG_VALUE_APP_ID_NULL = "null";
    // 消息通知发送成功
    public static final String TAG_VALUE_SEND_STATUS_SUCCESS = "success";
    // 消息通知发送失败
    public static final String TAG_VALUE_SEND_STATUS_FAILED = "failed";
}
