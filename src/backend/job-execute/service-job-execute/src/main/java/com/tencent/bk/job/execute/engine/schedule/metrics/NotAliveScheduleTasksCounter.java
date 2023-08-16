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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.Collections;
import java.util.List;

/**
 * 指标-调度引擎未存活任务数量
 */
public class NotAliveScheduleTasksCounter {
    /**
     * 未存活任务数量Counter
     */
    private final Counter notAliveScheduleTasksCounter;

    public NotAliveScheduleTasksCounter(MeterRegistry meterRegistry, String schedulerName) {
        List<Tag> metricTags = Collections.singletonList(Tag.of("scheduler", schedulerName));
        this.notAliveScheduleTasksCounter = meterRegistry.counter(
            ScheduleMetricNames.JOB_SCHEDULE_NOT_ALIVE_TASKS_TOTAL, metricTags);
    }

    /**
     * 计数+1
     */
    public void increment() {
        this.notAliveScheduleTasksCounter.increment();
    }

    /**
     * 计数+count
     */
    public void increment(int count) {
        this.notAliveScheduleTasksCounter.increment(count);
    }
}
