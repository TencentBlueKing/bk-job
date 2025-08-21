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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.iam.EsbActionDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbInstanceDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbRelatedResourceTypeDTO;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.constant.ActionInfo;
import com.tencent.bk.job.common.iam.constant.Actions;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AuthServiceImpl extends BasicAuthService implements AuthService {
    private final AuthHelper authHelper;
    private final IIamClient iamClient;
    private final MessageI18nService i18nService;
    private ResourceNameQueryService resourceNameQueryService;

    public AuthServiceImpl(AuthHelper authHelper,
                           MessageI18nService i18nService,
                           IIamClient iamClient) {
        this.authHelper = authHelper;
        this.i18nService = i18nService;
        this.iamClient = iamClient;
    }

    @Override
    public void setResourceNameQueryService(ResourceNameQueryService resourceNameQueryService) {
        this.resourceNameQueryService = resourceNameQueryService;
        super.setResourceNameQueryService(resourceNameQueryService);
    }

    @Override
    public AuthResult auth(User user, String actionId) {
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(), user.getUsername(), actionId);
        if (isAllowed) {
            return AuthResult.pass(user);
        } else {
            return buildFailAuthResult(user, actionId, null, (String) null);
        }
    }

    @Override
    public AuthResult auth(User user,
                           String actionId,
                           ResourceTypeEnum resourceType,
                           String resourceId,
                           PathInfoDTO pathInfo) {
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstance(resourceType, resourceId, pathInfo));
        if (isAllowed) {
            return AuthResult.pass(user);
        } else {
            return buildFailAuthResult(user, actionId, resourceType, resourceId);
        }
    }

    private AuthResult buildFailAuthResult(User user,
                                           String actionId,
                                           ResourceTypeEnum resourceType,
                                           String resourceId) {
        AuthResult authResult = AuthResult.fail(user);
        if (resourceType == null || StringUtils.isEmpty(resourceId)) {
            authResult.addRequiredPermission(actionId, null);
        } else {
            String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
            authResult.addRequiredPermission(actionId, new PermissionResource(resourceType, resourceId, resourceName));
        }
        return authResult;
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstanceList(resourceType, resourceIdList));
    }

    @Override
    public AuthResult batchAuthResources(User user,
                                         String actionId,
                                         List<PermissionResource> resources) {
        ResourceTypeEnum resourceType = resources.get(0).getResourceType();
        List<String> allowResourceIds = authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstanceList(resources));
        List<String> notAllowResourceIds =
            resources.stream().filter(resource -> !allowResourceIds.contains(resource.getResourceId()))
                .map(PermissionResource::getResourceId).collect(Collectors.toList());
        AuthResult authResult = new AuthResult();
        if (!notAllowResourceIds.isEmpty()) {
            authResult = buildFailAuthResult(user, actionId, resourceType, notAllowResourceIds);
        } else {
            authResult.setPass(true);
        }
        return authResult;
    }

    private List<InstanceDTO> buildInstanceList(ResourceTypeEnum resourceType,
                                                List<String> resourceIds) {
        List<InstanceDTO> instances = new LinkedList<>();
        resourceIds.forEach(resourceId -> instances.add(buildInstance(resourceType, resourceId, null)));
        return instances;
    }

    @Override
    public String getApplyUrl(String tenantId, List<PermissionActionResource> permissionActionResources) {
        if (permissionActionResources == null || permissionActionResources.isEmpty()) {
            log.warn("Get apply url, action resource is empty!");
            return "";
        }
        List<ActionDTO> actions = buildApplyActions(permissionActionResources);
        log.info("Get apply url, actions: {}", actions);
        return iamClient.getApplyUrl(actions);
    }

    private List<ActionDTO> buildApplyActions(List<PermissionActionResource> permissionActionResources) {
        Map<String, Map<String, List<PermissionResource>>> resourcesGroupByActionAndType =
            groupResourcesByActionAndResourceType(permissionActionResources);

        List<ActionDTO> actions = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<PermissionResource>>> entry :
            resourcesGroupByActionAndType.entrySet()) {
            String actionId = entry.getKey();
            Map<String, List<PermissionResource>> resourceGroups = entry.getValue();
            ActionDTO action = new ActionDTO();
            action.setId(actionId);
            actions.add(action);

            if (resourceGroups == null || resourceGroups.isEmpty()) {
                continue;
            }

            ActionInfo actionInfo = Actions.getActionInfo(actionId);
            if (actionInfo == null) {
                log.error("Invalid Action, actionId: {}", actionId);
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }

            List<RelatedResourceTypeDTO> relatedResourceTypes = new ArrayList<>();
            List<ResourceTypeEnum> actionRelatedResourceTypes = actionInfo.getRelatedResourceTypes();
            // 无关联资源的Action处理
            if (CollectionUtils.isEmpty(actionRelatedResourceTypes)) {
                action.setRelatedResourceTypes(relatedResourceTypes);
                continue;
            }
            // IAM 鉴权API对于依赖资源类型的顺序有要求，需要按照注册资源时候的顺序
            for (ResourceTypeEnum resourceType : actionRelatedResourceTypes) {
                List<PermissionResource> relatedResources = resourceGroups.get(resourceType.getId());
                if (CollectionUtils.isEmpty(relatedResources)) {
                    log.error("Action related resources is empty");
                    throw new InternalException(ErrorCode.INTERNAL_ERROR);
                }
                RelatedResourceTypeDTO relatedResourceType = new RelatedResourceTypeDTO();
                String systemId = relatedResources.get(0).getSystemId();
                relatedResourceType.setSystemId(systemId);
                relatedResourceType.setType(resourceType.getId());
                List<List<InstanceDTO>> instanceList = new ArrayList<>();
                for (PermissionResource relatedResource : relatedResources) {
                    InstanceDTO instance = convertPermissionResourceToInstance(relatedResource);
                    if (!CustomCollectionUtils.isEmptyCollection(relatedResource.getParentHierarchicalResources())) {
                        List<InstanceDTO> hierarchicalInstance = new ArrayList<>();
                        relatedResource.getParentHierarchicalResources().forEach(
                            resource -> hierarchicalInstance.add(convertPermissionResourceToInstance(resource)));
                        hierarchicalInstance.add(instance);
                        instanceList.add(hierarchicalInstance);
                    } else {
                        instanceList.add(Collections.singletonList(instance));
                    }
                }
                relatedResourceType.setInstance(instanceList);

                relatedResourceTypes.add(relatedResourceType);
            }
            action.setRelatedResourceTypes(relatedResourceTypes);
        }
        return actions;
    }

    private Map<String, Map<String, List<PermissionResource>>> groupResourcesByActionAndResourceType(
        List<PermissionActionResource> permissionActionResources) {
        // Map<actionId, Map<resourceType, resourceList>>
        Map<String, Map<String, List<PermissionResource>>> resourcesGroupByActionAndType = new HashMap<>();
        permissionActionResources.forEach(actionResource ->
            resourcesGroupByActionAndType.compute(actionResource.getActionId(), (actionId, resourcesGroupByType) -> {
                if (resourcesGroupByType == null) {
                    resourcesGroupByType = new HashMap<>();
                }
                for (PermissionResourceGroup resourceGroup : actionResource.getResourceGroups()) {
                    resourcesGroupByType.compute(resourceGroup.getResourceType().getId(), (resourceType, resources) -> {
                        if (resources == null) {
                            resources = new ArrayList<>();
                        }
                        resources.addAll(resourceGroup.getPermissionResources());
                        return resources;
                    });
                }
                return resourcesGroupByType;
            }));
        return resourcesGroupByActionAndType;
    }

    @Override
    public <T> EsbResp<T> buildEsbAuthFailResp(List<PermissionActionResource> permissionActionResources) {
        List<ActionDTO> actions = buildApplyActions(permissionActionResources);
        OpenApiApplyPermissionDTO applyPermission = buildPermissionApplyDTO(actions);
        return EsbResp.buildAuthFailResult(applyPermission);
    }

    @Override
    public OpenApiApplyPermissionDTO buildPermissionDetailByPermissionApplyDTO(PermissionDeniedException exception) {
        List<ActionDTO> actions = buildApplyActions(exception.getAuthResult().getRequiredActionResources());
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

    @Override
    public <T> EsbResp<T> buildEsbAuthFailResp(PermissionDeniedException exception) {
        return buildEsbAuthFailResp(exception.getAuthResult().getRequiredActionResources());
    }

    @Override
    public boolean registerResource(User user,
                                    String id,
                                    String name,
                                    String type,
                                    List<ResourceDTO> ancestors) {
        return iamClient.registerResource(id, name, type, user.getUsername(), ancestors);
    }
}
