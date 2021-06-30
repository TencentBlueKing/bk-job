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

package com.tencent.bk.job.analysis.task;

import com.tencent.bk.job.analysis.task.analysis.AnalysisTaskScheduler;
import com.tencent.bk.job.analysis.task.statistics.StatisticsTaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Description 增加调度需要注意到ScheduleConfig中更新线程池配置
 * @Date 2020/1/3
 * @Version 1.0
 */
@Component
@EnableScheduling
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private AnalysisTaskScheduler analysisTaskScheduler;
    private StatisticsTaskScheduler statisticsTaskScheduler;

    @Autowired
    public ScheduledTasks(AnalysisTaskScheduler analysisTaskScheduler,
                          StatisticsTaskScheduler statisticsTaskScheduler) {
        this.analysisTaskScheduler = analysisTaskScheduler;
        this.statisticsTaskScheduler = statisticsTaskScheduler;
    }

    /**
     * 驱动统计分析任务调度
     * 1h一次
     */
    @Scheduled(cron = "0 1 * * * *")
    public void statisticsTaskSchedulerTask() {
        logger.info("statisticsTaskSchedulerTask:"
            + Thread.currentThread().getId()
            + "," + Thread.currentThread().getName()
        );
        try {
            statisticsTaskScheduler.schedule();
        } catch (Exception e) {
            logger.error("analysisTaskSchedulerTask fail", e);
        }
    }

    /**
     * 驱动后台异常分析任务调度
     * 1s一次
     */
    @Scheduled(fixedDelay = 1000)
    public void analysisTaskSchedulerTask() {
        try {
            analysisTaskScheduler.schedule();
        } catch (Exception e) {
            logger.error("analysisTaskSchedulerTask fail", e);
        }
    }
}
