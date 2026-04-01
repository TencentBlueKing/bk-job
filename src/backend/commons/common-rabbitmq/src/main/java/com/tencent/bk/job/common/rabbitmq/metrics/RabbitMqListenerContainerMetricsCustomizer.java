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

package com.tencent.bk.job.common.rabbitmq.metrics;

import com.tencent.bk.job.common.mq.metrics.MqConsumerMetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;

/**
 * 为RabbitMQ listener container绑定消费者线程指标
 */
@Slf4j
public class RabbitMqListenerContainerMetricsCustomizer
    implements ListenerContainerCustomizer<MessageListenerContainer> {

    private final MqConsumerMetricsCollector mqConsumerMetricsCollector;

    public RabbitMqListenerContainerMetricsCustomizer(MqConsumerMetricsCollector mqConsumerMetricsCollector) {
        this.mqConsumerMetricsCollector = mqConsumerMetricsCollector;
    }

    @Override
    public void configure(MessageListenerContainer container, String bindingName, String group) {
        log.info(
            "Customize rabbitmq listener container metrics, container: {}, bindingName: {}, group: {}",
            container.getClass().getName(),
            bindingName,
            group
        );
        if (!mqConsumerMetricsCollector.supports(container)) {
            log.info("No rabbitmq consumer metrics collector found for container: {}", container.getClass().getName());
            return;
        }
        mqConsumerMetricsCollector.collect(container, bindingName, group);
        log.info("Bind rabbitmq consumer thread metrics, bindingName: {}, group: {}", bindingName, group);
    }
}
