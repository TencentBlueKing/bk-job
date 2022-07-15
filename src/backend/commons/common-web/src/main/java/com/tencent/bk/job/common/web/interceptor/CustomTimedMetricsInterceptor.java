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

package com.tencent.bk.job.common.web.interceptor;

import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.common.web.metrics.CustomTimedTagsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Job自定义Timer指标数据拦截处理器
 */
@Slf4j
public class CustomTimedMetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry registry;
    private final CustomTimedTagsProvider customTimedTagsProvider;

    public CustomTimedMetricsInterceptor(CustomTimedTagsProvider customTimedTagsProvider, MeterRegistry registry) {
        this.customTimedTagsProvider = customTimedTagsProvider;
        this.registry = registry;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        // 注解提取
        CustomTimed customTimedAnnotation = method.getAnnotation(CustomTimed.class);
        if (customTimedAnnotation == null) {
            return;
        }
        try {
            recordCustomTimedMetrics(customTimedAnnotation, request, response, handlerMethod, ex);
        } catch (Exception e) {
            log.warn("Fail to recordCustomTimedMetrics", e);
        }
    }

    private void recordCustomTimedMetrics(CustomTimed customTimedAnnotation,
                                          HttpServletRequest request,
                                          HttpServletResponse response,
                                          HandlerMethod handlerMethod,
                                          Exception ex) {
        final String metricName = customTimedAnnotation.metricName();
        // 标签获取
        Iterable<Tag> tags = customTimedTagsProvider.getTags(metricName, request, response, handlerMethod, ex);
        // 记录指标数据
        record(customTimedAnnotation, metricName, tags);
    }

    private long getRequestDurationMillis() {
        return System.currentTimeMillis() - JobContextUtil.getStartTime();
    }

    private void record(CustomTimed customTimedAnnotation, String metricName, Iterable<Tag> tags) {
        try {
            Timer.builder(metricName)
                .description(customTimedAnnotation.description().isEmpty() ?
                    null : customTimedAnnotation.description())
                .tags(tags)
                .tags(customTimedAnnotation.extraTags())
                .publishPercentileHistogram(customTimedAnnotation.histogram())
                .publishPercentiles(customTimedAnnotation.percentiles().length == 0 ? null :
                    customTimedAnnotation.percentiles())
                .minimumExpectedValue(Duration.ofMillis(customTimedAnnotation.minExpectedMillis()))
                .maximumExpectedValue(Duration.ofMillis(customTimedAnnotation.maxExpectedMillis()))
                .register(registry)
                .record(getRequestDurationMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Fail to record custom timer metrics", e);
        }
    }
}
