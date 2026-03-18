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

package com.tencent.bk.job.common.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.function.Supplier;

/**
 * OTel 配置（Spring Boot 3.x + Micrometer Tracing）
 *
 * <p>在跑测试用例启动时，会启动spring容器，会多次初始化OtelConfiguration。
 * 通过 @Profile("!test") 排除测试环境避免 GlobalOpenTelemetry 重复设置问题。</p>
 *
 * <p>Spring Boot 3.x 通过 micrometer-tracing-bridge-otel 自动配置 OTel SDK，
 * 本配置类仅提供 BK-JOB 定制的 Resource（bkDataToken）和条件化的 SpanProcessor。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@Profile("!test")
class JobOtelAutoConfiguration {

    @Value("${job.tracing.exporter.enabled:false}")
    private boolean exporterEnabled;

    @Value("${job.tracing.bk-data-token:}")
    private String bkDataToken;

    @Bean
    Supplier<Resource> otelBkDataTokenResourceProvider() {
        return this::buildResource;
    }

    @Bean
    Resource otelBkDataTokenResource() {
        return buildResource();
    }

    private Resource buildResource() {
        AttributesBuilder attributes = Attributes.builder();
        attributes.put("bk.data.token", bkDataToken);
        return Resource.create(attributes.build());
    }

    @Bean
    @ConditionalOnProperty(name = "job.tracing.exporter.enabled", havingValue = "true")
    SpanProcessor otelBatchSpanProcessor(ObjectProvider<List<SpanExporter>> spanExporters) {
        log.info("OTel exporter enabled, bkDataToken={}", bkDataToken);
        List<SpanExporter> exporters = spanExporters.getIfAvailable(List::of);
        if (exporters.isEmpty()) {
            log.warn("No SpanExporter available, skip batch span processor creation");
            return SpanProcessor.composite();
        }
        SpanExporter compositeExporter = SpanExporter.composite(exporters);
        return BatchSpanProcessor.builder(compositeExporter).build();
    }
}
