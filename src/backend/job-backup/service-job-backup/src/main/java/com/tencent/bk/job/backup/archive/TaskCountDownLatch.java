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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 任务 CountDownLatch 组件，用于等待所有任务完成后再退出
 */
@Slf4j
public class TaskCountDownLatch {
    private final Object taskMonitor = new Object();
    private final CountDownLatch latch;
    private final Set<String> taskIds = new HashSet<>();
    private volatile boolean monitorInitial = false;
    private volatile boolean isAllTaskDone = false;

    public TaskCountDownLatch(Set<String> taskIds) {
        this.taskIds.addAll(taskIds);
        this.latch = new CountDownLatch(taskIds.size());
        startMonitor();
    }

    public void decrement(String taskId) {
        synchronized (taskMonitor) {
            if (this.latch != null) {
                if (taskIds.remove(taskId)) {
                    this.latch.countDown();
                    log.info("Task is stopped, remove from counter! taskId: {}", taskId);
                } else {
                    log.warn("Unexpected stopped taskId: {}", taskId);
                }
            }
        }
    }

    public boolean waitingForAllTasksDone(long timeoutSeconds) {
        try {
            log.info("Waiting for all tasks done! total: {}", latch.getCount());
            isAllTaskDone = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (isAllTaskDone) {
                log.info("All tasks have been completed");
            } else {
                log.info("Some tasks did not end within the timeout period, timeout: {}, notCompletedTaskIds: {}",
                    timeoutSeconds, taskIds);
            }
            return isAllTaskDone;
        } catch (InterruptedException e) {
            log.warn("Task count down latch wait interrupted", e);
            return false;
        }
    }

    private synchronized void startMonitor() {
        if (!monitorInitial) {
            log.info("Start TaskCountDownLatch monitor ...");
            Thread monitorThread = new Thread("TaskCountDownLatchMonitor") {
                @Override
                public void run() {
                    while (!isAllTaskDone) {
                        log.info("Waiting for tasks stopped! taskSize: {}, tasks: {}", taskIds.size(), taskIds);
                        ThreadUtils.sleep(2000L);
                    }
                }
            };
            monitorThread.start();
            monitorInitial = true;
        }
    }

    @PreDestroy
    private void destroy() {
        if (isAllTaskDone) {
            log.info("All task are stopped!");
        } else {
            log.warn("Tasks are not completely stopped! remain task size: {}, remain tasks: {}", taskIds.size(),
                taskIds);
        }
    }
}
