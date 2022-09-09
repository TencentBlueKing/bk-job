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

package com.tencent.bk.job.manage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class ExecutorConfiguration {

    @Bean("syncAppExecutor")
    public ThreadPoolExecutor syncAppExecutor() {
        ThreadPoolExecutor syncAppExecutor = new ThreadPoolExecutor(
            5,
            5,
            1L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), (r, executor) ->
            log.error(
                "syncAppExecutor Runnable rejected! executor.poolSize={}, executor.queueSize={}",
                executor.getPoolSize(),
                executor.getQueue().size()
            )
        );
        syncAppExecutor.setThreadFactory(
            getThreadFactoryByNameAndSeq("syncAppExecutor-", new AtomicInteger(1))
        );
        return syncAppExecutor;
    }

    @Bean("notifySendExecutor")
    public ThreadPoolExecutor notifySendExecutor() {
        return new ThreadPoolExecutor(
            5,
            30,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10)
        );
    }

    private ThreadFactory getThreadFactoryByNameAndSeq(String namePrefix, AtomicInteger seq) {
        return r -> {
            Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                namePrefix + seq.getAndIncrement(),
                0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };
    }
}
