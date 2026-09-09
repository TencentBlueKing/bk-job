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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.KubeContainerOperator;
import com.tencent.bk.job.common.constant.QueryableContainerField;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.common.model.dto.KubeWorkloadObjectDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.OperatorDispatcher;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.validation.KubePropConditionValidator;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetContainerDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateAccountReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerFilterDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateExecuteTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateVarTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileSourceReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateGlobalVarReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplatePropConditionDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateScriptStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateWriteRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4KubeTopoDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobTemplateRequest;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.template.TemplateLocalFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 把 OpenAPI V4 的声明式模板写入请求转换为服务层要的 TaskTemplateInfoDTO。
 * 更新时负责「终态 → 增量」：为请求中未出现的既有步骤/变量补 delete 标记。
 */
@Slf4j
@Component
public class OpenApiV4JobTemplateWriteConverter {

    private static final String PARAM_STEP_LIST = "step_list";
    private static final String PARAM_GLOBAL_VAR_LIST = "global_var_list";

    private final TemplateLocalFileService templateLocalFileService;
    private final ScriptManager scriptManager;

    public OpenApiV4JobTemplateWriteConverter(TemplateLocalFileService templateLocalFileService,
                                              ScriptManager scriptManager) {
        this.templateLocalFileService = templateLocalFileService;
        this.scriptManager = scriptManager;
    }

    public TaskTemplateInfoDTO toCreateTemplateInfo(String username,
                                                    Long appId,
                                                    V4CreateJobTemplateRequest request) {
        TaskTemplateInfoDTO templateInfo = buildBasicInfo(username, appId, request);
        templateInfo.setCreator(username);
        templateInfo.setStatus(TaskTemplateStatusEnum.NEW);
        templateInfo.setTags(Collections.emptyList());

        rejectStepIdOnCreate(request.getStepList());
        templateInfo.setStepList(convertSteps(appId, request.getStepList()));
        templateInfo.setVariableList(convertVariables(request.getGlobalVarList(), Collections.emptyMap()));
        return templateInfo;
    }

    public TaskTemplateInfoDTO toUpdateTemplateInfo(String username,
                                                    Long appId,
                                                    V4UpdateJobTemplateRequest request,
                                                    TaskTemplateInfoDTO existingTemplate) {
        TaskTemplateInfoDTO templateInfo = buildBasicInfo(username, appId, request);
        templateInfo.setId(request.getId());
        // 名称缺省表示不改名。这里必须回填出真实名称而不是留空：服务层要拿它查重名，
        // 审计记录的资源名也取自这个 DTO，留空会让审计记到一条没有名字的变更
        if (StringUtils.isBlank(templateInfo.getName())) {
            templateInfo.setName(existingTemplate.getName());
        }
        // v4 写接口不接收 tags，沿用模板原有标签，避免声明式写回把标签清空
        templateInfo.setTags(existingTemplate.getTags());

        Map<Long, TaskStepDTO> existingSteps = indexStepsById(existingTemplate.getStepList());
        checkRequestStepIds(request.getStepList(), existingSteps.keySet());
        List<TaskStepDTO> steps = convertSteps(appId, request.getStepList());
        steps.addAll(buildDeletedSteps(request.getStepList(), existingSteps));
        templateInfo.setStepList(steps);

        Map<String, TaskVariableDTO> existingVariables = indexVariablesByName(existingTemplate.getVariableList());
        List<TaskVariableDTO> variables = convertVariables(request.getGlobalVarList(), existingVariables);
        variables.addAll(buildDeletedVariables(request.getGlobalVarList(), existingVariables));
        templateInfo.setVariableList(variables);
        return templateInfo;
    }

    private TaskTemplateInfoDTO buildBasicInfo(String username, Long appId, V4JobTemplateWriteRequest request) {
        TaskTemplateInfoDTO templateInfo = new TaskTemplateInfoDTO();
        templateInfo.setAppId(appId);
        templateInfo.setName(request.getName());
        templateInfo.setDescription(request.getDescription() == null ? "" : request.getDescription());
        templateInfo.setLastModifyUser(username);
        templateInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        return templateInfo;
    }

    // ---------------------------------------------------------------- 步骤

