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

import com.tencent.bk.job.backup.constant.LogEntityTypeEnum;
import com.tencent.bk.job.backup.constant.LogTypeEnum;
import com.tencent.bk.job.backup.dao.LogDAO;
import com.tencent.bk.job.backup.model.dto.LogEntityDTO;
import com.tencent.bk.job.backup.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @since 29/7/2020 11:10
 */
@Slf4j
@Service
public class LogServiceImpl implements LogService {

    private final LogDAO logDAO;

    @Autowired
    public LogServiceImpl(LogDAO logDAO) {
        this.logDAO = logDAO;
    }

    @Override
    public void addExportLog(Long appId, String jobId, String message) {
        doAddLog(appId, jobId, message, LogTypeEnum.EXPORT, LogEntityTypeEnum.NORMAL);
    }

    @Override
    public void addImportLog(Long appId, String jobId, String message) {
        doAddLog(appId, jobId, message, LogTypeEnum.IMPORT, LogEntityTypeEnum.NORMAL);
    }

    @Override
    public void addImportLog(Long appId, String jobId, String message, LogEntityTypeEnum entityType) {
        doAddLog(appId, jobId, message, LogTypeEnum.IMPORT, entityType);
    }

    private void doAddLog(Long appId, String jobId, String message, LogTypeEnum logType, LogEntityTypeEnum entityType) {
        LogEntityDTO logEntity = new LogEntityDTO();
        logEntity.setAppId(appId);
        logEntity.setJobId(jobId);
        logEntity.setContent(message);
        logEntity.setType(entityType);
        logDAO.insertLogEntity(logEntity, logType);
    }

    @Override
    public List<LogEntityDTO> getExportLogById(Long appId, String jobId) {
        return logDAO.listLogById(appId, jobId, LogTypeEnum.EXPORT);
    }

    @Override
    public List<LogEntityDTO> getImportLogById(Long appId, String jobId) {
        return logDAO.listLogById(appId, jobId, LogTypeEnum.IMPORT);
    }

    @Override
    public void addImportLog(Long appId, String jobId, String message, LogEntityTypeEnum type, long templateId,
                             long planId) {
        LogEntityDTO logEntity = new LogEntityDTO();
        logEntity.setAppId(appId);
        logEntity.setJobId(jobId);
        logEntity.setContent(message);
        logEntity.setType(type);
        logEntity.setTemplateId(templateId);
        if (planId > 0) {
            logEntity.setPlanId(planId);
        }
        logDAO.insertLogEntity(logEntity, LogTypeEnum.IMPORT);
    }
}
