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

package com.tencent.bk.job.crontab.timer;

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.crontab.metrics.ScheduleMeasureService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.ScopedSpan;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
public abstract class AbstractQuartzJobBean extends QuartzJobBean {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssX").withZone(ZoneOffset.UTC);

    @Autowired
    ScheduleMeasureService scheduleMeasureService;

    @Autowired
    Tracer tracer;

    protected static Instant getScheduledFireTime(JobExecutionContext context) {
        return context.getScheduledFireTime().toInstant();
    }

    protected static String getKey(JobExecutionContext context) {
        return context.getJobDetail().getKey() + ":" + FORMATTER.format(getScheduledFireTime(context));
    }

    protected static String getLockKey(JobExecutionContext context) {
        return "bk:cronjob:lock:" + getKey(context);
    }

    public abstract String name();

    @Override
    protected void executeInternal(JobExecutionContext context) {
        scheduleMeasureService.recordCronScheduleDelay(name(), context);
        ScopedSpan span = tracer.startScopedSpan("executeCronJob");
        JobContextUtil.setRequestId(span.context().traceId());
        String executeId = JobContextUtil.getRequestId();
        try {
            if (log.isDebugEnabled()) {
                log.debug("{}|Job {} key {} start execute ...", executeId, name(), getLockKey(context));
            }
            if (LockUtils.tryGetDistributedLock(getLockKey(context), executeId, 1000L)) {
                executeInternalInternal(context);
            } else {
                log.warn("{}|Job {} key {} execute aborted. Acquire lock failed!", executeId, name(),
                    getLockKey(context));
            }

            if (log.isDebugEnabled()) {
                log.debug("{}|Job {} key {} execute finished.", executeId, name(), getLockKey(context));
            }
        } catch (JobExecutionException e) {
            log.error("fail to executeInternal", e);
        } finally {
            LockUtils.releaseDistributedLock(getLockKey(context), executeId);
            span.end();
        }
    }

    protected abstract void executeInternalInternal(JobExecutionContext context) throws JobExecutionException;
}
