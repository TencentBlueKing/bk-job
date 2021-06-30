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

package com.tencent.bk.job.common.esb.metrics;

import com.tencent.bk.job.common.esb.model.EsbReq;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;


@Aspect
@Slf4j
public class EsbApiTimedAspect {
    private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");
    private final MeterRegistry registry;

    public EsbApiTimedAspect(MeterRegistry registry) {
        log.info("Init EsbApiTimedAspect");
        this.registry = registry;
    }

    public static Tag exception(Throwable exception) {
        if (exception != null) {
            String simpleName = exception.getClass().getSimpleName();
            return Tag.of("exception", org.springframework.util.StringUtils.hasText(simpleName) ? simpleName :
                exception.getClass().getName());
        }
        return EXCEPTION_NONE;
    }

    @Around("execution (@com.tencent.bk.job.common.esb.metrics.EsbApiTimed * *.*(..))")
    public Object timedMethod(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            return pjp.proceed();
        }

        String appCode = "";
        try {
            for (Object arg : args) {
                if (arg instanceof EsbReq) {
                    EsbReq esbReq = (EsbReq) arg;
                    appCode = esbReq.getAppCode();
                    break;
                }
            }
        } catch (Throwable e) {
            log.error("Process EsbReq fail!", e);
            return pjp.proceed();
        }

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        EsbApiTimed timed = method.getAnnotation(EsbApiTimed.class);
        final String metricName = timed.value();
        Tags tags = Tags.of("app_code", StringUtils.isNotBlank(appCode) ? appCode : "None");
        return processWithTimer(pjp, timed, metricName, tags);
    }

    private Object processWithTimer(ProceedingJoinPoint pjp, EsbApiTimed timed, String metricName,
                                    Tags tags) throws Throwable {

        Timer.Sample sample = Timer.start(registry);
        Exception exception = null;
        try {
            return pjp.proceed();
        } catch (Exception ex) {
            exception = ex;
            throw ex;
        } finally {
            tags.and(exception(exception));
            record(timed, metricName, sample, tags);
        }
    }

    private void record(EsbApiTimed timed, String metricName, Timer.Sample sample, Tags tags) {
        try {
            sample.stop(Timer.builder(metricName)
                .description(timed.description().isEmpty() ? null : timed.description())
                .tags(tags)
                .tags(timed.extraTags())
                .publishPercentileHistogram(timed.histogram())
                .publishPercentiles(timed.percentiles().length == 0 ? null : timed.percentiles())
                .register(registry));
        } catch (Exception e) {
            // ignoring on purpose
        }
    }
}
