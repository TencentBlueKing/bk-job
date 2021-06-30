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

package com.tencent.bk.job.file_gateway.task;

import com.tencent.bk.job.file_gateway.task.dispatch.ReDispatchTimeoutTask;
import com.tencent.bk.job.file_gateway.task.filesource.FileSourceStatusUpdateTask;
import com.tencent.bk.job.file_gateway.task.worker.WorkerOnlineStatusUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final FileSourceStatusUpdateTask fileSourceStatusUpdateTask;
    private final WorkerOnlineStatusUpdateTask workerOnlineStatusUpdateTask;
    private final ReDispatchTimeoutTask reDispatchTimeoutTask;

    @Autowired
    public ScheduledTasks(FileSourceStatusUpdateTask fileSourceStatusUpdateTask,
                          WorkerOnlineStatusUpdateTask workerOnlineStatusUpdateTask,
                          ReDispatchTimeoutTask reDispatchTimeoutTask) {
        this.fileSourceStatusUpdateTask = fileSourceStatusUpdateTask;
        this.workerOnlineStatusUpdateTask = workerOnlineStatusUpdateTask;
        this.reDispatchTimeoutTask = reDispatchTimeoutTask;
    }

    /**
     * 文件源状态更新：3s/次
     */
    @Scheduled(fixedDelay = 3 * 1000L)
    public void refreshFileSourceStatus() {
        logger.info(Thread.currentThread().getId() + ":refreshFileSourceStatus start");
        try {
            fileSourceStatusUpdateTask.run();
        } catch (Exception e) {
            logger.error("fileSourceStatusUpdateTask fail", e);
        }
    }

    /**
     * Worker在线状态更新：3s/次
     */
    @Scheduled(fixedDelay = 3 * 1000L, initialDelay = 2 * 1000L)
    public void refreshWorkerOnlineStatus() {
        logger.info(Thread.currentThread().getId() + ":refreshWorkerOnlineStatus start");
        try {
            workerOnlineStatusUpdateTask.run();
        } catch (Exception e) {
            logger.error("workerOnlineStatusUpdateTask fail", e);
        }
    }

    /**
     * 任务超时重调度：1s/次
     */
    @Scheduled(fixedDelay = 1000L, initialDelay = 3 * 1000L)
    public void reDispatchTimeoutFileSourceTask() {
        logger.info(Thread.currentThread().getId() + ":reDispatchTimeoutFileSourceTask start");
        try {
            reDispatchTimeoutTask.run();
        } catch (Exception e) {
            logger.error("reDispatchTimeoutFileSourceTask fail", e);
        }
    }
}
