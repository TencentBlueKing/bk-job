package com.tencent.bk.job.backup.runner;

import com.tencent.bk.job.backup.config.BackupStorageConfig;
import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.artifactory.sdk.InitJobRepoProcess;
import com.tencent.bk.job.common.constant.JobConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component("jobBackupInitArtifactoryDataRunner")
public class InitArtifactoryDataRunner implements CommandLineRunner {

    private final ArtifactoryConfig artifactoryConfig;
    private final BackupStorageConfig backupStorageConfig;
    private final ArtifactoryHelper artifactoryHelper;

    @Autowired
    public InitArtifactoryDataRunner(ArtifactoryConfig artifactoryConfig,
                                     BackupStorageConfig backupStorageConfig,
                                     ArtifactoryHelper artifactoryHelper) {
        this.artifactoryConfig = artifactoryConfig;
        this.backupStorageConfig = backupStorageConfig;
        this.artifactoryHelper = artifactoryHelper;
    }

    @Override
    public void run(String... args) {
        if (!JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(backupStorageConfig.getStorageBackend())) {
            //不使用制品库作为后端存储时不初始化
            return;
        }
        String backupRepo = backupStorageConfig.getBackupRepo();
        String repoDescription = "BlueKing bk-job official project backup repo," +
            " which is used to save job export data produced by program. " +
            "Do not delete me unless you know what you are doing";
        new InitJobRepoProcess(
            artifactoryConfig,
            artifactoryHelper,
            backupRepo,
            repoDescription
        ).execute();
    }

}
