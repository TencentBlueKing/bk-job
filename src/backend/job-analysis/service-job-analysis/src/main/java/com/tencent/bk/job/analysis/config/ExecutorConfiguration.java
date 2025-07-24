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

package com.tencent.bk.job.analysis.config;

import com.tencent.bk.job.analysis.task.statistics.StatisticsTaskScheduler;
import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration(value = "jobAnalysisExecutorConfig")
public class ExecutorConfiguration {

    @Bean("analysisAsyncTaskExecutor")
    public ThreadPoolExecutor analysisAsyncTaskExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "analysisAsyncTaskExecutor",
            0,
            50,
            180L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            (r, executor) -> log.warn("AsyncTask runnable rejected!")
        );
    }

    @Bean("analysisScheduleExecutor")
    public ThreadPoolExecutor analysisScheduleExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "analysisScheduleExecutor",
            5,
            10,
            60L,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(2), (r, executor) ->
            log.warn("analysisTask runnable rejected!")
        );
    }

    @Bean("currentStatisticsTaskExecutor")
    public ThreadPoolExecutor currentStatisticsTaskExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "currentStatisticsTaskExecutor",
            StatisticsTaskScheduler.defaultCorePoolSize,
            StatisticsTaskScheduler.defaultMaximumPoolSize,
            StatisticsTaskScheduler.defaultKeepAliveTime,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(StatisticsTaskScheduler.currentStatisticsTaskQueueSize),
            (r, executor) -> log.error("statisticsTask runnable rejected! num:{}",
                StatisticsTaskScheduler.rejectedStatisticsTaskNum.incrementAndGet()));
    }

    @Bean("pastStatisticsTaskExecutor")
    public ThreadPoolExecutor pastStatisticsTaskExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "pastStatisticsTaskExecutor",
            StatisticsTaskScheduler.defaultCorePoolSize,
            StatisticsTaskScheduler.defaultMaximumPoolSize,
            StatisticsTaskScheduler.defaultKeepAliveTime,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(StatisticsTaskScheduler.pastStatisticsTaskQueueSize),
            (r, executor) -> log.error("pastStatisticsTaskExecutor runnable rejected! num:{}",
                StatisticsTaskScheduler.rejectedStatisticsTaskNum.incrementAndGet()));
    }
}
