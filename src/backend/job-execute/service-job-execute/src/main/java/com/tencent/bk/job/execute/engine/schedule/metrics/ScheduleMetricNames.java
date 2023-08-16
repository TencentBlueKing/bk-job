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

package com.tencent.bk.job.execute.engine.schedule.metrics;

/**
 * 任务调度引擎监控指标
 */
public interface ScheduleMetricNames {
    /**
     * 任务调度引擎 -  worker
     */
    String JOB_SCHEDULE_WORKERS = "job.schedule.workers";
    /**
     * 任务调度引擎 - 异常任务数
     */
    String JOB_SCHEDULE_EXCEPTION_TASKS_TOTAL = "job.schedule.exception.tasks.total";
    /**
     * 任务调度引擎 - 调度延迟的任务数
     */
    String JOB_SCHEDULE_DELAYED_TASKS_TOTAL = "job.schedule.delayed.tasks.total";
    /**
     * 任务调度引擎 - 未被调度的任务数
     */
    String JOB_SCHEDULE_NOT_ALIVE_TASKS_TOTAL = "job.schedule.not.alive.tasks.total";
    /**
     * 任务调度引擎 - 等待被调度引擎处理的任务数量
     */
    String JOB_SCHEDULE_WAITING_TASKS_SIZE = "job.schedule.waiting.tasks.size";


    /**
     * 任务调度引擎 - 任务执行 histogram
     */
    String JOB_SCHEDULE_TASKS_SECONDS = "job.schedule.tasks.seconds";
}
