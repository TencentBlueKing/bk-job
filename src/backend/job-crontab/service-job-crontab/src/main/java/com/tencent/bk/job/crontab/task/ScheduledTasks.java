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

package com.tencent.bk.job.crontab.task;

import com.tencent.bk.job.crontab.service.CronJobLoadingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component("jobCrontabScheduledTasks")
@EnableScheduling
public class ScheduledTasks {

    private final CronJobLoadingService cronJobLoadingService;

    @Autowired
    public ScheduledTasks(CronJobLoadingService cronJobLoadingService) {
        this.cronJobLoadingService = cronJobLoadingService;
    }

    /**
     * 每间隔30min更新一次定时任务数据到Quartz内存
     */
    @Scheduled(initialDelay = 5 * 1000, fixedDelay = 30 * 60 * 1000)
    public void loadCronToQuartz() {
        log.info("loadCronToQuartz");
        long start = System.currentTimeMillis();
        try {
            cronJobLoadingService.loadAllCronJob();
        } catch (Exception e) {
            log.error("loadCronToQuartz fail", e);
        } finally {
            log.info("loadCronToQuartz end, duration={}ms", System.currentTimeMillis() - start);
        }
    }
}
