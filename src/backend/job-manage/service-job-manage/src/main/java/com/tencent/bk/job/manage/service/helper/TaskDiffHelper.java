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

package com.tencent.bk.job.manage.service.helper;

import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 任务差异比较
 */
@Component
public class TaskDiffHelper {

    public enum CompareMode {
        // 作业模板与作业模板比较
        TEMPLATE_TO_TEMPLATE,
        // 作业方案与作业模板比较
        PLAN_TO_TEMPLATE
    }

    /**
     * 比较任务步骤是否有变更
     */
    public boolean stepsHasChanged(List<TaskStepDTO> currentTaskStepList,
                                   List<TaskStepDTO> originTaskStepList,
                                   CompareMode compareMode) {
        if (currentTaskStepList.size() != originTaskStepList.size()) {
            return true;
        }

        for (int i = 0; i < currentTaskStepList.size(); i++) {
            TaskStepDTO currentTaskStep = currentTaskStepList.get(i);
            TaskStepDTO originTaskStep = originTaskStepList.get(i);
            if (stepBaseInfoChanged(currentTaskStep, originTaskStep, compareMode)) {
                return true;
            }
            if (stepDetailChanged(currentTaskStep, originTaskStep, compareMode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 比较任务变量列表是否有变更，忽略变量值的差异
     */
    public boolean variablesHasChanged(List<TaskVariableDTO> currentTaskVariableList,
                                       List<TaskVariableDTO> originTaskVariableList,
                                       CompareMode compareMode) {
        if (currentTaskVariableList.size() != originTaskVariableList.size()) {
            return true;
        }

        for (int i = 0; i < currentTaskVariableList.size(); i++) {
            TaskVariableDTO currentTaskVariable = currentTaskVariableList.get(i);
            TaskVariableDTO originTaskVariable = originTaskVariableList.get(i);
            TaskVariableDTO comparableOriginTaskVariable = buildComparableOriginVariable(
                currentTaskVariable,
                originTaskVariable,
                compareMode
            );
            if (!currentTaskVariable.equals(comparableOriginTaskVariable)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 比较步骤的基础信息
     */
    private boolean stepBaseInfoChanged(TaskStepDTO currentTaskStep,
                                        TaskStepDTO originTaskStep,
                                        CompareMode compareMode) {
        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            if (currentTaskStep.getId() == null || currentTaskStep.getId() <= 0 || currentTaskStep.getDelete() == 1) {
                return true;
            }
            if (!currentTaskStep.getId().equals(originTaskStep.getId())) {
                return true;
            }
        } else if (!Objects.equals(currentTaskStep.getTemplateStepId(), originTaskStep.getId())) {
            return true;
        }

        if (!Objects.equals(currentTaskStep.getType(), originTaskStep.getType())) {
            return true;
        }
        return !Objects.equals(currentTaskStep.getName(), originTaskStep.getName());
    }

    private TaskVariableDTO buildComparableOriginVariable(TaskVariableDTO currentTaskVariable,
                                                          TaskVariableDTO originTaskVariable,
                                                          CompareMode compareMode) {
        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            originTaskVariable.setDefaultValue(currentTaskVariable.getDefaultValue());
            originTaskVariable.setTemplateId(currentTaskVariable.getTemplateId());
            originTaskVariable.setDelete(false);
            originTaskVariable.setFollowTemplate(currentTaskVariable.getFollowTemplate());
            return originTaskVariable;
        }

        // currentTaskVariable是执行方案变量，originTaskVariable是作业模版变量；磨平不参与比较的属性后进行比较
        TaskVariableDTO comparableOriginTaskVariable = new TaskVariableDTO();
        comparableOriginTaskVariable.setId(originTaskVariable.getId());
        comparableOriginTaskVariable.setTemplateId(currentTaskVariable.getTemplateId());
        comparableOriginTaskVariable.setPlanId(currentTaskVariable.getPlanId());
        comparableOriginTaskVariable.setInstanceId(currentTaskVariable.getInstanceId());
        comparableOriginTaskVariable.setName(originTaskVariable.getName());
        comparableOriginTaskVariable.setType(originTaskVariable.getType());
        comparableOriginTaskVariable.setDefaultValue(currentTaskVariable.getDefaultValue());
        comparableOriginTaskVariable.setDescription(originTaskVariable.getDescription());
        comparableOriginTaskVariable.setChangeable(originTaskVariable.getChangeable());
        comparableOriginTaskVariable.setRequired(originTaskVariable.getRequired());
        comparableOriginTaskVariable.setDelete(currentTaskVariable.getDelete());
        comparableOriginTaskVariable.setFollowTemplate(currentTaskVariable.getFollowTemplate());
        return comparableOriginTaskVariable;
    }

    /**
     * 任务步骤详情是否有变更
     */
    private boolean stepDetailChanged(TaskStepDTO currentTaskStep,
                                      TaskStepDTO originTaskStep,
                                      CompareMode compareMode) {
        switch (currentTaskStep.getType()) {
            case SCRIPT:
                return scriptStepChanged(currentTaskStep, originTaskStep, compareMode);
            case FILE:
                return fileStepChanged(currentTaskStep, originTaskStep, compareMode);
            case APPROVAL:
                return approvalStepChanged(currentTaskStep, originTaskStep, compareMode);
            default:
                return true;
        }
    }

    /**
     * 脚本步骤是否有变
     */
    private boolean scriptStepChanged(TaskStepDTO currentTaskStep,
                                      TaskStepDTO originTaskStep,
                                      CompareMode compareMode) {
        TaskScriptStepDTO comparableOriginScriptStep = buildComparableOriginScriptStep(
            currentTaskStep,
            originTaskStep,
            compareMode
        );
        return !currentTaskStep.getScriptStepInfo().equals(comparableOriginScriptStep);
    }

    /**
     * 文件分发步骤是否有变
     */
    private boolean fileStepChanged(TaskStepDTO currentTaskStep,
                                    TaskStepDTO originTaskStep,
                                    CompareMode compareMode) {
        TaskFileStepDTO comparableOriginFileStep = buildComparableOriginFileStep(
            currentTaskStep,
            originTaskStep,
            compareMode
        );
        return !currentTaskStep.getFileStepInfo().equals(comparableOriginFileStep);
    }

    /**
     * 人工确认步骤是否有变更
     */
    private boolean approvalStepChanged(TaskStepDTO currentTaskStep,
                                        TaskStepDTO originTaskStep,
                                        CompareMode compareMode) {
        TaskApprovalStepDTO comparableOriginApprovalStep = buildComparableOriginApprovalStep(
            currentTaskStep,
            originTaskStep,
            compareMode
        );
        return !currentTaskStep.getApprovalStepInfo().equals(comparableOriginApprovalStep);
    }

    private TaskScriptStepDTO buildComparableOriginScriptStep(TaskStepDTO currentTaskStep,
                                                              TaskStepDTO originTaskStep,
                                                              CompareMode compareMode) {
        TaskScriptStepDTO comparableOriginScriptStep;
        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            comparableOriginScriptStep = originTaskStep.getScriptStepInfo();
            comparableOriginScriptStep.setId(null);
            comparableOriginScriptStep.setTemplateId(currentTaskStep.getScriptStepInfo().getTemplateId());
            return comparableOriginScriptStep;
        }

        // currentScriptStep是执行方案脚本步骤，originScriptStep是作业模版脚本步骤，磨平不参与比较的属性后进行比较
        TaskScriptStepDTO currentScriptStep = currentTaskStep.getScriptStepInfo();
        TaskScriptStepDTO originScriptStep = originTaskStep.getScriptStepInfo();
        return new TaskScriptStepDTO(
            currentScriptStep.getId(),
            currentScriptStep.getTemplateId(),
            currentScriptStep.getPlanId(),
            currentScriptStep.getInstanceId(),
            currentScriptStep.getStepId(),
            originScriptStep.getScriptSource(),
            originScriptStep.getScriptId(),
            originScriptStep.getScriptVersionId(),
            originScriptStep.getContent(),
            originScriptStep.getLanguage(),
            originScriptStep.getScriptParam(),
            originScriptStep.getWindowsInterpreter(),
            originScriptStep.getTimeout(),
            originScriptStep.getAccount(),
            originScriptStep.getExecuteTarget(),
            originScriptStep.getSecureParam(),
            originScriptStep.getStatus(),
            originScriptStep.getIgnoreError()
        );
    }

    private TaskFileStepDTO buildComparableOriginFileStep(TaskStepDTO currentTaskStep,
                                                          TaskStepDTO originTaskStep,
                                                          CompareMode compareMode) {
        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            TaskFileStepDTO comparableOriginFileStep = originTaskStep.getFileStepInfo();
            comparableOriginFileStep.setId(null);
            return comparableOriginFileStep;
        }

        TaskFileStepDTO currentFileStep = currentTaskStep.getFileStepInfo();
        TaskFileStepDTO originFileStep = originTaskStep.getFileStepInfo();
        return new TaskFileStepDTO(
            currentFileStep.getId(),
            currentFileStep.getStepId(),
            originFileStep.getOriginFileList(),
            originFileStep.getDestinationFileLocation(),
            originFileStep.getExecuteAccount(),
            originFileStep.getDestinationHostList(),
            originFileStep.getTimeout(),
            originFileStep.getOriginSpeedLimit(),
            originFileStep.getTargetSpeedLimit(),
            originFileStep.getDuplicateHandler(),
            originFileStep.getNotExistPathHandler(),
            originFileStep.getIgnoreError()
        );
    }

    private TaskApprovalStepDTO buildComparableOriginApprovalStep(TaskStepDTO currentTaskStep,
                                                                  TaskStepDTO originTaskStep,
                                                                  CompareMode compareMode) {
        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            TaskApprovalStepDTO comparableOriginApprovalStep = originTaskStep.getApprovalStepInfo();
            comparableOriginApprovalStep.setId(null);
            return comparableOriginApprovalStep;
        }

        TaskApprovalStepDTO currentApprovalStep = currentTaskStep.getApprovalStepInfo();
        TaskApprovalStepDTO originApprovalStep = originTaskStep.getApprovalStepInfo();
        return new TaskApprovalStepDTO(
            currentApprovalStep.getId(),
            currentApprovalStep.getStepId(),
            originApprovalStep.getApprovalType(),
            originApprovalStep.getApprovalUser(),
            originApprovalStep.getApprovalMessage(),
            originApprovalStep.getNotifyChannel()
        );
    }
}
