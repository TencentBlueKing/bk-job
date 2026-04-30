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

package com.tencent.bk.job.manage.service.plan.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.plan.TaskPlanVarUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskPlanVarUpdateServiceImpl implements TaskPlanVarUpdateService {

    private final TaskPlanDAO taskPlanDAO;
    private final AbstractTaskStepService taskPlanStepService;
    private final AbstractTaskVariableService taskPlanVariableService;

    @Autowired
    public TaskPlanVarUpdateServiceImpl(
        TaskPlanDAO taskPlanDAO,
        @Qualifier("TaskPlanStepServiceImpl") AbstractTaskStepService taskPlanStepService,
        @Qualifier("TaskPlanVariableServiceImpl") AbstractTaskVariableService taskPlanVariableService
    ) {
        this.taskPlanDAO = taskPlanDAO;
        this.taskPlanStepService = taskPlanStepService;
        this.taskPlanVariableService = taskPlanVariableService;
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @ActionAuditRecord(
        actionId = ActionId.EDIT_JOB_PLAN,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PLAN
        ),
        content = EventContentConstants.EDIT_JOB_PLAN
    )
    public boolean batchUpdatePlanVariable(List<TaskPlanInfoDTO> planInfoList) {
        if (CollectionUtils.isEmpty(planInfoList)) {
            return true;
        }
        for (TaskPlanInfoDTO planInfo : planInfoList) {
            try {
                TaskPlanInfoDTO originPlan = getTaskPlanById(planInfo.getId());
                if (originPlan == null) {
                    throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
                }

                List<TaskVariableDTO> updatableVariables = filterUpdatableVariables(originPlan, planInfo);
                if (CollectionUtils.isEmpty(updatableVariables)) {
                    continue;
                }

                if (taskPlanVariableService.batchUpdateVariableByName(updatableVariables)) {
                    if (taskPlanDAO.updateTaskPlanById(planInfo)) {
                        TaskPlanInfoDTO updatedPlan = getTaskPlanById(planInfo.getId());
                        // 添加审计
                        ActionAuditContext.current()
                            .addInstanceInfo(String.valueOf(planInfo.getId()), planInfo.getName(), originPlan,
                                updatedPlan);
                        continue;
                    } else {
                        log.error("Error while updating plan info after batch update variable value!|{}", planInfo);
                    }
                } else {
                    log.error("Error while batch update variable value!|{}", planInfo);
                }
                throw new InternalException(ErrorCode.BATCH_UPDATE_PLAN_VARIABLE_FAILED);
            } catch (Exception e) {
                String msg = MessageFormatter.format(
                    "Error while batch update plan variable value!|{}",
                    planInfo
                ).getMessage();
                log.error(msg, e);
                throw e;
            }
        }
        return true;
    }

    /**
     * 过滤出允许修改的执行方案变量；跟随模板的变量跳过并记录日志。
     */
    private List<TaskVariableDTO> filterUpdatableVariables(TaskPlanInfoDTO originPlan, TaskPlanInfoDTO planInfo) {
        Map<String, TaskVariableDTO> originPlanVariableMap = originPlan.getVariableList().stream()
            .collect(Collectors.toMap(
                TaskVariableDTO::getName,
                Function.identity(),
                (first, second) -> first)
            );
        List<TaskVariableDTO> updatableVariables = new ArrayList<>();
        List<String> skippedVariableNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(planInfo.getVariableList())) {
            for (TaskVariableDTO variable : planInfo.getVariableList()) {
                TaskVariableDTO originVariable = originPlanVariableMap.get(variable.getName());
                if (originVariable == null || !Boolean.TRUE.equals(originVariable.getFollowTemplate())) {
                    updatableVariables.add(variable);
                } else {
                    skippedVariableNames.add(variable.getName());
                }
            }
        }

        if (CollectionUtils.isNotEmpty(skippedVariableNames)) {
            log.warn("Skip update follow template plan variables, planId={}, templateId={}, variables={}",
                planInfo.getId(), planInfo.getTemplateId(), skippedVariableNames);
        }
        if (CollectionUtils.isEmpty(updatableVariables)) {
            log.info("Skip batch update plan variables because no variables can be updated, planId={}, templateId={}",
                planInfo.getId(), planInfo.getTemplateId());
        }
        return updatableVariables;
    }

    private TaskPlanInfoDTO getTaskPlanById(Long planId) {
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getTaskPlanById(planId);
        if (taskPlan == null) {
            return null;
        }
        List<TaskStepDTO> taskStepList = taskPlanStepService.listStepsByParentId(taskPlan.getId());
        taskPlan.setStepList(taskStepList);
        taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId()));
        return taskPlan;
    }
}
