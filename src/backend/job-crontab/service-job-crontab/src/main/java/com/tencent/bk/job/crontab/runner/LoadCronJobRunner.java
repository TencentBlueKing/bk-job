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

package com.tencent.bk.job.crontab.runner;

import com.tencent.bk.job.crontab.service.CronJobLoadingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 进程启动时立即将DB中的定时任务加载到Quartz
 */
@Slf4j
@Component
public class LoadCronJobRunner implements CommandLineRunner {

    private final CronJobLoadingService cronJobLoadingService;
    private final ThreadPoolExecutor crontabInitRunnerExecutor;

    private Future<?> loadCronJobFuture;

    @Autowired
    public LoadCronJobRunner(CronJobLoadingService cronJobLoadingService,
                             @Qualifier("crontabInitRunnerExecutor") ThreadPoolExecutor crontabInitRunnerExecutor) {
        this.cronJobLoadingService = cronJobLoadingService;
        this.crontabInitRunnerExecutor = crontabInitRunnerExecutor;
    }

    @Override
    public void run(String... args) {
        loadCronJobFuture = crontabInitRunnerExecutor.submit(() -> {
            log.info("loadCronToQuartzOnStartup");
            cronJobLoadingService.loadAllCronJob();
        });
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy LoadCronJobRunner");
        if (loadCronJobFuture != null) {
            boolean result = loadCronJobFuture.cancel(true);
            log.info("loadCronJobFuture cancel result:{}", result);
        }
    }
}
