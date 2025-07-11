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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbIpLogsV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbScriptHostLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbBatchGetJobInstanceIpLogV3Request;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.util.ExecuteObjectCompositeKeyUtils;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.util.LogFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbBatchGetJobInstanceIpLogV3ResourceImpl implements EsbBatchGetJobInstanceIpLogV3Resource {

    private final StepInstanceService stepInstanceService;
    private final LogService logService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;

    public EsbBatchGetJobInstanceIpLogV3ResourceImpl(LogService logService,
                                                     StepInstanceService stepInstanceService,
                                                     TaskInstanceAccessProcessor taskInstanceAccessProcessor) {
        this.logService = logService;
        this.stepInstanceService = stepInstanceService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_batch_get_job_instance_ip_log"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbIpLogsV3DTO> batchGetJobInstanceIpLogs(
        String username,
        String appCode,
        @AuditRequestBody EsbBatchGetJobInstanceIpLogV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Batch get job instance ip log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        taskInstanceAccessProcessor.processBeforeAccess(username,
            request.getAppResourceScope().getAppId(), taskInstanceId);

        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        EsbIpLogsV3DTO ipLogs = new EsbIpLogsV3DTO();
        ipLogs.setTaskInstanceId(taskInstanceId);
        ipLogs.setStepInstanceId(request.getStepInstanceId());

        List<ExecuteObjectCompositeKey> hostKeys =
            ExecuteObjectCompositeKeyUtils.fromEsbHostParams(request.getHostIdList(), request.getIpList());

        if (stepInstance.isScriptStep()) {
            buildScriptLogs(ipLogs, stepInstance, hostKeys);
        } else if (stepInstance.isFileStep()) {
            buildFileLogs(ipLogs, stepInstance, hostKeys);
        }
        return EsbResp.buildSuccessResp(ipLogs);
    }

    private ValidateResult checkRequest(EsbBatchGetJobInstanceIpLogV3Request request) {
        if (CollectionUtils.isEmpty(request.getHostIdList()) && CollectionUtils.isEmpty(request.getIpList())) {
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "host_id_list/ip_list");
        }

        int queryHostSize = CollectionUtils.isNotEmpty(request.getHostIdList()) ?
            request.getHostIdList().size() : request.getIpList().size();
        if (queryHostSize > 500) {
            log.warn("Host size is gt 500, stepInstanceId={}, size: {}", request.getStepInstanceId(), queryHostSize);
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "host_id_list/ip_list");
        }

        return ValidateResult.pass();
    }

    private void buildScriptLogs(EsbIpLogsV3DTO ipLogs,
                                 StepInstanceBaseDTO stepInstance,
                                 List<ExecuteObjectCompositeKey> hostKeys) {
        ipLogs.setLogType(LogTypeEnum.SCRIPT.getValue());

        String jobCreateDate = LogFieldUtil.buildJobCreateDate(stepInstance.getCreateTime());
        List<ScriptExecuteObjectLogContent> hostLogContentList = logService.batchGetScriptExecuteObjectLogContent(
            jobCreateDate, stepInstance, stepInstance.getExecuteCount(), null, hostKeys);

        if (CollectionUtils.isEmpty(hostLogContentList)) {
            return;
        }

        List<EsbScriptHostLogV3DTO> scriptTaskLogs = hostLogContentList.stream().map(hostLogContent -> {
            EsbScriptHostLogV3DTO scriptHostLog = new EsbScriptHostLogV3DTO();
            HostDTO host = hostLogContent.getExecuteObject().getHost();
            scriptHostLog.setHostId(host.getHostId());
            if (StringUtils.isNotEmpty(host.toCloudIp())) {
                Long bkCloudId = IpUtils.extractBkCloudId(host.toCloudIp());
                String ip = IpUtils.extractIp(host.toCloudIp());
                scriptHostLog.setCloudAreaId(bkCloudId);
                scriptHostLog.setIp(ip);
            }
            if (StringUtils.isNotEmpty(host.toCloudIpv6())) {
                Long bkCloudId = IpUtils.extractBkCloudId(host.toCloudIpv6());
                String ipv6 = IpUtils.extractIp(host.toCloudIpv6());
                scriptHostLog.setCloudAreaId(bkCloudId);
                scriptHostLog.setIpv6(ipv6);
            }
            scriptHostLog.setLogContent(hostLogContent.getContent());
            return scriptHostLog;
        }).collect(Collectors.toList());
        ipLogs.setScriptTaskLogs(scriptTaskLogs);
    }

    private void buildFileLogs(EsbIpLogsV3DTO esbIpLogs,
                               StepInstanceBaseDTO stepInstance,
                               List<ExecuteObjectCompositeKey> hostKeys) {
        esbIpLogs.setLogType(LogTypeEnum.FILE.getValue());

        List<FileExecuteObjectLogContent> ipLogs = logService.batchGetFileExecuteObjectLogContent(
            stepInstance.getId(), stepInstance.getExecuteCount(), null, null, hostKeys);

        if (CollectionUtils.isEmpty(ipLogs)) {
            return;
        }

        List<EsbFileIpLogV3DTO> fileTaskLogs = ipLogs.stream().map(ipLog -> {
            List<AtomicFileTaskLog> ipFileLogs = ipLog.getFileTaskLogs();
            EsbFileIpLogV3DTO esbFileIpLog = new EsbFileIpLogV3DTO();
            if (CollectionUtils.isNotEmpty(ipFileLogs)) {
                esbFileIpLog.setCloudAreaId(ipLog.getExecuteObject().getHost().getBkCloudId());
                esbFileIpLog.setIp(ipLog.getExecuteObject().getHost().getIp());
                List<EsbFileLogV3DTO> esbFileLogs = ipFileLogs.stream()
                    .map(this::toEsbFileLogV3DTO).collect(Collectors.toList());
                esbFileIpLog.setFileLogs(esbFileLogs);
            }
            return esbFileIpLog;
        }).collect(Collectors.toList());

        esbIpLogs.setFileTaskLogs(fileTaskLogs);
    }

    private EsbFileLogV3DTO toEsbFileLogV3DTO(AtomicFileTaskLog fileTaskLog) {
        EsbFileLogV3DTO fileLog = new EsbFileLogV3DTO();
        fileLog.setMode(fileTaskLog.getMode());
        if (fileTaskLog.getSrcExecuteObject() != null) {
            EsbIpDTO srcIp = EsbIpDTO.fromCloudIp(fileTaskLog.getSrcExecuteObject().getHost().toCloudIp());
            if (srcIp != null) {
                fileLog.setSrcIp(srcIp);
            }
        }
        fileLog.setSrcPath(fileTaskLog.getDisplaySrcFile());
        if (FileDistModeEnum.DOWNLOAD.getValue().equals(fileTaskLog.getMode())) {
            if (fileTaskLog.getDestExecuteObject() != null) {
                EsbIpDTO destIp = EsbIpDTO.fromCloudIp(fileTaskLog.getDestExecuteObject().getHost().toCloudIp());
                if (destIp != null) {
                    fileLog.setDestIp(destIp);
                }
            }
            fileLog.setDestPath(fileTaskLog.getDestFile());
        }

        fileLog.setLogContent(fileTaskLog.getContent());
        fileLog.setSize(fileTaskLog.getSize());
        fileLog.setSpeed(fileTaskLog.getSpeed());
        fileLog.setProcess(fileTaskLog.getProcess());
        fileLog.setStatus(fileTaskLog.getStatus());
        return fileLog;
    }
}
