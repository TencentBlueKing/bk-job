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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ScatterDispatchMonitor} 单元测试：验证 Issue #4368 #1 调度延迟埋点与队列积压 Gauge 可用。
 */
class ScatterDispatchMonitorTest {

    private SimpleMeterRegistry registry;
    private ScatterDispatchMonitor monitor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        monitor = new ScatterDispatchMonitor(registry);
    }

    @Test
    @DisplayName("记录批次下发延迟：DistributionSummary累计计数与总量正确，负值按0处理")
    void recordDispatchDelay() {
        monitor.recordDispatchDelay(500L);
        monitor.recordDispatchDelay(1500L);
        // 负值(实际早于计划，极少见)按0处理，避免污染分布
        monitor.recordDispatchDelay(-100L);

        DistributionSummary summary = registry.find(ExecuteMetricNames.ROLLING_SCATTER_DISPATCH_DELAY).summary();
        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(3);
        assertThat(summary.totalAmount()).isEqualTo(2000.0);
        assertThat(summary.max()).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("队列积压Gauge：绑定供给函数后反映实时积压数量")
    void registerQueueSizeGauge() {
        AtomicInteger queueSize = new AtomicInteger(0);
        monitor.registerQueueSizeGauge(queueSize::get);

        Gauge gauge = registry.find(ExecuteMetricNames.ROLLING_SCATTER_DISPATCH_QUEUE_SIZE).gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);

        queueSize.set(7);
        assertThat(gauge.value()).isEqualTo(7.0);
    }
}
