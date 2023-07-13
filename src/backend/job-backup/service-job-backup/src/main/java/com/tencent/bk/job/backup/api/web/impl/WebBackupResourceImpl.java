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
import com.tencent.bk.job.backup.config.ArtifactoryConfig;
import com.tencent.bk.job.backup.config.BackupStorageConfig;
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
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @since 21/7/2020 15:44
 */
@Slf4j
@RestController
public class WebBackupResourceImpl implements WebBackupResource {

    private final ImportJobService importJobService;
    private final ExportJobService exportJobService;
    private final LogService logService;
    private final StorageService storageService;
    private final ArtifactoryClient artifactoryClient;
    private final ArtifactoryConfig artifactoryConfig;
    private final BackupStorageConfig backupStorageConfig;

    @Autowired
    public WebBackupResourceImpl(ImportJobService importJobService,
                                 ExportJobService exportJobService,
                                 LogService logService,
                                 StorageService storageService,
                                 ArtifactoryClient artifactoryClient,
                                 ArtifactoryConfig artifactoryConfig,
                                 BackupStorageConfig backupStorageConfig) {
        this.importJobService = importJobService;
        this.exportJobService = exportJobService;
        this.logService = logService;
        this.storageService = storageService;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryConfig = artifactoryConfig;
        this.backupStorageConfig = backupStorageConfig;
    }

