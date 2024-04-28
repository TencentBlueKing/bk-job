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

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.config.FileDistributeConfig;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class LocalFilePrepareService {

    private final FileDistributeConfig fileDistributeConfig;
    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final AgentService agentService;
    private final StepInstanceService stepInstanceService;
    private final ArtifactoryClient artifactoryClient;
    private final Map<String, ArtifactoryLocalFilePrepareTask> taskMap = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor localFileDownloadExecutor;
    private final ThreadPoolExecutor localFileWatchExecutor;

    @Autowired
    public LocalFilePrepareService(FileDistributeConfig fileDistributeConfig,
                                   ArtifactoryConfig artifactoryConfig,
                                   LocalFileConfigForExecute localFileConfigForExecute,
                                   AgentService agentService,
                                   StepInstanceService stepInstanceService,
                                   @Qualifier("jobArtifactoryClient") ArtifactoryClient artifactoryClient,
                                   @Qualifier("localFileDownloadExecutor") ThreadPoolExecutor localFileDownloadExecutor,
                                   @Qualifier("localFileWatchExecutor") ThreadPoolExecutor localFileWatchExecutor) {
        this.fileDistributeConfig = fileDistributeConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.agentService = agentService;
        this.stepInstanceService = stepInstanceService;
        this.artifactoryClient = artifactoryClient;
        this.localFileDownloadExecutor = localFileDownloadExecutor;
        this.localFileWatchExecutor = localFileWatchExecutor;
    }

    public void stopPrepareLocalFilesAsync(
        StepInstanceDTO stepInstance
    ) {
        ArtifactoryLocalFilePrepareTask task = taskMap.get(stepInstance.getUniqueKey());
        if (task != null) {
            task.stop();
        }
    }

    public void prepareLocalFilesAsync(
        StepInstanceDTO stepInstance,
        List<FileSourceDTO> fileSourceList,
        LocalFilePrepareTaskResultHandler resultHandler
    ) {
        fillLocalFileSourceHost(fileSourceList, stepInstance);

        if (!JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(
            localFileConfigForExecute.getStorageBackend()
        )) {
            log.info("artifactory is not enable, not need to prepare local file");
            resultHandler.onSuccess(new NFSLocalFilePrepareTask(false));
            return;
        }
        ArtifactoryLocalFilePrepareTask task = new ArtifactoryLocalFilePrepareTask(
            stepInstance,
            false,
            fileSourceList,
            new RecordableLocalFilePrepareTaskResultHandler(stepInstance, resultHandler),
            artifactoryClient,
            artifactoryConfig.getArtifactoryJobProject(),
            localFileConfigForExecute.getLocalUploadRepo(),
            fileDistributeConfig.getJobDistributeRootPath(),
            localFileDownloadExecutor,
            localFileWatchExecutor
        );
        taskMap.put(stepInstance.getUniqueKey(), task);
        task.execute();
    }

    private void fillLocalFileSourceHost(List<FileSourceDTO> fileSourceList, StepInstanceBaseDTO stepInstance) {
        boolean isGseV2Task = stepInstance.isTargetGseV2Agent();
        fileSourceList.forEach(fileSourceDTO -> {
            if (fileSourceDTO.getFileType() == TaskFileTypeEnum.LOCAL.getType() || fileSourceDTO.isLocalUpload()) {
                HostDTO localHost = agentService.getLocalAgentHost().clone();
                if (!isGseV2Task) {
                    // 如果目标Agent是GSE V1, 那么源Agent也必须要GSE1.0 Agent，设置agentId={云区域:ip}
                    localHost.setAgentId(localHost.toCloudIp());
                }
                ExecuteTargetDTO fileSourceExecuteObjects = new ExecuteTargetDTO();
                fileSourceExecuteObjects.setStaticIpList(Collections.singletonList(localHost));
                fileSourceExecuteObjects.buildMergedExecuteObjects(stepInstance.isSupportExecuteObjectFeature());
                fileSourceDTO.setServers(fileSourceExecuteObjects);
                log.info("FillLocalFileSourceHost -> stepInstanceId: {}, isGseV2Task: {}, localFileSource: {}",
                    stepInstance.getId(), isGseV2Task, fileSourceDTO);
            }
        });
        // 更新本地文件任务内容
        stepInstanceService.updateResolvedSourceFile(stepInstance.getId(), fileSourceList);
    }

    public void clearPreparedTmpFile(long stepInstanceId) {
        // 本地文件暂不支持实时清理，依赖定时清理
    }

    class RecordableLocalFilePrepareTaskResultHandler implements LocalFilePrepareTaskResultHandler {

        StepInstanceDTO stepInstance;
        LocalFilePrepareTaskResultHandler resultHandler;

        RecordableLocalFilePrepareTaskResultHandler(
            StepInstanceDTO stepInstance,
            LocalFilePrepareTaskResultHandler resultHandler
        ) {
            this.stepInstance = stepInstance;
            this.resultHandler = resultHandler;
        }

        @Override
        public void onSuccess(JobTaskContext taskContext) {
            taskMap.remove(stepInstance.getUniqueKey());
            resultHandler.onSuccess(taskContext);
        }

        @Override
        public void onStopped(JobTaskContext taskContext) {
            taskMap.remove(stepInstance.getUniqueKey());
            resultHandler.onStopped(taskContext);
        }

        @Override
        public void onFailed(JobTaskContext taskContext) {
            taskMap.remove(stepInstance.getUniqueKey());
            resultHandler.onFailed(taskContext);
        }
    }
}
