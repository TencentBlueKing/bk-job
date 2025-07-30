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

package com.tencent.bk.job.execute.engine.result;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.gse.constants.GSECode;
import com.tencent.bk.job.common.gse.v2.model.AtomicFileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.AtomicFileTaskResultContent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.feature.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.FileGseTaskResult;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResult;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.util.GseUtils;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件任务执行结果处理
 */
@Slf4j
public class FileResultHandleTask extends AbstractResultHandleTask<FileTaskResult> {
    /**
     * 文件源-GSE任务与JOB执行对象任务的映射关系
     */
    private final Map<ExecuteObjectGseKey, ExecuteObjectTask> sourceExecuteObjectTaskMap;
    /**
     * 任务包含的源服务器
     */
    private final Set<ExecuteObjectGseKey> sourceExecuteObjectGseKeys = new HashSet<>();
    /**
     * 已经分析结果完成的源服务器
     */
    protected Set<ExecuteObjectGseKey> analyseFinishedSourceExecuteObjectGseKeys = new HashSet<>();
    /**
     * 成功的文件下载任务，key: ExecuteObjectGseKey, value: 成功的文件任务名称
     */
    private final Map<ExecuteObjectGseKey, Set<String>> successDownloadFileMap = new HashMap<>();
    /**
     * 已经结束的文件下载任务，key: ExecuteObjectGseKey, value: 已完成文件任务名称
     */
    private final Map<ExecuteObjectGseKey, Set<String>> finishedDownloadFileMap = new HashMap<>();
    /**
     * 下载文件任务数，key: ExecuteObjectGseKey, value: 主机对应的文件任务数
     */
    private final Map<ExecuteObjectGseKey, Integer> fileDownloadTaskNumMap = new HashMap<>();
    /**
     * 成功的文件上传任务，key: ExecuteObjectGseKey, value: 成功的文件任务名称
     */
    private final Map<ExecuteObjectGseKey, Set<String>> successUploadFileMap = new HashMap<>();
    /**
     * 已经结束的文件上传任务，key: ExecuteObjectGseKey, value: 已完成文件任务名称
     */
    private final Map<ExecuteObjectGseKey, Set<String>> finishedUploadFileMap = new HashMap<>();
    /**
     * 上传文件任务数，key: ExecuteObjectGseKey, value: 主机对应的文件任务数
     */
    private final Map<ExecuteObjectGseKey, Integer> fileUploadTaskNumMap = new HashMap<>();
    /**
     * 文件任务进度表
     */
    private final Map<String, Integer> processMap = new HashMap<>();

    private final Map<JobFile, FileDest> srcDestFileMap;

    // @tmp 兼容gse agent < 1.7.2 之前的版本， Map<目标路径, Map<源IP,源文件>>
    private Map<String, Map<String, JobFile>> compatibleDestSrcMap = null;

    /**
     * 源文件. key: file-key; value: 文件
     */
    private final Map<String, JobFile> srcFilesMap = new HashMap<>();

    /**
     * 下载全部结束的时间
     */
    private long downloadFinishedTime = 0;
    /**
     * 未结束的目标服务器
     */
    protected Set<ExecuteObjectGseKey> notFinishedSourceExecuteObjectGseKeys = new HashSet<>();
    /**
     * 文件任务执行结果处理调度策略
     */
    private volatile ScheduleStrategy scheduleStrategy;
    /**
     * 任务基本信息，用于日志输出
     */
    private String taskInfo;
    /**
     * 是否包含非法文件源
     */
    protected boolean hasInvalidSourceExecuteObject;


    public FileResultHandleTask(EngineDependentServiceHolder engineDependentServiceHolder,
                                FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                JobExecuteConfig jobExecuteConfig,
                                TaskInstanceDTO taskInstance,
                                StepInstanceDTO stepInstance,
                                TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                Map<ExecuteObjectGseKey, ExecuteObjectTask> targetExecuteObjectTaskMap,
                                Map<ExecuteObjectGseKey, ExecuteObjectTask> sourceExecuteObjectTaskMap,
                                GseTaskDTO gseTask,
                                Map<JobFile, FileDest> srcDestFileMap,
                                String requestId,
                                List<ExecuteObjectTask> executeObjectTasks) {
        super(engineDependentServiceHolder,
            fileExecuteObjectTaskService,
            jobExecuteConfig,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            targetExecuteObjectTaskMap,
            gseTask,
            requestId,
            executeObjectTasks);
        this.sourceExecuteObjectTaskMap = sourceExecuteObjectTaskMap;
        this.srcDestFileMap = srcDestFileMap;

        initSrcFilesMap(srcDestFileMap.keySet());
        initFileTaskNumMap();
        initSourceExecuteObjectGseKeys();

        log.info("InitFileResultHandleTask|stepInstanceId: {}|sourceExecuteObjectGseKeys: {}"
                + "|targetExecuteObjectGseKeys: {}|fileUploadTaskNumMap: {}|fileDownloadTaskNumMap: {}",
            stepInstance.getId(), sourceExecuteObjectGseKeys, targetExecuteObjectGseKeys, fileUploadTaskNumMap,
            fileDownloadTaskNumMap);
    }

