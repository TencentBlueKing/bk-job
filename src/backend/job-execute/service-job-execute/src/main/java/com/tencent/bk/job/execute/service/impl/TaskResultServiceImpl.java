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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.StepRunModeEnum;
import com.tencent.bk.job.execute.common.converter.StepTypeExecuteTypeConverter;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepExecutionDTO;
import com.tencent.bk.job.execute.model.StepExecutionDetailDTO;
import com.tencent.bk.job.execute.model.StepExecutionRecordDTO;
import com.tencent.bk.job.execute.model.StepExecutionResultQuery;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.TaskExecuteResultDTO;
import com.tencent.bk.job.execute.model.TaskExecutionDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.inner.CronTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.service.TaskInstanceService;
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
import java.util.Objects;
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
    private final TaskInstanceService taskInstanceService;
    private final FileSourceTaskLogDAO fileSourceTaskLogDAO;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final LogService logService;
    private final TaskOperationLogService operationLogService;
    private final RollingConfigService rollingConfigService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final StepInstanceService stepInstanceService;

    @Autowired
    public TaskResultServiceImpl(TaskInstanceDAO taskInstanceDAO,
                                 StepInstanceDAO stepInstanceDAO,
                                 TaskInstanceService taskInstanceService,
                                 FileSourceTaskLogDAO fileSourceTaskLogDAO,
                                 ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                 FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                 LogService logService,
                                 TaskOperationLogService operationLogService,
                                 RollingConfigService rollingConfigService,
                                 StepInstanceRollingTaskService stepInstanceRollingTaskService,
                                 TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                 StepInstanceService stepInstanceService) {
        this.taskInstanceDAO = taskInstanceDAO;
        this.stepInstanceDAO = stepInstanceDAO;
        this.taskInstanceService = taskInstanceService;
        this.fileSourceTaskLogDAO = fileSourceTaskLogDAO;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.logService = logService;
        this.operationLogService = operationLogService;
        this.rollingConfigService = rollingConfigService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    public PageData<TaskInstanceDTO> listPageTaskInstance(TaskInstanceQuery taskQuery,
                                                          BaseSearchCondition baseSearchCondition) {
        PageData<TaskInstanceDTO> pageData = taskInstanceDAO.listPageTaskInstance(taskQuery, baseSearchCondition);
        computeTotalTime(pageData.getData());
        return pageData;
    }

    private void computeTotalTime(List<TaskInstanceDTO> pageData) {
        if (CollectionUtils.isNotEmpty(pageData)) {
            pageData.forEach(taskInstanceDTO -> {
                if (taskInstanceDTO.getTotalTime() == null) {
                    RunStatusEnum status = taskInstanceDTO.getStatus();
                    if (status == RunStatusEnum.RUNNING || status == RunStatusEnum.WAITING_USER
                        || status == RunStatusEnum.STOPPING) {
                        taskInstanceDTO.setTotalTime((TaskCostCalculator.calculate(taskInstanceDTO.getStartTime(),
                            taskInstanceDTO.getEndTime(), taskInstanceDTO.getTotalTime())));
                    }
                }
            });
        }
    }

    @Override
    public TaskExecuteResultDTO getTaskExecutionResult(String username, Long appId,
                                                       Long taskInstanceId) throws ServiceException {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(username, appId, taskInstanceId);

        TaskExecutionDTO taskExecution = buildTaskExecutionDTO(taskInstance);

        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
        List<StepExecutionDTO> stepExecutionList = new ArrayList<>();
        for (StepInstanceBaseDTO stepInstance : stepInstanceList) {
            stepExecutionList.add(buildStepExecutionDTO(stepInstance));
        }

        TaskExecuteResultDTO taskExecuteResult = new TaskExecuteResultDTO();
        taskExecuteResult.setFinished(RunStatusEnum.isFinishedStatus(taskInstance.getStatus()));
        taskExecuteResult.setTaskInstanceExecutionResult(taskExecution);
        taskExecuteResult.setStepInstanceExecutionResults(stepExecutionList);

        return taskExecuteResult;
    }

    private TaskExecutionDTO buildTaskExecutionDTO(TaskInstanceDTO taskInstance) {
        TaskExecutionDTO taskExecution = new TaskExecutionDTO();
        taskExecution.setTaskInstanceId(taskInstance.getId());
        taskExecution.setName(taskInstance.getName());
        taskExecution.setType(taskInstance.getType());
        taskExecution.setStatus(taskInstance.getStatus().getValue());
        taskExecution.setTotalTime(TaskCostCalculator.calculate(taskInstance.getStartTime(),
            taskInstance.getEndTime(), taskInstance.getTotalTime()));
        taskExecution.setStartTime(taskInstance.getStartTime());
        taskExecution.setEndTime(taskInstance.getEndTime());
        taskExecution.setTaskId(taskInstance.getPlanId());
        taskExecution.setTaskTemplateId(taskInstance.getTaskTemplateId());
        taskExecution.setDebugTask(taskInstance.isDebugTask());
        taskExecution.setCurrentStepInstanceId(taskInstance.getCurrentStepInstanceId());
        return taskExecution;
    }

    private StepExecutionDTO buildStepExecutionDTO(StepInstanceBaseDTO stepInstance) {
        StepExecutionDTO stepExecution = new StepExecutionDTO();
        stepExecution.setStepInstanceId(stepInstance.getId());
        stepExecution.setName(stepInstance.getName());
        stepExecution.setExecuteCount(stepInstance.getExecuteCount());
        stepExecution.setStatus(stepInstance.getStatus().getValue());
        stepExecution.setType(StepTypeExecuteTypeConverter.convertToStepType(stepInstance.getExecuteType()).getValue());
        stepExecution.setStartTime(stepInstance.getStartTime());
        stepExecution.setEndTime(stepInstance.getEndTime());
        stepExecution.setOperator(stepInstance.getOperator());
        stepExecution.setTotalTime(TaskCostCalculator.calculate(stepInstance.getStartTime(),
            stepInstance.getEndTime(), stepInstance.getTotalTime()));
        stepExecution.setLastStep(stepInstance.isLastStep());
        if (stepInstance.getExecuteType() == StepExecuteTypeEnum.MANUAL_CONFIRM) {
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

    /**
     * 加入文件源文件拉取所使用的时间
     *
     * @param stepExecutionDetail 步骤执行详情
     * @param fileSourceTaskLog   第三方文件源任务
     */
    private void involveFileSourceTaskLog(StepExecutionDetailDTO stepExecutionDetail,
                                          FileSourceTaskLogDTO fileSourceTaskLog) {
        List<ResultGroupDTO> resultGroups = stepExecutionDetail.getResultGroups();
        for (ResultGroupDTO resultGroup : resultGroups) {
            if (resultGroup == null) {
                continue;
            }
            List<ExecuteObjectTask> executeObjectTasks = resultGroup.getExecuteObjectTasks();
            if (executeObjectTasks == null) {
                continue;
            }
            for (ExecuteObjectTask executeObjectTask : executeObjectTasks) {
                if (executeObjectTask == null) {
                    continue;
                }
                executeObjectTask.setStartTime(fileSourceTaskLog.getStartTime());
                if (executeObjectTask.getEndTime() == null || executeObjectTask.getEndTime() == 0) {
                    executeObjectTask.setEndTime(stepExecutionDetail.getEndTime());
                }
                executeObjectTask.calculateTotalTime();
            }
        }
    }

    private boolean isMatchResultGroup(ResultGroupDTO resultGroup, Integer resultType, String tag) {
        String matchTag = tag == null ? "" : tag;
        return resultType.equals(resultGroup.getStatus()) && matchTag.equals(resultGroup.getTag());
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

    private void preProcessViewStepExecutionResult(String username, Long appId, StepInstanceBaseDTO stepInstance)
        throws NotFoundException, PermissionDeniedException {
        // 查看步骤执行结果预处理，包括鉴权、审计等
        taskInstanceAccessProcessor.processBeforeAccess(username, appId, stepInstance.getTaskInstanceId());

        if (stepInstance.getExecuteType() == StepExecuteTypeEnum.MANUAL_CONFIRM) {
            log.warn("Manual confirm step does not support get-step-detail operation");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
    }

    private void setExecuteObjectTasksForSpecifiedResultType(List<ResultGroupDTO> resultGroups,
                                                             Integer status,
                                                             String tag,
                                                             List<ExecuteObjectTask> executeObjectTasksForResultType) {
        for (ResultGroupDTO resultGroup : resultGroups) {
            if (status.equals(resultGroup.getStatus()) && (
                (StringUtils.isEmpty(tag) ? StringUtils.isEmpty(resultGroup.getTag()) :
                    tag.equals(resultGroup.getTag())))) {
                resultGroup.setExecuteObjectTasks(executeObjectTasksForResultType);
            }
        }
    }

    private void setAgentTasksForAnyResultType(List<ResultGroupDTO> resultGroups,
                                               StepInstanceBaseDTO stepInstance,
                                               int executeCount,
                                               Integer batch,
                                               Integer maxAgentTasksForResultGroup,
                                               boolean fetchAllGroupData) {
        boolean isAgentTaskSet = false;
        for (ResultGroupDTO resultGroup : resultGroups) {
            if (!isAgentTaskSet || fetchAllGroupData) {
                isAgentTaskSet = fillAgentTasksForResultGroup(resultGroup, stepInstance, executeCount,
                    batch, resultGroup.getStatus(), resultGroup.getTag(), maxAgentTasksForResultGroup);
            } else {
                return;
            }
        }
    }

    private boolean fillAgentTasksForResultGroup(ResultGroupDTO resultGroup,
                                                 StepInstanceBaseDTO stepInstance,
                                                 int executeCount,
                                                 Integer batch,
                                                 Integer status,
                                                 String tag,
                                                 Integer maxAgentTasksForResultGroup) {
        List<ExecuteObjectTask> executeObjectTasks = listExecuteObjectTaskByResultGroup(stepInstance,
            executeCount, batch, status, tag, maxAgentTasksForResultGroup, null, null);
        if (CollectionUtils.isEmpty(executeObjectTasks)) {
            return false;
        }
        resultGroup.setExecuteObjectTasks(executeObjectTasks);
        return true;
    }

    /**
     * 步骤未启动，AgentTask数据还未在DB初始化，构造初始任务结果
     *
     * @param stepInstance                步骤实例
     * @param queryExecuteCount           执行次数
     * @param batch                       滚动批次
     * @param maxAgentTasksForResultGroup 任务分组下最大返回的AgentTask数量
     * @param fuzzySearchIp               模糊查询IP
     * @return 步骤执行结果
     */
    private StepExecutionDetailDTO buildNotStartStepExecutionResult(StepInstanceBaseDTO stepInstance,
                                                                    Integer queryExecuteCount,
                                                                    Integer batch,
                                                                    Integer maxAgentTasksForResultGroup,
                                                                    String fuzzySearchIp) {

        StepExecutionDetailDTO stepExecuteDetail = new StepExecutionDetailDTO(stepInstance);
        stepExecuteDetail.setExecuteCount(queryExecuteCount);

        List<ResultGroupDTO> resultGroups = new ArrayList<>();
        ResultGroupDTO resultGroup = new ResultGroupDTO();
        resultGroup.setStatus(ExecuteObjectTaskStatusEnum.WAITING.getValue());
        resultGroup.setTag(null);

        List<ExecuteObject> targetExecuteObjects = filterTargetExecuteObjectsByBatch(stepInstance, batch);

        List<ExecuteObjectTask> executeObjectTasks = new ArrayList<>();
        // 如果需要根据IP过滤，那么需要重新计算执行对象任务总数
        boolean fuzzyFilterByIp = StringUtils.isNotEmpty(fuzzySearchIp);
        int executeObjectTaskSize = targetExecuteObjects.size();
        if (fuzzyFilterByIp) {
            executeObjectTaskSize = (int) targetExecuteObjects.stream()
                .filter(executeObject -> isMatchByIp(executeObject, fuzzySearchIp)).count();
        }
        resultGroup.setTotal(executeObjectTaskSize);

        if (CollectionUtils.isNotEmpty(targetExecuteObjects)) {
            int maxExecuteObjectTasks = (maxAgentTasksForResultGroup != null ?
                Math.min(maxAgentTasksForResultGroup, targetExecuteObjects.size()) : targetExecuteObjects.size());
            for (ExecuteObject targetExecuteObject : targetExecuteObjects) {
                if (fuzzyFilterByIp && !isMatchByIp(targetExecuteObject, fuzzySearchIp)) {
                    // 如果需要根据IP过滤，那么过滤掉不匹配的任务
                    continue;
                }
                if (maxExecuteObjectTasks-- > 0) {
                    ExecuteObjectTask executeObjectTask = new ExecuteObjectTask();
                    executeObjectTask.setExecuteObject(targetExecuteObject);
                    executeObjectTask.setExecuteObjectId(targetExecuteObject.getId());
                    executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.WAITING);
                    executeObjectTask.setTag(null);
                    executeObjectTask.setErrorCode(0);
                    executeObjectTask.setExitCode(0);
                    executeObjectTask.setTotalTime(0L);
                    executeObjectTasks.add(executeObjectTask);
                }
            }
        }
        resultGroup.setExecuteObjectTasks(executeObjectTasks);
        resultGroups.add(resultGroup);
        stepExecuteDetail.setResultGroups(resultGroups);

        if (stepInstance.isRollingStep()) {
            setRollingInfoForStep(stepInstance, stepExecuteDetail);
        } else {
            stepExecuteDetail.setRunMode(StepRunModeEnum.RUN_ALL);
        }

        return stepExecuteDetail;
    }

    private List<ExecuteObject> filterTargetExecuteObjectsByBatch(StepInstanceBaseDTO stepInstance, Integer batch) {
        List<ExecuteObject> executeObjects;
        if (stepInstance.isRollingStep()) {
            executeObjects = rollingConfigService.getRollingServers(stepInstance, batch);
        } else {
            executeObjects = stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly();
        }

        return executeObjects;
    }

    @Override
    public StepExecutionDetailDTO getStepExecutionResult(String username, Long appId,
                                                         StepExecutionResultQuery query) throws ServiceException {
        StopWatch watch = new StopWatch("getStepExecutionResult");
        try {
            Long stepInstanceId = query.getStepInstanceId();

            watch.start("getAndCheckStepInstance");
            StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(appId, stepInstanceId);
            preProcessViewStepExecutionResult(username, appId, stepInstance);

            int queryExecuteCount = query.getExecuteCount() == null ? stepInstance.getExecuteCount() :
                query.getExecuteCount();
            query.setExecuteCount(queryExecuteCount);
            if (stepInstance.isRollingStep() && query.isFilterByLatestBatch()) {
                query.setBatch(stepInstance.getBatch());
            }
            watch.stop();

            if (stepInstance.getStatus() == RunStatusEnum.BLANK) {
                // 步骤未启动，AgentTask数据还未在DB初始化，构造初始任务结果
                return buildNotStartStepExecutionResult(stepInstance, queryExecuteCount, query.getBatch(),
                    query.getMaxTasksForResultGroup(), query.getSearchIp());
            }

            StepExecutionDetailDTO stepExecutionDetail;
            // 如果步骤的目标服务器数量<100,或者通过IP匹配的方式过滤执行对象任务，为了提升性能，直接全量从DB查询数据，在内存进行处理
            if ((stepInstance.getTargetExecuteObjectCount() <= 100) || query.hasExecuteObjectFilterCondition()) {
                stepExecutionDetail = loadAllTasksFromDBAndBuildExecutionResultInMemory(watch, stepInstance, query);
            } else {
                stepExecutionDetail = filterAndSortExecutionResultInDB(watch, stepInstance, query);
            }

            if (stepInstance.isRollingStep()) {
                watch.start("setRollingTasksForStep");
                setRollingInfoForStep(stepInstance, stepExecutionDetail);
                watch.stop();
            } else {
                stepExecutionDetail.setRunMode(StepRunModeEnum.RUN_ALL);
            }

            if (stepInstance.isFileStep()) {
                watch.start("involveFileSourceTaskLog");
                FileSourceTaskLogDTO fileSourceTaskLog =
                    fileSourceTaskLogDAO.getFileSourceTaskLog(
                        stepInstance.getId(),
                        queryExecuteCount
                    );
                if (fileSourceTaskLog != null) {
                    involveFileSourceTaskLog(stepExecutionDetail, fileSourceTaskLog);
                }
                watch.stop();
            }

            return stepExecutionDetail;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000L) {
                log.info("Get step execution detail is slow, watch: {}", watch.prettyPrint());
            }
        }

    }

    private StepExecutionDetailDTO loadAllTasksFromDBAndBuildExecutionResultInMemory(StopWatch watch,
                                                                                     StepInstanceBaseDTO stepInstance,
                                                                                     StepExecutionResultQuery query) {
        try {
            StepExecutionDetailDTO executeDetail = new StepExecutionDetailDTO(stepInstance);
            executeDetail.setExecuteCount(query.getExecuteCount());

            if (query.hasExecuteObjectFilterCondition()) {
                watch.start("getMatchExecuteObjectCompositeKeys");
                Set<ExecuteObjectCompositeKey> matchKeys = getMatchExecuteObjectCompositeKeys(stepInstance, query);
                if (CollectionUtils.isEmpty(matchKeys)) {
                    watch.stop();
                    executeDetail.setResultGroups(buildEmptyResultGroups(stepInstance, query.getBatch()));
                    return executeDetail;
                } else {
                    query.setMatchExecuteObjectCompositeKeys(matchKeys);
                }
                watch.stop();
            }

            watch.start("loadAllTasksFromDbAndGroup");
            List<ResultGroupDTO> resultGroups = listAndGroupExecuteObjectTasks(stepInstance,
                query.getExecuteCount(), query.getBatch());

            if (CollectionUtils.isNotEmpty(query.getMatchExecuteObjectCompositeKeys())) {
                filterExecuteObjectTasksByMatchKeys(resultGroups, query.getMatchExecuteObjectCompositeKeys());
            }

            if (!query.isFetchAllGroupData()) {
                removeAgentTasksForNotSpecifiedResultGroup(resultGroups, query.getStatus(), query.getTag());
            }
            watch.stop();


            watch.start("sortAndLimitTasks");
            sortExecuteObjectTasksAndLimitSize(resultGroups, query);
            watch.stop();

            executeDetail.setResultGroups(resultGroups);

            return executeDetail;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
        }

    }

    private List<ResultGroupDTO> listAndGroupExecuteObjectTasks(StepInstanceBaseDTO stepInstance,
                                                                int executeCount,
                                                                Integer batch) {
        List<ResultGroupDTO> resultGroups = null;
        if (stepInstance.isScriptStep()) {
            resultGroups = scriptExecuteObjectTaskService.listAndGroupTasks(stepInstance, executeCount, batch);
        } else if (stepInstance.isFileStep()) {
            resultGroups = fileExecuteObjectTaskService.listAndGroupTasks(stepInstance, executeCount, batch);
        }
        return resultGroups;
    }

    private List<ResultGroupBaseDTO> listResultGroups(StepInstanceBaseDTO stepInstance,
                                                      int executeCount,
                                                      Integer batch) {
        List<ResultGroupBaseDTO> resultGroups = null;
        if (stepInstance.isScriptStep()) {
            resultGroups = scriptExecuteObjectTaskService.listResultGroups(stepInstance, executeCount, batch);
        } else if (stepInstance.isFileStep()) {
            resultGroups = fileExecuteObjectTaskService.listResultGroups(stepInstance, executeCount, batch);
        }
        return resultGroups;
    }


    private void sortExecuteObjectTasksAndLimitSize(List<ResultGroupDTO> resultGroups,
                                                    StepExecutionResultQuery query) {
        resultGroups.stream()
            .filter(resultGroup -> CollectionUtils.isNotEmpty(resultGroup.getExecuteObjectTasks()))
            .forEach(resultGroup -> {
                // 排序
                if (StringUtils.isNotEmpty(query.getOrderField())) {
                    List<ExecuteObjectTask> executeObjectTasks = resultGroup.getExecuteObjectTasks();
                    if (StepExecutionResultQuery.ORDER_FIELD_TOTAL_TIME.equals(query.getOrderField())) {
                        executeObjectTasks.sort(Comparator.comparingLong(task -> task.getTotalTime() == null ? 0L :
                            task.getTotalTime()));
                        if (query.getOrder() == DESCENDING) {
                            Collections.reverse(executeObjectTasks);
                        }
                    } else if (StepExecutionResultQuery.ORDER_FIELD_EXIT_CODE.equals(query.getOrderField())) {
                        executeObjectTasks.sort((o1, o2) -> {
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
                            Collections.reverse(executeObjectTasks);
                        }
                    }
                }

                // 截断
                if (query.getMaxTasksForResultGroup() != null) {
                    resultGroup.setExecuteObjectTasks(
                        getLimitedSizedList(resultGroup.getExecuteObjectTasks(),
                            query.getMaxTasksForResultGroup()));
                }
            });
    }


    private Set<ExecuteObjectCompositeKey> getMatchExecuteObjectCompositeKeys(StepInstanceBaseDTO stepInstance,
                                                                              StepExecutionResultQuery query) {
        List<ExecuteObjectCompositeKey> matchKeysByLogKeywordSearch = null;
        if (StringUtils.isNotBlank(query.getLogKeyword())) {
            matchKeysByLogKeywordSearch = getExecuteObjectCompositeKeysByLogContentKeyword(stepInstance,
                query.getExecuteCount(), query.getBatch(), query.getLogKeyword());
        }
        List<ExecuteObjectCompositeKey> matchKeysByIpSearch = null;
        if (StringUtils.isNotBlank(query.getSearchIp())) {
            matchKeysByIpSearch = fuzzySearchHostsByIp(stepInstance, query.getSearchIp());
        }

        if (matchKeysByLogKeywordSearch != null && matchKeysByIpSearch != null) {
            return new HashSet<>(CollectionUtils.intersection(matchKeysByLogKeywordSearch, matchKeysByIpSearch));
        } else if (matchKeysByLogKeywordSearch != null) {
            return new HashSet<>(matchKeysByLogKeywordSearch);
        } else if (matchKeysByIpSearch != null) {
            return new HashSet<>(matchKeysByIpSearch);
        } else {
            return Collections.emptySet();
        }
    }

    private List<ResultGroupDTO> buildEmptyResultGroups(StepInstanceBaseDTO stepInstance,
                                                        Integer batch) {
        List<ResultGroupBaseDTO> resultGroups = listResultGroups(stepInstance,
            stepInstance.getExecuteCount(), batch);
        return resultGroups.stream().map(resultGroupBase -> {
            ResultGroupDTO resultGroup = new ResultGroupDTO(resultGroupBase);
            resultGroup.setTotal(0);
            return resultGroup;
        }).collect(Collectors.toList());
    }

    private void filterExecuteObjectTasksByMatchKeys(List<ResultGroupDTO> resultGroups,
                                                     Set<ExecuteObjectCompositeKey> matchExecuteObjectCompositeKeys) {
        ExecuteObjectCompositeKey.CompositeKeyType compositeKeyType =
            getCompositeKeyType(matchExecuteObjectCompositeKeys);
        for (ResultGroupDTO resultGroup : resultGroups) {
            List<ExecuteObjectTask> executeObjectTasks = resultGroup.getExecuteObjectTasks();
            if (CollectionUtils.isNotEmpty(executeObjectTasks)) {
                executeObjectTasks = executeObjectTasks.stream()
                    .filter(executeObjectTask -> isExecuteObjectMatch(executeObjectTask.getExecuteObject(),
                        matchExecuteObjectCompositeKeys, compositeKeyType))
                    .collect(Collectors.toList());
                resultGroup.setExecuteObjectTasks(executeObjectTasks);
                resultGroup.setTotal(executeObjectTasks.size());
            }
        }
    }

    private ExecuteObjectCompositeKey.CompositeKeyType getCompositeKeyType(
        Set<ExecuteObjectCompositeKey> matchExecuteObjectCompositeKeys) {
        return Objects.requireNonNull(matchExecuteObjectCompositeKeys.stream().findFirst().orElse(null))
            .getCompositeKeyType();
    }

    private boolean isExecuteObjectMatch(ExecuteObject executeObject,
                                         Set<ExecuteObjectCompositeKey> matchExecuteObjectCompositeKeys,
                                         ExecuteObjectCompositeKey.CompositeKeyType compositeKeyType) {
        return matchExecuteObjectCompositeKeys.contains(executeObject.toExecuteObjectCompositeKey(compositeKeyType));
    }

    private void removeAgentTasksForNotSpecifiedResultGroup(List<ResultGroupDTO> resultGroups, Integer status,
                                                            String tag) {
        if (status != null && resultGroups.contains(new ResultGroupDTO(status, tag))) {
            resultGroups.forEach(resultGroup -> {
                if (!isMatchResultGroup(resultGroup, status, tag)) {
                    resultGroup.setExecuteObjectTasks(Collections.emptyList());
                }
            });
        } else {
            int i = 0;
            for (ResultGroupDTO resultGroup : resultGroups) {
                i++;
                if (i != 1) {
                    resultGroup.setExecuteObjectTasks(Collections.emptyList());
                }
            }
        }
    }

    private StepExecutionDetailDTO filterAndSortExecutionResultInDB(StopWatch watch,
                                                                    StepInstanceBaseDTO stepInstance,
                                                                    StepExecutionResultQuery query) {
        query.transformOrderFieldToDbField();
        int queryExecuteCount = query.getExecuteCount();
        Integer status = query.getStatus();
        String tag = query.getTag();

        StepExecutionDetailDTO executeDetail = new StepExecutionDetailDTO(stepInstance);
        executeDetail.setExecuteCount(queryExecuteCount);

        watch.start("getBaseResultGroups");
        List<ResultGroupBaseDTO> baseResultGroups = listBaseResultGroups(stepInstance,
            queryExecuteCount, query.getBatch());
        watch.stop();

        watch.start("setExecuteObjectTasks");
        List<ResultGroupDTO> resultGroups = baseResultGroups.stream()
            .map(ResultGroupDTO::new)
            .collect(Collectors.toList());
        if (status != null) {
            List<ExecuteObjectTask> tasks = listExecuteObjectTaskByResultGroup(stepInstance, queryExecuteCount,
                query.getBatch(), status, tag, query.getMaxTasksForResultGroup(), query.getOrderField(),
                query.getOrder());
            if (CollectionUtils.isNotEmpty(tasks)) {
                setExecuteObjectTasksForSpecifiedResultType(resultGroups, status, tag, tasks);
            } else {
                setAgentTasksForAnyResultType(resultGroups, stepInstance, queryExecuteCount,
                    query.getBatch(), query.getMaxTasksForResultGroup(), query.isFetchAllGroupData());
            }
        } else {
            setAgentTasksForAnyResultType(resultGroups, stepInstance, queryExecuteCount,
                query.getBatch(), query.getMaxTasksForResultGroup(), query.isFetchAllGroupData());
        }
        watch.stop();


        executeDetail.setResultGroups(resultGroups);
        return executeDetail;
    }

    private List<ResultGroupBaseDTO> listBaseResultGroups(StepInstanceBaseDTO stepInstance,
                                                          int executeCount,
                                                          Integer batch) {
        List<ResultGroupBaseDTO> resultGroups = null;
        if (stepInstance.isScriptStep()) {
            resultGroups = scriptExecuteObjectTaskService.listResultGroups(stepInstance, executeCount, batch);
        } else if (stepInstance.isFileStep()) {
            resultGroups = fileExecuteObjectTaskService.listResultGroups(stepInstance, executeCount, batch);
        }
        return resultGroups;
    }

    private List<ExecuteObjectTask> listExecuteObjectTaskByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                       int queryExecuteCount,
                                                                       Integer batch,
                                                                       Integer status,
                                                                       String tag,
                                                                       Integer maxAgentTasksForResultGroup,
                                                                       String orderField,
                                                                       Order order) {
        List<ExecuteObjectTask> tasks = null;
        if (stepInstance.isScriptStep()) {
            tasks = scriptExecuteObjectTaskService.listTaskByResultGroup(stepInstance, queryExecuteCount,
                batch, status, tag, maxAgentTasksForResultGroup, orderField, order);
        } else if (stepInstance.isFileStep()) {
            tasks = fileExecuteObjectTaskService.listTaskByResultGroup(stepInstance, queryExecuteCount,
                batch, status, tag, maxAgentTasksForResultGroup, orderField, order);
        }
        return tasks;
    }

    private List<ExecuteObjectTask> listExecuteObjectTaskByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                       int queryExecuteCount,
                                                                       Integer batch,
                                                                       Integer status,
                                                                       String tag) {
        List<ExecuteObjectTask> tasks = null;
        if (stepInstance.isScriptStep()) {
            tasks = scriptExecuteObjectTaskService.listTaskByResultGroup(stepInstance, queryExecuteCount,
                batch, status, tag);
        } else if (stepInstance.isFileStep()) {
            tasks = fileExecuteObjectTaskService.listTaskByResultGroup(stepInstance, queryExecuteCount,
                batch, status, tag);
        }
        return tasks;
    }

    private void setRollingInfoForStep(StepInstanceBaseDTO stepInstance,
                                       StepExecutionDetailDTO stepExecutionDetail) {
        RollingConfigDTO rollingConfig =
            rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());

        Map<Integer, StepInstanceRollingTaskDTO> latestStepInstanceRollingTasks =
            stepInstanceRollingTaskService.listLatestRollingTasks(
                stepExecutionDetail.getStepInstanceId(),
                stepExecutionDetail.getExecuteCount())
                .stream()
                .collect(Collectors.toMap(StepInstanceRollingTaskDTO::getBatch,
                    stepInstanceRollingTask -> stepInstanceRollingTask, (oldValue, newValue) -> newValue));

        // 如果滚动任务还未调度，那么需要在结果中补充
        int totalBatch = rollingConfig.getConfigDetail().getTotalBatch();
        List<StepInstanceRollingTaskDTO> stepInstanceRollingTasks = new ArrayList<>();
        for (int batch = 1; batch <= totalBatch; batch++) {
            StepInstanceRollingTaskDTO stepInstanceRollingTask = latestStepInstanceRollingTasks.get(batch);
            if (stepInstanceRollingTask == null) {
                stepInstanceRollingTask = new StepInstanceRollingTaskDTO();
                stepInstanceRollingTask.setStepInstanceId(stepExecutionDetail.getStepInstanceId());
                stepInstanceRollingTask.setExecuteCount(stepInstance.getExecuteCount());
                stepInstanceRollingTask.setBatch(batch);
                stepInstanceRollingTask.setStatus(RunStatusEnum.BLANK);
                if (RunStatusEnum.WAITING_USER == stepInstance.getStatus()
                    && stepInstance.getBatch() + 1 == batch) {
                    // 如果当前步骤状态为"等待用户"，那么需要设置下一批次的滚动任务状态为WAITING_USER
                    stepInstanceRollingTask.setStatus(RunStatusEnum.WAITING_USER);
                } else {
                    stepInstanceRollingTask.setStatus(RunStatusEnum.BLANK);
                }
            }
            stepInstanceRollingTasks.add(stepInstanceRollingTask);
        }

        stepExecutionDetail.setRollingTasks(stepInstanceRollingTasks);
        stepExecutionDetail.setLatestBatch(stepInstance.getBatch());
        stepExecutionDetail.setRunMode(rollingConfig.isBatchRollingStep(stepInstance.getId()) ?
            StepRunModeEnum.ROLLING_IN_BATCH : StepRunModeEnum.ROLLING_ALL);
    }

    private List<ExecuteObjectCompositeKey> getExecuteObjectCompositeKeysByLogContentKeyword(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        String keyword
    ) {
        return logService.getExecuteObjectsCompositeKeysByContentKeyword(stepInstance, executeCount, batch, keyword);
    }

    private List<ExecuteObjectCompositeKey> fuzzySearchHostsByIp(StepInstanceBaseDTO stepInstance, String searchIp) {
        List<ExecuteObject> matchExecuteObjects =
            stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly().stream()
                .filter(executeObject -> isMatchByIp(executeObject, searchIp))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(matchExecuteObjects)) {
            return null;
        }
        if (stepInstance.isSupportExecuteObjectFeature()) {
            return matchExecuteObjects.stream()
                .map(executeObject -> ExecuteObjectCompositeKey.ofExecuteObjectId(executeObject.getId()))
                .collect(Collectors.toList());
        } else {
            return matchExecuteObjects.stream()
                .map(executeObject -> ExecuteObjectCompositeKey.ofHostId(executeObject.getResourceId()))
                .collect(Collectors.toList());
        }
    }

    private boolean isMatchByIp(ExecuteObject executeObject, String searchIp) {
        boolean isMatch = false;
        if (!executeObject.isHostExecuteObject()) {
            return false;
        }
        HostDTO host = executeObject.getHost();
        if (StringUtils.isNotBlank(host.getIp())) {
            isMatch = host.getIp().contains(searchIp);
        }
        if (!isMatch && StringUtils.isNotBlank(host.getIpv6())) {
            isMatch = host.getIpv6().contains(searchIp);
        }
        return isMatch;
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
                if (taskInstance.getStatus() != RunStatusEnum.RUNNING
                    && taskInstance.getStatus() != RunStatusEnum.STOPPING) {
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
            cronTaskExecuteResult.setPlanId(taskInstance.getPlanId());
            cronTaskExecuteResult.setStatus(taskInstance.getStatus().getValue());
            cronTaskExecuteResult.setExecuteTime(taskInstance.getCreateTime());
            return cronTaskExecuteResult;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ExecuteObject> getExecuteObjectsByResultType(String username,
                                                             Long appId,
                                                             Long stepInstanceId,
                                                             Integer executeCount,
                                                             Integer batch,
                                                             Integer resultType,
                                                             String tag,
                                                             String keyword) {
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(appId, stepInstanceId);
        preProcessViewStepExecutionResult(username, appId, stepInstance);

        StepExecutionResultQuery query = StepExecutionResultQuery.builder()
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .batch(batch)
            .logKeyword(keyword)
            .build();

        Set<ExecuteObjectCompositeKey> matchExecuteObjectCompositeKeys = null;
        boolean filterByKeyword = StringUtils.isNotEmpty(keyword);
        if (filterByKeyword) {
            matchExecuteObjectCompositeKeys = getMatchExecuteObjectCompositeKeys(stepInstance, query);
            if (CollectionUtils.isEmpty(matchExecuteObjectCompositeKeys)) {
                return Collections.emptyList();
            }
        }

        if (stepInstance.getStatus() == RunStatusEnum.BLANK) {
            // 步骤未启动，ExecuteObjectTask数据还未在DB初始化，构造初始任务结果
            return filterTargetExecuteObjectsByBatch(stepInstance, query.getBatch());
        }

        List<ExecuteObjectTask> executeObjectTaskGroupByResultType = listExecuteObjectTaskByResultGroup(stepInstance,
            executeCount, batch, resultType, tag);
        if (CollectionUtils.isEmpty(executeObjectTaskGroupByResultType)) {
            return Collections.emptyList();
        }
        List<ExecuteObject> executeObjects = executeObjectTaskGroupByResultType.stream()
            .map(ExecuteObjectTask::getExecuteObject)
            .collect(Collectors.toList());
        if (filterByKeyword && CollectionUtils.isNotEmpty(matchExecuteObjectCompositeKeys)) {
            ExecuteObjectCompositeKey.CompositeKeyType compositeKeyType =
                getCompositeKeyType(matchExecuteObjectCompositeKeys);
            List<ExecuteObject> finalExecuteObject = new ArrayList<>();
            for (ExecuteObject executeObject : executeObjects) {
                if (isExecuteObjectMatch(executeObject, matchExecuteObjectCompositeKeys, compositeKeyType)) {
                    finalExecuteObject.add(executeObject);
                }
            }
            return finalExecuteObject;
        } else {
            return executeObjects;
        }
    }


    @Override
    public List<StepExecutionRecordDTO> listStepExecutionHistory(String username,
                                                                 Long appId,
                                                                 Long stepInstanceId,
                                                                 Integer batch) {
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(appId, stepInstanceId);
        preProcessViewStepExecutionResult(username, appId, stepInstance);

        // 步骤没有重试执行过
        if (stepInstance.getExecuteCount() == 0) {
            StepExecutionRecordDTO record = new StepExecutionRecordDTO();
            record.setStepInstanceId(stepInstanceId);
            record.setRetryCount(0);
            record.setCreateTime(stepInstance.getCreateTime());
            return Collections.singletonList(record);
        }

        List<StepExecutionRecordDTO> records;
        if (batch == null || batch == 0) {
            // 获取步骤维度的重试记录
            records = queryStepRetryRecords(stepInstance);
        } else {
            // 获取滚动任务维度的重试记录
            records = queryStepRollingTaskRetryRecords(stepInstanceId, batch);
        }

        records.sort(Comparator.comparingInt(StepExecutionRecordDTO::getRetryCount).reversed());

        return records;
    }

    private List<StepExecutionRecordDTO> queryStepRetryRecords(StepInstanceBaseDTO stepInstance) {
        Long stepInstanceId = stepInstance.getId();
        // 步骤最新的重试次数
        int latestExecuteCount = stepInstance.getExecuteCount();
        List<OperationLogDTO> operationLogs =
            operationLogService.listOperationLog(stepInstance.getTaskInstanceId());


        List<StepExecutionRecordDTO> records = new ArrayList<>();
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

    private List<StepExecutionRecordDTO> queryStepRollingTaskRetryRecords(long stepInstanceId, int batch) {
        List<StepExecutionRecordDTO> records = new ArrayList<>();
        List<StepInstanceRollingTaskDTO> rollingTasks =
            stepInstanceRollingTaskService.listRollingTasksByBatch(stepInstanceId, batch);
        rollingTasks.forEach(rollingTask -> {
            StepExecutionRecordDTO record = new StepExecutionRecordDTO();
            record.setStepInstanceId(stepInstanceId);
            record.setRetryCount(rollingTask.getExecuteCount());
            record.setCreateTime(rollingTask.getStartTime());
            records.add(record);
        });
        return records;
    }
}
