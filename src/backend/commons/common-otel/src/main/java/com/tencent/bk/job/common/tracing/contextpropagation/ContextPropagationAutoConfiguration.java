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

package com.tencent.bk.job.common.tracing.contextpropagation;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步线程上下文传播自动配置。
 *
 * <p>Spring Boot 3.x / Micrometer Tracing 不再像 Spring Cloud Sleuth 那样自动包装容器中的
 * {@code Executor} bean，所有跨线程的上下文（trace + 业务）都需要通过
 * {@link io.micrometer.context.ContextRegistry} 注册的 {@link ThreadLocalAccessor} 来传播。
 * 本配置完成两件事：</p>
 * <ol>
 *   <li>调用 {@link ContextRegistry#loadThreadLocalAccessors()}，兜底加载 classpath 上通过
 *       {@code META-INF/services} 声明的第三方 accessor（OpenTelemetry / Observation 等）；</li>
 *   <li>把容器中所有 {@link ThreadLocalAccessor} 类型的 Spring bean 收集起来，注册到
 *       全局 {@link ContextRegistry}，供 {@link io.micrometer.context.ContextSnapshotFactory#captureAll()}
 *       捕获使用。</li>
 * </ol>
 *
 * <p>各业务模块只需把自己的 {@code ThreadLocalAccessor} 暴露成 Spring bean，即会被自动注册。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ContextRegistry.class)
@ConditionalOnProperty(
    name = "job.tracing.context-propagation.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ContextPropagationAutoConfiguration {

    @Bean
    public ContextRegistryInitializer contextRegistryInitializer(
        ObjectProvider<ThreadLocalAccessor<?>> accessorsProvider
    ) {
        return new ContextRegistryInitializer(accessorsProvider);
    }

    /**
     * 在 Spring 容器初始化阶段把 ThreadLocalAccessor bean 注册到全局 ContextRegistry，
     * 同时兜底加载第三方通过 SPI 声明的 accessor。
     */
    @Slf4j
    static class ContextRegistryInitializer implements InitializingBean {

        private final ObjectProvider<ThreadLocalAccessor<?>> accessorsProvider;

        ContextRegistryInitializer(ObjectProvider<ThreadLocalAccessor<?>> accessorsProvider) {
            this.accessorsProvider = accessorsProvider;
        }

        @Override
        public void afterPropertiesSet() {
            ContextRegistry registry = ContextRegistry.getInstance();
            registry.loadThreadLocalAccessors();

            AtomicInteger registered = new AtomicInteger();
            accessorsProvider.orderedStream().forEach(accessor -> {
                registry.registerThreadLocalAccessor(accessor);
                registered.incrementAndGet();
                if (log.isDebugEnabled()) {
                    log.debug("Registered ThreadLocalAccessor for context propagation: key={}, type={}",
                        accessor.key(), accessor.getClass().getName());
                }
            });
            log.info("Context propagation initialized, registered {} accessor bean(s) into ContextRegistry",
                registered.get());
        }
    }
}
