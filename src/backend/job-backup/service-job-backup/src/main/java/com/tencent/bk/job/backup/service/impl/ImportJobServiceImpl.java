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

import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.Constant;
import com.tencent.bk.job.backup.constant.DuplicateIdHandlerEnum;
import com.tencent.bk.job.backup.constant.LogEntityTypeEnum;
import com.tencent.bk.job.backup.constant.LogMessage;
import com.tencent.bk.job.backup.dao.ImportJobDAO;
import com.tencent.bk.job.backup.executor.ImportJobExecutor;
import com.tencent.bk.job.backup.model.dto.IdNameInfoDTO;
import com.tencent.bk.job.backup.model.dto.ImportJobInfoDTO;
import com.tencent.bk.job.backup.model.dto.JobBackupInfoDTO;
import com.tencent.bk.job.backup.service.ImportJobService;
import com.tencent.bk.job.backup.service.LogService;
import com.tencent.bk.job.backup.service.StorageService;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.common.util.file.ZipUtil;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * @since 28/7/2020 19:09
 */
@Slf4j
@Service
public class ImportJobServiceImpl implements ImportJobService {

    private static final String NOT_INIT = "__JOB_NOT_INIT__";
    private final ImportJobDAO importJobDAO;
    private final StorageService storageService;
    private final LogService logService;
    private final MessageI18nService i18nService;

    @Autowired
    public ImportJobServiceImpl(ImportJobDAO importJobDAO, StorageService storageService, LogService logService,
                                MessageI18nService i18nService) {
        this.importJobDAO = importJobDAO;
        this.storageService = storageService;
        this.logService = logService;
        this.i18nService = i18nService;
    }

    @Override
    public String addImportJob(String username, Long appId, String id, String fileName) {
        ImportJobInfoDTO importJobInfo = new ImportJobInfoDTO();
        importJobInfo.setId(id);
        importJobInfo.setAppId(appId);
        importJobInfo.setCreator(username);
        importJobInfo.setCreateTime(System.currentTimeMillis());
        importJobInfo.setUpdateTime(importJobInfo.getCreateTime());
        importJobInfo.setStatus(BackupJobStatusEnum.INIT);
        importJobInfo.setExportId(NOT_INIT);
        importJobInfo.setFileName(fileName);
        importJobInfo.setTemplateInfo(Collections.emptyList());
        importJobInfo.setDuplicateSuffix(NOT_INIT);
        importJobInfo.setDuplicateIdHandler(DuplicateIdHandlerEnum.AUTO_INCREMENT);
        importJobInfo.setIdNameInfo(null);
        return importJobDAO.insertImportJob(importJobInfo);
    }

    @Override
    public ImportJobInfoDTO getImportInfoById(Long appId, String jobId) {
        return importJobDAO.getImportJobById(appId, jobId);
    }

    @Override
    public List<ImportJobInfoDTO> getCurrentJobByUser(String username, Long appId) {
        return importJobDAO.getImportJobByUser(appId, username);
    }

    @Override
    public Boolean startImport(ImportJobInfoDTO importJobInfo) {
        ImportJobInfoDTO importJobInfoFromDb =
            importJobDAO.getImportJobById(importJobInfo.getAppId(), importJobInfo.getId());
        if (importJobInfoFromDb.getCreator().equals(importJobInfo.getCreator())
            && BackupJobStatusEnum.PARSE_SUCCESS == importJobInfoFromDb.getStatus()) {
            importJobInfo.setStatus(BackupJobStatusEnum.SUBMIT);
            Boolean startResult = importJobDAO.updateImportJobById(importJobInfo);
            if (startResult) {
                ImportJobExecutor.startImport(importJobInfo.getId());
            }
            return startResult;
        }
        return false;
    }

