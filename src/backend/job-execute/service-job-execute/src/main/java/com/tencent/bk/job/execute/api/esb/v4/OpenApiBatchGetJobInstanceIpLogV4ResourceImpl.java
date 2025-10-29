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
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4BatchGetJobInstanceIpLogRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4BatchIpLogResp;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4FileLogDTO;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4FileStepIpLogDTO;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4ScriptStepIpLogDTO;
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
public class OpenApiBatchGetJobInstanceIpLogV4ResourceImpl implements OpenApiBatchGetJobInstanceIpLogV4Resource {

    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final StepInstanceService stepInstanceService;
    private final LogService logService;

    @Autowired
    public OpenApiBatchGetJobInstanceIpLogV4ResourceImpl(TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                                         StepInstanceService stepInstanceService,
                                                         LogService logService) {
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.stepInstanceService = stepInstanceService;
        this.logService = logService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_batch_get_job_instance_ip_log"})
    public EsbV4Response<V4BatchIpLogResp> batchGetJobInstanceIpLog(String username,
                                                                    String appCode,
                                                                    @AuditRequestBody
                                                                        V4BatchGetJobInstanceIpLogRequest request) {
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

        V4BatchIpLogResp resp = new V4BatchIpLogResp();
        resp.setJobInstanceId(request.getJobInstanceId());
        resp.setStepInstanceId(request.getStepInstanceId());
        List<ExecuteObjectCompositeKey> hostKeys = new ArrayList<>();
        // 优先使用hostIdList
        if (CollectionUtils.isNotEmpty(request.getHostIdList())) {
            hostKeys.addAll(
                request.getHostIdList().stream()
                    .filter(Objects::nonNull)
                    .map(ExecuteObjectCompositeKey::ofHostId)
                    .collect(Collectors.toList())
            );
        } else {
            for (OpenApiV4HostDTO host : request.getIpList()) {
                hostKeys.add(
                    ExecuteObjectCompositeKeyUtils.fromHostParam(null, host.getBkCloudId(), host.getIp())
                );
            }
        }
        if (stepInstance.isScriptStep()) {
            resp.setLogType(LogTypeEnum.SCRIPT.getValue());
            List<V4ScriptStepIpLogDTO> logs = queryScriptLogs(stepInstance, hostKeys);
            resp.setScriptStepIpLogs(logs);
        } else if (stepInstance.isFileStep()) {
            resp.setLogType(LogTypeEnum.FILE.getValue());
            List<V4FileStepIpLogDTO> logs = queryFileLogs(stepInstance, hostKeys);
            resp.setFileStepIpLogs(logs);
        }

        return EsbV4Response.success(resp);
    }

    private List<V4ScriptStepIpLogDTO> queryScriptLogs(StepInstanceBaseDTO stepInstance,
                                                       List<ExecuteObjectCompositeKey> hostKeys) {
        List<ScriptExecuteObjectLogContent> scriptLogContents = logService.batchGetScriptExecuteObjectLogContent(
            LogFieldUtil.buildJobCreateDate(stepInstance.getCreateTime()),
            stepInstance,
            stepInstance.getExecuteCount(),
            null,
            hostKeys
        );
        return scriptLogContents.stream().map(this::convertToScriptIpLogDTO).collect(Collectors.toList());
    }

    private List<V4FileStepIpLogDTO> queryFileLogs(StepInstanceBaseDTO stepInstance,
                                                   List<ExecuteObjectCompositeKey> hostKeys) {
        List<FileExecuteObjectLogContent> fileLogContents = logService.batchGetFileExecuteObjectLogContent(
            stepInstance.getTaskInstanceId(),
            stepInstance.getId(),
            stepInstance.getExecuteCount(),
            null,
            null,
            hostKeys
        );
        return fileLogContents.stream().map(this::convertToFileIpLogDTO).collect(Collectors.toList());
    }

    private V4ScriptStepIpLogDTO convertToScriptIpLogDTO(ScriptExecuteObjectLogContent scriptLogContent) {
        V4ScriptStepIpLogDTO v4ScriptStepIpLogDTO = new V4ScriptStepIpLogDTO();
        v4ScriptStepIpLogDTO.setBkHostId(scriptLogContent.getExecuteObject().getHost().getHostId());
        v4ScriptStepIpLogDTO.setBkCloudId(scriptLogContent.getExecuteObject().getHost().getBkCloudId());
        v4ScriptStepIpLogDTO.setIp(scriptLogContent.getExecuteObject().getHost().getIp());
        v4ScriptStepIpLogDTO.setLogContent(scriptLogContent.getContent());
        return v4ScriptStepIpLogDTO;
    }

    private V4FileStepIpLogDTO convertToFileIpLogDTO(FileExecuteObjectLogContent fileLogContent) {
        V4FileStepIpLogDTO v4FileStepIpLogDTO = new V4FileStepIpLogDTO();
        v4FileStepIpLogDTO.setBkHostId(fileLogContent.getExecuteObject().getHost().getHostId());
        v4FileStepIpLogDTO.setBkCloudId(fileLogContent.getExecuteObject().getHost().getBkCloudId());
        v4FileStepIpLogDTO.setIp(fileLogContent.getExecuteObject().getHost().getIp());
        v4FileStepIpLogDTO.setFileLogs(fileLogContent.getFileTaskLogs().stream()
            .map(this::fromFileAtomicTaskLog)
            .collect(Collectors.toList()));
        return v4FileStepIpLogDTO;
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
