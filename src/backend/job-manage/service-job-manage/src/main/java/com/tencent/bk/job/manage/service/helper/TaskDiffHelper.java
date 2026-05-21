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

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetContainerDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 任务差异比较
 */
@Component
@Slf4j
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
            log.debug("Task step count differs, compareMode={}, currentStepCount={}, originStepCount={}",
                compareMode, currentTaskStepList.size(), originTaskStepList.size());
            return true;
        }

        for (int i = 0; i < currentTaskStepList.size(); i++) {
            TaskStepDTO currentTaskStep = currentTaskStepList.get(i);
            TaskStepDTO originTaskStep = originTaskStepList.get(i);
            if (stepBaseInfoChanged(currentTaskStep, originTaskStep, compareMode)) {
                log.debug("Task step base info differs, compareMode={}, index={}, stepId={}",
                    compareMode, i, currentTaskStep.getId());
                return true;
            }
            if (stepDetailChanged(currentTaskStep, originTaskStep)) {
                log.debug("Task step detail differs, compareMode={}, index={}, stepId={}",
                    compareMode, i, currentTaskStep.getId());
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
            log.debug("Task variable count differs, compareMode={}, currentVariableCount={}, originVariableCount={}",
                compareMode, currentTaskVariableList.size(), originTaskVariableList.size());
            return true;
        }

        for (int i = 0; i < currentTaskVariableList.size(); i++) {
            TaskVariableDTO currentTaskVariable = currentTaskVariableList.get(i);
            TaskVariableDTO originTaskVariable = originTaskVariableList.get(i);
            if (variableChangedIgnoreValue(currentTaskVariable, originTaskVariable, compareMode)) {
                log.debug("Task variable differs after ignoring value, compareMode={}, index={}, varId={}, varType={}",
                    compareMode, i, currentTaskVariable.getId(), currentTaskVariable.getType());
                return true;
            }
        }
        return false;
    }

    /**
     * 任务的全局变量是否有差异，不比较变量值
     */
    private boolean variableChangedIgnoreValue(TaskVariableDTO currentTaskVariable,
                                               TaskVariableDTO originTaskVariable,
                                               CompareMode compareMode) {
        boolean definitionChanged = variableBaseInfoChanged(currentTaskVariable, originTaskVariable);
        if (definitionChanged) {
            return true;
        }

        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            if (Boolean.TRUE.equals(currentTaskVariable.getDelete())) {
                log.debug("Task variable delete flag differs, varId={}", currentTaskVariable.getId());
                return true;
            }
        }
        return false;
    }

    /**
     * 变量基本信息是否有变，不比较变量值
     */
    private boolean variableBaseInfoChanged(TaskVariableDTO currentTaskVariable,
                                            TaskVariableDTO originTaskVariable) {
        return valueChanged(currentTaskVariable.getId(), originTaskVariable.getId())
            || valueChanged(currentTaskVariable.getName(), originTaskVariable.getName())
            || valueChanged(currentTaskVariable.getType(), originTaskVariable.getType())
            || valueChanged(currentTaskVariable.getDescription(), originTaskVariable.getDescription())
            || valueChanged(currentTaskVariable.getChangeable(), originTaskVariable.getChangeable())
            || valueChanged(currentTaskVariable.getRequired(), originTaskVariable.getRequired());
    }

    /**
     * 步骤的基础信息是否有变更
     */
    private boolean stepBaseInfoChanged(TaskStepDTO currentTaskStep,
                                        TaskStepDTO originTaskStep,
                                        CompareMode compareMode) {
        if (valueChanged(currentTaskStep.getType(), originTaskStep.getType())
            || valueChanged(currentTaskStep.getName(), originTaskStep.getName())) {
            return true;
        }

        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            return currentTaskStep.getId() == null
                || currentTaskStep.getId() <= 0
                || currentTaskStep.getDelete() == 1
                || valueChanged(currentTaskStep.getId(), originTaskStep.getId());
        } else {
            return valueChanged(currentTaskStep.getTemplateStepId(), originTaskStep.getId());
        }
    }

    /**
     * 任务步骤详情是否有变更
     */
    private boolean stepDetailChanged(TaskStepDTO currentTaskStep,
                                      TaskStepDTO originTaskStep) {
        return switch (currentTaskStep.getType()) {
            case SCRIPT -> scriptStepChanged(currentTaskStep, originTaskStep);
            case FILE -> fileStepChanged(currentTaskStep, originTaskStep);
            case APPROVAL -> approvalStepChanged(currentTaskStep, originTaskStep);
        };
    }

    /**
     * 脚本步骤是否有变
     */
    private boolean scriptStepChanged(TaskStepDTO currentTaskStep,
                                      TaskStepDTO originTaskStep) {
        TaskScriptStepDTO currentScriptStep = currentTaskStep.getScriptStepInfo();
        TaskScriptStepDTO originScriptStep = originTaskStep.getScriptStepInfo();

        if (scriptStepBaseInfoChanged(currentScriptStep, originScriptStep)) {
            log.debug("Script step base info differs, stepId={}", currentTaskStep.getId());
            return true;
        }

        if (scriptStepDeepInfoChanged(currentScriptStep, originScriptStep)) {
            log.debug("Script step target differs, stepId={}", currentTaskStep.getId());
            return true;
        }

        return false;
    }

    /**
     * 脚本步骤基础信息是否有变
     */
    private boolean scriptStepBaseInfoChanged(TaskScriptStepDTO currentScriptStep,
                                              TaskScriptStepDTO originScriptStep) {
        return valueChanged(currentScriptStep.getScriptSource(), originScriptStep.getScriptSource())
            || valueChanged(currentScriptStep.getScriptId(), originScriptStep.getScriptId())
            || valueChanged(currentScriptStep.getScriptVersionId(), originScriptStep.getScriptVersionId())
            || valueChanged(currentScriptStep.getContent(), originScriptStep.getContent())
            || valueChanged(currentScriptStep.getLanguage(), originScriptStep.getLanguage())
            || valueChanged(currentScriptStep.getScriptParam(), originScriptStep.getScriptParam())
            || valueChanged(currentScriptStep.getWindowsInterpreter(), originScriptStep.getWindowsInterpreter())
            || valueChanged(currentScriptStep.getTimeout(), originScriptStep.getTimeout())
            || valueChanged(currentScriptStep.getAccount(), originScriptStep.getAccount())
            || valueChanged(currentScriptStep.getSecureParam(), originScriptStep.getSecureParam())
            || valueChanged(currentScriptStep.getIgnoreError(), originScriptStep.getIgnoreError());
    }

    /**
     * 文件步骤复杂信息（如执行目标、文件源）是否有变
     */
    private boolean fileStepDeepInfoChanged(TaskFileStepDTO currentFileStep,
                                            TaskFileStepDTO originFileStep) {
        if (targetChanged(currentFileStep.getDestinationHostList(), originFileStep.getDestinationHostList())) {
            return true;
        }
        return originFileListChanged(currentFileStep.getOriginFileList(), originFileStep.getOriginFileList());
    }

    /**
     * 脚本步骤复杂信息（如执行目标）是否有变
     */
    private boolean scriptStepDeepInfoChanged(TaskScriptStepDTO currentScriptStep,
                                              TaskScriptStepDTO originScriptStep) {
        return targetChanged(currentScriptStep.getExecuteTarget(), originScriptStep.getExecuteTarget());
    }

    /**
     * 文件分发步骤是否有变
     */
    private boolean fileStepChanged(TaskStepDTO currentTaskStep,
                                    TaskStepDTO originTaskStep) {
        TaskFileStepDTO currentFileStep = currentTaskStep.getFileStepInfo();
        TaskFileStepDTO originFileStep = originTaskStep.getFileStepInfo();

        if (fileStepBaseInfoChanged(currentFileStep, originFileStep)) {
            log.debug("File step base info differs, stepId={}", currentTaskStep.getId());
            return true;
        }
        if (fileStepDeepInfoChanged(currentFileStep, originFileStep)) {
            log.debug("File step target or source differs, stepId={}", currentTaskStep.getId());
            return true;
        }

        return false;
    }

    /**
     * 文件步骤基础信息是否有变
     */
    private boolean fileStepBaseInfoChanged(TaskFileStepDTO currentFileStep,
                                            TaskFileStepDTO originFileStep) {
        return valueChanged(currentFileStep.getDestinationFileLocation(), originFileStep.getDestinationFileLocation())
            || valueChanged(currentFileStep.getExecuteAccount(), originFileStep.getExecuteAccount())
            || valueChanged(currentFileStep.getTimeout(), originFileStep.getTimeout())
            || valueChanged(currentFileStep.getOriginSpeedLimit(), originFileStep.getOriginSpeedLimit())
            || valueChanged(currentFileStep.getTargetSpeedLimit(), originFileStep.getTargetSpeedLimit())
            || valueChanged(currentFileStep.getDuplicateHandler(), originFileStep.getDuplicateHandler())
            || valueChanged(currentFileStep.getNotExistPathHandler(), originFileStep.getNotExistPathHandler())
            || valueChanged(currentFileStep.getIgnoreError(), originFileStep.getIgnoreError());
    }

    /**
     * 人工确认步骤是否有变更
     */
    private boolean approvalStepChanged(TaskStepDTO currentTaskStep,
                                        TaskStepDTO originTaskStep) {
        TaskApprovalStepDTO currentApprovalStep = currentTaskStep.getApprovalStepInfo();
        TaskApprovalStepDTO originApprovalStep = originTaskStep.getApprovalStepInfo();

        if (approvalStepBaseInfoChanged(currentApprovalStep, originApprovalStep)) {
            log.debug("Approval step base info differs, stepId={}", currentTaskStep.getId());
            return true;
        }

        return false;
    }

    /**
     * 人工审核步骤基础信息是否有变
     */
    private boolean approvalStepBaseInfoChanged(TaskApprovalStepDTO currentApprovalStep,
                                                TaskApprovalStepDTO originApprovalStep) {
        return valueChanged(currentApprovalStep.getApprovalType(), originApprovalStep.getApprovalType())
            || valueChanged(currentApprovalStep.getApprovalMessage(), originApprovalStep.getApprovalMessage())
            || valueChanged(currentApprovalStep.getNotifyChannel(), originApprovalStep.getNotifyChannel())
            || valueChanged(currentApprovalStep.getApprovalUser(), originApprovalStep.getApprovalUser());
    }

    /**
     * 执行目标是否有变更
     */
    private boolean targetChanged(TaskTargetDTO currentTarget,
                                  TaskTargetDTO originTarget) {
        if (currentTarget == originTarget) {
            return false;
        }
        if (currentTarget == null || originTarget == null) {
            return true;
        }

        if (valueChanged(currentTarget.getVariable(), originTarget.getVariable())) {
            return true;
        }
        if (hostNodeChanged(currentTarget.getHostNodeList(), originTarget.getHostNodeList())) {
            return true;
        }
        return containerListChanged(currentTarget.getContainerList(), originTarget.getContainerList());
    }

    /**
     * 源文件列表是否有变更
     */
    private boolean originFileListChanged(List<TaskFileInfoDTO> currentFileInfoList,
                                          List<TaskFileInfoDTO> originFileInfoList) {
        if (currentFileInfoList == originFileInfoList) {
            return false;
        }
        if (currentFileInfoList == null || originFileInfoList == null) {
            return true;
        }
        if (currentFileInfoList.size() != originFileInfoList.size()) {
            return true;
        }
        for (int i = 0; i < currentFileInfoList.size(); i++) {
            if (fileInfoChanged(currentFileInfoList.get(i), originFileInfoList.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 上传源文件信息是否有变更
     */
    private boolean fileInfoChanged(TaskFileInfoDTO currentFileInfo,
                                    TaskFileInfoDTO originFileInfo) {
        if (currentFileInfo == originFileInfo) {
            return false;
        }
        if (currentFileInfo == null || originFileInfo == null) {
            return true;
        }
        if (valueChanged(currentFileInfo.getFileType(), originFileInfo.getFileType())) {
            return true;
        }
        if (valueChanged(currentFileInfo.getFileLocation(), originFileInfo.getFileLocation())) {
            return true;
        }
        if (TaskFileTypeEnum.SERVER == currentFileInfo.getFileType()
            && (targetChanged(currentFileInfo.getHost(), originFileInfo.getHost())
            || valueChanged(currentFileInfo.getHostAccount(), originFileInfo.getHostAccount()))) {
            return true;
        }
        return TaskFileTypeEnum.FILE_SOURCE == currentFileInfo.getFileType()
            && valueChanged(currentFileInfo.getFileSourceId(), originFileInfo.getFileSourceId());
    }

    /**
     * 主机节点信息是否有变更
     */
    private boolean hostNodeChanged(TaskHostNodeDTO currentHostNode,
                                    TaskHostNodeDTO originHostNode) {
        if (currentHostNode == originHostNode) {
            return false;
        }
        if (currentHostNode == null || originHostNode == null) {
            return true;
        }
        if (valueChanged(currentHostNode.getNodeInfoList(), originHostNode.getNodeInfoList())) {
            return true;
        }
        if (valueChanged(currentHostNode.getDynamicGroupId(), originHostNode.getDynamicGroupId())) {
            return true;
        }
        return hostListChanged(currentHostNode.getHostList(), originHostNode.getHostList());
    }

    /**
     * 主机列表是否有变更，只比较hostId
     */
    private boolean hostListChanged(List<ApplicationHostDTO> currentHostList,
                                    List<ApplicationHostDTO> originHostList) {
        if (currentHostList == originHostList) {
            return false;
        }
        if (currentHostList == null || originHostList == null) {
            return true;
        }

        if (currentHostList.size() != originHostList.size()) {
            return true;
        }
        for (int i = 0; i < currentHostList.size(); i++) {
            ApplicationHostDTO currentHost = currentHostList.get(i);
            ApplicationHostDTO originHost = originHostList.get(i);
            if (currentHost == originHost) {
                continue;
            }
            if (currentHost == null || originHost == null) {
                return true;
            }
            if (valueChanged(currentHost.getHostId(), originHost.getHostId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 容器列表是否变更，只比较id和containerId
     */
    private boolean containerListChanged(List<TaskTargetContainerDTO> currentContainerList,
                                         List<TaskTargetContainerDTO> originContainerList) {
        if (currentContainerList == originContainerList) {
            return false;
        }
        if (currentContainerList == null || originContainerList == null) {
            return true;
        }
        if (currentContainerList.size() != originContainerList.size()) {
            return true;
        }
        for (int i = 0; i < currentContainerList.size(); i++) {
            TaskTargetContainerDTO currentContainer = currentContainerList.get(i);
            TaskTargetContainerDTO originContainer = originContainerList.get(i);
            if (currentContainer == originContainer) {
                continue;
            }
            if (currentContainer == null || originContainer == null) {
                return true;
            }
            if (valueChanged(currentContainer.getId(), originContainer.getId())) {
                return true;
            }
            if (valueChanged(currentContainer.getContainerId(), originContainer.getContainerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean valueChanged(Object currentValue,
                                 Object originValue) {
        return !Objects.equals(currentValue, originValue);
    }
}
