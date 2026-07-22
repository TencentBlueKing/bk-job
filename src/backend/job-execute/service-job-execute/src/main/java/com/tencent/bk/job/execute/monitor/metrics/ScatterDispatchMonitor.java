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

package com.tencent.bk.job.execute.monitor.metrics;

import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 指标-并行错峰批次下发调度可观测性。
 * <p>
 * 提供两类指标：
 * <ul>
 *     <li>批次下发延迟分布（实际下发时刻 - 计划下发时刻，单位 ms），反映到点触发的准时性；</li>
 *     <li>下发延迟队列积压数量（Gauge），反映待下发批次堆积情况。</li>
 * </ul>
 * 标签保持精简，禁止高基数标签（不使用 stepInstanceId 等）。
 */
@Component
public class ScatterDispatchMonitor {

    private final MeterRegistry meterRegistry;
    /**
     * 批次实际下发相对计划下发时刻的延迟分布(ms)
     */
    private final DistributionSummary dispatchDelaySummary;

    @Autowired
    public ScatterDispatchMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.dispatchDelaySummary = DistributionSummary.builder(ExecuteMetricNames.ROLLING_SCATTER_DISPATCH_DELAY)
            .description("Parallel scatter batch dispatch delay (actual dispatch time - planned dispatch time)")
            .baseUnit("milliseconds")
            .register(meterRegistry);
    }

    /**
     * 记录一次批次下发延迟。
     *
     * @param delayMs 实际下发时刻 - 计划下发时刻(ms)，负值按 0 处理
     */
    public void recordDispatchDelay(long delayMs) {
        dispatchDelaySummary.record(Math.max(0L, delayMs));
    }

    /**
     * 绑定延迟队列积压数量 Gauge。由持有队列的组件在初始化时注册一次。
     *
     * @param queueSizeSupplier 队列当前积压数量供给函数（如 {@code tasksQueue::size}）
     */
    public void registerQueueSizeGauge(Supplier<Number> queueSizeSupplier) {
        Gauge.builder(ExecuteMetricNames.ROLLING_SCATTER_DISPATCH_QUEUE_SIZE, queueSizeSupplier)
            .description("Pending scatter batch dispatch queue size")
            .register(meterRegistry);
    }
}
