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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.converter.StepTypeExecuteTypeConverter;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskExecutionDTO;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.ExecutionResultGroupDTO;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.StepExecutionDTO;
import com.tencent.bk.job.execute.model.StepExecutionDetailDTO;
import com.tencent.bk.job.execute.model.StepExecutionRecordDTO;
import com.tencent.bk.job.execute.model.StepExecutionResultQuery;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskExecuteResultDTO;
import com.tencent.bk.job.execute.model.TaskExecutionDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.inner.CronTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.job.execute.service.ExecuteAuthService;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ServerService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;

/**
 * 作业执行结果查询Service
 */
@Service
@Slf4j
public class TaskResultServiceImpl implements TaskResultService {
    private final TaskInstanceDAO taskInstanceDAO;
    private final StepInstanceDAO stepInstanceDAO;
    private final GseTaskLogService gseTaskLogService;
    private final FileSourceTaskLogDAO fileSourceTaskLogDAO;
    private final GseTaskIpLogDAO gseTaskIpLogDAO;
    private final ServerService serverService;
    private final LogService logService;
    private final ExecuteAuthService executeAuthService;
    private final TaskOperationLogService operationLogService;

    @Autowired
    public TaskResultServiceImpl(TaskInstanceDAO taskInstanceDAO, StepInstanceDAO stepInstanceDAO,
                                 GseTaskLogService gseTaskLogService, FileSourceTaskLogDAO fileSourceTaskLogDAO,
                                 GseTaskIpLogDAO gseTaskIpLogDAO,
                                 ServerService serverService, LogService logService,
                                 ExecuteAuthService executeAuthService,
                                 TaskOperationLogService operationLogService) {
        this.taskInstanceDAO = taskInstanceDAO;
        this.stepInstanceDAO = stepInstanceDAO;
        this.gseTaskLogService = gseTaskLogService;
        this.fileSourceTaskLogDAO = fileSourceTaskLogDAO;
        this.gseTaskIpLogDAO = gseTaskIpLogDAO;
        this.serverService = serverService;
        this.logService = logService;
        this.executeAuthService = executeAuthService;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageData<TaskInstanceDTO> listPageTaskInstance(TaskInstanceQuery taskQuery,
                                                          BaseSearchCondition baseSearchCondition) {
        PageData<TaskInstanceDTO> pageData = taskInstanceDAO.listPageTaskInstance(taskQuery, baseSearchCondition);
        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
            pageData.getData().forEach(taskInstanceDTO -> {
                if (taskInstanceDTO.getTotalTime() == null) {
                    if (taskInstanceDTO.getStatus().equals(RunStatusEnum.RUNNING.getValue())
                        || taskInstanceDTO.getStatus().equals(RunStatusEnum.WAITING.getValue())
                        || taskInstanceDTO.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
                        taskInstanceDTO.setTotalTime((TaskCostCalculator.calculate(taskInstanceDTO.getStartTime(),
                            taskInstanceDTO.getEndTime(), taskInstanceDTO.getTotalTime())));
                    }
                }
            });
        }
        return pageData;
    }

