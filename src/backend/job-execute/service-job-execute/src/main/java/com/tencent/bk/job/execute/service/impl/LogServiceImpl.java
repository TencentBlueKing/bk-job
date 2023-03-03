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
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.client.LogServiceResourceClient;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LogServiceImpl implements LogService {
    private final LogServiceResourceClient logServiceResourceClient;
    private final TaskInstanceService taskInstanceService;
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final FileAgentTaskService fileAgentTaskService;
    private final StepInstanceService stepInstanceService;

    @Autowired
    public LogServiceImpl(LogServiceResourceClient logServiceResourceClient,
                          TaskInstanceService taskInstanceService,
                          ScriptAgentTaskService scriptAgentTaskService,
                          FileAgentTaskService fileAgentTaskService,
                          StepInstanceService stepInstanceService) {
        this.logServiceResourceClient = logServiceResourceClient;
        this.taskInstanceService = taskInstanceService;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    public ServiceScriptLogDTO buildSystemScriptLog(HostDTO host, String content, int offset,
                                                    Long logTimeInMillSeconds) {
        String logDateTime;
        if (logTimeInMillSeconds != null) {
            logDateTime = DateUtils.formatUnixTimestamp(logTimeInMillSeconds, ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        } else {
            logDateTime = DateUtils.formatUnixTimestamp(System.currentTimeMillis(), ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        }
        String logContentWithDateTime = "[" + logDateTime + "] " + content + "\n";
        int length = logContentWithDateTime.getBytes(StandardCharsets.UTF_8).length;
        return new ServiceScriptLogDTO(host, offset + length, logContentWithDateTime);
    }

    @Override
    public void batchWriteScriptLog(long jobCreateTime, long stepInstanceId, int executeCount, Integer batch,
                                    List<ServiceScriptLogDTO> scriptLogs) {
        if (CollectionUtils.isEmpty(scriptLogs)) {
            return;
        }
        String jobCreateDate = DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        ServiceBatchSaveLogRequest request = new ServiceBatchSaveLogRequest();
        request.setJobCreateDate(jobCreateDate);
        request.setLogType(LogTypeEnum.SCRIPT.getValue());
        List<ServiceHostLogDTO> logs = scriptLogs.stream()
            .map(scriptLog -> buildServiceLogDTO(stepInstanceId, executeCount, batch, scriptLog))
            .collect(Collectors.toList());
        request.setLogs(logs);
        InternalResponse resp = logServiceResourceClient.saveLogs(request);
        if (!resp.isSuccess()) {
            log.error("Batch write log content fail, stepInstanceId:{}, executeCount:{}, batch: {}",
                stepInstanceId, executeCount, batch);
            throw new InternalException(resp.getCode());
        }
    }

    private ServiceHostLogDTO buildServiceLogDTO(long stepInstanceId, int executeCount, Integer batch,
                                                 ServiceScriptLogDTO scriptLog) {
        ServiceHostLogDTO logDTO = new ServiceHostLogDTO();
        logDTO.setStepInstanceId(stepInstanceId);
        logDTO.setExecuteCount(executeCount);
        logDTO.setBatch(batch);
        logDTO.setHostId(scriptLog.getHostId());
        logDTO.setCloudIp(scriptLog.getCloudIp());
        logDTO.setCloudIpv6(scriptLog.getCloudIpv6());
        logDTO.setScriptLog(scriptLog);
        return logDTO;
    }

    @Override
    public ScriptHostLogContent getScriptHostLogContent(long stepInstanceId, int executeCount, Integer batch,
                                                        HostDTO host) {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        // 如果存在重试，那么该ip可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        AgentTaskDTO agentTask = scriptAgentTaskService.getAgentTaskByHost(stepInstance, executeCount, batch, host);
        if (agentTask == null) {
            return null;
        }

        if (executeCount > 0 && agentTask.getActualExecuteCount() != null) {
            actualExecuteCount = agentTask.getActualExecuteCount();
        } else if (agentTask.getStatus() == AgentTaskStatusEnum.LAST_SUCCESS) {
            // 兼容历史数据
            actualExecuteCount = scriptAgentTaskService.getActualSuccessExecuteCount(stepInstanceId, host.toCloudIp());
        }

        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceHostLogDTO> resp;
        if (agentTask.getHostId() != null) {
            resp = logServiceResourceClient.getScriptHostLogByHostId(taskCreateDateStr,
                stepInstanceId, actualExecuteCount, agentTask.getHostId(), batch);
        } else {
            // 兼容ip查询
            resp = logServiceResourceClient.getScriptHostLogByIp(taskCreateDateStr,
                stepInstanceId, actualExecuteCount, agentTask.getCloudIp(), batch);
        }
        if (!resp.isSuccess()) {
            log.error("Get script log content by host error, stepInstanceId={}, executeCount={}, batch={}, host={}",
                stepInstanceId, actualExecuteCount, batch, host);
            throw new InternalException(resp.getCode());
        }
        return convertToScriptHostLogContent(stepInstanceId, executeCount, resp.getData(), agentTask);
    }

    private ScriptHostLogContent convertToScriptHostLogContent(long stepInstanceId,
                                                               int executeCount,
                                                               ServiceHostLogDTO logDTO,
                                                               AgentTaskDTO agentTask) {
        // 日志是否拉取完成
        boolean isFinished = agentTask.getStatus().isFinished();
        String scriptContent = logDTO != null && logDTO.getScriptLog() != null ?
            logDTO.getScriptLog().getContent() : "";
        Long hostId = logDTO != null ? logDTO.getHostId() : null;
        String ip = logDTO != null ? logDTO.getCloudIp() : null;
        String ipv6 = logDTO != null ? logDTO.getCloudIpv6() : null;
        return new ScriptHostLogContent(stepInstanceId, executeCount, hostId, ip, ipv6, scriptContent, isFinished);
    }

    @Override
    public List<ScriptHostLogContent> batchGetScriptHostLogContent(String jobCreateDateStr,
                                                                   long stepInstanceId,
                                                                   int executeCount,
                                                                   Integer batch,
                                                                   List<HostDTO> hosts) {

        ServiceScriptLogQueryRequest query = new ServiceScriptLogQueryRequest();
        query.setBatch(batch);

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (isQueryByHostIdCondition(stepInstance)) {
            query.setHostIds(buildHostIdQueryCondition(stepInstance, hosts));
        } else {
            // 兼容历史数据的查询（使用ip，没有hostId)
            query.setIps(hosts.stream().map(HostDTO::toCloudIp).distinct().collect(Collectors.toList()));
        }

        InternalResponse<List<ServiceHostLogDTO>> resp =
            logServiceResourceClient.listScriptLogs(jobCreateDateStr, stepInstanceId, executeCount, query);
        if (!resp.isSuccess()) {
            log.error("Get script log content by ips error, stepInstanceId={}, executeCount={}, batch={}, ips={}",
                stepInstanceId, executeCount, batch, hosts);
            throw new InternalException(resp.getCode());
        }
        if (CollectionUtils.isEmpty(resp.getData())) {
            return Collections.emptyList();
        }
        return resp.getData().stream().map(logDTO -> {
            String scriptContent = logDTO.getScriptLog() != null ?
                logDTO.getScriptLog().getContent() : "";
            return new ScriptHostLogContent(logDTO.getStepInstanceId(), logDTO.getExecuteCount(), logDTO.getHostId(),
                logDTO.getCloudIp(), logDTO.getCloudIpv6(), scriptContent, true);
        }).collect(Collectors.toList());

    }

    private boolean isQueryByHostIdCondition(StepInstanceBaseDTO stepInstance) {
        return stepInstance.getTargetServers().getIpList().stream()
            .anyMatch(host -> host.getHostId() != null);
    }

    private List<Long> buildHostIdQueryCondition(StepInstanceBaseDTO stepInstance, List<HostDTO> hosts) {
        boolean hostIdParamNull = hosts.get(0).getHostId() == null;
        List<Long> hostIds;
        if (hostIdParamNull) {
            // 需要把ip查询参数转换为基于hostId的查询参数
            Map<String, HostDTO> ip2Hosts = stepInstanceService.computeStepHosts(stepInstance, HostDTO::toCloudIp);
            hostIds = hosts.stream()
                .map(host -> ip2Hosts.get(host.toCloudIp()).getHostId())
                .distinct()
                .collect(Collectors.toList());
        } else {
            hostIds = hosts.stream()
                .map(HostDTO::getHostId)
                .distinct()
                .collect(Collectors.toList());
        }

        return hostIds;
    }

    @Override
    public FileIpLogContent getFileIpLogContent(long stepInstanceId, int executeCount, Integer batch, HostDTO host,
                                                Integer mode) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        // 如果存在重试，那么该ip可能是之前已经执行过的，查询日志的时候需要获取到对应的executeCount
        int actualExecuteCount = executeCount;
        AgentTaskDTO agentTask = fileAgentTaskService.getAgentTaskByHost(stepInstance, executeCount, batch,
            FileTaskModeEnum.getFileTaskMode(mode), host);
        if (agentTask == null) {
            return null;
        }
        if (executeCount > 0 && agentTask.getActualExecuteCount() != null) {
            actualExecuteCount = agentTask.getActualExecuteCount();
        } else if (agentTask.getStatus() == AgentTaskStatusEnum.LAST_SUCCESS) {
            // 兼容历史数据
            actualExecuteCount = scriptAgentTaskService.getActualSuccessExecuteCount(stepInstanceId, host.toCloudIp());
        }

        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceHostLogDTO> resp;
        if (agentTask.getHostId() != null) {
            resp = logServiceResourceClient.getFileHostLogByHostId(taskCreateDateStr,
                stepInstanceId, actualExecuteCount, agentTask.getHostId(), mode, batch);
        } else {
            // 兼容ip查询
            resp = logServiceResourceClient.getFileHostLogByIp(taskCreateDateStr,
                stepInstanceId, actualExecuteCount, agentTask.getCloudIp(), mode, batch);
        }

        if (!resp.isSuccess()) {
            log.error("Get file log content by ip error, stepInstanceId={}, executeCount={}, batch={}, host={}",
                stepInstanceId, actualExecuteCount, batch, host);
            throw new InternalException(resp.getCode());
        }
        List<ServiceFileTaskLogDTO> fileTaskLogs = (resp.getData() == null) ? null : resp.getData().getFileTaskLogs();
        AgentTaskStatusEnum agentTaskStatus = agentTask.getStatus();
        boolean isFinished = agentTaskStatus.isFinished() || isAllFileTasksFinished(fileTaskLogs);
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
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<ServiceHostLogDTO> resp = logServiceResourceClient.listFileHostLogsByTaskIds(
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
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<List<ServiceFileTaskLogDTO>> resp = logServiceResourceClient.listFileHostLogs(
            taskCreateDateStr, stepInstanceId, executeCount, batch, FileDistModeEnum.UPLOAD.getValue(), null, null);
        if (!resp.isSuccess()) {
            log.error("Get file source log content error, stepInstanceId={}, executeCount={}, batch={}",
                stepInstanceId, executeCount, batch);
            return Collections.emptyList();
        }
        return resp.getData();
    }

    @Override
    public ServiceHostLogsDTO batchGetFileIpLogContent(long stepInstanceId, int executeCount, Integer batch,
                                                       List<HostDTO> hosts) {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        ServiceFileLogQueryRequest request = new ServiceFileLogQueryRequest();
        request.setStepInstanceId(stepInstanceId);
        request.setExecuteCount(executeCount);
        request.setBatch(batch);
        request.setJobCreateDate(taskCreateDateStr);

        if (isQueryByHostIdCondition(stepInstance)) {
            request.setHostIds(buildHostIdQueryCondition(stepInstance, hosts));
        } else {
            request.setIps(hosts.stream().map(HostDTO::toCloudIp).distinct().collect(Collectors.toList()));
        }

        InternalResponse<ServiceHostLogsDTO> resp = logServiceResourceClient.listFileHostLogs(request);
        if (!resp.isSuccess()) {
            log.error("Get file log content error, request={}", request);
            return null;
        }
        return resp.getData();
    }

    @Override
    public List<HostDTO> getIpsByContentKeyword(long stepInstanceId, int executeCount, Integer batch,
                                                String keyword) {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        String taskCreateDateStr = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        InternalResponse<List<HostDTO>> resp = logServiceResourceClient.questHostsByLogKeyword(taskCreateDateStr,
            stepInstanceId, executeCount, batch, keyword);

        if (!resp.isSuccess()) {
            log.error("Search ips by keyword error, stepInstanceId={}, executeCount={}, keyword={}", stepInstanceId,
                executeCount, keyword);
            throw new InternalException(resp.getCode());
        }
        return resp.getData();
    }

    public void writeFileLogsWithTimestamp(long jobCreateTime,
                                           List<ServiceHostLogDTO> hostFileLogs,
                                           Long logTimeInMillSeconds) {

        if (CollectionUtils.isEmpty(hostFileLogs)) {
            return;
        }

        ServiceBatchSaveLogRequest request = new ServiceBatchSaveLogRequest();
        request.setJobCreateDate(DateUtils.formatUnixTimestamp(jobCreateTime, ChronoUnit.MILLIS, "yyyy_MM_dd",
            ZoneId.of("UTC")));
        request.setLogType(LogTypeEnum.FILE.getValue());

        String logDateTime = "[";
        if (logTimeInMillSeconds != null) {
            logDateTime += DateUtils.formatUnixTimestamp(logTimeInMillSeconds, ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        } else {
            logDateTime += DateUtils.formatUnixTimestamp(System.currentTimeMillis(), ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault());
        }
        logDateTime += "] ";
        for (ServiceHostLogDTO hostFileLog : hostFileLogs) {
            for (ServiceFileTaskLogDTO fileTaskLog : hostFileLog.getFileTaskLogs()) {
                if (StringUtils.isBlank(fileTaskLog.getContent())) {
                    continue;
                }
                fileTaskLog.setContent(logDateTime + fileTaskLog.getContent() + "\n");
            }
            request.setLogs(hostFileLogs);
        }
        logServiceResourceClient.saveLogs(request);
    }

    @Override
    public void writeFileLogs(long jobCreateTime, List<ServiceHostLogDTO> hostFileLogs) {
        writeFileLogsWithTimestamp(jobCreateTime, hostFileLogs, System.currentTimeMillis());
    }
}
