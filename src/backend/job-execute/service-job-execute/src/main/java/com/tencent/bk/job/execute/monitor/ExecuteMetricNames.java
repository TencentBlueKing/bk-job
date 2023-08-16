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

package com.tencent.bk.job.execute.monitor;

/**
 * 执行引擎监控指标
 */
public class ExecuteMetricNames {
    /**
     * 接收GSE任务数
     */
    public static final String GSE_TASKS_TOTAL = "job.gse.tasks.total";
    /**
     * 完成GSE任务数
     */
    public static final String GSE_FINISHED_TASKS_TOTAL = "job.gse.finished.tasks.total";
    /**
     * 正在处理的gse任务数
     */
    public static final String GSE_RUNNING_TASKS = "job.gse.running.tasks";
    /**
     * 异常的 GSE 任务数
     */
    public static final String GSE_EXCEPTION_TASKS_TOTAL = "job.gse.exception.tasks.total";

    /**
     * 任务下发指标前缀
     */
    public static final String EXECUTE_TASK_PREFIX = "job.execute.task";

}
