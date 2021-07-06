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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.converter.TaskStepConverter;
import com.tencent.bk.job.manage.model.dto.converter.TaskVariableConverter;
import com.tencent.bk.job.manage.model.dto.task.*;
import com.tencent.bk.job.manage.model.inner.*;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceTaskPlanResourceImpl implements ServiceTaskPlanResource {
    private static final Integer PLAN_IS_ENABLE = 1;
    private final TaskPlanService taskPlanService;
    private final AbstractTaskVariableService taskVariableService;

    private final ScriptService scriptService;

    private final AccountService accountService;

    private final MessageI18nService i18nService;

    @Autowired
    public ServiceTaskPlanResourceImpl(
        TaskPlanService taskPlanService,
        @Qualifier("TaskPlanVariableServiceImpl") AbstractTaskVariableService taskVariableService,
        ScriptService scriptService,
        AccountService accountService,
        MessageI18nService i18nService) {
        this.taskPlanService = taskPlanService;
        this.taskVariableService = taskVariableService;
        this.scriptService = scriptService;
        this.accountService = accountService;
        this.i18nService = i18nService;
    }

    @Override
    public ServiceResponse<ServiceTaskPlanDTO> getPlanBasicInfoById(Long appId, Long planId) {
        if (appId == null || appId <= 0 || planId == null || planId <= 0) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        List<TaskPlanInfoDTO> planList =
            taskPlanService.listPlanBasicInfoByIds(appId, Collections.singletonList(planId));
        if (CollectionUtils.isNotEmpty(planList)) {
            TaskPlanInfoDTO plan = planList.get(0);
            ServiceTaskPlanDTO planDTO = new ServiceTaskPlanDTO();
            planDTO.setId(plan.getId());
            planDTO.setTaskTemplateId(plan.getTemplateId());
            planDTO.setCreator(plan.getCreator());
            planDTO.setName(plan.getName());
            planDTO.setDebugTask(plan.getDebug());
            return ServiceResponse.buildSuccessResp(planDTO);
        } else {
            return ServiceResponse.buildCommonFailResp(ErrorCode.TASK_PLAN_NOT_EXIST, i18nService);
        }
    }

    @Override
    public ServiceResponse<String> getPlanName(Long planId) {
        if (planId == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        return ServiceResponse.buildSuccessResp(taskPlanService.getPlanName(planId));
    }

    @Override
    public ServiceResponse<Long> getGlobalVarIdByName(Long planId, String globalVarName) {
        TaskVariableDTO taskVariableDTO = taskVariableService.getVariableByName(planId, globalVarName);
        if (taskVariableDTO == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                "Cannot find globalVar by name " + globalVarName);
        }
        return ServiceResponse.buildSuccessResp(taskVariableDTO.getId());
    }

    @Override
    public ServiceResponse<String> getGlobalVarNameById(Long planId, Long globalVarId) {
        TaskVariableDTO taskVariableDTO = taskVariableService.getVariableById(planId, globalVarId);
        if (taskVariableDTO == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                "Cannot find globalVar by id " + globalVarId);
        }
        return ServiceResponse.buildSuccessResp(taskVariableDTO.getName());
    }

    @Override
    public ServiceResponse<Long> getPlanAppId(Long planId) {
        if (planId == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        TaskPlanInfoDTO taskPlanInfoDTO = taskPlanService.getTaskPlanById(planId);
        if (taskPlanInfoDTO == null) {
            log.warn("Cannot find taskPlanInfoDTO by id {}", planId);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        return ServiceResponse.buildSuccessResp(taskPlanInfoDTO.getAppId());
    }

    @Override
    public ServiceResponse<ServiceTaskPlanDTO> getPlanById(Long appId, Long planId, Boolean includeDisabledSteps) {
        TaskPlanInfoDTO plan = taskPlanService.getTaskPlanById(appId, planId);
        log.info("Get plan by planId, planId={}, plan={}", planId, JsonUtils.toJson(plan));
        if (plan == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"planId", "Cannot find plan by id " + planId});
        }
        ServiceTaskPlanDTO planDTO = new ServiceTaskPlanDTO();
        planDTO.setId(plan.getId());
        planDTO.setTaskTemplateId(plan.getTemplateId());
        planDTO.setCreator(plan.getCreator());
        planDTO.setName(plan.getName());
        planDTO.setDebugTask(plan.getDebug());

        planDTO.setStepList(buildSteps(plan, includeDisabledSteps));

        planDTO.setVariableList(buildVariables(plan));

        return ServiceResponse.buildSuccessResp(planDTO);
    }

    @Override
    public ServiceResponse<Long> createPlanWithIdForMigration(
        String username,
        Long appId,
        Long templateId,
        Long planId,
        Long createTime,
        Long lastModifyTime,
        String lastModifyUser
    ) {
        return ServiceResponse.buildSuccessResp(taskPlanService.saveTaskPlanForMigration(username, appId, templateId,
            planId, createTime, lastModifyTime, lastModifyUser));
    }

    @Override
    public ServiceResponse<ServiceIdNameCheckDTO> checkIdAndName(Long appId, Long templateId, Long planId,
                                                                 String name) {
        boolean idResult = taskPlanService.checkPlanId(planId);
        boolean nameResult = taskPlanService.checkPlanName(appId, templateId, 0L, name);

        ServiceIdNameCheckDTO idNameCheck = new ServiceIdNameCheckDTO();
        idNameCheck.setIdCheckResult(idResult ? 1 : 0);
        idNameCheck.setNameCheckResult(nameResult ? 1 : 0);
        return ServiceResponse.buildSuccessResp(idNameCheck);
    }

    @Override
    public ServiceResponse<Long> savePlanForImport(String username, Long appId, Long templateId,
                                                   Long createTime, TaskPlanVO planInfo) {
        if (planInfo.validateForImport()) {
            TaskPlanInfoDTO taskPlanInfo = TaskPlanInfoDTO.fromVO(username, appId, planInfo);
            if (createTime != null && createTime > 0) {
                taskPlanInfo.setCreateTime(createTime);
            }
            Long finalTemplateId = taskPlanService.saveTaskPlanForBackup(taskPlanInfo);
            return ServiceResponse.buildSuccessResp(finalTemplateId);
        } else {
            return ServiceResponse
                .buildCommonFailResp("Valid param failed!" + JsonUtils.toJson(JobContextUtil.getDebugMessage()));
        }
    }

    @Override
    public ServiceResponse<List<ServiceTaskVariableDTO>> getPlanVariable(String username, Long appId, Long templateId,
                                                                         Long planId) {
        List<TaskVariableDTO> taskVariableList = taskVariableService.listVariablesByParentId(planId);
        if (CollectionUtils.isNotEmpty(taskVariableList)) {
            List<ServiceTaskVariableDTO> variableList =
                taskVariableList.parallelStream().map(TaskVariableDTO::toServiceDTO).collect(Collectors.toList());
            return ServiceResponse.buildSuccessResp(variableList);
        }
        return ServiceResponse.buildSuccessResp(Collections.emptyList());
    }

    @Override
    public ServiceResponse<List<ServiceTaskPlanDTO>> listPlans(String username, Long appId, Long templateId) {
        List<TaskPlanInfoDTO> taskPlanInfoDTOList = taskPlanService.listPageTaskPlansBasicInfo(appId, templateId);
        List<ServiceTaskPlanDTO> resultList = taskPlanInfoDTOList.parallelStream().map(it -> {
            ServiceTaskPlanDTO serviceTaskPlanDTO = new ServiceTaskPlanDTO();
            serviceTaskPlanDTO.setId(it.getId());
            serviceTaskPlanDTO.setName(it.getName());
            serviceTaskPlanDTO.setCreator(it.getCreator());
            serviceTaskPlanDTO.setTaskTemplateId(it.getTemplateId());
            serviceTaskPlanDTO.setDebugTask(it.getDebug());

            List<TaskStepDTO> taskStepDTOList = it.getStepList();
            List<ServiceTaskStepDTO> serviceTaskStepDTOList = new ArrayList<>();
            if (taskStepDTOList != null && !taskStepDTOList.isEmpty()) {
                serviceTaskStepDTOList =
                    taskStepDTOList.parallelStream()
                        .map(TaskStepConverter::convertToServiceTaskStepDTO).collect(Collectors.toList());
            }
            serviceTaskPlanDTO.setStepList(serviceTaskStepDTOList);

            List<TaskVariableDTO> variableList = it.getVariableList();
            List<ServiceTaskVariableDTO> serviceTaskVariableDTOList = new ArrayList<>();
            if (variableList != null && !variableList.isEmpty()) {
                serviceTaskVariableDTOList =
                    variableList.parallelStream()
                        .map(TaskVariableConverter::convertToServiceTaskVariableDTO)
                        .collect(Collectors.toList());
            }
            serviceTaskPlanDTO.setVariableList(serviceTaskVariableDTOList);
            return serviceTaskPlanDTO;
        }).collect(Collectors.toList());
        return ServiceResponse.buildSuccessResp(resultList);
    }

    @Override
    public ServiceResponse<List<Long>> listPlanIds(Long templateId) {
        return ServiceResponse.buildSuccessResp(taskPlanService.listTaskPlanIds(templateId));
    }

    private List<ServiceTaskStepDTO> buildSteps(TaskPlanInfoDTO plan, Boolean includeDisabledSteps) {
        List<ServiceTaskStepDTO> stepDTOS = new ArrayList<>();
        Map<Long, ServiceAccountDTO> cacheAccountMap = new HashMap<>();
        if (plan.getStepList() != null) {
            for (TaskStepDTO step : plan.getStepList()) {
                // 过滤掉未使用的步骤
                if ((includeDisabledSteps == null || !includeDisabledSteps)
                    && !step.getEnable().equals(PLAN_IS_ENABLE)) {
                    continue;
                }
                stepDTOS.add(buildStep(step, cacheAccountMap));
            }
        }
        return stepDTOS;
    }

    private ServiceTaskStepDTO buildStep(TaskStepDTO step, Map<Long, ServiceAccountDTO> cacheAccountMap) {
        ServiceTaskStepDTO stepDTO = new ServiceTaskStepDTO();
        stepDTO.setId(step.getId());
        stepDTO.setName(step.getName());
        stepDTO.setType(step.getType().getValue());

        TaskStepTypeEnum stepType = step.getType();
        switch (stepType) {
            case SCRIPT:
                stepDTO.setScriptStepInfo(buildScriptStep(step.getScriptStepInfo(), cacheAccountMap));
                break;
            case FILE:
                stepDTO.setFileStepInfo(buildFileStep(step.getFileStepInfo(), cacheAccountMap));
                break;
            case APPROVAL:
                stepDTO.setApprovalStepInfo(buildApprovalStep(step.getApprovalStepInfo()));
                break;
            default:
                log.warn("Step type is illegal!");
                break;
        }
        return stepDTO;
    }

    private ServiceTaskScriptStepDTO buildScriptStep(TaskScriptStepDTO scriptStep,
                                                     Map<Long, ServiceAccountDTO> cacheAccountMap) {
        if (scriptStep == null) {
            return null;
        }
        ServiceTaskScriptStepDTO scriptStepDTO = new ServiceTaskScriptStepDTO();
        scriptStepDTO.setScriptId(scriptStep.getScriptId());
        scriptStepDTO.setScriptVersionId(scriptStep.getScriptVersionId());
        scriptStepDTO.setScriptSource(scriptStep.getScriptSource().getType());
        scriptStepDTO.setType(scriptStep.getLanguage().getValue());

        if (scriptStep.getScriptVersionId() != null && scriptStep.getScriptVersionId() > 0) {
            ScriptDTO script = scriptService.getByScriptVersionId(scriptStep.getScriptVersionId());
            if (script == null) {
                log.warn("Plan related script is not exist, planId={}, scriptVersionId={}", scriptStep.getPlanId(),
                    scriptStep.getScriptVersionId());
                scriptStepDTO.setContent(null);
            } else {
                scriptStepDTO.setContent(script.getContent());
                scriptStepDTO.setScriptStatus(script.getStatus());
            }
        } else {
            scriptStepDTO.setContent(scriptStep.getContent());
        }

        scriptStepDTO.setScriptParam(scriptStep.getScriptParam());
        scriptStepDTO.setScriptTimeout(scriptStep.getTimeout());
        scriptStepDTO.setSecureParam(scriptStep.getSecureParam());

        scriptStepDTO.setAccount(buildAccount(scriptStep.getAccount(), cacheAccountMap));

        TaskTargetDTO targetServer = scriptStep.getExecuteTarget();
        scriptStepDTO.setExecuteTarget(targetServer.toServiceTaskTargetDTO());
        scriptStepDTO.setIgnoreError(scriptStep.getIgnoreError());
        return scriptStepDTO;
    }

    private ServiceAccountDTO buildAccount(Long accountId, Map<Long, ServiceAccountDTO> cacheAccountMap) {
        ServiceAccountDTO cacheAccount = cacheAccountMap.get(accountId);
        if (cacheAccount != null) {
            return cacheAccount;
        } else {
            AccountDTO account = accountService.getAccountById(accountId);
            if (account != null) {
                ServiceAccountDTO serviceAccountDTO = account.toServiceAccountDTO();
                setDependedSystemAccountForDbAccount(serviceAccountDTO);
                cacheAccountMap.put(accountId, serviceAccountDTO);
                return serviceAccountDTO;
            }
        }
        return null;
    }

    private void setDependedSystemAccountForDbAccount(ServiceAccountDTO account) {
        if (account.getCategory().equals(AccountCategoryEnum.DB.getValue())) {
            ServiceAccountDTO dependedSystemAccount = account.getDbSystemAccount();
            AccountDTO systemAccount = accountService.getAccountById(dependedSystemAccount.getId());
            if (systemAccount != null) {
                dependedSystemAccount = systemAccount.toServiceAccountDTO();
                account.setDbSystemAccount(dependedSystemAccount);
            }
        }
    }

    private ServiceTaskFileStepDTO buildFileStep(TaskFileStepDTO fileStep,
                                                 Map<Long, ServiceAccountDTO> cacheAccountMap) {
        if (fileStep == null) {
            return null;
        }
        ServiceTaskFileStepDTO fileStepDTO = new ServiceTaskFileStepDTO();

        List<TaskFileInfoDTO> originFileList = fileStep.getOriginFileList();
        List<ServiceTaskFileInfoDTO> originFileDTOS = new ArrayList<>();
        originFileList.forEach(originFile -> {
            ServiceTaskFileInfoDTO originFileDTO = new ServiceTaskFileInfoDTO();
            originFileDTO.setId(originFile.getId());
            originFileDTO.setFileHash(originFile.getFileHash());
            originFileDTO.setFileSize(originFile.getFileSize());
            originFileDTO.setFileSourceId(originFile.getFileSourceId());
            originFileDTO.setFileLocation(originFile.getFileLocation());
            originFileDTO.setFileType(originFile.getFileType().getType());
            if (originFile.getFileType() == TaskFileTypeEnum.LOCAL) {
                // 本地文件分发不需要设置账号和服务器信息
                originFileDTO.setExecuteTarget(null);
                originFileDTO.setAccount(null);
            } else {
                originFileDTO.setAccount(buildAccount(originFile.getHostAccount(), cacheAccountMap));
                TaskTargetDTO targetServer = originFile.getHost();
                originFileDTO.setExecuteTarget(targetServer.toServiceTaskTargetDTO());
            }
            originFileDTOS.add(originFileDTO);
        });
        fileStepDTO.setOriginFileList(originFileDTOS);

        fileStepDTO.setAccount(buildAccount(fileStep.getExecuteAccount(), cacheAccountMap));
        fileStepDTO.setFileDuplicateHandle(fileStep.getDuplicateHandler().getId());
        fileStepDTO.setNotExistPathHandler(fileStep.getNotExistPathHandler().getValue());

        TaskTargetDTO targetServer = fileStep.getDestinationHostList();
        fileStepDTO.setExecuteTarget(targetServer.toServiceTaskTargetDTO());

        fileStepDTO.setDestinationFileLocation(fileStep.getDestinationFileLocation());

        fileStepDTO.setIgnoreError(fileStep.getIgnoreError());
        fileStepDTO.setTimeout(fileStep.getTimeout() == null ? null : fileStep.getTimeout().intValue());
        fileStepDTO.setDownloadSpeedLimit(
            fileStep.getTargetSpeedLimit() == null ? null : fileStep.getTargetSpeedLimit().intValue());
        fileStepDTO.setUploadSpeedLimit(
            (fileStep.getOriginSpeedLimit() == null ? null : fileStep.getOriginSpeedLimit().intValue()));
        return fileStepDTO;
    }

    private ServiceTaskApprovalStepDTO buildApprovalStep(TaskApprovalStepDTO approvalStep) {
        ServiceTaskApprovalStepDTO approvalStepDTO = new ServiceTaskApprovalStepDTO();
        approvalStepDTO.setApprovalType(approvalStep.getApprovalType().getType());
        ServiceTaskApprovalUserDTO approvalUserDTO = new ServiceTaskApprovalUserDTO();
        approvalUserDTO.setUserList(approvalStep.getApprovalUser().getUserList());
        approvalUserDTO.setRoleList(approvalStep.getApprovalUser().getRoleList());
        approvalStepDTO.setApprovalUser(approvalUserDTO);
        approvalStepDTO.setApprovalMessage(approvalStep.getApprovalMessage());
        approvalStepDTO.setChannels(approvalStep.getNotifyChannel());
        return approvalStepDTO;
    }

    private List<ServiceTaskVariableDTO> buildVariables(TaskPlanInfoDTO plan) {
        if (plan.getVariableList() == null) {
            return null;
        }
        List<ServiceTaskVariableDTO> variableDTOS = new ArrayList<>();
        plan.getVariableList().forEach(variableDTO -> {
            variableDTOS.add(buildVariable(variableDTO));
        });
        return variableDTOS;
    }

    private ServiceTaskVariableDTO buildVariable(TaskVariableDTO variable) {
        ServiceTaskVariableDTO variableDTO = new ServiceTaskVariableDTO();
        variableDTO.setId(variable.getId());
        variableDTO.setName(variable.getName());
        variableDTO.setChangeable(variable.getChangeable());
        variableDTO.setDefaultValue(variable.getDefaultValue());
        variableDTO.setRequired(variable.getRequired());
        variableDTO.setType(variable.getType().getType());
        if (variable.getType() == TaskVariableTypeEnum.HOST_LIST
            && StringUtils.isNotBlank(variable.getDefaultValue())) {
            variableDTO
                .setDefaultTargetValue(TaskTargetDTO.fromString(variable.getDefaultValue()).toServiceTaskTargetDTO());
        }
        return variableDTO;
    }
}
