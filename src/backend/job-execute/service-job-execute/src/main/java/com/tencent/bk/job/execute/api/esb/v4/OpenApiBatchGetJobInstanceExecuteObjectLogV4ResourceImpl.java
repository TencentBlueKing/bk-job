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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4BatchGetJobInstanceExecuteObjectLogRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4BatchExecuteObjectLogResp;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4FileLogDTO;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4FileStepExecuteObjectLogDTO;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4ScriptStepExecuteObjectLogDTO;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.util.ExecuteObjectCompositeKeyUtils;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.util.LogFieldUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class OpenApiBatchGetJobInstanceExecuteObjectLogV4ResourceImpl
    implements OpenApiBatchGetJobInstanceExecuteObjectLogV4Resource {

    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final StepInstanceService stepInstanceService;
    private final LogService logService;

    @Autowired
    public OpenApiBatchGetJobInstanceExecuteObjectLogV4ResourceImpl(
        TaskInstanceAccessProcessor taskInstanceAccessProcessor,
        StepInstanceService stepInstanceService,
        LogService logService
    ) {
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.stepInstanceService = stepInstanceService;
        this.logService = logService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    @EsbApiTimed(
        value = CommonMetricNames.ESB_API,
        extraTags = {"api_name", "v4_batch_get_job_instance_execute_object_log"}
    )
    public EsbV4Response<V4BatchExecuteObjectLogResp> batchGetJobInstanceExecuteObjectLog(
        String username,
        String appCode,
        @AuditRequestBody
        V4BatchGetJobInstanceExecuteObjectLogRequest request
    ) {
        long jobInstanceId = request.getJobInstanceId();
        // 触发【查看执行历史】审计
        taskInstanceAccessProcessor.processBeforeAccess(
            username,
            request.getAppResourceScope().getAppId(),
            jobInstanceId
        );

        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(
            request.getJobInstanceId(),
            request.getStepInstanceId()
        );
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        V4BatchExecuteObjectLogResp resp = new V4BatchExecuteObjectLogResp();
        resp.setJobInstanceId(request.getJobInstanceId());
        resp.setStepInstanceId(request.getStepInstanceId());
        List<ExecuteObjectCompositeKey> executeObjectKeys = new ArrayList<>();

        // 优先使用hostIdList
        if (CollectionUtils.isNotEmpty(request.getHostIdList())) {
            executeObjectKeys.addAll(
                request.getHostIdList().stream()
                    .filter(Objects::nonNull)
                    .map(ExecuteObjectCompositeKey::ofHostId)
                    .collect(Collectors.toList())
            );
        } else if (CollectionUtils.isNotEmpty(request.getIpList())) {
            // 使用ipList
            for (OpenApiV4HostDTO host : request.getIpList()) {
                executeObjectKeys.add(
                    ExecuteObjectCompositeKeyUtils.fromHostParam(null, host.getBkCloudId(), host.getIp())
                );
            }
        } else if (CollectionUtils.isNotEmpty(request.getContainerIdList())) {
            executeObjectKeys.addAll(
                request.getContainerIdList().stream()
                    .filter(Objects::nonNull)
                    .map(ExecuteObjectCompositeKey::ofContainerId)
                    .collect(Collectors.toList())
            );
        }

        if (stepInstance.isScriptStep()) {
            resp.setLogType(LogTypeEnum.SCRIPT.getValue());
            List<V4ScriptStepExecuteObjectLogDTO> logs = queryScriptLogs(stepInstance, executeObjectKeys);
            resp.setScriptStepExecuteObjectLogs(logs);
        } else if (stepInstance.isFileStep()) {
            resp.setLogType(LogTypeEnum.FILE.getValue());
            List<V4FileStepExecuteObjectLogDTO> logs = queryFileLogs(stepInstance, executeObjectKeys);
            resp.setFileStepExecuteObjectLogs(logs);
        }

        return EsbV4Response.success(resp);
    }

    private List<V4ScriptStepExecuteObjectLogDTO> queryScriptLogs(StepInstanceBaseDTO stepInstance,
                                                                  List<ExecuteObjectCompositeKey> hostKeys) {
        List<ScriptExecuteObjectLogContent> scriptLogContents = logService.batchGetScriptExecuteObjectLogContent(
            LogFieldUtil.buildJobCreateDate(stepInstance.getCreateTime()),
            stepInstance,
            stepInstance.getExecuteCount(),
            null,
            hostKeys
        );
        return scriptLogContents.stream().map(this::convertToScriptExecuteObjLogDTO).collect(Collectors.toList());
    }

    private List<V4FileStepExecuteObjectLogDTO> queryFileLogs(StepInstanceBaseDTO stepInstance,
                                                              List<ExecuteObjectCompositeKey> executeObjKeys) {
        List<FileExecuteObjectLogContent> fileLogContents = logService.batchGetFileExecuteObjectLogContent(
            stepInstance.getTaskInstanceId(),
            stepInstance.getId(),
            stepInstance.getExecuteCount(),
            null,
            null,
            executeObjKeys
        );
        return fileLogContents.stream().map(this::convertToFileExecuteObjLogDTO).collect(Collectors.toList());
    }

    private V4ScriptStepExecuteObjectLogDTO convertToScriptExecuteObjLogDTO(
        ScriptExecuteObjectLogContent scriptLogContent
    ) {
        V4ScriptStepExecuteObjectLogDTO v4ScriptStepExecuteObjectLogDTO = new V4ScriptStepExecuteObjectLogDTO();
        v4ScriptStepExecuteObjectLogDTO.setExecuteObject(
            scriptLogContent.getExecuteObject().toOpenApiExecuteObjectDTO());
        v4ScriptStepExecuteObjectLogDTO.setLogContent(scriptLogContent.getContent());
        return v4ScriptStepExecuteObjectLogDTO;
    }

    private V4FileStepExecuteObjectLogDTO convertToFileExecuteObjLogDTO(FileExecuteObjectLogContent fileLogContent) {
        V4FileStepExecuteObjectLogDTO v4FileStepExecuteObjectLogDTO = new V4FileStepExecuteObjectLogDTO();
        v4FileStepExecuteObjectLogDTO.setExecuteObject(fileLogContent.getExecuteObject().toOpenApiExecuteObjectDTO());
        
        ExecuteObject currentExecuteObject = fileLogContent.getExecuteObject();
        List<AtomicFileTaskLog> fileTaskLogs = fileLogContent.getFileTaskLogs();
        
        // 区分上传日志和下载日志
        List<V4FileLogDTO> uploadLogs = new ArrayList<>();
        List<V4FileLogDTO> downloadLogs = new ArrayList<>();
        
        for (AtomicFileTaskLog fileTaskLog : fileTaskLogs) {
            V4FileLogDTO v4FileLogDTO = fromFileAtomicTaskLog(fileTaskLog);
            
            // 判断当前执行对象是源还是目标
            boolean isSource = isExecuteObjectMatch(currentExecuteObject, fileTaskLog.getSrcExecuteObject());
            boolean isDestination = isExecuteObjectMatch(currentExecuteObject, fileTaskLog.getDestExecuteObject());
            
            if (isSource) {
                uploadLogs.add(v4FileLogDTO);
            }
            if (isDestination) {
                downloadLogs.add(v4FileLogDTO);
            }
        }

        if (!uploadLogs.isEmpty()) {
            v4FileStepExecuteObjectLogDTO.setUploadLogList(uploadLogs);
        }
        if (!downloadLogs.isEmpty()) {
            v4FileStepExecuteObjectLogDTO.setDownloadLogList(downloadLogs);
        }
        
        return v4FileStepExecuteObjectLogDTO;
    }
    
    /**
     * 判断两个执行对象是否是同一个
     */
    private boolean isExecuteObjectMatch(ExecuteObject obj1, ExecuteObject obj2) {
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    private V4FileLogDTO fromFileAtomicTaskLog(AtomicFileTaskLog fileTaskLog) {
        V4FileLogDTO v4FileLogDTO = new V4FileLogDTO();
        if (fileTaskLog.getSrcExecuteObject() != null) {
            v4FileLogDTO.setSrcHost(new OpenApiV4HostDTO(fileTaskLog.getSrcExecuteObject().getHost()));
        }
        if (fileTaskLog.getDestExecuteObject() != null) {
            v4FileLogDTO.setDestHost(new OpenApiV4HostDTO(fileTaskLog.getDestExecuteObject().getHost()));
        }

        v4FileLogDTO.setMode(fileTaskLog.getMode());
        v4FileLogDTO.setSrcPath(fileTaskLog.getDisplaySrcFile());
        v4FileLogDTO.setStatus(fileTaskLog.getStatus());
        v4FileLogDTO.setDestPath(fileTaskLog.getDestFile());
        v4FileLogDTO.setLogContent(fileTaskLog.getContent());
        v4FileLogDTO.setSize(fileTaskLog.getSize());
        v4FileLogDTO.setSpeed(fileTaskLog.getSpeed());
        v4FileLogDTO.setProcess(fileTaskLog.getProcess());
        return v4FileLogDTO;
    }
}
