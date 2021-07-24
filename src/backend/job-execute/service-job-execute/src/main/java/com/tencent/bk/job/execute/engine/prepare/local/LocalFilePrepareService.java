package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.execute.config.ArtifactoryConfigForExecute;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
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
    private final TaskInstanceService taskInstanceService;
    private final Map<Long, LocalFilePrepareTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    public LocalFilePrepareService(
        StorageSystemConfig storageSystemConfig,
        ArtifactoryConfigForExecute artifactoryConfig,
        ArtifactoryClient artifactoryClient,
        TaskInstanceService taskInstanceService) {
        this.storageSystemConfig = storageSystemConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryClient = artifactoryClient;
        this.taskInstanceService = taskInstanceService;
    }

    @PostConstruct
    private void init(){
        LocalFilePrepareTask.init(artifactoryConfig.getDownloadConcurrency());
    }

    public void stopPrepareLocalFilesAsync(
        long stepInstanceId
    ) {
        LocalFilePrepareTask task = taskMap.get(stepInstanceId);
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
            return;
        }
        LocalFilePrepareTask task = new LocalFilePrepareTask(
            false,
            fileSourceList,
            new RecordableLocalFilePrepareTaskResultHandler(stepInstanceId, resultHandler),
            artifactoryClient,
            artifactoryConfig.getJobLocalUploadRootPath(),
            storageSystemConfig.getJobStorageRootPath()
        );
        taskMap.put(stepInstanceId, task);
        task.execute();
    }

    class RecordableLocalFilePrepareTaskResultHandler implements LocalFilePrepareTaskResultHandler {

        long stepInstanceId;
        LocalFilePrepareTaskResultHandler resultHandler;

        public RecordableLocalFilePrepareTaskResultHandler(
            long stepInstanceId,
            LocalFilePrepareTaskResultHandler resultHandler
        ) {
            this.stepInstanceId = stepInstanceId;
            this.resultHandler = resultHandler;
        }

        @Override
        public void onSuccess(LocalFilePrepareTask prepareTask) {
            taskMap.remove(stepInstanceId);
            resultHandler.onSuccess(prepareTask);
        }

        @Override
        public void onStopped(LocalFilePrepareTask prepareTask) {
            taskMap.remove(stepInstanceId);
            resultHandler.onStopped(prepareTask);
        }

        @Override
        public void onFailed(LocalFilePrepareTask prepareTask) {
            taskMap.remove(stepInstanceId);
            resultHandler.onFailed(prepareTask);
        }
    }
}
