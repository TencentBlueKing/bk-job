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

package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 本地文件下载进度拉取任务调度
 */
@Slf4j
public class ArtifactoryLocalFilePrepareTask implements JobTaskContext {

    private final StepInstanceDTO stepInstance;
    private final boolean isForRetry;
    private final List<FileSourceDTO> fileSourceList;
    private final LocalFilePrepareTaskResultHandler resultHandler;
    private final ArtifactoryClient artifactoryClient;
    private final String artifactoryProject;
    private final String artifactoryRepo;
    private final String jobStorageRootPath;
    private final List<Future<Boolean>> futureList = new ArrayList<>();
    private final ExecutorService localFileDownloadExecutor;
    private final ExecutorService localFileWatchExecutor;
    public static Future<?> localFileWatchFuture = null;

    public ArtifactoryLocalFilePrepareTask(
        StepInstanceDTO stepInstance,
        boolean isForRetry,
        List<FileSourceDTO> fileSourceList,
        LocalFilePrepareTaskResultHandler resultHandler,
        ArtifactoryClient artifactoryClient,
        String artifactoryProject,
        String artifactoryRepo,
        String jobStorageRootPath,
        ExecutorService localFileDownloadExecutor,
        ExecutorService localFileWatchExecutor
    ) {
        this.stepInstance = stepInstance;
        this.isForRetry = isForRetry;
        this.fileSourceList = fileSourceList;
        this.resultHandler = resultHandler;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryProject = artifactoryProject;
        this.artifactoryRepo = artifactoryRepo;
        this.jobStorageRootPath = jobStorageRootPath;
        this.localFileDownloadExecutor = localFileDownloadExecutor;
        this.localFileWatchExecutor = localFileWatchExecutor;
    }

    @Override
    public boolean isForRetry() {
        return isForRetry;
    }

    public void stop() {
        for (Future<Boolean> future : futureList) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        if (localFileWatchFuture != null) {
            localFileWatchFuture.cancel(true);
        }
    }

    public void execute() {
        int localFileCount = 0;
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO == null) {
                log.warn("[{}]:fileSourceDTO is null", stepInstance.getUniqueKey());
                continue;
            }
            if (fileSourceDTO.isLocalUpload() || fileSourceDTO.getFileType() == TaskFileTypeEnum.LOCAL.getType()) {
                List<FileDetailDTO> files = fileSourceDTO.getFiles();
                for (FileDetailDTO file : files) {
                    LocalFileDownloadTask task = new LocalFileDownloadTask(
                        stepInstance,
                        artifactoryClient,
                        artifactoryProject,
                        artifactoryRepo,
                        jobStorageRootPath,
                        file
                    );
                    Future<Boolean> future = localFileDownloadExecutor.submit(task);
                    futureList.add(future);
                    localFileCount += 1;
                }
            }
        }
        LocalFileDownloadResultWatcher resultWatcher = new LocalFileDownloadResultWatcher(
            this,
            stepInstance,
            futureList,
            resultHandler
        );
        localFileWatchFuture = localFileWatchExecutor.submit(resultWatcher);
        log.info("[{}]: {} localFile downloadTask committed", stepInstance.getUniqueKey(), localFileCount);
    }

}
