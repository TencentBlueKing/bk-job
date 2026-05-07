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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.plan.TaskPlanSyncService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID;
import static com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME;

@Slf4j
@Service
public class TaskPlanSyncServiceImpl implements TaskPlanSyncService {

    private final TaskPlanDAO taskPlanDAO;
    private final TaskTemplateService taskTemplateService;
    private final AbstractTaskStepService taskPlanStepService;
    private final AbstractTaskVariableService taskPlanVariableService;

    @Autowired
    public TaskPlanSyncServiceImpl(
        TaskPlanDAO taskPlanDAO,
        TaskTemplateService taskTemplateService,
        @Qualifier("TaskPlanStepServiceImpl") AbstractTaskStepService taskPlanStepService,
        @Qualifier("TaskPlanVariableServiceImpl") AbstractTaskVariableService taskPlanVariableService
    ) {
        this.taskPlanDAO = taskPlanDAO;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanStepService = taskPlanStepService;
        this.taskPlanVariableService = taskPlanVariableService;
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @ActionAuditRecord(
        actionId = ActionId.SYNC_JOB_PLAN,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PLAN,
            instanceIds = "#planId"
        ),
        content = "Sync plan [{{" + INSTANCE_NAME + "}}]({{" + INSTANCE_ID + "}})"
    )
    public Boolean sync(Long appId, Long templateId, Long planId, String templateVersion) {
        // 获取作业
        TaskTemplateInfoDTO taskTemplate = getTemplateForSync(appId, templateId, templateVersion);
        if (taskTemplate == null) {
            return false;
        }

        // 获取执行方案
        TaskPlanInfoDTO taskPlan = getPlanForSync(appId, templateId, planId);
        if (taskPlan == null) {
            return false;
        }
        log.info("BeforeSync: taskPlan={}", taskPlan);

        // 用模板数据覆盖执行方案数据
        fillPlanBasicInfoForSync(taskPlan, taskTemplate);

        // 构建执行方案步骤
        buildPlanStepsFromTemplate(taskPlan, taskTemplate);

        // 构建执行方案全局变量
        buildPlanVariablesFromTemplate(taskPlan, taskTemplate);

        // 同步
        syncPlan(taskPlan);
        log.info("AfterSync: taskPlan={}", taskPlan);

        // 审计 - 实例名称
        ActionAuditContext.current().setInstanceName(taskPlan.getName());
        return true;
    }

    private TaskTemplateInfoDTO getTemplateForSync(Long appId, Long templateId, String templateVersion) {
        TaskTemplateInfoDTO taskTemplate = taskTemplateService.getTaskTemplateById(appId, templateId);
        if (taskTemplate == null) {
            log.warn("taskTemplate is null, appId={}, templateId={}", appId, templateId);
            return null;
        }
        if (!templateVersion.equals(taskTemplate.getVersion())) {
            log.warn(
                "templateVersion expired, templateVersion={}, newest version={}",
                templateVersion,
                taskTemplate.getVersion()
            );
            return null;
        }
        return taskTemplate;
    }

    private TaskPlanInfoDTO getPlanForSync(Long appId, Long templateId, Long planId) {
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getTaskPlanById(planId);
        if (taskPlan == null) {
            log.warn("taskPlan is null, appId={}, planId={}", appId, planId);
            return null;
        }
        if (!taskPlan.getTemplateId().equals(templateId)) {
            log.warn(
                "taskPlan(id={},templateId={}) not belong to template(id={})",
                taskPlan.getId(),
                taskPlan.getTemplateId(),
                templateId
            );
            return null;
        }
        return taskPlan;
    }

    /**
     * 用模板数据覆盖执行方案数据
     */
    private void fillPlanBasicInfoForSync(TaskPlanInfoDTO taskPlan, TaskTemplateInfoDTO taskTemplate) {
        taskPlan.setVersion(taskTemplate.getVersion());
        taskPlan.setLastModifyTime(DateUtils.currentTimeSeconds());
        taskPlan.setLastModifyUser(JobContextUtil.getUsername());
    }

    /**
     * 保留当前执行方案的启用状态，再基于模板步骤重建新的执行方案步骤。
     */
    private void buildPlanStepsFromTemplate(TaskPlanInfoDTO taskPlan, TaskTemplateInfoDTO taskTemplate) {
        // 记录执行方案中启用的步骤列表对应的模板步骤ID
        if (taskPlan.getStepList() != null) {
            taskPlan.setEnableStepList(new ArrayList<>());
            taskPlan.getStepList().forEach(taskStep -> {
                if (taskStep.getEnable() == 1) {
                    taskPlan.getEnableStepList().add(taskStep.getTemplateStepId());
                }
            });
        }
        // 通过模板的步骤列表数据构造执行方案步骤数据
        taskTemplate.getStepList().forEach(taskStep -> {
            taskStep.setTemplateStepId(taskStep.getId());
            taskStep.setId(null);
            taskStep.setTemplateId(null);
            taskStep.setScriptStepId(null);
            taskStep.setFileStepId(null);
            taskStep.setApprovalStepId(null);
            taskStep.setPlanId(taskPlan.getId());
            taskStep.setEnable(taskPlan.getEnableStepList().contains(taskStep.getId()) ? 1 : 0);
        });
        taskPlan.setStepList(taskTemplate.getStepList());
    }

