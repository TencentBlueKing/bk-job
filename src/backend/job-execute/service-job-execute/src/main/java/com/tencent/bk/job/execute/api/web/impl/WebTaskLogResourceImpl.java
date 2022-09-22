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

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.web.WebTaskLogResource;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LogExportConfig;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;

@RestController
@Slf4j
public class WebTaskLogResourceImpl implements WebTaskLogResource {
    private final String logFileDir;
    private final TaskInstanceService taskInstanceService;
    private final LogExportService logExportService;
    private final ArtifactoryClient artifactoryClient;
    private final ArtifactoryConfig artifactoryConfig;
    private final LogExportConfig logExportConfig;

    @Autowired
    public WebTaskLogResourceImpl(TaskInstanceService taskInstanceService,
                                  StorageSystemConfig storageSystemConfig,
                                  LogExportService logExportService,
                                  ArtifactoryClient artifactoryClient,
                                  ArtifactoryConfig artifactoryConfig,
                                  LogExportConfig logExportConfig) {
        this.taskInstanceService = taskInstanceService;
        this.logExportService = logExportService;
        this.logFileDir = NFSUtils.getFileDir(storageSystemConfig.getJobStorageRootPath(),
            FileDirTypeConf.JOB_INSTANCE_PATH);
        this.artifactoryClient = artifactoryClient;
        this.artifactoryConfig = artifactoryConfig;
        this.logExportConfig = logExportConfig;
    }

    private boolean existsLogZipFileInNFS(String zipFileName) {
        File zipFile = new File(logFileDir + zipFileName);
        boolean existsFlag = zipFile.exists();
        if (!existsFlag) {
            log.warn("Job info exist but file is gone!|{}", zipFileName);
        }
        return existsFlag;
    }

    private boolean existsLogZipFileInArtifactory(String zipFileName) {
        NodeDTO nodeDTO = null;
        try {
            nodeDTO = artifactoryClient.queryNodeDetail(
                artifactoryConfig.getArtifactoryJobProject(),
                logExportConfig.getLogExportRepo(),
                zipFileName
            );
        } catch (Throwable t) {
            log.warn("Fail to queryNodeDetail", t);
        }
        return nodeDTO != null;
    }

