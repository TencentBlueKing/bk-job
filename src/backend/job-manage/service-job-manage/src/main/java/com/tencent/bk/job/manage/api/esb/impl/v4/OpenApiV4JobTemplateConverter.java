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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.json.SecondToMillisUtil;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateDetailDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateAccountDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateApprovalStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateApprovalUserDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateFileDestinationDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateFileSourceDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateFileStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateGlobalVarDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateScriptStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateExecuteTargetDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateStepDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class OpenApiV4JobTemplateConverter {

    private OpenApiV4JobTemplateConverter() {
    }

    public static OpenApiV4JobTemplateDetailDTO toDetailDTO(TaskTemplateInfoDTO templateInfo,
                                                           AppScopeMappingService appScopeMappingService) {
        OpenApiV4JobTemplateDetailDTO detail = new OpenApiV4JobTemplateDetailDTO();
        detail.setId(templateInfo.getId());
        ResourceScope scope = appScopeMappingService.getScopeByAppId(templateInfo.getAppId());
        if (scope != null) {
            detail.setScopeType(scope.getType().getValue());
            detail.setScopeId(scope.getId());
        }
        detail.setName(templateInfo.getName());
        detail.setDescription(templateInfo.getDescription() == null ? "" : templateInfo.getDescription());
        detail.setCreator(templateInfo.getCreator());
        detail.setCreateTime(SecondToMillisUtil.toMillis(templateInfo.getCreateTime()));
        detail.setLastModifyUser(templateInfo.getLastModifyUser());
        detail.setLastModifyTime(SecondToMillisUtil.toMillis(templateInfo.getLastModifyTime()));
        detail.setGlobalVarList(toGlobalVarList(templateInfo.getVariableList()));
        detail.setStepList(toStepList(templateInfo.getStepList()));
        return detail;
    }

    private static List<V4JobTemplateGlobalVarDTO> toGlobalVarList(List<TaskVariableDTO> variableList) {
        if (CollectionUtils.isEmpty(variableList)) {
            return Collections.emptyList();
        }
        return variableList.stream().map(OpenApiV4JobTemplateConverter::toGlobalVar).collect(Collectors.toList());
    }

    private static V4JobTemplateGlobalVarDTO toGlobalVar(TaskVariableDTO taskVariable) {
        V4JobTemplateGlobalVarDTO globalVar = new V4JobTemplateGlobalVarDTO();
        globalVar.setName(taskVariable.getName());
        globalVar.setType(taskVariable.getType().getType());
        globalVar.setDescription(taskVariable.getDescription() == null ? "" : taskVariable.getDescription());
        globalVar.setRequired(Boolean.TRUE.equals(taskVariable.getRequired()) ? 1 : 0);
        if (TaskVariableTypeEnum.EXECUTE_OBJECT_LIST == taskVariable.getType()) {
            globalVar.setExecuteTarget(toExecuteTarget(
                TaskTargetDTO.fromJsonString(taskVariable.getDefaultValue())
            ));
        } else if (taskVariable.getType().needMask()) {
            globalVar.setValue(taskVariable.getType().getMask());
        } else {
            globalVar.setValue(taskVariable.getDefaultValue());
        }
        return globalVar;
    }

    private static List<V4JobTemplateStepDTO> toStepList(List<TaskStepDTO> stepList) {
        if (CollectionUtils.isEmpty(stepList)) {
            return Collections.emptyList();
        }
        return stepList.stream().map(OpenApiV4JobTemplateConverter::toStep).collect(Collectors.toList());
    }

    private static V4JobTemplateStepDTO toStep(TaskStepDTO taskStep) {
        V4JobTemplateStepDTO step = new V4JobTemplateStepDTO();
        step.setId(taskStep.getId());
        step.setName(taskStep.getName());
        step.setType(taskStep.getType().getValue());
        switch (taskStep.getType()) {
            case SCRIPT:
                step.setScriptInfo(toScriptInfo(taskStep.getScriptStepInfo()));
                break;
            case FILE:
                step.setFileInfo(toFileInfo(taskStep.getFileStepInfo()));
                break;
            case APPROVAL:
                step.setApprovalInfo(toApprovalInfo(taskStep.getApprovalStepInfo()));
                break;
            default:
                break;
        }
        return step;
    }

    private static V4JobTemplateScriptStepDTO toScriptInfo(TaskScriptStepDTO scriptStepInfo) {
        if (scriptStepInfo == null) {
            return null;
        }
        V4JobTemplateScriptStepDTO scriptInfo = new V4JobTemplateScriptStepDTO();
        scriptInfo.setScriptType(scriptStepInfo.getScriptSource().getType());
        scriptInfo.setScriptId(scriptStepInfo.getScriptId());
        scriptInfo.setScriptVersionId(scriptStepInfo.getScriptVersionId());
        if (StringUtils.isNotBlank(scriptStepInfo.getContent())) {
            scriptInfo.setScriptContent(Base64Util.encodeContentToStr(scriptStepInfo.getContent()));
        }
        scriptInfo.setScriptLanguage(scriptStepInfo.getLanguage().getValue());
        if (StringUtils.isNotBlank(scriptStepInfo.getScriptParam())) {
            scriptInfo.setScriptParam(Base64Util.encodeContentToStr(scriptStepInfo.getScriptParam()));
        }
        scriptInfo.setWindowsInterpreter(scriptStepInfo.getWindowsInterpreter());
        scriptInfo.setScriptTimeout(scriptStepInfo.getTimeout());
        scriptInfo.setIsParamSensitive(Boolean.TRUE.equals(scriptStepInfo.getSecureParam()) ? 1 : 0);
        scriptInfo.setIsIgnoreError(Boolean.TRUE.equals(scriptStepInfo.getIgnoreError()) ? 1 : 0);
        scriptInfo.setAccount(toAccount(scriptStepInfo.getAccount(), scriptStepInfo.getAccountVar()));
        scriptInfo.setExecuteTarget(toExecuteTarget(scriptStepInfo.getExecuteTarget()));
        return scriptInfo;
    }

    private static V4JobTemplateFileStepDTO toFileInfo(TaskFileStepDTO fileStepInfo) {
        if (fileStepInfo == null) {
            return null;
        }
        V4JobTemplateFileStepDTO fileInfo = new V4JobTemplateFileStepDTO();
        if (CollectionUtils.isNotEmpty(fileStepInfo.getOriginFileList())) {
            fileInfo.setFileSourceList(fileStepInfo.getOriginFileList().stream()
                .map(OpenApiV4JobTemplateConverter::toFileSource)
                .collect(Collectors.toList()));
        } else {
            fileInfo.setFileSourceList(Collections.emptyList());
        }
        V4JobTemplateFileDestinationDTO destination = new V4JobTemplateFileDestinationDTO();
        destination.setPath(fileStepInfo.getDestinationFileLocation());
        destination.setAccount(toAccount(fileStepInfo.getExecuteAccount(), fileStepInfo.getExecuteAccountVar()));
        destination.setExecuteTarget(toExecuteTarget(fileStepInfo.getDestinationHostList()));
        fileInfo.setFileDestination(destination);
        fileInfo.setTimeout(fileStepInfo.getTimeout());
        fileInfo.setSourceSpeedLimit(fileStepInfo.getOriginSpeedLimit());
        fileInfo.setDestinationSpeedLimit(fileStepInfo.getTargetSpeedLimit());
        fileInfo.setTransferMode(TaskFileStepDTO.toTransferMode(
            fileStepInfo.getDuplicateHandler(),
            fileStepInfo.getNotExistPathHandler()
        ).getValue());
        fileInfo.setIsIgnoreError(Boolean.TRUE.equals(fileStepInfo.getIgnoreError()) ? 1 : 0);
        return fileInfo;
    }

    private static V4JobTemplateFileSourceDTO toFileSource(TaskFileInfoDTO taskFileInfo) {
        V4JobTemplateFileSourceDTO fileSource = new V4JobTemplateFileSourceDTO();
        fileSource.setFileList(taskFileInfo.getFileLocation());
        if (taskFileInfo.getFileType() != null) {
            fileSource.setFileType(taskFileInfo.getFileType().getType());
        }
        if (taskFileInfo.getFileType() == TaskFileTypeEnum.SERVER) {
            fileSource.setAccount(toAccount(taskFileInfo.getHostAccount(), taskFileInfo.getHostAccountVar()));
            fileSource.setExecuteTarget(toExecuteTarget(taskFileInfo.getHost()));
        }
        return fileSource;
    }

    private static V4JobTemplateApprovalStepDTO toApprovalInfo(TaskApprovalStepDTO approvalStepInfo) {
        if (approvalStepInfo == null) {
            return null;
        }
        V4JobTemplateApprovalStepDTO approvalInfo = new V4JobTemplateApprovalStepDTO();
        approvalInfo.setApprovalType(approvalStepInfo.getApprovalType().getType());
        approvalInfo.setApprovalUser(toApprovalUser(approvalStepInfo.getApprovalUser()));
        approvalInfo.setApprovalMessage(approvalStepInfo.getApprovalMessage());
        approvalInfo.setNotifyChannel(approvalStepInfo.getNotifyChannel());
        return approvalInfo;
    }

    private static V4JobTemplateApprovalUserDTO toApprovalUser(UserRoleInfoDTO approvalUser) {
        if (approvalUser == null) {
            return null;
        }
        V4JobTemplateApprovalUserDTO user = new V4JobTemplateApprovalUserDTO();
        user.setUserList(approvalUser.getUserList());
        user.setRoleList(approvalUser.getRoleList());
        return user;
    }

    private static V4JobTemplateAccountDTO toAccount(Long accountId, String accountVar) {
        if ((accountId == null || accountId <= 0) && StringUtils.isBlank(accountVar)) {
            return null;
        }
        V4JobTemplateAccountDTO account = new V4JobTemplateAccountDTO();
        account.setId(accountId);
        account.setAccountVar(StringUtils.trimToNull(accountVar));
        return account;
    }

    /**
     * 仅映射主机类执行目标（host_list / dynamic_groups / topo_nodes），不含静态容器目标。
     */
    static V4JobTemplateExecuteTargetDTO toExecuteTarget(TaskTargetDTO taskTarget) {
        if (taskTarget == null) {
            return null;
        }
        V4JobTemplateExecuteTargetDTO executeTarget = new V4JobTemplateExecuteTargetDTO();
        executeTarget.setVariable(taskTarget.getVariable());
        if (taskTarget.getHostNodeList() != null) {
            TaskHostNodeDTO hostNode = taskTarget.getHostNodeList();
            if (CollectionUtils.isNotEmpty(hostNode.getHostList())) {
                executeTarget.setHostList(hostNode.getHostList().stream()
                    .map(OpenApiV4JobTemplateConverter::toV4Host)
                    .collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(hostNode.getDynamicGroupId())) {
                executeTarget.setDynamicGroups(hostNode.getDynamicGroupId().stream().map(id -> {
                    EsbDynamicGroupDTO group = new EsbDynamicGroupDTO();
                    group.setId(id);
                    return group;
                }).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(hostNode.getNodeInfoList())) {
                executeTarget.setTopoNodes(hostNode.getNodeInfoList().stream()
                    .map(TaskNodeInfoDTO::toEsbCmdbTopoNode)
                    .collect(Collectors.toList()));
            }
        }
        return executeTarget;
    }

    private static OpenApiV4HostDTO toV4Host(ApplicationHostDTO host) {
        OpenApiV4HostDTO v4Host = new OpenApiV4HostDTO();
        v4Host.setBkHostId(host.getHostId());
        v4Host.setBkCloudId(host.getCloudAreaId());
        v4Host.setIp(host.getIp());
        return v4Host;
    }
}