    /**
     * 复用模板变量构造新的执行方案变量，并保留执行方案中未跟随模板的变量值
     */
    private void buildPlanVariablesFromTemplate(TaskPlanInfoDTO taskPlan, TaskTemplateInfoDTO taskTemplate) {
        Map<Long, TaskVariableDTO> originPlanVariableMap = buildOriginPlanVariableMap(taskPlan.getVariableList());
        if (CollectionUtils.isNotEmpty(taskTemplate.getVariableList())) {
            // 通过赋值将模板变量改造为新的执行方案变量后续设置到执行方案中
            taskTemplate.getVariableList().forEach(templateVariable -> {
                templateVariable.setPlanId(taskPlan.getId());
                if (originPlanVariableMap != null) {
                    TaskVariableDTO originPlanVariable = originPlanVariableMap.get(templateVariable.getId());
                    // 复用模板变量作为新的变量
                    extractFieldToNewVariable(originPlanVariable, templateVariable);
                    originPlanVariableMap.remove(templateVariable.getId());
                }
            });
            taskPlan.setVariableList(taskTemplate.getVariableList());
        } else {
            taskPlan.setVariableList(Collections.emptyList());
        }
    }

    /**
     * 同步执行方案
     */
    private void syncPlan(TaskPlanInfoDTO taskPlanInfo) {
        try {
            Long planId = taskPlanInfo.getId();
            // 更新执行方案基础信息
            if (!taskPlanDAO.updateTaskPlanById(taskPlanInfo)) {
                throw new InternalException(ErrorCode.UPDATE_TASK_PLAN_FAILED);
            }

            // 删除所有老的步骤
            List<TaskStepDTO> taskStepList = taskPlanStepService.listStepsByParentId(planId);
            if (CollectionUtils.isNotEmpty(taskStepList)) {
                taskStepList
                    .forEach(taskStep -> taskPlanStepService.deleteStepById(planId, taskStep.getId()));
            }

            // 保存新的步骤信息
            createPlanStepsForSync(taskPlanInfo);

            // 更新变量
            updatePlanVariablesForSync(taskPlanInfo);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unknown exception while sync plan!", e);
            throw new InternalException(ErrorCode.SYNC_TASK_PLAN_UNKNOWN_ERROR);
        }
    }

    /**
     * 把执行方案变量转成模板变量ID与执行方案变量的map
     */
    private Map<Long, TaskVariableDTO> buildOriginPlanVariableMap(List<TaskVariableDTO> originPlanVariables) {
        if (originPlanVariables == null) {
            return null;
        }
        Map<Long, TaskVariableDTO> originPlanVariableMap = new HashMap<>(originPlanVariables.size());
        originPlanVariables.forEach(planVariable -> {
            // 执行方案变量ID实际是执行方案变量对应的模板变量ID
            // 赋值处见：TaskPlanVariableDAOImpl.extract()
            originPlanVariableMap.put(planVariable.getId(), planVariable);
        });
        return originPlanVariableMap;
    }

    /**
     * 提取执行方案变量中已经被用户设置的字段值放入新的变量中。
     */
    private static void extractFieldToNewVariable(TaskVariableDTO originPlanVariable, TaskVariableDTO newVariable) {
        if (originPlanVariable != null) {
            newVariable.setId(originPlanVariable.getId());
            newVariable.setPlanId(originPlanVariable.getPlanId());
            newVariable.setTemplateId(null);
            newVariable.setFollowTemplate(originPlanVariable.getFollowTemplate());
            if (!Boolean.TRUE.equals(originPlanVariable.getFollowTemplate())) {
                newVariable.setDefaultValue(originPlanVariable.getDefaultValue());
            }
        }
    }

    private void createPlanStepsForSync(TaskPlanInfoDTO taskPlanInfo) {
        for (TaskStepDTO taskStep : taskPlanInfo.getStepList()) {
            taskStep.setPlanId(taskPlanInfo.getId());
            if (taskPlanInfo.getEnableStepList().contains(taskStep.getTemplateStepId())) {
                taskStep.setEnable(1);
            } else {
                taskStep.setEnable(0);
            }
            taskStep.setId(taskPlanStepService.insertStep(taskStep));
        }
    }

    /**
     * 更新同步执行方案中的变量
     */
    private void updatePlanVariablesForSync(TaskPlanInfoDTO taskPlanInfo) {
        Long planId = taskPlanInfo.getId();
        List<TaskVariableDTO> oldVariableList = taskPlanVariableService.listVariablesByParentId(planId);
        List<Long> oldVariableIdList;
        if (CollectionUtils.isNotEmpty(oldVariableList)) {
            oldVariableIdList = oldVariableList.stream().map(TaskVariableDTO::getId).collect(Collectors.toList());
        } else {
            oldVariableIdList = null;
        }

        if (CollectionUtils.isNotEmpty(oldVariableIdList)) {
            List<TaskVariableDTO> newVariable = new ArrayList<>();
            for (TaskVariableDTO taskVariable : taskPlanInfo.getVariableList()) {
                taskVariable.setPlanId(planId);
                if (oldVariableIdList.contains(taskVariable.getId())) {
                    taskPlanVariableService.updateVarByParentResourceIdAndTplVarId(taskVariable);
                    oldVariableIdList.remove(taskVariable.getId());
                } else {
                    newVariable.add(taskVariable);
                }
            }
            taskPlanVariableService.batchInsertVariable(newVariable);
        } else {
            taskPlanVariableService.batchInsertVariable(taskPlanInfo.getVariableList());
        }

        // 删除模板中已删除的变量
        if (CollectionUtils.isNotEmpty(oldVariableIdList)) {
            oldVariableIdList
                .forEach(variableId -> taskPlanVariableService.deleteVariableById(planId, variableId));
        }
    }
}
