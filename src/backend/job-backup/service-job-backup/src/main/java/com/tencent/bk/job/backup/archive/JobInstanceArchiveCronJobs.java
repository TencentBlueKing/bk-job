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

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 归档定时任务
 */
@Component
@EnableScheduling
@Slf4j
public class JobInstanceArchiveCronJobs {

    private final JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator;

    private final JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler;

    public JobInstanceArchiveCronJobs(JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator,
                                      JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler) {
        this.jobInstanceArchiveTaskGenerator = jobInstanceArchiveTaskGenerator;
        this.jobInstanceArchiveTaskScheduler = jobInstanceArchiveTaskScheduler;
    }

    /**
     * 定时创建归档任务
     */
    @Scheduled(cron = "${job.backup.archive.execute.cron:0,6,12,18 0 0 * * *}")
    public void generateArchiveTask() {
        log.info("Generate archive task start...");
        jobInstanceArchiveTaskGenerator.generate();
        log.info("Generate archive task done");
    }

    /**
     * 定时调度并执行归档任务
     */
    @Scheduled(cron = "${job.backup.archive.execute.cron:1,7,13,19 0 0 * * *}")
    public void scheduleAndExecuteArchiveTask() {
        log.info("Schedule and execute archive task start...");
        jobInstanceArchiveTaskScheduler.schedule();
        log.info("Schedule and execute archive task done");
    }
}
