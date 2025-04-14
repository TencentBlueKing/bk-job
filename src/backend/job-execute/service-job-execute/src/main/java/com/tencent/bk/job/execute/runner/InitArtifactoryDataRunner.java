package com.tencent.bk.job.execute.runner;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.artifactory.sdk.InitJobRepoProcess;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.execute.config.LogExportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component("jobExecuteInitArtifactoryDataRunner")
public class InitArtifactoryDataRunner implements CommandLineRunner {

    private final ArtifactoryConfig artifactoryConfig;
    private final ArtifactoryHelper artifactoryHelper;
    private final LogExportConfig logExportConfig;

    @Autowired
    public InitArtifactoryDataRunner(ArtifactoryConfig artifactoryConfig,
                                     ArtifactoryHelper artifactoryHelper,
                                     LogExportConfig logExportConfig) {
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryHelper = artifactoryHelper;
        this.logExportConfig = logExportConfig;
    }

    @Override
    public void run(String... args) {
        if (!JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(logExportConfig.getStorageBackend())) {
            //不使用制品库作为后端存储时不初始化
            return;
        }
        String logExportRepo = logExportConfig.getLogExportRepo();
        String repoDescription = "BlueKing bk-job official project logExport repo," +
            " which is used to save job execute log export data produced by program. " +
            "Do not delete me unless you know what you are doing";
        new InitJobRepoProcess(
            artifactoryConfig,
            artifactoryHelper,
            logExportRepo,
            repoDescription
        ).execute();
    }

}
