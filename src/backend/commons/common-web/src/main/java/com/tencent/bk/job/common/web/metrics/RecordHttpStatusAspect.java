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

import com.tencent.bk.job.common.util.http.JobApiMetricUtil;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


@Aspect
@Slf4j
@Component
public class RecordHttpStatusAspect {

    public RecordHttpStatusAspect() {
        log.info("Init RecordHttpStatusAspect");
    }

    private void recordJobApiMetricNameAndInitTags(ProceedingJoinPoint pjp) {
        try {
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            RecordHttpStatus recordHttpStatusAnnotation = method.getAnnotation(RecordHttpStatus.class);
            final String metricName = recordHttpStatusAnnotation.value();
            JobApiMetricUtil.setJobApiMetricName(metricName);
            JobApiMetricUtil.addTagForCurrentMetric(Tags.of(recordHttpStatusAnnotation.extraTags()));
        } catch (Exception e) {
            log.warn("Fail to record jobApiMetricName or add initialTags", e);
        }
    }

    @Around("execution (@com.tencent.bk.job.common.web.metrics.RecordHttpStatus * *.*(..))")
    public Object recordHttpStatusMethod(ProceedingJoinPoint pjp) throws Throwable {
        recordJobApiMetricNameAndInitTags(pjp);
        return pjp.proceed();
    }
}
