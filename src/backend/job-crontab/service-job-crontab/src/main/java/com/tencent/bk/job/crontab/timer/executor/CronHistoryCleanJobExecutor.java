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

package com.tencent.bk.job.crontab.timer.executor;

import com.tencent.bk.job.crontab.service.CronJobHistoryService;
import com.tencent.bk.job.crontab.service.InnerJobHistoryService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzJobBean;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * 定时任务启动记录清理作业执行者
 *
 * @since 17/12/2020 17:11
 */
@Slf4j
@Setter
public class CronHistoryCleanJobExecutor extends AbstractQuartzJobBean {
    @Autowired
    CronJobHistoryService cronJobHistoryService;

    @Autowired
    InnerJobHistoryService innerCronJobHistoryService;

    /**
     * 启动记录保存时间
     */
    @Value("${job.crontab.history.clean.keep.day:7}")
    private Long logKeepDay;

    @Override
    public String name() {
        return "HistoryCleanJob";
    }

    @Override
    protected void executeInternalInternal(JobExecutionContext context) throws JobExecutionException {
        log.debug("Start cleaning cron job execute history...");
        long cleanTime = System.currentTimeMillis() - logKeepDay * 24 * 3600 * 1000;
        int jobHistoryCleanCount = cronJobHistoryService.cleanHistory(cleanTime, false);
        int innerJobHistoryCleanCount = innerCronJobHistoryService.cleanHistory(cleanTime, false);
        log.info("Clean success job history before {}|Common {}|Inner {}", cleanTime, jobHistoryCleanCount,
            innerJobHistoryCleanCount);
        log.debug("Cron job execute history successfully cleaned.");
    }
}