    @Override
    public TaskExecuteResultDTO getTaskExecutionResult(String username, Long appId,
                                                       Long taskInstanceId) throws ServiceException {
        TaskInstanceDTO taskInstance = taskInstanceDAO.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            log.warn("Task instance is not exist, taskInstanceId={}", taskInstanceId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        if (!taskInstance.getAppId().equals(appId)) {
            log.warn("Task instance is not in application, taskInstanceId={}, appId={}", taskInstanceId, appId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        authViewTaskInstance(username, appId, taskInstance);

        TaskExecutionDTO taskExecution = buildTaskExecutionDTO(taskInstance);

        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
        List<StepExecutionDTO> stepExecutionList = new ArrayList<>();
        for (StepInstanceBaseDTO stepInstance : stepInstanceList) {
            stepExecutionList.add(buildStepExecutionDTO(stepInstance));
        }

        TaskExecuteResultDTO taskExecuteResult = new TaskExecuteResultDTO();
        boolean isFinish = (!taskInstance.getStatus().equals(RunStatusEnum.BLANK.getValue())
            && !taskInstance.getStatus().equals(RunStatusEnum.RUNNING.getValue())
            && !taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue()));
        taskExecuteResult.setFinished(isFinish);
        taskExecuteResult.setTaskInstanceExecutionResult(taskExecution);
        taskExecuteResult.setStepInstanceExecutionResults(stepExecutionList);

        return taskExecuteResult;
    }

    private TaskExecutionDTO buildTaskExecutionDTO(TaskInstanceDTO taskInstance) {
        TaskExecutionDTO taskExecution = new TaskExecutionDTO();
        taskExecution.setTaskInstanceId(taskInstance.getId());
        taskExecution.setName(taskInstance.getName());
        taskExecution.setType(taskInstance.getType());
        taskExecution.setStatus(taskInstance.getStatus());
        taskExecution.setTotalTime(TaskCostCalculator.calculate(taskInstance.getStartTime(),
            taskInstance.getEndTime(), taskInstance.getTotalTime()));
        taskExecution.setStartTime(taskInstance.getStartTime());
        taskExecution.setEndTime(taskInstance.getEndTime());
        taskExecution.setTaskId(taskInstance.getTaskId());
        taskExecution.setTaskTemplateId(taskInstance.getTaskTemplateId());
        taskExecution.setDebugTask(taskInstance.isDebugTask());
        taskExecution.setCurrentStepInstanceId(taskInstance.getCurrentStepId());
        return taskExecution;
    }

    private StepExecutionDTO buildStepExecutionDTO(StepInstanceBaseDTO stepInstance) {
        StepExecutionDTO stepExecution = new StepExecutionDTO();
        stepExecution.setStepInstanceId(stepInstance.getId());
        stepExecution.setName(stepInstance.getName());
        stepExecution.setExecuteCount(stepInstance.getExecuteCount());
        stepExecution.setStatus(stepInstance.getStatus());
        stepExecution.setType(StepTypeExecuteTypeConverter.convertToStepType(stepInstance.getExecuteType()));
        stepExecution.setStartTime(stepInstance.getStartTime());
        stepExecution.setEndTime(stepInstance.getEndTime());
        stepExecution.setOperator(stepInstance.getOperator());
        stepExecution.setTotalTime(TaskCostCalculator.calculate(stepInstance.getStartTime(),
            stepInstance.getEndTime(), stepInstance.getTotalTime()));
        stepExecution.setLastStep(stepInstance.isLastStep());
        if (stepInstance.getExecuteType().equals(StepExecuteTypeEnum.MANUAL_CONFIRM.getValue())) {
            ConfirmStepInstanceDTO confirmStepInstance = stepInstanceDAO.getConfirmStepInstance(stepInstance.getId());
            if (confirmStepInstance != null) {
                stepExecution.setConfirmMessage(confirmStepInstance.getConfirmMessage());
                stepExecution.setConfirmReason(confirmStepInstance.getConfirmReason());
                stepExecution.setConfirmNotifyChannels(confirmStepInstance.getNotifyChannels());
                stepExecution.setConfirmUsers(confirmStepInstance.getConfirmUsers());
                stepExecution.setConfirmRoles(confirmStepInstance.getConfirmRoles());
            }
        }
        return stepExecution;
    }

    private void authViewTaskInstance(String username, Long appId, TaskInstanceDTO taskInstance) {
        AuthResult authResult = executeAuthService.authViewTaskInstance(username, appId, taskInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    private void authViewStepInstance(String username, Long appId, StepInstanceBaseDTO stepInstance) {
        String operator = stepInstance.getOperator();
        if (username.equals(operator)) {
            return;
        }
        AuthResult authResult = executeAuthService.authViewTaskInstance(username, appId,
            stepInstance.getTaskInstanceId());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    /**
     * 融入文件源文件拉取所使用的时间
     *
     * @param stepExecutionDetailDTO
     * @param fileSourceTaskLog
     */
    private void involveFileSourceTaskLog(StepExecutionDetailDTO stepExecutionDetailDTO,
                                          FileSourceTaskLogDTO fileSourceTaskLog) {
        List<ExecutionResultGroupDTO> resultGroups = stepExecutionDetailDTO.getResultGroups();
        for (ExecutionResultGroupDTO resultGroup : resultGroups) {
            if (resultGroup == null) {
                continue;
            }
            List<AgentTaskExecutionDTO> agentTaskExecutionDetailList = resultGroup.getAgentTaskExecutionDetail();
            if (agentTaskExecutionDetailList == null) {
                continue;
            }
            for (AgentTaskExecutionDTO agentTaskDetail : agentTaskExecutionDetailList) {
                if (agentTaskDetail == null) {
                    continue;
                }
                agentTaskDetail.setStartTime(fileSourceTaskLog.getStartTime());
                if (agentTaskDetail.getEndTime() == null || agentTaskDetail.getEndTime() == 0) {
                    agentTaskDetail.setEndTime(stepExecutionDetailDTO.getEndTime());
                }
                agentTaskDetail.calculateTotalTime();
            }
        }
    }

    private StepInstanceBaseDTO checkGetStepExecutionDetail(String username, long appId, long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        if (stepInstance == null) {
            log.warn("Step instance is not exist, stepInstanceId={}", stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        authViewStepInstance(username, appId, stepInstance);

        if (stepInstance.getExecuteType().equals(StepExecuteTypeEnum.MANUAL_CONFIRM.getValue())) {
            log.warn("Manual confirm step does not support get-step-detail operation");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        return stepInstance;
    }

    private boolean isMatchResultGroup(ExecutionResultGroupDTO resultGroup, Integer resultType, String tag) {
        String matchTag = tag == null ? "" : tag;
        return resultType.equals(resultGroup.getResultType()) && matchTag.equals(resultGroup.getTag());
    }

    private <T> List<T> getLimitedSizedList(List<T> list, Integer maxSize) {
        if (maxSize == null) {
            return list;
        }
        int size = list.size();
        if (size <= maxSize) {
            return list;
        } else {
            return list.subList(0, maxSize);
        }
    }

    private StepExecutionDetailDTO buildStepExecutionDetailDTO(StepInstanceBaseDTO stepInstance,
                                                               GseTaskLogDTO gseTaskLog, Integer executeCount) {
        StepExecutionDetailDTO executeDetail = new StepExecutionDetailDTO();
        executeDetail.setStepInstanceId(stepInstance.getId());
        executeDetail.setExecuteCount(executeCount);

        executeDetail.setName(stepInstance.getName());

        executeDetail.setFinished(!stepInstance.getStatus().equals(RunStatusEnum.BLANK.getValue())
            && !stepInstance.getStatus().equals(RunStatusEnum.RUNNING.getValue())
            && !stepInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue()));
        executeDetail.setStatus(stepInstance.getStatus());
        executeDetail.setStartTime(gseTaskLog.getStartTime());
        executeDetail.setEndTime(gseTaskLog.getEndTime());
        executeDetail.setTotalTime(TaskCostCalculator.calculate(gseTaskLog.getStartTime(), gseTaskLog.getEndTime(),
            gseTaskLog.getTotalTime()));
        executeDetail.setLastStep(stepInstance.isLastStep());
        executeDetail.setStepType(stepInstance.getStepType());
        executeDetail.setGseTaskId(gseTaskLog.getGseTaskId());
        return executeDetail;
    }

    @Override
    public StepExecutionDetailDTO getFastTaskStepExecutionResult(String username, Long appId, Long taskInstanceId,
                                                                 StepExecutionResultQuery query) {
        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
        StepInstanceBaseDTO stepInstance = stepInstanceList.get(0);
        if (stepInstance == null) {
            log.warn("Step instance is not exist, taskInstanceId={}", taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("Step instance is not in application, stepInstanceId={}, appId={}", stepInstance.getId(), appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        return getStepExecutionResult(username, appId, query);
    }

    private void setAgentTasksForSpecifiedResultType(List<ExecutionResultGroupDTO> resultGroups,
                                                     long appId, Integer resultType, String tag,
                                                     List<GseTaskIpLogDTO> ipLogsForResultType) {
        for (ExecutionResultGroupDTO resultGroup : resultGroups) {
            if (resultType.equals(resultGroup.getResultType()) && (
                (StringUtils.isEmpty(tag) ? StringUtils.isEmpty(resultGroup.getTag()) :
                    tag.equals(resultGroup.getTag())))) {
                fillAgentTasksForResultGroup(resultGroup, appId, ipLogsForResultType);
            }
        }
    }

    private List<ExecutionResultGroupDTO> getExecutionResultGroupsFromDB(long stepInstanceId, int executeCount) {
        List<ResultGroupBaseDTO> baseResultGroups = gseTaskIpLogDAO.getResultGroups(stepInstanceId, executeCount);
        return baseResultGroups.stream().map(this::buildExecutionResultGroup)
            .collect(Collectors.toList());
    }

    private void setAgentTasksForAnyResultType(List<ExecutionResultGroupDTO> resultGroups,
                                               long appId, long stepInstanceId,
                                               int executeCount, Integer maxAgentTasksForResultGroup) {
        boolean isAgentTaskSet = false;
        for (ExecutionResultGroupDTO resultGroup : resultGroups) {
            if (!isAgentTaskSet) {
                isAgentTaskSet = fillAgentTasksForResultGroup(resultGroup, appId, stepInstanceId, executeCount,
                    resultGroup.getResultType(), resultGroup.getTag(), maxAgentTasksForResultGroup);
            } else {
                return;
            }
        }
    }

    private ExecutionResultGroupDTO buildExecutionResultGroup(ResultGroupBaseDTO baseResultGroup) {
        ExecutionResultGroupDTO resultGroup = new ExecutionResultGroupDTO();
        resultGroup.setResultType(baseResultGroup.getResultType());
        resultGroup.setTag(baseResultGroup.getTag());
        resultGroup.setAgentTaskSize(baseResultGroup.getAgentTaskCount());
        return resultGroup;
    }

    private boolean fillAgentTasksForResultGroup(ExecutionResultGroupDTO resultGroup, long appId, long stepInstanceId,
                                                 int executeCount,
                                                 Integer resultType, String tag, Integer maxAgentTasksForResultGroup) {
        List<GseTaskIpLogDTO> ipLogGroupByResultType = gseTaskIpLogDAO.getIpLogByResultType(stepInstanceId,
            executeCount, resultType, tag, maxAgentTasksForResultGroup, null, null);
        return fillAgentTasksForResultGroup(resultGroup, appId, ipLogGroupByResultType);
    }

    private boolean fillAgentTasksForResultGroup(ExecutionResultGroupDTO resultGroup, long appId,
                                                 List<GseTaskIpLogDTO> ipLogGroupByResultType) {
        if (CollectionUtils.isEmpty(ipLogGroupByResultType)) {
            return false;
        }
        List<AgentTaskExecutionDTO> agentTaskExecutions = ipLogGroupByResultType.stream()
            .map(gseTaskIpLog -> buildAgentTaskExecutionDTO(appId, gseTaskIpLog)).collect(Collectors.toList());
        resultGroup.setAgentTaskExecutionDetail(agentTaskExecutions);
        return true;
    }

    private AgentTaskExecutionDTO buildAgentTaskExecutionDTO(long appId, GseTaskIpLogDTO gseTaskIpLog) {
        AgentTaskExecutionDTO agentTaskExecution = new AgentTaskExecutionDTO();
        agentTaskExecution.setCloudIp(gseTaskIpLog.getCloudAreaAndIp());
        long cloudAreaId = Long.parseLong(gseTaskIpLog.getCloudAreaAndIp().split(":")[0]);
        agentTaskExecution.setCloudAreaId(cloudAreaId);
        agentTaskExecution.setCloudAreaName(serverService.getCloudAreaName(appId, cloudAreaId));
        agentTaskExecution.setDisplayIp(gseTaskIpLog.getDisplayIp());
        agentTaskExecution.setStatus(gseTaskIpLog.getStatus());
        agentTaskExecution.setTag(gseTaskIpLog.getTag());
        agentTaskExecution.setErrorCode(gseTaskIpLog.getErrCode());
        agentTaskExecution.setExitCode(gseTaskIpLog.getExitCode());
        agentTaskExecution.setStartTime(gseTaskIpLog.getStartTime());
        agentTaskExecution.setEndTime(gseTaskIpLog.getEndTime());
        agentTaskExecution.setTotalTime(gseTaskIpLog.getTotalTime());
        agentTaskExecution.setExecuteCount(gseTaskIpLog.getExecuteCount());
        return agentTaskExecution;
    }

    private List<AgentTaskExecutionDTO> buildAgentTaskExecutionDTOList(long appId,
                                                                       List<GseTaskIpLogDTO> gseTaskIpLogs) {
        return gseTaskIpLogs.stream().map(gseTaskIpLog -> buildAgentTaskExecutionDTO(appId, gseTaskIpLog))
            .collect(Collectors.toList());
    }

    /*
     * 任务未下发到gse的情况下，构造步骤执行返回结果
     */
    private StepExecutionDetailDTO buildNotStartStepExecutionResult(long appId, StepInstanceBaseDTO stepInstance,
                                                                    Integer executeCount,
                                                                    Integer maxAgentTasksForResultGroup,
                                                                    String fuzzySearchIp) {
        long stepInstanceId = stepInstance.getId();
        int retry = executeCount == null ? stepInstance.getExecuteCount() : executeCount;

        StepExecutionDetailDTO executeDetail = new StepExecutionDetailDTO();
        executeDetail.setStepInstanceId(stepInstanceId);
        executeDetail.setExecuteCount(retry);

        executeDetail.setName(stepInstance.getName());

        executeDetail.setFinished(false);
        executeDetail.setStatus(stepInstance.getStatus());
        executeDetail.setStartTime(stepInstance.getStartTime());
        executeDetail.setEndTime(stepInstance.getEndTime());
        executeDetail.setTotalTime(0L);
        executeDetail.setLastStep(stepInstance.isLastStep());
        executeDetail.setStepType(stepInstance.getStepType());

        List<ExecutionResultGroupDTO> resultGroups = new ArrayList<>();
        ExecutionResultGroupDTO resultGroup = new ExecutionResultGroupDTO();
        resultGroup.setResultType(IpStatus.WAITING.getValue());
        resultGroup.setTag(null);

        List<AgentTaskExecutionDTO> agentTaskExecutionDTOS = new ArrayList<>();
        boolean fuzzyFilterByIp = StringUtils.isNotEmpty(fuzzySearchIp);
        int agentTaskSize = stepInstance.getTargetServers().getIpList().size();
        if (fuzzyFilterByIp) {
            agentTaskSize = (int) stepInstance.getTargetServers().getIpList().stream()
                .filter(ipDTO -> ipDTO.getIp().contains(fuzzySearchIp)).count();
        }
        if (stepInstance.getTargetServers().getIpList() != null) {
            int maxAgentTasks = (maxAgentTasksForResultGroup != null ?
                Math.min(maxAgentTasksForResultGroup, stepInstance.getTargetServers().getIpList().size())
                : stepInstance.getTargetServers().getIpList().size());
            for (IpDTO ipDTO : stepInstance.getTargetServers().getIpList()) {
                String ip = ipDTO.getIp();
                if (fuzzyFilterByIp && !ip.contains(fuzzySearchIp)) {
                    continue;
                }
                if (maxAgentTasks-- > 0) {
                    AgentTaskExecutionDTO agentTaskExecutionDTO = new AgentTaskExecutionDTO();
                    agentTaskExecutionDTO.setCloudIp(ipDTO.getCloudAreaId() + ":" + ipDTO.getIp());
                    Long cloudAreaId = ipDTO.getCloudAreaId();
                    agentTaskExecutionDTO.setCloudAreaId(cloudAreaId);
                    agentTaskExecutionDTO.setCloudAreaName(serverService.getCloudAreaName(appId, cloudAreaId));
                    agentTaskExecutionDTO.setDisplayIp(ipDTO.getIp());
                    agentTaskExecutionDTO.setStatus(IpStatus.WAITING.getValue());
                    agentTaskExecutionDTO.setTag(null);
                    agentTaskExecutionDTO.setErrorCode(0);
                    agentTaskExecutionDTO.setExitCode(0);
                    agentTaskExecutionDTO.setTotalTime(0L);
                    agentTaskExecutionDTOS.add(agentTaskExecutionDTO);
                }
            }
        }
        resultGroup.setAgentTaskExecutionDetail(agentTaskExecutionDTOS);
        resultGroup.setAgentTaskSize(agentTaskSize);
        resultGroups.add(resultGroup);

        executeDetail.setResultGroups(resultGroups);
        return executeDetail;
    }

    @Override
    public StepExecutionDetailDTO getStepExecutionResult(String username, Long appId,
                                                         StepExecutionResultQuery query) throws ServiceException {
        Long stepInstanceId = query.getStepInstanceId();
        StopWatch watch = new StopWatch("getStepExecutionResult");
        watch.start("checkGetStepExecutionDetail");
        StepInstanceBaseDTO stepInstance = checkGetStepExecutionDetail(username, appId, stepInstanceId);
        int finalExecuteCount = query.getExecuteCount() == null ? stepInstance.getExecuteCount() :
            query.getExecuteCount();
        query.setExecuteCount(finalExecuteCount);
        watch.stop();

        GseTaskLogDTO gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstance.getId(), finalExecuteCount);
        if (gseTaskLog == null) {
            return buildNotStartStepExecutionResult(appId, stepInstance, finalExecuteCount,
                query.getMaxAgentTasksForResultGroup(), query.getSearchIp());
        }

        StepExecutionDetailDTO stepExecutionDetail;
        // 如果步骤的目标服务器数量<100,或者通过IP匹配的方式过滤agent任务，为了提升性能，直接全量从DB查询数据，在内存进行处理
        if ((stepInstance.getTargetServerTotalCount() <= 100) || query.hasIpCondition()) {
            stepExecutionDetail = loadAllTasksFromDBAndBuildExecutionResultInMemory(watch, stepInstance, gseTaskLog,
                query);
        } else {
            stepExecutionDetail = filterAndSortExecutionResultInDB(watch, stepInstance, gseTaskLog, query);
        }

        stepExecutionDetail.setStartTime(stepInstance.getStartTime());
        if (stepInstance.getEndTime() != null) {
            stepExecutionDetail.setEndTime(stepInstance.getEndTime());
            stepExecutionDetail.calculateTotalTime();
        }

        watch.start("involveFileSourceTaskLog");
        FileSourceTaskLogDTO fileSourceTaskLog = fileSourceTaskLogDAO.getFileSourceTaskLog(stepInstance.getId(),
            finalExecuteCount);
        if (fileSourceTaskLog != null) {
            involveFileSourceTaskLog(stepExecutionDetail, fileSourceTaskLog);
        }
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("Get step execution detail is slow, watch: {}", watch.prettyPrint());
        }

        return stepExecutionDetail;
    }

    private StepExecutionDetailDTO loadAllTasksFromDBAndBuildExecutionResultInMemory(StopWatch watch,
                                                                                     StepInstanceBaseDTO stepInstance,
                                                                                     GseTaskLogDTO gseTaskLog,
                                                                                     StepExecutionResultQuery query) {
        try {
            long stepInstanceId = query.getStepInstanceId();
            int executeCount = query.getExecuteCount();
            long appId = stepInstance.getAppId();

            if (query.hasIpCondition()) {
                watch.start("getMatchIps");
                Set<String> matchIps = getMatchIps(query);
                if (CollectionUtils.isEmpty(matchIps)) {
                    watch.stop();
                    return buildExecutionDetailWhenTaskAreEmpty(stepInstance, gseTaskLog);
                } else {
                    query.setMatchIps(matchIps);
                }
                watch.stop();
            }

            watch.start("loadAllTasksFromDb");
            List<GseTaskIpLogDTO> tasks = gseTaskLogService.getIpLog(stepInstanceId, executeCount, true);
            watch.stop();
            watch.start("buildResultGroupsFromTasks");
            List<ExecutionResultGroupDTO> resultGroups = buildResultGroupsFromTasks(appId, tasks);
            watch.stop();

            if (CollectionUtils.isNotEmpty(query.getMatchIps())) {
                watch.start("filterExecutionResultByMatchIp");
                filterExecutionResultByMatchIp(resultGroups, query.getMatchIps());
                watch.stop();
            }

            watch.start("removeAgentTasksExecutionDetail");
            removeAgentTasksExecutionDetail(resultGroups, query.getResultType(), query.getTag());
            watch.stop();

            ExecutionResultGroupDTO resultGroupWithTasks = resultGroups.stream()
                .filter(resultGroup -> CollectionUtils.isNotEmpty(resultGroup.getAgentTaskExecutionDetail()))
                .findFirst().orElse(null);
            if (resultGroupWithTasks == null) {
                StepExecutionDetailDTO executeDetail = buildStepExecutionDetailDTO(stepInstance, gseTaskLog,
                    executeCount);
                executeDetail.setResultGroups(resultGroups);
                return executeDetail;
            }

            watch.start("sortAndLimitTasks");
            sortAgentTasks(resultGroupWithTasks, query);

            if (query.getMaxAgentTasksForResultGroup() != null) {
                resultGroupWithTasks.setAgentTaskExecutionDetail(
                    getLimitedSizedList(resultGroupWithTasks.getAgentTaskExecutionDetail(),
                        query.getMaxAgentTasksForResultGroup()));
            }

            StepExecutionDetailDTO executeDetail = buildStepExecutionDetailDTO(stepInstance, gseTaskLog, executeCount);
            executeDetail.setResultGroups(resultGroups);

            watch.stop();
            return executeDetail;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
        }

    }

    private void sortAgentTasks(ExecutionResultGroupDTO resultGroupWithTasks, StepExecutionResultQuery query) {
        if (StringUtils.isNotEmpty(query.getOrderField())) {
            List<AgentTaskExecutionDTO> taskList = resultGroupWithTasks.getAgentTaskExecutionDetail();
            if (StepExecutionResultQuery.ORDER_FIELD_TOTAL_TIME.equals(query.getOrderField())) {
                taskList.sort(Comparator.comparingLong(task -> task.getTotalTime() == null ? 0L : task.getTotalTime()));
                if (query.getOrder() == DESCENDING) {
                    Collections.reverse(taskList);
                }
            } else if (StepExecutionResultQuery.ORDER_FIELD_EXIT_CODE.equals(query.getOrderField())) {
                taskList.sort((o1, o2) -> {
                    if (o1.getExitCode() != null && o2.getExitCode() != null) {
                        if (o1.getExitCode().equals(o2.getExitCode())) {
                            return 0;
                        } else {
                            return o1.getExitCode() > o2.getExitCode() ? 1 : -1;
                        }
                    } else if (o1.getExitCode() == null) {
                        return -1;
                    } else if (o2.getExitCode() == null) {
                        return 1;
                    } else {
                        return 0;
                    }
                });
                if (query.getOrder() == DESCENDING) {
                    Collections.reverse(taskList);
                }
            } else if (StepExecutionResultQuery.ORDER_FIELD_CLOUD_AREA_ID.equals(query.getOrderField())) {
                taskList.sort(Comparator.comparing(AgentTaskExecutionDTO::getCloudIp));
                if (query.getOrder() == DESCENDING) {
                    Collections.reverse(taskList);
                }
            }
        }
    }


    private Set<String> getMatchIps(StepExecutionResultQuery query) {
        long stepInstanceId = query.getStepInstanceId();
        int executeCount = query.getExecuteCount();

        Set<String> matchIpsByLogKeywordSearch = null;
        if (StringUtils.isNotBlank(query.getLogKeyword())) {
            List<IpDTO> matchHosts = getHostsByLogContentKeyword(stepInstanceId, executeCount, query.getLogKeyword());
            if (CollectionUtils.isNotEmpty(matchHosts)) {
                matchIpsByLogKeywordSearch = matchHosts.stream().map(IpDTO::convertToStrIp).collect(Collectors.toSet());
            }
        }
        Set<String> matchIpsByIpSearch = null;
        if (StringUtils.isNotBlank(query.getSearchIp())) {
            List<IpDTO> matchHosts = fuzzySearchHostsByIp(stepInstanceId, executeCount, query.getSearchIp());
            if (CollectionUtils.isNotEmpty(matchHosts)) {
                matchIpsByIpSearch = matchHosts.stream().map(IpDTO::convertToStrIp).collect(Collectors.toSet());
            }
        }

        log.info("matchIpsByLogKeywordSearch: {}, matchIpsByIpSearch: {}", matchIpsByLogKeywordSearch,
            matchIpsByIpSearch);

        if (matchIpsByLogKeywordSearch != null && matchIpsByIpSearch != null) {
            return new HashSet<>(CollectionUtils.intersection(matchIpsByLogKeywordSearch, matchIpsByIpSearch));
        } else if (matchIpsByLogKeywordSearch != null) {
            return matchIpsByLogKeywordSearch;
        } else if (matchIpsByIpSearch != null) {
            return matchIpsByIpSearch;
        } else {
            return Collections.emptySet();
        }
    }

    private StepExecutionDetailDTO buildExecutionDetailWhenTaskAreEmpty(StepInstanceBaseDTO stepInstance,
                                                                        GseTaskLogDTO gseTaskLog) {
        List<ResultGroupBaseDTO> baseResultGroups = gseTaskIpLogDAO
            .getResultGroups(stepInstance.getId(), stepInstance.getExecuteCount());
        StepExecutionDetailDTO executeDetail = buildStepExecutionDetailDTO(stepInstance,
            gseTaskLog, stepInstance.getExecuteCount());
        List<ExecutionResultGroupDTO> resultGroups = new ArrayList<>();
        for (ResultGroupBaseDTO baseResultGroup : baseResultGroups) {
            ExecutionResultGroupDTO resultGroup = buildExecutionResultGroup(baseResultGroup);
            resultGroup.setAgentTaskSize(0);
            resultGroups.add(resultGroup);
        }
        executeDetail.setResultGroups(resultGroups);
        return executeDetail;
    }

    private void filterExecutionResultByMatchIp(List<ExecutionResultGroupDTO> resultGroups, Set<String> matchIps) {
        for (ExecutionResultGroupDTO resultGroup : resultGroups) {
            List<AgentTaskExecutionDTO> agentTaskExecutions = resultGroup.getAgentTaskExecutionDetail();
            if (CollectionUtils.isNotEmpty(agentTaskExecutions)) {
                agentTaskExecutions = agentTaskExecutions.stream().filter(
                    agentTaskExecution -> matchIps.contains(agentTaskExecution.getCloudIp())).collect(
                    Collectors.toList());
                resultGroup.setAgentTaskExecutionDetail(agentTaskExecutions);
                resultGroup.setAgentTaskSize(agentTaskExecutions.size());
            }
        }
    }

    private void removeAgentTasksExecutionDetail(List<ExecutionResultGroupDTO> resultGroups, Integer resultType,
                                                 String tag) {
        if (resultType != null && resultGroups.contains(new ExecutionResultGroupDTO(resultType, tag))) {
            resultGroups.forEach(resultGroup -> {
                if (!isMatchResultGroup(resultGroup, resultType, tag)) {
                    resultGroup.setAgentTaskExecutionDetail(Collections.emptyList());
                }
            });
        } else {
            int i = 0;
            for (ExecutionResultGroupDTO resultGroup : resultGroups) {
                i++;
                if (i != 1) {
                    resultGroup.setAgentTaskExecutionDetail(Collections.emptyList());
                }
            }
        }
    }

    private List<ExecutionResultGroupDTO> buildResultGroupsFromTasks(long appId, List<GseTaskIpLogDTO> tasks) {
        Map<ExecutionResultGroupDTO, List<GseTaskIpLogDTO>> resultGroups = new HashMap<>();
        tasks.forEach(task -> resultGroups.computeIfAbsent(task.getExecutionResultGroup(),
            resultGroup -> new ArrayList<>()).add(task));
        resultGroups.forEach((resultGroup, taskList) -> {
            List<AgentTaskExecutionDTO> agentTasks = buildAgentTaskExecutionDTOList(appId, taskList);
            resultGroup.setAgentTaskExecutionDetail(agentTasks);
            resultGroup.setAgentTaskSize(agentTasks.size());
        });
        return new ArrayList<>(resultGroups.keySet());
    }

    private StepExecutionDetailDTO filterAndSortExecutionResultInDB(StopWatch watch,
                                                                    StepInstanceBaseDTO stepInstance,
                                                                    GseTaskLogDTO gseTaskLog,
                                                                    StepExecutionResultQuery query) {
        query.transformOrderFieldToDbField();
        long stepInstanceId = query.getStepInstanceId();
        int executeCount = query.getExecuteCount();
        Integer resultType = query.getResultType();
        String tag = query.getTag();
        long appId = stepInstance.getAppId();

        StepExecutionDetailDTO executeDetail = buildStepExecutionDetailDTO(stepInstance, gseTaskLog, executeCount);

        watch.start("getBaseResultGroups");
        List<ExecutionResultGroupDTO> resultGroups = getExecutionResultGroupsFromDB(stepInstanceId, executeCount);
        watch.stop();
        if (resultType != null) {
            watch.start("loadTasksFromDbForResultType");
            List<GseTaskIpLogDTO> tasks = gseTaskIpLogDAO.getIpLogByResultType(stepInstanceId, executeCount,
                resultType, tag, query.getMaxAgentTasksForResultGroup(), query.getOrderField(), query.getOrder());
            watch.stop();
            if (CollectionUtils.isNotEmpty(tasks)) {
                watch.start("setAgentTasksForSpecifiedResultType");
                setAgentTasksForSpecifiedResultType(resultGroups, appId, resultType, tag, tasks);
                watch.stop();
            } else {
                watch.start("setAgentTasksForAnyResultType");
                setAgentTasksForAnyResultType(resultGroups, appId, stepInstanceId, executeCount,
                    query.getMaxAgentTasksForResultGroup());
                watch.stop();
            }
        } else {
            watch.start("setAgentTasksForAnyResultType");
            setAgentTasksForAnyResultType(resultGroups, appId, stepInstanceId, executeCount,
                query.getMaxAgentTasksForResultGroup());
            watch.stop();
        }

        executeDetail.setResultGroups(resultGroups);
        return executeDetail;
    }

    private List<IpDTO> getHostsByLogContentKeyword(long stepInstanceId, int executeCount, String keyword) {
        String searchKey = keyword.replaceAll("'", "").replaceAll("\\$", "")
            .replaceAll("&", "").replaceAll("$", "")
            .replaceAll("\\|", "").replaceAll("`", "")
            .replaceAll(";", "");

        return logService.getIpsByContentKeyword(stepInstanceId, executeCount, searchKey);
    }

    private List<IpDTO> fuzzySearchHostsByIp(long stepInstanceId, int executeCount, String searchIp) {
        return gseTaskIpLogDAO.fuzzySearchTargetIpsByIp(stepInstanceId, executeCount, searchIp)
            .stream().map(IpDTO::fromCloudAreaIdAndIpStr).collect(Collectors.toList());
    }

    @Override
    public Map<Long, ServiceCronTaskExecuteResultStatistics> getCronTaskExecuteResultStatistics(long appId,
                                                                                                List<Long> cronTaskIdList) {
        if (cronTaskIdList == null || cronTaskIdList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ServiceCronTaskExecuteResultStatistics> statisticsMap = new HashMap<>();
        StopWatch watch = new StopWatch("cron-task-statistics");
        for (Long cronTaskId : cronTaskIdList) {
            watch.start("get-last24h-tasks-" + cronTaskId);
            List<TaskInstanceDTO> last24HourTaskInstances = taskInstanceDAO.listLatestCronTaskInstance(appId,
                cronTaskId, 86400L, null, null);

            boolean isGe10Within24Hour = false;
            // 已执行完成任务计数
            int doneTaskCount = 0;
            for (TaskInstanceDTO taskInstance : last24HourTaskInstances) {
                if (!taskInstance.getStatus().equals(RunStatusEnum.RUNNING.getValue())
                    && !taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
                    doneTaskCount++;
                }
                if (doneTaskCount >= 10) {
                    isGe10Within24Hour = true;
                    break;
                }
            }
            watch.stop();

            // 24小时内执行超过10次，统计24小时内所有的数据
            if (isGe10Within24Hour) {
                ServiceCronTaskExecuteResultStatistics statistic = new ServiceCronTaskExecuteResultStatistics();
                statistic.setCronTaskId(cronTaskId);
                statistic.setLast24HourExecuteRecords(convertToCronTaskExecuteResult(last24HourTaskInstances));
                statisticsMap.put(cronTaskId, statistic);
            } else {
                watch.start("get-last10-tasks-" + cronTaskId);
                // 如果24小时内执行次数少于10次，那么统计最近10次的数据。由于可能存在正在运行任务，所以默认返回最近11次的数据
                List<TaskInstanceDTO> last10TaskInstances = taskInstanceDAO.listLatestCronTaskInstance(appId,
                    cronTaskId, null, null, 11);
                ServiceCronTaskExecuteResultStatistics statistic = new ServiceCronTaskExecuteResultStatistics();
                statistic.setCronTaskId(cronTaskId);
                statistic.setLast10ExecuteRecords(convertToCronTaskExecuteResult(last10TaskInstances));
                statisticsMap.put(cronTaskId, statistic);
                watch.stop();
            }
        }
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Get cron task statistics is slow, cost: {}", watch.prettyPrint());
        }
        return statisticsMap;
    }

    private List<CronTaskExecuteResult> convertToCronTaskExecuteResult(List<TaskInstanceDTO> taskInstances) {
        return taskInstances.stream().map(taskInstance -> {
            CronTaskExecuteResult cronTaskExecuteResult = new CronTaskExecuteResult();
            cronTaskExecuteResult.setCronTaskId(taskInstance.getCronTaskId());
            cronTaskExecuteResult.setPlanId(taskInstance.getTaskId());
            cronTaskExecuteResult.setStatus(taskInstance.getStatus());
            cronTaskExecuteResult.setExecuteTime(taskInstance.getCreateTime());
            return cronTaskExecuteResult;
        }).collect(Collectors.toList());
    }

    @Override
    public List<IpDTO> getHostsByResultType(String username, Long appId, Long stepInstanceId, Integer executeCount,
                                            Integer resultType, String tag, String keyword) {
        StepInstanceBaseDTO stepInstance = checkGetStepExecutionDetail(username, appId, stepInstanceId);

        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("Step instance is not in application, stepInstanceId={}, appId={}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        StepExecutionResultQuery query = new StepExecutionResultQuery();
        query.setStepInstanceId(stepInstanceId);
        query.setExecuteCount(executeCount);
        query.setLogKeyword(keyword);

        Set<String> matchIps = null;
        boolean filterByKeyword = StringUtils.isNotEmpty(keyword);
        if (filterByKeyword) {
            matchIps = getMatchIps(query);
            if (CollectionUtils.isEmpty(matchIps)) {
                return Collections.emptyList();
            }
        }

        GseTaskLogDTO gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstance.getId(),
            stepInstance.getExecuteCount());
        if (gseTaskLog == null) {
            if (stepInstance.getTargetServers().getIpList() != null) {
                return stepInstance.getTargetServers().getIpList();
            } else {
                return Collections.emptyList();
            }
        }

        List<GseTaskIpLogDTO> ipLogGroupByResultType = gseTaskIpLogDAO.getIpLogByResultType(stepInstanceId,
            executeCount, resultType, tag);
        if (CollectionUtils.isEmpty(ipLogGroupByResultType)) {
            return Collections.emptyList();
        }
        List<IpDTO> hosts = ipLogGroupByResultType.stream()
            .map(gseTaskIpLog -> new IpDTO(gseTaskIpLog.getCloudAreaId(), gseTaskIpLog.getIp()))
            .collect(Collectors.toList());
        if (filterByKeyword && CollectionUtils.isNotEmpty(matchIps)) {
            List<IpDTO> finalHosts = new ArrayList<>();
            for (IpDTO host : hosts) {
                if (matchIps.contains(host.convertToStrIp())) {
                    finalHosts.add(host);
                }
            }
            return finalHosts;
        } else {
            return hosts;
        }
    }

    @Override
    public List<StepExecutionRecordDTO> listStepExecutionHistory(String username, Long appId, Long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = checkGetStepExecutionDetail(username, appId, stepInstanceId);
        int latestExecuteCount = stepInstance.getExecuteCount();
        if (latestExecuteCount == 0) {
            StepExecutionRecordDTO record = new StepExecutionRecordDTO();
            record.setStepInstanceId(stepInstanceId);
            record.setRetryCount(latestExecuteCount);
            record.setCreateTime(stepInstance.getCreateTime());
            return Collections.singletonList(record);
        }

        List<OperationLogDTO> operationLogs = operationLogService.listOperationLog(stepInstance.getTaskInstanceId());
        List<StepExecutionRecordDTO> records = new ArrayList<>();
        if (CollectionUtils.isEmpty(operationLogs)) {
            for (int executeCount = latestExecuteCount; executeCount >= 0; executeCount--) {
                StepExecutionRecordDTO record = new StepExecutionRecordDTO();
                record.setStepInstanceId(stepInstanceId);
                record.setRetryCount(executeCount);
                record.setCreateTime(stepInstance.getCreateTime());
                records.add(record);
            }
            return records;
        }

        Map<Integer, Long> executeCountAndCreateTimeMap = new HashMap<>();
        operationLogs.forEach(opLog -> {
            UserOperationEnum operation = opLog.getOperationEnum();
            if (UserOperationEnum.START == operation) {
                executeCountAndCreateTimeMap.put(0, opLog.getCreateTime());
            } else if ((UserOperationEnum.RETRY_STEP_ALL == operation || UserOperationEnum.RETRY_STEP_FAIL == operation)
                && (opLog.getDetail() != null && stepInstanceId.equals(opLog.getDetail().getStepInstanceId()))) {
                // 操作记录保存的是重试前的任务信息，所以executeCount需要+1
                executeCountAndCreateTimeMap.put(opLog.getDetail().getExecuteCount() + 1, opLog.getCreateTime());
            }
        });

        for (int executeCount = latestExecuteCount; executeCount >= 0; executeCount--) {
            StepExecutionRecordDTO record = new StepExecutionRecordDTO();
            record.setStepInstanceId(stepInstanceId);
            record.setRetryCount(executeCount);
            record.setCreateTime(executeCountAndCreateTimeMap.get(executeCount));
            records.add(record);
        }
        return records;
    }
}