    @Override
    public Response<ExportInfoVO> startExport(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              ExportRequest exportRequest) {
        if (!exportRequest.validate()) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        ExportJobInfoDTO exportJobInfoDTO = new ExportJobInfoDTO();
        exportJobInfoDTO.setAppId(appResourceScope.getAppId());
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
        exportJobInfoDTO.setTemplateInfo(exportRequest.getTemplateInfo().stream()
            .map(BackupTemplateInfoDTO::fromVO).collect(Collectors.toList()));
        String id = exportJobService.startExport(exportJobInfoDTO);

        if (exportJobInfoDTO.getId().equals(id)) {
            ExportInfoVO exportInfoVO = new ExportInfoVO();
            exportInfoVO.setId(id);
            exportInfoVO.setStatus(BackupJobStatusEnum.INIT.getStatus());
            try {
                ExportJobExecutor.startExport(id);
            } catch (Exception e) {
                throw new InternalException("Start job failed! System busy!", e, ErrorCode.INTERNAL_ERROR);
            }
            return Response.buildSuccessResp(exportInfoVO);
        }
        throw new InternalException("Start failed!", ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Response<ExportInfoVO> getExportInfo(String username,
                                                AppResourceScope appResourceScope,
                                                String scopeType,
                                                String scopeId,
                                                String jobId) {
        Long appId = appResourceScope.getAppId();
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appId, jobId);
        if (exportInfo != null) {
            List<LogEntityDTO> exportLog = logService.getExportLogById(appId, jobId);
            ExportInfoVO exportInfoVO = ExportJobInfoDTO.toVO(exportInfo);
            exportInfoVO.setLog(exportLog.stream().map(LogEntityDTO::toVO).collect(Collectors.toList()));
            return Response.buildSuccessResp(exportInfoVO);
        }
        throw new InternalException("Not found", ErrorCode.INTERNAL_ERROR);
    }

    private Pair<Long, StreamingResponseBody> getFileSizeAndStreamFromNFS(
        String fileName) throws FileNotFoundException {
        File file = storageService.getFile(fileName);
        if (file.exists()) {
            StreamingResponseBody streamingResponseBody =
                outputStream -> {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        IOUtils.copy(fis, outputStream);
                    }
                };
            return Pair.of(file.length(), streamingResponseBody);
        } else {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
    }

    private Pair<Long, StreamingResponseBody> getFileSizeAndStreamFromArtifactory(String fileName) {
        NodeDTO nodeDTO;
        InputStream ins;
        try {
            nodeDTO = artifactoryClient.queryNodeDetail(
                artifactoryConfig.getArtifactoryJobProject(),
                backupStorageConfig.getBackupRepo(),
                fileName
            );
        } catch (Exception e) {
            throw new InternalException(ErrorCode.FAIL_TO_GET_NODE_INFO_FROM_ARTIFACTORY);
        }
        try {
            Pair<InputStream, HttpRequestBase> pair = artifactoryClient.getFileInputStream(
                artifactoryConfig.getArtifactoryJobProject(),
                backupStorageConfig.getBackupRepo(),
                fileName
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
    public ResponseEntity<StreamingResponseBody> getExportFile(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scopeType,
                                                               String scopeId,
                                                               String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appResourceScope.getAppId(), jobId);
        if (exportInfo != null) {
            if (BackupJobStatusEnum.ALL_SUCCESS.equals(exportInfo.getStatus())) {
                Pair<Long, StreamingResponseBody> fileInfoPair;
                switch (backupStorageConfig.getStorageBackend()) {
                    case JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY:
                        fileInfoPair = getFileSizeAndStreamFromArtifactory(exportInfo.getFileName());
                        break;
                    case JobConstants.FILE_STORAGE_BACKEND_LOCAL:
                        try {
                            fileInfoPair = getFileSizeAndStreamFromNFS(exportInfo.getFileName());
                        } catch (FileNotFoundException e) {
                            log.warn("export file not found", e);
                            return ResponseEntity.notFound().build();
                        }
                        break;
                    default:
                        log.error("storage backend:{} not support yet", backupStorageConfig.getStorageBackend());
                        return ResponseEntity.notFound().build();
                }

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportInfo.getPackageName());
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");
                return ResponseEntity.ok().headers(headers).contentLength(fileInfoPair.getLeft())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileInfoPair.getRight());
            }
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public Response<Boolean> completeExport(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appResourceScope.getAppId(), jobId);
        if (exportInfo != null) {
            if (BackupJobStatusEnum.ALL_SUCCESS.equals(exportInfo.getStatus())) {
                exportInfo.setStatus(BackupJobStatusEnum.FINISHED);
                exportInfo.setFileName(null);
                return Response.buildSuccessResp(exportJobService.updateExportJob(exportInfo));
            } else {
                throw new InternalException("Wrong job status", ErrorCode.INTERNAL_ERROR);
            }
        }
        throw new InternalException("Not found", ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Response<Boolean> abortExport(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(appResourceScope.getAppId(), jobId);
        if (exportInfo != null) {
            exportInfo.setStatus(BackupJobStatusEnum.CANCEL);
            exportInfo.setFileName(null);
            return Response.buildSuccessResp(exportJobService.updateExportJob(exportInfo));
        }
        throw new InternalException("Not found", ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Response<ImportInfoVO> getImportFileInfo(String username,
                                                    AppResourceScope appResourceScope,
                                                    String scopeType,
                                                    String scopeId,
                                                    MultipartFile uploadFile) {
        if (uploadFile.isEmpty()) {
            throw new InternalException("No File", ErrorCode.INTERNAL_ERROR);
        }
        Long appId = appResourceScope.getAppId();
        String originalFileName = uploadFile.getOriginalFilename();
        if (originalFileName != null && originalFileName.endsWith(Constant.JOB_EXPORT_FILE_SUFFIX)) {
            String id = UUID.randomUUID().toString();
            String fileName = importJobService.saveFile(username, appId, id, uploadFile);
            String jobId = importJobService.addImportJob(username, appId, id, fileName);
            if (id.equals(jobId)) {
                ImportInfoVO importInfoVO = new ImportInfoVO();
                importInfoVO.setId(jobId);
                importInfoVO.setStatus(BackupJobStatusEnum.INIT.getStatus());
                Boolean parseResult = importJobService.parseFile(username, appId, id);
                if (parseResult
                    && JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(
                    backupStorageConfig.getStorageBackend()
                )) {
                    // 上传至制品库
                    String filePath = "import"
                        + File.separator + username
                        + File.separator + id
                        + File.separator + originalFileName;
                    File file = new File(storageService.getStoragePath() + filePath);
                    try {
                        log.debug("begin to upload to artifactory:{}", filePath);
                        artifactoryClient.uploadGenericFile(
                            artifactoryConfig.getArtifactoryJobProject(),
                            backupStorageConfig.getBackupRepo(),
                            filePath,
                            file
                        );
                        log.debug("uploaded to artifactory:{}", filePath);
                    } catch (Exception e) {
                        log.error("Fail to save file to artifactory", e);
                        return Response.buildCommonFailResp(ErrorCode.INTERNAL_ERROR);
                    }
                }
                return Response.buildSuccessResp(importInfoVO);
            }
        } else {
            log.error("Upload unknown type of file!");
        }
        throw new InternalException("Upload failed! Unknown file type!", ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Response<Boolean> checkPassword(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           String jobId,
                                           CheckPasswordRequest passwordRequest) {
        Long appId = appResourceScope.getAppId();
        boolean lockResult =
            LockUtils.tryGetDistributedLock(getImportJobLockKey(appId, jobId), JobContextUtil.getRequestId(), 60_000L);
        if (lockResult) {
            try {
                return Response.buildSuccessResp(
                    importJobService.checkPassword(username, appId, jobId, passwordRequest.getPassword()));
            } catch (Exception e) {
                log.error("Error while check password!");
                return Response.buildCommonFailResp(ErrorCode.SERVICE_UNAVAILABLE);
            } finally {
                LockUtils.releaseDistributedLock(getImportJobLockKey(appId, jobId), JobContextUtil.getRequestId());
            }
        } else {
            log.warn("Acquire import job lock failed!|{}|{}", appId, jobId);
            return Response.buildCommonFailResp(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public Response<Boolean> startImport(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         String jobId,
                                         ImportRequest importRequest) {
        if (!importRequest.validate()) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        ImportJobInfoDTO importJobInfo = new ImportJobInfoDTO();
        importJobInfo.setId(jobId);
        importJobInfo.setCreator(username);
        importJobInfo.setAppId(appResourceScope.getAppId());
        if (CollectionUtils.isNotEmpty(importRequest.getTemplateInfo())) {
            importJobInfo.setTemplateInfo(importRequest.getTemplateInfo().stream().map(BackupTemplateInfoDTO::fromVO)
                .collect(Collectors.toList()));
        } else {
            throw new InternalException("No template selected!", ErrorCode.INTERNAL_ERROR);
        }
        importJobInfo.setDuplicateSuffix(importRequest.getDuplicateSuffix());
        importJobInfo.setDuplicateIdHandler(DuplicateIdHandlerEnum.valueOf(importRequest.getDuplicateIdHandler()));
        return Response.buildSuccessResp(importJobService.startImport(importJobInfo));
    }

    @Override
    public Response<ImportInfoVO> getImportInfo(String username,
                                                AppResourceScope appResourceScope,
                                                String scopeType,
                                                String scopeId,
                                                String jobId) {
        Long appId = appResourceScope.getAppId();
        ImportJobInfoDTO importInfo = importJobService.getImportInfoById(appId, jobId);
        if (importInfo != null) {
            List<LogEntityDTO> importLog = logService.getImportLogById(appId, jobId);
            ImportInfoVO importInfoVO = ImportJobInfoDTO.toVO(importInfo);
            importInfoVO.setLog(importLog.stream().map(LogEntityDTO::toVO).collect(Collectors.toList()));
            return Response.buildSuccessResp(importInfoVO);
        }
        throw new InternalException("Not Found", ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Response<BackupJobInfoVO> getCurrentJob(String username,
                                                   AppResourceScope appResourceScope,
                                                   String scopeType,
                                                   String scopeId) {
        Long appId = appResourceScope.getAppId();
        List<ExportJobInfoDTO> exportJobInfoList = exportJobService.getCurrentJobByUser(username, appId);
        List<ImportJobInfoDTO> importJobInfoList = importJobService.getCurrentJobByUser(username, appId);
        BackupJobInfoVO backupJobInfo = new BackupJobInfoVO();
        if (CollectionUtils.isNotEmpty(exportJobInfoList)) {
            backupJobInfo.setExportJob(
                exportJobInfoList.stream().map(ExportJobInfoDTO::toVO).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(importJobInfoList)) {
            backupJobInfo.setImportJob(
                importJobInfoList.stream().map(ImportJobInfoDTO::toVO).collect(Collectors.toList()));
        }
        return Response.buildSuccessResp(backupJobInfo);
    }

    private String getImportJobLockKey(Long appId, String jobId) {
        return "import" + appId + jobId;
    }

}
