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
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class FileTaskService {

    private static final ThreadPoolExecutor fileTaskExecutor = new ThreadPoolExecutor(20, 50, 1, TimeUnit.MINUTES,
        new LinkedBlockingQueue<>(1000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("fileTaskWorkerPool rejected a task, use current thread now, plz add more threads");
            r.run();
        }
    });
    private static final ThreadPoolExecutor watchingTaskExecutor = new ThreadPoolExecutor(20, 50, 1, TimeUnit.MINUTES
        , new LinkedBlockingQueue<>(1000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("watchingTaskExecutor rejected a task, ignore, plz add more threads");
        }
    });
    private static final ConcurrentHashMap<String, Future<?>> fileTaskMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Future<?>> watchingTaskMap = new ConcurrentHashMap<>();
    private final WorkerConfig workerConfig;
    private final TaskReporter taskReporter;
    @Autowired
    public FileTaskService(WorkerConfig workerConfig, TaskReporter taskReporter) {
        this.workerConfig = workerConfig;
        this.taskReporter = taskReporter;
    }

    public List<String> getAllTaskIdList() {
        List<String> runningTaskIdList = new ArrayList<>();
        Enumeration<String> keys = fileTaskMap.keys();
        while (keys.hasMoreElements()) {
            runningTaskIdList.add(keys.nextElement().split("_")[0]);
        }
        return runningTaskIdList;
    }

    public Integer downloadFiles(RemoteClient client, String taskId, List<String> filePathList, String filePrefix) {
        for (String filePath : filePathList) {
            String fileTaskKey = taskId + "_" + filePath;
            AtomicLong fileSize = new AtomicLong(0L);
            AtomicInteger speed = new AtomicInteger(0);
            AtomicInteger process = new AtomicInteger(0);
            FileProgressWatchingTask progressWatchingTask = new FileProgressWatchingTask(taskId, filePath,
                workerConfig.getWorkspaceDirPath(), fileSize, speed, process, taskReporter, watchingTaskMap::remove);
            DownloadFileTask downloadFileTask = new DownloadFileTask(client, taskId, filePath,
                workerConfig.getWorkspaceDirPath(), filePrefix, fileSize, speed, process, progressWatchingTask,
                taskReporter, tmpfileTaskKey -> {
                fileTaskMap.remove(tmpfileTaskKey);
                ThreadCommandBus.destroyCommandQueue(tmpfileTaskKey);
            });
            Future<?> fileTaskFuture = fileTaskExecutor.submit(downloadFileTask);
            Future<?> watchingTaskFuture = watchingTaskExecutor.submit(progressWatchingTask);
            fileTaskMap.put(fileTaskKey, fileTaskFuture);
            watchingTaskMap.put(fileTaskKey, watchingTaskFuture);
        }
        return filePathList.size();
    }

    public Integer clearTaskFilesAtOnce(List<String> taskIdList) {
        int count = 0;
        for (String taskId : taskIdList) {
            String deleteDirPath = taskId;
            try {
                deleteDirPath = PathUtil.joinFilePath(workerConfig.getWorkspaceDirPath(), taskId);
                FileUtils.deleteDirectory(new File(deleteDirPath));
                count += 1;
            } catch (IOException e) {
                log.warn("Fail to delete dir:{}", deleteDirPath, e);
            }
        }
        return count;
    }

    public Integer stopTasksAtOnce(List<String> taskIdList, ThreadCommandBus.Command command) {
        int allStoppedFileCount = 0;
        Enumeration<String> keys = fileTaskMap.keys();
        List<String> keyList = new ArrayList<>();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            keyList.add(key);
        }
        for (String taskId : taskIdList) {
            List<String> stopKeyList = new ArrayList<>();
            for (String key : keyList) {
                if (key.startsWith(taskId)) {
                    stopKeyList.add(key);
                }
            }
            for (String stopKey : stopKeyList) {
                log.info("try to stop {}", stopKey);
                Future<?> fileTaskFuture = fileTaskMap.get(stopKey);
                if (fileTaskFuture != null && !fileTaskFuture.isDone()) {
                    ThreadCommandBus.sendCommand(stopKey, command);
                    fileTaskFuture.cancel(true);
                    allStoppedFileCount += 1;
                } else {
                    log.info("task {} already done, stop too late", stopKey);
                }
            }
        }
        return allStoppedFileCount;
    }
}