    private void rejectStepIdOnCreate(List<V4JobTemplateStepReq> stepReqList) {
        for (V4JobTemplateStepReq stepReq : stepReqList) {
            if (stepReq.getId() != null) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{PARAM_STEP_LIST,
                        "step id is not allowed when creating a job template, but got " + stepReq.getId()});
            }
        }
    }

    /**
     * 请求中的步骤 ID 必须互不重复，且都属于该模板。校验在事务之前完成，非法时不做任何写入。
     */
    private void checkRequestStepIds(List<V4JobTemplateStepReq> stepReqList, Set<Long> existingStepIds) {
        Set<Long> seenIds = new HashSet<>();
        Set<Long> duplicatedIds = new LinkedHashSet<>();
        Set<Long> unknownIds = new LinkedHashSet<>();
        for (V4JobTemplateStepReq stepReq : stepReqList) {
            Long stepId = stepReq.getId();
            if (stepId == null) {
                continue;
            }
            if (!seenIds.add(stepId)) {
                duplicatedIds.add(stepId);
            }
            if (!existingStepIds.contains(stepId)) {
                unknownIds.add(stepId);
            }
        }
        if (!duplicatedIds.isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{PARAM_STEP_LIST, "duplicated step id: " + joinIds(duplicatedIds)});
        }
        if (!unknownIds.isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{PARAM_STEP_LIST,
                    "step id does not belong to this job template: " + joinIds(unknownIds)});
        }
    }

    private List<TaskStepDTO> convertSteps(Long appId, List<V4JobTemplateStepReq> stepReqList) {
        Map<Long, ScriptTypeEnum> refScriptLanguages = resolveRefScriptLanguages(stepReqList);
        List<TaskStepDTO> steps = new ArrayList<>(stepReqList.size());
        for (V4JobTemplateStepReq stepReq : stepReqList) {
            steps.add(convertStep(appId, stepReq, refScriptLanguages));
        }
        return steps;
    }

    /**
     * 一次性查出所有被引用脚本版本的语言。
     * 引用脚本的语言以被引用版本为准，调用方只传 script_version_id，语言由这里反查补齐。
     */
    private Map<Long, ScriptTypeEnum> resolveRefScriptLanguages(List<V4JobTemplateStepReq> stepReqList) {
        Set<Long> scriptVersionIds = stepReqList.stream()
            .filter(stepReq -> TaskStepTypeEnum.SCRIPT.getValue() == stepReq.getType())
            .map(V4JobTemplateStepReq::getScriptInfo)
            .filter(scriptReq -> scriptReq != null && isRefScript(scriptReq.getScriptType()))
            .map(V4JobTemplateScriptStepReq::getScriptVersionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (scriptVersionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ScriptDTO> scriptVersions = scriptManager.batchGetScriptVersionsByIds(scriptVersionIds);
        Set<Long> missingIds = new LinkedHashSet<>(scriptVersionIds);
        missingIds.removeAll(scriptVersions.keySet());
        if (!missingIds.isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"script_version_id", "script version does not exist: " + joinIds(missingIds)});
        }

        Map<Long, ScriptTypeEnum> languages = new HashMap<>(scriptVersions.size());
        scriptVersions.forEach((versionId, script) ->
            languages.put(versionId, ScriptTypeEnum.valOf(script.getType())));
        return languages;
    }

    private boolean isRefScript(Integer scriptType) {
        TaskScriptSourceEnum scriptSource = TaskScriptSourceEnum.valueOf(scriptType);
        return scriptSource == TaskScriptSourceEnum.CITING || scriptSource == TaskScriptSourceEnum.PUBLIC;
    }

    private TaskStepDTO convertStep(Long appId,
                                    V4JobTemplateStepReq stepReq,
                                    Map<Long, ScriptTypeEnum> refScriptLanguages) {
        TaskStepDTO step = new TaskStepDTO();
        step.setId(stepReq.getId());
        step.setName(stepReq.getName());
        step.setType(TaskStepTypeEnum.valueOf(stepReq.getType()));
        step.setDelete(0);
        step.setEnable(1);
        switch (step.getType()) {
            case SCRIPT:
                step.setScriptStepInfo(
                    convertScriptStep(stepReq.getId(), stepReq.getScriptInfo(), refScriptLanguages));
                break;
            case FILE:
                step.setFileStepInfo(convertFileStep(appId, stepReq.getId(), stepReq.getFileInfo()));
                break;
            case APPROVAL:
                step.setApprovalStepInfo(convertApprovalStep(stepReq.getId(), stepReq.getApprovalInfo()));
                break;
            default:
                throw new InvalidParamException(ErrorCode.WRONG_STEP_TYPE);
        }
        return step;
    }

    private List<TaskStepDTO> buildDeletedSteps(List<V4JobTemplateStepReq> stepReqList,
                                                Map<Long, TaskStepDTO> existingSteps) {
        Set<Long> keptStepIds = stepReqList.stream()
            .map(V4JobTemplateStepReq::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<TaskStepDTO> deletedSteps = new ArrayList<>();
        for (Map.Entry<Long, TaskStepDTO> entry : existingSteps.entrySet()) {
            if (keptStepIds.contains(entry.getKey())) {
                continue;
            }
            TaskStepDTO deletedStep = new TaskStepDTO();
            deletedStep.setId(entry.getKey());
            deletedStep.setName(entry.getValue().getName());
            deletedStep.setType(entry.getValue().getType());
            deletedStep.setDelete(1);
            deletedSteps.add(deletedStep);
        }
        return deletedSteps;
    }

    // ---------------------------------------------------------------- 脚本步骤

    private TaskScriptStepDTO convertScriptStep(Long stepId,
                                                V4JobTemplateScriptStepReq scriptReq,
                                                Map<Long, ScriptTypeEnum> refScriptLanguages) {
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setStepId(stepId);
        TaskScriptSourceEnum scriptSource = TaskScriptSourceEnum.valueOf(scriptReq.getScriptType());
        scriptStep.setScriptSource(scriptSource);
        if (scriptSource == TaskScriptSourceEnum.LOCAL) {
            scriptStep.setContent(decodeBase64(scriptReq.getScriptContent(), "script_content"));
            scriptStep.setLanguage(ScriptTypeEnum.valOf(scriptReq.getScriptLanguage()));
        } else {
            scriptStep.setScriptId(scriptReq.getScriptId());
            scriptStep.setScriptVersionId(scriptReq.getScriptVersionId());
            // 语言以被引用版本为准，请求里传的 script_language 一律忽略，避免落库的语言与脚本对不上
            scriptStep.setLanguage(refScriptLanguages.get(scriptReq.getScriptVersionId()));
        }
        scriptStep.setScriptParam(decodeBase64(scriptReq.getScriptParam(), "script_param"));
        scriptStep.setWindowsInterpreter(scriptReq.getWindowsInterpreter());
        scriptStep.setTimeout(scriptReq.getScriptTimeout() == null
            ? (long) JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : scriptReq.getScriptTimeout());
        scriptStep.setSecureParam(toBoolean(scriptReq.getIsParamSensitive()));
        scriptStep.setIgnoreError(toBoolean(scriptReq.getIsIgnoreError()));
        // 脚本状态标志位不可为空，先置 0 占位；保存末尾 refreshTemplateScriptStatusByTemplate 会按脚本版本真实状态重算
        scriptStep.setStatus(0);
        applyAccount(scriptReq.getAccount(), scriptStep::setAccount, scriptStep::setAccountVar);
        scriptStep.setExecuteTarget(convertExecuteTarget(scriptReq.getExecuteTarget()));
        return scriptStep;
    }

    // ---------------------------------------------------------------- 文件步骤

    private TaskFileStepDTO convertFileStep(Long appId, Long stepId, V4JobTemplateFileStepReq fileReq) {
        TaskFileStepDTO fileStep = new TaskFileStepDTO();
        fileStep.setStepId(stepId);
        fileStep.setOriginFileList(fileReq.getFileSourceList().stream()
            .map(fileSourceReq -> convertFileSource(appId, stepId, fileSourceReq))
            .collect(Collectors.toList()));
        fileStep.setDestinationFileLocation(fileReq.getFileDestination().getPath());
        applyAccount(fileReq.getFileDestination().getAccount(),
            fileStep::setExecuteAccount, fileStep::setExecuteAccountVar);
        fileStep.setDestinationHostList(convertExecuteTarget(fileReq.getFileDestination().getExecuteTarget()));
        fileStep.setTimeout(fileReq.getTimeout() == null
            ? (long) JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : fileReq.getTimeout());
        fileStep.setOriginSpeedLimit(fileReq.getSourceSpeedLimit());
        fileStep.setTargetSpeedLimit(fileReq.getDestinationSpeedLimit());
        FileTransferModeEnum transferMode = fileReq.getTransferMode() == null
            ? FileTransferModeEnum.FORCE : FileTransferModeEnum.getFileTransferModeEnum(fileReq.getTransferMode());
        fileStep.setDuplicateHandler(TaskFileStepDTO.toDuplicateHandler(transferMode));
        fileStep.setNotExistPathHandler(TaskFileStepDTO.toNotExistPathHandler(transferMode));
        fileStep.setIgnoreError(toBoolean(fileReq.getIsIgnoreError()));
        return fileStep;
    }

    private TaskFileInfoDTO convertFileSource(Long appId, Long stepId, V4JobTemplateFileSourceReq fileSourceReq) {
        TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();
        fileInfo.setStepId(stepId);
        // v4 不暴露源文件行的主键，没有可用来匹配既有行的稳定标识；统一填 0 让服务层当作新增，
        // 更新既有文件步骤时等价于「旧行全删、新行全插」，与声明式全量替换的语义一致
        fileInfo.setId(0L);
        TaskFileTypeEnum fileType = TaskFileTypeEnum.valueOf(fileSourceReq.getFileType());
        fileInfo.setFileType(fileType);
        fileInfo.setFileLocation(fileSourceReq.getFileList());
        switch (fileType) {
            case SERVER:
                applyAccount(fileSourceReq.getAccount(), fileInfo::setHostAccount, fileInfo::setHostAccountVar);
                fileInfo.setHost(convertExecuteTarget(fileSourceReq.getExecuteTarget()));
                break;
            case LOCAL:
                fillLocalFileDetail(appId, fileSourceReq, fileInfo);
                break;
            case FILE_SOURCE:
                // 文件源的存在性、业务内可用性与权限由服务层的 validateReferencedFileSources 统一校验
                fileInfo.setFileSourceId(fileSourceReq.getFileSourceId());
                break;
            default:
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"file_type", "unsupported file type: " + fileSourceReq.getFileType()});
        }
        return fileInfo;
    }

    /**
     * 本地文件的 hash 与大小是单值字段，一个源文件条目只能承载一个路径。
     */
    private void fillLocalFileDetail(Long appId, V4JobTemplateFileSourceReq fileSourceReq, TaskFileInfoDTO fileInfo) {
        if (fileSourceReq.getFileList().size() != 1) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"file_list",
                    "a local file source accepts exactly one file path, "
                        + "please split multiple local files into separate file_source_list items"});
        }
        TemplateLocalFileService.LocalFileDetail detail =
            templateLocalFileService.getFileDetail(appId, fileSourceReq.getFileList().get(0));
        fileInfo.setFileHash(detail.getFileHash());
        fileInfo.setFileSize(detail.getFileSize());
    }

    // ---------------------------------------------------------------- 审批步骤

    private TaskApprovalStepDTO convertApprovalStep(Long stepId, V4JobTemplateApprovalStepReq approvalReq) {
        if (approvalReq.getApprovalUser().isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"approval_user", "provide at least one of user_list and role_list"});
        }
        TaskApprovalStepDTO approvalStep = new TaskApprovalStepDTO();
        approvalStep.setStepId(stepId);
        approvalStep.setApprovalType(approvalReq.getApprovalType() == null
            ? TaskApprovalTypeEnum.ANYONE : TaskApprovalTypeEnum.valueOf(approvalReq.getApprovalType()));
        UserRoleInfoDTO approvalUser = new UserRoleInfoDTO();
        approvalUser.setUserList(approvalReq.getApprovalUser().getUserList());
        approvalUser.setRoleList(approvalReq.getApprovalUser().getRoleList());
        approvalStep.setApprovalUser(approvalUser);
        approvalStep.setApprovalMessage(approvalReq.getApprovalMessage());
        approvalStep.setNotifyChannel(approvalReq.getNotifyChannel());
        return approvalStep;
    }

    // ---------------------------------------------------------------- 全局变量

    private List<TaskVariableDTO> convertVariables(List<V4JobTemplateGlobalVarReq> variableReqList,
                                                   Map<String, TaskVariableDTO> existingVariables) {
        if (CollectionUtils.isEmpty(variableReqList)) {
            return new ArrayList<>();
        }
        Set<String> seenNames = new HashSet<>();
        List<TaskVariableDTO> variables = new ArrayList<>(variableReqList.size());
        for (V4JobTemplateGlobalVarReq variableReq : variableReqList) {
            if (!seenNames.add(variableReq.getName())) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{PARAM_GLOBAL_VAR_LIST, "duplicated variable name: " + variableReq.getName()});
            }
            variables.add(convertVariable(variableReq, existingVariables.get(variableReq.getName())));
        }
        return variables;
    }

    private TaskVariableDTO convertVariable(V4JobTemplateGlobalVarReq variableReq, TaskVariableDTO existingVariable) {
        TaskVariableTypeEnum type = TaskVariableTypeEnum.valOf(variableReq.getType());
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setName(variableReq.getName());
        variable.setType(type);
        variable.setDelete(false);
        variable.setRequired(toBoolean(variableReq.getRequired()));
        variable.setDescription(variableReq.getDescription() == null ? "" : variableReq.getDescription());
        variable.setDefaultValue(resolveVariableValue(variableReq, type, existingVariable));
        if (existingVariable != null) {
            // 变量按名称匹配到既有变量时保留其 ID，避免退化成「删旧建新」
            variable.setId(existingVariable.getId());
            // changeable 与 follow_template 未在 v4 契约中暴露，沿用既有取值
            variable.setChangeable(existingVariable.getChangeable());
            variable.setFollowTemplate(existingVariable.getFollowTemplate());
        } else {
            // 服务层用 id > 0 区分「更新既有变量」与「新增变量」，新增时不可为 null；
            // 真实 ID 由 task_template_variable 表自增生成，这里只是占位
            variable.setId(0L);
            variable.setChangeable(defaultChangeable(type));
            variable.setFollowTemplate(false);
        }
        return variable;
    }

    private String resolveVariableValue(V4JobTemplateGlobalVarReq variableReq,
                                        TaskVariableTypeEnum type,
                                        TaskVariableDTO existingVariable) {
        if (type == TaskVariableTypeEnum.EXECUTE_OBJECT_LIST) {
            if (variableReq.getExecuteTarget() == null) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{PARAM_GLOBAL_VAR_LIST,
                        "execute_target is required for host list variable: " + variableReq.getName()});
            }
            return convertVarTarget(variableReq.getExecuteTarget()).toJsonString();
        }
        // 密文变量读接口返回的是掩码，原样写回时视为未修改，保留原值
        if (type.needMask() && existingVariable != null && type.getMask().equals(variableReq.getValue())) {
            return existingVariable.getDefaultValue();
        }
        return variableReq.getValue();
    }

    private boolean defaultChangeable(TaskVariableTypeEnum type) {
        if (type == TaskVariableTypeEnum.EXECUTE_ACCOUNT) {
            return false;
        }
        return true;
    }

    private List<TaskVariableDTO> buildDeletedVariables(List<V4JobTemplateGlobalVarReq> variableReqList,
                                                        Map<String, TaskVariableDTO> existingVariables) {
        Set<String> keptNames = variableReqList == null ? Collections.emptySet() : variableReqList.stream()
            .map(V4JobTemplateGlobalVarReq::getName)
            .collect(Collectors.toSet());
        List<TaskVariableDTO> deletedVariables = new ArrayList<>();
        for (Map.Entry<String, TaskVariableDTO> entry : existingVariables.entrySet()) {
            if (keptNames.contains(entry.getKey())) {
                continue;
            }
            TaskVariableDTO deletedVariable = new TaskVariableDTO();
            deletedVariable.setId(entry.getValue().getId());
            deletedVariable.setName(entry.getKey());
            deletedVariable.setType(entry.getValue().getType());
            deletedVariable.setDelete(true);
            deletedVariables.add(deletedVariable);
        }
        return deletedVariables;
    }

    // ---------------------------------------------------------------- 执行目标与账号

    /**
     * 步骤的执行目标。可以引用全局变量，也可以直接指定目标。
     */
    private TaskTargetDTO convertExecuteTarget(V4JobTemplateExecuteTargetReq targetReq) {
        if (targetReq == null || targetReq.isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"execute_target",
                    "provide at least one of variable, host_list, dynamic_group_list, topo_node_list, "
                        + "container_list and container_filter_list"});
        }
        String variable = StringUtils.trimToNull(targetReq.getVariable());
        // 落库时 variable 与具体目标互斥（见 TaskTargetDTO#toJsonString），同时给会让主机维度被静默丢弃
        if (variable != null && !targetReq.isTargetEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"execute_target",
                    "variable is exclusive with host_list, dynamic_group_list, topo_node_list, "
                        + "container_list and container_filter_list"});
        }
        TaskTargetDTO target = convertTarget(targetReq);
        target.setVariable(variable);
        return target;
    }

    /**
     * 全局变量默认值的执行目标。只能是具体目标，不涉及 variable。
     */
    private TaskTargetDTO convertVarTarget(V4JobTemplateVarTargetReq targetReq) {
        if (targetReq == null || targetReq.isTargetEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{PARAM_GLOBAL_VAR_LIST,
                    "provide at least one of host_list, dynamic_group_list, topo_node_list, "
                        + "container_list and container_filter_list in execute_target"});
        }
        return convertTarget(targetReq);
    }

    private TaskTargetDTO convertTarget(V4JobTemplateTargetReq targetReq) {
        TaskTargetDTO target = new TaskTargetDTO();
        TaskHostNodeDTO hostNode = new TaskHostNodeDTO();
        if (CollectionUtils.isNotEmpty(targetReq.getHostList())) {
            hostNode.setHostList(targetReq.getHostList().stream()
                .map(this::convertHost)
                .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(targetReq.getDynamicGroups())) {
            hostNode.setDynamicGroupId(targetReq.getDynamicGroups().stream()
                .map(EsbDynamicGroupDTO::getId)
                .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(targetReq.getTopoNodes())) {
            hostNode.setNodeInfoList(targetReq.getTopoNodes().stream()
                .map(this::convertTopoNode)
                .collect(Collectors.toList()));
        }
        target.setHostNodeList(hostNode);
        if (CollectionUtils.isNotEmpty(targetReq.getContainerList())) {
            target.setContainerList(targetReq.getContainerList().stream()
                .map(this::convertContainer)
                .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(targetReq.getContainerFilters())) {
            target.setContainerFilters(targetReq.getContainerFilters().stream()
                .map(this::convertContainerFilter)
                .collect(Collectors.toList()));
        }
        TaskTargetDTO.fillHostDetail(target);
        return target;
    }

    /**
     * 静态容器只取 ID 落库，与主机一致不校验归属；容器是否有效由执行时的 CMDB 查询判定。
     * 其余字段是读接口带出的快照，写入时忽略，使响应可原样回传。
     */
    private TaskTargetContainerDTO convertContainer(V4JobTemplateContainerDTO containerReq) {
        TaskTargetContainerDTO container = new TaskTargetContainerDTO();
        container.setId(containerReq.getContainerId());
        return container;
    }

    private KubeContainerFilter convertContainerFilter(V4JobTemplateContainerFilterDTO filterReq) {
        KubeContainerFilter filter = new KubeContainerFilter();
        filter.setName(StringUtils.trimToNull(filterReq.getName()));
        filter.setKubeTopoList(filterReq.getKubeTopoList().stream()
            .map(this::convertKubeTopo)
            .collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(filterReq.getPropConditions())) {
            List<KubePropCondition> propConditions = filterReq.getPropConditions().stream()
                .map(this::convertPropCondition)
                .collect(Collectors.toList());
            KubePropConditionValidator.validate(propConditions);
            filter.setPropConditions(propConditions);
        }
        filter.setEmptyFilter(false);
        filter.setFetchAnyOneContainer(false);
        return filter;
    }

    /**
     * 接口不收运算符，按字段补齐页面固定使用的那一个，保证与页面写入的条件同形。
     */
    private KubePropCondition convertPropCondition(V4JobTemplatePropConditionDTO conditionReq) {
        QueryableContainerField field = QueryableContainerField.fromFieldName(conditionReq.getField());
        KubeContainerOperator operator = OperatorDispatcher.getCanonicalOperator(field);
        if (operator == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"prop_conditions",
                    "unsupported field: " + conditionReq.getField() + ", supported fields are "
                        + OperatorDispatcher.getExposedFields().stream()
                        .map(QueryableContainerField::getFieldName)
                        .collect(Collectors.joining(", "))});
        }
        return new KubePropCondition(field.getFieldName(), operator.getValue(), conditionReq.getValue());
    }

    private KubeTopoDTO convertKubeTopo(V4KubeTopoDTO topoReq) {
        KubeTopoDTO topo = new KubeTopoDTO();
        topo.setCluster(new KubeClusterObjectDTO(topoReq.getCluster().getId()));
        if (topoReq.getNamespace() != null) {
            topo.setNamespace(new KubeNamespaceObjectDTO(topoReq.getNamespace().getId()));
        }
        if (CollectionUtils.isNotEmpty(topoReq.getWorkloads())) {
            topo.setWorkloads(topoReq.getWorkloads().stream()
                .map(workload -> new KubeWorkloadObjectDTO(workload.getKind(), workload.getId()))
                .collect(Collectors.toList()));
        }
        return topo;
    }

    private ApplicationHostDTO convertHost(OpenApiV4HostDTO hostReq) {
        if (hostReq.getBkHostId() == null && StringUtils.isBlank(hostReq.getIp())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"host_list", "provide either bk_host_id or bk_cloud_id with ip"});
        }
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(hostReq.getBkHostId());
        host.setCloudAreaId(hostReq.getBkCloudId());
        host.setIp(hostReq.getIp());
        return host;
    }

    private TaskNodeInfoDTO convertTopoNode(EsbCmdbTopoNodeDTO topoNodeReq) {
        if (topoNodeReq.getId() == null || StringUtils.isBlank(topoNodeReq.getNodeType())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"topo_node_list", "both id and node_type are required"});
        }
        TaskNodeInfoDTO topoNode = new TaskNodeInfoDTO();
        topoNode.setId(topoNodeReq.getId());
        topoNode.setType(topoNodeReq.getNodeType());
        return topoNode;
    }

    private void applyAccount(V4JobTemplateAccountReq accountReq,
                              Consumer<Long> accountIdSetter,
                              Consumer<String> accountVarSetter) {
        if (accountReq == null || accountReq.isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"account", "provide at least one of id and account_var"});
        }
        accountIdSetter.accept(accountReq.getId());
        accountVarSetter.accept(StringUtils.trimToNull(accountReq.getAccountVar()));
    }


    private Map<Long, TaskStepDTO> indexStepsById(List<TaskStepDTO> stepList) {
        if (CollectionUtils.isEmpty(stepList)) {
            return Collections.emptyMap();
        }
        Map<Long, TaskStepDTO> stepMap = new LinkedHashMap<>();
        stepList.forEach(step -> stepMap.put(step.getId(), step));
        return stepMap;
    }

    private Map<String, TaskVariableDTO> indexVariablesByName(List<TaskVariableDTO> variableList) {
        if (CollectionUtils.isEmpty(variableList)) {
            return Collections.emptyMap();
        }
        Map<String, TaskVariableDTO> variableMap = new LinkedHashMap<>();
        variableList.forEach(variable -> variableMap.put(variable.getName(), variable));
        return variableMap;
    }

    private String decodeBase64(String encodedContent, String fieldName) {
        if (StringUtils.isBlank(encodedContent)) {
            return null;
        }
        try {
            return Base64Util.decodeContentToStr(encodedContent);
        } catch (Exception e) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{fieldName, fieldName + " is not a valid base64 encoded string"});
        }
    }

    private boolean toBoolean(Integer flag) {
        return flag != null && flag == 1;
    }

    private String joinIds(Set<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
