/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.backup.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.backup.config.ArtifactoryConfig;
import com.tencent.bk.job.backup.config.BackupStorageConfig;
import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.Constant;
import com.tencent.bk.job.backup.constant.DuplicateIdHandlerEnum;
import com.tencent.bk.job.backup.constant.LogEntityTypeEnum;
import com.tencent.bk.job.backup.constant.LogMessage;
import com.tencent.bk.job.backup.model.dto.BackupTemplateInfoDTO;
import com.tencent.bk.job.backup.model.dto.ImportJobInfoDTO;
import com.tencent.bk.job.backup.model.dto.JobBackupInfoDTO;
import com.tencent.bk.job.backup.model.dto.TemplateIdMapDTO;
import com.tencent.bk.job.backup.service.AccountService;
import com.tencent.bk.job.backup.service.ImportJobService;
import com.tencent.bk.job.backup.service.LogService;
import com.tencent.bk.job.backup.service.ScriptService;
import com.tencent.bk.job.backup.service.StorageService;
import com.tencent.bk.job.backup.service.TaskPlanService;
import com.tencent.bk.job.backup.service.TaskTemplateService;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.util.file.FileUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.common.util.file.ZipUtil;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileDestinationInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileSourceInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileStepVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskScriptStepVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskStepVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskVariableVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @since 30/7/2020 15:47
 */
@Slf4j
@Service
public class ImportJobExecutor {
    private static final LinkedBlockingQueue<String> IMPORT_JOB_QUEUE = new LinkedBlockingQueue<>(100);
    private static final String LOG_HR = "************************************************************";
    private final ImportJobService importJobService;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final ScriptService scriptService;
    private final AccountService accountService;
    private final LogService logService;
    private final StorageService storageService;
    private final MessageI18nService i18nService;
    private final ArtifactoryClient artifactoryClient;
    private final ArtifactoryConfig artifactoryConfig;
    private final BackupStorageConfig backupStorageConfig;

    @Autowired
    public ImportJobExecutor(ImportJobService importJobService, TaskTemplateService taskTemplateService,
                             TaskPlanService taskPlanService, ScriptService scriptService,
                             AccountService accountService, LogService logService,
                             StorageService storageService, MessageI18nService i18nService,
                             ArtifactoryClient artifactoryClient,
                             ArtifactoryConfig artifactoryConfig, BackupStorageConfig backupStorageConfig) {
        this.importJobService = importJobService;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.scriptService = scriptService;
        this.accountService = accountService;
        this.logService = logService;
        this.storageService = storageService;
        this.i18nService = i18nService;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryConfig = artifactoryConfig;
        this.backupStorageConfig = backupStorageConfig;

        ImportJobExecutor.ImportJobExecutorThread importJobExecutorThread =
            new ImportJobExecutor.ImportJobExecutorThread();
        importJobExecutorThread.start();
    }

