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

package com.tencent.bk.job.file.worker.config;

import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class WorkerConfig {

    @Value("${swagger.url:swagger.job.com}")
    private String swaggerUrl;

    @Value("${job.file-worker.version:0.0.1}")
    private String version;

    @Value("${job.file-worker.id:-1}")
    private Long id;

    @Value("${job.file-worker.name:anon}")
    private String name;

    @Value("${job.file-worker.token}")
    private String token;

    @Value("${job.file-worker.app-id}")
    private Long appId;

    @Value("${job.file-worker.ability-tags:}")
    private String abilityTagStr;

    @Value("${job.file-worker.access.host:}")
    private String accessHost;

    @Value("${job.file-worker.access.port:19810}")
    private Integer accessPort;

    @Value("${job.file-worker.cloud-area-id:0}")
    private Long cloudAreaId;

    @Value("${job.file-worker.inner-ip:}")
    private String innerIp;

    @Value("${job.file-worker.download-file.dir:/tmp/job}")
    private String downloadFileDir;

    @Value("${job.file-worker.download-file.expire-days:7}")
    private Integer downloadFileExpireDays;

    @Value("${job.file-gateway.api.root-url:http://api.job.com}")
    private String jobApiRootUrl;

    private String workSpaceName = "JobFileWorkerWorkspace";

    public String getWorkspaceDirPath() {
        return PathUtil.joinFilePath(getDownloadFileDir(), getWorkSpaceName());
    }

    public List<String> getAbilityTagList() {
        return StringUtil.strToList(abilityTagStr, String.class, "[,，|;；\n]");
    }
}
