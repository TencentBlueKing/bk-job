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

package com.tencent.bk.job.manage.service.plan;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OpenAPI V4 执行方案请求体的公共解析逻辑，由 create_job_plan 与 update_job_plan 共用。
 */
@Component
public class OpenApiV4JobPlanRequestResolver {

    private final TenantHostService tenantHostService;

    @Autowired
    public OpenApiV4JobPlanRequestResolver(TenantHostService tenantHostService) {
        this.tenantHostService = tenantHostService;
    }

    /**
     * 解析创建方案时的启用步骤，入参为<b>模板步骤 ID</b>（方案尚不存在，只能按模板步骤寻址）。
     * 不传时默认启用模板的全部步骤。
     */
    public List<Long> resolveEnableStepsForCreate(List<Long> requestedStepIds, TaskTemplateInfoDTO template) {
        List<Long> templateStepIds = collectStepIds(template.getStepList());
        if (requestedStepIds == null) {
            return templateStepIds;
        }
        assertStepIdsIn(
            requestedStepIds,
            new HashSet<>(templateStepIds),
            stepId -> "step id " + stepId + " is not in template " + template.getId()
        );
        return new ArrayList<>(requestedStepIds);
    }

    /**
     * 解析更新方案时的启用步骤，入参为<b>方案步骤 ID</b>（更新的对象是方案自身的步骤快照）。
     */
    public List<Long> resolveEnableStepsForUpdate(List<Long> requestedStepIds,
                                                  List<TaskStepDTO> planSteps,
                                                  Long planId) {
        assertStepIdsIn(
            requestedStepIds,
            new HashSet<>(collectStepIds(planSteps)),
            stepId -> "step id " + stepId + " does not belong to job plan " + planId
                + ". If you intend to enable a step newly added to the job template, sync the job plan first"
        );
        return new ArrayList<>(requestedStepIds);
    }

    private List<Long> collectStepIds(List<TaskStepDTO> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return new ArrayList<>();
        }
        return steps.stream().map(TaskStepDTO::getId).collect(Collectors.toList());
    }

    private void assertStepIdsIn(List<Long> requestedStepIds,
                                 Set<Long> allowedStepIds,
                                 Function<Long, String> reasonBuilder) {
        for (Long stepId : requestedStepIds) {
            if (stepId == null || !allowedStepIds.contains(stepId)) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{
                        "enable_steps",
                        stepId == null ? "step id must not be null" : reasonBuilder.apply(stepId)
                    }
                );
            }
        }
    }

    /**
     * 按变量名把请求中的变量覆盖项映射为 {@link TaskVariableDTO}。
     *
     * <p>返回的 DTO 以<b>模板变量 ID</b> 作为 id：方案变量表按 template_variable_id 关联，
     * 创建与更新两条路径都依赖这一点。
     */
    public List<TaskVariableDTO> mapVariables(List<V4JobPlanVariableItem> variables,
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
            result.add(toTaskVariableDTO(item, templateVar, tenantId));
        }
        return result;
    }

    /**
     * 组装执行方案 DTO 的公共字段。
     *
     * <p>不设置 creator：创建方案时由调用方补上，更新方案时必须沿用原创建人。
     */
    public TaskPlanInfoDTO buildTaskPlanInfoDTO(String username,
                                                Long appId,
                                                Long templateId,
                                                String name,
                                                List<Long> enableSteps,
                                                List<TaskVariableDTO> variableList) {
        TaskPlanInfoDTO planInfo = new TaskPlanInfoDTO();
        planInfo.setAppId(appId);
        planInfo.setTemplateId(templateId);
        planInfo.setName(name);
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

    private TaskVariableDTO toTaskVariableDTO(V4JobPlanVariableItem item,
                                              TaskVariableDTO templateVar,
                                              String tenantId) {
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
                dto.setDefaultValue(buildTaskTargetDTO(item.getExecuteTarget(), tenantId).toJsonString());
            }
        } else if (!item.isFollowTemplate() && item.getValue() != null) {
            dto.setDefaultValue(item.getValue());
        }
        return dto;
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
                .map(OpenApiV4JobPlanRequestResolver::toTaskNodeInfoDTO)
                .collect(Collectors.toList()));
        }
        return new TaskTargetDTO(null, hostNode, null, null);
    }

    /**
     * 将 OpenAPI 主机列表解析为 {@link ApplicationHostDTO} 列表，并补全 hostId。
     *
     * <p>已带 bk_host_id 的直接使用；仅传 bk_cloud_id+ip 的批量从 CMDB（经 TenantHostService 缓存兜底）反查 hostId。
     * 未能解析到 hostId 的主机会抛 {@link InvalidParamException}，避免创建出页面回显"主机无效"的执行方案。
     *
     * @param hosts    入参主机列表，校验阶段已保证每台主机至少含有 bk_host_id 或 bk_cloud_id+ip
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
}
