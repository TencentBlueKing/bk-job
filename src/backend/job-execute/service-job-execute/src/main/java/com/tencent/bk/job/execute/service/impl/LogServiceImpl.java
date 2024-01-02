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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.logsvr.api.ServiceLogResource;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectScriptLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@SuppressWarnings("Duplicates")
public class LogServiceImpl implements LogService {
    private final ServiceLogResource logResource;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final StepInstanceService stepInstanceService;

    @Autowired
    public LogServiceImpl(ServiceLogResource logResource,
                          ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                          FileExecuteObjectTaskService fileExecuteObjectTaskService,
                          StepInstanceService stepInstanceService) {
        this.logResource = logResource;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    public ServiceExecuteObjectScriptLogDTO buildSystemScriptLog(StepInstanceBaseDTO stepInstance,
                                                                 ExecuteObject executeObject,
                                                                 String content,
                                                                 int offset,
                                                                 Long logTimeInMillSeconds) {
        String logDateTime;
        if (logTimeInMillSeconds != null) {
            logDateTime = DateUtils.formatUnixTimestamp(logTimeInMillSeconds, ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        } else {
            logDateTime = DateUtils.formatUnixTimestamp(System.currentTimeMillis(), ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        }
        String logContentWithDateTime = "[" + logDateTime + "] " + content + "\n";
        return buildScriptLog(stepInstance, executeObject, logContentWithDateTime, offset);
    }

    @Override
    public ServiceExecuteObjectScriptLogDTO buildScriptLog(StepInstanceBaseDTO stepInstance,
                                                           ExecuteObject executeObject,
                                                           String content,
                                                           int offset) {

        int length = content.getBytes(StandardCharsets.UTF_8).length;
        if (stepInstance.isSupportExecuteObjectFeature()) {
            return new ServiceExecuteObjectScriptLogDTO(
                executeObject.getId(), content, offset + length);
        } else {
            // 兼容历史版本使用 hostId 的方式；发布完成并全量切换到执行对象特性之后，可删除这里的兼容代码
            HostDTO host = executeObject.getHost();
            return new ServiceExecuteObjectScriptLogDTO(
                host.getHostId(), host.toCloudIp(), host.toCloudIpv6(), content, offset + length);
        }
    }

    @Override
    public void batchWriteScriptLog(long jobCreateTime,
                                    long stepInstanceId,
                                    int executeCount,
                                    Integer batch,
                                    List<ServiceExecuteObjectScriptLogDTO> scriptLogs) {
        if (CollectionUtils.isEmpty(scriptLogs)) {
            return;
        }
        String jobCreateDate = DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        ServiceBatchSaveLogRequest request = new ServiceBatchSaveLogRequest();
        request.setJobCreateDate(jobCreateDate);
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        List<ServiceExecuteObjectLogDTO> logs = scriptLogs.stream()
            .map(scriptLog -> buildServiceExecuteObjectLogDTO(stepInstanceId, executeCount, batch, scriptLog))
            .collect(Collectors.toList());
        request.setLogs(logs);
        InternalResponse<?> resp = logResource.saveLogs(request);
        if (!resp.isSuccess()) {
            log.error("Batch write log content fail, stepInstanceId:{}, executeCount:{}, batch: {}",
                stepInstanceId, executeCount, batch);
            throw new InternalException(resp.getCode());
        }
    }

    private ServiceExecuteObjectLogDTO buildServiceExecuteObjectLogDTO(long stepInstanceId,
                                                                       int executeCount,
                                                                       Integer batch,
                                                                       ServiceExecuteObjectScriptLogDTO scriptLog) {
        ServiceExecuteObjectLogDTO logDTO = new ServiceExecuteObjectLogDTO();
        logDTO.setStepInstanceId(stepInstanceId);
        logDTO.setExecuteCount(executeCount);
        logDTO.setBatch(batch);
        logDTO.setExecuteObjectId(scriptLog.getExecuteObjectId());
        logDTO.setHostId(scriptLog.getHostId());
        logDTO.setCloudIp(scriptLog.getCloudIp());
        logDTO.setCloudIpv6(scriptLog.getCloudIpv6());
        logDTO.setScriptLog(scriptLog);
        return logDTO;
    }

    @Override
    public ScriptExecuteObjectLogContent getScriptExecuteObjectLogContent(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        ExecuteObjectCompositeKey executeObjectCompositeKey
    ) {
        ExecuteObjectTask executeObjectTask =
            scriptExecuteObjectTaskService.getTaskByExecuteObjectCompositeKey(stepInstance,
                executeCount, batch, executeObjectCompositeKey);
        return getScriptExecuteObjectLogContent(stepInstance, executeCount, batch, executeObjectTask);
    }

    private String buildTaskCreateDateStr(StepInstanceBaseDTO stepInstance) {
        return DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
    }

    private ScriptExecuteObjectLogContent convertToScriptExecuteObjectLogContent(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        ServiceExecuteObjectLogDTO executeObjectLog,
        ExecuteObjectTask executeObjectTask
    ) {
        if (executeObjectLog == null) {
            return null;
        }
        // 日志是否拉取完成
        boolean isFinished = executeObjectTask.getStatus().isFinished();
        String scriptContent = executeObjectLog.getScriptLog() != null ?
            executeObjectLog.getScriptLog().getContent() : "";
        return new ScriptExecuteObjectLogContent(stepInstance.getId(), executeCount,
            executeObjectTask.getExecuteObject(), scriptContent, isFinished);
    }

    @Override
    public ScriptExecuteObjectLogContent getScriptExecuteObjectLogContent(StepInstanceBaseDTO stepInstance,
                                                                          int executeCount,
                                                                          Integer batch,
                                                                          ExecuteObjectTask executeObjectTask) {
        String taskCreateDateStr = buildTaskCreateDateStr(stepInstance);
        long stepInstanceId = stepInstance.getId();
        InternalResponse<ServiceExecuteObjectLogDTO> resp;

        // 如果存在重试，那么该任务可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        if (executeCount > 0 && executeObjectTask.getActualExecuteCount() != null) {
            actualExecuteCount = executeObjectTask.getActualExecuteCount();
        }

        ExecuteObject executeObject = executeObjectTask.getExecuteObject();
        if (stepInstance.isSupportExecuteObjectFeature()) {
            resp = logResource.getScriptLogByExecuteObjectId(taskCreateDateStr,
                stepInstanceId, actualExecuteCount, executeObject.getId(), batch);
        } else {
            // 兼容hostId查询
            resp = logResource.getScriptHostLogByHostId(taskCreateDateStr,
                stepInstanceId, actualExecuteCount, executeObject.getHost().getHostId(), batch);
        }

        return convertToScriptExecuteObjectLogContent(stepInstance, executeCount, resp.getData(), executeObjectTask);
    }

    @Override
    public List<ScriptExecuteObjectLogContent> batchGetScriptExecuteObjectLogContent(
        String jobCreateDateStr,
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        List<ExecuteObjectCompositeKey> executeObjectCompositeKeys
    ) {
        if (CollectionUtils.isEmpty(executeObjectCompositeKeys)) {
            return Collections.emptyList();
        }
        ServiceScriptLogQueryRequest query = new ServiceScriptLogQueryRequest();
        query.setBatch(batch);

        List<ExecuteObject> queryExecuteObjects =
            stepInstanceService.findExecuteObjectByCompositeKeys(stepInstance, executeObjectCompositeKeys);
        if (stepInstance.isSupportExecuteObjectFeature()) {
            List<String> executeObjectId = queryExecuteObjects.stream()
                .map(ExecuteObject::getId).collect(Collectors.toList());
            query.setExecuteObjectIds(executeObjectId);
        } else {
            // 兼容 hostId 查询
            List<Long> hostIds = queryExecuteObjects.stream()
                .map(executeObject -> executeObject.getHost().getHostId()).collect(Collectors.toList());
            query.setHostIds(hostIds);
        }

        long stepInstanceId = stepInstance.getId();
        InternalResponse<List<ServiceExecuteObjectLogDTO>> resp =
            logResource.listScriptExecuteObjectLogs(jobCreateDateStr, stepInstanceId, executeCount, query);
        if (!resp.isSuccess()) {
            log.error("Get script log content by execute objects error, stepInstanceId={}, executeCount={}, batch={}," +
                "executeObjectCompositeKeys: {}", stepInstanceId, executeCount, batch, executeObjectCompositeKeys);
            throw new InternalException(resp.getCode());
        }
        if (CollectionUtils.isEmpty(resp.getData())) {
            return Collections.emptyList();
        }

        if (stepInstance.isSupportExecuteObjectFeature()) {
            Map<String, ExecuteObject> executeObjectMap = queryExecuteObjects.stream()
                .collect(Collectors.toMap(ExecuteObject::getId, executeObject -> executeObject));
            return resp.getData().stream().map(logDTO -> {
                String scriptContent = logDTO.getScriptLog() != null ?
                    logDTO.getScriptLog().getContent() : "";
                return new ScriptExecuteObjectLogContent(logDTO.getStepInstanceId(), logDTO.getExecuteCount(),
                    executeObjectMap.get(logDTO.getExecuteObjectId()), scriptContent, true);
            }).collect(Collectors.toList());
        } else {
            Map<Long, ExecuteObject> executeObjectMap = queryExecuteObjects.stream()
                .collect(Collectors.toMap(
                    executeObject -> executeObject.getHost().getHostId(), executeObject -> executeObject));
            return resp.getData().stream().map(logDTO -> {
                String scriptContent = logDTO.getScriptLog() != null ?
                    logDTO.getScriptLog().getContent() : "";
                return new ScriptExecuteObjectLogContent(logDTO.getStepInstanceId(), logDTO.getExecuteCount(),
                    executeObjectMap.get(logDTO.getHostId()), scriptContent, true);
            }).collect(Collectors.toList());
        }
    }

    @Override
    public FileExecuteObjectLogContent getFileExecuteObjectLogContent(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        ExecuteObjectCompositeKey executeObjectCompositeKey,
        Integer mode
    ) {
        ExecuteObjectTask executeObjectTask = fileExecuteObjectTaskService.getTaskByExecuteObjectCompositeKey(
            stepInstance, executeCount, batch, FileTaskModeEnum.getFileTaskMode(mode), executeObjectCompositeKey);
        return getFileExecuteObjectLogContent(stepInstance, executeCount, batch, executeObjectTask);
    }

    private boolean isAllFileTasksFinished(List<ServiceFileTaskLogDTO> fileTaskLogs) {
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return false;
        }
        return fileTaskLogs.stream().noneMatch(this::isFileTaskNotFinished);
    }

    private boolean isFileTaskNotFinished(ServiceFileTaskLogDTO fileTaskLog) {
        FileDistStatusEnum status = FileDistStatusEnum.getFileDistStatus(fileTaskLog.getStatus());
        if (status == null) {
            return true;
        }
        return status == FileDistStatusEnum.DOWNLOADING || status == FileDistStatusEnum.UPLOADING
            || status == FileDistStatusEnum.PULLING || status == FileDistStatusEnum.WAITING;
    }

    @Override
    public FileExecuteObjectLogContent getFileExecuteObjectLogContent(StepInstanceBaseDTO stepInstance,
                                                                      int executeCount,
                                                                      Integer batch,
                                                                      ExecuteObjectTask executeObjectTask) {
        // 如果存在重试，那么该任务可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        if (executeCount > 0 && executeObjectTask.getActualExecuteCount() != null) {
            actualExecuteCount = executeObjectTask.getActualExecuteCount();
        }

        String taskCreateDateStr = buildTaskCreateDateStr(stepInstance);
        long stepInstanceId = stepInstance.getId();
        InternalResponse<ServiceExecuteObjectLogDTO> resp;
        ExecuteObject executeObject = executeObjectTask.getExecuteObject();
        if (stepInstance.isSupportExecuteObjectFeature()) {
            resp = logResource.getFileLogByExecuteObjectId(taskCreateDateStr, stepInstanceId, actualExecuteCount,
                executeObject.getId(), executeObjectTask.getFileTaskMode().getValue(), batch);
        } else {
            // 兼容hostId查询
            resp = logResource.getFileHostLogByHostId(taskCreateDateStr, stepInstanceId, actualExecuteCount,
                executeObject.getHost().getHostId(), executeObjectTask.getFileTaskMode().getValue(), batch);
        }

        List<ServiceFileTaskLogDTO> fileTaskLogs = (resp.getData() == null) ? null : resp.getData().getFileTaskLogs();
        ExecuteObjectTaskStatusEnum executeObjectTaskStatus = executeObjectTask.getStatus();
        boolean isFinished = executeObjectTaskStatus.isFinished() || isAllFileTasksFinished(fileTaskLogs);
        return new FileExecuteObjectLogContent(stepInstanceId, executeCount, executeObject,
            batchConvertToAtomicFileTaskLog(stepInstance, fileTaskLogs), isFinished);
    }

