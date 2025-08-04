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

package com.tencent.bk.job.execute.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import com.tencent.bk.job.execute.util.ContextExecutorService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration(value = "jobExecuteExecutorConfig")
public class ExecutorConfiguration {

    @Bean("logExportExecutor")
    public ExecutorService logExportExecutor(MeterRegistry meterRegistry) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("log-export-thread-%d").build();
        return ContextExecutorService.wrap(new WatchableThreadPoolExecutor(
            meterRegistry,
            "logExportExecutor",
            10,
            100,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory
        ));
    }

    @Bean("getHostsByTopoExecutor")
    public ExecutorService getHostsByTopoExecutor(MeterRegistry meterRegistry) {
        return ContextExecutorService.wrap(new WatchableThreadPoolExecutor(
            meterRegistry,
            "getHostsByTopoExecutor",
            50,
            100,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        ));
    }

    @Bean("getHostTopoPathExecutor")
    public ExecutorService getHostTopoPathExecutor(MeterRegistry meterRegistry) {
        return ContextExecutorService.wrap(new WatchableThreadPoolExecutor(
            meterRegistry,
            "getHostTopoPathExecutor",
            5,
            50,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5),
            (r, executor) -> {
                //使用请求的线程直接拉取数据
                log.error(
                    "getHostTopoPath runnable rejected," +
                        " use current thread({}), plz add more threads",
                    Thread.currentThread().getName());
                r.run();
            }
        ));
    }

    @Bean("localFileDownloadExecutor")
    public ExecutorService localFileDownloadExecutor(LocalFileConfigForExecute localFileConfigForExecute,
                                                     MeterRegistry meterRegistry) {
        int concurrency = localFileConfigForExecute.getDownloadConcurrency();
        return ContextExecutorService.wrap(new WatchableThreadPoolExecutor(
            meterRegistry,
            "localFileDownloadExecutor",
            concurrency,
            concurrency,
            180L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            (r, executor) -> {
                //使用请求的线程直接拉取数据
                log.error(
                    "download localupload file from artifactory runnable rejected," +
                        " use current thread({}), plz add more threads",
                    Thread.currentThread().getName());
                r.run();
            }
        ));
    }

    @Bean("localFileWatchExecutor")
    public ExecutorService localFileWatchExecutor(MeterRegistry meterRegistry) {
        return ContextExecutorService.wrap(new WatchableThreadPoolExecutor(
            meterRegistry,
            "localFileWatchExecutor",
            0,
            50,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            (r, executor) -> {
                //使用请求的线程直接拉取数据
                log.error(
                    "watch localupload file from artifactory runnable rejected," +
                        " use current thread({}), plz add more job-execute instances",
                    Thread.currentThread().getName());
                r.run();
            }
        ));
    }

    @Bean("shutdownExecutor")
    public ExecutorService shutdownExecutor(MeterRegistry meterRegistry) {
        return ContextExecutorService.wrap(new WatchableThreadPoolExecutor(
            meterRegistry,
            "shutdownExecutor",
            10,
            20,
            120,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        ));
    }

    @Bean("executeInitRunnerExecutor")
    public ThreadPoolExecutor executeInitRunnerExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "initRunnerExecutor",
            0,
            5,
            1,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );
    }
}
