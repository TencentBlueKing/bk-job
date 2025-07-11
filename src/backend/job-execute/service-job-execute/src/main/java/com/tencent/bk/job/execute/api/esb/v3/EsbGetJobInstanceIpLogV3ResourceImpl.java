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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceIpLogV3Request;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.util.ExecuteObjectCompositeKeyUtils;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetJobInstanceIpLogV3ResourceImpl implements EsbGetJobInstanceIpLogV3Resource {

    private final StepInstanceService stepInstanceService;
    private final LogService logService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetJobInstanceIpLogV3ResourceImpl(LogService logService,
                                                StepInstanceService stepInstanceService,
                                                TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                                AppScopeMappingService appScopeMappingService) {
        this.logService = logService;
        this.stepInstanceService = stepInstanceService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_instance_ip_log"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbIpLogV3DTO> getJobInstanceIpLogUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetJobInstanceIpLogV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance ip log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        taskInstanceAccessProcessor.processBeforeAccess(username,
            request.getAppResourceScope().getAppId(), taskInstanceId);

        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        EsbIpLogV3DTO ipLog = new EsbIpLogV3DTO();
        ipLog.setCloudAreaId(request.getCloudAreaId());
        ipLog.setIp(request.getIp());
        ipLog.setHostId(request.getHostId());
        ExecuteObjectCompositeKey hostKey =
            ExecuteObjectCompositeKeyUtils.fromHostParam(
                request.getHostId(), request.getCloudAreaId(), request.getIp());
        if (stepInstance.isScriptStep()) {
            buildScriptLog(ipLog, stepInstance, stepInstance.getExecuteCount(), hostKey);
        } else if (stepInstance.isFileStep()) {
            buildFileLog(ipLog, stepInstance, stepInstance.getExecuteCount(), hostKey);
        }
        return EsbResp.buildSuccessResp(ipLog);
    }

    private ValidateResult checkRequest(EsbGetJobInstanceIpLogV3Request request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        if (request.getStepInstanceId() == null || request.getStepInstanceId() < 1) {
            log.warn("StepInstanceId is empty or illegal, stepInstanceId={}", request.getStepInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "step_instance_id");
        }

        return ValidateResult.pass();
    }

    private void buildScriptLog(EsbIpLogV3DTO ipLog,
                                StepInstanceBaseDTO stepInstance,
                                Integer executeCount,
                                ExecuteObjectCompositeKey hostKey) {
        ipLog.setLogType(LogTypeEnum.SCRIPT.getValue());
        ScriptExecuteObjectLogContent logContent = logService.getScriptExecuteObjectLogContent(stepInstance,
            executeCount, null, hostKey);
        if (logContent != null && StringUtils.isNotBlank(logContent.getContent())) {
            ipLog.setScriptLogContent(logContent.getContent());
            if (ipLog.getHostId() == null && logContent.getExecuteObject() != null) {
                ipLog.setHostId(logContent.getExecuteObject().getHost().getHostId());
            }
        }
    }

    private void buildFileLog(EsbIpLogV3DTO ipLog,
                              StepInstanceBaseDTO stepInstance,
                              Integer executeCount,
                              ExecuteObjectCompositeKey hostKey) {
        ipLog.setLogType(LogTypeEnum.FILE.getValue());
        FileExecuteObjectLogContent downloadIpLog = logService.getFileExecuteObjectLogContent(stepInstance,
            executeCount, null, hostKey, FileDistModeEnum.DOWNLOAD.getValue());
        List<FileExecuteObjectLogContent> uploadExecuteObjectLogs =
            logService.batchGetFileSourceExecuteObjectLogContent(stepInstance.getId(), executeCount, null);

        List<EsbFileLogV3DTO> fileLogs = new ArrayList<>();
        fileLogs.addAll(buildDownloadFileLogs(downloadIpLog));
        fileLogs.addAll(buildUploadFileLogs(uploadExecuteObjectLogs));
        // 根据 bk_cloud_id + ip 条件查询，需要补全bk_host_id
        if (ipLog.getHostId() == null && StringUtils.isNotEmpty(ipLog.getIp()) && ipLog.getCloudAreaId() != null) {
            for (EsbFileLogV3DTO fileLog : fileLogs) {
                if (fileLog.getSrcIp() != null) {
                    if (ipLog.getIp().equals(fileLog.getSrcIp().getIp()) &&
                        ipLog.getCloudAreaId().equals(fileLog.getSrcIp().getBkCloudId())) {
                        ipLog.setHostId(fileLog.getSrcIp().getHostId());
                        break;
                    }
                }
                if (fileLog.getDestIp() != null) {
                    if (ipLog.getIp().equals(fileLog.getDestIp().getIp()) &&
                        ipLog.getCloudAreaId().equals(fileLog.getDestIp().getBkCloudId())) {
                        ipLog.setHostId(fileLog.getDestIp().getHostId());
                        break;
                    }
                }
            }
        }
        ipLog.setFileLogs(fileLogs);
    }

    private List<EsbFileLogV3DTO> buildDownloadFileLogs(FileExecuteObjectLogContent executeObjectLogContent) {
        List<EsbFileLogV3DTO> downloadFileLogs = new ArrayList<>();
        if (executeObjectLogContent != null && CollectionUtils.isNotEmpty(executeObjectLogContent.getFileTaskLogs())) {
            downloadFileLogs = executeObjectLogContent.getFileTaskLogs().stream().map(this::toEsbFileLogV3DTO)
                .collect(Collectors.toList());
        }
        return downloadFileLogs;
    }

    private List<EsbFileLogV3DTO> buildUploadFileLogs(List<FileExecuteObjectLogContent> uploadExecuteObjectTaskLogs) {
        List<EsbFileLogV3DTO> uploadFileLogs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(uploadExecuteObjectTaskLogs)) {
            uploadFileLogs = uploadExecuteObjectTaskLogs.stream()
                .flatMap(executeObjectLogContent -> executeObjectLogContent.getFileTaskLogs().stream())
                .map(this::toEsbFileLogV3DTO)
                .collect(Collectors.toList());
        }
        return uploadFileLogs;
    }

    private EsbFileLogV3DTO toEsbFileLogV3DTO(AtomicFileTaskLog fileTaskLog) {
        EsbFileLogV3DTO fileLog = new EsbFileLogV3DTO();
        fileLog.setMode(fileTaskLog.getMode());

        // source
        EsbIpDTO srcHost = new EsbIpDTO();
        ExecuteObject srcExecuteObject = fileTaskLog.getSrcExecuteObject();
        srcHost.setHostId(srcExecuteObject.getHost().getHostId());
        EsbIpDTO srcIp = EsbIpDTO.fromCloudIp(srcExecuteObject.getHost().getIp());
        if (srcIp != null) {
            srcHost.setBkCloudId(srcIp.getBkCloudId());
            srcHost.setIp(srcIp.getIp());
        }
        fileLog.setSrcIp(srcHost);
        fileLog.setSrcPath(fileTaskLog.getDisplaySrcFile());

        // dest
        if (FileDistModeEnum.DOWNLOAD.getValue().equals(fileTaskLog.getMode())) {
            EsbIpDTO destHost = new EsbIpDTO();
            ExecuteObject destExecuteObject = fileTaskLog.getDestExecuteObject();
            destHost.setHostId(destExecuteObject.getHost().getHostId());
            if (StringUtils.isNotBlank(destExecuteObject.getHost().getIp())) {
                EsbIpDTO destIp = EsbIpDTO.fromCloudIp(destExecuteObject.getHost().getIp());
                if (destIp != null) {
                    destHost.setBkCloudId(destIp.getBkCloudId());
                    destHost.setIp(destIp.getIp());
                }
            }
            fileLog.setDestIp(destHost);
            fileLog.setDestPath(fileTaskLog.getDestFile());
        }

        fileLog.setLogContent(fileTaskLog.getContent());
        fileLog.setStatus(fileTaskLog.getStatus());
        return fileLog;
    }

    @Override
    public EsbResp<EsbIpLogV3DTO> getJobInstanceIpLog(String username,
                                                      String appCode,
                                                      Long bizId,
                                                      String scopeType,
                                                      String scopeId,
                                                      Long taskInstanceId,
                                                      Long stepInstanceId,
                                                      Long hostId,
                                                      Long cloudAreaId,
                                                      String ip) {
        EsbGetJobInstanceIpLogV3Request request = new EsbGetJobInstanceIpLogV3Request();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setTaskInstanceId(taskInstanceId);
        request.setStepInstanceId(stepInstanceId);
        request.setHostId(hostId);
        request.setCloudAreaId(cloudAreaId);
        request.setIp(ip);
        request.fillAppResourceScope(appScopeMappingService);
        return getJobInstanceIpLogUsingPost(username, appCode, request);
    }
}
