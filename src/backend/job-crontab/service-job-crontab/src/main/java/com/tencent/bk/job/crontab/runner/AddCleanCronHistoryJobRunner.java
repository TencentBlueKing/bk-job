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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.crontab.config.CleanHistoryProperties;
import com.tencent.bk.job.crontab.runner.order.RunnerOrder;
import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.crontab.timer.QuartzJob;
import com.tencent.bk.job.crontab.timer.QuartzJobBuilder;
import com.tencent.bk.job.crontab.timer.QuartzTrigger;
import com.tencent.bk.job.crontab.timer.QuartzTriggerBuilder;
import com.tencent.bk.job.crontab.timer.executor.CronHistoryCleanJobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.SimpleTrigger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 进程启动后添加自动清理定时任务启动记录的内部定时任务
 */
@Slf4j
@Order(RunnerOrder.ADD_CLEAN_CRON_HISTORY_JOB)
@Component
public class AddCleanCronHistoryJobRunner implements CommandLineRunner {

    private final ThreadPoolExecutor crontabInitRunnerExecutor;

    private final AbstractQuartzTaskHandler quartzTaskHandler;

    private final CleanHistoryProperties cleanHistoryProperties;

    @Autowired
    public AddCleanCronHistoryJobRunner(@Qualifier("crontabInitRunnerExecutor")
                                        ThreadPoolExecutor crontabInitRunnerExecutor,
                                        AbstractQuartzTaskHandler quartzTaskHandler,
                                        CleanHistoryProperties cleanHistoryProperties) {
        this.crontabInitRunnerExecutor = crontabInitRunnerExecutor;
        this.quartzTaskHandler = quartzTaskHandler;
        this.cleanHistoryProperties = cleanHistoryProperties;
    }

    @Override
    public void run(String... args) {
        crontabInitRunnerExecutor.submit(this::addCleanJob);
    }

    private void addCleanJob() {
        if (!cleanHistoryProperties.getEnabled()) {
            log.info("cleanHistory is not enabled, please check config");
            return;
        }
        log.info("begin to addCleanJob");
        String cleanJobCron = cleanHistoryProperties.getCron();
        try {
            new CronExpression(cleanJobCron);
        } catch (ParseException e) {
            String msg = MessageFormatter.format(
                "Error while adding cron history clean job! Invalid expression|{}",
                cleanJobCron
            ).getMessage();
            log.warn(msg, e);
            return;
        }

        String cleanJobKey = "cron_history_clean";
        String systemId = "job-crontab";
        QuartzTrigger trigger = QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.CRON)
            .withIdentity(cleanJobKey, systemId).withDescription("Auto clean cron job history table")
            .withCronExpression(cleanJobCron)
            .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT).build();

        if (trigger == null) {
            return;
        }

        QuartzJob job = QuartzJobBuilder.newJob().withIdentity(cleanJobKey, systemId)
            .forJob(CronHistoryCleanJobExecutor.class).withTrigger(trigger)
            .build();

        try {
            quartzTaskHandler.waitSchedulerReadyThenAddJob(job);
            log.info("addCleanJob finished");
        } catch (Exception e) {
            log.error("Error while add job to quartz!", e);
            throw new InternalException("Add to quartz failed!", e, ErrorCode.INTERNAL_ERROR);
        }
    }
}
