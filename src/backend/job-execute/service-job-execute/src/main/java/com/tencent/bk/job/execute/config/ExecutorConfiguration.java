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

package com.tencent.bk.job.execute.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class ExecutorConfiguration {

    @Bean("logExportExecutor")
    public ThreadPoolExecutor logExportExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("log-export-thread-%d").build();
        return new ThreadPoolExecutor(
            10,
            100,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory
        );
    }

    @Bean("getHostsByTopoExecutor")
    public ThreadPoolExecutor getHostsByTopoExecutor() {
        return new ThreadPoolExecutor(
            50,
            100,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );
    }

    @Bean("localFilePrepareExecutor")
    public ThreadPoolExecutor localFilePrepareExecutor(LocalFileConfigForExecute localFileConfigForExecute) {
        int concurrency = localFileConfigForExecute.getDownloadConcurrency();
        return new ThreadPoolExecutor(
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
        );
    }

    @Bean("shutdownExecutor")
    public ThreadPoolExecutor shutdownExecutor() {
        return new ThreadPoolExecutor(
            10,
            20,
            120,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );
    }
}
