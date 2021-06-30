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

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.web.controller.AbstractJobController;
import com.tencent.bk.job.execute.api.web.WebTaskLogResource;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.util.NFSUtils;
import com.tencent.bk.job.execute.model.LogExportJobInfoDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.web.vo.LogExportJobInfoVO;
import com.tencent.bk.job.execute.service.LogExportService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;

@RestController
@Slf4j
public class WebTaskLogResourceImpl extends AbstractJobController implements WebTaskLogResource {
    private final String logFileDir;
    private final MessageI18nService i18nService;
    private final TaskInstanceService taskInstanceService;
    private final LogExportService logExportService;

    @Autowired
    public WebTaskLogResourceImpl(MessageI18nService i18nService, TaskInstanceService taskInstanceService,
                                  StorageSystemConfig storageSystemConfig, LogExportService logExportService) {
        this.i18nService = i18nService;
        this.taskInstanceService = taskInstanceService;
        this.logExportService = logExportService;
        this.logFileDir = NFSUtils.getFileDir(storageSystemConfig.getJobStorageRootPath(),
            FileDirTypeConf.JOB_INSTANCE_PATH);
    }

    @Override
    public ServiceResponse<LogExportJobInfoVO> requestDownloadLogFile(String username, Long appId,
                                                                      Long stepInstanceId, String ip,
                                                                      Boolean repackage) {
        if (appId == null || appId <= 0 || stepInstanceId == null || stepInstanceId < 0) {
            log.warn("Check request param fail, appId={}, stepInstanceId={}", appId, stepInstanceId);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        if (repackage == null) {
            repackage = false;
        }

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.STEP_INSTANCE_NOT_EXIST, i18nService);
        }

        if (!repackage) {
            log.debug("Do not need repackage, check exist job");
            LogExportJobInfoDTO exportInfo = logExportService.getExportInfo(appId, stepInstanceId, ip);
            if (exportInfo != null) {
                log.debug("Find exist job info|{}", exportInfo);
                switch (exportInfo.getStatus()) {
                    case INIT:
                    case PROCESSING:
                    case FAILED:
                        return ServiceResponse.buildSuccessResp(LogExportJobInfoDTO.toVO(exportInfo));
                    case SUCCESS:
                        File zipFile = new File(logFileDir + exportInfo.getZipFileName());
                        // 如果日志文件已存在，直接返回
                        if (zipFile.exists()) {
                            return ServiceResponse.buildSuccessResp(LogExportJobInfoDTO.toVO(exportInfo));
                        } else {
                            log.warn("Job info exist but file is gone!|{}", exportInfo);
                            repackage = true;
                        }
                        break;
                    default:
                        return ServiceResponse.buildCommonFailResp(ErrorCode.EXPORT_STEP_EXECUTION_LOG_FAIL,
                            i18nService);
                }
            }
        }

        int executeCount = stepInstance.getExecuteCount();

        String logFileName = getLogFileName(stepInstanceId, ip, executeCount);
        if (StringUtils.isBlank(logFileName)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.EXPORT_STEP_EXECUTION_LOG_FAIL, i18nService);
        }

        LogExportJobInfoDTO exportInfo = logExportService.packageLogFile(username, appId, stepInstanceId, ip,
            executeCount, logFileDir, logFileName, repackage);
        return ServiceResponse.buildSuccessResp(LogExportJobInfoDTO.toVO(exportInfo));
    }

    private String getLogFileName(Long stepInstanceId, String ip, int executeCount) {
        String fileName = makeExportLogFileName(stepInstanceId, executeCount, ip);
        String logFileName = fileName + ".log";

        File dir = new File(logFileDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return logFileName;
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadLogFile(HttpServletResponse response, String username,
                                                                 Long appId, Long stepInstanceId, String ip) {
        if (appId == null || appId <= 0 || stepInstanceId == null || stepInstanceId < 0) {
            log.warn("Check request param fail, appId={}, stepInstanceId={}", appId, stepInstanceId);
            return ResponseEntity.notFound().build();
        }
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            return ResponseEntity.notFound().build();
        }

        int executeCount = stepInstance.getExecuteCount();

        LogExportJobInfoDTO exportInfo;

        boolean isGetByIp = StringUtils.isNotBlank(ip);
        if (isGetByIp) {
            String logFileName = getLogFileName(stepInstanceId, ip, executeCount);
            if (StringUtils.isBlank(logFileName)) {
                return ResponseEntity.notFound().build();
            }
            exportInfo = logExportService.packageLogFile(username, appId, stepInstanceId, ip, executeCount,
                logFileDir, logFileName, false);
        } else {
            exportInfo = logExportService.getExportInfo(appId, stepInstanceId, ip);
        }

        if (exportInfo != null) {
            log.debug("Find exist job info|{}", exportInfo);
            switch (exportInfo.getStatus()) {
                case INIT:
                case PROCESSING:
                case FAILED:
                    break;
                case SUCCESS:
                    File zipFile = new File(logFileDir + exportInfo.getZipFileName());
                    // 如果日志文件已存在，直接返回
                    if (zipFile.exists()) {
                        StreamingResponseBody streamingResponseBody =
                            outputStream -> {
                                try (FileInputStream fis = new FileInputStream(zipFile)) {
                                    IOUtils.copy(fis, outputStream);
                                }
                            };

                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile.getName() +
                            "\"");
                        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                        headers.add("Pragma", "no-cache");
                        headers.add("Expires", "0");

                        return ResponseEntity.ok().headers(headers).contentLength(zipFile.length())
                            .contentType(MediaType.APPLICATION_OCTET_STREAM).body(streamingResponseBody);
                    } else {
                        log.warn("Job info exist but file is gone!|{}", exportInfo);
                    }
                    break;
                default:
            }
        }
        return ResponseEntity.notFound().build();
    }

    private String makeExportLogFileName(Long stepInstanceId, Integer executeCount, String ip) {
        StringBuilder fileName = new StringBuilder();
        fileName.append("bk_job_export_log_");
        fileName.append("step_").append(stepInstanceId).append("_").append(executeCount).append("_");
        if (!StringUtils.isBlank(ip)) {
            fileName.append(ip).append("_");
        }
        fileName.append(DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS"));
        return fileName.toString();
    }

}
