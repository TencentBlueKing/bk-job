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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.helper.TaskDiffHelper;
import com.tencent.bk.job.manage.service.plan.TaskPlanVarFollowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskPlanVarFollowServiceImpl implements TaskPlanVarFollowService {

    private final TaskPlanDAO taskPlanDAO;
    private final TaskTemplateDAO taskTemplateDAO;
    private final AbstractTaskStepService taskTemplateStepService;
    private final AbstractTaskStepService taskPlanStepService;
    private final AbstractTaskVariableService taskTemplateVariableService;
    private final AbstractTaskVariableService taskPlanVariableService;
    private final TaskDiffHelper taskDiffHelper;

    @Autowired
    public TaskPlanVarFollowServiceImpl(
        TaskPlanDAO taskPlanDAO,
        TaskTemplateDAO taskTemplateDAO,
        @Qualifier("TaskTemplateStepServiceImpl") AbstractTaskStepService taskTemplateStepService,
        @Qualifier("TaskPlanStepServiceImpl") AbstractTaskStepService taskPlanStepService,
        @Qualifier("TaskTemplateVariableServiceImpl") AbstractTaskVariableService taskTemplateVariableService,
        @Qualifier("TaskPlanVariableServiceImpl") AbstractTaskVariableService taskPlanVariableService,
        TaskDiffHelper taskDiffHelper
    ) {
        this.taskPlanDAO = taskPlanDAO;
        this.taskTemplateDAO = taskTemplateDAO;
        this.taskTemplateStepService = taskTemplateStepService;
        this.taskPlanStepService = taskPlanStepService;
        this.taskTemplateVariableService = taskTemplateVariableService;
        this.taskPlanVariableService = taskPlanVariableService;
        this.taskDiffHelper = taskDiffHelper;
    }

    @Override
    public void updatePlanVersionIfNeeded(TaskPlanInfoDTO taskPlanInfo) {
        // 跟随作业模板的变量值如果有差异，生成新的执行方案version;
        // 如果没有差异，进一步判断执行方案的步骤和变量是否跟模板有差异，如果没有差异则重置执行方案version为模板version
        if (followVarChanged(taskPlanInfo)) {
            String newVersion = UUID.randomUUID().toString();
            log.info("Updating plan version, planId={}, newVersion={}", taskPlanInfo.getId(), newVersion);
            taskPlanDAO.batchUpdatePlanVersionByIds(
                Collections.singletonList(taskPlanInfo.getId()),
                newVersion,
                JobContextUtil.getUsername(),
                DateUtils.currentTimeSeconds()
            );
            return;
        }

        TaskPlanInfoDTO currentPlan = getCurrentPlan(taskPlanInfo.getId());
        if (currentPlan == null) {
            return;
        }
        TaskTemplateInfoDTO taskTemplate = getTemplate(taskPlanInfo.getTemplateId());
        if (taskTemplate == null) {
            return;
        }

        // plan.version已经等于template.version时，说明当前不存在差异
        if (Objects.equals(currentPlan.getVersion(), taskTemplate.getVersion())) {
            log.debug("Plan version already matches template, planId={}", taskPlanInfo.getId());
            return;
        }

        // 执行方案与作业模板没有差异，重置执行方案version为模板version
        if (!planHasTemplateDiff(currentPlan, taskTemplate)) {
            log.info("Reset plan version to template version, planId={}, templateVersion={}",
                taskPlanInfo.getId(), taskTemplate.getVersion());
            taskPlanDAO.batchUpdatePlanVersionByIds(
                Collections.singletonList(taskPlanInfo.getId()),
                taskTemplate.getVersion(),
                JobContextUtil.getUsername(),
                DateUtils.currentTimeSeconds()
            );
        }
    }

    /**
     * 跟随作业模板的变量，变量值是否有变更
     */
    private boolean followVarChanged(TaskPlanInfoDTO taskPlanInfo) {
        List<TaskVariableDTO> followTemplateVars = taskPlanVariableService.listFollowVarsByPlanId(taskPlanInfo.getId());
        if (CollectionUtils.isEmpty(followTemplateVars)) {
            return false;
        }

        log.info("Found follow template variables, planId={}, varSize={}",
            taskPlanInfo.getId(), followTemplateVars.size());

        Map<Long, TaskVariableDTO> templateVarMap = taskTemplateVariableService
            .listVariablesByParentId(taskPlanInfo.getTemplateId())
            .stream()
            .collect(Collectors.toMap(TaskVariableDTO::getId, Function.identity()));

        return followTemplateVars.stream().anyMatch(variable -> {
            TaskVariableDTO templateVar = templateVarMap.get(variable.getId());
            return templateVar != null && isVariableValueChanged(templateVar, variable);
        });
    }

    @Override
    public void updatePlanVersionIfVarValueChanged(TaskTemplateInfoDTO taskTemplateInfo, Boolean changed) {
        if (changed) {
            log.debug("The task template has changed, and there is no need to determine the default value of the task" +
                " plan variables. templateId: {}", taskTemplateInfo.getId());
            return;
        }

        List<TaskVariableDTO> templateVariableDTOList = taskTemplateInfo.getVariableList();
        if (CollectionUtils.isEmpty(templateVariableDTOList)) {
            return;
        }

        List<Long> templateVarIdList = templateVariableDTOList.stream()
            .map(TaskVariableDTO::getId)
            .collect(Collectors.toList());

        List<TaskVariableDTO> planVariableDTOList =
            taskPlanVariableService.listVariablesByTemplateVarId(templateVarIdList);
        Set<Long> planIds = findNeedModifiedPlanIds(templateVariableDTOList, planVariableDTOList);
        if (CollectionUtils.isEmpty(planIds)) {
            return;
        }

        String newVersion = UUID.randomUUID().toString();
        log.info("Update task template, found task plans need to update version, " +
                "templateId={}, planIds={}, newVersion={}", taskTemplateInfo.getId(), planIds, newVersion);
        taskPlanDAO.batchUpdatePlanVersionByIds(
            new ArrayList<>(planIds),
            newVersion,
            JobContextUtil.getUsername(),
            DateUtils.currentTimeSeconds()
        );
    }

    /**
     * 找出需要修改版本号的执行方案ID
     */
    private Set<Long> findNeedModifiedPlanIds(List<TaskVariableDTO> templateVariableDTOList,
                                              List<TaskVariableDTO> planVariableDTOList) {
        if (CollectionUtils.isEmpty(planVariableDTOList)) {
            return null;
        }

        Map<Long, TaskVariableDTO> templateVarMap = templateVariableDTOList.stream()
            .collect(Collectors.toMap(TaskVariableDTO::getId, Function.identity()));
        Set<Long> needModifiedPlanIds = new HashSet<>();
        for (TaskVariableDTO planVar : planVariableDTOList) {
            if (!Boolean.TRUE.equals(planVar.getFollowTemplate())) {
                continue;
            }

            TaskVariableDTO templateVar = templateVarMap.get(planVar.getId());
            if (templateVar == null) {
                continue;
            }

            if (templateVar.cipherNotChange()) {
                continue;
            }

            if (isVariableValueChanged(templateVar, planVar)) {
                needModifiedPlanIds.add(planVar.getPlanId());
            }
        }
        return needModifiedPlanIds;
    }

    /**
     * 判断执行方案的变量值跟作业是否一样
     */
    private boolean isVariableValueChanged(TaskVariableDTO templateVar, TaskVariableDTO planVar) {
        // 主机变量只需要比较hostId是否相同
        if (templateVar.getType() == planVar.getType()
            && TaskVariableTypeEnum.EXECUTE_OBJECT_LIST == templateVar.getType()) {
            return !sameHostId(templateVar.getDefaultValue(), planVar.getDefaultValue());
        }
        return !Objects.equals(templateVar.getDefaultValue(), planVar.getDefaultValue());
    }

    private boolean sameHostId(String templateValue, String planValue) {
        return extractHostIds(templateValue).equals(extractHostIds(planValue));
    }

    private Set<Long> extractHostIds(String targetValue) {
        TaskTargetDTO taskTarget = TaskTargetDTO.fromJsonString(targetValue);
        if (taskTarget == null
            || taskTarget.getHostNodeList() == null
            || CollectionUtils.isEmpty(taskTarget.getHostNodeList().getHostList())) {
            return Collections.emptySet();
        }
        return taskTarget.getHostNodeList().getHostList().stream()
            .map(host -> host == null ? null : host.getHostId())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private TaskTemplateInfoDTO getTemplate(Long templateId) {
        TaskTemplateInfoDTO taskTemplate = taskTemplateDAO.getTaskTemplateById(templateId);
        if (taskTemplate == null) {
            return null;
        }
        taskTemplate.setStepList(taskTemplateStepService.listStepsByParentId(templateId));
        taskTemplate.setVariableList(taskTemplateVariableService.listVariablesByParentId(templateId));
        return taskTemplate;
    }

    private TaskPlanInfoDTO getCurrentPlan(Long planId) {
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getTaskPlanById(planId);
        if (taskPlan == null) {
            return null;
        }
        taskPlan.setStepList(taskPlanStepService.listStepsByParentId(planId));
        taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(planId));
        return taskPlan;
    }

    /**
     * 当前执行方案与作业模板差异比较，变量不比较具体的值
     */
    private boolean planHasTemplateDiff(TaskPlanInfoDTO currentPlan, TaskTemplateInfoDTO taskTemplate) {
        boolean variableChanged = taskDiffHelper.variablesHasChanged(
            currentPlan.getVariableList(),
            taskTemplate.getVariableList(),
            TaskDiffHelper.CompareMode.PLAN_TO_TEMPLATE
        );
        if (variableChanged) {
            log.debug("Plan variable differs from template, planId={}, templateId={}",
                currentPlan.getId(), taskTemplate.getId());
            return true;
        }

        boolean stepChanged = taskDiffHelper.stepsHasChanged(
            currentPlan.getStepList(),
            taskTemplate.getStepList(),
            TaskDiffHelper.CompareMode.PLAN_TO_TEMPLATE
        );
        if (stepChanged) {
            log.debug("Plan step differs from template, planId={}, templateId={}",
                currentPlan.getId(), taskTemplate.getId());
            return true;
        }
        return false;
    }
}
