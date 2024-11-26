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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.config.ArchiveProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 归档定时任务
 */
@EnableScheduling
@Slf4j
public class JobInstanceArchiveCronJobs {

    private final JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator;

    private final JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler;

    private final ArchiveProperties archiveProperties;

    private final AbnormalArchiveTaskReScheduler abnormalArchiveTaskReScheduler;

    public JobInstanceArchiveCronJobs(JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator,
                                      JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler,
                                      ArchiveProperties archiveProperties,
                                      AbnormalArchiveTaskReScheduler abnormalArchiveTaskReScheduler) {
        this.jobInstanceArchiveTaskGenerator = jobInstanceArchiveTaskGenerator;
        this.jobInstanceArchiveTaskScheduler = jobInstanceArchiveTaskScheduler;
        this.archiveProperties = archiveProperties;
        this.abnormalArchiveTaskReScheduler = abnormalArchiveTaskReScheduler;
    }

    /**
     * 定时创建归档任务,每小时 0 分钟 触发一次（正常情况一天触发一次即可；为了保障异常情况下任务有一定频率的重试机会）
     */
    @Scheduled(cron = "0 0 * * * *")
    public void generateArchiveTask() {
        if (!archiveProperties.isEnabled()) {
            return;
        }
        log.info("Generate archive task start...");
        jobInstanceArchiveTaskGenerator.generate();
        log.info("Generate archive task done");
    }

    /**
     * 定时调度并执行归档任务，默认每小时第 1 分钟触发一次
     */
    @Scheduled(cron = "${job.backup.archive.execute.cron: 0 1 * * * *}")
    public void scheduleAndExecuteArchiveTask() {
        if (!archiveProperties.isEnabled()) {
            return;
        }
        log.info("Schedule and execute archive task start...");
        jobInstanceArchiveTaskScheduler.schedule();
        log.info("Schedule and execute archive task done");
    }

    /**
     * 失败归档任务重调度，每小时触发一次
     */
    @Scheduled(cron = "0 59 * * * *")
    public void scheduleFailedTasks() {
        if (!archiveProperties.isEnabled()) {
            return;
        }
        log.info("ReSchedule fail/timout archive task start...");
        abnormalArchiveTaskReScheduler.rescheduleFailedArchiveTasks();
        log.info("ReSchedule fail/timeout archive task done");
    }
}
