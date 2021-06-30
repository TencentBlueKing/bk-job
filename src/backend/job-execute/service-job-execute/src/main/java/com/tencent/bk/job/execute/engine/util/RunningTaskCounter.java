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

package com.tencent.bk.job.execute.engine.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RunningTaskCounter<T> {
    private final String counterName;
    private final Set<T> runningTasks = new HashSet<>();
    private final Object lock = new Object();
    private volatile boolean active = false;
    private CountDownLatch latch;

    public RunningTaskCounter(String counterName) {
        this.counterName = counterName;
    }

    public void start() {
        this.active = true;
    }

    public void stop() {
        synchronized (lock) {
            this.active = false;
            latch = new CountDownLatch(runningTasks.size());
        }
    }

    public void add(T task) {
        synchronized (lock) {
            if (active) {
                this.runningTasks.add(task);
            }
        }
    }

    public void release(T task) {
        synchronized (lock) {
            this.runningTasks.remove(task);
            if (latch != null) {
                this.latch.countDown();
            }
        }
    }

    public void waitUntilTaskDone(long timeout, TimeUnit unit) {
        if (active) {
            log.warn("Counter is active, will operation is not allowed!");
            return;
        }
        if (runningTasks.isEmpty()) {
            return;
        }
        try {
            log.info("[" + counterName + "] Waiting for tasks, count: {}", runningTasks.size());
            synchronized (lock) {
                boolean finished = latch.await(timeout, unit);
                if (finished) {
                    log.info("[" + counterName + "] Successfully waited for tasks to finish.");
                } else {
                    log.error("[" + counterName + "] Tasks not finished. runningTasks:{}", runningTasks);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted waiting for workers.  Continuing waiting");
        }
    }
}
