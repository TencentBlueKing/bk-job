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

package com.tencent.bk.job.execute.metrics;

public class ExecuteMetricsConstants {
    // metric name
    /**
     * 任务启动指标
     */
    public static final String NAME_JOB_TASK_START = "job.task.start";
    // tag
    /**
     * 任务启动方式
     */
    public static final String TAG_KEY_START_MODE = "start_mode";
    /**
     * 任务启动方式：页面执行
     */
    public static final String TAG_VALUE_START_MODE_WEB = "web";
    /**
     * 任务启动方式：API调用
     */
    public static final String TAG_VALUE_START_MODE_API = "api";
    /**
     * 任务启动方式：定时任务
     */
    public static final String TAG_VALUE_START_MODE_CRON = "cron";
    /**
     * 任务类型
     */
    public static final String TAG_KEY_TASK_TYPE = "task_type";
    /**
     * 任务类型：快速执行脚本
     */
    public static final String TAG_VALUE_TASK_TYPE_FAST_SCRIPT = "fast_script";
    /**
     * 任务类型：快速执行SQL
     */
    public static final String TAG_VALUE_TASK_TYPE_FAST_SQL = "fast_sql";
    /**
     * 任务类型：快速分发文件
     */
    public static final String TAG_VALUE_TASK_TYPE_FAST_FILE = "fast_file";
    /**
     * 任务类型：快速分发配置文件
     */
    public static final String TAG_VALUE_TASK_TYPE_FAST_CONFIG_FILE = "fast_config_file";
    /**
     * 任务类型：执行方案
     */
    public static final String TAG_VALUE_TASK_TYPE_EXECUTE_PLAN = "execute_plan";
    /**
     * 任务启动状态
     */
    public static final String TAG_KEY_START_STATUS = "start_status";
    /**
     * 任务启动状态：启动成功
     */
    public static final String TAG_VALUE_START_STATUS_SUCCESS = "success";
    /**
     * 任务启动状态：启动失败
     */
    public static final String TAG_VALUE_START_STATUS_FAILED = "failed";
}
