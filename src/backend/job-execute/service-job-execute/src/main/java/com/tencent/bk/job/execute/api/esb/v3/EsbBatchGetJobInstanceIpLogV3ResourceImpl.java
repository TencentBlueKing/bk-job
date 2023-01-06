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
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.api.esb.v2.impl.JobQueryCommonProcessor;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbFileLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbIpLogsV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbScriptHostLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbBatchGetJobInstanceIpLogV3Request;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogsDTO;
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
    private final AppScopeMappingService appScopeMappingService;

    public EsbBatchGetJobInstanceIpLogV3ResourceImpl(LogService logService,
                                                     TaskInstanceService taskInstanceService,
                                                     AppScopeMappingService appScopeMappingService) {
        this.appScopeMappingService = appScopeMappingService;
        this.logService = logService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_batch_get_job_instance_ip_log"})
    public EsbResp<EsbIpLogsV3DTO> batchGetJobInstanceIpLogs(EsbBatchGetJobInstanceIpLogV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
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

        authViewTaskInstance(request.getUserName(), request.getAppResourceScope(), taskInstance);

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        EsbIpLogsV3DTO ipLogs = new EsbIpLogsV3DTO();
        ipLogs.setTaskInstanceId(taskInstanceId);
        ipLogs.setStepInstanceId(request.getStepInstanceId());

        List<HostDTO> queryHosts = buildQueryHosts(request);

        if (stepInstance.isScriptStep()) {
            buildScriptLogs(ipLogs, stepInstance, queryHosts);
        } else if (stepInstance.isFileStep()) {
            buildFileLogs(ipLogs, stepInstance, queryHosts);
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
                                 List<HostDTO> queryHosts) {
        ipLogs.setLogType(LogTypeEnum.SCRIPT.getValue());

        String jobCreateDate = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        List<ScriptHostLogContent> hostLogContentList = logService.batchGetScriptHostLogContent(jobCreateDate,
            stepInstance.getId(), stepInstance.getExecuteCount(), null, queryHosts);

        if (CollectionUtils.isEmpty(hostLogContentList)) {
            return;
        }

        List<EsbScriptHostLogV3DTO> scriptTaskLogs = hostLogContentList.stream().map(hostLogContent -> {
            EsbScriptHostLogV3DTO scriptHostLog = new EsbScriptHostLogV3DTO();
            scriptHostLog.setHostId(hostLogContent.getHostId());
            if (StringUtils.isNotEmpty(hostLogContent.getCloudIp())) {
                Long bkCloudId = IpUtils.extractBkCloudId(hostLogContent.getCloudIp());
                String ip = IpUtils.extractIp(hostLogContent.getCloudIp());
                scriptHostLog.setCloudAreaId(bkCloudId);
                scriptHostLog.setIp(ip);
            }
            if (StringUtils.isNotEmpty(hostLogContent.getCloudIpv6())) {
                Long bkCloudId = IpUtils.extractBkCloudId(hostLogContent.getCloudIpv6());
                String ipv6 = IpUtils.extractIp(hostLogContent.getCloudIpv6());
                scriptHostLog.setCloudAreaId(bkCloudId);
                scriptHostLog.setIp(ipv6);
            }
            scriptHostLog.setLogContent(hostLogContent.getContent());
            return scriptHostLog;
        }).collect(Collectors.toList());
        ipLogs.setScriptTaskLogs(scriptTaskLogs);
    }

    private List<HostDTO> buildQueryHosts(EsbBatchGetJobInstanceIpLogV3Request request) {
        if (CollectionUtils.isNotEmpty(request.getHostIdList())) {
            return request.getHostIdList().stream()
                .map(HostDTO::fromHostId)
                .distinct()
                .collect(Collectors.toList());
        } else {
            return request.getIpList().stream()
                .map(hostIp -> new HostDTO(hostIp.getBkCloudId(), hostIp.getIp()))
                .distinct()
                .collect(Collectors.toList());
        }
    }

    private void buildFileLogs(EsbIpLogsV3DTO esbIpLogs, StepInstanceBaseDTO stepInstance,
                               List<HostDTO> queryHosts) {
        esbIpLogs.setLogType(LogTypeEnum.FILE.getValue());

        ServiceHostLogsDTO ipLogs = logService.batchGetFileIpLogContent(
            stepInstance.getId(), stepInstance.getExecuteCount(), null, queryHosts);

        if (ipLogs == null || CollectionUtils.isEmpty(ipLogs.getIpLogs())) {
            return;
        }

        List<EsbFileIpLogV3DTO> fileTaskLogs = ipLogs.getIpLogs().stream().map(ipLog -> {
            List<ServiceFileTaskLogDTO> ipFileLogs = ipLog.getFileTaskLogs();
            EsbFileIpLogV3DTO esbFileIpLog = new EsbFileIpLogV3DTO();
            if (CollectionUtils.isNotEmpty(ipFileLogs)) {
                HostDTO cloudIp = HostDTO.fromCloudIp(ipLog.getCloudIp());
                esbFileIpLog.setCloudAreaId(cloudIp.getBkCloudId());
                esbFileIpLog.setIp(cloudIp.getIp());
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
        if (StringUtils.isNotBlank(fileTaskLog.getSrcIp())) {
            EsbIpDTO srcIp = EsbIpDTO.fromCloudIp(fileTaskLog.getSrcIp());
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
