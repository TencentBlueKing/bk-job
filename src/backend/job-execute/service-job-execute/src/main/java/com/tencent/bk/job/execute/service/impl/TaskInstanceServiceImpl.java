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

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.common.context.JobInstanceContext;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.manage.remote.RemoteAppService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class TaskInstanceServiceImpl implements TaskInstanceService {

    private final RemoteAppService remoteAppService;
    private final TaskInstanceDAO taskInstanceDAO;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final ExecuteAuthService executeAuthService;
    private final StepInstanceService stepInstanceService;

    private final IdGen idGen;

    @Autowired
    public TaskInstanceServiceImpl(RemoteAppService remoteAppService,
                                   TaskInstanceDAO taskInstanceDAO,
                                   TaskInstanceVariableService taskInstanceVariableService,
                                   ExecuteAuthService executeAuthService,
                                   StepInstanceService stepInstanceService,
                                   IdGen idGen) {
        this.remoteAppService = remoteAppService;
        this.stepInstanceService = stepInstanceService;
        this.taskInstanceDAO = taskInstanceDAO;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.executeAuthService = executeAuthService;
        this.idGen = idGen;
    }

    @Override
    public long addTaskInstance(TaskInstanceDTO taskInstance) {
        taskInstance.setId(idGen.genTaskInstanceId());
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
    public TaskInstanceDTO getTaskInstance(User user, long appId, long taskInstanceId)
        throws NotFoundException, PermissionDeniedException {

        TaskInstanceDTO taskInstance = getTaskInstance(taskInstanceId);
        checkTaskInstanceExist(appId, taskInstanceId, taskInstance);
        auditAndAuthViewTaskInstance(user, taskInstance);
        JobExecuteContext jobExecuteContext = JobExecuteContextThreadLocalRepo.get();
        if (jobExecuteContext != null) {
            JobInstanceContext jobInstanceContext = new JobInstanceContext();
            jobInstanceContext.setTaskInstanceId(taskInstanceId);
        }
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

    private void auditAndAuthViewTaskInstance(User user,
                                              TaskInstanceDTO taskInstance) {
        // 审计
        ActionAuditContext.current()
            .setInstanceId(String.valueOf(taskInstance.getId()))
            .setInstanceName(taskInstance.getName());

        // 鉴权
        executeAuthService.authViewTaskInstance(user, new AppResourceScope(taskInstance.getAppId()), taskInstance);
    }

    @Override
    public TaskInstanceDTO getTaskInstanceDetail(long taskInstanceId) {
        TaskInstanceDTO taskInstance = getTaskInstance(taskInstanceId);
        fillStepAndVariable(taskInstance);
        return taskInstance;
    }

    private void fillStepAndVariable(TaskInstanceDTO taskInstance) {
        taskInstance.setStepInstances(stepInstanceService.listStepInstanceByTaskInstanceId(taskInstance.getId()));
        taskInstance.setVariables(taskInstanceVariableService.getByTaskInstanceId(taskInstance.getId()));
    }

    @Override
    public TaskInstanceDTO getTaskInstanceDetail(User user, long appId, long taskInstanceId)
        throws NotFoundException, PermissionDeniedException {
        TaskInstanceDTO taskInstance = getTaskInstance(user, appId, taskInstanceId);
        fillStepAndVariable(taskInstance);
        return taskInstance;
    }

    @Override
    public void updateTaskStatus(long taskInstanceId, int status) {
        taskInstanceDAO.updateTaskStatus(taskInstanceId, status);
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
    public void resetTaskExecuteInfoForRetry(long taskInstanceId) {
        taskInstanceDAO.resetTaskExecuteInfoForRetry(taskInstanceId);
    }

    @Override
    public void updateTaskExecutionInfo(long taskInstanceId,
                                        RunStatusEnum status,
                                        Long currentStepId,
                                        Long startTime,
                                        Long endTime,
                                        Long totalTime) {
        taskInstanceDAO.updateTaskExecutionInfo(taskInstanceId, status, currentStepId, startTime, endTime, totalTime);
    }

    @Override
    public List<Long> getJoinedAppIdList(String tenantId) {
        // 加全量appId作为in条件查询以便走索引
        return taskInstanceDAO.listTaskInstanceAppId(
            remoteAppService.listAllAppIds(tenantId),
            null,
            null
        );
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
    public void saveTaskInstanceHosts(long appId,
                                      long taskInstanceId,
                                      Collection<HostDTO> hosts) {
        taskInstanceDAO.saveTaskInstanceHosts(appId, taskInstanceId, hosts);
    }
}
