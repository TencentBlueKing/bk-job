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

package com.tencent.bk.job.backup.api.inner.impl;

import com.tencent.bk.job.backup.api.inner.ServiceArchiveResource;
import com.tencent.bk.job.backup.archive.JobExecuteArchiveManage;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.model.inner.ServiceArchiveDBRequest;
import com.tencent.bk.job.common.model.InternalResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@Profile("dev")
public class ServiceArchiveResourceImpl implements ServiceArchiveResource {

    private final JobExecuteArchiveManage jobExecuteArchiveManage;

    public ServiceArchiveResourceImpl(@Autowired(required = false) JobExecuteArchiveManage jobExecuteArchiveManage) {
        this.jobExecuteArchiveManage = jobExecuteArchiveManage;
    }

    @Override
    public InternalResponse<?> archive(ServiceArchiveDBRequest request) {
        log.info("Begin archive db, request: {}", request);
        ArchiveDBProperties archiveDBProperties = new ArchiveDBProperties();
        if (StringUtils.isNotEmpty(request.getMode())) {
            archiveDBProperties.setMode(request.getMode());
        } else {
            archiveDBProperties.setMode(ArchiveModeEnum.BACKUP_THEN_DELETE.getMode());
        }
        archiveDBProperties.setEnabled(true);
        archiveDBProperties.setKeepDays(request.getKeepDays());
        archiveDBProperties.setBatchInsertRowSize(request.getBatchInsertRowSize());
        archiveDBProperties.setDeleteRowLimit(request.getDeleteRowLimit());
        archiveDBProperties.setReadIdStepSize(request.getReadIdStepSize());
        archiveDBProperties.setReadRowLimit(request.getReadRowLimit());

        if (request.getTableConfigs() != null) {
            Map<String, ArchiveDBProperties.TableConfig> tableConfigMap = new HashMap<>();

            request.getTableConfigs().forEach((table, config) -> {
                ArchiveDBProperties.TableConfig tableConfig = new ArchiveDBProperties.TableConfig();
                tableConfig.setBatchInsertRowSize(config.getBatchInsertRowSize());
                tableConfig.setDeleteRowLimit(config.getDeleteRowLimit());
                tableConfig.setReadIdStepSize(config.getReadIdStepSize());
                tableConfig.setReadRowLimit(config.getReadRowLimit());
                tableConfigMap.put(table, tableConfig);
            });

            archiveDBProperties.setTableConfigs(tableConfigMap);
        }

        jobExecuteArchiveManage.archive(archiveDBProperties);
        return InternalResponse.buildSuccessResp(archiveDBProperties);
    }
}
