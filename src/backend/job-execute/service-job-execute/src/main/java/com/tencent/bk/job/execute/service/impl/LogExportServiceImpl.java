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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.file.ZipUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.config.LogExportConfig;
import com.tencent.bk.job.execute.constants.LogExportStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.LogExportJobInfoDTO;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.LogExportService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.logsvr.util.LogFieldUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LogExportServiceImpl implements LogExportService {
    private static final String EXPORT_KEY_PREFIX = "execute:log:export:";
    private final LogService logService;
    private final ExecutorService logExportExecutor;
    private final StringRedisTemplate redisTemplate;
    private final StepInstanceService stepInstanceService;
    private final ArtifactoryClient artifactoryClient;
    private final ArtifactoryHelper artifactoryHelper;
    private final LogExportConfig logExportConfig;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;

    @Autowired
    public LogExportServiceImpl(LogService logService,
                                StringRedisTemplate redisTemplate,
                                StepInstanceService stepInstanceService,
                                @Qualifier("jobArtifactoryClient") ArtifactoryClient artifactoryClient,
                                ArtifactoryHelper artifactoryHelper,
                                LogExportConfig logExportConfig,
                                ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                @Qualifier("logExportExecutor") ExecutorService logExportExecutor) {
        this.logService = logService;
        this.redisTemplate = redisTemplate;
        this.stepInstanceService = stepInstanceService;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryHelper = artifactoryHelper;
        this.logExportConfig = logExportConfig;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.logExportExecutor = logExportExecutor;
    }

    @Override
    public LogExportJobInfoDTO packageLogFile(String username,
                                              Long appId,
                                              Long taskInstanceId,
                                              Long stepInstanceId,
                                              ExecuteObjectTypeEnum executeObjectType,
                                              Long executeObjectResourceId,
                                              int executeCount,
                                              String logFileDir,
                                              String logFileName,
                                              Boolean repackage) {
        log.info("Package log file for username={}|appId={}|stepInstanceId={}|executeObjectType={}" +
                "|executeObjectResourceId={}|executeCount={}|logFileDir={}|logFileName={}|repackage={}",
            username, appId, stepInstanceId, executeObjectType, executeObjectResourceId, executeCount, logFileDir,
            logFileName, repackage);
        LogExportJobInfoDTO exportJobInfo = new LogExportJobInfoDTO();
        exportJobInfo.setJobKey(getExportJobKey(appId, stepInstanceId, executeObjectType, executeObjectResourceId));
        exportJobInfo.setStatus(LogExportStatusEnum.INIT);

        if (repackage) {
            log.debug("Force release lock because of repackage flag");
            LockUtils.forceReleaseDistributedLock(exportJobInfo.getJobKey());
            deleteExportInfo(exportJobInfo.getJobKey());
        }
        saveExportInfo(exportJobInfo);

        boolean isGetByExecuteObject = executeObjectResourceId != null;

        if (isGetByExecuteObject) {
            doPackage(exportJobInfo, taskInstanceId, stepInstanceId, executeObjectType, executeObjectResourceId,
                executeCount, logFileDir, logFileName);
        } else {
            String requestId = JobContextUtil.getRequestId();
            logExportExecutor.execute(() -> {
                log.debug("Begin log package process |{}", stepInstanceId);
                try {
                    boolean lockResult = LockUtils.tryGetDistributedLock(exportJobInfo.getJobKey(),
                        requestId, 3600_000L);
                    if (lockResult) {
                        log.debug("Acquire lock success! Begin process!|{}", stepInstanceId);
                        exportJobInfo.setStatus(LogExportStatusEnum.PROCESSING);
                        saveExportInfo(exportJobInfo);

                        doPackage(exportJobInfo, taskInstanceId, stepInstanceId, executeObjectType,
                            executeObjectResourceId, executeCount, logFileDir, logFileName);
                    } else {
                        log.error("Job already running!|appId={}|stepInstanceId={}", appId, stepInstanceId);
                    }
                } catch (Exception e) {
                    String msg = MessageFormatter.arrayFormat(
                        "Error while package log file!|stepInstanceId={}|executeCount={}",
                        new String[]{
                            String.valueOf(stepInstanceId),
                            String.valueOf(executeCount)
                        }
                    ).getMessage();
                    log.error(msg, e);
                    markJobFailed(exportJobInfo);
                } finally {
                    LockUtils.releaseDistributedLock(exportJobInfo.getJobKey(), requestId);
                    log.debug("Process finished!|stepInstanceId={}|executeCount={}|logFileName={}", stepInstanceId,
                        executeCount, logFileName);
                }
            });
        }
        return exportJobInfo;
    }

    private void markJobFailed(LogExportJobInfoDTO exportJobInfo) {
        exportJobInfo.setStatus(LogExportStatusEnum.FAILED);
        saveExportInfo(exportJobInfo);
    }

    @Override
    public LogExportJobInfoDTO getExportInfo(Long appId,
                                             Long stepInstanceId,
                                             ExecuteObjectTypeEnum executeObjectType,
                                             Long executeObjectResourceId) {
        return JsonUtils.fromJson(redisTemplate.opsForValue().get(
                getExportJobKey(appId, stepInstanceId, executeObjectType, executeObjectResourceId)),
            LogExportJobInfoDTO.class);
    }

    private void saveExportInfo(LogExportJobInfoDTO exportJobInfo) {
        redisTemplate.opsForValue().set(exportJobInfo.getJobKey(), JsonUtils.toJson(exportJobInfo));
    }

    private void deleteExportInfo(String jobKey) {
        redisTemplate.delete(jobKey);
    }

    private String getExportJobKey(Long appId,
                                   Long stepInstanceId,
                                   ExecuteObjectTypeEnum executeObjectType,
                                   Long executeObjectResourceId) {
        String exportJobKey = EXPORT_KEY_PREFIX + appId + ":" + stepInstanceId;
        if (executeObjectResourceId != null) {
            exportJobKey += ":" + executeObjectType.getValue() + ":" +
                executeObjectResourceId;
        }
        return exportJobKey;
    }

    private void doPackage(LogExportJobInfoDTO exportJobInfo,
                           long taskInstanceId,
                           long stepInstanceId,
                           ExecuteObjectTypeEnum executeObjectType,
                           Long executeObjectResourceId,
                           int executeCount,
                           String logFileDir,
                           String logFileName) {
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(taskInstanceId, stepInstanceId);
        File logFile = new File(logFileDir + logFileName);

        StopWatch watch = new StopWatch("exportJobLog");
        watch.start("getExecuteObjectTasks");
        List<ExecuteObjectTask> executeObjectTasks = getExecuteObjectTasks(stepInstance, executeCount,
            executeObjectType, executeObjectResourceId);
        watch.stop();

        if (executeObjectTasks == null || executeObjectTasks.isEmpty()) {
            log.warn("Gse task ips are empty! stepInstanceId={}", stepInstanceId);
            markJobFailed(exportJobInfo);
            return;
        }

        watch.start("getLogContentAndWriteToFile");
        if (!getLogContentAndWriteToFile(stepInstance, executeObjectTasks, logFile, exportJobInfo)) {
            log.warn("Fail to getLogContentAndWriteToFile");
            return;
        }
        watch.stop();

        watch.start("zipLogFileAndSaveToBackend");
        zipLogFileAndSaveToBackend(logFile, exportJobInfo, logFileName);
        watch.stop();

        if (watch.getTotalTimeMillis() > 10000L) {
            log.info("Export job execution log is slow, cost: {}", watch.prettyPrint());
        }
        log.info("Package log success.|stepInstanceId={}|executeCount={}|logFileName={}", stepInstanceId,
            executeCount, logFileName);
    }

    /**
     * 获取执行对象任务
     *
     * @param stepInstance            步骤实例
     * @param executeCount            重试次数
     * @param executeObjectType       要获取日志记录的执行对象类型
     * @param executeObjectResourceId 要获取日志记录的执行对象资源 ID
     * @return 日志记录信息列表
     */
    private List<ExecuteObjectTask> getExecuteObjectTasks(StepInstanceBaseDTO stepInstance,
                                                          int executeCount,
                                                          ExecuteObjectTypeEnum executeObjectType,
                                                          Long executeObjectResourceId) {
        List<ExecuteObjectTask> executeObjectTasks = new ArrayList<>();
        boolean isGetByExecuteObject = executeObjectResourceId != null;
        if (isGetByExecuteObject) {
            ExecuteObjectCompositeKey executeObjectCompositeKey =
                ExecuteObjectCompositeKey.ofExecuteObjectResource(executeObjectType, executeObjectResourceId);
            ExecuteObjectTask executeObjectTask =
                scriptExecuteObjectTaskService.getTaskByExecuteObjectCompositeKey(
                    stepInstance, executeCount, null, executeObjectCompositeKey);
            if (executeObjectTask != null) {
                executeObjectTasks.add(executeObjectTask);
            }
        } else {
            executeObjectTasks = scriptExecuteObjectTaskService.listTasks(stepInstance, executeCount, null);
        }
        return executeObjectTasks;
    }

    private boolean getLogContentAndWriteToFile(StepInstanceBaseDTO stepInstance,
                                                List<ExecuteObjectTask> executeObjectTasks,
                                                File logFile,
                                                LogExportJobInfoDTO exportJobInfo) {
        Collection<LogBatchQuery> querys = buildLogBatchQuery(stepInstance.getId(), executeObjectTasks);

        String jobCreateDate = LogFieldUtil.buildJobCreateDate(stepInstance.getCreateTime());
        try (PrintWriter out = new PrintWriter(logFile, "UTF-8")) {
            for (LogBatchQuery query : querys) {
                for (List<ExecuteObject> executeObjects : query.getExecuteObjectBatches()) {
                    writeOneBatchExecuteObjectLogs(out, jobCreateDate, stepInstance, query, executeObjects);
                }
            }
            out.flush();
            return true;
        } catch (Exception e) {
            log.warn("Export execution log fail", e);
            FileUtils.deleteQuietly(logFile);
            markJobFailed(exportJobInfo);
            return false;
        }
    }

    private void writeOneBatchExecuteObjectLogs(PrintWriter out,
                                                String jobCreateDate,
                                                StepInstanceBaseDTO stepInstance,
                                                LogBatchQuery query,
                                                List<ExecuteObject> executeObjects) {
        List<ExecuteObjectCompositeKey> executeObjectQueryKeys;
        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectQueryKeys = executeObjects.stream()
                .map(executeObject -> ExecuteObjectCompositeKey.ofExecuteObjectId(executeObject.getId()))
                .collect(Collectors.toList());
        } else {
            executeObjectQueryKeys = executeObjects.stream()
                .map(executeObject -> ExecuteObjectCompositeKey.ofHostId(executeObject.getResourceId()))
                .collect(Collectors.toList());
        }
        List<ScriptExecuteObjectLogContent> scriptExecuteObjectLogContentList =
            logService.batchGetScriptExecuteObjectLogContent(jobCreateDate, stepInstance,
                query.getExecuteCount(), null, executeObjectQueryKeys);
        for (ScriptExecuteObjectLogContent scriptExecuteObjectLogContent : scriptExecuteObjectLogContentList) {
            if (scriptExecuteObjectLogContent != null
                && StringUtils.isNotEmpty(scriptExecuteObjectLogContent.getContent())) {

                String[] logList = scriptExecuteObjectLogContent.getContent().split("\n");
                for (String log : logList) {
                    out.println(scriptExecuteObjectLogContent.getExecuteObject().getExecuteObjectName() + " | " + log);
                }
            }
        }
    }

    private void zipLogFileAndSaveToBackend(File logFile, LogExportJobInfoDTO exportJobInfo, String logFileName) {
        try {
            File zipFile = ZipUtil.zip(logFile.getAbsolutePath());
            if (zipFile == null) {
                log.warn("Zip log file fail, fileName={}", logFile.getName());
                markJobFailed(exportJobInfo);
                return;
            } else {
                FileUtils.deleteQuietly(logFile);
            }
            long zipFileLength = zipFile.length();
            uploadZipFileToArtifactoryIfNeeded(zipFile, exportJobInfo);
            exportJobInfo.setStatus(LogExportStatusEnum.SUCCESS);
            exportJobInfo.setZipFileName(logFileName + ".zip");
            exportJobInfo.setFileSize(zipFileLength);
            saveExportInfo(exportJobInfo);
        } catch (Exception e) {
            log.warn("zipLogFileAndSaveToBackend fail, fileName={}", logFile.getName());
            markJobFailed(exportJobInfo);
        }
    }

    private void uploadZipFileToArtifactoryIfNeeded(File zipFile, LogExportJobInfoDTO exportJobInfo) {
        if (!JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(logExportConfig.getStorageBackend())) {
            return;
        }
        // 将zip文件上传至制品库
        try {
            artifactoryClient.uploadGenericFile(
                artifactoryHelper.getJobRealProject(),
                logExportConfig.getLogExportRepo(),
                zipFile.getName(),
                zipFile
            );
            FileUtils.deleteQuietly(zipFile);
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to upload {} to artifactory",
                zipFile.getAbsolutePath()
            ).getMessage();
            log.error(msg, e);
            markJobFailed(exportJobInfo);
            throw e;
        }
    }

    private Collection<LogBatchQuery> buildLogBatchQuery(long stepInstanceId,
                                                         List<ExecuteObjectTask> executeObjectTasks) {
        Map<Integer, LogBatchQuery> batchQueryGroups = new HashMap<>();
        executeObjectTasks.forEach(executeObjectTask -> {
            LogBatchQuery query = batchQueryGroups.computeIfAbsent(executeObjectTask.getExecuteCount(),
                (executeCount) -> new LogBatchQuery(stepInstanceId, executeCount));
            query.addExecuteObject(executeObjectTask.getExecuteObject());
        });
        batchQueryGroups.values().forEach(LogBatchQuery::batchHosts);
        return batchQueryGroups.values();
    }

    @Data
    private static class LogBatchQuery {
        private static final int MAX_BATCH_SIZE = 1000;
        private long stepInstanceId;
        private int executeCount;
        private List<ExecuteObject> executeObjects = new ArrayList<>();
        private List<List<ExecuteObject>> executeObjectBatches;

        LogBatchQuery(long stepInstanceId, int executeCount) {
            this.stepInstanceId = stepInstanceId;
            this.executeCount = executeCount;
        }

        void addExecuteObject(ExecuteObject executeObject) {
            if (executeObjects == null) {
                executeObjects = new ArrayList<>();
            }
            executeObjects.add(executeObject);
        }

        void batchHosts() {
            executeObjectBatches = CollectionUtil.partitionList(executeObjects, MAX_BATCH_SIZE);
        }
    }
}
