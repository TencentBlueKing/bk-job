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
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.api.esb.v2.impl.JobQueryCommonProcessor;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbIpLogsV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbScriptIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbBatchGetJobInstanceIpLogV3Request;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogsDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbBatchGetJobInstanceIpLogV3ResourceImpl
    extends JobQueryCommonProcessor
    implements EsbBatchGetJobInstanceIpLogV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final LogService logService;
    private final MessageI18nService i18nService;

    public EsbBatchGetJobInstanceIpLogV3ResourceImpl(MessageI18nService i18nService,
                                                     LogService logService,
                                                     TaskInstanceService taskInstanceService) {
        this.i18nService = i18nService;
        this.logService = logService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_batch_get_job_instance_ip_log"})
    public EsbResp<EsbIpLogsV3DTO> batchGetJobInstanceIpLogs(EsbBatchGetJobInstanceIpLogV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Batch get job instance ip log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        authViewTaskInstance(request.getUserName(), request.getAppId(), taskInstance);

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        EsbIpLogsV3DTO ipLogs = new EsbIpLogsV3DTO();
        ipLogs.setTaskInstanceId(taskInstanceId);
        ipLogs.setStepInstanceId(request.getStepInstanceId());

        if (stepInstance.isScriptStep()) {
            buildScriptLogs(ipLogs, stepInstance, request.getIpList());
        } else if (stepInstance.isFileStep()) {
            buildFileLogs(ipLogs, stepInstance, request.getIpList());
        }
        return EsbResp.buildSuccessResp(ipLogs);
    }

    private ValidateResult checkRequest(EsbBatchGetJobInstanceIpLogV3Request request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("App is empty or illegal, appId={}", request.getAppId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        if (request.getStepInstanceId() == null || request.getStepInstanceId() < 1) {
            log.warn("StepInstanceId is empty or illegal, stepInstanceId={}", request.getStepInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "step_instance_id");
        }

        ValidateResult ipCheckResult = checkIps(request.getIpList());
        if (!ipCheckResult.isPass()) {
            return ipCheckResult;
        }

        int ipSize = request.getIpList().size();
        if (ipSize > 500) {
            log.warn("IpList size is gt 500, stepInstanceId={}, size: {}", request.getStepInstanceId(), ipSize);
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "ip_list");
        }

        return ValidateResult.pass();
     }

    private ValidateResult checkIps(List<EsbIpDTO> cloudIpList) {
        if (CollectionUtils.isEmpty(cloudIpList)) {
            log.warn("IpList is empty ");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "ip_list");
        }
        for (EsbIpDTO cloudIp : cloudIpList) {
            if (cloudIp.getCloudAreaId() == null || cloudIp.getCloudAreaId() < 0) {
                log.warn("CloudAreaId is empty or illegal, cloudAreaId={}", cloudIp.getCloudAreaId());
                return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                    "bk_cloud_id");
            }
            if (StringUtils.isBlank(cloudIp.getIp())) {
                log.warn("Ip is empty");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "ip");
            }
            if (!IpUtils.checkIp(cloudIp.getIp())) {
                log.warn("Ip is illegal, ip={}", cloudIp.getIp());
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "ip");
            }
        }
        return ValidateResult.pass();
    }

    private void buildScriptLogs(EsbIpLogsV3DTO ipLogs, StepInstanceBaseDTO stepInstance,
                                 List<EsbIpDTO> ipList) {
        ipLogs.setLogType(LogTypeEnum.SCRIPT.getValue());

        String jobCreateDate = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        List<ScriptIpLogContent> ipLogContentList = logService.batchGetScriptIpLogContent(jobCreateDate,
            stepInstance.getId(), stepInstance.getExecuteCount(),
            ipList.stream().map(cloudIp -> new IpDTO(cloudIp.getCloudAreaId(), cloudIp.getIp()))
                .collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(ipLogContentList)) {
            return;
        }

        List<EsbScriptIpLogV3DTO> scriptTaskLogs = ipLogContentList.stream().map(ipLogContent -> {
            EsbScriptIpLogV3DTO scriptIpLog = new EsbScriptIpLogV3DTO();
            IpDTO cloudIp = IpUtils.transform(ipLogContent.getIp());
            scriptIpLog.setCloudAreaId(cloudIp.getCloudAreaId());
            scriptIpLog.setIp(cloudIp.getIp());
            scriptIpLog.setLogContent(ipLogContent.getContent());
            return scriptIpLog;
        }).collect(Collectors.toList());
        ipLogs.setScriptTaskLogs(scriptTaskLogs);
    }

    private void buildFileLogs(EsbIpLogsV3DTO esbIpLogs, StepInstanceBaseDTO stepInstance,
                               List<EsbIpDTO> ipList) {
        esbIpLogs.setLogType(LogTypeEnum.FILE.getValue());

        ServiceIpLogsDTO ipLogs = logService.batchGetFileIpLogContent(
            stepInstance.getId(), stepInstance.getExecuteCount(),
            ipList.stream().map(cloudIp -> new IpDTO(cloudIp.getCloudAreaId(), cloudIp.getIp()))
                .collect(Collectors.toList()));

        if (ipLogs == null || CollectionUtils.isEmpty(ipLogs.getIpLogs())) {
            return;
        }

        List<EsbFileIpLogV3DTO> fileTaskLogs = ipLogs.getIpLogs().stream().map(ipLog -> {
            List<ServiceFileTaskLogDTO> ipFileLogs = ipLog.getFileTaskLogs();
            EsbFileIpLogV3DTO esbFileIpLog = new EsbFileIpLogV3DTO();
            if (CollectionUtils.isNotEmpty(ipFileLogs)) {
                IpDTO cloudIp = IpDTO.fromCloudAreaIdAndIpStr(ipLog.getIp());
                if (cloudIp != null) {
                    esbFileIpLog.setCloudAreaId(cloudIp.getCloudAreaId());
                    esbFileIpLog.setIp(cloudIp.getIp());
                }
                List<EsbFileLogV3DTO> esbFileLogs = ipFileLogs.stream()
                    .map(this::toEsbFileLogV3DTO).collect(Collectors.toList());
                esbFileIpLog.setFileLogs(esbFileLogs);
            }
            return esbFileIpLog;
        }).collect(Collectors.toList());

        esbIpLogs.setFileTaskLogs(fileTaskLogs);
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
}
