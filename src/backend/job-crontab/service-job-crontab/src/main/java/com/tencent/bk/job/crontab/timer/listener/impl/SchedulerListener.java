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

package com.tencent.bk.job.crontab.timer.listener.impl;

import com.tencent.bk.job.crontab.timer.listener.AbstractSchedulerListener;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class SchedulerListener extends AbstractSchedulerListener {

    @Override
    public void jobScheduled(Trigger trigger) {
        if (log.isDebugEnabled()) {
            log.debug("Job {} Trigger {} scheduled at {}.", trigger.getJobKey(), trigger.getKey(), LocalDateTime.now());
        }
    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
        if (log.isDebugEnabled()) {
            log.debug("Trigger {} not scheduled.", triggerKey);
        }
    }

    @Override
    public void triggerFinalized(Trigger trigger) {
        if (log.isDebugEnabled()) {
            log.debug("Job {} Trigger {} reached stop time, finalized at {}.", trigger.getJobKey(), trigger.getKey(),
                LocalDateTime.now());
        }
    }

    @Override
    public void triggerPaused(TriggerKey triggerKey) {
        if (log.isDebugEnabled()) {
            log.debug("Trigger {} paused at {}.", triggerKey, LocalDateTime.now());
        }
    }

    @Override
    public void triggersPaused(String triggerGroup) {
        if (log.isDebugEnabled()) {
            log.debug("TriggerGroup {} paused at {}.", triggerGroup, LocalDateTime.now());
        }
    }

    @Override
    public void triggerResumed(TriggerKey triggerKey) {
        if (log.isDebugEnabled()) {
            log.debug("Trigger {} resumed at {}.", triggerKey, LocalDateTime.now());
        }
    }

    @Override
    public void triggersResumed(String triggerGroup) {
        if (log.isDebugEnabled()) {
            log.debug("TriggerGroup {} resumed at {}.", triggerGroup, LocalDateTime.now());
        }
    }

    @Override
    public void jobAdded(JobDetail jobDetail) {
        if (log.isDebugEnabled()) {
            log.debug("Job {} added at {}.", jobDetail, LocalDateTime.now());
        }
    }

    @Override
    public void jobDeleted(JobKey jobKey) {
        if (log.isDebugEnabled()) {
            log.debug("Job {} deleted at {}.", jobKey, LocalDateTime.now());
        }
    }

    @Override
    public void jobPaused(JobKey jobKey) {
        if (log.isDebugEnabled()) {
            log.debug("Job {} paused at {}.", jobKey, LocalDateTime.now());
        }
    }

    @Override
    public void jobsPaused(String jobGroup) {
        if (log.isDebugEnabled()) {
            log.debug("JobGroup {} paused at {}.", jobGroup, LocalDateTime.now());
        }
    }

    @Override
    public void jobResumed(JobKey jobKey) {
        if (log.isDebugEnabled()) {
            log.debug("Job {} resumed at {}.", jobKey, LocalDateTime.now());
        }
    }

    @Override
    public void jobsResumed(String jobGroup) {
        if (log.isDebugEnabled()) {
            log.debug("JobGroup {} resumed at {}.", jobGroup, LocalDateTime.now());
        }
    }

    @Override
    public void schedulerError(String msg, SchedulerException cause) {
        if (log.isDebugEnabled()) {
            log.debug("Scheduler error at {}.Message {}.", LocalDateTime.now(), msg, cause);
        }
    }

    @Override
    public void schedulerInStandbyMode() {
        if (log.isDebugEnabled()) {
            log.debug("Scheduler in standby at {}.", LocalDateTime.now());
        }
    }

    @Override
    public void schedulerStarted() {
        if (log.isDebugEnabled()) {
            log.debug("Scheduler started at {}.", LocalDateTime.now());
        }
    }

    @Override
    public void schedulerStarting() {
        if (log.isDebugEnabled()) {
            log.debug("Starting scheduler...|{}", LocalDateTime.now());
        }
    }

    @Override
    public void schedulerShutdown() {
        if (log.isDebugEnabled()) {
            log.debug("Scheduler stopped at {}.", LocalDateTime.now());
        }
    }

    @Override
    public void schedulerShuttingdown() {
        if (log.isDebugEnabled()) {
            log.debug("Shutting down scheduler...|{}", LocalDateTime.now());
        }
    }

    @Override
    public void schedulingDataCleared() {
        if (log.isDebugEnabled()) {
            log.debug("Scheduler data cleared at {}.", LocalDateTime.now());
        }
    }

}
