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

package com.tencent.bk.job.crontab.metrics;

import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.crontab.constant.CronConstants;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 度量Quartz调度过程中的各种指标
 */
@Slf4j
@Service
public class ScheduleMeasureService {

    private final Tag APP_ID_NONE_TAG = Tag.of(CommonMetricTags.KEY_APP_ID, "None");

    private final MeterRegistry registry;

    @Autowired
    public ScheduleMeasureService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCronScheduleDelay(String name, JobExecutionContext context) {
        try {
            recordCronScheduleDelayIndeed(name, context);
        } catch (Exception e) {
            log.warn("Fail to recordCronScheduleDelay", e);
        }
    }

    public void recordCronScheduleDelayIndeed(String name, JobExecutionContext context) {
        Tag tag = Tag.of(CommonMetricTags.KEY_NAME, name);
        Tags tags = Tags.of(tag);
        tags = tags.and(parseTagsFromJobExecutionContext(context));
        record(context.getFireTime().getTime() - context.getScheduledFireTime().getTime(), tags);
    }

    private Tags parseTagsFromJobExecutionContext(JobExecutionContext context) {
        String appIdStr = (String) context.getMergedJobDataMap().get(CronConstants.JOB_DATA_KEY_APP_ID_STR);
        if (StringUtils.isBlank(appIdStr)) {
            return Tags.of(APP_ID_NONE_TAG);
        } else {
            return Tags.of(Tag.of(CommonMetricTags.KEY_APP_ID, appIdStr));
        }
    }

    private void record(long delayMillis, Tags tags) {
        Timer.builder(CronMetricsConstants.NAME_JOB_CRON_SCHEDULE_DELAY)
            .description("cron schedule delay")
            .tags(tags)
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofSeconds(30))
            .register(registry)
            .record(delayMillis, TimeUnit.MILLISECONDS);
    }
}
