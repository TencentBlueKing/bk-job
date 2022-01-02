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
import org.apache.commons.io.FileUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClearFileTask {

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
                    log.warn("Fail to delete {}", taskDirFile.getAbsolutePath(), e);
                }
            }
        }
        log.info("clearTask end");
    }

    /**
     * 删除文件，若删除失败则记录路径
     *
     * @param file          文件
     * @param failedPathSet 删除失败时用于记录路径的Set
     * @return 是否删除成功
     */
    private boolean deleteFileAndRecordIfFail(File file, Set<String> failedPathSet) {
        try {
            FileUtils.deleteQuietly(file);
            return true;
        } catch (Exception e) {
            failedPathSet.add(file.getAbsolutePath());
            FormattingTuple message = MessageFormatter.format(
                "Fail to delete file {}",
                file.getAbsolutePath()
            );
            log.warn(message.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查磁盘容量并清理最旧的文件
     */
    public void checkVolumeAndClear() {
        long maxSizeBytes = workerConfig.getMaxSizeGB() * 1024 * 1024 * 1024;
        File workDirFile = new File(workerConfig.getDownloadFileDir());
        long currentSize = FileUtils.sizeOfDirectory(workDirFile);
        File[] files = workDirFile.listFiles();
        if (files == null || files.length == 0) return;
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.sort(Comparator.comparingLong(File::lastModified));
        // 记录删除失败的文件，下次不再列出
        Set<String> deleteFailedFilePathSet = new HashSet<>();
        int count = 0;
        while (currentSize > maxSizeBytes) {
            if (fileList.isEmpty()) {
                // 上一次拿到的文件列表已删完，空间依然超限，说明删除过程中又新产生了许多文件，重新列出
                files = workDirFile.listFiles();
                if (files == null || files.length == 0) return;
                fileList.addAll(Arrays.stream(files)
                    .filter(file -> !deleteFailedFilePathSet.contains(file.getAbsolutePath()))
                    .collect(Collectors.toList())
                );
                fileList.sort(Comparator.comparingLong(File::lastModified));
            }
            if (fileList.isEmpty()) {
                log.warn("volume still overlimit after clear, deleteFailedFilePathSet={}", deleteFailedFilePathSet);
                return;
            }
            File oldestFile = fileList.remove(0);
            if (deleteFileAndRecordIfFail(oldestFile, deleteFailedFilePathSet)) {
                count += 1;
                log.info("delete file {} because of volume overlimit", oldestFile.getAbsolutePath());
            }
            currentSize = FileUtils.sizeOfDirectory(workDirFile);
        }
        if (log.isDebugEnabled()) {
            log.debug("{} files deleted because of volume overlimit", count);
        } else if (count > 0) {
            log.info("{} files deleted because of volume overlimit", count);
        }
    }
}
