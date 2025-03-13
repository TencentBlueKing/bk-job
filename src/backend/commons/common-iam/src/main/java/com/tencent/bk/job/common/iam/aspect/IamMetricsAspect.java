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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 记录AuthHelper.isAllow方法调用结果统计数据
 */
@Slf4j
@Aspect
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
