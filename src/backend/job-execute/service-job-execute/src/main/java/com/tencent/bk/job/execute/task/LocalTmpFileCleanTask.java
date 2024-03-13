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
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.file.FileUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.config.FileDistributeConfig;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
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
    private final FileDistributeConfig fileDistributeConfig;

    public LocalTmpFileCleanTask(
        LocalFileConfigForExecute localFileConfigForExecute,
        FileDistributeConfig fileDistributeConfig) {
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.fileDistributeConfig = fileDistributeConfig;
    }

    public void execute() {
        // 使用制品库作为存储后端时才清理下载产生的临时文件
        if (JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY
            .equals(localFileConfigForExecute.getStorageBackend())) {
            cleanLocalTmpFileAndDir();
        } else {
            // 使用本地NFS作为存储后端时在job-manage中筛选被引用文件并清理NFS，此处无需清理
            log.info(
                "local file storageBackend:{}, do not clean tmp file",
                localFileConfigForExecute.getStorageBackend()
            );
        }
    }

    /**
     * 清理下载到本地的临时文件及目录
     */
    private void cleanLocalTmpFileAndDir() {
        log.info("begin to cleanLocalTmpFileAndDir downloaded from artifactory");
        // 单个任务最长执行时间为1天，清理一天之前的所有临时文件
        Date thresholdDate = new Date(
            System.currentTimeMillis() - 3600 * 24 * 1000
        );
        String localFileDirPath = PathUtil.joinFilePath(
            fileDistributeConfig.getJobDistributeRootPath(), Consts.LOCAL_FILE_DIR_NAME
        );

        File localFileDir = new File(localFileDirPath);
        if (!localFileDir.exists() || !localFileDir.isDirectory()) {
            log.warn("Local tmp file directory does not exist or is not a directory: {}", localFileDirPath);
            log.info("cleanLocalTmpFile finished");
            return;
        }

        File[] files = localFileDir.listFiles();
        if (files != null && files.length > 0) {
            Iterator<File> fileIterator = FileUtils.iterateFilesAndDirs(
                localFileDir,
                new AgeFileFilter(thresholdDate),
                TrueFileFilter.TRUE
            );
            while (fileIterator.hasNext()) {
                File aFile = fileIterator.next();
                // 清理存量遗留的空目录
                if (aFile.isDirectory()) {
                    clearEmptyDirAndParent(aFile);
                    continue;
                }
                // 清理文件
                boolean fileDeleted = FileUtils.deleteQuietly(aFile);
                if (!fileDeleted) {
                    log.warn("Fail to delete tmp file {}, ignore", aFile.getAbsolutePath());
                    continue;
                }
                // 同时清理两级无用的父目录
                File parentDirFile = aFile.getParentFile();
                boolean parentDirDeleted = false;
                boolean grandParentDirDeleted = false;
                if (parentDirFile != null) {
                    parentDirDeleted = deleteEmptyDirIfNotDisTributeRoot(parentDirFile);
                    File grandParentDirFile = parentDirFile.getParentFile();
                    if (grandParentDirFile != null) {
                        grandParentDirDeleted = deleteEmptyDirIfNotDisTributeRoot(grandParentDirFile);
                    }
                }
                log.info(
                    "Local tmp file {} deleted, parentDirDeleted={}, grandParentDirDeleted={}",
                    aFile.getPath(),
                    parentDirDeleted,
                    grandParentDirDeleted
                );
            }
        } else {
            log.warn("Local tmp file directory is empty: {}", localFileDirPath);
        }
        log.info("cleanLocalTmpFile finished");
    }

    /**
     * 清理非分发根目录的空目录及其父目录
     */
    private void clearEmptyDirAndParent(File dirFile) {
        boolean dirDeleted = deleteEmptyDirIfNotDisTributeRoot(dirFile);
        if (!dirDeleted) {
            return;
        }
        File parentDirFile = dirFile.getParentFile();
        boolean parentDirDeleted = false;
        if (parentDirFile != null) {
            parentDirDeleted = deleteEmptyDirIfNotDisTributeRoot(parentDirFile);
        }
        log.info("Local tmp dir {} deleted, parentDirDeleted={}", dirFile.getAbsolutePath(), parentDirDeleted);
    }

    /**
     * 清理非分发根目录的空目录
     *
     * @param dirFile 目录文件
     * @return 是否删除了空目录
     */
    private boolean deleteEmptyDirIfNotDisTributeRoot(File dirFile) {
        if (!isJobDisTributeRootPath(dirFile.getAbsolutePath())) {
            return FileUtil.deleteEmptyDirectory(dirFile);
        }
        return false;
    }

    /**
     * 判断路径是否指向分发根目录（忽略反斜杠后缀）
     *
     * @param path 路径
     * @return 路径是否指向分发根目录
     */
    private boolean isJobDisTributeRootPath(String path) {
        if (path == null) {
            return false;
        }
        String jobDisTributeRootPath = StringUtil.removeSuffix(fileDistributeConfig.getJobDistributeRootPath(), "/");
        return jobDisTributeRootPath.equals(StringUtil.removeSuffix(path, "/"));
    }
}
