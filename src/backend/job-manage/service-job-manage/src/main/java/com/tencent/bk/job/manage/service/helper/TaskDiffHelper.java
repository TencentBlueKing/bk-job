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
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
            if (stepDetailChanged(currentTaskStep, originTaskStep, compareMode)) {
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
            TaskVariableDTO comparableOriginTaskVariable = buildComparableOriginVariable(
                currentTaskVariable,
                originTaskVariable,
                compareMode
            );
            if (!currentTaskVariable.equals(comparableOriginTaskVariable)) {
                log.debug("Task variable differs after ignoring value, compareMode={}, index={}, varId={}, varType={}",
                    compareMode, i, currentTaskVariable.getId(), currentTaskVariable.getType());
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
            comparableOriginScriptStep.setExecuteTarget(buildComparableOriginTarget(
                currentTaskStep.getScriptStepInfo().getExecuteTarget(),
                comparableOriginScriptStep.getExecuteTarget()
            ));
            return comparableOriginScriptStep;
        }

        // currentScriptStep是执行方案脚本步骤，originScriptStep是作业模版脚本步骤，磨平不参与比较的属性后进行比较
        TaskScriptStepDTO currentScriptStep = currentTaskStep.getScriptStepInfo();
        TaskScriptStepDTO originScriptStep = originTaskStep.getScriptStepInfo();
        comparableOriginScriptStep = new TaskScriptStepDTO(
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
        comparableOriginScriptStep.setExecuteTarget(buildComparableOriginTarget(
            currentScriptStep.getExecuteTarget(),
            comparableOriginScriptStep.getExecuteTarget()
        ));
        return comparableOriginScriptStep;
    }

    private TaskFileStepDTO buildComparableOriginFileStep(TaskStepDTO currentTaskStep,
                                                          TaskStepDTO originTaskStep,
                                                          CompareMode compareMode) {
        if (compareMode == CompareMode.TEMPLATE_TO_TEMPLATE) {
            TaskFileStepDTO comparableOriginFileStep = originTaskStep.getFileStepInfo();
            comparableOriginFileStep.setId(null);
            alignFileStepHostIdForCompare(currentTaskStep.getFileStepInfo(), comparableOriginFileStep);
            return comparableOriginFileStep;
        }

        TaskFileStepDTO currentFileStep = currentTaskStep.getFileStepInfo();
        TaskFileStepDTO originFileStep = originTaskStep.getFileStepInfo();
        TaskFileStepDTO comparableOriginFileStep = new TaskFileStepDTO(
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
        alignFileStepHostIdForCompare(currentFileStep, comparableOriginFileStep);
        return comparableOriginFileStep;
    }

    private void alignFileStepHostIdForCompare(TaskFileStepDTO currentFileStep,
                                               TaskFileStepDTO originFileStep) {
        originFileStep.setDestinationHostList(buildComparableOriginTarget(
            currentFileStep.getDestinationHostList(),
            originFileStep.getDestinationHostList()
        ));
        originFileStep.setOriginFileList(buildComparableOriginFileList(
            currentFileStep.getOriginFileList(),
            originFileStep.getOriginFileList()
        ));
    }

    private List<TaskFileInfoDTO> buildComparableOriginFileList(List<TaskFileInfoDTO> currentFileList,
                                                                List<TaskFileInfoDTO> originFileList) {
        if (CollectionUtils.isEmpty(currentFileList) || CollectionUtils.isEmpty(originFileList)
            || currentFileList.size() != originFileList.size()) {
            return originFileList;
        }
        List<TaskFileInfoDTO> comparableOriginFileList = new ArrayList<>(originFileList.size());
        for (int i = 0; i < currentFileList.size(); i++) {
            TaskFileInfoDTO currentFileInfo = currentFileList.get(i);
            TaskFileInfoDTO originFileInfo = originFileList.get(i);
            if (currentFileInfo == null || originFileInfo == null) {
                return originFileList;
            }
            comparableOriginFileList.add(buildComparableOriginFileInfo(currentFileInfo, originFileInfo));
        }
        return comparableOriginFileList;
    }

    private TaskFileInfoDTO buildComparableOriginFileInfo(TaskFileInfoDTO currentFileInfo,
                                                          TaskFileInfoDTO originFileInfo) {
        return new TaskFileInfoDTO(
            originFileInfo.getId(),
            originFileInfo.getStepId(),
            originFileInfo.getFileType(),
            originFileInfo.getFileLocation(),
            originFileInfo.getFileHash(),
            originFileInfo.getFileSize(),
            buildComparableOriginTarget(currentFileInfo.getHost(), originFileInfo.getHost()),
            originFileInfo.getHostAccount(),
            originFileInfo.getFileSourceId()
        );
    }

    /**
     * current、origin的host可能有差异，导致equal比较不符合预期，只关注hostId
     */
    private TaskTargetDTO buildComparableOriginTarget(TaskTargetDTO currentTarget,
                                                      TaskTargetDTO originTarget) {
        List<ApplicationHostDTO> currentHosts = getHostList(currentTarget);
        List<ApplicationHostDTO> originHosts = getHostList(originTarget);
        if (CollectionUtils.isEmpty(currentHosts) || CollectionUtils.isEmpty(originHosts)
            || currentHosts.size() != originHosts.size()) {
            return originTarget;
        }
        List<ApplicationHostDTO> originHostsForCompare = new ArrayList<>(currentHosts.size());
        for (int i = 0; i < currentHosts.size(); i++) {
            if (currentHosts.get(i) == null || originHosts.get(i) == null) {
                return originTarget;
            }
            originHostsForCompare.add(buildOriginHostForCompare(currentHosts.get(i), originHosts.get(i)));
        }
        return buildOriginTargetWithHostList(originTarget, originHostsForCompare);
    }

    private List<ApplicationHostDTO> getHostList(TaskTargetDTO target) {
        TaskHostNodeDTO hostNode = target == null ? null : target.getHostNodeList();
        return hostNode == null ? null : hostNode.getHostList();
    }

    private TaskTargetDTO buildOriginTargetWithHostList(TaskTargetDTO originTarget,
                                                       List<ApplicationHostDTO> hostList) {
        TaskHostNodeDTO originHostNode = originTarget.getHostNodeList();
        TaskHostNodeDTO hostNodeForCompare = new TaskHostNodeDTO(
            originHostNode.getNodeInfoList(),
            originHostNode.getDynamicGroupId(),
            hostList
        );
        return new TaskTargetDTO(originTarget.getVariable(), hostNodeForCompare, originTarget.getContainerList());
    }

    private ApplicationHostDTO buildOriginHostForCompare(ApplicationHostDTO currentHost,
                                                         ApplicationHostDTO originHost) {
        return new ApplicationHostDTO(
            originHost.getHostId(),
            currentHost.getAppId(),
            currentHost.getBizId(),
            currentHost.getIp(),
            currentHost.getIpv6(),
            currentHost.getAgentId(),
            currentHost.getDisplayIp(),
            currentHost.getHostName(),
            currentHost.getGseAgentStatus(),
            currentHost.getGseAgentAlive(),
            currentHost.getCloudAreaId(),
            currentHost.getCloudAreaName(),
            currentHost.getCloudIp(),
            currentHost.getOsName(),
            currentHost.getOsType(),
            currentHost.getOsTypeName(),
            currentHost.getCloudVendorId(),
            currentHost.getCloudVendorName(),
            currentHost.getLastTime(),
            currentHost.getTenantId(),
            currentHost.getSetId(),
            currentHost.getModuleId(),
            currentHost.getModuleType(),
            currentHost.getIpList(),
            currentHost.getTopoPathList()
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