    private void initSrcFilesMap(Collection<JobFile> srcFiles) {
        srcFiles.forEach(srcFile ->
            srcFilesMap.put(
                buildSrcFileKey(srcFile.getExecuteObject().toExecuteObjectGseKey(), srcFile.getStandardFilePath()),
                srcFile)
        );
    }

    private String buildSrcFileKey(ExecuteObjectGseKey executeObjectGseKey, String standardSrcFilePath) {
        return executeObjectGseKey.getKey() + ":" + standardSrcFilePath;
    }

    /**
     * 初始化文件任务计数器
     */
    private void initFileTaskNumMap() {
        for (ExecuteObjectGseKey executeObjectGseKey : this.targetExecuteObjectGseKeys) {
            this.fileDownloadTaskNumMap.put(executeObjectGseKey, this.srcDestFileMap.size());
        }

        for (JobFile sendFile : this.srcDestFileMap.keySet()) {
            ExecuteObjectGseKey executeObjectGseKey = sendFile.getExecuteObject().toExecuteObjectGseKey();
            this.fileUploadTaskNumMap.put(executeObjectGseKey,
                this.fileUploadTaskNumMap.get(executeObjectGseKey) == null ?
                    1 : (this.fileUploadTaskNumMap.get(executeObjectGseKey) + 1));
        }
    }

    private void initSourceExecuteObjectGseKeys() {
        sourceExecuteObjectTaskMap.values().forEach(executeObjectTask -> {
            this.notFinishedSourceExecuteObjectGseKeys.add(
                executeObjectTask.getExecuteObject().toExecuteObjectGseKey());
            this.sourceExecuteObjectGseKeys.add(executeObjectTask.getExecuteObject().toExecuteObjectGseKey());
        });
    }

    @Override
    GseLogBatchPullResult<FileTaskResult> pullGseTaskResultInBatches() {
        GetTransferFileResultRequest request = new GetTransferFileResultRequest();
        request.setGseV2Task(gseV2Task);
        request.setTaskId(gseTask.getGseTaskId());

        if (CollectionUtils.isNotEmpty(this.analyseFinishedSourceExecuteObjectGseKeys)
            || CollectionUtils.isNotEmpty(this.analyseFinishedTargetExecuteObjectGseKeys)) {
            List<ExecuteObjectGseKey> notFinishedExecuteObjectGseKeys = new ArrayList<>();
            notFinishedExecuteObjectGseKeys.addAll(notFinishedSourceExecuteObjectGseKeys);
            notFinishedExecuteObjectGseKeys.addAll(notFinishedTargetExecuteObjectGseKeys);
            request.batchAddAgentQuery(notFinishedExecuteObjectGseKeys);
        }
        FileTaskResult result = gseClient.getTransferFileResult(request);
        GseLogBatchPullResult<FileTaskResult> pullResult;
        if (result != null) {
            pullResult = new GseLogBatchPullResult<>(true, new FileGseTaskResult(result));
        } else {
            pullResult = new GseLogBatchPullResult<>(true, null);
        }
        return pullResult;
    }

