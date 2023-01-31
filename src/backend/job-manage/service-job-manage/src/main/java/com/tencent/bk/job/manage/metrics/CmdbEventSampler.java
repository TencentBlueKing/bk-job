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

import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 对CMDB事件处理情况进行统计采样，暴露出Metrics
 */
@Component
public class CmdbEventSampler {

    private final MeterRegistry meterRegistry;

    @Autowired
    public CmdbEventSampler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录监听到的事件数量
     *
     * @param eventNum 事件数量
     * @param tags     标签
     */
    public void recordWatchedEvents(int eventNum, Iterable<Tag> tags) {
        Counter.builder(MetricsConstants.NAME_CMDB_EVENT_WATCHED_COUNT)
            .tags(tags)
            .register(meterRegistry)
            .increment(eventNum);
    }

    /**
     * 记录事件从被监听到被处理完成之间的耗时
     *
     * @param timeConsumingMillis 事件处理耗时毫秒数
     * @param tags                标签
     */
    public void recordEventHandleTimeConsuming(long timeConsumingMillis, Iterable<Tag> tags) {
        Timer.builder(MetricsConstants.NAME_CMDB_EVENT_HANDLE_TIME)
            .description("CMDB Event Handle Time(From watched to handled)")
            .tags(tags)
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofMinutes(5L))
            .register(meterRegistry)
            .record(timeConsumingMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 注册事件队列，用于监控事件队列中的事件数量
     *
     * @param eventQueue 事件队列
     * @param tags       标签
     */
    public <T> void registerEventQueueToGauge(BlockingQueue<ResourceEvent<T>> eventQueue, Iterable<Tag> tags) {
        meterRegistry.gauge(
            MetricsConstants.NAME_CMDB_EVENT_QUEUE_SIZE,
            tags,
            eventQueue,
            Collection::size
        );
    }

}
