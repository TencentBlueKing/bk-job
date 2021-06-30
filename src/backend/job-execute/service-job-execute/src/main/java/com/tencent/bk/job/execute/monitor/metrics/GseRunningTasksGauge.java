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

package com.tencent.bk.job.execute.monitor.metrics;

import com.tencent.bk.job.execute.engine.GseTaskManager;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 指标-正在处理的gse任务数
 */
@Component
public class GseRunningTasksGauge {

    @Autowired
    public GseRunningTasksGauge(MeterRegistry meterRegistry, GseTaskManager gseTaskManager,
                                ResultHandleManager resultHandleManager) {
        meterRegistry.gauge(ExecuteMetricNames.GSE_RUNNING_TASKS, Arrays.asList(Tag.of("stage", "send-task"), Tag.of(
            "type", "script")),
            gseTaskManager, GseTaskManager::getRunningScriptTaskCount);
        meterRegistry.gauge(ExecuteMetricNames.GSE_RUNNING_TASKS, Arrays.asList(Tag.of("stage", "send-task"), Tag.of(
            "type", "file")),
            gseTaskManager, GseTaskManager::getRunningFileTaskCount);
        meterRegistry.gauge(ExecuteMetricNames.GSE_RUNNING_TASKS, Arrays.asList(Tag.of("stage", "result-handle"),
            Tag.of("type", "script")),
            resultHandleManager, ResultHandleManager::getRunningScriptTaskCount);
        meterRegistry.gauge(ExecuteMetricNames.GSE_RUNNING_TASKS, Arrays.asList(Tag.of("stage", "result-handle"),
            Tag.of("type", "file")),
            resultHandleManager, ResultHandleManager::getRunningFileTaskCount);
    }
}
