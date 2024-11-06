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

package com.tencent.bk.job.common.iam.aspect;

import com.tencent.bk.job.common.iam.metrics.MetricsConstants;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 记录AuthHelper.isAllow方法调用结果统计数据
 */
@Slf4j
@Aspect
@Component
public class IamMetricsAspect {

    private final MeterRegistry meterRegistry;

    public IamMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Pointcut("within(com.tencent.bk.sdk.iam.helper.AuthHelper) " +
        "&& execution (* com.tencent.bk.sdk.iam.helper.AuthHelper.isAllowed(..))")
    public void processIsAllowedAction() {
    }

    @Around("processIsAllowedAction()")
    public Object recordMetricsDuringAuth(ProceedingJoinPoint pjp) throws Throwable {
        String action = MetricsConstants.TAG_VALUE_ACTION_NONE;
        String result = MetricsConstants.TAG_VALUE_RESULT_TRUE;
        Long startTimeMills = System.currentTimeMillis();
        try {
            Object[] args = pjp.getArgs();
            if (args.length >= 2 && args[1] instanceof String) {
                action = (String) args[1];
            }
            Object methodResult = pjp.proceed();
            if (methodResult instanceof Boolean) {
                if (!(Boolean) methodResult) {
                    result = MetricsConstants.TAG_VALUE_RESULT_FALSE;
                }
            }
            return methodResult;
        } catch (Throwable t) {
            result = MetricsConstants.TAG_VALUE_RESULT_ERROR;
            throw t;
        } finally {
            tryToRecordMetrics(action, result, startTimeMills);
        }
    }

    private void tryToRecordMetrics(String action, String status, Long startTimeMills) {
        try {
            recordMetrics(action, status, startTimeMills);
        } catch (Throwable t) {
            log.warn("Fail to recordMetrics", t);
        }
    }

    private void recordMetrics(String action, String status, Long startTimeMills) {
        Tags tags = Tags.of(
            Tag.of(MetricsConstants.TAG_KEY_ACTION, action),
            Tag.of(MetricsConstants.TAG_KEY_STATUS, status)
        );
        Timer.builder(MetricsConstants.NAME_AUTH_HELPER_IS_ALLOW)
            .tags(tags)
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofSeconds(30L))
            .register(meterRegistry)
            .record(System.currentTimeMillis() - startTimeMills, TimeUnit.MILLISECONDS);
    }
}
