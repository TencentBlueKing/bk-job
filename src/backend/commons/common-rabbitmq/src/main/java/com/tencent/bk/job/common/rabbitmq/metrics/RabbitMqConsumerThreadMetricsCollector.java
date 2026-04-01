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
import com.tencent.bk.job.common.mq.metrics.MqMetricsConstants;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ消费者线程指标采集实现
 */
@Slf4j
public class RabbitMqConsumerThreadMetricsCollector implements MqConsumerMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Set<String> registeredMetricKeys = ConcurrentHashMap.newKeySet();

    public RabbitMqConsumerThreadMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 判断是否为RabbitMQ listener container
     */
    @Override
    public boolean supports(Object container) {
        return container instanceof SimpleMessageListenerContainer
            || container instanceof DirectMessageListenerContainer;
    }

    /**
     * 注册RabbitMQ消费者线程活跃状态指标
     *
     * @param container listener container
     * @param bindingName binding名称
     * @param group 消费组
     */
    @Override
    public void collect(Object container, String bindingName, String group) {
        AbstractMessageListenerContainer listenerContainer = (AbstractMessageListenerContainer) container;
        String groupTagValue = group == null ? "" : group;
        String metricKey = bindingName + ':' + groupTagValue;
        if (!registeredMetricKeys.add(metricKey)) {
            if (log.isDebugEnabled()) {
                log.debug("Skip rabbitmq consumer thread metrics because metric key already registered, " +
                    "bindingName: {}, group: {}", bindingName, group);
            }
            return;
        }

        log.info("Collect rabbitmq consumer thread metrics, container: {}, bindingName: {}, group: {}",
            container.getClass().getName(), bindingName, group);

        Iterable<Tag> tags = Tags.of(
            Tag.of(MqMetricsConstants.TAG_KEY_GROUP, groupTagValue),
            Tag.of(MqMetricsConstants.TAG_KEY_BINDING, bindingName),
            Tag.of(MqMetricsConstants.TAG_KEY_MESSAGE_NAME, bindingName)
        );
        Gauge.builder(MqMetricsConstants.NAME_JOB_MQ_CONSUMER_ACTIVE_COUNT,
                listenerContainer,
                this::getActiveConsumerCount)
            .tags(tags)
            .register(meterRegistry);
        Gauge.builder(MqMetricsConstants.NAME_JOB_MQ_CONSUMER_CONFIGURED_COUNT,
                listenerContainer,
                this::getConfiguredConsumerCount)
            .tags(tags)
            .register(meterRegistry);
        Gauge.builder(MqMetricsConstants.NAME_JOB_MQ_CONSUMER_MAX_COUNT,
                listenerContainer,
                this::getMaxConsumerCount)
            .tags(tags)
            .register(meterRegistry);
        log.info("Register rabbitmq consumer thread metrics, bindingName: {}, group: {}", bindingName, group);
    }

    /**
     * 获取当前活跃消费线程数
     *
     * @param container listener container
     * @return 当前活跃消费线程数
     */
    private double getActiveConsumerCount(AbstractMessageListenerContainer container) {
        if (container instanceof SimpleMessageListenerContainer simpleContainer) {
            return simpleContainer.getActiveConsumerCount();
        }
        return getNumberFieldValue(container, "consumers", true);
    }

    /**
     * 获取当前配置的消费线程数
     *
     * @param container listener container
     * @return 当前配置的消费线程数
     */
    private double getConfiguredConsumerCount(AbstractMessageListenerContainer container) {
        if (container instanceof SimpleMessageListenerContainer) {
            return getNumberFieldValue(container, "concurrentConsumers", false);
        }
        if (container instanceof DirectMessageListenerContainer directContainer) {
            double consumersPerQueue = getNumberFieldValue(directContainer, "consumersPerQueue", false);
            return consumersPerQueue * directContainer.getQueueNames().length;
        }
        return 0D;
    }

    /**
     * 获取当前最大消费线程数
     *
     * @param container listener container
     * @return 当前最大消费线程数
     */
    private double getMaxConsumerCount(AbstractMessageListenerContainer container) {
        if (container instanceof SimpleMessageListenerContainer) {
            double maxConcurrentConsumers = getNumberFieldValue(container, "maxConcurrentConsumers", false);
            if (maxConcurrentConsumers > 0) {
                return maxConcurrentConsumers;
            }
            return getConfiguredConsumerCount(container);
        }
        return getConfiguredConsumerCount(container);
    }

    /**
     * 读取listener container中的数值字段
     * <p>
     * Gauge是动态采样的，因此这里只是注册字段读取逻辑，真正采样时会读取container当前状态
     * 当前依赖版本下，相关配置值并未以统一父类getter暴露，因此对Rabbit的两种container做显式兼容
     *
     * @param target listener container
     * @param fieldName 字段名
     * @param collectionSize 是否返回集合大小
     * @return 当前字段值
     */
    private double getNumberFieldValue(Object target, String fieldName, boolean collectionSize) {
        try {
            Field field = ReflectionUtils.findField(target.getClass(), fieldName);
            if (field == null) {
                return 0D;
            }
            ReflectionUtils.makeAccessible(field);
            Object result = field.get(target);
            if (collectionSize && result instanceof java.util.Collection) {
                return ((java.util.Collection<?>) result).size();
            }
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (IllegalAccessException e) {
            log.debug("Get rabbitmq listener container metric value failed, fieldName: {}", fieldName, e);
        }
        return 0D;
    }
}
