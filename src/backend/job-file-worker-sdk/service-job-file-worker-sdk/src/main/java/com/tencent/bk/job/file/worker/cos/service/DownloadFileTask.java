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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.file.worker.model.FileMetaData;
import com.tencent.bk.job.file_gateway.consts.TaskCommandEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
class DownloadFileTask extends Thread {

    RemoteClient remoteClient;
    String taskId;
    String filePath;
    String downloadFileDir;
    String filePrefix;
    AtomicLong fileSize;
    AtomicInteger speed;
    AtomicInteger process;
    TaskReporter taskReporter;
    DownloadFileTaskEventListener taskEventListener;
    FileProgressWatchingTask watchingTask;
    public DownloadFileTask(RemoteClient remoteClient, String taskId, String filePath, String downloadFileDir,
                            String filePrefix, AtomicLong fileSize, AtomicInteger speed, AtomicInteger process,
                            FileProgressWatchingTask watchingTask,
                            TaskReporter taskReporter, DownloadFileTaskEventListener taskEventListener) {
        this.remoteClient = remoteClient;
        this.taskId = taskId;
        this.filePath = filePath;
        this.downloadFileDir = downloadFileDir;
        this.filePrefix = filePrefix;
        this.fileSize = fileSize;
        this.speed = speed;
        this.process = process;
        this.watchingTask = watchingTask;
        this.taskReporter = taskReporter;
        this.taskEventListener = taskEventListener;
    }

    private void clearTmpFile(String path) {
        File tmpFile = new File(path);
        if (tmpFile.exists()) {
            if (!tmpFile.delete()) {
                log.warn("Fail to delete tmpFile {}", path);
            }
        }
    }

    // 下载文件到本地
    public void downloadFileToLocal(
        RemoteClient remoteClient,
        String filePath,
        String targetPath,
        AtomicLong fileSizeWrapper,
        AtomicInteger speed,
        AtomicInteger process
    ) throws ServiceException {
        InputStream ins = null;
        try {
            String fileMd5 = "";
            String currentMd5 = "";
            int count = 0;
            boolean downloadSuccess = false;
            do {
                count += 1;
                FileMetaData metaData = remoteClient.getFileMetaData(filePath);
                long fileSize = metaData.getSize();
                fileMd5 = metaData.getMd5();
                fileSizeWrapper.set(fileSize);
                ins = remoteClient.getFileInputStream(filePath);
                currentMd5 = FileUtil.writeInsToFile(ins, targetPath, fileSize, speed, process);
                if (fileMd5 == null) {
                    log.warn("No Md5 in metadata, do not check,key={},targetPath={},fileMd5={},currentMd5={}",
                        filePath, targetPath, fileMd5, currentMd5);
                    downloadSuccess = true;
                } else {
                    if (!fileMd5.equals(currentMd5)) {
                        log.warn("Md5 not match,key={},targetPath={},fileMd5={},currentMd5={},retry {}", filePath,
                            targetPath, fileMd5, currentMd5, count);
                        clearTmpFile(targetPath);
                    } else {
                        downloadSuccess = true;
                    }
                }
                Thread.sleep(0);
            } while (!downloadSuccess && count < 10);
            if (!downloadSuccess) {
                throw new InternalException(ErrorCode.INTERNAL_ERROR,
                    String.format("Fail to download %s because md5 not match 10 times, " +
                    "filePath=%s", targetPath, filePath));
            }
        } catch (InterruptedException e) {
            // 停止下载任务时，线程被主动中断，删除下一半的文件
            clearTmpFile(targetPath);
            String msg = String.format("download interrupted, %s deleted", targetPath);
            log.info(msg);
            throw new RuntimeException(msg, e);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    log.warn("Fail to close inputStream", e);
                }
            }
            remoteClient.shutdown();
        }
    }

    @Override
    public void run() {
        String fileTaskKey = taskId + "_" + filePath;
        String downloadPath = PathUtil.joinFilePath(downloadFileDir, taskId + "/" + filePath);
        // 加前缀
        int i = downloadPath.lastIndexOf("/");
        String dir = downloadPath.substring(0, i + 1);
        String fileName = downloadPath.substring(i + 1);
        if (filePrefix == null) filePrefix = "";
        fileName = filePrefix + fileName;
        downloadPath = dir + fileName;
        try {
            taskReporter.reportFileDownloadStart(taskId, filePath, downloadPath);
            downloadFileToLocal(remoteClient, filePath, downloadPath, fileSize, speed, process);
            watchingTask.stopWatching();
            taskReporter.reportFileDownloadSuccess(taskId, filePath, downloadPath, fileSize.get(), speed.get(),
                process.get());
        } catch (Throwable t) {
            watchingTask.stopWatching();
            if (t.getCause() instanceof InterruptedException) {
                ThreadCommandBus.Command command = ThreadCommandBus.getCommandQueue(fileTaskKey).poll();
                if (command == null) {
                    log.error("DownloadFileTask interrupted unexpectedly", t);
                } else if (command.cmd == TaskCommandEnum.STOP_AND_REPORT) {
                    //由于主动停止下载引起的异常
                    taskReporter.reportFileDownloadStopped(taskId, filePath, downloadPath, fileSize.get(),
                        process.get());
                } else if (command.cmd == TaskCommandEnum.STOP_QUIETLY) {
                    log.info("stop {} quitely, wait to be reDispatch", filePath);
                }
            } else {
                log.error("Fail to download file:filePath={},downloadPath={}", filePath, downloadPath, t);
                taskReporter.reportFileDownloadFailure(taskId, filePath, downloadPath);
            }
        } finally {
            if (taskEventListener != null) {
                taskEventListener.onTaskFinally(fileTaskKey);
            }
        }
    }

    public interface DownloadFileTaskEventListener {
        void onTaskFinally(String fileTaskKey);
    }
}
