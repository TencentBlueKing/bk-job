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

package com.tencent.bk.job.backup.api.web.impl;

import com.tencent.bk.job.backup.api.web.WebBackupResource;
import com.tencent.bk.job.backup.config.BkConfig;
import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.Constant;
import com.tencent.bk.job.backup.constant.DuplicateIdHandlerEnum;
import com.tencent.bk.job.backup.constant.SecretHandlerEnum;
import com.tencent.bk.job.backup.executor.ExportJobExecutor;
import com.tencent.bk.job.backup.model.dto.BackupTemplateInfoDTO;
import com.tencent.bk.job.backup.model.dto.ExportJobInfoDTO;
import com.tencent.bk.job.backup.model.dto.ImportJobInfoDTO;
import com.tencent.bk.job.backup.model.dto.LogEntityDTO;
import com.tencent.bk.job.backup.model.req.CheckPasswordRequest;
import com.tencent.bk.job.backup.model.req.ExportRequest;
import com.tencent.bk.job.backup.model.req.ImportRequest;
import com.tencent.bk.job.backup.model.web.BackupJobInfoVO;
import com.tencent.bk.job.backup.model.web.ExportInfoVO;
import com.tencent.bk.job.backup.model.web.ImportInfoVO;
import com.tencent.bk.job.backup.service.ExportJobService;
import com.tencent.bk.job.backup.service.ImportJobService;
import com.tencent.bk.job.backup.service.LogService;
import com.tencent.bk.job.backup.service.StorageService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.web.controller.AbstractJobController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @since 21/7/2020 15:44
 */
@Slf4j
@RestController
public class WebBackupResourceImpl extends AbstractJobController implements WebBackupResource {

    private final ImportJobService importJobService;
    private final ExportJobService exportJobService;
    private final LogService logService;
    private final BkConfig bkConfig;
    private final StorageService storageService;

    @Autowired
    public WebBackupResourceImpl(ImportJobService importJobService, ExportJobService exportJobService,
                                 LogService logService, BkConfig bkConfig, StorageService storageService,
                                 AuthService authService) {
        super(authService);
        this.importJobService = importJobService;
        this.exportJobService = exportJobService;
        this.logService = logService;
        this.bkConfig = bkConfig;
        this.storageService = storageService;
    }

    @Override
    public ServiceResponse<ExportInfoVO> startExport(String username, Long appId, ExportRequest exportRequest) {
        if (!exportRequest.validate()) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        ExportJobInfoDTO exportJobInfoDTO = new ExportJobInfoDTO();
        exportJobInfoDTO.setAppId(appId);
        exportJobInfoDTO.setCreator(username);
        if (exportRequest.getPackageName().endsWith(Constant.JOB_EXPORT_FILE_SUFFIX)) {
            exportJobInfoDTO.setPackageName(exportRequest.getPackageName());
        } else {
            exportJobInfoDTO.setPackageName(exportRequest.getPackageName().concat(Constant.JOB_EXPORT_FILE_SUFFIX));
        }
        exportJobInfoDTO.setSecretHandler(SecretHandlerEnum.valueOf(exportRequest.getSecretHandler()));
        exportJobInfoDTO.setStatus(BackupJobStatusEnum.INIT);
        exportJobInfoDTO.setPassword(exportRequest.getPassword());
        if (exportRequest.getExpireTime() > 0) {
            exportJobInfoDTO
                .setExpireTime(System.currentTimeMillis() + exportRequest.getExpireTime() * 24 * 60 * 60 * 1000L);
        } else {
            exportJobInfoDTO.setExpireTime(0L);
        }
        exportJobInfoDTO.setTemplateInfo(exportRequest.getTemplateInfo().parallelStream()
            .map(BackupTemplateInfoDTO::fromVO).collect(Collectors.toList()));
        String id = exportJobService.startExport(exportJobInfoDTO);

        if (exportJobInfoDTO.getId().equals(id)) {
            ExportInfoVO exportInfoVO = new ExportInfoVO();
            exportInfoVO.setId(id);
            exportInfoVO.setStatus(BackupJobStatusEnum.INIT.getStatus());
            try {
                ExportJobExecutor.startExport(id);
            } catch (Exception e) {
                return ServiceResponse.buildCommonFailResp("Start job failed! System busy!");
            }
            return ServiceResponse.buildSuccessResp(exportInfoVO);
        }

        return ServiceResponse.buildCommonFailResp("Start failed!");
    }

