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

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.constants.Consts;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ThreadPoolExecutor threadPoolExecutor;
    public static FinalResultHandler finalResultHandler = null;

    public ArtifactoryLocalFilePrepareTask(
        StepInstanceDTO stepInstance,
        boolean isForRetry,
        List<FileSourceDTO> fileSourceList,
        LocalFilePrepareTaskResultHandler resultHandler,
        ArtifactoryClient artifactoryClient,
        String artifactoryProject,
        String artifactoryRepo,
        String jobStorageRootPath,
        ThreadPoolExecutor threadPoolExecutor
    ) {
        this.stepInstance = stepInstance;
        this.isForRetry = isForRetry;
        this.fileSourceList = fileSourceList;
        this.resultHandler = resultHandler;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryProject = artifactoryProject;
        this.artifactoryRepo = artifactoryRepo;
        this.jobStorageRootPath = jobStorageRootPath;
        this.threadPoolExecutor = threadPoolExecutor;
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
        if (finalResultHandler != null) {
            finalResultHandler.interrupt();
        }
    }

    public void execute() {
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO == null) {
                log.warn("[{}]:fileSourceDTO is null", stepInstance.getUniqueKey());
                continue;
            }
            if (fileSourceDTO.isLocalUpload() || fileSourceDTO.getFileType() == TaskFileTypeEnum.LOCAL.getType()) {
                List<FileDetailDTO> files = fileSourceDTO.getFiles();
                for (FileDetailDTO file : files) {
                    FileDownloadTask task = new FileDownloadTask(file);
                    Future<Boolean> future = threadPoolExecutor.submit(task);
                    futureList.add(future);
                }
            }
        }
        finalResultHandler = new FinalResultHandler(this, futureList, resultHandler);
        finalResultHandler.start();
    }

    class FinalResultHandler extends Thread {

        ArtifactoryLocalFilePrepareTask artifactoryLocalFilePrepareTask;
        List<Future<Boolean>> futureList;
        LocalFilePrepareTaskResultHandler resultHandler;

        FinalResultHandler(
            ArtifactoryLocalFilePrepareTask artifactoryLocalFilePrepareTask,
            List<Future<Boolean>> futureList,
            LocalFilePrepareTaskResultHandler resultHandler
        ) {
            this.artifactoryLocalFilePrepareTask = artifactoryLocalFilePrepareTask;
            this.futureList = futureList;
            this.resultHandler = resultHandler;
        }

        @Override
        public void run() {
            List<Boolean> resultList = new ArrayList<>();
            for (Future<Boolean> future : futureList) {
                try {
                    resultList.add(future.get(30, TimeUnit.MINUTES));
                } catch (InterruptedException e) {
                    log.info("[{}]:task stopped", stepInstance.getUniqueKey());
                    resultHandler.onStopped(artifactoryLocalFilePrepareTask);
                    return;
                } catch (ExecutionException e) {
                    log.info("[{}]:task download failed", stepInstance.getUniqueKey());
                    resultHandler.onFailed(artifactoryLocalFilePrepareTask);
                    return;
                } catch (TimeoutException e) {
                    log.info("[{}]:task download timeout", stepInstance.getUniqueKey());
                    resultHandler.onFailed(artifactoryLocalFilePrepareTask);
                    return;
                }
            }
            if (ListUtil.isAllTrue(resultList)) {
                log.info("[{}]:all task success", stepInstance.getUniqueKey());
                resultHandler.onSuccess(artifactoryLocalFilePrepareTask);
            } else {
                int failCount = 0;
                for (Boolean result : resultList) {
                    if (!result) {
                        failCount++;
                    }
                }
                log.warn(
                    "[{}]:{}/{} localFile prepare tasks failed",
                    stepInstance.getUniqueKey(),
                    failCount,
                    resultList.size()
                );
                resultHandler.onFailed(artifactoryLocalFilePrepareTask);
            }
        }
    }

    class FileDownloadTask implements Callable<Boolean> {

        private final FileDetailDTO file;

        FileDownloadTask(FileDetailDTO file) {
            this.file = file;
        }

        @Override
        public Boolean call() {
            try {
                return doCall();
            } catch (Throwable t) {
                FormattingTuple msg = MessageFormatter.format(
                    "[{}]:Unexpected error when prepare localFile {}",
                    stepInstance.getUniqueKey(),
                    file.getFilePath()
                );
                log.error(msg.getMessage(), t);
                return false;
            }
        }

        private Boolean doCall() {
            String filePath = file.getFilePath();
            // 本地存储路径
            String localPath = PathUtil.joinFilePath(jobStorageRootPath, Consts.LOCAL_FILE_DIR_NAME);
            localPath = PathUtil.joinFilePath(localPath, filePath);
            File localFile = new File(localPath);
            // 如果本地文件还未下载就已存在，说明是分发配置文件，直接完成准备阶段
            if (localFile.exists()) {
                log.debug("[{}]:local file already exists", stepInstance.getUniqueKey());
                return true;
            }
            // 制品库的完整路径
            NodeDTO nodeDTO = artifactoryClient.queryNodeDetail(artifactoryProject, artifactoryRepo, filePath);
            if (nodeDTO == null) {
                log.warn(
                    "[{}]:File {} not exists in project {} repo {}",
                    stepInstance.getUniqueKey(),
                    filePath,
                    artifactoryProject,
                    artifactoryRepo
                );
                return false;
            }
            Pair<InputStream, HttpRequestBase> pair = artifactoryClient.getFileInputStream(
                artifactoryProject,
                artifactoryRepo,
                filePath
            );
            InputStream ins = pair.getLeft();
            Long fileSize = nodeDTO.getSize();
            // 保存到本地临时目录
            AtomicInteger speed = new AtomicInteger(0);
            AtomicInteger process = new AtomicInteger(0);
            try {
                log.debug(
                    "[{}]:begin to download {} to {}",
                    stepInstance.getUniqueKey(),
                    filePath,
                    localPath
                );
                FileUtil.writeInsToFile(ins, localPath, fileSize, speed, process);
                return true;
            } catch (InterruptedException e) {
                log.warn(
                    "[{}]:Interrupted:Download {} to {}",
                    stepInstance.getUniqueKey(),
                    filePath,
                    localPath
                );
            } catch (Exception e) {
                log.error(
                    "[{}]:Fail to download {} to {}",
                    stepInstance.getUniqueKey(),
                    filePath,
                    localPath
                );
            }
            return false;
        }
    }
}
