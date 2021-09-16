package com.tencent.bk.job.manage.runner;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.dto.ProjectDTO;
import com.tencent.bk.job.common.artifactory.model.req.CheckRepoExistReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateProjectReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateRepoReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateUserToProjectReq;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.manage.config.ArtifactoryConfigForManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InitArtifactoryDataRunner implements CommandLineRunner {

    private final ArtifactoryConfigForManage artifactoryConfig;

    @Autowired
    public InitArtifactoryDataRunner(ArtifactoryConfigForManage artifactoryConfig) {
        this.artifactoryConfig = artifactoryConfig;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1.检查用户、仓库是否存在
        ArtifactoryClient jobClient = new ArtifactoryClient(
            artifactoryConfig.getBaseUrl(),
            artifactoryConfig.getJobUsername(),
            artifactoryConfig.getJobPassword(),
            null
        );
        NodeDTO localUploadRepoRootNode = null;
        try {
            localUploadRepoRootNode = jobClient.queryNodeDetail(
                artifactoryConfig.getJobPassword(),
                artifactoryConfig.getJobLocalUploadRepo(),
                "/"
            );
        } catch (Exception ignore) {
            log.info("Fail to queryNodeDetail");
        }
        if (localUploadRepoRootNode != null) {
            log.info(
                "user {} project {} repo {} already initialized",
                artifactoryConfig.getJobUsername(),
                artifactoryConfig.getJobProject(),
                artifactoryConfig.getJobLocalUploadRepo()
            );
            return;
        }
        ArtifactoryClient adminClient = new ArtifactoryClient(
            artifactoryConfig.getBaseUrl(),
            artifactoryConfig.getAdminUsername(),
            artifactoryConfig.getAdminPassword(),
            null
        );
        // 2.项目不存在则创建
        CreateProjectReq req = new CreateProjectReq();
        req.setName(artifactoryConfig.getJobProject());
        req.setDisplayName(artifactoryConfig.getJobProject());
        String PROJECT_DESCRIPTION = "BlueKing bk-job official project, " +
            "which is used to save job data produced by users. " +
            "Do not delete me unless you know what you are doing";
        req.setDescription(PROJECT_DESCRIPTION);
        if (!createProjectIfNotExist(adminClient, req)) {
            return;
        } else {
            log.info(
                "project {} created",
                artifactoryConfig.getJobProject()
            );
        }
        // 3.用户不存在则创建
        CreateUserToProjectReq createUserToProjectReq = new CreateUserToProjectReq();
        createUserToProjectReq.setUserId(artifactoryConfig.getJobUsername());
        createUserToProjectReq.setName(artifactoryConfig.getJobUsername());
        createUserToProjectReq.setPwd(artifactoryConfig.getJobPassword());
        createUserToProjectReq.setAdmin(true);
        createUserToProjectReq.setProjectId(artifactoryConfig.getJobProject());
        if (!createUserToProjectIfNotExist(adminClient, createUserToProjectReq)) {
            return;
        } else {
            log.info(
                "user {} created",
                artifactoryConfig.getJobUsername()
            );
        }
        // 4.本地文件上传仓库不存在则创建
        CreateRepoReq createRepoReq = new CreateRepoReq();
        createRepoReq.setProjectId(artifactoryConfig.getJobProject());
        createRepoReq.setName(artifactoryConfig.getJobLocalUploadRepo());
        String REPO_LOCALUPLOAD_DESCRIPTION = "BlueKing bk-job official project localupload repo," +
            " which is used to save job data produced by users. " +
            "Do not delete me unless you know what you are doing";
        createRepoReq.setDescription(REPO_LOCALUPLOAD_DESCRIPTION);
        if (createRepoIfNotExist(adminClient, createRepoReq)) {
            log.info(
                "repo {} created",
                artifactoryConfig.getJobLocalUploadRepo()
            );
        }
    }

    private boolean checkProjectExist(ArtifactoryClient adminClient, String projectName) {
        List<ProjectDTO> projectDTOList = adminClient.listProject();
        for (ProjectDTO projectDTO : projectDTOList) {
            if (projectDTO.getName().equals(projectName)) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "project {} already exists, do not create again",
                        projectName
                    );
                }
                return true;
            }
        }
        return false;
    }

    private boolean createProjectIfNotExist(ArtifactoryClient adminClient, CreateProjectReq req) {
        boolean projectCreated = false;
        int retryCount = 0;
        do {
            try {
                if (checkProjectExist(adminClient, req.getName())) return true;
                projectCreated = adminClient.createProject(req);
            } catch (Exception e) {
                log.warn("Fail to create project {}, retry {} after 5 seconds", req.getName(), ++retryCount, e);
            }
        } while (!projectCreated && retryCount < 3);
        if (!projectCreated) {
            log.error("Fail to create project {} after retry {}", req.getName(), retryCount);
        }
        return projectCreated;
    }

    private boolean createUserToProjectIfNotExist(ArtifactoryClient adminClient, CreateUserToProjectReq req) {
        boolean projectUserCreated = false;
        int retryCount = 0;
        do {
            try {
                projectUserCreated = adminClient.createUserToProject(req);
            } catch (Exception e) {
                log.warn(
                    "Fail to create user {} to project {}, retry {} after 5 seconds",
                    req.getName(),
                    req.getProjectId(),
                    ++retryCount,
                    e
                );
            }
        } while (!projectUserCreated && retryCount < 3);
        if (!projectUserCreated) {
            log.error("Fail to create user {} to project {} after retry {}",
                req.getName(),
                req.getProjectId(),
                retryCount
            );
        }
        return projectUserCreated;
    }

    private boolean createRepoIfNotExist(ArtifactoryClient adminClient, CreateRepoReq req) {
        CheckRepoExistReq checkRepoExistReq = new CheckRepoExistReq();
        checkRepoExistReq.setProjectId(req.getProjectId());
        checkRepoExistReq.setRepoName(req.getName());
        boolean repoCreated = false;
        int retryCount = 0;
        do {
            try {
                if (adminClient.checkRepoExist(checkRepoExistReq)) return true;
                repoCreated = adminClient.createRepo(req);
            } catch (Exception e) {
                log.warn("Fail to create repo {} to project {}, retry {} after 5 seconds",
                    req.getName(),
                    req.getProjectId(),
                    ++retryCount,
                    e
                );
            }
        } while (!repoCreated && retryCount < 3);
        if (!repoCreated) {
            log.error("Fail to create repo {} to project {} after retry {}",
                req.getName(),
                req.getProjectId(),
                retryCount
            );
        }
        return repoCreated;
    }
}