    @Override
    public ServiceResponse<ExportInfoVO> getExportInfo(String username, Long appId, String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appId, jobId);
        if (exportInfo != null) {
            List<LogEntityDTO> exportLog = logService.getExportLogById(appId, jobId);
            ExportInfoVO exportInfoVO = ExportJobInfoDTO.toVO(exportInfo);
            exportInfoVO.setLog(exportLog.stream().map(LogEntityDTO::toVO).collect(Collectors.toList()));
            return ServiceResponse.buildSuccessResp(exportInfoVO);
        }
        return ServiceResponse.buildCommonFailResp("Not found");
    }

    @Override
    public ResponseEntity<StreamingResponseBody> getExportFile(String username, Long appId, String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appId, jobId);
        if (exportInfo != null) {
            if (BackupJobStatusEnum.SUCCESS.equals(exportInfo.getStatus())) {
                File file = storageService.getFile(exportInfo.getFileName());
                if (file != null && file.exists()) {
                    StreamingResponseBody streamingResponseBody =
                        outputStream -> {
                            try (FileInputStream fis = new FileInputStream(file)) {
                                IOUtils.copy(fis, outputStream);
                            }
                        };

                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportInfo.getPackageName());
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                    headers.add("Pragma", "no-cache");
                    headers.add("Expires", "0");

                    return ResponseEntity.ok().headers(headers).contentLength(file.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM).body(streamingResponseBody);
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ServiceResponse<Boolean> completeExport(String username, Long appId, String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appId, jobId);
        if (exportInfo != null) {
            if (BackupJobStatusEnum.SUCCESS.equals(exportInfo.getStatus())) {
                exportInfo.setStatus(BackupJobStatusEnum.FINISHED);
                exportInfo.setFileName(null);
                return ServiceResponse.buildSuccessResp(exportJobService.updateExportJob(exportInfo));
            } else {
                return ServiceResponse.buildCommonFailResp("Wrong job status");
            }
        }
        return ServiceResponse.buildCommonFailResp("Not found");
    }

    @Override
    public ServiceResponse<Boolean> abortExport(String username, Long appId, String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appId, jobId);
        if (exportInfo != null) {
            exportInfo.setStatus(BackupJobStatusEnum.CANCEL);
            exportInfo.setFileName(null);
            return ServiceResponse.buildSuccessResp(exportJobService.updateExportJob(exportInfo));
        }
        return ServiceResponse.buildCommonFailResp("Not found");
    }

    @Override
    public ServiceResponse<ImportInfoVO> getImportFileInfo(String username, Long appId, MultipartFile uploadFile) {
        if (uploadFile.isEmpty()) {
            return ServiceResponse.buildCommonFailResp("No file");
        }
        String originalFileName = uploadFile.getOriginalFilename();
        if (originalFileName != null && originalFileName.endsWith(Constant.JOB_EXPORT_FILE_SUFFIX)) {
            String id = UUID.randomUUID().toString();
            String fileName = importJobService.saveFile(username, appId, id, uploadFile);
            String jobId = importJobService.addImportJob(username, appId, id, fileName);
            if (id.equals(jobId)) {
                ImportInfoVO importInfoVO = new ImportInfoVO();
                importInfoVO.setId(jobId);
                importInfoVO.setStatus(BackupJobStatusEnum.INIT.getStatus());
                importJobService.parseFile(username, appId, id);
                return ServiceResponse.buildSuccessResp(importInfoVO);
            }
        } else {
            log.error("Upload unknown type of file!");
            return ServiceResponse.buildCommonFailResp("Upload failed! Unknown file type!");
        }
        return ServiceResponse.buildCommonFailResp("Upload failed!");
    }

    @Override
    public ServiceResponse<Boolean> checkPassword(String username, Long appId, String jobId,
                                                  CheckPasswordRequest passwordRequest) {
        boolean lockResult =
            LockUtils.tryGetDistributedLock(getImportJobLockKey(appId, jobId), JobContextUtil.getRequestId(), 60_000L);
        if (lockResult) {
            try {
                return ServiceResponse.buildSuccessResp(
                    importJobService.checkPassword(username, appId, jobId, passwordRequest.getPassword()));
            } catch (Exception e) {
                log.error("Error while check password!");
                return ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_UNAVAILABLE);
            } finally {
                LockUtils.releaseDistributedLock(getImportJobLockKey(appId, jobId), JobContextUtil.getRequestId());
            }
        } else {
            log.warn("Acquire import job lock failed!|{}|{}", appId, jobId);
            return ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ServiceResponse<Boolean> startImport(String username, Long appId, String jobId,
                                                ImportRequest importRequest) {
        if (!importRequest.validate()) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        ImportJobInfoDTO importJobInfo = new ImportJobInfoDTO();
        importJobInfo.setId(jobId);
        importJobInfo.setCreator(username);
        importJobInfo.setAppId(appId);
        if (CollectionUtils.isNotEmpty(importRequest.getTemplateInfo())) {
            importJobInfo.setTemplateInfo(importRequest.getTemplateInfo().stream().map(BackupTemplateInfoDTO::fromVO)
                .collect(Collectors.toList()));
        } else {
            return ServiceResponse.buildCommonFailResp("No template selected!");
        }
        importJobInfo.setDuplicateSuffix(importRequest.getDuplicateSuffix());
        importJobInfo.setDuplicateIdHandler(DuplicateIdHandlerEnum.valueOf(importRequest.getDuplicateIdHandler()));
        return ServiceResponse.buildSuccessResp(importJobService.startImport(importJobInfo));
    }

    @Override
    public ServiceResponse<ImportInfoVO> getImportInfo(String username, Long appId, String jobId) {
        ImportJobInfoDTO importInfo = importJobService.getImportInfoById(appId, jobId);
        if (importInfo != null) {
            List<LogEntityDTO> importLog = logService.getImportLogById(appId, jobId);
            ImportInfoVO importInfoVO = ImportJobInfoDTO.toVO(importInfo);
            importInfoVO.setLog(importLog.stream().map(LogEntityDTO::toVO).collect(Collectors.toList()));
            return ServiceResponse.buildSuccessResp(importInfoVO);
        }
        return ServiceResponse.buildCommonFailResp("Not found");
    }

    @Override
    public ServiceResponse<BackupJobInfoVO> getCurrentJob(String username, Long appId) {
        List<ExportJobInfoDTO> exportJobInfoList = exportJobService.getCurrentJobByUser(username, appId);
        List<ImportJobInfoDTO> importJobInfoList = importJobService.getCurrentJobByUser(username, appId);
        BackupJobInfoVO backupJobInfo = new BackupJobInfoVO();
        if (CollectionUtils.isNotEmpty(exportJobInfoList)) {
            backupJobInfo.setExportJob(
                exportJobInfoList.parallelStream().map(ExportJobInfoDTO::toVO).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(importJobInfoList)) {
            backupJobInfo.setImportJob(
                importJobInfoList.parallelStream().map(ImportJobInfoDTO::toVO).collect(Collectors.toList()));
        }
        return ServiceResponse.buildSuccessResp(backupJobInfo);
    }

    private String getImportJobLockKey(Long appId, String jobId) {
        return "import" + appId + jobId;
    }

}