    @Override
    public List<AtomicFileTaskLog> getAtomicFileTaskLogByTaskIds(long stepInstanceId,
                                                                 int executeCount,
                                                                 Integer batch,
                                                                 List<String> taskIds) {
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(stepInstanceId);
        String taskCreateDateStr = buildTaskCreateDateStr(stepInstance);
        InternalResponse<List<ServiceFileTaskLogDTO>> resp = logResource.listTaskFileLogsByTaskIds(
            taskCreateDateStr, stepInstanceId, executeCount, batch, taskIds);
        if (!resp.isSuccess()) {
            log.error("Get file log content by ids error, stepInstanceId={}, executeCount={}, batch={}, taskIds={}",
                stepInstanceId, executeCount, batch, taskIds);
            throw new InternalException(resp.getCode());
        }
        List<ServiceFileTaskLogDTO> fileTaskLogs = resp.getData();
        return batchConvertToAtomicFileTaskLog(stepInstance, fileTaskLogs);
    }

    private List<AtomicFileTaskLog> batchConvertToAtomicFileTaskLog(
        StepInstanceBaseDTO stepInstance,
        List<ServiceFileTaskLogDTO> fileTaskLogs
    ) {
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return Collections.emptyList();
        }

        if (stepInstance.isSupportExecuteObjectFeature()) {
            Map<String, ExecuteObject> executeObjectMap =
                stepInstanceService.computeStepExecuteObjects(stepInstance, ExecuteObject::getId);
            return batchConvertToAtomicFileTaskLog(
                executeObjectMap,
                fileTaskLogs,
                ServiceFileTaskLogDTO::getSrcExecuteObjectId,
                ServiceFileTaskLogDTO::getDestExecuteObjectId);
        } else {
            // 兼容老版本不支持执行对象的数据
            Map<Long, ExecuteObject> hostExecuteObjects = stepInstanceService.computeStepExecuteObjects(
                stepInstance, executeObject -> executeObject.getHost().getHostId());
            return batchConvertToAtomicFileTaskLog(
                hostExecuteObjects,
                fileTaskLogs,
                ServiceFileTaskLogDTO::getSrcHostId,
                ServiceFileTaskLogDTO::getDestHostId);
        }
    }

    private <K> List<AtomicFileTaskLog> batchConvertToAtomicFileTaskLog(
        Map<K, ExecuteObject> executeObjectMap,
        List<ServiceFileTaskLogDTO> fileTaskLogs,
        Function<ServiceFileTaskLogDTO, K> srcKeyExtractor,
        Function<ServiceFileTaskLogDTO, K> destKeyExtractor
    ) {
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return Collections.emptyList();
        }

        return fileTaskLogs.stream()
            .map(fileTaskLog ->
                AtomicFileTaskLog.fromServiceExecuteObjectLogDTO(
                    fileTaskLog,
                    fileTaskLogParam -> srcKeyExtractor.apply(fileTaskLogParam) == null ?
                        null : executeObjectMap.get(srcKeyExtractor.apply(fileTaskLogParam)),
                    fileTaskLogParam -> destKeyExtractor.apply(fileTaskLogParam) == null ?
                        null : executeObjectMap.get(destKeyExtractor.apply(fileTaskLogParam))))
            .collect(Collectors.toList());
    }

    @Override
    public List<FileExecuteObjectLogContent> batchGetFileSourceExecuteObjectLogContent(long stepInstanceId,
                                                                                       int executeCount,
                                                                                       Integer batch) {
        StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(stepInstanceId);
        String taskCreateDateStr = buildTaskCreateDateStr(stepInstance);
        ServiceFileLogQueryRequest request = new ServiceFileLogQueryRequest();
        request.setStepInstanceId(stepInstance.getId());
        request.setExecuteCount(executeCount);
        request.setBatch(batch);
        request.setJobCreateDate(taskCreateDateStr);
        request.setMode(FileTaskModeEnum.UPLOAD.getValue());
        InternalResponse<List<ServiceExecuteObjectLogDTO>> resp = logResource.listFileExecuteObjectLogs(
            taskCreateDateStr, stepInstanceId, executeCount, request);
        if (!resp.isSuccess()) {
            log.error("Get file source log content error, stepInstanceId={}, executeCount={}, batch={}",
                stepInstanceId, executeCount, batch);
            return Collections.emptyList();
        }

        List<ServiceExecuteObjectLogDTO> executeObjectLogs = resp.getData();

        return batchConvertToFileExecuteObjectLogContent(stepInstance, executeObjectLogs);
    }

    private List<FileExecuteObjectLogContent> batchConvertToFileExecuteObjectLogContent(
        StepInstanceDTO stepInstance,
        List<ServiceExecuteObjectLogDTO> executeObjectLogs) {

        if (CollectionUtils.isEmpty(executeObjectLogs)) {
            return Collections.emptyList();
        }
        if (stepInstance.isSupportExecuteObjectFeature()) {
            Map<String, ExecuteObject> executeObjectMap =
                stepInstanceService.computeStepExecuteObjects(stepInstance, ExecuteObject::getId);

            return executeObjectLogs.stream()
                .map(executeObjectLog -> {
                    FileExecuteObjectLogContent executeObjectLogContent = new FileExecuteObjectLogContent();
                    executeObjectLogContent.setStepInstanceId(executeObjectLog.getStepInstanceId());
                    executeObjectLogContent.setExecuteCount(executeObjectLog.getExecuteCount());
                    executeObjectLogContent.setFileTaskLogs(
                        batchConvertToAtomicFileTaskLog(
                            executeObjectMap,
                            executeObjectLog.getFileTaskLogs(),
                            ServiceFileTaskLogDTO::getSrcExecuteObjectId,
                            ServiceFileTaskLogDTO::getDestExecuteObjectId));
                    executeObjectLogContent.setExecuteObject(
                        executeObjectMap.get(executeObjectLog.getExecuteObjectId()));
                    return executeObjectLogContent;
                })
                .collect(Collectors.toList());
        } else {
            // 兼容老版本不支持执行对象的数据
            Map<Long, ExecuteObject> hostExecuteObjects = stepInstanceService.computeStepExecuteObjects(
                stepInstance, executeObject -> executeObject.getHost().getHostId());
            return executeObjectLogs.stream()
                .map(executeObjectLog -> {
                    FileExecuteObjectLogContent executeObjectLogContent = new FileExecuteObjectLogContent();
                    executeObjectLogContent.setStepInstanceId(executeObjectLog.getStepInstanceId());
                    executeObjectLogContent.setExecuteCount(executeObjectLog.getExecuteCount());
                    executeObjectLogContent.setFileTaskLogs(
                        batchConvertToAtomicFileTaskLog(
                            hostExecuteObjects,
                            executeObjectLog.getFileTaskLogs(),
                            ServiceFileTaskLogDTO::getSrcHostId,
                            ServiceFileTaskLogDTO::getDestHostId));
                    executeObjectLogContent.setExecuteObject(
                        hostExecuteObjects.get(executeObjectLog.getHostId()));
                    return executeObjectLogContent;
                })
                .collect(Collectors.toList());
        }
    }


    @Override
    public List<ServiceExecuteObjectLogDTO> batchGetFileExecuteObjectLogContent(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        List<ExecuteObjectCompositeKey> executeObjectCompositeKeys
    ) {
        if (CollectionUtils.isEmpty(executeObjectCompositeKeys)) {
            return null;
        }
        String taskCreateDateStr = buildTaskCreateDateStr(stepInstance);
        ServiceFileLogQueryRequest request = new ServiceFileLogQueryRequest();
        request.setStepInstanceId(stepInstance.getId());
        request.setExecuteCount(executeCount);
        request.setBatch(batch);
        request.setJobCreateDate(taskCreateDateStr);

        List<ExecuteObject> queryExecuteObjects =
            stepInstanceService.findExecuteObjectByCompositeKeys(stepInstance, executeObjectCompositeKeys);
        if (stepInstance.isSupportExecuteObjectFeature()) {
            List<String> executeObjectId = queryExecuteObjects.stream()
                .map(ExecuteObject::getId).collect(Collectors.toList());
            request.setExecuteObjectIds(executeObjectId);
        } else {
            // 兼容 hostId 查询
            List<Long> hostIds = queryExecuteObjects.stream()
                .map(executeObject -> executeObject.getHost().getHostId()).collect(Collectors.toList());
            request.setHostIds(hostIds);
        }

        InternalResponse<List<ServiceExecuteObjectLogDTO>> resp = logResource.listFileExecuteObjectLogs(
            taskCreateDateStr, stepInstance.getId(), executeCount, request);
        if (!resp.isSuccess()) {
            log.error("Get file log content error, request={}", request);
            return null;
        }
        return resp.getData();
    }

    @Override
    public List<ExecuteObjectCompositeKey> getExecuteObjectsCompositeKeysByContentKeyword(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        String keyword
    ) {
        String taskCreateDateStr = buildTaskCreateDateStr(stepInstance);
        long stepInstanceId = stepInstance.getId();
        if (stepInstance.isSupportExecuteObjectFeature()) {
            InternalResponse<List<String>> resp = logResource.queryExecuteObjectsByLogKeyword(taskCreateDateStr,
                stepInstanceId, executeCount, batch, keyword);
            if (!resp.isSuccess()) {
                log.error("Search execute object by keyword error, stepInstanceId={}, executeCount={}, keyword={}",
                    stepInstanceId, executeCount, keyword);
                throw new InternalException(resp.getCode());
            }
            List<String> matchExecuteObjectIds = resp.getData();
            if (CollectionUtils.isEmpty(matchExecuteObjectIds)) {
                return Collections.emptyList();
            }
            return matchExecuteObjectIds.stream()
                .map(ExecuteObjectCompositeKey::ofExecuteObjectId)
                .collect(Collectors.toList());
        } else {
            InternalResponse<List<HostDTO>> resp = logResource.queryHostsByLogKeyword(taskCreateDateStr,
                stepInstanceId, executeCount, batch, keyword);
            if (!resp.isSuccess()) {
                log.error("Search host by keyword error, stepInstanceId={}, executeCount={}, keyword={}",
                    stepInstanceId, executeCount, keyword);
                throw new InternalException(resp.getCode());
            }
            List<HostDTO> matchHosts = resp.getData();
            if (CollectionUtils.isEmpty(matchHosts)) {
                return Collections.emptyList();
            }
            return matchHosts.stream()
                .map(host -> ExecuteObjectCompositeKey.ofHostId(host.getHostId()))
                .collect(Collectors.toList());
        }
    }

    public void writeFileLogsWithTimestamp(long jobCreateTime,
                                           List<ServiceExecuteObjectLogDTO> hostFileLogs,
                                           Long logTimeInMillSeconds) {

        if (CollectionUtils.isEmpty(hostFileLogs)) {
            return;
        }

        ServiceBatchSaveLogRequest request = new ServiceBatchSaveLogRequest();
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd",
            ZoneId.of("UTC")));
        request.setLogType(LogTypeEnum.FILE.getValue());

        String logDateTime = "[";
        if (logTimeInMillSeconds != null) {
            logDateTime += DateUtils.formatUnixTimestamp(logTimeInMillSeconds, ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        } else {
            logDateTime += DateUtils.formatUnixTimestamp(System.currentTimeMillis(), ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        }
        logDateTime += "] ";
        for (ServiceExecuteObjectLogDTO hostFileLog : hostFileLogs) {
            for (ServiceFileTaskLogDTO fileTaskLog : hostFileLog.getFileTaskLogs()) {
                if (StringUtils.isBlank(fileTaskLog.getContent())) {
                    continue;
                }
                fileTaskLog.setContent(logDateTime + fileTaskLog.getContent() + "\n");
            }
            request.setLogs(hostFileLogs);
        }
        logResource.saveLogs(request);
    }

    @Override
    public void writeFileLogs(long jobCreateTime, List<ServiceExecuteObjectLogDTO> executeObjectLogs) {
        writeFileLogsWithTimestamp(jobCreateTime, executeObjectLogs, System.currentTimeMillis());
    }

    @Override
    public ServiceFileTaskLogDTO buildUploadServiceFileTaskLogDTO(StepInstanceDTO stepInstance,
                                                                  JobFile srcFile,
                                                                  FileDistStatusEnum status,
                                                                  String size,
                                                                  String speed,
                                                                  String process,
                                                                  String content) {
        if (stepInstance.isSupportExecuteObjectFeature()) {
            return new ServiceFileTaskLogDTO(
                FileDistModeEnum.UPLOAD.getValue(),
                null,
                null,
                srcFile.getExecuteObject().getId(),
                srcFile.getFileType().getType(),
                srcFile.getStandardFilePath(),
                srcFile.getDisplayFilePath(),
                size,
                status.getValue(),
                status.getName(),
                speed,
                process,
                content
            );
        } else {
            HostDTO sourceHost = srcFile.getExecuteObject().getHost();
            return new ServiceFileTaskLogDTO(
                FileDistModeEnum.UPLOAD.getValue(),
                null,
                null,
                null,
                null,
                sourceHost.getHostId(),
                sourceHost.toCloudIp(),
                sourceHost.toCloudIpv6(),
                srcFile.getFileType().getType(),
                srcFile.getStandardFilePath(),
                srcFile.getDisplayFilePath(),
                size,
                status.getValue(),
                status.getName(),
                speed,
                process,
                content
            );
        }
    }

    @Override
    public ServiceFileTaskLogDTO buildDownloadServiceFileTaskLogDTO(StepInstanceDTO stepInstance,
                                                                    JobFile srcFile,
                                                                    ExecuteObject targetExecuteObject,
                                                                    String targetPath,
                                                                    FileDistStatusEnum status,
                                                                    String size,
                                                                    String speed,
                                                                    String process,
                                                                    String content) {
        if (stepInstance.isSupportExecuteObjectFeature()) {
            return new ServiceFileTaskLogDTO(
                FileDistModeEnum.DOWNLOAD.getValue(),
                targetExecuteObject.getId(),
                targetPath,
                srcFile.getExecuteObject().getId(),
                srcFile.getFileType().getType(),
                srcFile.getStandardFilePath(),
                srcFile.getDisplayFilePath(),
                size,
                status.getValue(),
                status.getName(),
                speed,
                process,
                content
            );
        } else {
            HostDTO sourceHost = srcFile.getExecuteObject().getHost();
            HostDTO targetHost = targetExecuteObject.getHost();
            return new ServiceFileTaskLogDTO(
                FileDistModeEnum.DOWNLOAD.getValue(),
                targetHost.getHostId(),
                targetHost.toCloudIp(),
                targetHost.toCloudIpv6(),
                targetPath,
                sourceHost.getHostId(),
                sourceHost.toCloudIp(),
                sourceHost.toCloudIpv6(),
                srcFile.getFileType().getType(),
                srcFile.getStandardFilePath(),
                srcFile.getDisplayFilePath(),
                size,
                status.getValue(),
                status.getName(),
                speed,
                process,
                content
            );
        }
    }
}
