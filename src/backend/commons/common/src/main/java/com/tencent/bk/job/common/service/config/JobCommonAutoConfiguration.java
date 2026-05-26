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

package com.tencent.bk.job.common.service.config;

import com.tencent.bk.job.common.VersionInfoLogApplicationRunner;
import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import com.tencent.bk.job.common.config.BkConfig;
import com.tencent.bk.job.common.context.JobContextThreadLocalAccessor;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import io.micrometer.context.ThreadLocalAccessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Import({JobCommonConfig.class, BkConfig.class})
public class JobCommonAutoConfiguration {
    @Bean("applicationContextRegister")
    @Lazy(false)
    public ApplicationContextRegister applicationContextRegister() {
        return new ApplicationContextRegister();
    }

    @Bean
    HttpConfigSetter httpConfigSetter(@Autowired MeterRegistry meterRegistry) {
        HttpHelperFactory.setMeterRegistry(meterRegistry);
        log.info("meterRegistry for HttpHelperFactory init");
        return new HttpConfigSetter();
    }

    static class HttpConfigSetter {
        HttpConfigSetter() {
        }
    }

    @Value("${spring.application.name:bk-job}")
    private String serviceName;

    @Bean
    public VersionInfoLogApplicationRunner versionInfoLogApplicationRunner(BuildProperties buildProperties) {
        return new VersionInfoLogApplicationRunner(serviceName, buildProperties);
    }

    /**
     * 把 {@link JobContext} 接入 Micrometer Context Propagation 体系，
     * 由 ContextPropagationAutoConfiguration 统一注册到 ContextRegistry，
     * 进而被 {@code WatchableThreadPoolExecutor} 等线程池在 captureAll() 时一并捕获、跨线程恢复。
     */
    @Bean
    public ThreadLocalAccessor<?> jobContextThreadLocalAccessor() {
        return new JobContextThreadLocalAccessor();
    }

    /**
     * 把容器中的 {@link Tracer} 注入 {@link WatchableThreadPoolExecutor} 静态字段，
     * 使其在工作线程上为每个任务创建 child span（类比 MQ producer/consumer 跨边界开 span），
     * 让异步任务在 trace 树中拥有独立的 spanId 与耗时统计。
     *
     * <p>若容器中没有 Tracer（如部分测试场景），任务仍能正常执行，仅跳过 span 创建。</p>
     */
    @Bean
    public WatchableThreadPoolExecutorTracerInitializer watchableThreadPoolExecutorTracerInitializer(
        ObjectProvider<Tracer> tracerProvider
    ) {
        return new WatchableThreadPoolExecutorTracerInitializer(tracerProvider);
    }

    static class WatchableThreadPoolExecutorTracerInitializer implements InitializingBean {

        private final ObjectProvider<Tracer> tracerProvider;

        WatchableThreadPoolExecutorTracerInitializer(ObjectProvider<Tracer> tracerProvider) {
            this.tracerProvider = tracerProvider;
        }

        @Override
        public void afterPropertiesSet() {
            Tracer tracer = tracerProvider.getIfAvailable();
            if (tracer != null) {
                WatchableThreadPoolExecutor.setTracer(tracer);
                log.info("Tracer injected to WatchableThreadPoolExecutor for async child span creation");
            } else {
                log.info("No Tracer bean available, WatchableThreadPoolExecutor will skip child span creation");
            }
        }
    }
}
