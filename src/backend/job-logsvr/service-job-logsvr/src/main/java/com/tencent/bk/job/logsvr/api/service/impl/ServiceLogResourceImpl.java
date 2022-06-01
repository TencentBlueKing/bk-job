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
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.logsvr.api.ServiceLogResource;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLog;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLog;
import com.tencent.bk.job.logsvr.model.TaskIpLog;
import com.tencent.bk.job.logsvr.model.service.BatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.FileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.SaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ScriptLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;
import com.tencent.bk.job.logsvr.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    public InternalResponse<?> saveLog(SaveLogRequest request) {
        TaskIpLog taskIpLog = convertToTaskLog(request.getLogType(), request.getJobCreateDate(),
            request.getStepInstanceId(), request.getExecuteCount(), request.getBatch(), request.getIp(),
            request.getScriptLog(), request.getFileTaskLogs());
        logService.saveLog(taskIpLog);
        return InternalResponse.buildSuccessResp(null);
    }

    private TaskIpLog convertToTaskLog(Integer logType,
                                       String jobCreateDate,
                                       long stepInstanceId,
                                       int executeCount,
                                       Integer batch,
                                       String ip,
                                       ServiceScriptLogDTO scriptLog,
                                       List<ServiceFileTaskLogDTO> serviceFileTaskLogs) {
        TaskIpLog taskIpLog = new TaskIpLog();
        taskIpLog.setLogType(logType);
        taskIpLog.setStepInstanceId(stepInstanceId);
        taskIpLog.setExecuteCount(executeCount);
        taskIpLog.setBatch(batch);
        taskIpLog.setIp(ip);
        taskIpLog.setJobCreateDate(jobCreateDate);
        if (scriptLog != null) {
            taskIpLog.setScriptTaskLog(new ScriptTaskLog(stepInstanceId, executeCount, batch, ip,
                scriptLog.getContent(), scriptLog.getOffset()));
        }
        if (CollectionUtils.isNotEmpty(serviceFileTaskLogs)) {
            List<FileTaskLog> fileTaskLogs = serviceFileTaskLogs.parallelStream()
                .map(FileTaskLog::convert).collect(Collectors.toList());
            taskIpLog.setFileTaskLogs(fileTaskLogs);
        }
        return taskIpLog;
    }

    @Override
    public InternalResponse<?> saveLogs(BatchSaveLogRequest request) {
        List<TaskIpLog> taskIpLogs =
            request.getLogs().stream()
                .map(log -> convertToTaskLog(request.getLogType(), request.getJobCreateDate(), log.getStepInstanceId(),
                    log.getExecuteCount(), log.getBatch(), log.getIp(), log.getScriptLog(), log.getFileTaskLogs()))
                .collect(Collectors.toList());
        LogTypeEnum logType = LogTypeEnum.getLogType(request.getLogType());
        logService.saveLogs(logType, taskIpLogs);
        return InternalResponse.buildSuccessResp(null);
    }

    @Override
    public InternalResponse<ServiceIpLogDTO> getIpLogContent(String jobCreateDate,
                                                             Long stepInstanceId,
                                                             Integer executeCount,
                                                             String ip,
                                                             Integer batch,
                                                             Integer logType) {
        if (LogTypeEnum.SCRIPT.getValue().equals(logType)) {
            return getScriptIpLogContent(jobCreateDate, stepInstanceId, executeCount, ip, batch);
        } else if (LogTypeEnum.FILE.getValue().equals(logType)) {
            return getFileIpLogContent(jobCreateDate, stepInstanceId, executeCount, ip, null, batch);
        } else {
            return InternalResponse.buildSuccessResp(null);
        }
    }

    @Override
    public InternalResponse<ServiceIpLogDTO> getScriptIpLogContent(String jobCreateDate,
                                                                   Long stepInstanceId,
                                                                   Integer executeCount,
                                                                   String ip,
                                                                   Integer batch) {
        ScriptLogQuery query = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount, batch, ip);
        TaskIpLog taskIpLog = logService.getScriptLogByIp(query);
        ServiceIpLogDTO result = toServiceLogDTO(taskIpLog);
        return InternalResponse.buildSuccessResp(result);
    }

    private ServiceIpLogDTO toServiceLogDTO(TaskIpLog taskIpLog) {
        ServiceIpLogDTO result = new ServiceIpLogDTO();
        if (taskIpLog != null) {
            result.setStepInstanceId(taskIpLog.getStepInstanceId());
            result.setExecuteCount(taskIpLog.getExecuteCount());
            result.setBatch(taskIpLog.getBatch());
            result.setIp(taskIpLog.getIp());
            result.setScriptLog(new ServiceScriptLogDTO(taskIpLog.getScriptContent()));
        }
        return result;
    }

    @Override
    public InternalResponse<List<ServiceIpLogDTO>> batchGetScriptLogContent(String jobCreateDate,
                                                                            Long stepInstanceId,
                                                                            Integer executeCount,
                                                                            ScriptLogQueryRequest query) {
        ScriptLogQuery scriptLogQuery = new ScriptLogQuery(jobCreateDate, stepInstanceId, executeCount,
            query.getBatch(), query.getIps());
        List<TaskIpLog> taskIpLogs = logService.batchGetScriptLogByIps(scriptLogQuery);
        List<ServiceIpLogDTO> scriptLogs = taskIpLogs.stream().map(this::toServiceLogDTO).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(scriptLogs);
    }

    @Override
    public InternalResponse<ServiceIpLogDTO> getFileIpLogContent(String jobCreateDate,
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
            .ip(ip)
            .build();
        TaskIpLog taskIpLog = logService.getFileLogByIp(query);
        ServiceIpLogDTO result = new ServiceIpLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        result.setIp(ip);
        if (taskIpLog != null) {
            if (taskIpLog.getFileTaskLogs() != null && !taskIpLog.getFileTaskLogs().isEmpty()) {
                result.setFileTaskLogs(taskIpLog.getFileTaskLogs().parallelStream()
                    .map(FileTaskLog::toServiceFileTaskLogDTO).collect(Collectors.toList()));
            }
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<List<ServiceFileTaskLogDTO>> getFileLogContent(String jobCreateDate,
                                                                           Long stepInstanceId,
                                                                           Integer executeCount,
                                                                           Integer batch,
                                                                           Integer mode,
                                                                           String ip) {
        FileLogQuery query = FileLogQuery.builder()
            .jobCreateDate(jobCreateDate)
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .batch(batch)
            .mode(mode)
            .ip(ip)
            .build();

        List<FileTaskLog> fileTaskLogs = logService.getFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }
        List<ServiceFileTaskLogDTO> results = fileTaskLogs.stream().map(FileTaskLog::toServiceFileTaskLogDTO)
            .collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(results);
    }

    @Override
    public InternalResponse<ServiceIpLogDTO> getFileLogContentListByTaskIds(String jobCreateDate,
                                                                            Long stepInstanceId,
                                                                            Integer executeCount,
                                                                            Integer batch,
                                                                            List<String> taskIds) {
        ServiceIpLogDTO result = new ServiceIpLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        if (CollectionUtils.isEmpty(taskIds)) {
            return InternalResponse.buildSuccessResp(result);
        }
        List<FileTaskLog> fileTaskLogs = logService.getFileLogsByTaskIds(jobCreateDate, stepInstanceId, executeCount,
            batch, taskIds);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            result.setFileTaskLogs(fileTaskLogs.stream().map(FileTaskLog::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<ServiceIpLogsDTO> getFileLogContent(FileLogQueryRequest request) {
        FileLogQuery query = FileLogQuery.builder()
            .stepInstanceId(request.getStepInstanceId())
            .executeCount(request.getExecuteCount())
            .jobCreateDate(request.getJobCreateDate())
            .batch(request.getBatch())
            .mode(request.getMode())
            .ip(request.getIp())
            .ips(request.getIps())
            .build();

        ServiceIpLogsDTO ipLogsResult = new ServiceIpLogsDTO();
        ipLogsResult.setStepInstanceId(request.getStepInstanceId());
        ipLogsResult.setExecuteCount(request.getExecuteCount());

        List<FileTaskLog> fileTaskLogs = logService.getFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return InternalResponse.buildSuccessResp(ipLogsResult);
        }

        List<ServiceFileTaskLogDTO> fileLogs = fileTaskLogs.stream().map(FileTaskLog::toServiceFileTaskLogDTO)
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

        List<ServiceIpLogDTO> ipLogs = new ArrayList<>();
        ipLogsResult.setIpLogs(ipLogs);
        cloudIpAndLogs.forEach((cloudIp, logs) -> {
            ServiceIpLogDTO ipLog = new ServiceIpLogDTO();
            ipLog.setIp(cloudIp);
            ipLog.setFileTaskLogs(logs);
            ipLogs.add(ipLog);
        });

        return InternalResponse.buildSuccessResp(ipLogsResult);
    }

    @Override
    public InternalResponse<List<IpDTO>> getIpsByKeyword(String jobCreateDate,
                                                         Long stepInstanceId,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         String keyword) {
        List<IpDTO> ips = logService.getIpsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
        return InternalResponse.buildSuccessResp(ips);
    }
}
