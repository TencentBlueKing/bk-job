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

package com.tencent.bk.job.execute.task;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.constants.Consts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

@Slf4j
@Component
public class LocalTmpFileCleanTask {

    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final StorageSystemConfig storageSystemConfig;

    public LocalTmpFileCleanTask(
        LocalFileConfigForExecute localFileConfigForExecute,
        StorageSystemConfig storageSystemConfig) {
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.storageSystemConfig = storageSystemConfig;
    }

    public void execute() {
        // 使用制品库作为存储后端时才清理下载产生的临时文件
        if (JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY
            .equals(localFileConfigForExecute.getStorageBackend())) {
            cleanLocalTmpFile();
        } else {
            // 使用本地NFS作为存储后端时在job-manage中筛选被引用文件并清理NFS，此处无需清理
            log.info(
                "local file storageBackend:{}, do not clean tmp file",
                localFileConfigForExecute.getStorageBackend()
            );
        }
    }

    /**
     * 清理下载到本地的临时文件
     */
    private void cleanLocalTmpFile() {
        log.info("begin to cleanLocalTmpFile downloaded from artifactory");
        // 单个任务最长执行时间为1天，清理一天之前的所有临时文件
        Date thresholdDate = new Date(
            System.currentTimeMillis() - 3600 * 24 * 1000
        );
        String localFileDirPath = PathUtil.joinFilePath(
            storageSystemConfig.getJobStorageRootPath(), Consts.LOCAL_FILE_DIR_NAME
        );
        Iterator<File> fileIterator = FileUtils.iterateFiles(
            new File(localFileDirPath),
            new AgeFileFilter(thresholdDate),
            TrueFileFilter.TRUE
        );
        while (fileIterator.hasNext()) {
            File aFile = fileIterator.next();
            log.info("Delete local tmp file {}", aFile.getPath());
            FileUtils.deleteQuietly(aFile);
        }
        log.info("cleanLocalTmpFile finished");
    }
}
