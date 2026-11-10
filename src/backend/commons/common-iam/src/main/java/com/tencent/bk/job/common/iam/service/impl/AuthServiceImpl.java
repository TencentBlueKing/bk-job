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

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.iam.EsbActionDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbInstanceDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbRelatedResourceTypeDTO;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.ActionAuthExecutor;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamAuthVersionHelper;
import com.tencent.bk.job.common.iam.util.IamV3ApplyActionBuilder;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用资源鉴权的统一入口 / 路由层。
 *
 * <p>持有 v3 / v4 两个 {@link ActionAuthExecutor}，按热更灰度开关 {@link IamAuthVersionHelper#useV4(String)}
 * 选择执行器，只做灰度路由 + 结果包装。申请链接 / 注册委派给执行器（版本相关）；ESB 无权限响应为版本无关的
 * 格式化，保留在本类。本类不直接依赖 v3/v4 的 IAM 客户端。</p>
 */
@Slf4j
public class AuthServiceImpl extends AbstractAuthResultBuilder implements AuthService {

    private final ActionAuthExecutor v3Executor;
    private final ActionAuthExecutor v4Executor;
    private final MessageI18nService i18nService;

    public AuthServiceImpl(ActionAuthExecutor v3Executor,
                           ActionAuthExecutor v4Executor,
                           MessageI18nService i18nService) {
        this.v3Executor = v3Executor;
        this.v4Executor = v4Executor;
        this.i18nService = i18nService;
    }

    private ActionAuthExecutor pickExecutor(User user) {
        return IamAuthVersionHelper.useV4(user.getUsername()) ? v4Executor : v3Executor;
    }

    private ActionAuthExecutor pickExecutorByContext() {
        return IamAuthVersionHelper.useV4(JobContextUtil.getUsername()) ? v4Executor : v3Executor;
    }

    @Override
    public AuthResult auth(User user, String actionId) {
        boolean isAllowed = pickExecutor(user).auth(user, actionId);
        if (isAllowed) {
            return AuthResult.pass(user);
        }
        return buildResourceFailAuthResult(user, actionId, null, null, null);
    }

    @Override
    public AuthResult auth(User user,
                           String actionId,
                           ResourceTypeEnum resourceType,
                           String resourceId,
                           PathInfoDTO pathInfo) {
        boolean isAllowed = pickExecutor(user).auth(user, actionId, resourceType, resourceId, pathInfo);
        if (isAllowed) {
            return AuthResult.pass(user);
        }
        return buildResourceFailAuthResult(user, actionId, resourceType, resourceId, pathInfo);
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return pickExecutor(user).batchAuth(user, actionId, resourceType, resourceIdList);
    }

    @Override
    public AuthResult batchAuthResources(User user,
                                         String actionId,
                                         List<PermissionResource> resources) {
        List<String> allowResourceIds = pickExecutor(user).listAllowedResourceIds(user, actionId, resources);
        List<PermissionResource> deniedResources = resources.stream()
            .filter(resource -> !allowResourceIds.contains(resource.getResourceId()))
            .collect(Collectors.toList());
        return deniedResources.isEmpty()
            ? AuthResult.pass(user)
            : buildFailAuthResultFromResources(user, actionId, deniedResources);
    }

    @Override
    public String getApplyUrl(String actionId, ResourceTypeEnum resourceType) {
        return pickExecutorByContext().getApplyUrl(actionId, resourceType);
    }

    @Override
    public String getApplyUrl(String tenantId, List<PermissionActionResource> permissionActionResources) {
        if (permissionActionResources == null || permissionActionResources.isEmpty()) {
            log.warn("Get apply url, action resource is empty!");
            return "";
        }
        return pickExecutorByContext().getApplyUrl(tenantId, permissionActionResources);
    }

    @Override
    public boolean registerResource(User creator,
                                    String id,
                                    String name,
                                    String type,
                                    List<ResourceDTO> ancestors) {
        return pickExecutor(creator).registerResource(creator, id, name, type, ancestors);
    }

    private AuthResult buildResourceFailAuthResult(User user,
                                                   String actionId,
                                                   ResourceTypeEnum resourceType,
                                                   String resourceId,
                                                   PathInfoDTO pathInfo) {
        AuthResult authResult = AuthResult.fail(user);
        if (resourceType == null || StringUtils.isEmpty(resourceId)) {
            authResult.addRequiredPermission(actionId, null);
        } else {
            String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
            PermissionResource permissionResource =
                new PermissionResource(resourceType, resourceId, resourceName);
            if (pathInfo != null) {
                List<PermissionResource> parentResources =
                    buildParentHierarchicalResources(pathInfo, permissionResource);
                if (!parentResources.isEmpty()) {
                    permissionResource.setParentHierarchicalResources(parentResources);
                }
            }
            authResult.addRequiredPermission(actionId, permissionResource);
        }
        return authResult;
    }

    /**
     * 将 pathInfo 链表解析为父级层级资源列表。
     * <p>
     * pathInfo 可能只含祖先（如 biz/{bizId}），也可能带上资源自身（如 biz/{bizId} → account/{accountId}）。
     * 跳过 type+id 与被鉴权资源匹配的节点，只保留祖先。
     *
     * @param pathInfo           资源路径信息
     * @param permissionResource 被鉴权资源
     * @return 父级层级资源列表
     */
    private List<PermissionResource> buildParentHierarchicalResources(PathInfoDTO pathInfo,
                                                                      PermissionResource permissionResource) {
        List<PermissionResource> parentResources = new ArrayList<>();
        String resourceType = StringUtils.isNotEmpty(permissionResource.getType())
            ? permissionResource.getType() : permissionResource.getResourceType().getId();
        String resourceId = permissionResource.getResourceId();
        PathInfoDTO node = pathInfo;
        while (node != null) {
            ResourceTypeEnum nodeType = ResourceTypeEnum.getByResourceTypeId(node.getType());
            String nodeId = node.getId();
            if (nodeType != null && StringUtils.isNotEmpty(nodeId)
                && !(node.getType().equals(resourceType) && nodeId.equals(resourceId))) {
                String parentName = resourceNameQueryService.getResourceName(nodeType, nodeId);
                PermissionResource parentResource = new PermissionResource(nodeType, nodeId, parentName);
                parentResources.add(parentResource);
            }
            node = node.getChild();
        }
        return parentResources;
    }

    @Override
    public <T> EsbResp<T> buildEsbAuthFailResp(List<PermissionActionResource> permissionActionResources) {
        List<ActionDTO> actions = IamV3ApplyActionBuilder.buildApplyActions(permissionActionResources);
        OpenApiApplyPermissionDTO applyPermission = buildPermissionApplyDTO(actions);
        return EsbResp.buildAuthFailResult(applyPermission);
    }

    @Override
    public <T> EsbResp<T> buildEsbAuthFailResp(PermissionDeniedException exception) {
        return buildEsbAuthFailResp(exception.getAuthResult().getRequiredActionResources());
    }

    @Override
    public OpenApiApplyPermissionDTO buildPermissionDetailByPermissionApplyDTO(PermissionDeniedException exception) {
        List<ActionDTO> actions =
            IamV3ApplyActionBuilder.buildApplyActions(exception.getAuthResult().getRequiredActionResources());
        return buildPermissionApplyDTO(actions);
    }

    private OpenApiApplyPermissionDTO buildPermissionApplyDTO(List<ActionDTO> actions) {
        OpenApiApplyPermissionDTO applyPermission = new OpenApiApplyPermissionDTO();
        applyPermission.setSystemId(SystemId.JOB);
        applyPermission.setSystemName(i18nService.getI18n("system.bk_job"));
        applyPermission.setActions(actions.stream().map(this::convertToEsbAction).collect(Collectors.toList()));
        return applyPermission;
    }

    private EsbActionDTO convertToEsbAction(ActionDTO action) {
        EsbActionDTO esbAction = new EsbActionDTO();
        esbAction.setId(action.getId());
        esbAction.setName(i18nService.getI18n("permission.action.name." + action.getId()));

        List<RelatedResourceTypeDTO> relatedResourceTypes = action.getRelatedResourceTypes();
        if (relatedResourceTypes == null || relatedResourceTypes.isEmpty()) {
            esbAction.setRelatedResourceTypes(Collections.emptyList());
            return esbAction;
        }
        List<EsbRelatedResourceTypeDTO> esbRelatedResourceTypes = new ArrayList<>();
        for (RelatedResourceTypeDTO relatedResourceType : relatedResourceTypes) {
            EsbRelatedResourceTypeDTO esbRelatedResourceType = new EsbRelatedResourceTypeDTO();
            esbRelatedResourceType.setSystemId(relatedResourceType.getSystemId());
            esbRelatedResourceType.setSystemName(i18nService.getI18n("system."
                + relatedResourceType.getSystemId()));
            esbRelatedResourceType.setType(relatedResourceType.getType());
            esbRelatedResourceType.setTypeName(i18nService.getI18n("permission.resource.type.name."
                + relatedResourceType.getType()));

            List<List<EsbInstanceDTO>> esbInstances = new ArrayList<>();
            for (List<InstanceDTO> hierarchicalInstance : relatedResourceType.getInstance()) {
                List<EsbInstanceDTO> esbHierarchicalInstance = new ArrayList<>(hierarchicalInstance.size());
                for (InstanceDTO instance : hierarchicalInstance) {
                    EsbInstanceDTO esbInstance = new EsbInstanceDTO();
                    esbInstance.setId(instance.getId());
                    esbInstance.setName(instance.getName());
                    esbInstance.setType(instance.getType());
                    esbInstance.setTypeName(i18nService.getI18n("permission.resource.type.name."
                        + instance.getType()));
                    esbHierarchicalInstance.add(esbInstance);
                }
                esbInstances.add(esbHierarchicalInstance);
            }
            esbRelatedResourceType.setInstance(esbInstances);
            esbRelatedResourceTypes.add(esbRelatedResourceType);
        }
        esbAction.setRelatedResourceTypes(esbRelatedResourceTypes);
        return esbAction;
    }
}
