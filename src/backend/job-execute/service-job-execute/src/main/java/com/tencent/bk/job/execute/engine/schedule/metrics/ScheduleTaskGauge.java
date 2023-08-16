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

import com.tencent.bk.job.execute.engine.schedule.ScheduleTaskManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleTaskGauge {

    public ScheduleTaskGauge(MeterRegistry meterRegistry,
                             ScheduleTaskManager scheduleTaskManager) {
        registerSchedulerWaitingTasksSizeGauge(meterRegistry, scheduleTaskManager);
        registerSchedulerWorkerGauge(meterRegistry, scheduleTaskManager);
    }

    private void registerSchedulerWaitingTasksSizeGauge(MeterRegistry meterRegistry,
                                                        ScheduleTaskManager scheduleTaskManager) {
        List<Tag> tags = Collections.singletonList(Tag.of("scheduler", scheduleTaskManager.getSchedulerName()));
        meterRegistry.gauge(ScheduleMetricNames.JOB_SCHEDULE_WAITING_TASKS_SIZE, tags,
            scheduleTaskManager, ScheduleTaskManager::getResultHandleWaitingScheduleTasks);

    }

    private void registerSchedulerWorkerGauge(MeterRegistry meterRegistry,
                                              ScheduleTaskManager scheduleTaskManager) {
        List<Tag> busyWorkerMetricsTag = new ArrayList<>(2);
        busyWorkerMetricsTag.add(Tag.of("scheduler", scheduleTaskManager.getSchedulerName()));
        busyWorkerMetricsTag.add(Tag.of("status", "busy"));
        meterRegistry.gauge(
            ScheduleMetricNames.JOB_SCHEDULE_WORKERS,
            busyWorkerMetricsTag,
            scheduleTaskManager,
            ScheduleTaskManager::getResultHandleBusyThreads);

        List<Tag> idleWorkerMetricsTag = new ArrayList<>(2);
        idleWorkerMetricsTag.add(Tag.of("scheduler", scheduleTaskManager.getSchedulerName()));
        idleWorkerMetricsTag.add(Tag.of("status", "idle"));
        meterRegistry.gauge(
            ScheduleMetricNames.JOB_SCHEDULE_WORKERS,
            idleWorkerMetricsTag,
            scheduleTaskManager,
            ScheduleTaskManager::getResultHandleIdleThreads);
    }

}
