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

package com.tencent.bk.job.backup.executor;

import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.Constant;
import com.tencent.bk.job.backup.constant.LogMessage;
import com.tencent.bk.job.backup.constant.SecretHandlerEnum;
import com.tencent.bk.job.backup.model.dto.BackupTemplateInfoDTO;
import com.tencent.bk.job.backup.model.dto.ExportJobInfoDTO;
import com.tencent.bk.job.backup.model.dto.JobBackupInfoDTO;
import com.tencent.bk.job.backup.service.AccountService;
import com.tencent.bk.job.backup.service.ExportJobService;
import com.tencent.bk.job.backup.service.LogService;
import com.tencent.bk.job.backup.service.ScriptService;
import com.tencent.bk.job.backup.service.StorageService;
import com.tencent.bk.job.backup.service.TaskPlanService;
import com.tencent.bk.job.backup.service.TaskTemplateService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.common.util.file.ZipUtil;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * @since 29/7/2020 17:12
 */
@Slf4j
@Service
public class ExportJobExecutor {
    private static final LinkedBlockingQueue<String> EXPORT_JOB_QUEUE = new LinkedBlockingQueue<>(100);
    private static final String JOB_EXPORT_FILE_PREFIX = "export" + File.separatorChar;
    private final ExportJobService exportJobService;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final ScriptService scriptService;
    private final AccountService accountService;
    private final LogService logService;
    private final StorageService storageService;
    private final MessageI18nService i18nService;

    @Autowired
    public ExportJobExecutor(ExportJobService exportJobService, TaskTemplateService taskTemplateService,
                             TaskPlanService taskPlanService, ScriptService scriptService,
                             AccountService accountService, LogService logService,
                             StorageService storageService, MessageI18nService i18nService) {
        this.exportJobService = exportJobService;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.scriptService = scriptService;
        this.accountService = accountService;
        this.logService = logService;
        this.storageService = storageService;
        this.i18nService = i18nService;

        File storageDirectory = new File(storageService.getStoragePath().concat(JOB_EXPORT_FILE_PREFIX));
        checkDirectory(storageDirectory);

        ExportJobExecutorThread exportJobExecutorThread = new ExportJobExecutorThread();
        exportJobExecutorThread.start();
    }

    public static String getExportLocalUploadFilePath(ExportJobInfoDTO exportInfo) {
        return getExportFilePrefix(exportInfo.getCreator(), exportInfo.getId()) + Constant.LOCAL_UPLOAD_FILE_PREFIX;
    }

    public static String getExportFilePath(ExportJobInfoDTO exportInfo) {
        return getExportFilePrefix(exportInfo.getCreator(), exportInfo.getId()) + exportInfo.getId() + ".json";
    }

    public static String getExportFilePrefix(String username, String id) {
        return JOB_EXPORT_FILE_PREFIX + username + File.separatorChar + id + File.separatorChar;
    }

    public static void startExport(String id) {
        try {
            EXPORT_JOB_QUEUE.add(id);
        } catch (Exception e) {
            log.error("Export job queue is full!");
            throw e;
        }
    }

    private void processExportJob(String jobId) {
        ExportJobInfoDTO exportInfo = exportJobService.getExportInfo(-1L, jobId);
        if (BackupJobStatusEnum.INIT.equals(exportInfo.getStatus())) {
            LocaleContextHolder.setLocale(exportInfo.getLocale(), true);
            exportInfo.setStatus(BackupJobStatusEnum.PROCESSING);
            exportJobService.updateExportJob(exportInfo);
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.START_EXPORT));

