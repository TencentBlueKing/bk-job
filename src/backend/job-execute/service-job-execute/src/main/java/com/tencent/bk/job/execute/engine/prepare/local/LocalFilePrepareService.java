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
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class LocalFilePrepareService {

    private final StorageSystemConfig storageSystemConfig;
    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final AgentService agentService;
    private final TaskInstanceService taskInstanceService;
    private final ArtifactoryClient artifactoryClient;
    private final Map<Long, ArtifactoryLocalFilePrepareTask> taskMap = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor localFilePrepareExecutor;

    @Autowired
    public LocalFilePrepareService(StorageSystemConfig storageSystemConfig,
                                   ArtifactoryConfig artifactoryConfig,
                                   LocalFileConfigForExecute localFileConfigForExecute,
                                   AgentService agentService,
                                   TaskInstanceService taskInstanceService,
                                   ArtifactoryClient artifactoryClient,
                                   @Qualifier("localFilePrepareExecutor") ThreadPoolExecutor localFilePrepareExecutor) {
        this.storageSystemConfig = storageSystemConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.agentService = agentService;
        this.taskInstanceService = taskInstanceService;
        this.artifactoryClient = artifactoryClient;
        this.localFilePrepareExecutor = localFilePrepareExecutor;
    }

    public void stopPrepareLocalFilesAsync(
        long stepInstanceId
    ) {
        ArtifactoryLocalFilePrepareTask task = taskMap.get(stepInstanceId);
        if (task != null) {
            task.stop();
        }
    }

    public void prepareLocalFilesAsync(
        long stepInstanceId,
        List<FileSourceDTO> fileSourceList,
        LocalFilePrepareTaskResultHandler resultHandler
    ) {
        if (!JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(
            localFileConfigForExecute.getStorageBackend()
        )) {
            log.info("artifactory is not enable, not need to prepare local file");
            resultHandler.onSuccess(new NFSLocalFilePrepareTask(false));
            return;
        }
        fileSourceList.forEach(fileSourceDTO -> {
            if (fileSourceDTO.getFileType() == TaskFileTypeEnum.LOCAL.getType() || fileSourceDTO.isLocalUpload()) {
                fileSourceDTO.setServers(agentService.getLocalServersDTO());
            }
        });
        // 更新本地文件任务内容
        taskInstanceService.updateResolvedSourceFile(stepInstanceId, fileSourceList);
        ArtifactoryLocalFilePrepareTask task = new ArtifactoryLocalFilePrepareTask(
            stepInstanceId,
            false,
            fileSourceList,
            new RecordableLocalFilePrepareTaskResultHandler(stepInstanceId, resultHandler),
            artifactoryClient,
            artifactoryConfig.getArtifactoryJobProject(),
            localFileConfigForExecute.getLocalUploadRepo(),
            storageSystemConfig.getJobStorageRootPath(),
            localFilePrepareExecutor
        );
        taskMap.put(stepInstanceId, task);
        task.execute();
    }

    public void clearPreparedTmpFile(long stepInstanceId) {
        // 本地文件暂不支持实时清理，依赖定时清理
    }

    class RecordableLocalFilePrepareTaskResultHandler implements LocalFilePrepareTaskResultHandler {

        long stepInstanceId;
        LocalFilePrepareTaskResultHandler resultHandler;

        RecordableLocalFilePrepareTaskResultHandler(
            long stepInstanceId,
            LocalFilePrepareTaskResultHandler resultHandler
        ) {
            this.stepInstanceId = stepInstanceId;
            this.resultHandler = resultHandler;
        }

        @Override
        public void onSuccess(JobTaskContext taskContext) {
            taskMap.remove(stepInstanceId);
            resultHandler.onSuccess(taskContext);
        }

        @Override
        public void onStopped(JobTaskContext taskContext) {
            taskMap.remove(stepInstanceId);
            resultHandler.onStopped(taskContext);
        }

        @Override
        public void onFailed(JobTaskContext taskContext) {
            taskMap.remove(stepInstanceId);
            resultHandler.onFailed(taskContext);
        }
    }
}
