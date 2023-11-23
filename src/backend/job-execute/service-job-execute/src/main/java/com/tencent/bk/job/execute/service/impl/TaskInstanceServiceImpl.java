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

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.converter.StepTypeExecuteTypeConverter;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SCRIPT;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SQL;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.MANUAL_CONFIRM;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.SEND_FILE;

@Slf4j
@Service
public class TaskInstanceServiceImpl implements TaskInstanceService {

    private final ApplicationService applicationService;
    private final StepInstanceDAO stepInstanceDAO;
    private final TaskInstanceDAO taskInstanceDAO;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final ExecuteAuthService executeAuthService;

    @Autowired
    public TaskInstanceServiceImpl(ApplicationService applicationService,
                                   StepInstanceDAO stepInstanceDAO,
                                   TaskInstanceDAO taskInstanceDAO,
                                   TaskInstanceVariableService taskInstanceVariableService,
                                   ExecuteAuthService executeAuthService) {
        this.applicationService = applicationService;
        this.stepInstanceDAO = stepInstanceDAO;
        this.taskInstanceDAO = taskInstanceDAO;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.executeAuthService = executeAuthService;
    }

    @Override
    public long addTaskInstance(TaskInstanceDTO taskInstance) {
        return taskInstanceDAO.addTaskInstance(taskInstance);
    }

