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

package com.tencent.bk.job.file.worker.cos.service;

import com.tencent.bk.job.common.util.file.PathUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
class FileProgressWatchingTask extends Thread {

    String taskId;
    String filePath;
    String downloadFileDir;
    AtomicLong fileSize;
    AtomicInteger speed;
    AtomicInteger process;
    TaskReporter taskReporter;
    FileProgressWatchingTaskEventListener watchingTaskEventListener;
    volatile boolean runFlag = true;

    public FileProgressWatchingTask(String taskId, String filePath, String downloadFileDir, AtomicLong fileSize,
                                    AtomicInteger speed, AtomicInteger process, TaskReporter taskReporter,
                                    FileProgressWatchingTaskEventListener watchingTaskEventListener) {
        this.taskId = taskId;
        this.filePath = filePath;
        this.downloadFileDir = downloadFileDir;
        this.fileSize = fileSize;
        this.speed = speed;
        this.process = process;
        this.taskReporter = taskReporter;
        this.watchingTaskEventListener = watchingTaskEventListener;
    }

    public void stopWatching() {
        this.runFlag = false;
    }

    @Override
    public void run() {
        String fileTaskKey = taskId + "_" + filePath;
        try {
            String downloadPath = PathUtil.joinFilePath(downloadFileDir, taskId + "/" + filePath);
            while (runFlag) {
                try {
                    taskReporter.reportFileDownloadProgress(taskId, filePath, downloadPath, fileSize.get(),
                        speed.get(), process.get());
                } catch (Throwable t) {
                    log.error("Fail to reportFileDownloadProgress of file:{}", filePath, t);
                }
                if (runFlag) {
                    sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            log.info("watching interrupted", e);
        } finally {
            if (watchingTaskEventListener != null) {
                watchingTaskEventListener.onWatchingTaskFinally(fileTaskKey);
            }
        }
        log.debug("end watching:{}", fileTaskKey);
    }

    public interface FileProgressWatchingTaskEventListener {
        void onWatchingTaskFinally(String fileTaskKey);
    }
}
