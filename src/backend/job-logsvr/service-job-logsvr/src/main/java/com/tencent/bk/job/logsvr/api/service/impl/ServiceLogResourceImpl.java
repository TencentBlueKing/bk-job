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

package com.tencent.bk.job.logsvr.api.service.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.logsvr.api.ServiceLogResource;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLogDoc;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDoc;
import com.tencent.bk.job.logsvr.model.TaskHostLog;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogQueryRequest;
import com.tencent.bk.job.logsvr.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ServiceLogResourceImpl implements ServiceLogResource {
    private final LogService logService;

    @Autowired
    public ServiceLogResourceImpl(LogService logService) {
        this.logService = logService;
    }

    @Override
    public InternalResponse<?> saveLog(ServiceSaveLogRequest request) {
        TaskHostLog taskHostLog = convertToTaskLog(request.getLogType(), request.getJobCreateDate(),
            request.getStepInstanceId(), request.getExecuteCount(), request.getBatch(), request.getHostId(),
            request.getIp(), request.getScriptLog(), request.getFileTaskLogs());
        logService.saveLog(taskHostLog);
        return InternalResponse.buildSuccessResp(null);
    }

    private TaskHostLog convertToTaskLog(Integer logType,
                                         String jobCreateDate,
                                         long stepInstanceId,
                                         int executeCount,
                                         Integer batch,
                                         Long hostId,
                                         String ip,
                                         ServiceScriptLogDTO scriptLog,
                                         List<ServiceFileTaskLogDTO> serviceFileTaskLogs) {
        TaskHostLog taskHostLog = new TaskHostLog();
        taskHostLog.setLogType(logType);
        taskHostLog.setStepInstanceId(stepInstanceId);
        taskHostLog.setExecuteCount(executeCount);
        taskHostLog.setBatch(batch);
        taskHostLog.setHostId(hostId);
        taskHostLog.setIp(ip);
        taskHostLog.setJobCreateDate(jobCreateDate);
        if (scriptLog != null) {
            taskHostLog.setScriptTaskLog(new ScriptTaskLogDoc(stepInstanceId, executeCount, batch, hostId, ip,
                scriptLog.getContent(), scriptLog.getOffset()));
        }
        if (CollectionUtils.isNotEmpty(serviceFileTaskLogs)) {
            List<FileTaskLogDoc> fileTaskLogs = serviceFileTaskLogs.stream()
                .map(FileTaskLogDoc::convert).collect(Collectors.toList());
            taskHostLog.setFileTaskLogs(fileTaskLogs);
        }
        return taskHostLog;
    }

    @Override
    public InternalResponse<?> saveLogs(ServiceBatchSaveLogRequest request) {
        List<TaskHostLog> taskHostLogs =
            request.getLogs().stream()
                .map(log -> convertToTaskLog(request.getLogType(), request.getJobCreateDate(), log.getStepInstanceId(),
                    log.getExecuteCount(), log.getBatch(), log.getHostId(), log.getIp(), log.getScriptLog(),
                    log.getFileTaskLogs()))
                .collect(Collectors.toList());
        LogTypeEnum logType = LogTypeEnum.getLogType(request.getLogType());
        logService.saveLogs(logType, taskHostLogs);
        return InternalResponse.buildSuccessResp(null);
    }

    @Override
    public InternalResponse<ServiceHostLogDTO> getScriptHostLogByIp(String jobCreateDate,
                                                                    Long stepInstanceId,
                                                                    Integer executeCount,
                                                                    String ip,
                                                                    Integer batch) {
        return InternalResponse.buildSuccessResp(
            getScriptHostLog(jobCreateDate, stepInstanceId, executeCount, null, batch, ip));
    }

    private ServiceHostLogDTO toServiceLogDTO(TaskHostLog taskHostLog) {
        if (taskHostLog == null) {
            return null;
        }
        ServiceHostLogDTO result = new ServiceHostLogDTO();
        result.setStepInstanceId(taskHostLog.getStepInstanceId());
        result.setExecuteCount(taskHostLog.getExecuteCount());
        result.setBatch(taskHostLog.getBatch());
        result.setHostId(taskHostLog.getHostId());
        result.setIp(taskHostLog.getIp());
        if (StringUtils.isNotEmpty(taskHostLog.getScriptContent())) {
            result.setScriptLog(new ServiceScriptLogDTO(taskHostLog.getHostId(), taskHostLog.getIp(),
                taskHostLog.getScriptContent()));
        }
        if (CollectionUtils.isNotEmpty(taskHostLog.getFileTaskLogs())) {
            result.setFileTaskLogs(taskHostLog.getFileTaskLogs().stream()
                .map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public InternalResponse<ServiceHostLogDTO> getScriptHostLogByHostId(String jobCreateDate,
                                                                        Long stepInstanceId,
                                                                        Integer executeCount,
                                                                        Long hostId,
                                                                        Integer batch) {

        return InternalResponse.buildSuccessResp(
            getScriptHostLog(jobCreateDate, stepInstanceId, executeCount, hostId, batch, null));
    }

    private ServiceHostLogDTO getScriptHostLog(String jobCreateDate,
                                               Long stepInstanceId,
                                               Integer executeCount,
                                               Long hostId,
                                               Integer batch,
                                               String ip) {
        ScriptLogQuery query = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount, batch,
            hostId == null ? null : Collections.singletonList(hostId),
            StringUtils.isEmpty(ip) ? null : Collections.singletonList(ip));
        List<TaskHostLog> taskHostLogs = logService.listScriptLogs(query);
        if (CollectionUtils.isEmpty(taskHostLogs)) {
            return null;
        }

        return toServiceLogDTO(taskHostLogs.get(0));
    }

    @Override
    public InternalResponse<List<ServiceHostLogDTO>> listScriptLogs(String jobCreateDate,
                                                                    Long stepInstanceId,
                                                                    Integer executeCount,
                                                                    ServiceScriptLogQueryRequest query) {
        ScriptLogQuery scriptLogQuery = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount,
            query.getBatch(), query.getHostIds(), query.getIps());
        List<TaskHostLog> taskHostLogs = logService.listScriptLogs(scriptLogQuery);
        List<ServiceHostLogDTO> scriptLogs =
            taskHostLogs.stream().map(this::toServiceLogDTO).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(scriptLogs);
    }

    @Override
    public InternalResponse<ServiceHostLogDTO> getFileHostLogByIp(String jobCreateDate,
                                                                  Long stepInstanceId,
                                                                  Integer executeCount,
                                                                  String ip,
                                                                  Integer mode,
                                                                  Integer batch) {
        FileLogQuery query = FileLogQuery.builder()
            .jobCreateDate(jobCreateDate)
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .mode(mode)
            .batch(batch)
            .ips(Collections.singletonList(ip))
            .build();
        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        ServiceHostLogDTO result = new ServiceHostLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        result.setBatch(batch);
        result.setIp(ip);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            result.setFileTaskLogs(fileTaskLogs.stream()
                .map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<ServiceHostLogDTO> getFileHostLogByHostId(String jobCreateDate, Long stepInstanceId,
                                                                      Integer executeCount, Long hostId, Integer mode,
                                                                      Integer batch) {
        return null;
    }

    public InternalResponse<List<ServiceFileTaskLogDTO>> listFileHostLogs(String jobCreateDate,
                                                                          Long stepInstanceId,
                                                                          Integer executeCount,
                                                                          Integer batch,
                                                                          Integer mode,
                                                                          String ip,
                                                                          Long hostId) {
        FileLogQuery query = FileLogQuery.builder()
            .jobCreateDate(jobCreateDate)
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .batch(batch)
            .mode(mode)
            .ips(Collections.singletonList(ip))
            .hostIds(Collections.singletonList(hostId))
            .build();

        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }
        List<ServiceFileTaskLogDTO> results = fileTaskLogs.stream().map(FileTaskLogDoc::toServiceFileTaskLogDTO)
            .collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(results);
    }

    @Override
    public InternalResponse<ServiceHostLogDTO> listFileHostLogsByTaskIds(String jobCreateDate,
                                                                         Long stepInstanceId,
                                                                         Integer executeCount,
                                                                         Integer batch,
                                                                         List<String> taskIds) {
        ServiceHostLogDTO result = new ServiceHostLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        if (CollectionUtils.isEmpty(taskIds)) {
            return InternalResponse.buildSuccessResp(result);
        }
        List<FileTaskLogDoc> fileTaskLogs = logService.getFileLogsByTaskIds(jobCreateDate, stepInstanceId, executeCount,
            batch, taskIds);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            result.setFileTaskLogs(fileTaskLogs.stream().map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<ServiceHostLogsDTO> listFileHostLogs(ServiceFileLogQueryRequest request) {
        FileLogQuery query = FileLogQuery.builder()
            .stepInstanceId(request.getStepInstanceId())
            .executeCount(request.getExecuteCount())
            .jobCreateDate(request.getJobCreateDate())
            .batch(request.getBatch())
            .mode(request.getMode())
            .ips(request.getIps())
            .build();

        ServiceHostLogsDTO ipLogsResult = new ServiceHostLogsDTO();
        ipLogsResult.setStepInstanceId(request.getStepInstanceId());
        ipLogsResult.setExecuteCount(request.getExecuteCount());

        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return InternalResponse.buildSuccessResp(ipLogsResult);
        }

        List<ServiceFileTaskLogDTO> fileLogs = fileTaskLogs.stream().map(FileTaskLogDoc::toServiceFileTaskLogDTO)
            .collect(Collectors.toList());

        Map<String, List<ServiceFileTaskLogDTO>> cloudIpAndLogs = new HashMap<>();
        fileLogs.forEach(fileLog -> {
            String cloudIp = (FileTaskModeEnum.DOWNLOAD.getValue().equals(fileLog.getMode()) ?
                fileLog.getDestIp() : fileLog.getSrcIp());
            cloudIpAndLogs.compute(cloudIp, (k, logs) -> {
                if (logs == null) {
                    logs = new ArrayList<>();
                }
                logs.add(fileLog);
                return logs;
            });
        });

        List<ServiceHostLogDTO> ipLogs = new ArrayList<>();
        ipLogsResult.setIpLogs(ipLogs);
        cloudIpAndLogs.forEach((cloudIp, logs) -> {
            ServiceHostLogDTO ipLog = new ServiceHostLogDTO();
            ipLog.setIp(cloudIp);
            ipLog.setFileTaskLogs(logs);
            ipLogs.add(ipLog);
        });

        return InternalResponse.buildSuccessResp(ipLogsResult);
    }

    @Override
    public InternalResponse<List<HostDTO>> getIpsByKeyword(String jobCreateDate,
                                                           Long stepInstanceId,
                                                           Integer executeCount,
                                                           Integer batch,
                                                           String keyword) {
        List<HostDTO> ips = logService.getIpsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
        return InternalResponse.buildSuccessResp(ips);
    }

}
