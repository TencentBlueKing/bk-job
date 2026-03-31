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

package com.tencent.bk.job.common.mq.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;

import java.util.List;

/**
 * 为listener container绑定MQ消费者线程指标
 */
@Slf4j
public class MqListenerContainerMetricsCustomizer implements ListenerContainerCustomizer<Object> {

    private final List<MqConsumerMetricsCollector> mqConsumerMetricsCollectors;

    public MqListenerContainerMetricsCustomizer(List<MqConsumerMetricsCollector> mqConsumerMetricsCollectors) {
        this.mqConsumerMetricsCollectors = mqConsumerMetricsCollectors;
    }

    @Override
    public void configure(Object container, String bindingName, String group) {
        for (MqConsumerMetricsCollector collector : mqConsumerMetricsCollectors) {
            if (collector.supports(container)) {
                collector.collect(container, bindingName, group);
                log.info("Bind mq consumer thread metrics, bindingName: {}, group: {}", bindingName, group);
                return;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("No mq consumer metrics collector found for container: {}", container.getClass().getName());
        }
    }
}
