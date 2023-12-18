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
import com.tencent.bk.job.logsvr.model.TaskExecuteObjectLog;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
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
    public InternalResponse<?> saveLogs(ServiceBatchSaveLogRequest request) {
        if (CollectionUtils.isEmpty(request.getLogs())) {
            return InternalResponse.buildSuccessResp(null);
        }

        List<TaskExecuteObjectLog> taskExecuteObjectLogs =
            request.getLogs().stream()
                .map(log -> convertToTaskHostLog(
                    request.getLogType(),
                    request.getJobCreateDate(),
                    log.getStepInstanceId(),
                    log.getExecuteCount(),
                    log.getBatch(),
                    log.getHostId(),
                    log.getCloudIp(),
                    log.getCloudIpv6(),
                    log.getScriptLog(),
                    log.getFileTaskLogs()))
                .collect(Collectors.toList());
        LogTypeEnum logType = LogTypeEnum.getLogType(request.getLogType());
        logService.saveLogs(logType, taskExecuteObjectLogs);
        return InternalResponse.buildSuccessResp(null);
    }

    private TaskExecuteObjectLog convertToTaskHostLog(Integer logType,
                                                      String jobCreateDate,
                                                      long stepInstanceId,
                                                      int executeCount,
                                                      Integer batch,
                                                      Long hostId,
                                                      String ip,
                                                      String ipv6,
                                                      ServiceScriptLogDTO scriptLog,
                                                      List<ServiceFileTaskLogDTO> fileTaskLogs) {
        TaskExecuteObjectLog taskExecuteObjectLog = new TaskExecuteObjectLog();
        taskExecuteObjectLog.setLogType(logType);
        taskExecuteObjectLog.setStepInstanceId(stepInstanceId);
        taskExecuteObjectLog.setExecuteCount(executeCount);
        taskExecuteObjectLog.setBatch(batch);
        taskExecuteObjectLog.setHostId(hostId);
        taskExecuteObjectLog.setIp(ip);
        taskExecuteObjectLog.setIpv6(ipv6);
        taskExecuteObjectLog.setJobCreateDate(jobCreateDate);
        if (scriptLog != null) {
            taskExecuteObjectLog.setScriptTaskLog(new ScriptTaskLogDoc(stepInstanceId, executeCount, batch, hostId, ip, ipv6,
                scriptLog.getContent(), scriptLog.getOffset()));
        }
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            List<FileTaskLogDoc> fileTaskLogDocs = fileTaskLogs.stream()
                .map(FileTaskLogDoc::convert).collect(Collectors.toList());
            taskExecuteObjectLog.setFileTaskLogs(fileTaskLogDocs);
        }
        return taskExecuteObjectLog;
    }

    private ServiceExecuteObjectLogDTO toServiceLogDTO(TaskExecuteObjectLog taskExecuteObjectLog) {
        if (taskExecuteObjectLog == null) {
            return null;
        }
        ServiceExecuteObjectLogDTO result = new ServiceExecuteObjectLogDTO();
        result.setStepInstanceId(taskExecuteObjectLog.getStepInstanceId());
        result.setExecuteCount(taskExecuteObjectLog.getExecuteCount());
        result.setBatch(taskExecuteObjectLog.getBatch());
        result.setHostId(taskExecuteObjectLog.getHostId());
        result.setCloudIp(taskExecuteObjectLog.getIp());
        result.setCloudIpv6(taskExecuteObjectLog.getIpv6());
        if (StringUtils.isNotEmpty(taskExecuteObjectLog.getScriptContent())) {
            result.setScriptLog(new ServiceScriptLogDTO(taskExecuteObjectLog.getHostId(), taskExecuteObjectLog.getIp(),
                taskExecuteObjectLog.getIpv6(), taskExecuteObjectLog.getScriptContent()));
        }
        if (CollectionUtils.isNotEmpty(taskExecuteObjectLog.getFileTaskLogs())) {
            result.setFileTaskLogs(taskExecuteObjectLog.getFileTaskLogs().stream()
                .map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getScriptHostLogByHostId(String jobCreateDate,
                                                                                 Long stepInstanceId,
                                                                                 Integer executeCount,
                                                                                 Long hostId,
                                                                                 Integer batch) {

        return InternalResponse.buildSuccessResp(
            getScriptHostLog(jobCreateDate, stepInstanceId, executeCount, hostId, batch, null));
    }

    private ServiceExecuteObjectLogDTO getScriptHostLog(String jobCreateDate,
                                                        Long stepInstanceId,
                                                        Integer executeCount,
                                                        Long hostId,
                                                        Integer batch,
                                                        String ip) {
        ScriptLogQuery query = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount, batch,
            hostId == null ? null : Collections.singletonList(hostId));
        List<TaskExecuteObjectLog> taskExecuteObjectLogs = logService.listScriptLogs(query);
        if (CollectionUtils.isEmpty(taskExecuteObjectLogs)) {
            return null;
        }

        return toServiceLogDTO(taskExecuteObjectLogs.get(0));
    }

    @Override
    public InternalResponse<List<ServiceExecuteObjectLogDTO>> listScriptLogs(String jobCreateDate,
                                                                             Long stepInstanceId,
                                                                             Integer executeCount,
                                                                             ServiceScriptLogQueryRequest query) {
        ScriptLogQuery scriptLogQuery = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount,
            query.getBatch(), query.getHostIds());
        List<TaskExecuteObjectLog> taskExecuteObjectLogs = logService.listScriptLogs(scriptLogQuery);
        List<ServiceExecuteObjectLogDTO> scriptLogs =
            taskExecuteObjectLogs.stream().map(this::toServiceLogDTO).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(scriptLogs);
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getFileHostLogByIp(String jobCreateDate,
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
        ServiceExecuteObjectLogDTO result = new ServiceExecuteObjectLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        result.setBatch(batch);
        result.setCloudIp(ip);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            result.setFileTaskLogs(fileTaskLogs.stream()
                .map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getFileLogByExecuteObjectId(String jobCreateDate,
                                                                                    Long stepInstanceId,
                                                                                    Integer executeCount,
                                                                                    Long hostId,
                                                                                    Integer mode,
                                                                                    Integer batch) {
        FileLogQuery query = FileLogQuery.builder()
            .jobCreateDate(jobCreateDate)
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .mode(mode)
            .batch(batch)
            .hostIds(Collections.singletonList(hostId))
            .build();
        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        ServiceExecuteObjectLogDTO result = new ServiceExecuteObjectLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        result.setBatch(batch);
        result.setHostId(hostId);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            result.setFileTaskLogs(fileTaskLogs.stream()
                .map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return InternalResponse.buildSuccessResp(result);
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
            .ips(StringUtils.isEmpty(ip) ? null : Collections.singletonList(ip))
            .hostIds(hostId == null ? null : Collections.singletonList(hostId))
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
    public InternalResponse<ServiceExecuteObjectLogDTO> listFileLogsByTaskIds(String jobCreateDate,
                                                                              Long stepInstanceId,
                                                                              Integer executeCount,
                                                                              Integer batch,
                                                                              List<String> taskIds) {
        ServiceExecuteObjectLogDTO result = new ServiceExecuteObjectLogDTO();
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
    public InternalResponse<ServiceExecuteObjectLogsDTO> listFileHostLogs(ServiceFileLogQueryRequest request) {
        FileLogQuery query = FileLogQuery.builder()
            .stepInstanceId(request.getStepInstanceId())
            .executeCount(request.getExecuteCount())
            .jobCreateDate(request.getJobCreateDate())
            .batch(request.getBatch())
            .mode(request.getMode())
            .hostIds(request.getHostIds())
            .build();

        ServiceExecuteObjectLogsDTO ipLogsResult = new ServiceExecuteObjectLogsDTO();
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

        List<ServiceExecuteObjectLogDTO> ipLogs = new ArrayList<>();
        ipLogsResult.setIpLogs(ipLogs);
        cloudIpAndLogs.forEach((cloudIp, logs) -> {
            ServiceExecuteObjectLogDTO ipLog = new ServiceExecuteObjectLogDTO();
            ipLog.setCloudIp(cloudIp);
            ipLog.setFileTaskLogs(logs);
            ipLogs.add(ipLog);
        });

        return InternalResponse.buildSuccessResp(ipLogsResult);
    }

    @Override
    public InternalResponse<List<ServiceExecuteObjectLogDTO>> listFileExecuteObjectLogs(String jobCreateDate,
                                                                                        Long stepInstanceId,
                                                                                        Integer executeCount,
                                                                                        ServiceFileLogQueryRequest query) {
        return null;
    }

    @Override
    public InternalResponse<List<HostDTO>> questHostsByLogKeyword(String jobCreateDate,
                                                                  Long stepInstanceId,
                                                                  Integer executeCount,
                                                                  Integer batch,
                                                                  String keyword) {
        List<HostDTO> ips = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
        return InternalResponse.buildSuccessResp(ips);
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getScriptLogByExecuteObjectId(String jobCreateDate,
                                                                                      Long stepInstanceId,
                                                                                      Integer executeCount,
                                                                                      String executeObjectId,
                                                                                      Integer batch) {
        return null;
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getFileHostLogByHostId(String jobCreateDate,
                                                                               Long stepInstanceId,
                                                                               Integer executeCount,
                                                                               Long hostId,
                                                                               Integer mode, Integer batch) {
        return null;
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getFileLogByExecuteObjectId(String jobCreateDate,
                                                                                    Long stepInstanceId,
                                                                                    Integer executeCount,
                                                                                    String executeObjectId,
                                                                                    Integer mode, Integer batch) {
        return null;
    }

    @Override
    public InternalResponse<List<String>> queryExecuteObjectsByLogKeyword(String jobCreateDate,
                                                                          Long stepInstanceId,
                                                                          Integer executeCount,
                                                                          Integer batch,
                                                                          String keyword) {
        return null;
    }
}
