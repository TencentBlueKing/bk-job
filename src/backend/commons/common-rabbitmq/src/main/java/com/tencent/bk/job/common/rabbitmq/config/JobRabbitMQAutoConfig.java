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

package com.tencent.bk.job.common.rabbitmq.config;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import com.tencent.bk.job.common.mq.metrics.MqConsumerMetricsCollector;
import com.tencent.bk.job.common.mq.metrics.MqListenerContainerMetricsCustomizer;
import com.tencent.bk.job.common.mq.metrics.MqMetricsProperties;
import com.tencent.bk.job.common.mq.metrics.MqSendTimeChannelInterceptor;
import com.tencent.bk.job.common.rabbitmq.metrics.RabbitMqConsumerThreadMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionFactoryBeanConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import java.util.List;

@Slf4j
@Configuration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@EnableConfigurationProperties({RabbitProperties.class, MqMetricsProperties.class})
public class JobRabbitMQAutoConfig {

    @Bean
    RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer(
        RabbitProperties properties,
        ObjectProvider<CredentialsProvider> credentialsProvider,
        ObjectProvider<CredentialsRefreshService> credentialsRefreshService
    ) {
        RabbitConnectionFactoryBeanConfigurer configurer = new RabbitConnectionFactoryBeanConfigurer(
            // 从系统文件读取truststore与keystore
            new FileSystemResourceLoader(),
            properties
        );
        configurer.setCredentialsProvider(credentialsProvider.getIfUnique());
        configurer.setCredentialsRefreshService(credentialsRefreshService.getIfUnique());
        log.info("rabbitConnectionFactoryBeanConfigurer init");
        return configurer;
    }

    /**
     * 注册MQ消息发送时间拦截器
     */
    @Bean
    MqSendTimeChannelInterceptor mqSendTimeChannelInterceptor() {
        return new MqSendTimeChannelInterceptor();
    }

    /**
     * 注册RabbitMQ消费者线程指标收集器
     */
    @Bean
    MqConsumerMetricsCollector mqConsumerMetricsCollector(MeterRegistry meterRegistry) {
        return new RabbitMqConsumerThreadMetricsCollector(meterRegistry);
    }

    /**
     * 注册MQ listener container监控指标收集器
     */
    @Bean
    ListenerContainerCustomizer<Object> mqListenerContainerMetricsCustomizer(
        List<MqConsumerMetricsCollector> mqConsumerMetricsCollectors
    ) {
        return new MqListenerContainerMetricsCustomizer(mqConsumerMetricsCollectors);
    }
}
