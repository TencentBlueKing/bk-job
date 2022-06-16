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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.api.esb.v2.impl.JobQueryCommonProcessor;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceIpLogV3Request;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetJobInstanceIpLogV3ResourceImpl extends JobQueryCommonProcessor
    implements EsbGetJobInstanceIpLogV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final LogService logService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetJobInstanceIpLogV3ResourceImpl(LogService logService,
                                                TaskInstanceService taskInstanceService,
                                                AppScopeMappingService appScopeMappingService) {
        this.logService = logService;
        this.taskInstanceService = taskInstanceService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_instance_ip_log"})
    public EsbResp<EsbIpLogV3DTO> getJobInstanceIpLogUsingPost(EsbGetJobInstanceIpLogV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance ip log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        authViewTaskInstance(request.getUserName(), request.getAppResourceScope(), taskInstance);

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        EsbIpLogV3DTO ipLog = new EsbIpLogV3DTO();
        ipLog.setCloudAreaId(request.getCloudAreaId());
        ipLog.setIp(request.getIp());
        ipLog.setHostId(request.getHostId());
        if (stepInstance.isScriptStep()) {
            buildScriptLog(ipLog, request.getStepInstanceId(), stepInstance.getExecuteCount(),
                request.getHostId(), request.getCloudAreaId(), request.getIp());
        } else if (stepInstance.isFileStep()) {
            buildFileLog(ipLog, request.getStepInstanceId(), stepInstance.getExecuteCount(),
                request.getHostId(), request.getCloudAreaId(), request.getIp());
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

    private void buildScriptLog(EsbIpLogV3DTO ipLog, Long stepInstanceId, Integer executeCount,
                                Long hostId, Long cloudAreaId, String ip) {
        ipLog.setLogType(LogTypeEnum.SCRIPT.getValue());
        ScriptHostLogContent logContent = logService.getScriptHostLogContent(stepInstanceId,
            executeCount, null, HostDTO.fromHostIdOrCloudIp(hostId, cloudAreaId, ip));
        if (logContent != null && StringUtils.isNotBlank(logContent.getContent())) {
            ipLog.setScriptLogContent(logContent.getContent());
        }
    }

    private void buildFileLog(EsbIpLogV3DTO ipLog, Long stepInstanceId, Integer executeCount,
                              Long hostId, Long cloudAreaId, String ip) {
        ipLog.setLogType(LogTypeEnum.FILE.getValue());
        FileIpLogContent downloadIpLog = logService.getFileIpLogContent(stepInstanceId, executeCount, null,
            HostDTO.fromHostIdOrCloudIp(hostId, cloudAreaId, ip), FileDistModeEnum.DOWNLOAD.getValue());
        List<ServiceFileTaskLogDTO> uploadTaskLogs = logService.batchGetFileSourceIpLogContent(
            stepInstanceId, executeCount, null);

        List<EsbFileLogV3DTO> fileLogs = new ArrayList<>();
        fileLogs.addAll(buildDownloadFileLogs(downloadIpLog));
        fileLogs.addAll(buildUploadFileLogs(uploadTaskLogs));
        ipLog.setFileLogs(fileLogs);
    }

    private List<EsbFileLogV3DTO> buildDownloadFileLogs(FileIpLogContent downloadIpLog) {
        List<EsbFileLogV3DTO> downloadFileLogs = new ArrayList<>();
        if (downloadIpLog != null && CollectionUtils.isNotEmpty(downloadIpLog.getFileTaskLogs())) {
            downloadFileLogs = downloadIpLog.getFileTaskLogs().stream().map(this::toEsbFileLogV3DTO)
                .collect(Collectors.toList());
        }
        return downloadFileLogs;
    }

    private List<EsbFileLogV3DTO> buildUploadFileLogs(List<ServiceFileTaskLogDTO> uploadTaskLogs) {
        List<EsbFileLogV3DTO> uploadFileLogs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(uploadTaskLogs)) {
            uploadFileLogs = uploadTaskLogs.stream().map(this::toEsbFileLogV3DTO).collect(Collectors.toList());
        }
        return uploadFileLogs;
    }

    private EsbFileLogV3DTO toEsbFileLogV3DTO(ServiceFileTaskLogDTO fileTaskLog) {
        EsbFileLogV3DTO fileLog = new EsbFileLogV3DTO();
        fileLog.setMode(fileTaskLog.getMode());
        if (StringUtils.isNotBlank(fileTaskLog.getDisplaySrcIp())) {
            EsbIpDTO srcIp = EsbIpDTO.fromCloudIp(fileTaskLog.getDisplaySrcIp());
            if (srcIp != null) {
                fileLog.setSrcIp(srcIp);
            }
        }
        fileLog.setSrcPath(fileTaskLog.getDisplaySrcFile());
        if (FileDistModeEnum.DOWNLOAD.getValue().equals(fileTaskLog.getMode())) {
            if (StringUtils.isNotBlank(fileTaskLog.getDestIp())) {
                EsbIpDTO destIp = EsbIpDTO.fromCloudIp(fileTaskLog.getDestIp());
                if (destIp != null) {
                    fileLog.setDestIp(destIp);
                }
            }
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
                                                      Long cloudAreaId,
                                                      String ip) {
        EsbGetJobInstanceIpLogV3Request request = new EsbGetJobInstanceIpLogV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setTaskInstanceId(taskInstanceId);
        request.setStepInstanceId(stepInstanceId);
        request.setCloudAreaId(cloudAreaId);
        request.setIp(ip);
        return getJobInstanceIpLogUsingPost(request);
    }
}
