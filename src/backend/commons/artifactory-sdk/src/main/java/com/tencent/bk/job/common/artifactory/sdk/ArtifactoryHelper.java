package com.tencent.bk.job.common.artifactory.sdk;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.exception.ProjectExistedException;
import com.tencent.bk.job.common.artifactory.exception.RepoNotFoundException;
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.req.CheckRepoExistReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateProjectReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateRepoReq;
import com.tencent.bk.job.common.artifactory.model.req.CreateUserToProjectReq;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tentent.bk.job.common.api.artifactory.IRealProjectNameStore;
import io.micrometer.core.instrument.util.StringUtils;
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
    private final ArtifactoryConfig artifactoryConfig;

    public ArtifactoryHelper(TenantEnvService tenantEnvService,
                             IRealProjectNameStore realProjectNameStore,
                             ArtifactoryConfig artifactoryConfig) {
        this.tenantEnvService = tenantEnvService;
        this.realProjectNameStore = realProjectNameStore;
        this.artifactoryConfig = artifactoryConfig;
    }

    /**
     * 阻塞等待存储服务准备就绪
     *
     * @param maxWaitSeconds 最大等待时间，单位：秒
     * @return 是否准备就绪
     */
    public boolean waitUntilStoreServiceReady(Integer maxWaitSeconds) {
        return realProjectNameStore.waitUntilStoreServiceReady(maxWaitSeconds);
    }

    /**
     * 获取Job实际使用的制品库项目名称
     *
     * @return 实际的项目名称
     */
    public String getJobRealProject() {
        if (!tenantEnvService.isTenantEnabled()) {
            // 非多租户环境，直接返回配置的项目名称
            return artifactoryConfig.getArtifactoryJobProject();
        }
        return realProjectNameStore.queryRealProjectName(JobConstants.SAVE_KEY_ARTIFACTORY_JOB_REAL_PROJECT);
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
        String jobProject
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
        if (!createProjectIfNotExist(adminClient, JobConstants.SAVE_KEY_ARTIFACTORY_JOB_REAL_PROJECT, req)) {
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
        String jobRealProject = getJobRealProject();
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
        } catch (RepoNotFoundException t) {
            return false;
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
                String realProjectName = adminClient.createProject(tenantId, req);
                if (StringUtils.isNotBlank(realProjectName)) {
                    projectCreated = true;
                    realProjectNameStore.saveRealProjectName(jobRealProjectSaveKey, realProjectName);
                } else {
                    log.warn("returned realProjectName is blank, unexpected, retry {}", ++retryCount);
                }
            } catch (ProjectExistedException e) {
                log.info("Project {} already existed, ignore to create", req.getName());
                projectCreated = true;
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