    @Override
    public Response<LogExportJobInfoVO> requestDownloadLogFile(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scopeType,
                                                               String scopeId,
                                                               Long stepInstanceId,
                                                               Long hostId,
                                                               String cloudIp,
                                                               Boolean repackage) {
        Long appId = appResourceScope.getAppId();

        if (repackage == null) {
            repackage = false;
        }

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (!stepInstance.getAppId().equals(appResourceScope.getAppId())) {
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        if (!repackage) {
            log.debug("Do not need repackage, check exist job");
            LogExportJobInfoDTO exportInfo = logExportService.getExportInfo(appId, stepInstanceId, hostId, cloudIp);
            if (exportInfo != null) {
                log.debug("Find exist job info|{}", exportInfo);
                switch (exportInfo.getStatus()) {
                    case INIT:
                    case PROCESSING:
                    case FAILED:
                        return Response.buildSuccessResp(LogExportJobInfoDTO.toVO(exportInfo));
                    case SUCCESS:
                        switch (logExportConfig.getStorageBackend()) {
                            case JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY:
                                repackage = !existsLogZipFileInArtifactory(exportInfo.getZipFileName());
                                break;
                            case JobConstants.FILE_STORAGE_BACKEND_LOCAL:
                                repackage = !existsLogZipFileInNFS(exportInfo.getZipFileName());
                                break;
                            default:
                                break;
                        }
                        // 不需要重新打包
                        if (!repackage) {
                            return Response.buildSuccessResp(LogExportJobInfoDTO.toVO(exportInfo));
                        }
                        break;
                    default:
                        throw new InternalException(ErrorCode.EXPORT_STEP_EXECUTION_LOG_FAIL);
                }
            }
        }

        int executeCount = stepInstance.getExecuteCount();

        String logFileName = getLogFileName(stepInstanceId, hostId, cloudIp, executeCount);
        if (StringUtils.isBlank(logFileName)) {
            throw new InternalException(ErrorCode.EXPORT_STEP_EXECUTION_LOG_FAIL);
        }

        LogExportJobInfoDTO exportInfo = logExportService.packageLogFile(username, appId, stepInstanceId, hostId, cloudIp,
            executeCount, logFileDir, logFileName, repackage);
        return Response.buildSuccessResp(LogExportJobInfoDTO.toVO(exportInfo));
    }

    private String getLogFileName(Long stepInstanceId, Long hostId, String cloudIp, int executeCount) {
        String fileName = makeExportLogFileName(stepInstanceId, executeCount, hostId, cloudIp);
        String logFileName = fileName + ".log";

        File dir = new File(logFileDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return logFileName;
    }

    private Pair<Long, StreamingResponseBody> getFileSizeAndStreamFromNFS(
        LogExportJobInfoDTO exportInfo
    ) throws FileNotFoundException {
        File zipFile = new File(logFileDir + exportInfo.getZipFileName());
        // 如果日志压缩文件已存在，直接返回
        if (zipFile.exists()) {
            StreamingResponseBody streamingResponseBody =
                outputStream -> {
                    try (FileInputStream fis = new FileInputStream(zipFile)) {
                        IOUtils.copy(fis, outputStream);
                    }
                };
            return Pair.of(zipFile.length(), streamingResponseBody);
        } else {
            log.warn("Job info exist but file is gone!|{}", exportInfo);
            throw new FileNotFoundException(zipFile.getAbsolutePath());
        }
    }

    private Pair<Long, StreamingResponseBody> getFileSizeAndStreamFromArtifactory(
        LogExportJobInfoDTO exportInfo
    ) {
        NodeDTO nodeDTO;
        InputStream ins;
        try {
            nodeDTO = artifactoryClient.queryNodeDetail(
                artifactoryConfig.getArtifactoryJobProject(),
                logExportConfig.getLogExportRepo(),
                exportInfo.getZipFileName()
            );
        } catch (Exception e) {
            throw new InternalException(ErrorCode.FAIL_TO_GET_NODE_INFO_FROM_ARTIFACTORY);
        }
        try {
            log.debug("get {} fileInputStream from artifactory", exportInfo.getZipFileName());
            Pair<InputStream, HttpRequestBase> pair = artifactoryClient.getFileInputStream(
                artifactoryConfig.getArtifactoryJobProject(),
                logExportConfig.getLogExportRepo(),
                exportInfo.getZipFileName()
            );
            ins = pair.getLeft();
        } catch (Exception e) {
            throw new InternalException(ErrorCode.FAIL_TO_DOWNLOAD_NODE_FROM_ARTIFACTORY);
        }
        final InputStream finalIns = ins;
        StreamingResponseBody streamingResponseBody =
            outputStream -> IOUtils.copy(finalIns, outputStream);
        return Pair.of(nodeDTO.getSize(), streamingResponseBody);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadLogFile(HttpServletResponse response,
                                                                 String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 Long stepInstanceId,
                                                                 Long hostId,
                                                                 String cloudIp) {
        Long appId = appResourceScope.getAppId();

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            log.info("StepInstance: {} is not in app: {}", stepInstance.getId(), appResourceScope.getAppId());
            return ResponseEntity.notFound().build();
        }

        int executeCount = stepInstance.getExecuteCount();

        LogExportJobInfoDTO exportInfo;

        boolean isGetByHost = hostId != null || StringUtils.isNotBlank(cloudIp);
        if (isGetByHost) {
            String logFileName = getLogFileName(stepInstanceId, hostId, cloudIp, executeCount);
            if (StringUtils.isBlank(logFileName)) {
                return ResponseEntity.notFound().build();
            }
            exportInfo = logExportService.packageLogFile(username, appId, stepInstanceId, hostId, cloudIp, executeCount,
                logFileDir, logFileName, false);
        } else {
            exportInfo = logExportService.getExportInfo(appId, stepInstanceId, hostId, cloudIp);
        }

        if (exportInfo != null) {
            log.debug("Find exist job info|{}", exportInfo);
            switch (exportInfo.getStatus()) {
                case INIT:
                case PROCESSING:
                case FAILED:
                    break;
                case SUCCESS:
                    Pair<Long, StreamingResponseBody> fileInfoPair;
                    switch (logExportConfig.getStorageBackend()) {
                        case JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY:
                            // 从制品库获取文件下载流
                            fileInfoPair = getFileSizeAndStreamFromArtifactory(exportInfo);
                            break;
                        case JobConstants.FILE_STORAGE_BACKEND_LOCAL:
                            try {
                                // 从NFS获取文件下载流
                                fileInfoPair = getFileSizeAndStreamFromNFS(exportInfo);
                            } catch (FileNotFoundException e) {
                                log.warn("log export file not found", e);
                                return ResponseEntity.notFound().build();
                            }
                            break;
                        default:
                            log.error("storage backend:{} not support yet", logExportConfig.getStorageBackend());
                            return ResponseEntity.notFound().build();
                    }
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exportInfo.getZipFileName() + "\""
                    );
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                    headers.add("Pragma", "no-cache");
                    headers.add("Expires", "0");
                    return ResponseEntity.ok().headers(headers).contentLength(fileInfoPair.getLeft())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileInfoPair.getRight());
                default:
            }
        }
        return ResponseEntity.notFound().build();
    }

    private String makeExportLogFileName(Long stepInstanceId, Integer executeCount, Long hostId, String cloudIp) {
        StringBuilder fileName = new StringBuilder();
        fileName.append("bk_job_export_log_");
        fileName.append("step_").append(stepInstanceId).append("_").append(executeCount).append("_");
        if (hostId != null) {
            fileName.append(hostId).append("_");
        } else if (StringUtils.isNotBlank(cloudIp)) {
            fileName.append(cloudIp).append("_");
        }
        fileName.append(DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS"));
        return fileName.toString();
    }

}