    @Override
    GseTaskExecuteResult analyseGseTaskResult(GseTaskResult<FileTaskResult> taskDetail) {
        if (taskDetail == null || taskDetail.isEmptyResult()) {
            log.info("Analyse gse task result, result is empty!");
            return analyseExecuteResult();
        }
        // 执行日志Map,按照执行对象分组
        Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs = new HashMap<>();

        StopWatch watch = new StopWatch("analyse-gse-file-task");
        watch.start("analyse");
        for (AtomicFileTaskResult result : taskDetail.getResult().getAtomicFileTaskResults()) {
            compatibleProtocolBeforeV2(result.getContent());
            if (!shouldAnalyse(result)) {
                continue;
            }

            log.info("[{}] Analyse file result: {}", gseTaskInfo, JsonUtils.toJson(result));
            JobAtomicFileTaskResult jobAtomicFileTaskResult = buildJobAtomicFileTaskResult(result);


            AtomicFileTaskResultContent content = result.getContent();
            boolean isDownloadResult = content.isDownloadMode();
            ExecuteObjectGseKey executeObjectGseKey = isDownloadResult ?
                content.getDestExecuteObjectGseKey() : content.getSourceExecuteObjectGseKey();
            analyseFileResult(executeObjectGseKey, jobAtomicFileTaskResult, executionLogs, isDownloadResult);
        }
        watch.stop();

        // 保存文件分发日志
        watch.start("saveFileLog");
        writeFileTaskLogContent(executionLogs);
        watch.stop();

        // 保存任务执行结果
        watch.start("saveExecuteObjectTasks");
        batchSaveChangedExecuteObjectTasks(targetExecuteObjectTasks.values());
        batchSaveChangedExecuteObjectTasks(sourceExecuteObjectTaskMap.values());
        watch.stop();

        log.info("[{}] Analyse gse task result -> "
                + "notFinishedTargetExecuteObjectGseKeys={}, notFinishedSourceExecuteObjectGseKeys={}, "
                + "analyseFinishedTargetExecuteObjectGseKeys={}, analyseFinishedSourceExecuteObjectGseKeys={}"
                + ", finishedDownloadFileMap={}, successDownloadFileMap={}, finishedUploadFileMap={},"
                + " successUploadFileMap={}",
            this.gseTaskInfo,
            this.notFinishedTargetExecuteObjectGseKeys,
            this.notFinishedSourceExecuteObjectGseKeys,
            this.analyseFinishedTargetExecuteObjectGseKeys,
            this.analyseFinishedSourceExecuteObjectGseKeys,
            this.finishedDownloadFileMap,
            this.successDownloadFileMap,
            this.finishedUploadFileMap,
            this.successUploadFileMap
        );

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("Analyse file gse task is slow, statistics: {}", watch.prettyPrint());
        }
        return analyseExecuteResult();
    }

    private void compatibleProtocolBeforeV2(AtomicFileTaskResultContent content) {
        if (content == null) {
            return;
        }
        if (content.isApiProtocolBeforeV2() && content.isDownloadMode() && isSupportProtocolBeforeV2()) {
            // 老版本协议(协议版本<2.0，gse agent 版本 < 1.7.2),下载任务结果存在问题（没有源主机云区域ID、没有源文件路径), 需要根据任务上下文推测并补充
            if (compatibleDestSrcMap == null) {
                compatibleDestSrcMap = new HashMap<>();
                srcDestFileMap.forEach((jobFile, fileDest) -> {
                    // GSE BUG, 如果是目录分发，那么下载结果中只会返回目标目录的上一级
                    // 例如：download:-1:-1:/tmp/test/:0:127.0.0.1， 表示从源分发一个目录到目标/tmp/test/目录下
                    String destPath = jobFile.isDir() ? fileDest.getDestDirPath() : fileDest.getDestPath();
                    compatibleDestSrcMap.compute(destPath, (dest, map) -> {
                        if (map == null) {
                            map = new HashMap<>();
                        }
                        if (StringUtils.isNotBlank(jobFile.getExecuteObject().getHost().getIp())) {
                            map.put(jobFile.getExecuteObject().getHost().getIp(), jobFile);
                        }
                        return map;
                    });
                });
                log.info("[CompatibleProtocolBeforeV2] Init destSrcMap: {}", compatibleDestSrcMap);
            }
            String destPath = content.getStandardDestFilePath();
            Map<String, JobFile> srcIpAndSrcFile = compatibleDestSrcMap.get(destPath);
            if (srcIpAndSrcFile == null) {
                log.error("[CompatibleProtocolBeforeV2] Can not get srcFile by destPath: {}. ", destPath);
                return;
            }
            String srcAgentId = content.getSourceAgentId();
            // 源主机没有包含云区域信息，需要处理
            String srcIp = IpUtils.extractIp(srcAgentId);
            JobFile srcFile = srcIpAndSrcFile.get(srcIp);
            if (srcFile == null) {
                log.error("[CompatibleProtocolBeforeV2] Can not get srcFile by destPath: {} and srcIp: {}. ",
                    destPath, srcIp);
                return;
            }
            content.setSourceAgentId(srcFile.getExecuteObject().getHost().toCloudIp());
            content.setSourceFileDir(srcFile.getDir());
            content.setSourceFileName(srcFile.getFileName());
            // 重置
            content.setStandardSourceFilePath(null);
            content.setTaskId(null);
        }
    }

    private boolean isSupportProtocolBeforeV2() {
        return FeatureToggle.checkFeature(
            FeatureIdConstants.GSE_FILE_PROTOCOL_BEFORE_V2,
            FeatureExecutionContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE,
                    GlobalAppScopeMappingService.get().getScopeByAppId(appId))
        );
    }

    private void analyseFileResult(ExecuteObjectGseKey executeObjectGseKey,
                                   JobAtomicFileTaskResult result,
                                   Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs,
                                   boolean isDownloadResult) {
        ExecuteObjectTask executeObjectTask = result.getExecuteObjectTask();
        if (executeObjectTask.getStartTime() == null) {
            executeObjectTask.setStartTime(System.currentTimeMillis());
        }
        executeObjectTask.setErrorCode(result.getResult().getErrorCode());
        GSECode.AtomicErrorCode errorCode = GSECode.AtomicErrorCode.getErrorCode(result.getResult().getErrorCode());
        switch (errorCode) {
            case RUNNING:
                analyseRunningFileResult(result, executionLogs, executeObjectTask);
                break;
            case FINISHED:
                analyseSuccessFileResult(executeObjectGseKey, result, executionLogs, isDownloadResult);
                break;
            case TERMINATE:
                analyseTerminatedFileResult(executeObjectGseKey, result, executionLogs, isDownloadResult);
                break;
            default:
                analyseFailedFileResult(result, executionLogs, isDownloadResult);
                break;
        }
    }

    private void analyseRunningFileResult(JobAtomicFileTaskResult result,
                                          Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs,
                                          ExecuteObjectTask executeObjectTask) {
        parseExecutionLog(result, executionLogs);
        executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.RUNNING);
        executeObjectTask.setStartTime(result.getResult().getContent().getStartTime());
    }

    private void analyseSuccessFileResult(ExecuteObjectGseKey executeObjectGseKey,
                                          JobAtomicFileTaskResult result,
                                          Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs,
                                          boolean isDownloadResult) {
        AtomicFileTaskResultContent content = result.getResult().getContent();
        parseExecutionLog(result, executionLogs);
        if (isDownloadResult) {
            addFinishedFile(true, true,
                content.getDestExecuteObjectGseKey(), content.getTaskId());
        } else {
            addFinishedFile(true, false,
                content.getSourceExecuteObjectGseKey(), content.getTaskId());
        }
        // 分析执行对象任务结果
        analyseExecuteObjectTaskResult(GSECode.AtomicErrorCode.FINISHED.getValue(), executeObjectGseKey,
            content.getStartTime(), content.getEndTime(), isDownloadResult);
    }

    private void analyseTerminatedFileResult(ExecuteObjectGseKey executeObjectGseKey,
                                             JobAtomicFileTaskResult result,
                                             Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs,
                                             boolean isDownloadResult) {
        AtomicFileTaskResultContent content = result.getResult().getContent();
        parseExecutionLog(result, executionLogs);
        if (isDownloadResult) {
            addFinishedFile(false, true,
                content.getDestExecuteObjectGseKey(), content.getTaskId());
        } else {
            addFinishedFile(false, false,
                content.getSourceExecuteObjectGseKey(), content.getTaskId());
        }
        analyseExecuteObjectTaskResult(GSECode.AtomicErrorCode.TERMINATE.getValue(), executeObjectGseKey,
            content.getStartTime(), content.getEndTime(), isDownloadResult);
        this.isTerminatedSuccess = true;
    }

    private JobAtomicFileTaskResult buildJobAtomicFileTaskResult(AtomicFileTaskResult atomicFileTaskResult) {
        AtomicFileTaskResultContent content = atomicFileTaskResult.getContent();
        boolean isDownloadResult = content.isDownloadMode();
        ExecuteObjectGseKey executeObjectGseKey = isDownloadResult ?
            content.getDestExecuteObjectGseKey() : content.getSourceExecuteObjectGseKey();
        JobFile srcFile = srcFilesMap.get(buildSrcFileKey(content.getSourceExecuteObjectGseKey(),
            content.getStandardSourceFilePath()));
        if (srcFile == null) {
            log.error("Src file not found, sourceAgentId: {}, filePath:{}", content.getSourceAgentId(),
                content.getStandardSourceFilePath());
            throw new InternalException("Parse src file fail", ErrorCode.INTERNAL_ERROR);
        }

        ExecuteObjectTask executeObjectTask = getExecuteObjectTask(isDownloadResult, executeObjectGseKey);

        ExecuteObject sourceExecuteObject = findExecuteObject(sourceExecuteObjectTaskMap,
            content.getSourceExecuteObjectGseKey());
        if (sourceExecuteObject == null) {
            log.error("Src execute object not found, sourceExecuteObjectGseKey: {}",
                content.getSourceExecuteObjectGseKey());
            throw new InternalException("Parse src execute object fail", ErrorCode.INTERNAL_ERROR);
        }

        ExecuteObject targetExecuteObject = null;
        if (isDownloadResult) {
            targetExecuteObject = findExecuteObject(targetExecuteObjectTasks,
                content.getDestExecuteObjectGseKey());
            if (targetExecuteObject == null) {
                log.error("Target execute object not found, destExecuteObjectGseKey: {}",
                    content.getDestExecuteObjectGseKey());
                throw new InternalException("Parse target host fail", ErrorCode.INTERNAL_ERROR);
            }
        }

        return new JobAtomicFileTaskResult(atomicFileTaskResult, sourceExecuteObject, targetExecuteObject,
            srcFile, executeObjectTask);
    }

    private ExecuteObject findExecuteObject(Map<ExecuteObjectGseKey, ExecuteObjectTask> executeObjectTaskMap,
                                            ExecuteObjectGseKey executeObjectGseKey) {
        ExecuteObjectTask executeObjectTask = executeObjectTaskMap.get(executeObjectGseKey);
        if (executeObjectGseKey == null) {
            return null;
        }
        return executeObjectTask.getExecuteObject();
    }

    private ExecuteObjectTask getExecuteObjectTask(boolean isDownloadResult,
                                                   ExecuteObjectGseKey executeObjectGseKey) {
        if (isDownloadResult) {
            return targetExecuteObjectTasks.get(executeObjectGseKey);
        } else {
            return sourceExecuteObjectTaskMap.get(executeObjectGseKey);
        }
    }

    /**
     * 分析执行结果
     *
     * @return 任务执行结果
     */
    private GseTaskExecuteResult analyseExecuteResult() {
        GseTaskExecuteResult rst;
        // 目标下载全部完成
        if (isAllTargetExecuteObjectTasksDone()) {
            // 源上传全部完成
            if (isAllSourceExecuteObjectTasksDone()) {
                rst = analyseFinishedExecuteResult();
                log.info("[{}] AnalyseExecuteResult-> Result: finished. All source and target execute object " +
                        "have completed tasks",
                    this.stepInstanceId);
            } else {
                // 场景：下载任务已全部结束，但是GSE未更新上传任务的状态。如果超过15s没有结束上传任务，那么任务结束
                if (this.downloadFinishedTime == 0) {
                    this.downloadFinishedTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - this.downloadFinishedTime > 15_000L) {
                    rst = analyseFinishedExecuteResult();
                    log.info("[{}] AnalyseExecuteResult-> Result: finished. Download tasks are finished, " +
                            "but upload tasks are not finished after 15 seconds. Ignore upload tasks",
                        this.stepInstanceId);
                } else {
                    rst = GseTaskExecuteResult.RUNNING;
                    log.info("[{}] AnalyseExecuteResult-> Result: running. Download tasks are finished, " +
                        "wait for upload task to complete.", this.stepInstanceId);
                }
            }

        } else {
            rst = GseTaskExecuteResult.RUNNING;
            log.info("[{}] AnalyseExecuteResult-> Result: running. Download tasks has not been finished",
                this.stepInstanceId);
        }
        return rst;
    }

    private boolean isAllSourceExecuteObjectTasksDone() {
        return this.notFinishedSourceExecuteObjectGseKeys.isEmpty()
            && this.notFinishedTargetExecuteObjectGseKeys.isEmpty();
    }

    private boolean shouldAnalyse(AtomicFileTaskResult result) {
        if (result == null) {
            return false;
        }

        AtomicFileTaskResultContent content = result.getContent();
        boolean isDownloadMode = content.isDownloadMode();
        ExecuteObjectGseKey executeObjectGseKey = isDownloadMode ?
            content.getDestExecuteObjectGseKey() : content.getSourceExecuteObjectGseKey();
        if (isDownloadMode) {
            if (this.analyseFinishedTargetExecuteObjectGseKeys.contains(executeObjectGseKey) // 该Agent已经分析结束，不需要再分析
                // 该文件下载任务已结束
                || (this.finishedDownloadFileMap.get(executeObjectGseKey) != null
                && this.finishedDownloadFileMap.get(executeObjectGseKey).contains(content.getTaskId()))) {
                return false;
            }
            // 不属于当前任务的目标Agent
            if (!this.fileDownloadTaskNumMap.containsKey(executeObjectGseKey)) {
                log.warn("[{}] Unexpected target executeObjectGseKey {}. result: {}", gseTaskInfo, executeObjectGseKey,
                    JsonUtils.toJson(result));
                return false;
            }
        } else {
            if (this.analyseFinishedSourceExecuteObjectGseKeys.contains(executeObjectGseKey) // 该Agent已经分析结束，不需要再分析
                || (this.finishedUploadFileMap.get(executeObjectGseKey) != null
                && this.finishedUploadFileMap.get(executeObjectGseKey).contains(content.getTaskId()))) {
                return false;
            }
            // 不属于当前任务的源Agent
            if (!this.fileUploadTaskNumMap.containsKey(executeObjectGseKey)) {
                log.warn("[{}] Unexpected source executeObjectGseKey {}. result: {}", gseTaskInfo, executeObjectGseKey,
                    JsonUtils.toJson(result));
                return false;
            }
        }
        return true;
    }

    private void analyseFailedFileResult(JobAtomicFileTaskResult result,
                                         Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs,
                                         boolean isDownloadResult) {
        if (isDownloadResult) {
            dealDownloadTaskFail(result.getResult(), executionLogs);
        } else {
            dealUploadTaskFail(result, executionLogs);
        }
    }

    private void dealDownloadTaskFail(AtomicFileTaskResult result,
                                      Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs) {
        AtomicFileTaskResultContent content = result.getContent();
        dealDownloadTaskFail(executionLogs, content.getSourceExecuteObjectGseKey(), content.getStandardSourceFilePath(),
            content.getDestExecuteObjectGseKey(), content.getStandardDestFilePath(), result.getErrorCode(),
            buildErrorLogContent(result), content.getStartTime(), content.getEndTime());
    }

    private void dealDownloadTaskFail(Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs,
                                      ExecuteObjectGseKey sourceExecuteObjectGseKey,
                                      String sourceFilePath,
                                      ExecuteObjectGseKey destExecuteObjectGseKey,
                                      String destFilePath,
                                      Integer errorCode,
                                      String errorMsg,
                                      Long startTime,
                                      Long endTime) {
        JobFile srcFile = srcFilesMap.get(buildSrcFileKey(sourceExecuteObjectGseKey, sourceFilePath));
        ExecuteObject targetExecuteObject = findExecuteObject(targetExecuteObjectTasks, destExecuteObjectGseKey);
        FileDest fileDest = srcDestFileMap.get(srcFile);

        // 记录目标IP单个文件下载任务的结束状态
        addFinishedFile(false,
            true,
            destExecuteObjectGseKey,
            AtomicFileTaskResultContent.buildTaskId(
                FileDistModeEnum.DOWNLOAD.getValue(),
                sourceExecuteObjectGseKey,
                sourceFilePath,
                destExecuteObjectGseKey,
                destFilePath)
        );
        // 每个目标执行对象增加一条下载失败的日志到日志总Map中
        logService.addFileTaskLog(
            stepInstance,
            executionLogs,
            targetExecuteObject,
            logService.buildDownloadServiceFileTaskLogDTO(
                stepInstance,
                srcFile,
                targetExecuteObject,
                fileDest.getDestPath(),
                FileDistStatusEnum.FAILED,
                null,
                null,
                null,
                errorMsg
            )
        );
        analyseExecuteObjectTaskResult(errorCode, destExecuteObjectGseKey, getTimeOrDefault(startTime),
            getTimeOrDefault(endTime), true);
    }

    private long getTimeOrDefault(Long time) {
        return time != null && time > 0 ? time : System.currentTimeMillis();
    }


    /**
     * 处理下载失败任务
     *
     * @param result        任务结果
     * @param executionLogs 执行日志总Map
     */
    private void dealUploadTaskFail(JobAtomicFileTaskResult result,
                                    Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs) {
        AtomicFileTaskResultContent content = result.getResult().getContent();
        ExecuteObjectGseKey sourceExecuteObjectGseKey = content.getSourceExecuteObjectGseKey();
        JobFile srcFile = result.getSrcFile();
        Long startTime = getTimeOrDefault(content.getStartTime());
        Long endTime = getTimeOrDefault(content.getEndTime());
        // 记录源执行对象单个文件上传任务的结束状态
        addFinishedFile(false, false, sourceExecuteObjectGseKey, content.getTaskId());

        // 增加一条上传源失败的上传日志
        logService.addFileTaskLog(
            stepInstance,
            executionLogs,
            srcFile.getExecuteObject(),
            logService.buildUploadServiceFileTaskLogDTO(
                stepInstance,
                srcFile,
                FileDistStatusEnum.FAILED,
                null,
                null,
                null,
                buildErrorLogContent(result.getResult()))
        );
        analyseExecuteObjectTaskResult(result.getResult().getErrorCode(), sourceExecuteObjectGseKey, startTime,
            endTime, false);

        // 源失败了，会影响所有目标执行对象对应的agent上的download任务
        for (ExecuteObjectGseKey targetExecuteObjectGseKey : this.targetExecuteObjectGseKeys) {
            FileDest fileDest = srcDestFileMap.get(srcFile);
            dealDownloadTaskFail(
                executionLogs,
                sourceExecuteObjectGseKey,
                content.getStandardSourceFilePath(),
                targetExecuteObjectGseKey,
                fileDest.getDestPath(),
                result.getResult().getErrorCode(),
                buildErrorLogContent(result.getResult()),
                startTime,
                endTime);
        }
    }

    private String buildErrorLogContent(AtomicFileTaskResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getErrorMsg()).append(".");
        AtomicFileTaskResultContent content = result.getContent();
        if (content != null && content.getStatus() != null) {
            sb.append(" Status: ").append(content.getStatus());
            if (StringUtils.isNotBlank(content.getStatusInfo())) {
                sb.append(", StatusDesc: ").append(content.getStatusInfo());
            }
        }
        return sb.toString();
    }


    /**
     * 分析并设置执行对象任务的状态
     *
     * @param errorCode           GSE错误码
     * @param executeObjectGseKey executeObjectGseKey
     * @param fileNum             文件总数
     * @param successNum          成功分发的文件总数
     * @param isDownload          是否为下载结果
     * @param executeObjectTask   执行对象任务
     */
    private void analyseExecuteObjectTaskStatus(int errorCode,
                                                ExecuteObjectGseKey executeObjectGseKey,
                                                int fileNum,
                                                int successNum,
                                                boolean isDownload,
                                                ExecuteObjectTask executeObjectTask) {
        // 文件任务成功数=任务总数
        if (successNum >= fileNum) {
            if (hasInvalidSourceExecuteObject) {
                // 如果包含了非法的源文件主机，即使GSE任务（已过滤非法主机)执行成功，那么对于这个主机来说，整体上任务状态是失败
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.FAILED);
            } else {
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SUCCESS);
                if (isDownload) {
                    this.successTargetExecuteObjectGseKeys.add(executeObjectGseKey);
                }
            }
        } else {
            ExecuteObjectTaskStatusEnum executeObjectTaskStatus = ExecuteObjectTaskStatusEnum.FAILED;
            if (errorCode != 0) {
                executeObjectTaskStatus = GseUtils.getStatusByGseErrorCode(errorCode);
            }
            executeObjectTask.setStatus(executeObjectTaskStatus);
        }
    }

    /**
     * 分析并判定最终整体任务状态
     *
     * @param errorCode           GSE错误码
     * @param executeObjectGseKey executeObjectGseKey
     * @param startTime           任务起始时间
     * @param endTime             任务终止时间
     * @param isDownloadResult    是否为下载结果
     */
    private void analyseExecuteObjectTaskResult(int errorCode,
                                                ExecuteObjectGseKey executeObjectGseKey,
                                                long startTime,
                                                long endTime,
                                                boolean isDownloadResult) {
        int finishedNum;
        int fileNum;
        int successNum;
        if (isDownloadResult) {
            finishedNum = this.finishedDownloadFileMap.get(executeObjectGseKey) == null ? 0 :
                this.finishedDownloadFileMap.get(executeObjectGseKey).size();
            fileNum = this.fileDownloadTaskNumMap.get(executeObjectGseKey) == null ? 0 :
                this.fileDownloadTaskNumMap.get(executeObjectGseKey);
            successNum = this.successDownloadFileMap.get(executeObjectGseKey) == null ? 0 :
                this.successDownloadFileMap.get(executeObjectGseKey).size();
        } else {
            finishedNum = this.finishedUploadFileMap.get(executeObjectGseKey) == null ? 0 :
                this.finishedUploadFileMap.get(executeObjectGseKey).size();
            successNum = this.successUploadFileMap.get(executeObjectGseKey) == null ? 0 :
                this.successUploadFileMap.get(executeObjectGseKey).size();
            fileNum = this.fileUploadTaskNumMap.get(executeObjectGseKey) == null ?
                0 : this.fileUploadTaskNumMap.get(executeObjectGseKey);
        }

        ExecuteObjectTask executeObjectTask = getExecuteObjectTask(isDownloadResult, executeObjectGseKey);
        if (finishedNum >= fileNum) {
            log.info("[{}] Analyse Agent task finished! executeObjectGseKey: {}, finishedTaskNum: {}, " +
                "expectedTaskNum: {}", gseTaskInfo, executeObjectGseKey, finishedNum, fileNum);
            // 更新执行对象任务结果
            if (isDownloadResult) {
                dealTargetExecuteObjectFinish(executeObjectGseKey, startTime, endTime, executeObjectTask);
            } else {
                dealUploadAgentFinished(executeObjectGseKey, startTime, endTime, executeObjectTask);
            }
            analyseExecuteObjectTaskStatus(errorCode, executeObjectGseKey, fileNum, successNum, isDownloadResult,
                executeObjectTask);
        } else {
            executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.RUNNING);
        }
    }

    /**
     * 设置源执行对象任务结束状态
     *
     * @param executeObjectGseKey executeObjectGseKey
     * @param startTime           起始时间
     * @param endTime             终止时间
     * @param executeObjectTask   执行对象任务
     */
    private void dealUploadAgentFinished(ExecuteObjectGseKey executeObjectGseKey,
                                         Long startTime,
                                         Long endTime,
                                         ExecuteObjectTask executeObjectTask) {
        log.info("[{}]: Deal source agent finished| executeObjectGseKey={}| startTime:{}, endTime:{}, " +
                "executeObjectTask:{}",
            gseTaskInfo, executeObjectGseKey, startTime, endTime,
            JsonUtils.toJsonWithoutSkippedFields(executeObjectTask));

        this.notFinishedSourceExecuteObjectGseKeys.remove(executeObjectGseKey);
        this.analyseFinishedSourceExecuteObjectGseKeys.add(executeObjectGseKey);
        if (endTime - startTime <= 0) {
            executeObjectTask.setTotalTime(100L);
        } else {
            executeObjectTask.setTotalTime(endTime - startTime);
        }
        executeObjectTask.setStartTime(startTime);
        executeObjectTask.setEndTime(endTime);
    }

    /**
     * 从执行结果生成执行日志
     */
    private void parseExecutionLog(JobAtomicFileTaskResult result,
                                   Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs) {
        AtomicFileTaskResultContent content = result.getResult().getContent();
        Integer mode = content.getMode();
        JobFile srcFile = result.getSrcFile();
        boolean isDownloadResult = isDownloadResult(mode);
        FileDistStatusEnum status = parseFileTaskStatus(result.getResult(), isDownloadResult);
        GSECode.AtomicErrorCode errorCode = GSECode.AtomicErrorCode.getErrorCode(result.getResult().getErrorCode());
        String key = content.getTaskId();
        Integer process = processMap.computeIfAbsent(key, k -> -1);
        if (errorCode == GSECode.AtomicErrorCode.RUNNING && process.equals(content.getProgress())) {
            return;
        }
        // 由于GSE Bug,当状态为Finished的时候，偶现progress不为100的情况；为了避免用户理解歧义，统一以任务状态为准
        processMap.put(key, status == FileDistStatusEnum.FINISHED ? 100 : content.getProgress());

        String displayFilePath = isDownloadResult ? content.getStandardDestFilePath() :
            srcFile.getDisplayFilePath();
        String fileSize = "--";
        String speed = "";
        String progressText = "";

        if (content.getSize() != null && content.getSize() > 0) {
            fileSize = GseUtils.tranByteReadable(content.getSize());
        }
        if (content.getSpeed() != null) {
            speed = formatSpeed(content.getSpeed()) + " KB/s";
        }
        if (content.getProgress() != null) {
            progressText = content.getProgress() + "%";
        }
        String logContentStr = buildFileLogContent(displayFilePath, fileSize, content.getStatus(),
            content.getStatusInfo(), speed, progressText, result.getResult().getErrorMsg());

        if (isDownloadResult) {
            logService.addFileTaskLog(
                stepInstance,
                executionLogs,
                result.getTargetExecuteObject(),
                logService.buildDownloadServiceFileTaskLogDTO(
                    stepInstance, srcFile, result.getTargetExecuteObject(), content.getStandardDestFilePath(), status,
                    fileSize, speed, progressText, logContentStr));
        } else {
            logService.addFileTaskLog(
                stepInstance,
                executionLogs,
                result.getSourceExecuteObject(),
                logService.buildUploadServiceFileTaskLogDTO(
                    stepInstance, srcFile, status, fileSize, speed, progressText, logContentStr));
        }
    }

    private String buildFileLogContent(String displayFilePath,
                                       String fileSize,
                                       Integer status,
                                       String statusDesc,
                                       String speed,
                                       String progress,
                                       String detail) {
        StringBuilder logContent = new StringBuilder(512);
        if (displayFilePath.endsWith("/") || displayFilePath.endsWith("\\")) {
            // 传输的是目录，目录名以‘/’或‘\’结束
            logContent.append("Directory: ");
        } else {
            logContent.append("FileName: ");
        }
        logContent.append(displayFilePath);

        if (StringUtils.isNotBlank(fileSize)) {
            logContent.append(" FileSize: ").append(fileSize);
        }
        if (status != null) {
            logContent.append(" Status: ").append(status);
        }
        if (StringUtils.isNotBlank(statusDesc)) {
            logContent.append(" StatusDesc: ").append(statusDesc);
        }
        if (StringUtils.isNotBlank(speed)) {
            logContent.append(" Speed: ").append(speed);
        }
        if (StringUtils.isNotBlank(speed)) {
            logContent.append(" Progress: ").append(progress);
        }
        if (StringUtils.isNotBlank(detail)) {
            logContent.append(" Detail: ").append(detail);
        }
        return logContent.toString();
    }

    private FileDistStatusEnum parseFileTaskStatus(AtomicFileTaskResult result, boolean isDownloadResult) {
        FileDistStatusEnum status;
        if (result.getErrorCode().equals(GSECode.AtomicErrorCode.RUNNING.getValue())) {
            if (isDownloadResult) {
                status = FileDistStatusEnum.DOWNLOADING;
            } else {
                status = FileDistStatusEnum.UPLOADING;
            }
        } else if (result.getErrorCode().equals(GSECode.AtomicErrorCode.FINISHED.getValue())) {
            status = FileDistStatusEnum.FINISHED;
        } else {
            status = FileDistStatusEnum.FAILED;
        }
        return status;
    }

    private boolean isDownloadResult(Integer mode) {
        return FileDistModeEnum.DOWNLOAD.getValue().equals(mode);
    }

    private String formatSpeed(int speed) {
        DecimalFormat formatter = new DecimalFormat("###,###");
        return formatter.format(speed);
    }

    private void writeFileTaskLogContent(Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs) {
        if (!executionLogs.isEmpty()) {
            logService.writeFileLogs(taskInstance.getCreateTime(), new ArrayList<>(executionLogs.values()));
        }
    }

    /**
     * 向某个IP上传/下载文件的已结束状态Map、已成功状态Map中添加记录
     *
     * @param isSuccess           单个文件上传/下载任务是否成功
     * @param isDownloadMode      是否为下载
     * @param executeObjectGseKey executeObjectGseKey
     * @param taskId              单个文件任务唯一Key
     */
    private void addFinishedFile(boolean isSuccess,
                                 boolean isDownloadMode,
                                 ExecuteObjectGseKey executeObjectGseKey,
                                 String taskId) {
        if (isDownloadMode) {
            addFinishedFile(isSuccess, executeObjectGseKey, taskId, finishedDownloadFileMap, successDownloadFileMap);
        } else {
            addFinishedFile(isSuccess, executeObjectGseKey, taskId, finishedUploadFileMap, successUploadFileMap);
        }
    }

    private void addFinishedFile(boolean isSuccess,
                                 ExecuteObjectGseKey executeObjectGseKey,
                                 String taskId,
                                 Map<ExecuteObjectGseKey, Set<String>> finishedFileMap,
                                 Map<ExecuteObjectGseKey, Set<String>> successFileMap) {
        Set<String> finishedFileSet = finishedFileMap.computeIfAbsent(
            executeObjectGseKey, k -> Sets.newHashSet());
        finishedFileSet.add(taskId);

        if (isSuccess) {
            Set<String> successFileSet =
                successFileMap.computeIfAbsent(executeObjectGseKey, k -> Sets.newHashSet());
            successFileSet.add(taskId);
        }
    }

    @Override
    public boolean isFinished() {
        return !getExecuteResult().getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING);
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        if (scheduleStrategy == null) {
            this.scheduleStrategy = new FileTaskResultHandleScheduleStrategy(scheduleStrategyProperties.getFile());
        }
        return this.scheduleStrategy;
    }

    @Override
    public String toString() {
        if (this.taskInfo == null) {
            this.taskInfo = "FileTaskResultHandle-" + stepInstance.getTaskInstanceId() + "-" + stepInstance.getId();
        }
        return this.taskInfo;
    }

    @Getter
    @Setter
    @ToString
    private static class JobAtomicFileTaskResult {
        /**
         * GSE 文件任务结果
         */
        private AtomicFileTaskResult result;
        /**
         * 源执行对象
         */
        private ExecuteObject sourceExecuteObject;
        /**
         * 目标执行对象
         */
        private ExecuteObject targetExecuteObject;
        /**
         * 源文件
         */
        private JobFile srcFile;
        /**
         * Agent 任务
         */
        private ExecuteObjectTask executeObjectTask;

        public JobAtomicFileTaskResult(AtomicFileTaskResult result,
                                       ExecuteObject sourceExecuteObject,
                                       ExecuteObject targetExecuteObject,
                                       JobFile srcFile,
                                       ExecuteObjectTask executeObjectTask) {
            this.result = result;
            this.sourceExecuteObject = sourceExecuteObject;
            this.targetExecuteObject = targetExecuteObject;
            this.srcFile = srcFile;
            this.executeObjectTask = executeObjectTask;
        }
    }
}
