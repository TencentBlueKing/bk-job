/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.artifactory.sdk;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class InitJobRepoProcess {

    private final ArtifactoryConfig artifactoryConfig;
    private final ArtifactoryHelper artifactoryHelper;

    /**
     * 仓库名称
     */
    private final String repoName;
    /**
     * 仓库描述
     */
    private final String repoDescription;

    public InitJobRepoProcess(ArtifactoryConfig artifactoryConfig,
                              ArtifactoryHelper artifactoryHelper,
                              String repoName,
                              String repoDescription) {
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryHelper = artifactoryHelper;
        this.repoName = repoName;
        this.repoDescription = repoDescription;
    }

    /**
     * 执行仓库初始化流程
     */
    public void execute() {
        log.info("InitJobRepoProcess start");
        try {
            doExecute();
        } catch (Exception e) {
            log.error("InitJobRepoProcess error", e);
        } finally {
            log.info("InitJobRepoProcess end");
        }
    }

    private void doExecute() {
        boolean storeServiceReady = artifactoryHelper.waitUntilStoreServiceReady(1800);
        if (!storeServiceReady) {
            log.error(
                "Store service to interact with Artifactory is not ready after 30 minutes, " +
                    "ignore InitJobRepoProcess"
            );
            return;
        }
        String baseUrl = artifactoryConfig.getArtifactoryBaseUrl();
        String adminUsername = artifactoryConfig.getArtifactoryAdminUsername();
        String adminPassword = artifactoryConfig.getArtifactoryAdminPassword();
        String jobUsername = artifactoryConfig.getArtifactoryJobUsername();
        String jobPassword = artifactoryConfig.getArtifactoryJobPassword();
        String jobProject = artifactoryConfig.getArtifactoryJobProject();
        String jobRealProject = artifactoryHelper.getJobRealProject();
        // 1.检查用户、仓库是否存在
        boolean userRepoExists = false;
        if (!StringUtils.isBlank(jobRealProject)) {
            userRepoExists = artifactoryHelper.checkRepoExists(
                baseUrl,
                jobUsername,
                jobPassword,
                jobRealProject,
                repoName
            );
        }
        if (userRepoExists) {
            log.info("Repo already exists");
            return;
        }
        // 2.创建项目与用户
        boolean projectUserCreated = artifactoryHelper.createJobUserAndProjectIfNotExists(
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
        jobRealProject = artifactoryHelper.getJobRealProject();
        if (StringUtils.isBlank(jobRealProject)) {
            log.warn("Cannot get real project name, use project name directly");
            jobRealProject = jobProject;
        }
        // 3.仓库不存在则创建
        boolean repoCreated = artifactoryHelper.createRepoIfNotExist(
            baseUrl,
            adminUsername,
            adminPassword,
            jobRealProject,
            repoName,
            repoDescription
        );
        if (repoCreated) {
            log.info(
                "repo {} created",
                repoName
            );
        }
    }
}
