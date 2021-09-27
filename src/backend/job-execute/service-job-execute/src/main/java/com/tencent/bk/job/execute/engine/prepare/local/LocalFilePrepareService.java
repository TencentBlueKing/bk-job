package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LocalFilePrepareService {

    private final StorageSystemConfig storageSystemConfig;
    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final ArtifactoryClient artifactoryClient;
    private final Map<Long, ArtifactoryLocalFilePrepareTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    public LocalFilePrepareService(
        StorageSystemConfig storageSystemConfig,
        LocalFileConfigForExecute localFileConfigForExecute,
        ArtifactoryClient artifactoryClient
    ) {
        this.storageSystemConfig = storageSystemConfig;
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.artifactoryClient = artifactoryClient;
    }

    @PostConstruct
    private void init() {
        ArtifactoryLocalFilePrepareTask.init(localFileConfigForExecute.getArtifactoryDownloadConcurrency());
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
        if (JobConstants.LOCAL_FILE_STORAGE_BACKEND_ARTIFACTORY.equals(localFileConfigForExecute.getStorageBackend())) {
            log.info("artifactory is not enable, not need to prepare local file");
            resultHandler.onSuccess(new NFSLocalFilePrepareTask(false));
            return;
        }
        ArtifactoryLocalFilePrepareTask task = new ArtifactoryLocalFilePrepareTask(
            false,
            fileSourceList,
            new RecordableLocalFilePrepareTaskResultHandler(stepInstanceId, resultHandler),
            artifactoryClient,
            localFileConfigForExecute.getArtifactoryJobProject()
                + "/" + localFileConfigForExecute.getArtifactoryJobLocalUploadRepo(),
            storageSystemConfig.getJobStorageRootPath()
        );
        taskMap.put(stepInstanceId, task);
        task.execute();
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
