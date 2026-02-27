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

package com.tencent.bk.job.crontab.timer.executor;

import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.crontab.config.CleanHistoryProperties;
import com.tencent.bk.job.crontab.service.CronJobHistoryService;
import com.tencent.bk.job.crontab.service.InnerJobHistoryService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzJobBean;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.IntFunction;

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

    @Autowired
    CleanHistoryProperties cleanHistoryProperties;

    @Override
    public String name() {
        return "HistoryCleanJob";
    }

    @Override
    protected void executeInternalInternal(JobExecutionContext context) {
        try {
            cleanCronJobHistory(context);
        } catch (Exception e) {
            log.error("Fail to cleanCronJobHistory", e);
        }
    }

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    protected void cleanCronJobHistory(JobExecutionContext context) {
        long cleanTime = System.currentTimeMillis() - cleanHistoryProperties.getKeepDays() * 24 * 3600 * 1000L;
        log.info(
            "Begin to delete cron job history before {}, cleanTime={}, " +
                "keepDays={}, batchSize={}, sleepMillisBetweenBatches={}",
            TimeUtil.formatTime(cleanTime, TIME_FORMAT),
            cleanTime,
            cleanHistoryProperties.getKeepDays(),
            cleanHistoryProperties.getBatchSize(),
            cleanHistoryProperties.getSleepMillisBetweenBatches()
        );

        int jobHistoryCleanCount = cleanHistoryInBatches(cronJobHistoryService, cleanTime);
        int innerJobHistoryCleanCount =
            cleanHistoryInBatches(innerCronJobHistoryService, cleanTime);

        log.info(
            "Finish to clean cron job history, totalDeletedNum: cron_job_history={}, inner_cron_job_history={}, "
                + "cleanTime={}",
            jobHistoryCleanCount,
            innerJobHistoryCleanCount,
            TimeUtil.formatTime(cleanTime, TIME_FORMAT)
        );
    }

    /**
     * 分批次清理历史记录，每批次最多删除 batchSize 条，避免一次性删除大量数据导致 DB 高负载
     */
    private int cleanHistoryInBatches(CronJobHistoryService service,
                                      long cleanTime) {
        return doCleanHistoryInBatches(
            (limit) -> service.cleanHistory(cleanTime, limit),
            "cron_job_history"
        );
    }

    private int cleanHistoryInBatches(InnerJobHistoryService service, long cleanTime) {
        return doCleanHistoryInBatches(
            (limit) -> service.cleanHistory(cleanTime, limit),
            "inner_cron_job_history"
        );
    }

    private int doCleanHistoryInBatches(IntFunction<Integer> cleanBatch, String tableLabel) {
        int batchSize = cleanHistoryProperties.getBatchSize();
        long sleepMillis = cleanHistoryProperties.getSleepMillisBetweenBatches();
        int totalDeletedNum = 0;
        int deletedNum;
        do {
            deletedNum = cleanBatch.apply(batchSize);
            totalDeletedNum += deletedNum;
            if (deletedNum > 0) {
                // 每删除10w条记录打印一次日志
                if (totalDeletedNum % 100000 == 0) {
                    log.info(
                        "Clean {} batch deleted {} records, totalDeletedNum={}",
                        tableLabel,
                        deletedNum,
                        totalDeletedNum
                    );
                } else {
                    log.debug(
                        "Clean {} batch deleted {} records, totalDeletedNum={}",
                        tableLabel,
                        deletedNum,
                        totalDeletedNum
                    );
                }
                ThreadUtils.sleep(sleepMillis);
            }
        } while (deletedNum >= batchSize);
        return totalDeletedNum;
    }
}