            JobBackupInfoDTO jobBackupInfo = new JobBackupInfoDTO();
            jobBackupInfo.setId(exportInfo.getId());
            jobBackupInfo.setCreator(exportInfo.getCreator());
            jobBackupInfo.setCreateTime(exportInfo.getCreateTime());
            jobBackupInfo.setExpireTime(exportInfo.getExpireTime());
            jobBackupInfo.setTemplateInfo(exportInfo.getTemplateInfo());
            processTemplatePlanDetail(exportInfo, jobBackupInfo);
            try {
                processAccount(exportInfo, jobBackupInfo);
            } catch (Exception e) {
                log.error("Error while processing account!", e);
                logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                    "Process account failed! Please " +
                    "try again!");
                exportInfo.setPassword(null);
                exportInfo.setStatus(BackupJobStatusEnum.FAILED);
                exportJobService.updateExportJob(exportInfo);
            }
            processLocalFile(exportInfo, jobBackupInfo);
            processLinkScript(exportInfo, jobBackupInfo);
            processVariableValue(exportInfo, jobBackupInfo);
            String backupInfoString = JsonMapper.nonEmptyMapper().toJson(jobBackupInfo);

            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.PROCESS_TEMPLATE_PLAN_FINISHED));

            File zipFile = generateFile(exportInfo, backupInfoString);
            if (zipFile == null) {
                log.error("Fail to generate zipFile");
                return;
            }
            String fileName = encryptFile(exportInfo, zipFile);
            if (StringUtils.isBlank(fileName)) {
                return;
            }

            fileName = getExportFilePrefix(exportInfo.getCreator(), exportInfo.getId()) + fileName;

            exportInfo.setPassword(null);
            exportInfo.setStatus(BackupJobStatusEnum.SUCCESS);
            exportInfo.setFileName(fileName);
            exportJobService.updateExportJob(exportInfo);

            if (exportInfo.getExpireTime() > 0) {
                long expireDay = (exportInfo.getExpireTime() - System.currentTimeMillis()) / (1000 * 3600 * 24) + 1;
                logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                    String.format(i18nService.getI18n(LogMessage.EXPORT_FINISHED),
                        expireDay + " " + i18nService.getI18n(LogMessage.DAY)));
            } else {
                logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                    String.format(i18nService.getI18n(LogMessage.EXPORT_FINISHED),
                        i18nService.getI18n(LogMessage.FOREVER)));
            }
        }
    }

    private void processAccount(ExportJobInfoDTO exportInfo, JobBackupInfoDTO jobBackupInfo) {
        List<ServiceAccountDTO> accountList = new ArrayList<>();
        Set<Long> accountIdSet = new HashSet<>();
        for (TaskTemplateVO taskTemplate : jobBackupInfo.getTemplateDetailInfoMap().values()) {
            extractAccount(taskTemplate.getStepList(), accountIdSet);
        }

        if (MapUtils.isNotEmpty(jobBackupInfo.getPlanDetailInfoMap())) {
            for (TaskPlanVO taskPlan : jobBackupInfo.getPlanDetailInfoMap().values()) {
                extractAccount(taskPlan.getStepList(), accountIdSet);
            }
        }

        if (CollectionUtils.isNotEmpty(accountIdSet)) {
            accountIdSet.forEach(accountId -> accountList.add(
                accountService.getAccountAliasById(accountId)));
        }

        jobBackupInfo.setAccountList(accountList);
    }

    private void extractAccount(List<TaskStepVO> stepList, Set<Long> accountIdSet) {
        for (TaskStepVO taskStep : stepList) {
            switch (taskStep.getType()) {
                case 1:
                    if (taskStep.getScriptStepInfo() != null) {
                        accountIdSet.add(taskStep.getScriptStepInfo().getAccount());
                    }
                    break;
                case 2:
                    if (taskStep.getFileStepInfo() != null) {
                        accountIdSet.add(taskStep.getFileStepInfo().getFileDestination().getAccount());
                        if (CollectionUtils.isNotEmpty(taskStep.getFileStepInfo().getFileSourceList())) {
                            for (TaskFileSourceInfoVO taskFileInfo : taskStep.getFileStepInfo().getFileSourceList()) {
                                if (taskFileInfo.getFileType() == 1) {
                                    accountIdSet.add(taskFileInfo.getAccount());
                                }
                            }
                        }
                    }
                    break;
                default:
                    continue;
            }
        }
    }

    private String encryptFile(ExportJobInfoDTO exportInfo, File zipFile) {
        String fileName;
        if (StringUtils.isNotBlank(exportInfo.getPassword())) {
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.START_ENCRYPTING));
            File finalFileTmp = new File(zipFile.getPath().concat(".enc.tmp"));
            try {
                AESUtils.encrypt(zipFile, finalFileTmp, exportInfo.getPassword());
                FileUtils.deleteQuietly(zipFile);
            } catch (Exception e) {
                log.error("Error while processing export job! Encrypt failed!", e);
                exportInfo.setStatus(BackupJobStatusEnum.FAILED);
                exportJobService.updateExportJob(exportInfo);
                return null;
            }

            File finalFile = new File(zipFile.getPath().concat(Constant.JOB_EXPORT_ENCRYPT_SUFFIX));
            try (FileInputStream in = new FileInputStream(finalFileTmp); FileOutputStream out =
                new FileOutputStream(finalFile)) {
                out.write(Constant.JOB_FILE_MAGIC);
                IOUtils.copy(in, out);
            } catch (IOException e) {
                log.error("Error while processing export job! Generate final file failed!", e);
                exportInfo.setStatus(BackupJobStatusEnum.FAILED);
                exportJobService.updateExportJob(exportInfo);
                return null;
            }
            FileUtils.deleteQuietly(finalFileTmp);

            fileName = finalFile.getName();
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.ENCRYPTING_FINISHED));
        } else {
            fileName = zipFile.getName();
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.SKIP_ENCRYPTING));
        }
        return fileName;
    }

    private File generateFile(ExportJobInfoDTO exportInfo, String backupInfoString) {
        logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
            i18nService.getI18n(LogMessage.START_PACKAGE));
        File outputFile = new File(storageService.getStoragePath().concat(getExportFilePath(exportInfo)));
        checkDirectory(outputFile.getParentFile());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            fos.write(backupInfoString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("fail to generateFile", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.warn("error when close fos", e);
                }
            }
        }
        File uploadFileDirectory =
            new File(storageService.getStoragePath().concat(getExportLocalUploadFilePath(exportInfo)));
        File zipFile = ZipUtil.zip(outputFile.getPath() + ".zip", outputFile, uploadFileDirectory);
        try {
            FileUtils.deleteQuietly(outputFile);
            FileUtils.deleteDirectory(uploadFileDirectory);
        } catch (Exception e) {
            log.warn("Delete temp file failed!", e);
        }
        logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
            i18nService.getI18n(LogMessage.PACKAGE_FINISHED));
        return zipFile;
    }

    private void processTemplatePlanDetail(ExportJobInfoDTO exportInfo, JobBackupInfoDTO jobBackupInfo) {
        Map<Long, TaskTemplateVO> templateInfoMap = new HashMap<>(exportInfo.getTemplateInfo().size());
        Map<Long, TaskPlanVO> planInfoMap = new HashMap<>(exportInfo.getTemplateInfo().size());

        for (BackupTemplateInfoDTO backupTemplateInfo : exportInfo.getTemplateInfo()) {
            TaskTemplateVO taskTemplate = taskTemplateService.getTemplateById(exportInfo.getCreator(),
                exportInfo.getAppId(), backupTemplateInfo.getId());
            if (taskTemplate != null) {
                templateInfoMap.put(taskTemplate.getId(), taskTemplate);
                if (backupTemplateInfo.getExportAll()) {
                    List<TaskPlanVO> taskPlanList = taskPlanService.listPlans(exportInfo.getCreator(),
                        exportInfo.getAppId(), backupTemplateInfo.getId());
                    if (CollectionUtils.isNotEmpty(taskPlanList)) {
                        backupTemplateInfo.setPlanId(
                            taskPlanList.parallelStream().map(TaskPlanVO::getId).collect(Collectors.toList()));
                    }
                }
                for (TaskPlanVO taskPlan : taskPlanService.getTaskPlanByIdList(exportInfo.getCreator(),
                    exportInfo.getAppId(), backupTemplateInfo.getId(), backupTemplateInfo.getPlanId())) {
                    if (taskPlan != null) {
                        planInfoMap.put(taskPlan.getId(), taskPlan);
                    }
                }
            }
        }

        jobBackupInfo.setTemplateDetailInfoMap(templateInfoMap);
        jobBackupInfo.setPlanDetailInfoMap(planInfoMap);
    }

    private void processLinkScript(ExportJobInfoDTO exportJobInfo, JobBackupInfoDTO jobBackupInfo) {
        Map<String, Map<Long, ServiceScriptDTO>> linkScriptContentMap = new HashMap<>();

        for (TaskTemplateVO taskTemplate : jobBackupInfo.getTemplateDetailInfoMap().values()) {
            extractScriptInfo(taskTemplate.getStepList(), linkScriptContentMap);
        }

        if (MapUtils.isNotEmpty(jobBackupInfo.getPlanDetailInfoMap())) {
            for (TaskPlanVO taskPlan : jobBackupInfo.getPlanDetailInfoMap().values()) {
                extractScriptInfo(taskPlan.getStepList(), linkScriptContentMap);
            }
        }

        if (MapUtils.isNotEmpty(linkScriptContentMap)) {
            logService.addExportLog(exportJobInfo.getAppId(), exportJobInfo.getId(),
                i18nService.getI18n(LogMessage.PROCESS_SCRIPT));
            for (Map.Entry<String, Map<Long, ServiceScriptDTO>> scriptInfoMapEntry : linkScriptContentMap.entrySet()) {
                for (Map.Entry<Long, ServiceScriptDTO> scriptInfoEntity : scriptInfoMapEntry.getValue().entrySet()) {
                    ServiceScriptDTO scriptInfo = scriptService.getScriptInfoById(exportJobInfo.getCreator(),
                        exportJobInfo.getAppId(), scriptInfoEntity.getKey());
                    if (scriptInfo != null) {
                        if (scriptInfo.getId().equals(scriptInfoMapEntry.getKey())) {
                            if (scriptInfo.getScriptVersionId().equals(scriptInfoEntity.getKey())) {
                                scriptInfo.setContent(Base64Util.encodeContentToStr(scriptInfo.getContent()));
                                scriptInfoMapEntry.getValue().put(scriptInfoEntity.getKey(), scriptInfo);
                            }
                        }
                    }
                }
            }

            jobBackupInfo.setLinkScriptContentMap(linkScriptContentMap);
            logService.addExportLog(exportJobInfo.getAppId(), exportJobInfo.getId(),
                i18nService.getI18n(LogMessage.PROCESS_FINISHED));
        } else {
            logService.addExportLog(exportJobInfo.getAppId(), exportJobInfo.getId(),
                i18nService.getI18n(LogMessage.NO_SCRIPT));
        }
    }

    private void extractScriptInfo(List<TaskStepVO> stepList,
                                   Map<String, Map<Long, ServiceScriptDTO>> linkScriptContentMap) {
        for (TaskStepVO taskStep : stepList) {
            if (TaskStepTypeEnum.SCRIPT.getType() == taskStep.getType()) {
                TaskScriptStepVO scriptStepInfo = taskStep.getScriptStepInfo();
                if (TaskScriptSourceEnum.CITING.getType() == scriptStepInfo.getScriptSource()
                    || TaskScriptSourceEnum.PUBLIC.getType() == scriptStepInfo.getScriptSource()) {
                    linkScriptContentMap.computeIfAbsent(scriptStepInfo.getScriptId(), k -> new HashMap<>());
                    linkScriptContentMap.get(scriptStepInfo.getScriptId()).put(scriptStepInfo.getScriptVersionId(),
                        null);
                }
            }
        }
    }

    private void processVariableValue(ExportJobInfoDTO exportInfo, JobBackupInfoDTO jobBackupInfo) {
        logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
            i18nService.getI18n(LogMessage.PROCESS_CIPHER_TEXT));
        if (SecretHandlerEnum.SAVE_NULL == exportInfo.getSecretHandler()) {
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.PROCESS_FINISHED) + i18nService.getI18n(LogMessage.SAVE_NULL));
            return;
        }

        for (TaskTemplateVO taskTemplate : jobBackupInfo.getTemplateDetailInfoMap().values()) {
            extractVariableRealValue(exportInfo, taskTemplate.getId(), null, taskTemplate.getVariableList());
        }

        if (MapUtils.isNotEmpty(jobBackupInfo.getPlanDetailInfoMap())) {
            for (TaskPlanVO taskPlan : jobBackupInfo.getPlanDetailInfoMap().values()) {
                extractVariableRealValue(exportInfo, taskPlan.getTemplateId(), taskPlan.getId(),
                    taskPlan.getVariableList());
            }
        }

        logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
            i18nService.getI18n(LogMessage.PROCESS_FINISHED) + i18nService.getI18n(LogMessage.SAVE_REAL));
    }

    private void extractVariableRealValue(ExportJobInfoDTO exportInfo, Long templateId, Long planId,
                                          List<TaskVariableVO> variableList) {
        if (CollectionUtils.isEmpty(variableList)) {
            return;
        }

        Map<Long,
            TaskVariableVO> needProcessVariableList = variableList.parallelStream()
            .filter(taskVariableVO -> taskVariableVO.getType().equals(TaskVariableTypeEnum.CIPHER.getType()))
            .collect(Collectors.toMap(TaskVariableVO::getId, taskVariableVO -> taskVariableVO));

        if (MapUtils.isEmpty(needProcessVariableList)) {
            return;
        }

        List<ServiceTaskVariableDTO> serviceVariableList;
        if (planId == null) {
            serviceVariableList =
                taskTemplateService.getTemplateVariable(exportInfo.getCreator(), exportInfo.getAppId(), templateId);
        } else {
            serviceVariableList =
                taskPlanService.getPlanVariable(exportInfo.getCreator(), exportInfo.getAppId(), templateId, planId);
        }

        for (ServiceTaskVariableDTO serviceTaskVariableDTO : serviceVariableList) {
            if (TaskVariableTypeEnum.CIPHER.getType() == serviceTaskVariableDTO.getType()) {
                if (needProcessVariableList.get(serviceTaskVariableDTO.getId()) != null) {
                    needProcessVariableList.get(serviceTaskVariableDTO.getId())
                        .setDefaultValue(serviceTaskVariableDTO.getDefaultValue());
                }
            }
        }
    }

    private void processLocalFile(ExportJobInfoDTO exportInfo, JobBackupInfoDTO jobBackupInfo) {
        logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
            i18nService.getI18n(LogMessage.PROCESS_LOCAL_FILE));
        File uploadFileDirectory =
            new File(storageService.getStoragePath().concat(getExportLocalUploadFilePath(exportInfo)));
        checkDirectory(uploadFileDirectory);

        Map<Long, List<String>> templateFileList = new HashMap<>(jobBackupInfo.getTemplateDetailInfoMap().size());
        for (TaskTemplateVO taskTemplate : jobBackupInfo.getTemplateDetailInfoMap().values()) {
            templateFileList.put(taskTemplate.getId(),
                extractLocalFileList(taskTemplate.getStepList(), uploadFileDirectory));
        }
        jobBackupInfo.setTemplateFileList(templateFileList);

        if (MapUtils.isNotEmpty(jobBackupInfo.getPlanDetailInfoMap())) {
            Map<Long, List<String>> planFileList = new HashMap<>(jobBackupInfo.getPlanDetailInfoMap().size());
            for (TaskPlanVO taskPlan : jobBackupInfo.getPlanDetailInfoMap().values()) {
                planFileList.put(taskPlan.getId(), extractLocalFileList(taskPlan.getStepList(), uploadFileDirectory));
            }
            jobBackupInfo.setPlanFileList(planFileList);
        }
        if (MapUtils.isNotEmpty(jobBackupInfo.getTemplateFileList())
            || MapUtils.isNotEmpty(jobBackupInfo.getPlanFileList())) {
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.PROCESS_FINISHED));
        } else {
            logService.addExportLog(exportInfo.getAppId(), exportInfo.getId(),
                i18nService.getI18n(LogMessage.NO_LOCAL_FILE));
        }
    }

    private List<String> extractLocalFileList(List<TaskStepVO> stepList, File uploadFileDirectory) {
        List<String> localFileList = new ArrayList<>();
        for (TaskStepVO taskStep : stepList) {
            if (TaskStepTypeEnum.FILE.getType() == taskStep.getType()) {
                TaskFileStepVO fileStepInfo = taskStep.getFileStepInfo();
                for (TaskFileSourceInfoVO taskFileInfo : fileStepInfo.getFileSourceList()) {
                    if (TaskFileTypeEnum.LOCAL.getType() == taskFileInfo.getFileType()) {
                        localFileList.addAll(taskFileInfo.getFileLocation());
                    }
                }
            }
        }
        localFileList.forEach(file -> copyFile(file, uploadFileDirectory));
        return localFileList;
    }

    private void copyFile(String file, File uploadFileDirectory) {
        File localUploadFile = storageService.getLocalUploadFile(file);
        if (localUploadFile != null) {
            File exportFile = new File(uploadFileDirectory.getPath() + File.separatorChar + file);
            if (exportFile.exists()) {
                return;
            }
            checkDirectory(exportFile.getParentFile());
            try {
                FileUtils.copyFile(localUploadFile, exportFile);
            } catch (IOException e) {
                log.error("fail to copyFile", e);
            }
        }
    }

    private void checkDirectory(File directory) {
        if (directory.exists() && !directory.isDirectory()) {
            if (!directory.delete()) {
                log.error("Error while deleting exist export directory!");
                throw new InternalException(ErrorCode.INTERNAL_ERROR,
                    "Delete exist export directory failed!");
            }
        }
        if (!directory.exists()) {
            if (!directory.mkdirs() || !directory.setWritable(true)) {
                log.error("Create export directory failed!|{}|{}", directory.getPath(), directory.getAbsolutePath());

                throw new InternalException(ErrorCode.INTERNAL_ERROR,
                    "Create export directory failed! Check path config or permission!");
            }
        }
    }

    class ExportJobExecutorThread extends Thread {
        @Override
        public void run() {
            this.setName("Export-Job-Executor-Thread");
            while (true) {
                String uuid = UUID.randomUUID().toString();
                try {
                    String jobId = EXPORT_JOB_QUEUE.take();
                    log.debug("{}|Export job queue length|{}", uuid, EXPORT_JOB_QUEUE.size());
                    processExportJob(jobId);
                    log.info("{}|Export job process finished!|{}", uuid, jobId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("{}|Error while processing export job!", uuid, e);
                }
            }
        }

    }
}
