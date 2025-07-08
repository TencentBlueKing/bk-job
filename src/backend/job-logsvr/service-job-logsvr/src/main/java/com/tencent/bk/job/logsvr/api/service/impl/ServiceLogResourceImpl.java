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
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectScriptLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
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
                .map(log -> convertToTaskExecuteObjectLog(
                    request.getLogType(),
                    request.getJobCreateDate(),
                    log))
                .collect(Collectors.toList());
        LogTypeEnum logType = LogTypeEnum.getLogType(request.getLogType());
        logService.saveLogs(logType, taskExecuteObjectLogs);
        return InternalResponse.buildSuccessResp(null);
    }

    private TaskExecuteObjectLog convertToTaskExecuteObjectLog(Integer logType,
                                                               String jobCreateDate,
                                                               ServiceExecuteObjectLogDTO log) {
        TaskExecuteObjectLog taskExecuteObjectLog = new TaskExecuteObjectLog();
        taskExecuteObjectLog.setLogType(logType);
        taskExecuteObjectLog.setStepInstanceId(log.getStepInstanceId());
        taskExecuteObjectLog.setExecuteCount(log.getExecuteCount());
        taskExecuteObjectLog.setBatch(log.getBatch());
        if (log.getExecuteObjectId() != null) {
            taskExecuteObjectLog.setExecuteObjectId(log.getExecuteObjectId());
            ServiceExecuteObjectScriptLogDTO scriptLog = log.getScriptLog();
            if (scriptLog != null) {
                taskExecuteObjectLog.setScriptTaskLog(new ScriptTaskLogDoc(log.getStepInstanceId(),
                    log.getExecuteCount(), log.getBatch(), log.getExecuteObjectId(), scriptLog.getContent(),
                    scriptLog.getOffset()));
            }
            List<ServiceFileTaskLogDTO> fileTaskLogs = log.getFileTaskLogs();
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                List<FileTaskLogDoc> fileTaskLogDocs = fileTaskLogs.stream()
                    .map(FileTaskLogDoc::convert).collect(Collectors.toList());
                taskExecuteObjectLog.setFileTaskLogs(fileTaskLogDocs);
            }
        } else {
            // 兼容历史版本使用 hostId的方式
            taskExecuteObjectLog.setHostId(log.getHostId());
            taskExecuteObjectLog.setIp(log.getCloudIp());
            taskExecuteObjectLog.setIpv6(log.getCloudIpv6());
            ServiceExecuteObjectScriptLogDTO scriptLog = log.getScriptLog();
            if (scriptLog != null) {
                taskExecuteObjectLog.setScriptTaskLog(new ScriptTaskLogDoc(log.getStepInstanceId(),
                    log.getExecuteCount(), log.getBatch(), log.getHostId(), log.getCloudIp(), log.getCloudIpv6(),
                    scriptLog.getContent(), scriptLog.getOffset()));
            }
            List<ServiceFileTaskLogDTO> fileTaskLogs = log.getFileTaskLogs();
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                List<FileTaskLogDoc> fileTaskLogDocs = fileTaskLogs.stream()
                    .map(FileTaskLogDoc::convert).collect(Collectors.toList());
                taskExecuteObjectLog.setFileTaskLogs(fileTaskLogDocs);
            }
        }
        taskExecuteObjectLog.setJobCreateDate(jobCreateDate);

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
        if (taskExecuteObjectLog.getExecuteObjectId() != null) {
            result.setExecuteObjectId(taskExecuteObjectLog.getExecuteObjectId());
        } else {
            result.setHostId(taskExecuteObjectLog.getHostId());
            result.setCloudIp(taskExecuteObjectLog.getIp());
            result.setCloudIpv6(taskExecuteObjectLog.getIpv6());
        }
        if (StringUtils.isNotEmpty(taskExecuteObjectLog.getScriptContent())) {
            if (taskExecuteObjectLog.getExecuteObjectId() != null) {
                result.setScriptLog(new ServiceExecuteObjectScriptLogDTO(
                    taskExecuteObjectLog.getExecuteObjectId(),
                    taskExecuteObjectLog.getScriptContent(),
                    0,
                    0)
                );
            } else {
                result.setScriptLog(new ServiceExecuteObjectScriptLogDTO(
                    taskExecuteObjectLog.getHostId(),
                    taskExecuteObjectLog.getIp(),
                    taskExecuteObjectLog.getIpv6(),
                    taskExecuteObjectLog.getScriptContent(),
                    0,
                    0)
                );
            }
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
            getScriptExecuteObjectLog(jobCreateDate, stepInstanceId, executeCount, batch, null, hostId));
    }

    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getScriptLogByExecuteObjectId(String jobCreateDate,
                                                                                      Long stepInstanceId,
                                                                                      Integer executeCount,
                                                                                      String executeObjectId,
                                                                                      Integer batch) {
        return InternalResponse.buildSuccessResp(
            getScriptExecuteObjectLog(jobCreateDate, stepInstanceId, executeCount, batch, executeObjectId, null));
    }

    private ServiceExecuteObjectLogDTO getScriptExecuteObjectLog(String jobCreateDate,
                                                                 Long stepInstanceId,
                                                                 Integer executeCount,
                                                                 Integer batch,
                                                                 String executeObjectId,
                                                                 Long hostId) {
        ScriptLogQuery query = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount, batch,
            executeObjectId == null ? null : Collections.singletonList(executeObjectId),
            hostId == null ? null : Collections.singletonList(hostId));
        List<TaskExecuteObjectLog> taskExecuteObjectLogs = logService.listScriptLogs(query);
        if (CollectionUtils.isEmpty(taskExecuteObjectLogs)) {
            return null;
        }

        return toServiceLogDTO(taskExecuteObjectLogs.get(0));
    }

    @Override
    public InternalResponse<List<ServiceExecuteObjectLogDTO>> listScriptExecuteObjectLogs(
        String jobCreateDate,
        Long stepInstanceId,
        Integer executeCount,
        ServiceScriptLogQueryRequest query) {

        ScriptLogQuery scriptLogQuery = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount,
            query.getBatch(), query.getExecuteObjectIds(), query.getHostIds());
        List<TaskExecuteObjectLog> taskExecuteObjectLogs = logService.listScriptLogs(scriptLogQuery);
        List<ServiceExecuteObjectLogDTO> scriptLogs =
            taskExecuteObjectLogs.stream().map(this::toServiceLogDTO).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(scriptLogs);
    }


    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getFileLogByExecuteObjectId(String jobCreateDate,
                                                                                    Long stepInstanceId,
                                                                                    Integer executeCount,
                                                                                    String executeObjectId,
                                                                                    Integer mode,
                                                                                    Integer batch) {
        FileLogQuery query = FileLogQuery.builder()
            .jobCreateDate(jobCreateDate)
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .mode(mode)
            .batch(batch)
            .executeObjectIds(Collections.singletonList(executeObjectId))
            .build();
        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        ServiceExecuteObjectLogDTO result = new ServiceExecuteObjectLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        result.setBatch(batch);
        result.setExecuteObjectId(executeObjectId);
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
    public InternalResponse<List<ServiceFileTaskLogDTO>> listTaskFileLogsByTaskIds(String jobCreateDate,
                                                                                   Long stepInstanceId,
                                                                                   Integer executeCount,
                                                                                   Integer batch,
                                                                                   List<String> taskIds) {
        List<ServiceFileTaskLogDTO> fileTaskLogs = new ArrayList<>();

        if (CollectionUtils.isEmpty(taskIds)) {
            return InternalResponse.buildSuccessResp(fileTaskLogs);
        }
        List<FileTaskLogDoc> fileTaskLogDocs = logService.getFileLogsByTaskIds(jobCreateDate, stepInstanceId,
            executeCount, batch, taskIds);
        if (CollectionUtils.isEmpty(fileTaskLogDocs)) {
            return InternalResponse.buildSuccessResp(fileTaskLogs);
        }
        return InternalResponse.buildSuccessResp(
            fileTaskLogDocs.stream().map(FileTaskLogDoc::toServiceFileTaskLogDTO).collect(Collectors.toList()));
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

        ServiceExecuteObjectLogsDTO executeObjectLogs = new ServiceExecuteObjectLogsDTO();
        executeObjectLogs.setStepInstanceId(request.getStepInstanceId());
        executeObjectLogs.setExecuteCount(request.getExecuteCount());

        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return InternalResponse.buildSuccessResp(executeObjectLogs);
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
        executeObjectLogs.setIpLogs(ipLogs);
        cloudIpAndLogs.forEach((cloudIp, logs) -> {
            ServiceExecuteObjectLogDTO ipLog = new ServiceExecuteObjectLogDTO();
            ipLog.setCloudIp(cloudIp);
            ipLog.setFileTaskLogs(logs);
            ipLogs.add(ipLog);
        });

        return InternalResponse.buildSuccessResp(executeObjectLogs);
    }

    @Override
    public InternalResponse<List<ServiceExecuteObjectLogDTO>> listFileExecuteObjectLogs(
        String jobCreateDate,
        Long stepInstanceId,
        Integer executeCount,
        ServiceFileLogQueryRequest request) {

        List<ServiceExecuteObjectLogDTO> executeObjectLogs = new ArrayList<>();

        FileLogQuery query = FileLogQuery.builder()
            .jobCreateDate(jobCreateDate)
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .batch(request.getBatch())
            .mode(request.getMode())
            .hostIds(request.getHostIds())
            .executeObjectIds(request.getExecuteObjectIds())
            .build();

        List<FileTaskLogDoc> fileTaskLogs = logService.listFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return InternalResponse.buildSuccessResp(executeObjectLogs);
        }

        boolean isExistExecuteObjectId = StringUtils.isNotEmpty(fileTaskLogs.get(0).getExecuteObjectId());

        if (isExistExecuteObjectId) {
            Map<String, List<FileTaskLogDoc>> logGroups = new HashMap<>();
            fileTaskLogs.forEach(fileTaskLogDoc -> {
                List<FileTaskLogDoc> logGroup = logGroups.computeIfAbsent(fileTaskLogDoc.getExecuteObjectId(),
                    k -> new ArrayList<>());
                logGroup.add(fileTaskLogDoc);
            });
            logGroups.forEach(
                (executeObjectId, logGroup) -> {
                    ServiceExecuteObjectLogDTO executeObjectLog = new ServiceExecuteObjectLogDTO();
                    executeObjectLog.setStepInstanceId(stepInstanceId);
                    executeObjectLog.setExecuteCount(executeCount);
                    executeObjectLog.setBatch(request.getBatch());
                    executeObjectLog.setExecuteObjectId(executeObjectId);
                    executeObjectLog.setFileTaskLogs(logGroup.stream().map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                        .collect(Collectors.toList()));
                    executeObjectLogs.add(executeObjectLog);
                }
            );
        } else {
            Map<Long, List<FileTaskLogDoc>> logGroups = new HashMap<>();
            fileTaskLogs.forEach(fileTaskLogDoc -> {
                List<FileTaskLogDoc> logGroup = logGroups.computeIfAbsent(fileTaskLogDoc.getHostId(),
                    k -> new ArrayList<>());
                logGroup.add(fileTaskLogDoc);
            });
            logGroups.forEach(
                (hostId, logGroup) -> {
                    ServiceExecuteObjectLogDTO executeObjectLog = new ServiceExecuteObjectLogDTO();
                    executeObjectLog.setStepInstanceId(stepInstanceId);
                    executeObjectLog.setExecuteCount(executeCount);
                    executeObjectLog.setBatch(request.getBatch());
                    executeObjectLog.setHostId(hostId);
                    executeObjectLog.setFileTaskLogs(logGroup.stream().map(FileTaskLogDoc::toServiceFileTaskLogDTO)
                        .collect(Collectors.toList()));
                    executeObjectLogs.add(executeObjectLog);
                }
            );
        }

        return InternalResponse.buildSuccessResp(executeObjectLogs);
    }

    @Override
    public InternalResponse<List<HostDTO>> queryHostsByLogKeyword(String jobCreateDate,
                                                                  Long stepInstanceId,
                                                                  Integer executeCount,
                                                                  Integer batch,
                                                                  String keyword) {
        List<HostDTO> hosts = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
        return InternalResponse.buildSuccessResp(hosts);
    }


    @Override
    public InternalResponse<ServiceExecuteObjectLogDTO> getFileHostLogByHostId(String jobCreateDate,
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


    @Override
    public InternalResponse<List<String>> queryExecuteObjectsByLogKeyword(String jobCreateDate,
                                                                          Long stepInstanceId,
                                                                          Integer executeCount,
                                                                          Integer batch,
                                                                          String keyword) {
        List<String> executeObjectIds = logService.getExecuteObjectIdsByKeyword(jobCreateDate, stepInstanceId,
            executeCount, batch, keyword);
        return InternalResponse.buildSuccessResp(executeObjectIds);
    }
}
