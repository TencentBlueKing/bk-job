package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.execute.config.ArtifactoryConfigForExecute;
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
    private final ArtifactoryConfigForExecute artifactoryConfig;
    private final ArtifactoryClient artifactoryClient;
    private final Map<Long, ArtifactoryLocalFilePrepareTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    public LocalFilePrepareService(
        StorageSystemConfig storageSystemConfig,
        ArtifactoryConfigForExecute artifactoryConfig,
        ArtifactoryClient artifactoryClient
    ) {
        this.storageSystemConfig = storageSystemConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryClient = artifactoryClient;
    }

    @PostConstruct
    private void init() {
        ArtifactoryLocalFilePrepareTask.init(artifactoryConfig.getDownloadConcurrency());
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
        if (!artifactoryConfig.isEnable()) {
            log.info("artifactory is not enable, not need to prepare local file");
            resultHandler.onSuccess(new NFSLocalFilePrepareTask(false));
            return;
        }
        ArtifactoryLocalFilePrepareTask task = new ArtifactoryLocalFilePrepareTask(
            false,
            fileSourceList,
            new RecordableLocalFilePrepareTaskResultHandler(stepInstanceId, resultHandler),
            artifactoryClient,
            artifactoryConfig.getJobProject()
                + "/" + artifactoryConfig.getJobLocalUploadRepo(),
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