    @Override
    public Boolean parseFile(String username, Long appId, String jobId) {
        ImportJobInfoDTO importJob = importJobDAO.getImportJobById(appId, jobId);
        if (importJob != null) {
            logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.PROCESS_UPLOAD_FILE));
            File uploadFile = storageService.getFile(importJob.getFileName());
            if (uploadFile != null && uploadFile.exists()) {
                logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.DETECT_FILE_TYPE));
                if (!uploadFile.getName().endsWith(Constant.JOB_EXPORT_FILE_SUFFIX)
                    && !uploadFile.getName().endsWith(
                        Constant.JOB_EXPORT_FILE_SUFFIX.concat(Constant.JOB_IMPORT_DECRYPT_SUFFIX))) {
                    markJobFailed(importJob, i18nService.getI18n(LogMessage.WRONG_FILE_TYPE));
                    return false;
                }
                logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.CORRECT_FILE_TYPE));
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(uploadFile);
                    List<File> fileList = ZipUtil.unzip(uploadFile);
                    logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.EXTRACT_FILE_DATA));
                    boolean success = false;
                    for (File file : fileList) {
                        if (file.getName().endsWith(".json")) {
                            JobBackupInfoDTO jobBackupInfo = ImportJobExecutor.readJobBackupInfoFromFile(file);
                            if (jobBackupInfo != null) {
                                if (jobBackupInfo.getExpireTime() != 0
                                    && jobBackupInfo.getExpireTime() <= System.currentTimeMillis()) {
                                    markJobFailed(importJob, "作业已过期！");
                                }
                                importJob.setExportId(jobBackupInfo.getId());
                                importJob.setStatus(BackupJobStatusEnum.PARSE_SUCCESS);
                                importJob.setTemplateInfo(jobBackupInfo.getTemplateInfo());
                                importJob.setIdNameInfo(extractIdNameInfo(jobBackupInfo));
                                importJobDAO.updateImportJobById(importJob);
                                logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.EXTRACT_SUCCESS));
                                success = true;
                                break;
                            }
                        }
                    }
                    if (!success) {
                        markJobFailed(importJob, i18nService.getI18n(LogMessage.EXTRACT_FAILED));
                    }
                } catch (IOException | RuntimeException e) {
                    log.error("Error while unzip upload file", e);
                    if (detectAndRemoveMagic(appId, jobId, uploadFile)) {
                        importJob.setStatus(BackupJobStatusEnum.NEED_PASSWORD);
                        importJobDAO.updateImportJobById(importJob);
                        logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.FILE_ENCRYPTED),
                            LogEntityTypeEnum.REQUEST_PASSWORD);
                    } else {
                        markJobFailed(importJob, i18nService.getI18n(LogMessage.EXTRACT_FAILED));
                    }
                } finally {
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException e) {
                            log.warn("Error when close", e);
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean detectAndRemoveMagic(Long appId, String jobId, File uploadFile) {
        boolean success = false;
        File tmpFile = new File(uploadFile.getPath().concat(".tmp"));
        try (FileInputStream in = new FileInputStream(uploadFile); FileOutputStream out =
            new FileOutputStream(tmpFile)) {
            byte[] buffer = new byte[Constant.JOB_FILE_MAGIC.length];
            int readBytes = IOUtils.read(in, buffer);
            if (readBytes < buffer.length) {
                log.warn("Invalid file head: readBytes {}", readBytes);
            }
            if (Arrays.equals(Constant.JOB_FILE_MAGIC, buffer)) {
                IOUtils.copy(in, out);
                success = true;
            }
        } catch (IOException e) {
            log.error("Error while process file magic!", e);
        }
        if (success) {
            if (tmpFile.exists()) {
                FileUtils.deleteQuietly(uploadFile);
                try {
                    FileUtils.moveFile(tmpFile, uploadFile);
                    return true;
                } catch (IOException e) {
                    log.error("Error while process file magic!");
                }
            }
        }
        return false;
    }

    private IdNameInfoDTO extractIdNameInfo(JobBackupInfoDTO jobBackupInfo) {
        if (jobBackupInfo == null) {
            return null;
        }
        IdNameInfoDTO idNameInfoDTO = new IdNameInfoDTO();
        Map<Long, String> templateNameMap = new HashMap<>();
        Map<Long, String> planNameMap = new HashMap<>();
        if (MapUtils.isNotEmpty(jobBackupInfo.getTemplateDetailInfoMap())) {
            for (Map.Entry<Long, TaskTemplateVO> templateEntry : jobBackupInfo.getTemplateDetailInfoMap().entrySet()) {
                templateNameMap.put(templateEntry.getKey(), templateEntry.getValue().getName());
            }
        }
        if (MapUtils.isNotEmpty(jobBackupInfo.getPlanDetailInfoMap())) {
            for (Map.Entry<Long, TaskPlanVO> planEntry : jobBackupInfo.getPlanDetailInfoMap().entrySet()) {
                planNameMap.put(planEntry.getKey(), planEntry.getValue().getName());
            }
        }
        idNameInfoDTO.setTemplateNameMap(templateNameMap);
        idNameInfoDTO.setPlanNameMap(planNameMap);
        return idNameInfoDTO;
    }

    @Override
    public String saveFile(String username, Long appId, String id, MultipartFile file) {
        String fileName = storageService.store(id, "import" + File.separatorChar + username, file);
        if (StringUtils.isEmpty(fileName)) {
            log.error("Save file failed!");
            return null;
        }
        return fileName;
    }

    @Override
    public Boolean checkPassword(String username, Long appId, String jobId, String password) {
        ImportJobInfoDTO importInfo = importJobDAO.getImportJobById(appId, jobId);
        if (BackupJobStatusEnum.NEED_PASSWORD == importInfo.getStatus()
            || BackupJobStatusEnum.WRONG_PASSWORD == importInfo.getStatus()) {
            String fileName = importInfo.getFileName();
            File uploadFile = storageService.getFile(fileName);
            if (uploadFile != null && uploadFile.exists()) {
                String parentPath = uploadFile.getParent();
                if (parentPath == null) {
                    parentPath = "";
                }
                File decryptedFile =
                    new File(parentPath.concat(File.separatorChar + uploadFile.getName()
                        + Constant.JOB_IMPORT_DECRYPT_SUFFIX));
                try {
                    AESUtils.decrypt(uploadFile, decryptedFile, password);
                    logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.CORRECT_PASSWORD));
                    FileUtils.deleteQuietly(uploadFile);
                    importInfo.setFileName(importInfo.getFileName().concat(Constant.JOB_IMPORT_DECRYPT_SUFFIX));
                    importJobDAO.updateImportJobById(importInfo);
                    parseFile(username, appId, jobId);
                    return true;
                } catch (Exception e) {
                    logService.addImportLog(appId, jobId, i18nService.getI18n(LogMessage.WRONG_PASSWORD),
                        LogEntityTypeEnum.RETRY_PASSWORD);
                    importInfo.setStatus(BackupJobStatusEnum.WRONG_PASSWORD);
                    importJobDAO.updateImportJobById(importInfo);
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public Boolean updateImportJob(ImportJobInfoDTO importJob) {
        return importJobDAO.updateImportJobById(importJob);
    }

    @Override
    public void markJobFailed(ImportJobInfoDTO importJob, String message) {
        importJob.setStatus(BackupJobStatusEnum.FAILED);
        importJobDAO.updateImportJobById(importJob);
        logService.addImportLog(importJob.getAppId(), importJob.getId(), message, LogEntityTypeEnum.ERROR);
    }

    @Override
    public void cleanFile() {
        String importCleanLockKey = "import:file:clean";
        try {
            boolean lockResult =
                LockUtils.tryGetDistributedLock(importCleanLockKey,
                    JobContextUtil.getRequestId(), 60_000L);
            if (lockResult) {
                List<ImportJobInfoDTO> oldImportJob = importJobDAO.listOldImportJob();
                if (CollectionUtils.isNotEmpty(oldImportJob)) {
                    for (ImportJobInfoDTO importJobInfo : oldImportJob) {
                        File importFileFolder = storageService.getFile(importJobInfo.getFileName()).getParentFile();
                        log.debug("Cleaning file of job {}/{}|{}", importJobInfo.getAppId(), importJobInfo.getId(),
                            importFileFolder);
                        if (importFileFolder != null && importFileFolder.exists()) {
                            FileUtils.deleteDirectory(importFileFolder);
                        }
                        importJobDAO.setCleanMark(importJobInfo.getAppId(), importJobInfo.getId());
                        log.debug("Cleaned import job {}/{}", importJobInfo.getAppId(), importJobInfo.getId());
                    }
                } else {
                    log.debug("No import job to clean.");
                }
            } else {
                log.warn("Acquire lock failed! Maybe another instance is running!");
            }
        } catch (Exception e) {
            log.error("Error while clean import file!", e);
        } finally {
            LockUtils.releaseDistributedLock(importCleanLockKey, JobContextUtil.getRequestId());
        }
    }

}
