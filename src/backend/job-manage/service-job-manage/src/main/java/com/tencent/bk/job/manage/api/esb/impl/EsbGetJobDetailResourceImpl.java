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

package com.tencent.bk.job.manage.api.esb.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.EsbFileSourceDTO;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.esb.EsbGetJobDetailResource;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.EsbJobDetailDTO;
import com.tencent.bk.job.manage.model.esb.EsbStepDTO;
import com.tencent.bk.job.manage.model.esb.EsbTaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.request.EsbGetJobDetailRequest;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class EsbGetJobDetailResourceImpl implements EsbGetJobDetailResource {
    private final TaskPlanService taskPlanService;
    private final MessageI18nService i18nService;
    private final ScriptService scriptService;
    private final AccountService accountService;
    private final AuthService authService;

    public EsbGetJobDetailResourceImpl(TaskPlanService taskPlanService, ScriptService scriptService,
                                       AccountService accountService, MessageI18nService i18nService,
                                       AuthService authService) {
        this.taskPlanService = taskPlanService;
        this.scriptService = scriptService;
        this.accountService = accountService;
        this.i18nService = i18nService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_get_job_detail"})
    public EsbResp<EsbJobDetailDTO> getJobDetail(EsbGetJobDetailRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job detail, request is illegal!");
            throw new InvalidParamException(checkResult);
        }
        Long appId = request.getAppId();
        Long jobId = request.getPlanId();

        TaskPlanInfoDTO taskPlan = taskPlanService.getTaskPlanById(appId, jobId);
        if (taskPlan == null) {
            AuthResult authResult = authService.auth(true, request.getUserName(), ActionId.LIST_BUSINESS,
                ResourceTypeEnum.BUSINESS, request.getAppId().toString(), null);
            if (!authResult.isPass()) {
                return authService.buildEsbAuthFailResp(authResult.getRequiredActionResources());
            } else {
                log.info("Get job detail, job is not exist! appId={}, jobId={}", appId, jobId);
                return EsbResp.buildSuccessResp(null);
            }
        }

        AuthResult authResult = authService.auth(true, request.getUserName(), ActionId.VIEW_JOB_PLAN,
            ResourceTypeEnum.PLAN, request.getPlanId().toString(), PathBuilder
                .newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString())
                .child(ResourceTypeEnum.TEMPLATE.getId(), taskPlan.getTemplateId().toString())
                .build());
        if (!authResult.isPass()) {
            return authService.buildEsbAuthFailResp(authResult.getRequiredActionResources());
        }

        return EsbResp.buildSuccessResp(buildJobDetail(taskPlan));
    }

    private EsbJobDetailDTO buildJobDetail(TaskPlanInfoDTO taskPlan) {
        EsbJobDetailDTO job = new EsbJobDetailDTO();
        job.setCreator(taskPlan.getCreator());
        job.setLastModifyUser(taskPlan.getLastModifyUser());
        job.setAppId(taskPlan.getAppId());
        job.setId(taskPlan.getId());
        job.setName(taskPlan.getName());
        job.setTemplateId(taskPlan.getTemplateId());
        if (taskPlan.getLastModifyTime() != null) {
            job.setLastModifyTime(DateUtils.formatUnixTimestamp(taskPlan.getLastModifyTime(),
                ChronoUnit.SECONDS, "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault()));
        }
        job.setCreateTime(DateUtils.formatUnixTimestamp(taskPlan.getCreateTime(),
            ChronoUnit.SECONDS, "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault()));
        job.setSteps(buildSteps(taskPlan));
        job.setVariables(buildVariables(taskPlan));
        return job;
    }

    private ValidateResult checkRequest(EsbGetJobDetailRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getPlanId() == null || request.getPlanId() < 1) {
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_job_id");
        }
        return ValidateResult.pass();
    }

    private List<EsbStepDTO> buildSteps(TaskPlanInfoDTO plan) {
        List<EsbStepDTO> esbSteps = new ArrayList<>();
        Map<Long, ServiceAccountDTO> cacheAccountMap = new HashMap<>();
        if (plan.getStepList() != null) {
            int PLAN_IS_ENABLE = 1;
            for (TaskStepDTO step : plan.getStepList()) {
                // 过滤掉未使用的步骤
                if (!step.getEnable().equals(PLAN_IS_ENABLE)) {
                    continue;
                }
                esbSteps.add(buildStep(step, cacheAccountMap));
            }
        }
        return esbSteps;
    }

    private EsbStepDTO buildStep(TaskStepDTO step, Map<Long, ServiceAccountDTO> cacheAccountMap) {
        EsbStepDTO esbStep = new EsbStepDTO();
        esbStep.setId(step.getId());
        esbStep.setName(step.getName());
        esbStep.setType(step.getType().getValue());

        TaskStepTypeEnum stepType = step.getType();
        switch (stepType) {
            case SCRIPT:
                buildScriptStep(esbStep, step.getScriptStepInfo(), cacheAccountMap);
                break;
            case FILE:
                buildFileStep(esbStep, step.getFileStepInfo(), cacheAccountMap);
                break;
            case APPROVAL:
                buildApprovalStep(esbStep, step.getApprovalStepInfo());
                break;
            default:
                log.warn("Step type is illegal!");
                break;
        }
        return esbStep;
    }

    private void buildScriptStep(EsbStepDTO esbStep, TaskScriptStepDTO scriptStep,
                                 Map<Long, ServiceAccountDTO> cacheAccountMap) {
        if (scriptStep == null) {
            return;
        }
        esbStep.setScriptId(scriptStep.getScriptVersionId());
        esbStep.setType(scriptStep.getScriptSource().getType());

        if (scriptStep.getScriptVersionId() != null && scriptStep.getScriptVersionId() > 0) {
            ScriptDTO script = scriptService.getScriptVersion(scriptStep.getScriptVersionId());
            if (script == null) {
                log.warn("Plan related script is not exist, planId={}, scriptVersionId={}", scriptStep.getPlanId(),
                    scriptStep.getScriptVersionId());
                esbStep.setScriptContent(null);
            } else {
                esbStep.setScriptContent(script.getContent());
            }
        } else {
            esbStep.setScriptContent(scriptStep.getContent());
        }

        esbStep.setScriptParam(scriptStep.getScriptParam());
        esbStep.setScriptTimeout(scriptStep.getTimeout());
        esbStep.setIsParamSensive(scriptStep.getSecureParam() ? 1 : 0);

        ServiceAccountDTO account = buildAccount(scriptStep.getAccount(), cacheAccountMap);
        if (account == null) {
            log.error("Script account is empty! planId:{}, stepId:{}", scriptStep.getPlanId(), scriptStep.getId());
            esbStep.setAccount(null);
        } else {
            esbStep.setAccount(account.getAlias());
            esbStep.setAccountId(account.getId());
        }
        fillStepTargetServerInfo(esbStep, scriptStep.getExecuteTarget());
    }

    private void fillStepTargetServerInfo(EsbStepDTO esbStep, TaskTargetDTO targetServer) {
        if (targetServer != null && StringUtils.isEmpty(targetServer.getVariable()) &&
            targetServer.getHostNodeList() != null) {
            TaskHostNodeDTO taskHostNode = targetServer.getHostNodeList();
            esbStep.setIpList(convertToEsbIpDTOList(taskHostNode.getHostList()));
            if (taskHostNode.getDynamicGroupId() != null && !taskHostNode.getDynamicGroupId().isEmpty()) {
                esbStep.setCustomQueryId(taskHostNode.getDynamicGroupId());
            }
            if (taskHostNode.getNodeInfoList() != null && !taskHostNode.getNodeInfoList().isEmpty()) {
                //TODO 暂不处理，后续添加
            }
        }
    }

    private List<EsbIpDTO> convertToEsbIpDTOList(List<ApplicationHostInfoDTO> hostList) {
        List<EsbIpDTO> ipList = new ArrayList<>();
        if (hostList != null && !hostList.isEmpty()) {
            hostList.forEach(host -> {
                EsbIpDTO ipDTO = new EsbIpDTO(host.getCloudAreaId(), host.getIp());
                ipList.add(ipDTO);
            });
        }
        return ipList;
    }


    private ServiceAccountDTO buildAccount(Long accountId, Map<Long, ServiceAccountDTO> cacheAccountMap) {
        ServiceAccountDTO cacheAccount = cacheAccountMap.get(accountId);
        if (cacheAccount != null) {
            return cacheAccount;
        } else {
            AccountDTO account = accountService.getAccountById(accountId);
            ServiceAccountDTO serviceAccountDTO = account.toServiceAccountDTO();
            cacheAccountMap.put(accountId, serviceAccountDTO);
            return serviceAccountDTO;
        }
    }

    private void buildFileStep(EsbStepDTO esbStep, TaskFileStepDTO fileStep,
                               Map<Long, ServiceAccountDTO> cacheAccountMap) {
        if (fileStep == null) {
            return;
        }
        List<TaskFileInfoDTO> originFileList = fileStep.getOriginFileList();
        List<EsbFileSourceDTO> fileSources = new ArrayList<>();
        originFileList.forEach(originFile -> {
            EsbFileSourceDTO fileSource = new EsbFileSourceDTO();
            fileSource.setFiles(originFile.getFileLocation());
            fileSource.setFileType(originFile.getFileType().getType());
            fileSource.setFileSourceId(originFile.getFileSourceId());
            // 本地文件分发、文件源分发不需要设置账号和服务器信息
            if (originFile.getFileType() == TaskFileTypeEnum.SERVER) {
                fileSource.setAccount(buildAccount(originFile.getHostAccount(), cacheAccountMap).getAlias());
                TaskTargetDTO targetServer = originFile.getHost();
                // TODO 需要新增源文件服务器支持动态节点/主机变量的参数
                if (targetServer != null) {
                    if (StringUtils.isEmpty(targetServer.getVariable()) && targetServer.getHostNodeList() != null) {
                        fileSource.setIpList(convertToEsbIpDTOList(targetServer.getHostNodeList().getHostList()));
                        fileSource.setDynamicGroupIdList(targetServer.getHostNodeList().getDynamicGroupId());
                    }
                }
            }
            fileSources.add(fileSource);
        });
        esbStep.setFileSources(fileSources);

        esbStep.setAccount(buildAccount(fileStep.getExecuteAccount(), cacheAccountMap).getAlias());
        TaskTargetDTO targetServer = fileStep.getDestinationHostList();
        fillStepTargetServerInfo(esbStep, targetServer);

        esbStep.setFileTargetPath(fileStep.getDestinationFileLocation());
    }

    private void buildApprovalStep(EsbStepDTO esbStep, TaskApprovalStepDTO approvalStep) {
        esbStep.setConfirmMessage(approvalStep.getApprovalMessage());
    }

    private List<EsbTaskVariableDTO> buildVariables(TaskPlanInfoDTO plan) {
        if (plan.getVariableList() == null) {
            return null;
        }
        List<EsbTaskVariableDTO> variableDTOS = new ArrayList<>();
        plan.getVariableList().forEach(variableDTO -> {
            variableDTOS.add(buildVariable(variableDTO));
        });
        return variableDTOS;
    }

    private EsbTaskVariableDTO buildVariable(TaskVariableDTO variable) {
        EsbTaskVariableDTO variableDTO = new EsbTaskVariableDTO();
        variableDTO.setId(variable.getId());
        variableDTO.setName(variable.getName());
        variableDTO.setValue(variable.getDefaultValue());
        variableDTO.setCategory(variable.getType().getType());
        if (variable.getType() == TaskVariableTypeEnum.HOST_LIST
            && StringUtils.isNotBlank(variable.getDefaultValue())) {
            TaskTargetDTO target = TaskTargetDTO.fromString(variable.getDefaultValue());
            if (target == null) {
                log.error("Variable target is empty! variableId:{}", variable.getId());
                return variableDTO;
            }
            variableDTO.setIpList(convertToEsbIpDTOList(target.getHostNodeList().getHostList()));
            variableDTO.setCustomQueryId(target.getHostNodeList().getDynamicGroupId());
        }
        variableDTO.setDescription(variable.getDescription());
        return variableDTO;
    }

}
