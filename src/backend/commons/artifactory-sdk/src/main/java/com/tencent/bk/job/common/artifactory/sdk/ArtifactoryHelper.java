package com.tencent.bk.job.common.artifactory.sdk;

import com.tencent.bk.job.common.artifactory.constants.ArtifactoryInterfaceConsts;
import com.tencent.bk.job.common.artifactory.model.dto.ArtifactoryResp;
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.req.CheckRepoExistReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateProjectReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateRepoReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateUserToProjectReq;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tentent.bk.job.common.api.artifactory.IRealProjectNameStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于制品库相关操作：创建项目、仓库等
 */
@Slf4j
public class ArtifactoryHelper {

    private final TenantEnvService tenantEnvService;
    @Getter
    private final IRealProjectNameStore realProjectNameStore;

    public ArtifactoryHelper(TenantEnvService tenantEnvService, IRealProjectNameStore realProjectNameStore) {
        this.tenantEnvService = tenantEnvService;
        this.realProjectNameStore = realProjectNameStore;
    }

    /**
     * 向制品库注册Job用户、项目
     */
    public boolean createJobUserAndProjectIfNotExists(
        String baseUrl,
        String adminUsername,
        String adminPassword,
        String jobUsername,
        String jobPassword,
        String jobProject,
        String jobRealProjectSaveKey
    ) {
        ArtifactoryClient adminClient = new ArtifactoryClient(
            baseUrl,
            adminUsername,
            adminPassword,
            null
        );
        // 1.项目不存在则创建
        CreateProjectReq req = new CreateProjectReq();
        req.setName(jobProject);
        req.setDisplayName(jobProject);
        String PROJECT_DESCRIPTION = "BlueKing bk-job official project, " +
            "which is used to save job data produced by users. " +
            "Do not delete me unless you know what you are doing";
        req.setDescription(PROJECT_DESCRIPTION);
        if (!createProjectIfNotExist(adminClient, jobRealProjectSaveKey, req)) {
            return false;
        } else {
            log.info(
                "project {} created",
                jobProject
            );
        }
        // 2.用户不存在则创建
        CreateUserToProjectReq createUserToProjectReq = new CreateUserToProjectReq();
        createUserToProjectReq.setUserId(jobUsername);
        createUserToProjectReq.setName(jobUsername);
        createUserToProjectReq.setPwd(jobPassword);
        createUserToProjectReq.setAdmin(true);
        String jobRealProject = realProjectNameStore.queryRealProjectName(jobRealProjectSaveKey);
        createUserToProjectReq.setProjectId(jobRealProject);
        if (!createUserToProjectIfNotExist(adminClient, createUserToProjectReq)) {
            return false;
        } else {
            log.info(
                "user {} created",
                jobUsername
            );
        }
        return true;
    }

    /**
     * 检查仓库是否已存在
     */
    public boolean checkRepoExists(
        String baseUrl,
        String username,
        String password,
        String projectId,
        String repoName
    ) {
        ArtifactoryClient jobClient = new ArtifactoryClient(
            baseUrl,
            username,
            password,
            null
        );
        NodeDTO localUploadRepoRootNode;
        try {
            localUploadRepoRootNode = jobClient.queryNodeDetail(
                projectId,
                repoName,
                "/"
            );
            return localUploadRepoRootNode != null;
        } catch (Throwable t) {
            log.info("Fail to queryNodeDetail", t);
        }
        return false;
    }


    /**
     * 在Job项目下创建指定仓库（若不存在）
     */
    public boolean createRepoIfNotExist(
        String baseUrl,
        String username,
        String password,
        String projectId,
        String repoName,
        String repoDescription
    ) {

        ArtifactoryClient artifactoryClient = new ArtifactoryClient(
            baseUrl,
            username,
            password,
            null
        );
        CreateRepoReq createRepoReq = new CreateRepoReq();
        createRepoReq.setProjectId(projectId);
        createRepoReq.setName(repoName);
        createRepoReq.setDescription(repoDescription);
        CheckRepoExistReq checkRepoExistReq = new CheckRepoExistReq();
        checkRepoExistReq.setProjectId(projectId);
        checkRepoExistReq.setRepoName(repoName);
        boolean repoCreated = false;
        int retryCount = 0;
        do {
            try {
                if (artifactoryClient.checkRepoExist(checkRepoExistReq)) return true;
                repoCreated = artifactoryClient.createRepo(createRepoReq);
            } catch (Exception e) {
                log.warn("Fail to create repo {} to project {}, retry {} after 5 seconds",
                    createRepoReq.getName(),
                    createRepoReq.getProjectId(),
                    ++retryCount,
                    e
                );
                ThreadUtils.sleep(5000);
            }
        } while (!repoCreated && retryCount < 3);
        if (!repoCreated) {
            log.error("Fail to create repo {} to project {} after retry {}",
                createRepoReq.getName(),
                createRepoReq.getProjectId(),
                retryCount
            );
        }
        return repoCreated;
    }

    private boolean createProjectIfNotExist(ArtifactoryClient adminClient,
                                            String jobRealProjectSaveKey,
                                            CreateProjectReq req) {
        boolean projectCreated = false;
        int retryCount = 0;
        do {
            try {
                String tenantId = tenantEnvService.getTenantIdForArtifactoryBkJobProject();
                ArtifactoryResp<String> resp = adminClient.createProject(tenantId, req);
                int code = resp.getCode();
                if (code == ArtifactoryInterfaceConsts.RESULT_CODE_PROJECT_EXISTED) {
                    log.info("Project {} already existed, ignore to create", req.getName());
                    projectCreated = true;
                } else if (code == ArtifactoryInterfaceConsts.RESULT_CODE_OK) {
                    String realProjectName = resp.getData();
                    projectCreated = true;
                    realProjectNameStore.saveRealProjectName(jobRealProjectSaveKey, realProjectName);
                } else {
                    log.error(
                        "Fail to create project {}, code={}, message={}, retry {} after 5 seconds",
                        req.getName(),
                        code,
                        resp.getMessage(),
                        ++retryCount
                    );
                    ThreadUtils.sleep(5000);
                }
            } catch (Throwable t) {
                log.warn("Fail to create project {}, retry {} after 5 seconds", req.getName(), ++retryCount, t);
                ThreadUtils.sleep(5000);
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
            } catch (Throwable t) {
                log.warn(
                    "Fail to create user {} to project {}, retry {} after 5 seconds",
                    req.getName(),
                    req.getProjectId(),
                    ++retryCount,
                    t
                );
                ThreadUtils.sleep(5000);
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

}