    @Override
    public TaskInstanceDTO getTaskInstance(long appId, long taskInstanceId) throws NotFoundException {
        TaskInstanceDTO taskInstance = getTaskInstance(taskInstanceId);
        if (!taskInstance.getAppId().equals(appId)) {
            log.warn("Task instance is not in application, taskInstanceId={}, appId={}", taskInstanceId, appId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        return taskInstance;
    }

    @Override
    public TaskInstanceDTO getTaskInstance(long taskInstanceId) throws NotFoundException {
        TaskInstanceDTO taskInstance = taskInstanceDAO.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            log.warn("Task instance is not exist, taskInstanceId={}", taskInstanceId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        return taskInstance;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.VIEW_HISTORY,
        instance = @AuditInstanceRecord(
            instanceIds = "#taskInstanceId",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.VIEW_JOB_INSTANCE
    )
    public TaskInstanceDTO getTaskInstance(String username, long appId, long taskInstanceId)
        throws NotFoundException, PermissionDeniedException {

        TaskInstanceDTO taskInstance = getTaskInstance(taskInstanceId);
        checkTaskInstanceExist(appId, taskInstanceId, taskInstance);
        auditAndAuthViewTaskInstance(username, taskInstance);
        return taskInstance;
    }

    private void checkTaskInstanceExist(long appId, long taskInstanceId, TaskInstanceDTO taskInstance) {
        if (taskInstance == null) {
            log.warn("Task instance is not exist, taskInstanceId={}", taskInstanceId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        if (!taskInstance.getAppId().equals(appId)) {
            log.warn("Task instance is not in application, taskInstanceId={}, appId={}", taskInstanceId, appId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
    }

    private void auditAndAuthViewTaskInstance(String username,
                                              TaskInstanceDTO taskInstance) {
        // 审计
        ActionAuditContext.current()
            .setInstanceId(String.valueOf(taskInstance.getId()))
            .setInstanceName(taskInstance.getName());

        // 鉴权
        executeAuthService.authViewTaskInstance(username, new AppResourceScope(taskInstance.getAppId()), taskInstance);
    }

    @Override
    public long addStepInstance(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstanceDAO.addStepInstanceBase(stepInstance);
        if (stepInstanceId > 0) {
            stepInstance.setId(stepInstanceId);
            Integer stepType = stepInstance.getExecuteType();
            if (stepType.equals(EXECUTE_SQL.getValue()) || stepType.equals(EXECUTE_SCRIPT.getValue())) {
                stepInstanceDAO.addScriptStepInstance(stepInstance);
            } else if (stepType.equals(SEND_FILE.getValue())) {
                stepInstanceDAO.addFileStepInstance(stepInstance);
            } else if (stepType.equals(MANUAL_CONFIRM.getValue())) {
                stepInstanceDAO.addConfirmStepInstance(stepInstance);
            }
        }
        return stepInstanceId;
    }

    @Override
    public TaskInstanceDTO getTaskInstanceDetail(long taskInstanceId) {
        TaskInstanceDTO taskInstance = getTaskInstance(taskInstanceId);
        fillStepAndVariable(taskInstance);
        return taskInstance;
    }

    private void fillStepAndVariable(TaskInstanceDTO taskInstance) {
        List<StepInstanceBaseDTO> baseStepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstance.getId());
        if (CollectionUtils.isNotEmpty(baseStepInstanceList)) {
            List<StepInstanceDTO> stepInstanceList = new ArrayList<>();
            for (StepInstanceBaseDTO baseStepInstance : baseStepInstanceList) {
                StepInstanceDTO stepInstance = new StepInstanceDTO(baseStepInstance);
                fillStepInstanceDetail(stepInstance);
                stepInstanceList.add(stepInstance);
            }
            taskInstance.setStepInstances(stepInstanceList);
        }
        List<TaskVariableDTO> taskVariables = taskInstanceVariableService.getByTaskInstanceId(taskInstance.getId());
        taskInstance.setVariables(taskVariables);
    }

    private void fillStepInstanceDetail(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        Integer stepType = StepTypeExecuteTypeConverter.convertToStepType(stepInstance.getExecuteType());
        if (stepType.equals(TaskStepTypeEnum.SCRIPT.getValue())) {
            stepInstance.fillScriptStepInfo(stepInstanceDAO.getScriptStepInstance(stepInstanceId));
        } else if (stepType.equals(TaskStepTypeEnum.FILE.getValue())) {
            stepInstance.fillFileStepInfo(stepInstanceDAO.getFileStepInstance(stepInstanceId));
        } else if (stepType.equals(TaskStepTypeEnum.APPROVAL.getValue())) {
            stepInstance.fillConfirmStepInfo(stepInstanceDAO.getConfirmStepInstance(stepInstanceId));
        }
    }

    @Override
    public TaskInstanceDTO getTaskInstanceDetail(String username, long appId, long taskInstanceId)
        throws NotFoundException, PermissionDeniedException {
        TaskInstanceDTO taskInstance = getTaskInstance(username, appId, taskInstanceId);
        fillStepAndVariable(taskInstance);
        return taskInstance;
    }

    @Override
    public List<StepInstanceBaseDTO> listStepInstanceByTaskInstanceId(long taskInstanceId) {
        return stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstance(long stepInstanceId) {
        return stepInstanceDAO.getStepInstanceBase(stepInstanceId);
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstance(long appId, long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = getBaseStepInstance(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(long stepInstanceId) throws NotFoundException {
        StepInstanceBaseDTO stepInstanceBase = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        if (stepInstanceBase == null) {
            log.warn("StepInstance:{} not exist", stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(stepInstanceBase);
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(long appId, long stepInstanceId) throws NotFoundException {
        StepInstanceDTO stepInstance = getStepInstanceDetail(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceBaseDTO getFirstStepInstance(long taskInstanceId) {
        return stepInstanceDAO.getFirstStepInstanceBase(taskInstanceId);
    }

    @Override
    public void updateTaskStatus(long taskInstanceId, int status) {
        taskInstanceDAO.updateTaskStatus(taskInstanceId, status);
    }

    @Override
    public List<Long> getTaskStepIdList(long taskInstanceId) {
        return taskInstanceDAO.getTaskStepInstanceIdList(taskInstanceId);
    }

    @Override
    public void updateTaskCurrentStepId(long taskInstanceId, Long stepInstanceId) {
        taskInstanceDAO.updateTaskCurrentStepId(taskInstanceId, stepInstanceId);
    }

    @Override
    public void resetTaskStatus(long taskInstanceId) {
        taskInstanceDAO.resetTaskStatus(taskInstanceId);
    }

    @Override
    public void updateStepStatus(long stepInstanceId, int status) {
        stepInstanceDAO.updateStepStatus(stepInstanceId, status);
    }

    @Override
    public void resetStepExecuteInfoForRetry(long stepInstanceId) {
        stepInstanceDAO.resetStepExecuteInfoForRetry(stepInstanceId);
    }

    @Override
    public void resetTaskExecuteInfoForRetry(long taskInstanceId) {
        taskInstanceDAO.resetTaskExecuteInfoForRetry(taskInstanceId);
    }

    @Override
    public void resetStepStatus(long stepInstanceId) {
        stepInstanceDAO.resetStepStatus(stepInstanceId);
    }

    @Override
    public void updateStepStartTime(long stepInstanceId, Long startTime) {
        stepInstanceDAO.updateStepStartTime(stepInstanceId, startTime);
    }

    @Override
    public void updateStepStartTimeIfNull(long stepInstanceId, Long startTime) {
        stepInstanceDAO.updateStepStartTimeIfNull(stepInstanceId, startTime);
    }

    @Override
    public void updateStepEndTime(long stepInstanceId, Long endTime) {
        stepInstanceDAO.updateStepEndTime(stepInstanceId, endTime);
    }

    @Override
    public void addStepInstanceExecuteCount(long stepInstanceId) {
        stepInstanceDAO.addStepInstanceExecuteCount(stepInstanceId);
    }

    @Override
    public void updateStepTotalTime(long stepInstanceId, long totalTime) {
        stepInstanceDAO.updateStepTotalTime(stepInstanceId, totalTime);
    }

    @Override
    public void updateTaskExecutionInfo(long taskInstanceId, RunStatusEnum status, Long currentStepId, Long startTime
        , Long endTime, Long totalTime) {
        taskInstanceDAO.updateTaskExecutionInfo(taskInstanceId, status, currentStepId, startTime, endTime, totalTime);
    }

    @Override
    public void updateStepExecutionInfo(long stepInstanceId, RunStatusEnum status, Long startTime, Long endTime,
                                        Long totalTime) {
        stepInstanceDAO.updateStepExecutionInfo(stepInstanceId, status, startTime, endTime, totalTime);
    }

    @Override
    public void updateResolvedScriptParam(long stepInstanceId, boolean isSecureParam, String resolvedScriptParam) {
        stepInstanceDAO.updateResolvedScriptParam(stepInstanceId, isSecureParam, resolvedScriptParam);
    }

    @Override
    public void updateResolvedSourceFile(long stepInstanceId, List<FileSourceDTO> resolvedFileSources) {
        if (log.isDebugEnabled()) {
            log.debug("updateResolvedSourceFile={}", JsonUtils.toJson(resolvedFileSources));
        }
        stepInstanceDAO.updateResolvedSourceFile(stepInstanceId, resolvedFileSources);
    }

    @Override
    public void updateResolvedTargetPath(long stepInstanceId, String resolvedTargetPath) {
        stepInstanceDAO.updateResolvedTargetPath(stepInstanceId, resolvedTargetPath);
    }

    @Override
    public void updateConfirmReason(long stepInstanceId, String confirmReason) {
        stepInstanceDAO.updateConfirmReason(stepInstanceId, confirmReason);
    }

    @Override
    public void updateStepOperator(long stepInstanceId, String operator) {
        stepInstanceDAO.updateStepOperator(stepInstanceId, operator);
    }

    @Override
    public StepInstanceDTO getPreExecutableStepInstance(long taskInstanceId, long stepInstanceId) {
        StepInstanceBaseDTO preStepInstance = stepInstanceDAO.getPreExecutableStepInstance(taskInstanceId,
            stepInstanceId);
        if (preStepInstance == null) {
            return null;
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(preStepInstance);
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceByTaskInstanceId(long taskInstanceId) {
        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
        if (CollectionUtils.isEmpty(stepInstanceList)) {
            return null;
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(stepInstanceList.get(0));
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public List<Long> getJoinedAppIdList() {
        // 加全量appId作为in条件查询以便走索引
        return taskInstanceDAO.listTaskInstanceAppId(applicationService.listAllAppIds(), null, null);
    }

    @Override
    public boolean hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime) {
        return taskInstanceDAO.hasExecuteHistory(appId, cronTaskId, fromTime, toTime);
    }

    @Override
    public List<Long> listTaskInstanceId(Long appId, Long fromTime, Long toTime, int offset, int limit) {
        return taskInstanceDAO.listTaskInstanceId(appId, fromTime, toTime, offset, limit);
    }

    @Override
    public void saveTaskInstanceHosts(long taskInstanceId,
                                      Collection<HostDTO> hosts) {
        taskInstanceDAO.saveTaskInstanceHosts(taskInstanceId, hosts);
    }

    @Override
    public Long getTaskInstanceId(long appId, long stepInstanceId) {
        return stepInstanceDAO.getTaskInstanceId(appId, stepInstanceId);
    }
}
