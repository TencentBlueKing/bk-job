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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.plan.V4JobPlanCreateService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class V4JobPlanCreateServiceImpl implements V4JobPlanCreateService {

    private final TaskPlanService planService;
    private final TaskTemplateService templateService;
    private final TemplateAuthService templateAuthService;
    private final PlanAuthService planAuthService;
    private final AppScopeMappingService appScopeMappingService;
    private final TenantHostService tenantHostService;

    @Autowired
    public V4JobPlanCreateServiceImpl(TaskPlanService planService,
                                      TaskTemplateService templateService,
                                      TemplateAuthService templateAuthService,
                                      PlanAuthService planAuthService,
                                      AppScopeMappingService appScopeMappingService,
                                      TenantHostService tenantHostService) {
        this.planService = planService;
        this.templateService = templateService;
        this.templateAuthService = templateAuthService;
        this.planAuthService = planAuthService;
        this.appScopeMappingService = appScopeMappingService;
        this.tenantHostService = tenantHostService;
    }

    @Override
    public TaskPlanInfoDTO createJobPlan(User operator, V4CreateJobPlanRequest request, boolean dryRun) {
        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        AppResourceScope appResourceScope = request.getAppResourceScope();

        templateAuthService.authViewJobTemplate(operator, appResourceScope, request.getJobTemplateId())
            .denyIfNoPermission();
        planAuthService.authCreateJobPlan(operator, appResourceScope, request.getJobTemplateId(), null)
            .denyIfNoPermission();

        TaskTemplateInfoDTO template = templateService.getTaskTemplateById(appId, request.getJobTemplateId());
        if (template == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        List<Long> enableSteps = resolveEnableSteps(request, template);
        List<TaskVariableDTO> variableList = mapVariables(request.getVariables(), template, operator.getTenantId());

        String planName = StringUtils.strip(request.getName());
        if (Boolean.FALSE.equals(
            planService.checkPlanName(appId, request.getJobTemplateId(), 0L, planName)
        )) {
            throw new AlreadyExistsException(ErrorCode.PLAN_NAME_EXIST);
        }

        TaskPlanInfoDTO planInfoDTO = buildTaskPlanInfoDTO(
            operator.getUsername(), appId, request.getJobTemplateId(), planName, enableSteps, variableList
        );

        // ============ dryRun 预检返回点 ============
        // 此行之上不得新增写操作：预检与真实创建必须走同一段校验代码，但预检绝不能把执行方案落库。
        // 往上插入写操作会让预检穿透成真实创建，用户还没审批，执行方案已经建出来了。
        if (dryRun) {
            // 审批概要要按名称而不是 ID 展示启用的步骤，把上文已查出的模板步骤带回去，省得为一行展示再查一次模板。
            // 与真实创建时 TaskPlanInfoDTO#buildPlanInfo 的填法一致：stepList 是方案的全部步骤，
            // enableStepList 是其中启用的那些
            planInfoDTO.setStepList(template.getStepList());
            // 概要要列出方案生效的全部变量，未覆盖的也得带上模板默认值，因此这里复用真实创建的合并逻辑，
            // 把 variableList 从"本次覆盖项"换成"合并后的全部方案变量"。纯内存计算，不落库
            TaskPlanInfoDTO.fillPlanVariablesFromTemplate(planInfoDTO, template);
            return planInfoDTO;
        }

        return planService.createTaskPlan(operator, planInfoDTO);
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
                dto.setDefaultValue(item.getValue());
            }
            result.add(dto);
        }
        return result;
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
                .map(V4JobPlanCreateServiceImpl::toTaskNodeInfoDTO)
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
}
