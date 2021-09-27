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

package com.tencent.bk.job.manage.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class LocalFileConfigForManage {

    @Value("${local-file.storage-backend:local}")
    private String storageBackend;

    @Value("${local-file.artifactory.base-url:}")
    private String artifactoryBaseUrl;

    @Value("${local-file.artifactory.admin.username:admin}")
    private String artifactoryAdminUsername;

    @Value("${local-file.artifactory.admin.password:blueking}")
    private String artifactoryAdminPassword;

    @Value("${local-file.artifactory.job.username:bkjob}")
    private String artifactoryJobUsername;

    @Value("${local-file.artifactory.job.password:bkjob}")
    private String artifactoryJobPassword;

    @Value("${local-file.artifactory.job.project:bkjob}")
    private String artifactoryJobProject;

    @Value("${local-file.artifactory.job.repo.local-upload:localupload}")
    private String artifactoryJobLocalUploadRepo;

    public String getJobLocalUploadRootPath() {
        return artifactoryJobProject + "/" + artifactoryJobLocalUploadRepo;
    }
}
