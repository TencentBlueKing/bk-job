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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.client.LogServiceResourceClient;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.service.BatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.FileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.SaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ScriptLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LogServiceImpl implements LogService {
    private final LogServiceResourceClient logServiceResourceClient;
    private final StepInstanceDAO stepInstanceDAO;
    private final GseTaskIpLogDAO gseTaskIpLogDAO;

    @Autowired
    public LogServiceImpl(LogServiceResourceClient logServiceResourceClient, StepInstanceDAO stepInstanceDAO,
                          GseTaskIpLogDAO gseTaskIpLogDAO) {
        this.logServiceResourceClient = logServiceResourceClient;
        this.stepInstanceDAO = stepInstanceDAO;
        this.gseTaskIpLogDAO = gseTaskIpLogDAO;
    }

    @Override
    public void deleteStepLog(String jobCreateDate, long stepInstanceId, int executeCount) throws ServiceException {
        logServiceResourceClient.deleteStepContent(stepInstanceId, executeCount, jobCreateDate);
    }

    @Override
    public void writeJobSystemScriptLog(long jobCreateTime, long stepInstanceId, int executeCount,
                                        String cloudAreaAndIp,
                                        String content, int offset, Long logTimeInMillSeconds) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        writeScriptLog(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd", ZoneId.of("UTC")),
            stepInstanceId, executeCount, buildSystemScriptLog(cloudAreaAndIp, content, offset, logTimeInMillSeconds));
    }

    @Override
    public void batchWriteJobSystemScriptLog(long jobCreateTime, long stepInstanceId, int executeCount,
                                             Map<String, Integer> ipsAndOffset, String content,
                                             Long logTimeInMillSeconds) throws ServiceException {
        BatchSaveLogRequest request = new BatchSaveLogRequest();
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC")));
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        List<ServiceIpLogDTO> logs = new ArrayList<>(ipsAndOffset.size());
        ipsAndOffset.forEach((cloudIp, offset) -> logs.add(buildServiceLogDTO(stepInstanceId,
            executeCount, new ServiceScriptLogDTO(cloudIp, offset, content))));
        request.setLogs(logs);

        InternalResponse resp = logServiceResourceClient.saveLogs(request);
        if (!resp.isSuccess()) {
            log.error("Batch write system script log content fail, stepInstanceId:{}, executeCount:{}", stepInstanceId
                , executeCount);
            throw new InternalException(resp.getCode());
        }
    }

    @Override
    public ServiceScriptLogDTO buildSystemScriptLog(String cloudIp, String content, int offset,
                                                    Long logTimeInMillSeconds) {
        String logDateTime;
        if (logTimeInMillSeconds != null) {
            logDateTime = DateUtils.formatUnixTimestamp(logTimeInMillSeconds, ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        } else {
            logDateTime = DateUtils.formatUnixTimestamp(System.currentTimeMillis(), ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        }
        String logContentWithDateTime = "[" +
            logDateTime +
            "] " + content + "\n";
        int length = logContentWithDateTime.getBytes(StandardCharsets.UTF_8).length;
        return new ServiceScriptLogDTO(cloudIp, offset + length, logContentWithDateTime);
    }

    @Override
    public void writeScriptLog(String jobCreateDate, long stepInstanceId, int executeCount,
                               ServiceScriptLogDTO scriptLog) throws ServiceException {
        if (scriptLog == null || StringUtils.isEmpty(scriptLog.getContent())) {
            return;
        }
        SaveLogRequest request = new SaveLogRequest();
        request.setStepInstanceId(stepInstanceId);
        request.setExecuteCount(executeCount);
        request.setIp(scriptLog.getCloudIp());
        request.setJobCreateDate(jobCreateDate);
        request.setScriptLog(scriptLog);
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        InternalResponse resp = logServiceResourceClient.saveLog(request);
        if (!resp.isSuccess()) {
            log.error("Write log content fail, stepInstanceId:{}, executeCount:{}, ip:{}", stepInstanceId, executeCount,
                scriptLog.getCloudIp());
            throw new InternalException(resp.getCode());
        }
    }

    @Override
    public void batchWriteScriptLog(String jobCreateDate, long stepInstanceId, int executeCount,
                                    List<ServiceScriptLogDTO> scriptLogs) throws ServiceException {
        if (CollectionUtils.isEmpty(scriptLogs)) {
            return;
        }
        BatchSaveLogRequest request = new BatchSaveLogRequest();
        request.setJobCreateDate(jobCreateDate);
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        List<ServiceIpLogDTO> logs = scriptLogs.stream().map(scriptLog -> buildServiceLogDTO(stepInstanceId,
            executeCount, scriptLog)).collect(Collectors.toList());
        request.setLogs(logs);
        InternalResponse resp = logServiceResourceClient.saveLogs(request);
        if (!resp.isSuccess()) {
            log.error("Batch write log content fail, stepInstanceId:{}, executeCount:{}", stepInstanceId, executeCount);
            throw new InternalException(resp.getCode());
        }
    }

    private ServiceIpLogDTO buildServiceLogDTO(long stepInstanceId, int executeCount, ServiceScriptLogDTO scriptLog) {
        ServiceIpLogDTO logDTO = new ServiceIpLogDTO();
        logDTO.setStepInstanceId(stepInstanceId);
        logDTO.setExecuteCount(executeCount);
        logDTO.setIp(scriptLog.getCloudIp());
        logDTO.setScriptLog(scriptLog);
        return logDTO;
    }

    @Override
    public ScriptIpLogContent getScriptIpLogContent(long stepInstanceId, int executeCount,
                                                    IpDTO ip) throws ServiceException {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        // 如果存在重试，那么该ip可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        GseTaskIpLogDTO gseTaskIpLog = gseTaskIpLogDAO.getIpLogByIp(stepInstanceId, executeCount, ip.convertToStrIp());
        if (gseTaskIpLog == null) {
            return null;
        }
        if (gseTaskIpLog.getStatus() == IpStatus.LAST_SUCCESS.getValue()) {
            actualExecuteCount = gseTaskIpLogDAO.getSuccessRetryCount(stepInstanceId, ip.convertToStrIp());
        }
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceIpLogDTO> resp = logServiceResourceClient.getScriptIpLogContent(stepInstanceId,
            actualExecuteCount,
            ip.convertToStrIp(), taskCreateDateStr);
        if (!resp.isSuccess()) {
            log.error("Get script log content by ip error, stepInstanceId={}, executeCount={}, ip={}", stepInstanceId,
                actualExecuteCount, ip);
            throw new InternalException(resp.getCode());
        }
        return convertToScriptIpLogContent(resp.getData(), gseTaskIpLog);
    }

    private ScriptIpLogContent convertToScriptIpLogContent(ServiceIpLogDTO logDTO, GseTaskIpLogDTO gseTaskIpLog) {
        if (logDTO == null) {
            return null;
        }
        int ipStatus = gseTaskIpLog.getStatus();
        boolean isFinished = ipStatus != IpStatus.RUNNING.getValue() && ipStatus != IpStatus.WAITING.getValue();
        String scriptContent = logDTO.getScriptLog() != null ?
            logDTO.getScriptLog().getContent() : "";
        return new ScriptIpLogContent(logDTO.getStepInstanceId(), logDTO.getExecuteCount(),
            logDTO.getIp(), scriptContent, isFinished);
    }

    @Override
    public List<ScriptIpLogContent> batchGetScriptIpLogContent(String jobCreateDateStr, long stepInstanceId,
                                                               int executeCount,
                                                               List<IpDTO> ips) throws ServiceException {

        ScriptLogQueryRequest query = new ScriptLogQueryRequest();
        query.setIps(ips.stream().map(IpDTO::convertToStrIp).collect(Collectors.toList()));
        InternalResponse<List<ServiceIpLogDTO>> resp =
            logServiceResourceClient.batchGetScriptLogContent(stepInstanceId, executeCount, jobCreateDateStr, query);
        if (!resp.isSuccess()) {
            log.error("Get script log content by ips error, stepInstanceId={}, executeCount={}, ips={}", stepInstanceId,
                executeCount, ips);
            throw new InternalException(resp.getCode());
        }
        if (CollectionUtils.isEmpty(resp.getData())) {
            return Collections.emptyList();
        }
        return resp.getData().stream().map(logDTO -> {
            String scriptContent = logDTO.getScriptLog() != null ?
                logDTO.getScriptLog().getContent() : "";
            return new ScriptIpLogContent(logDTO.getStepInstanceId(), logDTO.getExecuteCount(),
                logDTO.getIp(), scriptContent, true);
        }).collect(Collectors.toList());

    }

    @Override
    public FileIpLogContent getFileIpLogContent(long stepInstanceId, int executeCount, IpDTO ip,
                                                Integer mode) throws ServiceException {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        // 如果存在重试，那么该ip可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        GseTaskIpLogDTO gseTaskIpLog = gseTaskIpLogDAO.getIpLogByIp(stepInstanceId, executeCount, ip.convertToStrIp());
        if (gseTaskIpLog == null) {
            return null;
        }
        if (gseTaskIpLog.getStatus() == IpStatus.LAST_SUCCESS.getValue()) {
            actualExecuteCount = gseTaskIpLogDAO.getSuccessRetryCount(stepInstanceId, ip.convertToStrIp());
        }

        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceIpLogDTO> resp = logServiceResourceClient.getFileIpLogContent(stepInstanceId,
            actualExecuteCount,
            ip.convertToStrIp(), taskCreateDateStr, mode);
        if (!resp.isSuccess()) {
            log.error("Get file log content by ip error, stepInstanceId={}, executeCount={}, ip={}", stepInstanceId,
                actualExecuteCount, ip);
            throw new InternalException(resp.getCode());
        }
        List<ServiceFileTaskLogDTO> fileTaskLogs = (resp.getData() == null) ? null : resp.getData().getFileTaskLogs();
        int ipStatus = gseTaskIpLog.getStatus();
        boolean isFinished = (ipStatus != IpStatus.RUNNING.getValue() && ipStatus != IpStatus.WAITING.getValue()) ||
            isAllFileTasksFinished(fileTaskLogs);
        return new FileIpLogContent(stepInstanceId, executeCount, null, fileTaskLogs, isFinished);
    }

    private boolean isAllFileTasksFinished(List<ServiceFileTaskLogDTO> fileTaskLogs) {
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return false;
        }
        return fileTaskLogs.stream().noneMatch(this::isFileTaskNotFinished);
    }

    private boolean isFileTaskNotFinished(ServiceFileTaskLogDTO fileTaskLog) {
        FileDistStatusEnum status = FileDistStatusEnum.getFileDistStatus(fileTaskLog.getStatus());
        if (status == null) {
            return true;
        }
        return status == FileDistStatusEnum.DOWNLOADING || status == FileDistStatusEnum.UPLOADING
            || status == FileDistStatusEnum.PULLING || status == FileDistStatusEnum.WAITING;
    }

    @Override
    public List<ServiceFileTaskLogDTO> getFileLogContentByTaskIds(long stepInstanceId, int executeCount,
                                                                  List<String> taskIds) throws ServiceException {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceIpLogDTO> resp = logServiceResourceClient.getFileLogContentListByTaskIds(stepInstanceId,
            executeCount,
            taskCreateDateStr, taskIds);
        if (!resp.isSuccess()) {
            log.error("Get file log content by ids error, stepInstanceId={}, executeCount={}, taskIds={}",
                stepInstanceId, executeCount, taskIds);
            throw new InternalException(resp.getCode());
        }
        if (resp.getData() == null) {
            return Collections.emptyList();
        }
        return resp.getData().getFileTaskLogs();
    }

    @Override
    public List<ServiceFileTaskLogDTO> batchGetFileSourceIpLogContent(long stepInstanceId,
                                                                      int executeCount) throws ServiceException {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<List<ServiceFileTaskLogDTO>> resp = logServiceResourceClient.getFileLogContent(stepInstanceId,
            executeCount, taskCreateDateStr, FileDistModeEnum.UPLOAD.getValue(), null);
        if (!resp.isSuccess()) {
            log.error("Get file source log content error, stepInstanceId={}, executeCount={}", stepInstanceId,
                executeCount);
            return Collections.emptyList();
        }
        return resp.getData();
    }

    @Override
    public ServiceIpLogsDTO batchGetFileIpLogContent(long stepInstanceId, int executeCount,
                                                     List<IpDTO> ips) throws ServiceException {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        FileLogQueryRequest request = new FileLogQueryRequest();
        request.setStepInstanceId(stepInstanceId);
        request.setExecuteCount(executeCount);
        request.setJobCreateDate(taskCreateDateStr);
        request.setIps(ips.stream().map(IpDTO::convertToStrIp).collect(Collectors.toList()));

        InternalResponse<ServiceIpLogsDTO> resp = logServiceResourceClient.getFileLogContent(request);
        if (!resp.isSuccess()) {
            log.error("Get file log content error, request={}", request);
            return null;
        }
        return resp.getData();
    }

    @Override
    public List<IpDTO> getIpsByContentKeyword(long stepInstanceId, int executeCount,
                                              String keyword) throws ServiceException {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<List<IpDTO>> resp = logServiceResourceClient.getIpsByKeyword(stepInstanceId, executeCount,
            taskCreateDateStr, keyword);

        if (!resp.isSuccess()) {
            log.error("Search ips by keyword error, stepInstanceId={}, executeCount={}, keyword={}", stepInstanceId,
                executeCount, keyword);
            throw new InternalException(resp.getCode());
        }
        return resp.getData();
    }

    @Override
    public void writeFileLogWithTimestamp(long jobCreateTime, long stepInstanceId, int executeCount,
                                          String cloudAreaIdAndIp,
                                          ServiceIpLogDTO executionLog,
                                          Long logTimeInMillSeconds) throws ServiceException {

        String logDateTime = "[";
        if (logTimeInMillSeconds != null) {
            logDateTime += DateUtils.formatUnixTimestamp(logTimeInMillSeconds, ChronoUnit.MILLIS, "yyyy-MM-dd " +
                "HH:mm:ss", ZoneId.systemDefault());
        } else {
            logDateTime += DateUtils.formatUnixTimestamp(System.currentTimeMillis(), ChronoUnit.MILLIS, "yyyy-MM-dd " +
                "HH:mm:ss", ZoneId.systemDefault());
        }
        logDateTime += "] ";
        for (ServiceFileTaskLogDTO fileTaskLog : executionLog.getFileTaskLogs()) {
            fileTaskLog.setContent(logDateTime + fileTaskLog.getContent() + "\n");
        }
        SaveLogRequest request = new SaveLogRequest();
        request.setLogType(LogTypeEnum.FILE.getValue());
        request.setStepInstanceId(stepInstanceId);
        request.setExecuteCount(executeCount);
        request.setIp(cloudAreaIdAndIp);
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd",
            ZoneId.of("UTC")));
        request.setFileTaskLogs(executionLog.getFileTaskLogs());
        logServiceResourceClient.saveLog(request);
    }

    @Override
    public void writeFileLogs(long jobCreateTime, List<ServiceIpLogDTO> fileLogs) throws ServiceException {
        BatchSaveLogRequest request = new BatchSaveLogRequest();
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd",
            ZoneId.of("UTC")));
        request.setLogs(fileLogs);
        request.setLogType(LogTypeEnum.FILE.getValue());
        logServiceResourceClient.saveLogs(request);
    }
}
