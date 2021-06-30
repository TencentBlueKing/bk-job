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

package com.tencent.bk.job.file.worker.task.clear;

import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

@Slf4j
@Service
public class ClearExpiredFileTask {

    private final WorkerConfig workerConfig;

    @Autowired
    public ClearExpiredFileTask(WorkerConfig workerConfig) {
        this.workerConfig = workerConfig;
    }

    public void run() {
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
                    log.warn("Fail to delete {}", taskDirFile.getAbsolutePath(), e);
                }
            }
        }
        log.info("clearTask end");
    }
}
