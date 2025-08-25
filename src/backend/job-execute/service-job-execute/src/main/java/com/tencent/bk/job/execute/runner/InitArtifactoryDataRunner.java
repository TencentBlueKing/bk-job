package com.tencent.bk.job.execute.runner;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.artifactory.sdk.InitJobRepoProcess;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.execute.config.LogExportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component("jobExecuteInitArtifactoryDataRunner")
public class InitArtifactoryDataRunner implements CommandLineRunner {

    private final ArtifactoryConfig artifactoryConfig;
    private final ArtifactoryHelper artifactoryHelper;
    private final LogExportConfig logExportConfig;
    private final ThreadPoolExecutor initRunnerExecutor;

    @Autowired
    public InitArtifactoryDataRunner(ArtifactoryConfig artifactoryConfig,
                                     ArtifactoryHelper artifactoryHelper,
                                     LogExportConfig logExportConfig,
                                     @Qualifier("executeInitRunnerExecutor") ThreadPoolExecutor initRunnerExecutor) {
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryHelper = artifactoryHelper;
        this.logExportConfig = logExportConfig;
        this.initRunnerExecutor = initRunnerExecutor;
    }

    @Override
    public void run(String... args) {
        initRunnerExecutor.submit(this::initRepo);
    }

    /**
     * 初始化所需制品库仓库
     */
    private void initRepo() {
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
