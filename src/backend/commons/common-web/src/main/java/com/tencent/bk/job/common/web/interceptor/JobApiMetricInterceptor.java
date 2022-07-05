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
import com.tencent.bk.job.common.util.http.JobApiMetricUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Job API指标记录拦截器
 */
@Slf4j
@Component
public class JobApiMetricInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    @Autowired
    public JobApiMetricInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private long getRequestTimeMillis() {
        return System.currentTimeMillis() - JobContextUtil.getStartTime();
    }

    private void recordHttpStatus(HttpServletResponse response) {
        String metricName = JobApiMetricUtil.getJobApiMetricName();
        if (StringUtils.isBlank(metricName)) {
            return;
        }
        int httpStatus = response.getStatus();
        List<Tag> tagList = JobApiMetricUtil.getCurrentMetricTags();
        tagList.add(Tag.of("http_status", "" + httpStatus));
        log.info("metricName={},httpStatus={},tags={}", metricName, httpStatus, tagList);
        meterRegistry.timer(metricName, tagList).record(getRequestTimeMillis(), TimeUnit.MILLISECONDS);
    }

    private void tryToRecordHttpStatus(HttpServletResponse response) {
        try {
            recordHttpStatus(response);
        } catch (Exception e) {
            log.warn("Fail to recordHttpStatus", e);
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        tryToRecordHttpStatus(response);
    }
}
