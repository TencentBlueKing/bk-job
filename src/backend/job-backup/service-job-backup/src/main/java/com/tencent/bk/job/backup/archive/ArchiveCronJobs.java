/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.config.JobLogArchiveProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 归档定时任务
 */
@EnableScheduling
@Slf4j
public class ArchiveCronJobs {

    private final JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator;

    private final JobLogArchiveTaskGenerator jobLogArchiveTaskGenerator;

    private final JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler;

    private final JobLogArchiveTaskScheduler jobLogArchiveTaskScheduler;

    private final ArchiveProperties archiveProperties;

    private final JobLogArchiveProperties jobLogArchiveProperties;

    private final AbnormalArchiveTaskReScheduler abnormalArchiveTaskReScheduler;

    public ArchiveCronJobs(JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator,
                           JobLogArchiveTaskGenerator jobLogArchiveTaskGenerator,
                           JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler,
                           JobLogArchiveTaskScheduler jobLogArchiveTaskScheduler,
                           ArchiveProperties archiveProperties,
                           JobLogArchiveProperties jobLogArchiveProperties,
                           AbnormalArchiveTaskReScheduler abnormalArchiveTaskReScheduler) {
        this.jobInstanceArchiveTaskGenerator = jobInstanceArchiveTaskGenerator;
        this.jobLogArchiveTaskGenerator = jobLogArchiveTaskGenerator;
        this.jobInstanceArchiveTaskScheduler = jobInstanceArchiveTaskScheduler;
        this.jobLogArchiveTaskScheduler = jobLogArchiveTaskScheduler;
        this.archiveProperties = archiveProperties;
        this.jobLogArchiveProperties = jobLogArchiveProperties;
        this.abnormalArchiveTaskReScheduler = abnormalArchiveTaskReScheduler;
    }

    /**
     * 定时创建归档任务,每小时 0 分钟 触发一次（正常情况一天触发一次即可；为了保障异常情况下任务有一定频率的重试机会）
     */
    @Scheduled(cron = "0 0 * * * *")
    public void generateArchiveTask() {
        if (archiveProperties.isEnabled()) {
            log.info("Generate historical data archive task start...");
            jobInstanceArchiveTaskGenerator.generate();
            log.info("Generate historical data archive task done");
        }

        if (jobLogArchiveProperties.isEnabled()) {
            log.info("Generate historical log archive task start...");
            jobLogArchiveTaskGenerator.generate();
            log.info("Generate historical log archive task done");
        }
    }

    /**
     * 定时调度并执行历史数据归档任务，默认每小时第 1 分钟触发一次
     */
    @Scheduled(cron = "${job.backup.archive.execute.cron: 0 1 * * * *}")
    public void runHistoricalDataArchive() {
        if (archiveProperties.isEnabled()) {
            log.info("Schedule and execute historical data archive task start...");
            jobInstanceArchiveTaskScheduler.schedule();
            log.info("Schedule and execute historical data archive task done");
        }
    }

    /**
     * 定时调度并执行执行日志归档任务，默认每小时第 1 分钟触发一次
     */
    @Scheduled(cron = "${job.backup.archive.execute-log.cron: 0 1 * * * *}")
    public void runJobLogArchive() {
        if (jobLogArchiveProperties.isEnabled()) {
            log.info("Schedule and execute historical log archive task start...");
            jobLogArchiveTaskScheduler.schedule();
            log.info("Schedule and execute historical log archive task done");
        }
    }

    /**
     * 失败归档任务重调度，每小时触发一次
     */
    @Scheduled(cron = "0 59 * * * *")
    public void scheduleFailedTasks() {
        if (!archiveProperties.isEnabled() && !jobLogArchiveProperties.isEnabled()) {
            return;
        }
        log.info("ReSchedule fail/timout/dryrun archive task start...");
        abnormalArchiveTaskReScheduler.rescheduleFailedArchiveTasks();
        log.info("ReSchedule fail/timeout/dryrun archive task done");
    }
}
