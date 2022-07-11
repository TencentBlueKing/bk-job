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

package com.tencent.bk.job.common.web.metrics;

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;


@Aspect
@Slf4j
@Component
public class RecordTaskStartAspect {
    private final MeterRegistry registry;

    @Autowired
    public RecordTaskStartAspect(MeterRegistry registry) {
        this.registry = registry;
        log.info("Init RecordTaskStartAspect");
    }

    @Around("execution (@com.tencent.bk.job.common.web.metrics.RecordTaskStart * *.*(..))")
    public Object recordHttpStatusMethod(ProceedingJoinPoint pjp) throws Throwable {
        return processAndRecordTaskStartMetrics(pjp);
    }

    private Object processAndRecordTaskStartMetrics(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RecordTaskStart recordTaskStartAnnotation = method.getAnnotation(RecordTaskStart.class);
        final String metricName = recordTaskStartAnnotation.value();
        Tags tags = Tags.of(recordTaskStartAnnotation.extraTags());
        try {
            Object returnObj = pjp.proceed();
            tags = tags.and(analysisTagsFromReturnObj(returnObj));
            return returnObj;
        } catch (Throwable t) {
            tags = tags.and(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_FAILED);
            throw t;
        } finally {
            record(recordTaskStartAnnotation, metricName, sample, tags);
        }
    }

    @SuppressWarnings("rawtypes")
    private Tag analysisTagsFromReturnObj(Object returnObj) {
        try {
            if (returnObj instanceof EsbResp) {
                EsbResp esbResp = (EsbResp) returnObj;
                return analysisTagsFromEsbResp(esbResp);
            } else if (returnObj instanceof InternalResponse) {
                InternalResponse internalResp = (InternalResponse) returnObj;
                return analysisTagsFromInternalResp(internalResp);
            } else if (returnObj instanceof Response) {
                Response webResp = (Response) returnObj;
                return analysisTagsFromWebResp(webResp);
            } else {
                log.warn("Unknown resp type:{}, please contact developer to check", returnObj.getClass().getName());
            }
        } catch (Throwable t) {
            log.warn("Fail to analysisTagsFromReturnObj", t);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Tag analysisTagsFromEsbResp(EsbResp esbResp) {
        if (esbResp.getResult()) {
            return Tag.of(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_SUCCESS);
        } else {
            return Tag.of(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_FAILED);
        }
    }

    @SuppressWarnings("rawtypes")
    private Tag analysisTagsFromInternalResp(InternalResponse internalResp) {
        if (internalResp.isSuccess()) {
            return Tag.of(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_SUCCESS);
        } else {
            return Tag.of(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_FAILED);
        }
    }

    @SuppressWarnings("rawtypes")
    private Tag analysisTagsFromWebResp(Response webResp) {
        if (webResp.isSuccess()) {
            return Tag.of(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_SUCCESS);
        } else {
            return Tag.of(CommonMetricTags.KEY_START_STATUS, CommonMetricTags.VALUE_START_STATUS_FAILED);
        }
    }

    private void record(RecordTaskStart recordTaskStartAnnotation, String metricName, Timer.Sample sample, Tags tags) {
        try {
            sample.stop(Timer.builder(metricName)
                .description(recordTaskStartAnnotation.description().isEmpty() ?
                    null : recordTaskStartAnnotation.description())
                .tags(tags)
                .tags(recordTaskStartAnnotation.extraTags())
                .publishPercentileHistogram(recordTaskStartAnnotation.histogram())
                .publishPercentiles(recordTaskStartAnnotation.percentiles().length == 0 ? null :
                    recordTaskStartAnnotation.percentiles())
                .minimumExpectedValue(Duration.ofMillis(10))
                .maximumExpectedValue(Duration.ofSeconds(60))
                .register(registry));
        } catch (Exception e) {
            log.warn("Fail to record taskStart", e);
        }
    }
}