    public static JobBackupInfoDTO readJobBackupInfoFromFile(File file) {
        JobBackupInfoDTO jobBackupInfo;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            byte[] contentBytes = new byte[fileInputStream.available()];
            int readBytes = fileInputStream.read(contentBytes);
            if (readBytes < 0) {
                log.error("cannot read any bytes from file");
                return null;
            }
            String json = new String(contentBytes, StandardCharsets.UTF_8);
            jobBackupInfo = JsonUtils.fromJson(json, JobBackupInfoDTO.class);
            return jobBackupInfo;
        } catch (Exception e) {
            log.error("Error while parsing upload file!", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("Close file input stream failed!", e);
                }
            }
        }
        return null;
    }

    public static void startImport(String id) {
        try {
            IMPORT_JOB_QUEUE.add(id);
        } catch (Exception e) {
            log.error("Import job queue is full!");
            throw e;
        }
    }

    private void processImportJob(String jobId) {
        ImportJobInfoDTO importJob = importJobService.getImportInfoById(-1L, jobId);
        if (importJob != null) {
            LocaleContextHolder.setLocale(importJob.getLocale());
            importJob.setStatus(BackupJobStatusEnum.PROCESSING);
            importJobService.updateImportJob(importJob);
            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                i18nService.getI18n(LogMessage.START_IMPORT));
            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                i18nService.getI18n(LogMessage.IMPORT_SETTING));
            switch (importJob.getDuplicateIdHandler()) {
                case AUTO_INCREMENT:
                    logService.addImportLog(importJob.getAppId(), importJob.getId(),
                        i18nService.getI18n(LogMessage.ID_AUTO_INCREMENT));
                    break;
                case ON_DUPLICATE_INCREMENT:
                    logService.addImportLog(importJob.getAppId(), importJob.getId(),
                        i18nService.getI18n(LogMessage.ID_KEEP_ON_DUPLICATE_INCREMENT));
                    break;
                case ON_DUPLICATE_SKIP:
                    logService.addImportLog(importJob.getAppId(), importJob.getId(),
                        i18nService.getI18n(LogMessage.ID_KEEP_ON_DUPLICATE_SKIP));
                    break;
            }
            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                String.format(i18nService.getI18n(LogMessage.NAME_DUPLICATE_SUFFIX), importJob.getDuplicateSuffix()));

            File importFileDirectory = getImportFileDirectory(importJob);
            // 下载文件
            if (!importFileDirectory.exists()) {
                log.debug("begin to download from artifactory:{}", importJob.getFileName());
                Pair<InputStream, HttpRequestBase> pair = artifactoryClient.getFileInputStream(
                    artifactoryConfig.getArtifactoryJobProject(),
                    backupStorageConfig.getBackupRepo(),
                    importJob.getFileName()
                );
                InputStream ins = pair.getLeft();
                String localPath = PathUtil.joinFilePath(
                    storageService.getStoragePath(),
                    importJob.getFileName()
                );
                try {
                    FileUtil.writeInsToFile(ins, localPath);
                    log.debug("end to download from artifactory:{}", importJob.getFileName());
                } catch (InterruptedException e) {
                    String msg = "Fail to write artifactory file to local";
                    log.warn(msg, e);
                    importJobService.markJobAllFailed(importJob, msg);
                    return;
                }
                // 解压
                log.debug("local path:{}", localPath);
                File uploadFile = new File(localPath);
                List<File> fileList = ZipUtil.unzip(uploadFile);
                log.debug("unzipped:{} files", fileList.size());
            }
            if (importFileDirectory.exists() && importFileDirectory.isDirectory()) {
                File[] importFileList = importFileDirectory.listFiles();
                if (importFileList != null && importFileList.length > 0) {
                    for (File file : importFileList) {
                        log.debug("process {}", file.getAbsolutePath());
                        if (processImportFile(file, importJob)) return;
                    }
                } else {
                    log.warn("Import file not found");
                    importJobService.markJobAllFailed(importJob, i18nService.getI18n(LogMessage.EXTRACT_FAILED));
                }
            } else {
                importJobService.markJobAllFailed(importJob, i18nService.getI18n(LogMessage.CANNOT_FIND_IMPORT_FILE));
            }
        }
    }

    private boolean processImportFile(File file, ImportJobInfoDTO importJob) {
        if (!file.getName().endsWith(".json")) {
            return false;
        }
        JobBackupInfoDTO jobBackupInfo = readJobBackupInfoFromFile(file);
        if (jobBackupInfo == null) {
            importJobService.markJobAllFailed(importJob,
                i18nService.getI18n(LogMessage.EXTRACT_FAILED));
            return false;
        }
        try {
            return importJobBackupInfo(importJob, jobBackupInfo);
        } catch (Exception e) {
            log.error("Error while process import job!|{}|{}", importJob.getAppId(),
                importJob.getId(), e);
            if (e instanceof ServiceException) {
                if (ErrorCode.PERMISSION_DENIED == ((ServiceException) e).getErrorCode()) {
                    logService.addImportLog(importJob.getAppId(), importJob.getId(),
                        i18nService.getI18n(String.valueOf(((ServiceException) e).getErrorCode())),
                        LogEntityTypeEnum.ERROR);
                }
            }
            importJobService.markJobAllFailed(importJob,
                i18nService.getI18n(LogMessage.IMPORT_FAILED));
        }
        return false;
    }

    private boolean importJobBackupInfo(ImportJobInfoDTO importJob, JobBackupInfoDTO jobBackupInfo) {
        processAccount(importJob, jobBackupInfo);
        List<BackupTemplateInfoDTO> templateInfo = importJob.getTemplateInfo();
        if (CollectionUtils.isEmpty(templateInfo)) {
            setImportJobResult(importJob, BackupJobStatusEnum.ALL_SUCCESS);
            return true;
        }
        int totalTemplateNum = templateInfo.size();
        int failedTemplateNum = 0;
        for (BackupTemplateInfoDTO backupTemplateInfo : templateInfo) {
            boolean success = processBackupTemplateInfo(
                backupTemplateInfo,
                importJob,
                jobBackupInfo
            );
            if (!success) {
                failedTemplateNum += 1;
            }
        }
        if (failedTemplateNum == 0) {
            setImportJobResult(importJob, BackupJobStatusEnum.ALL_SUCCESS);
            return true;
        } else if (failedTemplateNum < totalTemplateNum) {
            setImportJobResult(importJob, BackupJobStatusEnum.PARTIAL_FAILED);
            return true;
        } else {
            setImportJobResult(importJob, BackupJobStatusEnum.ALL_FAILED);
            return false;
        }
    }

    /**
     * 导入单个作业模板及对应的执行方案
     *
     * @param backupTemplateInfo 作业模板信息
     * @param importJob          导入任务信息
     * @param jobBackupInfo      整体导入数据信息
     * @return 是否导入成功
     */
    private boolean processBackupTemplateInfo(BackupTemplateInfoDTO backupTemplateInfo,
                                              ImportJobInfoDTO importJob,
                                              JobBackupInfoDTO jobBackupInfo) {
        long templateId = backupTemplateInfo.getId();
        TaskTemplateVO oldTemplate = JsonUtils.fromJson(
            JsonMapper.getAllOutPutMapper().toJson(jobBackupInfo.getTemplateDetailInfoMap().get(templateId)),
            new TypeReference<TaskTemplateVO>() {
            }
        );
        Pair<Boolean, TaskTemplateVO> pair = processTemplate(importJob, jobBackupInfo, templateId);
        Boolean successImportedOrSkipped = pair.getLeft();
        TaskTemplateVO newTemplate = pair.getRight();
        if (!successImportedOrSkipped) {
            return false;
        }
        if (newTemplate == null) {
            return true;
        }
        TemplateIdMapDTO templateIdMap = processTemplateIdMap(oldTemplate, newTemplate);
        if (CollectionUtils.isEmpty(backupTemplateInfo.getPlanId())) {
            return true;
        }
        int failedPlanNum = 0;
        for (Long planId : backupTemplateInfo.getPlanId()) {
            try {
                boolean success = processPlan(importJob, jobBackupInfo, templateIdMap, newTemplate.getId(), planId);
                failedPlanNum += success ? 0 : 1;
            } catch (Exception e) {
                failedPlanNum += 1;
                FormattingTuple msg = MessageFormatter.arrayFormat(
                    "Fail to import plan {} of template (id={},name={})",
                    new Object[]{
                        planId,
                        newTemplate.getId(),
                        newTemplate.getName()
                    }
                );
                log.warn(msg.getMessage(), e);
            }
        }
        if (failedPlanNum > 0) {
            log.warn(
                "Fail to import {}/{} plan of template (id={},name={})",
                failedPlanNum,
                backupTemplateInfo.getPlanId().size(),
                newTemplate.getId(),
                newTemplate.getName()
            );
            return false;
        }
        return true;
    }

    private void setImportJobResult(ImportJobInfoDTO importJob, BackupJobStatusEnum status) {
        importJob.setStatus(status);
        importJobService.updateImportJob(importJob);
        logService.addImportLog(importJob.getAppId(), importJob.getId(), LOG_HR);
        logService.addImportLog(importJob.getAppId(), importJob.getId(),
            i18nService.getI18n(LogMessage.IMPORT_FINISHED), LogEntityTypeEnum.FINISHED);
    }

    private void processAccount(ImportJobInfoDTO importJob, JobBackupInfoDTO jobBackupInfo) {
        Map<Long, Long> finalAccountIdMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(jobBackupInfo.getAccountList())) {
            List<ServiceAccountDTO> appAccountList = accountService.listAccountByAppId(importJob.getCreator(),
                importJob.getAppId());
            Map<Integer, Map<String, Long>> categoryAliasToAccountIdMap = new ConcurrentHashMap<>();
            appAccountList.forEach(account -> categoryAliasToAccountIdMap
                .computeIfAbsent(account.getCategory(), k -> new ConcurrentHashMap<>())
                .put(account.getAlias(), account.getId())
            );
            Set<ServiceAccountDTO> noExistAccountSet = new LinkedHashSet<>();
            for (ServiceAccountDTO account : jobBackupInfo.getAccountList()) {
                if (AccountCategoryEnum.DB.getValue().equals(account.getCategory())) {
                    // DB account process related system account first
                    doProcessAccount(importJob, finalAccountIdMap, categoryAliasToAccountIdMap,
                        account.getDbSystemAccount(), noExistAccountSet);
                    if (finalAccountIdMap.get(account.getDbSystemAccount().getId()) == null) {
                        log.warn("The system account associated with the database was not found!|{}|{}|{}|{}",
                            importJob.getCreator(), account.getAppId(), account.getAlias(),
                            account.getDbSystemAccount().getAlias());
                    } else {
                        account.getDbSystemAccount().setId(finalAccountIdMap.get(account.getDbSystemAccount().getId()));
                    }
                }
                doProcessAccount(importJob, finalAccountIdMap, categoryAliasToAccountIdMap, account,
                    noExistAccountSet);
            }
            if (!noExistAccountSet.isEmpty()) {
                logService.addImportLog(importJob.getAppId(), importJob.getId(), LOG_HR);
                noExistAccountSet.forEach(account -> logService.addImportLog(
                    importJob.getAppId(),
                    importJob.getId(),
                    String.format(
                        i18nService.getI18n(LogMessage.IMPORT_ACCOUNT_NOT_EXIST),
                        AccountTypeEnum.valueOf(account.getType()).getName(),
                        account.getAlias()
                    )
                ));
            }
        }
        importJob.setAccountIdMap(finalAccountIdMap);
    }

    private void doProcessAccount(ImportJobInfoDTO importJob,
                                  Map<Long, Long> finalAccountIdMap,
                                  Map<Integer, Map<String, Long>> categoryAliasToAccountIdMap,
                                  ServiceAccountDTO account,
                                  Set<ServiceAccountDTO> noExistAccountSet) {
        Map<String, Long> accountAliasMap = categoryAliasToAccountIdMap.get(account.getCategory());
        if (accountAliasMap != null && accountAliasMap.get(account.getAlias()) != null) {
            finalAccountIdMap.put(account.getId(), accountAliasMap.get(account.getAlias()));
        } else if (finalAccountIdMap.get(account.getId()) == null) {
            saveAccount(importJob, account, finalAccountIdMap, noExistAccountSet);
        } else {
            log.debug("Already create account|{}|{}", account.getAppId(), account.getAlias());
        }
    }

    private void saveAccount(ImportJobInfoDTO importJob,
                             ServiceAccountDTO account,
                             Map<Long, Long> finalAccountIdMap,
                             Set<ServiceAccountDTO> noExistAccountSet) {
        // 导入作业只支持自动创建不需要填写密码的账号
        if (AccountTypeEnum.LINUX.getType().equals(account.getType())) {
            Long newAccountId = accountService.saveAccount(importJob.getCreator(), importJob.getAppId(), account);
            if (newAccountId != null && newAccountId > 0) {
                finalAccountIdMap.put(account.getId(), newAccountId);
            } else {
                log.error("Failed to create account!|{}|{}|{}|{}", importJob.getCreator(), account.getAppId(),
                    account.getAlias(), AccountTypeEnum.valueOf(account.getType()).getName());
            }
        } else {
            log.warn("[appId={},alias={}] account does not exist, it needs to be added manually",
                account.getAppId(), account.getAlias());
            noExistAccountSet.add(account);
        }
    }

    private File getImportFileDirectory(ImportJobInfoDTO importJob) {
        return storageService.getFile(importJob.getFileName()).getParentFile();
    }

    private TemplateIdMapDTO processTemplateIdMap(TaskTemplateVO oldTemplate, TaskTemplateVO newTemplate) {
        TemplateIdMapDTO templateIdMap = new TemplateIdMapDTO();
        templateIdMap.setOldVersion(oldTemplate.getVersion());
        templateIdMap.setNewVersion(newTemplate.getVersion());

        templateIdMap.setStepIdMap(new HashMap<>());
        templateIdMap.setVariableIdMap(new HashMap<>());

        int i = 0;
        for (TaskStepVO taskStep : oldTemplate.getStepList()) {
            TaskStepVO newTaskStep = newTemplate.getStepList().get(i);
            if (newTaskStep != null && newTaskStep.getName().equals(taskStep.getName())) {
                templateIdMap.getStepIdMap().put(taskStep.getId(), newTaskStep.getId());
            }
            i += 1;
        }

        if (oldTemplate.getVariableList() != null) {
            i = 0;
            for (TaskVariableVO taskVariable : oldTemplate.getVariableList()) {
                TaskVariableVO newTaskVariable = newTemplate.getVariableList().get(i);
                if (newTaskVariable != null && newTaskVariable.getName().equals(taskVariable.getName())) {
                    templateIdMap.getVariableIdMap().put(taskVariable.getId(), newTaskVariable.getId());
                }
                i += 1;
            }
        }

        return templateIdMap;
    }

    private void addStartImportLog(ImportJobInfoDTO importJob, Long planId) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            String.format(
                i18nService.getI18n(LogMessage.START_IMPORT_PLAN),
                importJob.getIdNameInfo().getPlanNameMap().get(planId)
            )
        );
    }

    private void addIdAutoIncrementImportLog(ImportJobInfoDTO importJob) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            i18nService.getI18n(LogMessage.ID_AUTO_INCREMENT_SUFFIX)
        );
    }

    private void addIdKeepImportLog(ImportJobInfoDTO importJob) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            i18nService.getI18n(LogMessage.ID_KEEP_SUFFIX)
        );
    }

    private void addIdDuplicateSkipImportLog(ImportJobInfoDTO importJob) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            String.format(
                i18nService.getI18n(LogMessage.ID_DUPLICATE_SKIP_SUFFIX),
                i18nService.getI18n(LogMessage.PLAN)
            )
        );
    }

    private void addIdDuplicateIncrementLog(ImportJobInfoDTO importJob) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            i18nService.getI18n(LogMessage.ID_DUPLICATE_INCREMENT_SUFFIX)
        );
    }

    /**
     * @param importJob  导入任务信息
     * @param templateId 作业模板ID
     * @param planId     执行方案ID
     * @param planInfo   执行方案内容
     * @return 是否需要跳过该执行方案
     */
    private boolean processIdAndName(ImportJobInfoDTO importJob,
                                     Long templateId,
                                     Long planId,
                                     TaskPlanVO planInfo
    ) {
        ServiceIdNameCheckDTO idNameCheckResult = taskPlanService.checkIdAndName(
            importJob.getAppId(),
            templateId,
            planId,
            planInfo.getName()
        );

        if (DuplicateIdHandlerEnum.AUTO_INCREMENT.equals(importJob.getDuplicateIdHandler())) {
            addIdAutoIncrementImportLog(importJob);
            planInfo.setId(0L);
        } else {
            if (idNameCheckResult != null && idNameCheckResult.getIdCheckResult() == 1) {
                addIdKeepImportLog(importJob);
            } else {
                if (DuplicateIdHandlerEnum.ON_DUPLICATE_SKIP == importJob.getDuplicateIdHandler()) {
                    addIdDuplicateSkipImportLog(importJob);
                    return true;
                } else if (DuplicateIdHandlerEnum.ON_DUPLICATE_INCREMENT == importJob.getDuplicateIdHandler()) {
                    addIdDuplicateIncrementLog(importJob);
                    planInfo.setId(0L);
                }
            }
        }
        return false;
    }

    private boolean processPlan(ImportJobInfoDTO importJob,
                                JobBackupInfoDTO jobBackupInfo,
                                TemplateIdMapDTO templateIdMap,
                                Long templateId,
                                Long planId) {
        TaskPlanVO planInfo = jobBackupInfo.getPlanDetailInfoMap().get(planId);

        addStartImportLog(importJob, planId);
        boolean needToSkip = processIdAndName(importJob, templateId, planId, planInfo);
        if (needToSkip) {
            return true;
        }

        if (planInfo.getVersion().equals(templateIdMap.getOldVersion())) {
            planInfo.setVersion(templateIdMap.getNewVersion());
        }

        Map<Long, Long> stepIdMap = templateIdMap.getStepIdMap();
        for (TaskStepVO taskStep : planInfo.getStepList()) {
            if (stepIdMap.get(taskStep.getTemplateStepId()) != null) {
                taskStep.setTemplateStepId(stepIdMap.get(taskStep.getTemplateStepId()));
            }
        }

        if (CollectionUtils.isNotEmpty(planInfo.getVariableList())) {
            Map<Long, Long> variableIdMap = templateIdMap.getVariableIdMap();
            for (TaskVariableVO taskVariable : planInfo.getVariableList()) {
                if (variableIdMap.get(taskVariable.getId()) != null) {
                    taskVariable.setId(variableIdMap.get(taskVariable.getId()));
                }
            }
        }

        Map<String, Map<Long, ServiceScriptDTO>> linkScriptContentMap = jobBackupInfo.getLinkScriptContentMap();
        if (MapUtils.isNotEmpty(linkScriptContentMap)) {
            fixScript(importJob, linkScriptContentMap, planInfo.getStepList());
        }
        fixAccount(importJob, planInfo.getStepList(), planInfo.getName());

        Long resultPlanId = taskPlanService.savePlan(
            importJob.getCreator(),
            importJob.getAppId(),
            templateId, planInfo
        );

        if (resultPlanId != null && resultPlanId > 0) {
            addImportPlanSuccessLog(importJob, templateId, resultPlanId);
            if (MapUtils.isNotEmpty(jobBackupInfo.getPlanFileList())) {
                processFile(importJob, jobBackupInfo.getPlanFileList().get(planId));
            }
            return true;
        } else {
            addImportFailedLog(importJob);
            return false;
        }
    }

    private void addImportPlanSuccessLog(ImportJobInfoDTO importJob, Long templateId, Long resultPlanId) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            i18nService.getI18n(LogMessage.IMPORT_PLAN_SUCCESS),
            LogEntityTypeEnum.PLAN,
            templateId,
            resultPlanId
        );
    }

    private void addImportFailedLog(ImportJobInfoDTO importJob) {
        logService.addImportLog(
            importJob.getAppId(),
            importJob.getId(),
            i18nService.getI18n(LogMessage.IMPORT_FAILED),
            LogEntityTypeEnum.ERROR
        );
    }

    /**
     * 导入作业模板
     *
     * @param importJob     导入任务信息
     * @param jobBackupInfo 导入作业信息
     * @param templateId    作业模板ID
     * @return Pair<是否成功导入或跳过, 成功导入后得到的作业模板>
     */
    private Pair<Boolean, TaskTemplateVO> processTemplate(ImportJobInfoDTO importJob, JobBackupInfoDTO jobBackupInfo,
                                                          long templateId) {
        TaskTemplateVO templateInfo = jobBackupInfo.getTemplateDetailInfoMap().get(templateId);

        logService.addImportLog(importJob.getAppId(), importJob.getId(), LOG_HR);
        logService.addImportLog(importJob.getAppId(), importJob.getId(),
            String.format(i18nService.getI18n(LogMessage.START_IMPORT_TEMPLATE), templateId,
                importJob.getIdNameInfo().getTemplateNameMap().get(templateId)));

        if (templateInfo.getName().length() > 60) {
            logService.addImportLog(
                importJob.getAppId(),
                importJob.getId(),
                i18nService.getI18n(LogMessage.TEMPLATE_NAME_TOO_LONG_SKIP)
            );
            return Pair.of(false, null);
        }

        ServiceIdNameCheckDTO idNameCheckResult =
            taskTemplateService.checkIdAndName(importJob.getAppId(), templateId, templateInfo.getName());

        if (DuplicateIdHandlerEnum.AUTO_INCREMENT.equals(importJob.getDuplicateIdHandler())) {
            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                i18nService.getI18n(LogMessage.ID_AUTO_INCREMENT_SUFFIX));
            templateInfo.setId(0L);
        } else {
            if (idNameCheckResult != null && idNameCheckResult.getIdCheckResult() == 1) {
                logService.addImportLog(importJob.getAppId(), importJob.getId(),
                    i18nService.getI18n(LogMessage.ID_KEEP_SUFFIX));
            } else {
                if (DuplicateIdHandlerEnum.ON_DUPLICATE_SKIP == importJob.getDuplicateIdHandler()) {
                    logService.addImportLog(importJob.getAppId(), importJob.getId(),
                        String.format(i18nService.getI18n(LogMessage.ID_DUPLICATE_SKIP_SUFFIX),
                            i18nService.getI18n(LogMessage.TEMPLATE)));
                    return Pair.of(true, null);
                } else if (DuplicateIdHandlerEnum.ON_DUPLICATE_INCREMENT == importJob.getDuplicateIdHandler()) {
                    logService.addImportLog(importJob.getAppId(), importJob.getId(),
                        i18nService.getI18n(LogMessage.ID_DUPLICATE_INCREMENT_SUFFIX));
                    templateInfo.setId(0L);
                }
            }
        }

        if (idNameCheckResult != null && idNameCheckResult.getNameCheckResult() == 0) {
            templateInfo.setName(templateInfo.getName() + importJob.getDuplicateSuffix());

            getUsableName(importJob.getAppId(), templateInfo);

            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                String.format(i18nService.getI18n(LogMessage.TEMPLATE_NAME_CHANGE), templateInfo.getName()));

            if (templateInfo.getName().length() > 60) {
                logService.addImportLog(
                    importJob.getAppId(),
                    importJob.getId(),
                    i18nService.getI18n(LogMessage.TEMPLATE_NAME_TOO_LONG_SKIP)
                );
                return Pair.of(false, null);
            }
        }

        Map<String, Map<Long, ServiceScriptDTO>> linkScriptContentMap = jobBackupInfo.getLinkScriptContentMap();
        if (MapUtils.isNotEmpty(linkScriptContentMap)) {
            fixScript(importJob, linkScriptContentMap, templateInfo.getStepList());
        }
        fixAccount(importJob, templateInfo.getStepList(), templateInfo.getName());

        Long resultTemplateId =
            taskTemplateService.saveTemplate(importJob.getCreator(), importJob.getAppId(), templateInfo);

        if (resultTemplateId != null && resultTemplateId > 0) {
            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                i18nService.getI18n(LogMessage.IMPORT_TEMPLATE_SUCCESS),
                LogEntityTypeEnum.TEMPLATE,
                resultTemplateId, 0);
            if (MapUtils.isNotEmpty(jobBackupInfo.getTemplateFileList())) {
                processFile(importJob, jobBackupInfo.getTemplateFileList().get(templateId));
            }
        } else {
            logService.addImportLog(importJob.getAppId(), importJob.getId(),
                i18nService.getI18n(LogMessage.IMPORT_FAILED),
                LogEntityTypeEnum.ERROR);
            return Pair.of(false, null);
        }
        return Pair.of(true, taskTemplateService.getTemplateById(
            importJob.getCreator(),
            importJob.getAppId(),
            resultTemplateId)
        );
    }

    private void fixAccount(ImportJobInfoDTO importJob, List<TaskStepVO> stepList, String jobName) {
        Map<Long, Long> accountIdMap = importJob.getAccountIdMap();
        if (CollectionUtils.isNotEmpty(stepList)) {
            Set<String> withoutAccountStepSet = new LinkedHashSet<>();
            for (TaskStepVO taskStep : stepList) {
                switch (taskStep.getType()) {
                    case 1:
                        // Script
                        TaskScriptStepVO scriptStepInfo = taskStep.getScriptStepInfo();
                        if (scriptStepInfo != null) {
                            Long newAccountId = accountIdMap.get(scriptStepInfo.getAccount());
                            if (newAccountId != null && newAccountId > 0) {
                                scriptStepInfo.setAccount(newAccountId);
                            } else {
                                log.warn("Error while fix old account {}|{}|{}|{}", scriptStepInfo.getAccount(),
                                    taskStep.getId(), taskStep.getName(), taskStep.getType());
                                withoutAccountStepSet.add(taskStep.getName());
                            }
                        } else {
                            log.warn("Empty script step info|{}|{}|{}", taskStep.getId(), taskStep.getName(),
                                taskStep.getType());
                        }
                        break;
                    case 2:
                        TaskFileStepVO fileStepInfo = taskStep.getFileStepInfo();
                        if (fileStepInfo != null) {
                            TaskFileDestinationInfoVO fileDestination = fileStepInfo.getFileDestination();
                            if (fileDestination != null) {
                                Long newAccountId = accountIdMap.get(fileDestination.getAccount());
                                if (newAccountId != null && newAccountId > 0) {
                                    fileDestination.setAccount(newAccountId);
                                } else {
                                    log.warn("Error while fix old account {}|{}|{}|{}", fileDestination.getAccount(),
                                        taskStep.getId(), taskStep.getName(), taskStep.getType());
                                    withoutAccountStepSet.add(taskStep.getName());
                                }
                            } else {
                                log.warn("Empty file destination|{}|{}|{}", taskStep.getId(), taskStep.getName(),
                                    taskStep.getType());
                            }

                            List<TaskFileSourceInfoVO> fileSourceList = fileStepInfo.getFileSourceList();
                            if (CollectionUtils.isNotEmpty(fileSourceList)) {
                                for (TaskFileSourceInfoVO fileSourceInfo : fileSourceList) {
                                    switch (fileSourceInfo.getFileType()) {
                                        case 1:
                                            // Server file
                                            if (fileSourceInfo.getAccount() != null) {
                                                Long newAccountId = accountIdMap.get(fileSourceInfo.getAccount());
                                                if (newAccountId != null && newAccountId > 0) {
                                                    fileSourceInfo.setAccount(newAccountId);
                                                } else {
                                                    log.warn("Error while fix old account {}|{}|{}|{}",
                                                        fileSourceInfo.getAccount(), taskStep.getId(),
                                                        taskStep.getName(), taskStep.getType());
                                                    withoutAccountStepSet.add(taskStep.getName());
                                                }
                                            } else {
                                                log.warn("Missing account in file source info|{}|{}|{}",
                                                    taskStep.getId(), taskStep.getName(), taskStep.getType());
                                            }
                                            break;
                                        case 2:
                                            // Local file
                                            break;
                                        default:
                                            log.warn("Unknown step type!|{}|{}|{}", taskStep.getId(),
                                                taskStep.getName(), taskStep.getType());
                                    }
                                }
                            } else {
                                log.warn("Empty file source list|{}|{}|{}", taskStep.getId(), taskStep.getName(),
                                    taskStep.getType());
                            }
                        } else {
                            log.warn("Empty file step info|{}|{}|{}", taskStep.getId(), taskStep.getName(),
                                taskStep.getType());
                        }
                        // File
                        break;
                    case 3:
                        // Approval
                        break;
                    default:
                        log.warn("Unknown step type!|{}|{}|{}", taskStep.getId(), taskStep.getName(),
                            taskStep.getType());
                }
            }

            if (!withoutAccountStepSet.isEmpty()) {
                withoutAccountStepSet.forEach(stepName -> logService.addImportLog(
                    importJob.getAppId(),
                    importJob.getId(),
                    String.format(
                        i18nService.getI18n(LogMessage.IMPORT_STEP_ACCOUNT_INVALID),
                        jobName,
                        stepName
                    )
                ));
            }
        }
    }

    private void processFile(ImportJobInfoDTO importJob, List<String> fileList) {
        if (CollectionUtils.isEmpty(fileList)) {
            return;
        }
        File importFileDirectory = getImportFileDirectory(importJob);
        String uploadBaseDirectory =
            importFileDirectory.getPath() + File.separatorChar + Constant.LOCAL_UPLOAD_FILE_PREFIX;

        for (String file : fileList) {
            File importFile = new File(uploadBaseDirectory + file);
            if (importFile.exists()) {
                File destinationFile = new File(storageService.getLocalUploadPath() + file);
                if (destinationFile.exists()) {
                    return;
                }
                try {
                    FileUtils.copyFile(importFile, destinationFile);
                } catch (Exception e) {
                    log.warn("Error while trying to copy import file!", e);
                }
            }
        }
    }

    private void fixScript(ImportJobInfoDTO importJob, Map<String, Map<Long, ServiceScriptDTO>> linkScriptContentMap,
                           List<TaskStepVO> stepList) {
        for (TaskStepVO taskStep : stepList) {
            if (TaskStepTypeEnum.SCRIPT.getValue() == taskStep.getType()) {
                TaskScriptStepVO scriptStepInfo = taskStep.getScriptStepInfo();
                if (TaskScriptSourceEnum.CITING.getType() == scriptStepInfo.getScriptSource()
                    || TaskScriptSourceEnum.PUBLIC.getType() == scriptStepInfo.getScriptSource()) {
                    String scriptId = scriptStepInfo.getScriptId();
                    Long scriptVersionId = scriptStepInfo.getScriptVersionId();
                    ServiceScriptDTO scriptInfoById =
                        scriptService.getScriptInfoById(importJob.getCreator(), importJob.getAppId(), scriptVersionId);
                    if (scriptInfoById != null && scriptInfoById.getId().equals(scriptId)) {
                        continue;
                    }

                    Map<Long, ServiceScriptDTO> scriptVersionInfoMap = linkScriptContentMap.get(scriptId);
                    if (MapUtils.isNotEmpty(scriptVersionInfoMap)) {
                        ServiceScriptDTO originScriptInfo = scriptVersionInfoMap.get(scriptVersionId);
                        if (originScriptInfo != null) {
                            scriptStepInfo.setScriptId(null);
                            scriptStepInfo.setScriptLanguage(null);

                            scriptStepInfo.setStatus(0);
                            scriptStepInfo.setScriptSource(TaskScriptSourceEnum.LOCAL.getType());
                            scriptStepInfo.setContent(originScriptInfo.getContent());
                            scriptStepInfo.setScriptLanguage(originScriptInfo.getType());
                        }
                    }
                }
            }
        }
    }

    private void getUsableName(Long appId, TaskTemplateVO templateInfo) {
        long i = 0;
        String templateName = templateInfo.getName();
        while (true) {
            ServiceIdNameCheckDTO idNameCheckResult =
                taskTemplateService.checkIdAndName(appId, templateInfo.getId(), templateName);
            if (idNameCheckResult != null && idNameCheckResult.getNameCheckResult() == 1) {
                break;
            } else {
                templateName = templateInfo.getName() + i;
                i++;
            }
        }
        templateInfo.setName(templateName);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    class ImportJobExecutorThread extends Thread {

        @Override
        public void run() {
            this.setName("Import-Job-Executor-Thread");
            while (true) {
                String uuid = UUID.randomUUID().toString();
                try {
                    String jobId = IMPORT_JOB_QUEUE.take();
                    log.debug("{}|Import job queue length|{}", uuid, IMPORT_JOB_QUEUE.size());
                    processImportJob(jobId);
                    log.info("{}|Import job process finished!|{}", uuid, jobId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    String msg = MessageFormatter.format(
                        "{}|Error while processing import job!",
                        uuid
                    ).getMessage();
                    log.error(msg, e);
                }
            }
        }
    }
}
