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

package com.tencent.bk.job.file.worker.task.clear;

import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.FileUtil;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

@Slf4j
@Service
public class ClearFileTask {

    private volatile boolean checkVolumeRunning = false;
    private final WorkerConfig workerConfig;

    @Autowired
    public ClearFileTask(WorkerConfig workerConfig) {
        this.workerConfig = workerConfig;
    }

    /**
     * 清理长时间自动过期的文件
     */
    public void clearExpiredFile() {
        long expireTimeMills =
            System.currentTimeMillis() - workerConfig.getDownloadFileExpireDays() * 24 * 3600 * 1000L;
        log.info("begin to clear taskFileDir lastModified before {}",
            DateUtils.defaultFormatTimestamp(new Timestamp(expireTimeMills)));
        File wsFile = new File(workerConfig.getWorkspaceDirPath());
        File[] taskDirs = wsFile.listFiles();
        if (taskDirs == null) {
            log.info("taskDirs is null under workspace:{}", workerConfig.getWorkspaceDirPath());
            return;
        }
        if (taskDirs.length > 10000) {
            log.info("{} taskDirs found", taskDirs.length);
        }
        for (int i = 0; i < taskDirs.length; i++) {
            File taskDirFile = taskDirs[i];
            if (taskDirFile.lastModified() < expireTimeMills) {
                try {
                    FileUtils.deleteDirectory(taskDirFile);
                } catch (IOException e) {
                    String msg = MessageFormatter.format(
                        "Fail to delete {}",
                        taskDirFile.getAbsolutePath()
                    ).getMessage();
                    log.warn(msg, e);
                }
            }
        }
        log.info("clearTask end");
    }

    /**
     * 检查磁盘容量并清理最旧的文件
     */
    public void checkVolumeAndClear() {
        try {
            // 上一次清理任务还未跑完则不清理
            if (checkVolumeRunning) {
                log.info("last checkVolumeAndClear task still running, skip");
                return;
            }
            checkVolumeRunning = true;
            doCheckVolumeAndClear();
        } catch (Exception e) {
            log.warn("Exception when checkVolumeAndClear", e);
        } finally {
            checkVolumeRunning = false;
        }
    }

    private void doCheckVolumeAndClear() {
        long maxSizeBytes = workerConfig.getMaxSizeGB() * 1024L * 1024L * 1024L;
        int count = FileUtil.checkVolumeAndClearOldestFiles(maxSizeBytes, workerConfig.getWorkspaceDirPath());
        if (count > 0) {
            log.info("{} file cleared", count);
        }
    }

}
