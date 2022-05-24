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
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.client.LogServiceResourceClient;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
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
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final FileAgentTaskService fileAgentTaskService;

    @Autowired
    public LogServiceImpl(LogServiceResourceClient logServiceResourceClient,
                          StepInstanceDAO stepInstanceDAO,
                          ScriptAgentTaskService scriptAgentTaskService,
                          FileAgentTaskService fileAgentTaskService) {
        this.logServiceResourceClient = logServiceResourceClient;
        this.stepInstanceDAO = stepInstanceDAO;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
    }

    @Override
    public void batchWriteJobSystemScriptLog(long jobCreateTime, long stepInstanceId, int executeCount, Integer batch,
                                             Map<String, Integer> ipsAndOffset, String content,
                                             Long logTimeInMillSeconds) {
        BatchSaveLogRequest request = new BatchSaveLogRequest();
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC")));
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        List<ServiceIpLogDTO> logs = new ArrayList<>(ipsAndOffset.size());
        ipsAndOffset.forEach((cloudIp, offset) -> logs.add(buildServiceLogDTO(stepInstanceId,
            executeCount, batch, new ServiceScriptLogDTO(cloudIp, offset, content))));
        request.setLogs(logs);

        InternalResponse resp = logServiceResourceClient.saveLogs(request);
        if (!resp.isSuccess()) {
            log.error("Batch write system script log content fail, stepInstanceId:{}, executeCount:{}, batch:{}",
                stepInstanceId, executeCount, batch);
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
    public void writeScriptLog(String jobCreateDate, long stepInstanceId, int executeCount, Integer batch,
                               ServiceScriptLogDTO scriptLog) {
        if (scriptLog == null || StringUtils.isEmpty(scriptLog.getContent())) {
            return;
        }
        SaveLogRequest request = new SaveLogRequest();
        request.setStepInstanceId(stepInstanceId);
        request.setExecuteCount(executeCount);
        request.setBatch(batch);
        request.setIp(scriptLog.getCloudIp());
        request.setJobCreateDate(jobCreateDate);
        request.setScriptLog(scriptLog);
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        InternalResponse resp = logServiceResourceClient.saveLog(request);
        if (!resp.isSuccess()) {
            log.error("Write log content fail, stepInstanceId:{}, executeCount:{}, batch: {}, ip:{}",
                stepInstanceId, executeCount, batch, scriptLog.getCloudIp());
            throw new InternalException(resp.getCode());
        }
    }

    @Override
    public void batchWriteScriptLog(String jobCreateDate, long stepInstanceId, int executeCount, Integer batch,
                                    List<ServiceScriptLogDTO> scriptLogs) {
        if (CollectionUtils.isEmpty(scriptLogs)) {
            return;
        }
        BatchSaveLogRequest request = new BatchSaveLogRequest();
        request.setJobCreateDate(jobCreateDate);
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        List<ServiceIpLogDTO> logs = scriptLogs.stream().map(scriptLog -> buildServiceLogDTO(stepInstanceId,
            executeCount, batch, scriptLog)).collect(Collectors.toList());
        request.setLogs(logs);
        InternalResponse resp = logServiceResourceClient.saveLogs(request);
        if (!resp.isSuccess()) {
            log.error("Batch write log content fail, stepInstanceId:{}, executeCount:{}, batch: {}",
                stepInstanceId, executeCount, batch);
            throw new InternalException(resp.getCode());
        }
    }

    private ServiceIpLogDTO buildServiceLogDTO(long stepInstanceId, int executeCount, Integer batch,
                                               ServiceScriptLogDTO scriptLog) {
        ServiceIpLogDTO logDTO = new ServiceIpLogDTO();
        logDTO.setStepInstanceId(stepInstanceId);
        logDTO.setExecuteCount(executeCount);
        logDTO.setBatch(batch);
        logDTO.setIp(scriptLog.getCloudIp());
        logDTO.setScriptLog(scriptLog);
        return logDTO;
    }

    @Override
    public ScriptIpLogContent getScriptIpLogContent(long stepInstanceId, int executeCount, Integer batch,
                                                    IpDTO ip) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        // 如果存在重试，那么该ip可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        AgentTaskDTO agentTask = scriptAgentTaskService.getAgentTaskByIp(stepInstanceId, executeCount,
            batch, ip.convertToStrIp());
        if (agentTask == null) {
            return null;
        }
        if (agentTask.getStatus() == IpStatus.LAST_SUCCESS.getValue()) {
            actualExecuteCount = scriptAgentTaskService.getActualSuccessExecuteCount(stepInstanceId,
                agentTask.getBatch(), ip.convertToStrIp());
        }
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceIpLogDTO> resp = logServiceResourceClient.getScriptIpLogContent(taskCreateDateStr,
            stepInstanceId, actualExecuteCount, ip.convertToStrIp(), batch);
        if (!resp.isSuccess()) {
            log.error("Get script log content by ip error, stepInstanceId={}, executeCount={}, batch={}, ip={}",
                stepInstanceId, actualExecuteCount, batch, ip);
            throw new InternalException(resp.getCode());
        }
        return convertToScriptIpLogContent(resp.getData(), agentTask);
    }

    private ScriptIpLogContent convertToScriptIpLogContent(ServiceIpLogDTO logDTO, AgentTaskDTO gseTaskIpLog) {
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
    public List<ScriptIpLogContent> batchGetScriptIpLogContent(String jobCreateDateStr,
                                                               long stepInstanceId,
                                                               int executeCount,
                                                               Integer batch,
                                                               List<IpDTO> ips) {

        ScriptLogQueryRequest query = new ScriptLogQueryRequest();
        query.setIps(ips.stream().map(IpDTO::convertToStrIp).collect(Collectors.toList()));
        query.setBatch(batch);
        InternalResponse<List<ServiceIpLogDTO>> resp =
            logServiceResourceClient.batchGetScriptLogContent(jobCreateDateStr, stepInstanceId, executeCount, query);
        if (!resp.isSuccess()) {
            log.error("Get script log content by ips error, stepInstanceId={}, executeCount={}, batch={}, ips={}",
                stepInstanceId, executeCount, batch, ips);
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
    public FileIpLogContent getFileIpLogContent(long stepInstanceId, int executeCount, Integer batch, IpDTO ip,
                                                Integer mode) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        // 如果存在重试，那么该ip可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        AgentTaskDTO agentTask = fileAgentTaskService.getAgentTask(stepInstanceId, executeCount, batch,
            FileTaskModeEnum.getFileTaskMode(mode), ip.convertToStrIp());
        if (agentTask == null) {
            return null;
        }
        if (agentTask.getStatus() == IpStatus.LAST_SUCCESS.getValue()) {
            actualExecuteCount = fileAgentTaskService.getActualSuccessExecuteCount(stepInstanceId,
                agentTask.getBatch(), agentTask.getFileTaskMode(), agentTask.getCloudIp());
        }

        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceIpLogDTO> resp = logServiceResourceClient.getFileIpLogContent(taskCreateDateStr,
            stepInstanceId, actualExecuteCount, ip.convertToStrIp(), mode, batch);
        if (!resp.isSuccess()) {
            log.error("Get file log content by ip error, stepInstanceId={}, executeCount={}, batch={}, ip={}",
                stepInstanceId, actualExecuteCount, batch, ip);
            throw new InternalException(resp.getCode());
        }
        List<ServiceFileTaskLogDTO> fileTaskLogs = (resp.getData() == null) ? null : resp.getData().getFileTaskLogs();
        int ipStatus = agentTask.getStatus();
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
    public List<ServiceFileTaskLogDTO> getFileLogContentByTaskIds(long stepInstanceId, int executeCount, Integer batch,
                                                                  List<String> taskIds) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceIpLogDTO> resp = logServiceResourceClient.getFileLogContentListByTaskIds(
            taskCreateDateStr, stepInstanceId, executeCount, batch, taskIds);
        if (!resp.isSuccess()) {
            log.error("Get file log content by ids error, stepInstanceId={}, executeCount={}, batch={}, taskIds={}",
                stepInstanceId, executeCount, batch, taskIds);
            throw new InternalException(resp.getCode());
        }
        if (resp.getData() == null) {
            return Collections.emptyList();
        }
        return resp.getData().getFileTaskLogs();
    }

    @Override
    public List<ServiceFileTaskLogDTO> batchGetFileSourceIpLogContent(long stepInstanceId,
                                                                      int executeCount,
                                                                      Integer batch) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<List<ServiceFileTaskLogDTO>> resp = logServiceResourceClient.getFileLogContent(
            taskCreateDateStr, stepInstanceId, executeCount, batch, FileDistModeEnum.UPLOAD.getValue(), null);
        if (!resp.isSuccess()) {
            log.error("Get file source log content error, stepInstanceId={}, executeCount={}, batch={}",
                stepInstanceId, executeCount, batch);
            return Collections.emptyList();
        }
        return resp.getData();
    }

    @Override
    public ServiceIpLogsDTO batchGetFileIpLogContent(long stepInstanceId, int executeCount, Integer batch,
                                                     List<IpDTO> ips) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        FileLogQueryRequest request = new FileLogQueryRequest();
        request.setStepInstanceId(stepInstanceId);
        request.setExecuteCount(executeCount);
        request.setBatch(batch);
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
    public List<IpDTO> getIpsByContentKeyword(long stepInstanceId, int executeCount, Integer batch,
                                              String keyword) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<List<IpDTO>> resp = logServiceResourceClient.getIpsByKeyword(taskCreateDateStr,
            stepInstanceId, executeCount, batch, keyword);

        if (!resp.isSuccess()) {
            log.error("Search ips by keyword error, stepInstanceId={}, executeCount={}, keyword={}", stepInstanceId,
                executeCount, keyword);
            throw new InternalException(resp.getCode());
        }
        return resp.getData();
    }

    @Override
    public void writeFileLogWithTimestamp(long jobCreateTime,
                                          long stepInstanceId,
                                          int executeCount,
                                          Integer batch,
                                          String cloudIp,
                                          ServiceIpLogDTO executionLog,
                                          Long logTimeInMillSeconds) {

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
        request.setBatch(batch);
        request.setIp(cloudIp);
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd",
            ZoneId.of("UTC")));
        request.setFileTaskLogs(executionLog.getFileTaskLogs());
        logServiceResourceClient.saveLog(request);
    }

    @Override
    public void writeFileLogs(long jobCreateTime, List<ServiceIpLogDTO> fileLogs) {
        BatchSaveLogRequest request = new BatchSaveLogRequest();
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd",
            ZoneId.of("UTC")));
        request.setLogs(fileLogs);
        request.setLogType(LogTypeEnum.FILE.getValue());
        logServiceResourceClient.saveLogs(request);
    }
}
