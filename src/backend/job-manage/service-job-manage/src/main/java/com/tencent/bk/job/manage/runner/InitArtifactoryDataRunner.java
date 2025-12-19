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

package com.tencent.bk.job.manage.runner;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.artifactory.sdk.InitJobRepoProcess;
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
    private final ArtifactoryHelper artifactoryHelper;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final ThreadPoolExecutor initRunnerExecutor;

    @Autowired
    public InitArtifactoryDataRunner(
        ArtifactoryConfig artifactoryConfig,
        ArtifactoryHelper artifactoryHelper,
        LocalFileConfigForManage localFileConfigForManage,
        @Qualifier("initRunnerExecutor") ThreadPoolExecutor initRunnerExecutor
    ) {
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryHelper = artifactoryHelper;
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
        String localUploadRepo = localFileConfigForManage.getLocalUploadRepo();
        String repoDescription = "BlueKing bk-job official project localupload repo," +
            " which is used to save job data produced by users. " +
            "Do not delete me unless you know what you are doing";
        new InitJobRepoProcess(
            artifactoryConfig,
            artifactoryHelper,
            localUploadRepo,
            repoDescription
        ).execute();
    }

}
