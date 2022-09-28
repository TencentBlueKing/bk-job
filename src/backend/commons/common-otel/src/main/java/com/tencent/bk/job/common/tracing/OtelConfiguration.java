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

package com.tencent.bk.job.common.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.autoconfig.otel.SpanProcessorProvider;
import org.springframework.cloud.sleuth.otel.bridge.SpanExporterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Configuration(proxyBeanMethods = false)
class OtelConfiguration {

    @Value("${spring.sleuth.otel.exporter.enabled:false}")
    private boolean exporterEnabled;

    @Value("${spring.sleuth.otel.resource.bkDataToken:}")
    private String bkDataToken;

    @Bean
    Supplier<Resource> otelBkDataTokenResourceProvider() {
        return this::buildResource;
    }

    Resource buildResource() {
        AttributesBuilder attributes = Attributes.builder();
        attributes.put("bk.data.token", bkDataToken);
        return Resource.create(attributes.build(), ResourceAttributes.SCHEMA_URL);
    }

    @Bean
    SdkTracerProvider otelTracerProvider(SpanLimits spanLimits,
                                         ObjectProvider<List<SpanProcessor>> spanProcessors,
                                         SpanExporterCustomizer spanExporterCustomizer,
                                         ObjectProvider<List<SpanExporter>> spanExporters,
                                         Sampler sampler,
                                         Resource resource,
                                         SpanProcessorProvider spanProcessorProvider) {
        log.debug("exporterEnabled={},bkDataToken={}", exporterEnabled, bkDataToken);
        SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder().setResource(resource)
            .setSampler(sampler).setSpanLimits(spanLimits);
        List<SpanProcessor> processors = spanProcessors.getIfAvailable(ArrayList::new);
        if (exporterEnabled) {
            processors.addAll(spanExporters.getIfAvailable(ArrayList::new).stream()
                .map(e -> spanProcessorProvider.toSpanProcessor(spanExporterCustomizer.customize(e)))
                .collect(Collectors.toList()));
        }
        processors.forEach(sdkTracerProviderBuilder::addSpanProcessor);
        return sdkTracerProviderBuilder.build();
    }

    @Bean
    OpenTelemetry otel(SdkTracerProvider tracerProvider, ContextPropagators contextPropagators) {
        OpenTelemetry openTelemetry =
            OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).setPropagators(contextPropagators).build();
        GlobalOpenTelemetry.set(openTelemetry);
        log.info("GlobalOpenTelemetry has been set");
        return openTelemetry;
    }
}
