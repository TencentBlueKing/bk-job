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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.logsvr.api.ServiceLogResource;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.*;
import com.tencent.bk.job.logsvr.model.service.*;
import com.tencent.bk.job.logsvr.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.ErrorCode.DELETE_JOB_EXECUTION_LOG_FAIL;
import static com.tencent.bk.job.common.constant.ErrorCode.GET_JOB_EXECUTION_LOG_FAIL;

@RestController
@Slf4j
public class ServiceLogResourceImpl implements ServiceLogResource {
    private final LogService logService;
    private final MessageI18nService i18nService;

    @Autowired
    public ServiceLogResourceImpl(LogService logService, MessageI18nService i18nService) {
        this.logService = logService;
        this.i18nService = i18nService;
    }

    @Override
    public ServiceResponse saveLog(SaveLogRequest request) {
        TaskIpLog taskIpLog = convertToTaskLog(request.getLogType(), request.getJobCreateDate(),
            request.getStepInstanceId(),
            request.getExecuteCount(), request.getIp(), request.getScriptLog(), request.getFileTaskLogs());
        try {
            logService.saveLog(taskIpLog);
            return ServiceResponse.buildSuccessResp(null);
        } catch (Throwable e) {
            String errorMsg = "Fail to save log, request=" + request;
            log.error(errorMsg, e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.SAVE_JOB_EXECUTION_LOG_FAIL, i18nService);
        }
    }

    private TaskIpLog convertToTaskLog(Integer logType, String jobCreateDate, long stepInstanceId, int executeCount,
                                       String ip, ServiceScriptLogDTO scriptLog,
                                       List<ServiceFileTaskLogDTO> serviceFileTaskLogs) {
        TaskIpLog taskIpLog = new TaskIpLog();
        taskIpLog.setLogType(logType);
        taskIpLog.setStepInstanceId(stepInstanceId);
        taskIpLog.setExecuteCount(executeCount);
        taskIpLog.setIp(ip);
        taskIpLog.setJobCreateDate(jobCreateDate);
        if (scriptLog != null) {
            taskIpLog.setScriptTaskLog(new ScriptTaskLog(stepInstanceId, ip, executeCount, scriptLog.getContent(),
                scriptLog.getOffset()));
        }
        if (CollectionUtils.isNotEmpty(serviceFileTaskLogs)) {
            List<FileTaskLog> fileTaskLogs = serviceFileTaskLogs.parallelStream()
                .map(FileTaskLog::convert).collect(Collectors.toList());
            taskIpLog.setFileTaskLogs(fileTaskLogs);
        }
        return taskIpLog;
    }

    @Override
    public ServiceResponse saveLogs(BatchSaveLogRequest request) {
        try {
            List<TaskIpLog> taskIpLogs =
                request.getLogs().parallelStream().map(log -> convertToTaskLog(request.getLogType(),
                    request.getJobCreateDate(), log.getStepInstanceId(),
                    log.getExecuteCount(), log.getIp(), log.getScriptLog(), log.getFileTaskLogs()))
                    .collect(Collectors.toList());
            LogTypeEnum logType = LogTypeEnum.getLogType(request.getLogType());
            logService.saveLogs(logType, taskIpLogs);
            return ServiceResponse.buildSuccessResp(null);
        } catch (Throwable e) {
            String errorMsg = "Fail to save logs, request=" + request;
            log.warn(errorMsg, e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.SAVE_JOB_EXECUTION_LOG_FAIL, i18nService);
        }
    }

    @Override
    public ServiceResponse<ServiceLogDTO> getIpLogContent(Long stepInstanceId, Integer executeCount, String ip,
                                                          String jobCreateDate, Integer logType) {
        if (LogTypeEnum.SCRIPT.getValue().equals(logType)) {
            return getScriptIpLogContent(stepInstanceId, executeCount, ip, jobCreateDate);
        } else if (LogTypeEnum.FILE.getValue().equals(logType)) {
            return getFileIpLogContent(stepInstanceId, executeCount, ip, jobCreateDate, null);
        } else {
            return ServiceResponse.buildSuccessResp(null);
        }
    }

    @Override
    public ServiceResponse<ServiceLogDTO> getScriptIpLogContent(Long stepInstanceId, Integer executeCount, String ip,
                                                                String jobCreateDate) {
        ScriptLogQuery query = new ScriptLogQuery(jobCreateDate, stepInstanceId, ip, executeCount);
        try {
            TaskIpLog taskIpLog = logService.getScriptLogByIp(query);
            ServiceLogDTO result = toServiceLogDTO(taskIpLog);
            return ServiceResponse.buildSuccessResp(result);
        } catch (Throwable e) {
            String errorMsg =
                "Get script log fail, stepInstanceId=" + stepInstanceId + ",executeCount=" + executeCount + ",ip=" + ip;
            log.error(errorMsg, e);
            return ServiceResponse.buildCommonFailResp(GET_JOB_EXECUTION_LOG_FAIL, i18nService);
        }
    }

    private ServiceLogDTO toServiceLogDTO(TaskIpLog taskIpLog) {
        ServiceLogDTO result = new ServiceLogDTO();
        if (taskIpLog != null) {
            result.setStepInstanceId(taskIpLog.getStepInstanceId());
            result.setExecuteCount(taskIpLog.getExecuteCount());
            result.setIp(taskIpLog.getIp());
            result.setScriptLog(new ServiceScriptLogDTO(taskIpLog.getScriptContent()));
        }
        return result;
    }

