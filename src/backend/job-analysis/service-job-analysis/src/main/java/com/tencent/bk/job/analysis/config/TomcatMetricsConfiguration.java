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

package com.tencent.bk.job.analysis.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Slf4j
@Configuration(value = "jobAnalysisTomcatMetricsConfig")
public class TomcatMetricsConfiguration {

    @Bean
    public TomcatConnectorCustomizer tomcatThreadPoolCustomizer(MeterRegistry registry) {
        return connector -> {
            Executor executor = connector.getProtocolHandler().getExecutor();
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor threadExecutor = (ThreadPoolExecutor) executor;
                registry.gauge("tomcat.threads.max", threadExecutor, ThreadPoolExecutor::getMaximumPoolSize);
                registry.gauge("tomcat.threads.current", threadExecutor, ThreadPoolExecutor::getPoolSize);
                registry.gauge("tomcat.threads.busy", threadExecutor, ThreadPoolExecutor::getActiveCount);
            } else {
                log.warn("Unknown executor type: {}, ignore tomcat executor metrics", executor.getClass().getName());
            }
        };
    }
}
