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

package com.tencent.bk.job.execute.colddata;

import lombok.extern.slf4j.Slf4j;


//@Aspect
@Slf4j
public class JobInstanceRecordQueryAspect {
//    private final MeterRegistry registry;
//
//    public JobInstanceRecordQueryAspect(MeterRegistry registry) {
//        log.info("Init JobInstanceRecordQueryAspect");
//        this.registry = registry;
//    }
//
//    @Around("within(com.tencent.bk.job..*) && execution (@com.tencent.bk.job.execute.colddata.JobInstanceRecordQuery * *.*(..))")
//    public Object process(ProceedingJoinPoint pjp) throws Throwable {
//        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//        HttpServletRequest request = null;
//        if (requestAttributes != null) {
//            request = ((ServletRequestAttributes)requestAttributes).getRequest();
//        }
//        if (request == null) {
//            return pjp.proceed();
//        }
//
//        JobContextUtil.set
//
//        String appCode = request.getHeader(JobCommonHeaders.APP_CODE);
//        Tags tags = Tags.of("app_code", StringUtils.isNotBlank(appCode) ? appCode : "None");
//
//        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
//        JobInstanceRecordQuery timed = method.getAnnotation(JobInstanceRecordQuery.class);
//        final String metricName = timed.value();
//        return processWithTimer(pjp, timed, metricName, tags);
//    }
//
//    private Object processWithTimer(ProceedingJoinPoint pjp, JobInstanceRecordQuery timed, String metricName,
//                                    Tags tags) throws Throwable {
//
//        Timer.Sample sample = Timer.start(registry);
//        Exception exception = null;
//        try {
//            return pjp.proceed();
//        } catch (Exception ex) {
//            exception = ex;
//            throw ex;
//        } finally {
//            tags.and(exception(exception));
//            record(timed, metricName, sample, tags);
//        }
//    }
//
//    private void record(JobInstanceRecordQuery timed, String metricName, Timer.Sample sample, Tags tags) {
//        try {
//            sample.stop(Timer.builder(metricName)
//                .description(timed.description().isEmpty() ? null : timed.description())
//                .tags(tags)
//                .tags(timed.extraTags())
//                .publishPercentileHistogram(timed.histogram())
//                .publishPercentiles(timed.percentiles().length == 0 ? null : timed.percentiles())
//                .register(registry));
//        } catch (Exception e) {
//            // ignoring on purpose
//        }
//    }
}
