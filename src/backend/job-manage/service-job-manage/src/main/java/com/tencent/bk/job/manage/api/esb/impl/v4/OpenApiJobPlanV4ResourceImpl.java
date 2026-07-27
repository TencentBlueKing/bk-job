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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiJobPlanV4Resource;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.OpenApiV4JobPlanDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class OpenApiJobPlanV4ResourceImpl implements OpenApiJobPlanV4Resource {

    private final TaskPlanService planService;
    private final TaskTemplateService templateService;
    private final TemplateAuthService templateAuthService;
    private final PlanAuthService planAuthService;
    private final AppScopeMappingService appScopeMappingService;
    private final TenantHostService tenantHostService;
    private final AccountService accountService;

    @Autowired
    public OpenApiJobPlanV4ResourceImpl(TaskPlanService planService,
                                        TaskTemplateService templateService,
                                        TemplateAuthService templateAuthService,
                                        PlanAuthService planAuthService,
                                        AppScopeMappingService appScopeMappingService,
                                        TenantHostService tenantHostService,
                                        AccountService accountService) {
        this.planService = planService;
        this.templateService = templateService;
        this.templateAuthService = templateAuthService;
        this.planAuthService = planAuthService;
        this.appScopeMappingService = appScopeMappingService;
        this.tenantHostService = tenantHostService;
        this.accountService = accountService;
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_JOB_PLAN)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_create_job_plan"})
    public EsbV4Response<OpenApiV4JobPlanDTO> createJobPlan(String username,
                                                        String appCode,
                                                        @AuditRequestBody V4CreateJobPlanRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        AppResourceScope appResourceScope = request.getAppResourceScope();

        User user = JobContextUtil.getUser();
        templateAuthService.authViewJobTemplate(user, appResourceScope, request.getJobTemplateId())
            .denyIfNoPermission();
        planAuthService.authCreateJobPlan(user, appResourceScope, request.getJobTemplateId(), null)
            .denyIfNoPermission();

        TaskTemplateInfoDTO template = templateService.getTaskTemplateById(appId, request.getJobTemplateId());
        if (template == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        List<Long> enableSteps = resolveEnableSteps(request, template);
        List<TaskVariableDTO> variableList = mapVariables(request.getVariables(), template, appId, user.getTenantId());

        String planName = StringUtils.strip(request.getName());
        if (Boolean.FALSE.equals(
            planService.checkPlanName(appId, request.getJobTemplateId(), 0L, planName)
        )) {
            throw new AlreadyExistsException(ErrorCode.PLAN_NAME_EXIST);
        }

        TaskPlanInfoDTO planInfoDTO = buildTaskPlanInfoDTO(
            username, appId, request.getJobTemplateId(), planName, enableSteps, variableList
        );

        TaskPlanInfoDTO savedPlan = planService.createTaskPlan(user, planInfoDTO);

        return EsbV4Response.success(toOpenApiV4JobPlanDTO(appId, username, savedPlan));
    }

    private List<Long> resolveEnableSteps(V4CreateJobPlanRequest request, TaskTemplateInfoDTO template) {
        List<Long> templateStepIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(template.getStepList())) {
            for (TaskStepDTO step : template.getStepList()) {
                templateStepIds.add(step.getId());
            }
        }
        if (request.getEnableSteps() == null) {
            return templateStepIds;
        }
        Set<Long> templateStepIdSet = new HashSet<>(templateStepIds);
        for (Long stepId : request.getEnableSteps()) {
            if (stepId == null || !templateStepIdSet.contains(stepId)) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{
                        "enable_steps",
                        "step id " + stepId + " is not in template " + template.getId()
                    }
                );
            }
        }
        return new ArrayList<>(request.getEnableSteps());
    }

    private List<TaskVariableDTO> mapVariables(List<V4JobPlanVariableItem> variables,
                                               TaskTemplateInfoDTO template,
                                               Long appId,
                                               String tenantId) {
        if (CollectionUtils.isEmpty(variables)) {
            return new ArrayList<>();
        }
        Map<String, TaskVariableDTO> templateVarByName = new HashMap<>();
        if (CollectionUtils.isNotEmpty(template.getVariableList())) {
            for (TaskVariableDTO variable : template.getVariableList()) {
                templateVarByName.put(variable.getName(), variable);
            }
        }
        List<TaskVariableDTO> result = new ArrayList<>(variables.size());
        Set<String> seenNames = new HashSet<>();
        for (V4JobPlanVariableItem item : variables) {
            String name = item.getName();
            if (!seenNames.add(name)) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{"variables", "duplicated variable name: " + name}
                );
            }
            TaskVariableDTO templateVar = templateVarByName.get(name);
            if (templateVar == null) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{"variables", "variable name '" + name + "' not exist in template"}
                );
            }
            TaskVariableDTO dto = new TaskVariableDTO();
            dto.setId(templateVar.getId());
            dto.setName(templateVar.getName());
            dto.setDescription(templateVar.getDescription() == null ? "" : templateVar.getDescription());
            dto.setChangeable(templateVar.getChangeable());
            dto.setRequired(templateVar.getRequired());
            dto.setDelete(false);
            dto.setFollowTemplate(item.isFollowTemplate());
            TaskVariableTypeEnum varType = templateVar.getType();
            dto.setType(varType);
            if (varType == TaskVariableTypeEnum.EXECUTE_OBJECT_LIST) {
                if (item.isFollowTemplate()) {
                    if (item.getExecuteTarget() != null) {
                        throw new InvalidParamException(
                            ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                            new Object[]{
                                "variables[].execute_target",
                                "execute_target must not be provided when follow_template is true"
                            }
                        );
                    }
                } else {
                    if (item.getValue() != null) {
                        throw new InvalidParamException(
                            ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                            new Object[]{
                                "variables",
                                "EXECUTE_OBJECT_LIST variable must use execute_target instead of value"
                            }
                        );
                    }
                    TaskTargetDTO taskTargetDTO = buildTaskTargetDTO(item.getExecuteTarget(), tenantId);
                    dto.setDefaultValue(taskTargetDTO.toJsonString());
                }
            } else if (!item.isFollowTemplate() && item.getValue() != null) {
                String value = item.getValue();
                if (varType == TaskVariableTypeEnum.EXECUTE_ACCOUNT) {
                    value = validateExecuteAccountVariableValue(appId, value);
                }
                dto.setDefaultValue(value);
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * 校验执行账号全局变量值：保存账号 ID 字符串。
     */
    private String validateExecuteAccountVariableValue(Long appId, String value) {
        String accountValue = StringUtils.trim(value);
        if (StringUtils.isBlank(accountValue)) {
            return accountValue;
        }
        Long accountId;
        try {
            accountId = Long.valueOf(accountValue);
        } catch (NumberFormatException e) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{"variables", "execute account variable value must be account id"}
            );
        }
        if (accountId <= 0) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{"variables", "execute account variable value must be positive account id"}
            );
        }
        accountService.getAccount(appId, accountId);
        return accountValue;
    }

    /** 执行目标变量覆盖：仅主机维度，容器 filter 暂不支持。 */
    private TaskTargetDTO buildTaskTargetDTO(V4ExecuteTargetDTO v4, String tenantId) {
        if (v4 == null) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "variables[].execute_target",
                    "execute_target is required for EXECUTE_OBJECT_LIST variable"
                }
            );
        }
        if (CollectionUtils.isNotEmpty(v4.getKubeContainerFilters())) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "variables[].execute_target.kube_container_filters",
                    "container target is not supported by this API"
                }
            );
        }
        boolean hostDimensionEmpty = CollectionUtils.isEmpty(v4.getHostList())
            && CollectionUtils.isEmpty(v4.getDynamicGroups())
            && CollectionUtils.isEmpty(v4.getTopoNodes());
        if (hostDimensionEmpty) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "variables[].execute_target",
                    "at least one of host_list/dynamic_group_list/topo_node_list is required"
                }
            );
        }
        TaskHostNodeDTO hostNode = new TaskHostNodeDTO();
        if (CollectionUtils.isNotEmpty(v4.getHostList())) {
            hostNode.setHostList(resolveHostList(v4.getHostList(), tenantId));
        }
        if (CollectionUtils.isNotEmpty(v4.getDynamicGroups())) {
            hostNode.setDynamicGroupId(v4.getDynamicGroups().stream()
                .map(EsbDynamicGroupDTO::getId)
                .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(v4.getTopoNodes())) {
            hostNode.setNodeInfoList(v4.getTopoNodes().stream()
                .map(OpenApiJobPlanV4ResourceImpl::toTaskNodeInfoDTO)
                .collect(Collectors.toList()));
        }
        return new TaskTargetDTO(null, hostNode, null);
    }

    /**
     * 将 OpenAPI 主机列表解析为 {@link ApplicationHostDTO} 列表，并补全 hostId。
     *
     * <p>已带 bk_host_id 的直接使用；仅传 bk_cloud_id+ip 的批量从 CMDB（经 TenantHostService 缓存兜底）反查 hostId。
     * 未能解析到 hostId 的主机会抛 {@link InvalidParamException}，避免创建出页面回显"主机无效"的执行方案。
     *
     * @param hosts    入参主机列表，已由 {@link com.tencent.bk.job.execute.model.esb.v4.req.validator.V4HostGroupSequenceProvider}
     *                 保证至少含有 bk_host_id 或 bk_cloud_id+ip
     * @param tenantId 当前请求租户 ID
     * @return 已补全 hostId 的主机列表
     */
    private List<ApplicationHostDTO> resolveHostList(List<OpenApiV4HostDTO> hosts, String tenantId) {
        List<ApplicationHostDTO> result = new ArrayList<>(hosts.size());
        Set<String> cloudIpsToResolve = new HashSet<>();
        for (OpenApiV4HostDTO host : hosts) {
            ApplicationHostDTO dto = new ApplicationHostDTO();
            if (host.getBkHostId() != null) {
                dto.setHostId(host.getBkHostId());
            } else {
                dto.setCloudAreaId(host.getBkCloudId());
                dto.setIp(host.getIp());
                cloudIpsToResolve.add(dto.getCloudIp());
            }
            result.add(dto);
        }
        if (cloudIpsToResolve.isEmpty()) {
            return result;
        }

        Map<String, ApplicationHostDTO> hostsFromCmdb =
            tenantHostService.listHostsByIps(tenantId, cloudIpsToResolve);
        List<String> missingCloudIps = new ArrayList<>();
        for (ApplicationHostDTO dto : result) {
            if (dto.getHostId() != null) {
                continue;
            }
            ApplicationHostDTO cmdbHost = hostsFromCmdb == null ? null : hostsFromCmdb.get(dto.getCloudIp());
            if (cmdbHost == null || cmdbHost.getHostId() == null) {
                missingCloudIps.add(dto.getCloudIp());
                continue;
            }
            dto.setHostId(cmdbHost.getHostId());
        }
        if (!missingCloudIps.isEmpty()) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "variables[].execute_target.host_list",
                    "host not found in cmdb by cloud_id+ip: " + String.join(",", missingCloudIps)
                }
            );
        }
        return result;
    }

    private static TaskNodeInfoDTO toTaskNodeInfoDTO(EsbCmdbTopoNodeDTO topoNode) {
        TaskNodeInfoDTO nodeInfo = new TaskNodeInfoDTO();
        nodeInfo.setId(topoNode.getId());
        nodeInfo.setType(topoNode.getNodeType());
        return nodeInfo;
    }

    private TaskPlanInfoDTO buildTaskPlanInfoDTO(String username,
                                                 Long appId,
                                                 Long templateId,
                                                 String name,
                                                 List<Long> enableSteps,
                                                 List<TaskVariableDTO> variableList) {
        TaskPlanInfoDTO planInfo = new TaskPlanInfoDTO();
        planInfo.setAppId(appId);
        planInfo.setTemplateId(templateId);
        planInfo.setName(name);
        planInfo.setCreator(username);
        planInfo.setLastModifyUser(username);
        planInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        planInfo.setEnableStepList(
            enableSteps == null ? Collections.emptyList() : enableSteps
        );
        planInfo.setVariableList(
            variableList == null ? Collections.emptyList() : variableList
        );
        planInfo.setDebug(false);
        return planInfo;
    }

    private OpenApiV4JobPlanDTO toOpenApiV4JobPlanDTO(Long appId, String username, TaskPlanInfoDTO savedPlan) {
        OpenApiV4JobPlanDTO data = new OpenApiV4JobPlanDTO();
        ResourceScope scope = appScopeMappingService.getScopeByAppId(appId);
        if (scope != null) {
            data.setScopeType(scope.getType().getValue());
            data.setScopeId(scope.getId());
        }
        data.setJobPlanId(savedPlan.getId());
        data.setJobPlanName(savedPlan.getName());
        data.setJobTemplateId(savedPlan.getTemplateId());
        data.setCreator(savedPlan.getCreator() != null ? savedPlan.getCreator() : username);
        Long createTimeSeconds = savedPlan.getCreateTime();
        data.setCreateTime(createTimeSeconds == null ? null : createTimeSeconds * 1000L);
        data.setNeedUpdate(Boolean.TRUE.equals(savedPlan.getNeedUpdate()));
        return data;
    }
}
