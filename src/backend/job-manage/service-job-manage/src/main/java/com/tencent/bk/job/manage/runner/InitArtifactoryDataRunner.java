/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.manage.runner;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component("jobManageInitArtifactoryDataRunner")
public class InitArtifactoryDataRunner implements CommandLineRunner {

    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final ThreadPoolExecutor initRunnerExecutor;

    @Autowired
    public InitArtifactoryDataRunner(
        ArtifactoryConfig artifactoryConfig,
        LocalFileConfigForManage localFileConfigForManage,
        @Qualifier("initRunnerExecutor") ThreadPoolExecutor initRunnerExecutor
    ) {
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForManage = localFileConfigForManage;
        this.initRunnerExecutor = initRunnerExecutor;
    }

    @Override
    public void run(String... args) {
        initRunnerExecutor.submit(this::initArtifactoryData);
    }

    public void initArtifactoryData() {
        if (!JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(localFileConfigForManage.getStorageBackend())) {
            //不使用制品库作为后端存储时不初始化
            return;
        }
        String baseUrl = artifactoryConfig.getArtifactoryBaseUrl();
        String adminUsername = artifactoryConfig.getArtifactoryAdminUsername();
        String adminPassword = artifactoryConfig.getArtifactoryAdminPassword();
        String jobUsername = artifactoryConfig.getArtifactoryJobUsername();
        String jobPassword = artifactoryConfig.getArtifactoryJobPassword();
        String jobProject = artifactoryConfig.getArtifactoryJobProject();
        String localUploadRepo = localFileConfigForManage.getLocalUploadRepo();
        // 1.检查用户、仓库是否存在
        boolean userRepoExists = ArtifactoryHelper.checkRepoExists(
            baseUrl,
            jobUsername,
            jobPassword,
            jobProject,
            localUploadRepo
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
        // 3.本地文件上传仓库不存在则创建
        String REPO_LOCALUPLOAD_DESCRIPTION = "BlueKing bk-job official project localupload repo," +
            " which is used to save job data produced by users. " +
            "Do not delete me unless you know what you are doing";
        boolean repoCreated = ArtifactoryHelper.createRepoIfNotExist(
            baseUrl,
            adminUsername,
            adminPassword,
            jobProject,
            localUploadRepo,
            REPO_LOCALUPLOAD_DESCRIPTION
        );
        if (repoCreated) {
            log.info(
                "repo {} created",
                localUploadRepo
            );
        }
    }

}
