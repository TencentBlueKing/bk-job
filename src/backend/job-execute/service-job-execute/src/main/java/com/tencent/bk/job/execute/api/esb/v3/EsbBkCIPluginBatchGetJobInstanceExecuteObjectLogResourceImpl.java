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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbBkCIPluginBatchGetJobInstanceExecuteObjectLogRequest;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbExecuteObjectLogsDTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbFileAtomicTaskLogDTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbFileExecuteObjectLogDTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbScriptExecuteObjectLogDTO;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.util.ExecuteObjectCompositeKeyUtils;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.util.LogFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbBkCIPluginBatchGetJobInstanceExecuteObjectLogResourceImpl
    implements EsbBkCIPluginBatchGetJobInstanceExecuteObjectLogV3Resource {

    private final StepInstanceService stepInstanceService;
    private final LogService logService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;

    public EsbBkCIPluginBatchGetJobInstanceExecuteObjectLogResourceImpl(
        LogService logService,
        StepInstanceService stepInstanceService,
        TaskInstanceAccessProcessor taskInstanceAccessProcessor) {

        this.logService = logService;
        this.stepInstanceService = stepInstanceService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API,
        extraTags = {"api_name", "v3_bkci_plugin_batch_get_job_instance_execute_object_log"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbExecuteObjectLogsDTO> batchGetJobInstanceExecuteObjectLogs(
        String username,
        String appCode,
        @AuditRequestBody
        @Validated
            EsbBkCIPluginBatchGetJobInstanceExecuteObjectLogRequest request) {

        long taskInstanceId = request.getTaskInstanceId();
        taskInstanceAccessProcessor.processBeforeAccess(username,
            request.getAppResourceScope().getAppId(), taskInstanceId);

        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(
            request.getTaskInstanceId(), request.getStepInstanceId());
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        EsbExecuteObjectLogsDTO executeObjectLogs = new EsbExecuteObjectLogsDTO();
        executeObjectLogs.setTaskInstanceId(taskInstanceId);
        executeObjectLogs.setStepInstanceId(request.getStepInstanceId());


        List<ExecuteObjectCompositeKey> executeObjectCompositeKeys =
            ExecuteObjectCompositeKeyUtils.fromOpenApiExecuteObjectDTOList(request.getExecuteObjects());

        if (stepInstance.isScriptStep()) {
            buildScriptLogs(executeObjectLogs, stepInstance, executeObjectCompositeKeys);
        } else if (stepInstance.isFileStep()) {
            buildFileLogs(executeObjectLogs, stepInstance, executeObjectCompositeKeys);
        }
        return EsbResp.buildSuccessResp(executeObjectLogs);
    }

    private void buildScriptLogs(EsbExecuteObjectLogsDTO executeObjectLogs,
                                 StepInstanceBaseDTO stepInstance,
                                 List<ExecuteObjectCompositeKey> executeObjectCompositeKeys) {
        executeObjectLogs.setLogType(LogTypeEnum.SCRIPT.getValue());

        String jobCreateDate = LogFieldUtil.buildJobCreateDate(stepInstance.getCreateTime());
        List<ScriptExecuteObjectLogContent> executeObjectLogContentList =
            logService.batchGetScriptExecuteObjectLogContent(jobCreateDate, stepInstance,
                stepInstance.getExecuteCount(), null, executeObjectCompositeKeys);

        if (CollectionUtils.isEmpty(executeObjectLogContentList)) {
            return;
        }

        List<EsbScriptExecuteObjectLogDTO> scriptTaskLogs =
            executeObjectLogContentList.stream().map(content -> {
                EsbScriptExecuteObjectLogDTO executeObjectLog = new EsbScriptExecuteObjectLogDTO();
                executeObjectLog.setExecuteObject(content.getExecuteObject().toOpenApiExecuteObjectDTO());
                executeObjectLog.setLogContent(content.getContent());
                return executeObjectLog;
            }).collect(Collectors.toList());
        executeObjectLogs.setScriptTaskLogs(scriptTaskLogs);
    }

    private void buildFileLogs(EsbExecuteObjectLogsDTO esbExecuteObjectLogs,
                               StepInstanceBaseDTO stepInstance,
                               List<ExecuteObjectCompositeKey> executeObjectCompositeKeys) {
        esbExecuteObjectLogs.setLogType(LogTypeEnum.FILE.getValue());

        List<FileExecuteObjectLogContent> executeObjectLogs = logService.batchGetFileExecuteObjectLogContent(
            stepInstance.getTaskInstanceId(), stepInstance.getId(), stepInstance.getExecuteCount(),
            null, null, executeObjectCompositeKeys);

        if (CollectionUtils.isEmpty(executeObjectLogs)) {
            return;
        }

        List<EsbFileExecuteObjectLogDTO> esbFileExecuteObjectLogs =
            executeObjectLogs.stream()
                .map(executeObjectLog -> {
                    List<AtomicFileTaskLog> atomicFileTaskLogs = executeObjectLog.getFileTaskLogs();
                    EsbFileExecuteObjectLogDTO esbFileExecuteObjectLogDTO = new EsbFileExecuteObjectLogDTO();
                    esbFileExecuteObjectLogDTO.setExecuteObject(
                        executeObjectLog.getExecuteObject().toOpenApiExecuteObjectDTO());
                    if (CollectionUtils.isNotEmpty(atomicFileTaskLogs)) {
                        List<EsbFileAtomicTaskLogDTO> esbFileAtomicTaskLogDTOS = atomicFileTaskLogs.stream()
                            .map(this::toEsbFileAtomicTaskLogDTO).collect(Collectors.toList());
                        esbFileExecuteObjectLogDTO.setFileAtomicTaskLogs(esbFileAtomicTaskLogDTOS);
                    }
                    return esbFileExecuteObjectLogDTO;
                }).collect(Collectors.toList());

        esbExecuteObjectLogs.setFileTaskLogs(esbFileExecuteObjectLogs);
    }

    private EsbFileAtomicTaskLogDTO toEsbFileAtomicTaskLogDTO(AtomicFileTaskLog fileTaskLog) {
        EsbFileAtomicTaskLogDTO esbFileAtomicTaskLogDTO = new EsbFileAtomicTaskLogDTO();
        esbFileAtomicTaskLogDTO.setMode(fileTaskLog.getMode());
        if (fileTaskLog.getSrcExecuteObject() != null) {
            esbFileAtomicTaskLogDTO.setSrcExecuteObject(fileTaskLog.getSrcExecuteObject().toOpenApiExecuteObjectDTO());
        }
        if (fileTaskLog.getDestExecuteObject() != null) {
            esbFileAtomicTaskLogDTO.setDestExecuteObject(
                fileTaskLog.getDestExecuteObject().toOpenApiExecuteObjectDTO());
        }

        esbFileAtomicTaskLogDTO.setSrcPath(fileTaskLog.getDisplaySrcFile());
        esbFileAtomicTaskLogDTO.setDestPath(fileTaskLog.getDestFile());
        esbFileAtomicTaskLogDTO.setLogContent(fileTaskLog.getContent());
        esbFileAtomicTaskLogDTO.setSize(fileTaskLog.getSize());
        esbFileAtomicTaskLogDTO.setSpeed(fileTaskLog.getSpeed());
        esbFileAtomicTaskLogDTO.setProcess(fileTaskLog.getProcess());
        esbFileAtomicTaskLogDTO.setStatus(fileTaskLog.getStatus());
        return esbFileAtomicTaskLogDTO;
    }

}
