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

package com.tencent.bk.job.backup.service.impl;

import com.tencent.bk.job.backup.dao.ExportJobDAO;
import com.tencent.bk.job.backup.executor.ExportJobExecutor;
import com.tencent.bk.job.backup.model.dto.ExportJobInfoDTO;
import com.tencent.bk.job.backup.service.ExportJobService;
import com.tencent.bk.job.backup.service.StorageService;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @since 28/7/2020 19:08
 */
@Slf4j
@Service
public class ExportJobServiceImpl implements ExportJobService {

    private final ExportJobDAO exportJobDAO;
    private final StorageService storageService;

    @Autowired
    public ExportJobServiceImpl(ExportJobDAO exportJobDAO, StorageService storageService) {
        this.exportJobDAO = exportJobDAO;
        this.storageService = storageService;
    }

    @Override
    public String startExport(ExportJobInfoDTO exportJobInfoDTO) {
        return exportJobDAO.insertExportJob(exportJobInfoDTO);
    }

    @Override
    public ExportJobInfoDTO getExportInfo(Long appId, String jobId) {
        return exportJobDAO.getExportJobById(appId, jobId);
    }

    @Override
    public List<ExportJobInfoDTO> getCurrentJobByUser(String username, Long appId) {
        return exportJobDAO.getExportJobByUser(appId, username);
    }

    @Override
    public Boolean updateExportJob(ExportJobInfoDTO exportInfo) {
        return exportJobDAO.updateExportJob(exportInfo);
    }

    @Override
    public void cleanFile() {
        String exportCleanLockKey = "export:file:clean";
        try {
            boolean lockResult =
                LockUtils.tryGetDistributedLock(exportCleanLockKey, JobContextUtil.getRequestId(), 60_000L);
            if (lockResult) {
                List<ExportJobInfoDTO> oldExportJob = exportJobDAO.listOldExportJob();
                if (CollectionUtils.isNotEmpty(oldExportJob)) {
                    for (ExportJobInfoDTO exportJobInfo : oldExportJob) {
                        File exportFileFolder =
                            storageService.getFile(ExportJobExecutor.getExportFilePrefix(exportJobInfo.getCreator(),
                                exportJobInfo.getId()));
                        log.debug("Cleaning file of job {}/{}|{}", exportJobInfo.getAppId(), exportJobInfo.getId(),
                            exportFileFolder);
                        if (exportFileFolder != null && exportFileFolder.exists()) {
                            FileUtils.deleteDirectory(exportFileFolder);
                        }
                        exportJobDAO.setCleanMark(exportJobInfo.getAppId(), exportJobInfo.getId());
                        log.debug("Cleaned export job {}/{}", exportJobInfo.getAppId(), exportJobInfo.getId());
                    }
                } else {
                    log.debug("No export job to clean.");
                }
            } else {
                log.warn("Acquire lock failed! Maybe another instance is running!");
            }
        } catch (Exception e) {
            log.error("Error while clean export file!", e);
        } finally {
            LockUtils.releaseDistributedLock(exportCleanLockKey, JobContextUtil.getRequestId());
        }
    }

}
