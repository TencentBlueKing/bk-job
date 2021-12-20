package com.tencent.bk.job.execute.runner;

import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LogExportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InitArtifactoryDataRunner implements CommandLineRunner {

    private final ArtifactoryConfig artifactoryConfig;
    private final LogExportConfig logExportConfig;

    @Autowired
    public InitArtifactoryDataRunner(ArtifactoryConfig artifactoryConfig, LogExportConfig logExportConfig) {
        this.artifactoryConfig = artifactoryConfig;
        this.logExportConfig = logExportConfig;
    }

    @Override
    public void run(String... args) {
        String baseUrl = artifactoryConfig.getArtifactoryBaseUrl();
        String adminUsername = artifactoryConfig.getArtifactoryAdminUsername();
        String adminPassword = artifactoryConfig.getArtifactoryAdminPassword();
        String jobUsername = artifactoryConfig.getArtifactoryJobUsername();
        String jobPassword = artifactoryConfig.getArtifactoryJobPassword();
        String jobProject = artifactoryConfig.getArtifactoryJobProject();
        String logExportRepo = logExportConfig.getLogExportRepo();
        // 1.检查用户、仓库是否存在
        boolean userRepoExists = ArtifactoryHelper.checkRepoExists(
            baseUrl,
            jobUsername,
            jobPassword,
            jobProject,
            logExportRepo
        );
        if (userRepoExists) {
            return;
        }
        // 2.创建项目与用户
        boolean projectUserCreated = ArtifactoryHelper.createJobUserAndProjectIfNotExists(
            baseUrl,
            adminUsername,
            adminPassword,
            jobUsername,
            jobPassword,
            jobProject
        );
        if (!projectUserCreated) {
            log.error(
                "Fail to create project {} or user {}",
                jobProject,
                jobUsername
            );
        }
        // 3.logExport仓库不存在则创建
        String REPO_LOG_EXPORT_DESCRIPTION = "BlueKing bk-job official project logExport repo," +
            " which is used to save job execute log export data produced by program. " +
            "Do not delete me unless you know what you are doing";
        boolean repoCreated = ArtifactoryHelper.createRepoIfNotExist(
            baseUrl,
            adminUsername,
            adminPassword,
            jobProject,
            logExportRepo,
            REPO_LOG_EXPORT_DESCRIPTION
        );
        if (repoCreated) {
            log.info(
                "repo {} created",
                logExportRepo
            );
        }
    }

}