    @Override
    public ServiceResponse<List<ServiceLogDTO>> batchGetScriptLogContent(Long stepInstanceId, Integer executeCount,
                                                                         String jobCreateDate,
                                                                         ScriptLogQueryRequest query) {
        ScriptLogQuery scriptLogQuery = new ScriptLogQuery(jobCreateDate, stepInstanceId, query.getIps(), executeCount);
        List<TaskIpLog> taskIpLogs = logService.batchGetScriptLogByIps(scriptLogQuery);
        List<ServiceLogDTO> scriptLogs = taskIpLogs.stream().map(this::toServiceLogDTO).collect(Collectors.toList());
        return ServiceResponse.buildSuccessResp(scriptLogs);
    }

    @Override
    public ServiceResponse<ServiceLogDTO> getFileIpLogContent(Long stepInstanceId, Integer executeCount, String ip,
                                                              String jobCreateDate, Integer mode) {
        FileLogQuery query = new FileLogQuery(jobCreateDate, stepInstanceId, ip, executeCount, mode);
        try {
            TaskIpLog taskIpLog = logService.getFileLogByIp(query);
            ServiceLogDTO result = new ServiceLogDTO();
            result.setStepInstanceId(stepInstanceId);
            result.setExecuteCount(executeCount);
            result.setIp(ip);
            if (taskIpLog != null) {
                if (taskIpLog.getFileTaskLogs() != null && !taskIpLog.getFileTaskLogs().isEmpty()) {
                    result.setFileTaskLogs(taskIpLog.getFileTaskLogs().parallelStream()
                        .map(FileTaskLog::toServiceFileTaskLogDTO).collect(Collectors.toList()));
                }
            }
            return ServiceResponse.buildSuccessResp(result);
        } catch (Throwable e) {
            String errorMsg =
                "Get file log fail, stepInstanceId=" + stepInstanceId + ",executeCount=" + executeCount + ",ip=" + ip;
            log.error(errorMsg, e);
            return ServiceResponse.buildCommonFailResp(GET_JOB_EXECUTION_LOG_FAIL, i18nService);
        }
    }

    @Override
    public ServiceResponse<List<ServiceFileTaskLogDTO>> getFileLogContent(Long stepInstanceId,
                                                                          Integer executeCount,
                                                                          String jobCreateDate,
                                                                          Integer mode,
                                                                          String ip) {
        FileLogQuery query = new FileLogQuery(jobCreateDate, stepInstanceId, ip, executeCount, mode);
        List<FileTaskLog> fileTaskLogs = logService.getFileLogs(query);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return ServiceResponse.buildSuccessResp(Collections.emptyList());
        }
        List<ServiceFileTaskLogDTO> results = fileTaskLogs.stream().map(FileTaskLog::toServiceFileTaskLogDTO)
            .collect(Collectors.toList());
        return ServiceResponse.buildSuccessResp(results);
    }

    @Override
    public ServiceResponse<ServiceLogDTO> getFileLogContentListByTaskIds(Long stepInstanceId, Integer executeCount,
                                                                         String jobCreateDate, List<String> taskIds) {
        ServiceLogDTO result = new ServiceLogDTO();
        result.setStepInstanceId(stepInstanceId);
        result.setExecuteCount(executeCount);
        if (CollectionUtils.isEmpty(taskIds)) {
            return ServiceResponse.buildSuccessResp(result);
        }
        List<FileTaskLog> fileTaskLogs = logService.getFileLogsByTaskIds(jobCreateDate, stepInstanceId, executeCount,
            taskIds);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            result.setFileTaskLogs(fileTaskLogs.stream().map(FileTaskLog::toServiceFileTaskLogDTO)
                .collect(Collectors.toList()));
        }
        return ServiceResponse.buildSuccessResp(result);
    }

    @Override
    public ServiceResponse deleteStepContent(Long stepInstanceId, Integer executeCount, String jobCreateDate) {
        try {
            long deleteCount = logService.deleteStepContent(stepInstanceId, executeCount, jobCreateDate);
            return ServiceResponse.buildSuccessResp(deleteCount);
        } catch (Throwable e) {
            String errorMsg = "Delete log fail, stepInstanceId=" + stepInstanceId + ",executeCount=" + executeCount;
            log.error(errorMsg, e);
            return ServiceResponse.buildCommonFailResp(DELETE_JOB_EXECUTION_LOG_FAIL, i18nService);
        }
    }

    @Override
    public ServiceResponse<List<IpDTO>> getIpsByKeyword(Long stepInstanceId, Integer executeCount, String jobCreateDate,
                                                        String keyword) {
        try {
            List<IpDTO> ips = logService.getIpsByKeyword(stepInstanceId, executeCount, jobCreateDate, keyword);
            return ServiceResponse.buildSuccessResp(ips);
        } catch (Throwable e) {
            String errorMsg =
                "Get ips by keyword fail, stepInstanceId=" + stepInstanceId + ",executeCount=" + executeCount + "," +
                    "keyword=" + keyword;
            log.error(errorMsg, e);
            return ServiceResponse.buildCommonFailResp(GET_JOB_EXECUTION_LOG_FAIL, i18nService);
        }
    }
}
