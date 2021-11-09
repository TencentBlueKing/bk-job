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

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.iam.EsbActionDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbApplyPermissionDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbInstanceDTO;
import com.tencent.bk.job.common.esb.model.iam.EsbRelatedResourceTypeDTO;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.client.EsbIamClient;
import com.tencent.bk.job.common.iam.config.EsbConfiguration;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ActionInfo;
import com.tencent.bk.job.common.iam.constant.Actions;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.dto.AppIdResult;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO;
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final AuthHelper authHelper;
    private final BusinessAuthHelper businessAuthHelper;
    private final PolicyService policyService;
    private final EsbIamClient iamClient;
    private final MessageI18nService i18nService;
    private ResourceNameQueryService resourceNameQueryService;
    private ResourceAppInfoQueryService resourceAppInfoQueryService;


    public AuthServiceImpl(@Autowired AuthHelper authHelper,
                           @Autowired BusinessAuthHelper businessAuthHelper,
                           @Autowired IamConfiguration iamConfiguration,
                           @Autowired PolicyService policyService,
                           @Autowired EsbConfiguration esbConfiguration, MessageI18nService i18nService) {
        this.authHelper = authHelper;
        this.businessAuthHelper = businessAuthHelper;
        this.policyService = policyService;
        this.i18nService = i18nService;
        this.iamClient = new EsbIamClient(esbConfiguration.getEsbUrl(), iamConfiguration.getAppCode(),
            iamConfiguration.getAppSecret(), esbConfiguration.isUseEsbTestEnv());
//        this.resourceNameQueryService = resourceNameQueryService;
    }

    @Override
    public void setResourceAppInfoQueryService(ResourceAppInfoQueryService resourceAppInfoQueryService) {
        this.resourceAppInfoQueryService = resourceAppInfoQueryService;
    }

    @Override
    public void setResourceNameQueryService(ResourceNameQueryService resourceNameQueryService) {
        this.resourceNameQueryService = resourceNameQueryService;
    }

    @Override
    public AuthResult auth(boolean returnApplyUrl, String username, String actionId) {
        boolean isAllowed = authHelper.isAllowed(username, actionId);
        if (isAllowed) {
            return AuthResult.pass();
        } else {
            return buildFailAuthResult(returnApplyUrl, actionId, null, null);
        }
    }

    public boolean authSpecialAppByMaintainer(String username, ResourceTypeEnum resourceType,
                                               String resourceId) {
        // 业务集、全业务特殊鉴权
        if (resourceAppInfoQueryService != null) {
            ResourceAppInfo resourceAppInfo = resourceAppInfoQueryService.getResourceAppInfo(resourceType, resourceId);
            if (resourceAppInfo != null && resourceAppInfo.getAppType() != AppTypeEnum.NORMAL) {
                return resourceAppInfo.getMaintainerList().contains(username);
            }
        } else {
            log.warn("appInfoQueryService not set, cannot auth special business");
        }
        return false;
    }

    @Override
    public AuthResult auth(boolean returnApplyUrl, String username, String actionId, ResourceTypeEnum resourceType,
                           String resourceId, PathInfoDTO pathInfo) {
        if (authSpecialAppByMaintainer(username, resourceType, resourceId)) {
            return AuthResult.pass();
        }
        boolean isAllowed = authHelper.isAllowed(username, actionId, buildInstance(resourceType, resourceId, pathInfo));
        if (isAllowed) {
            return AuthResult.pass();
        } else {
            return buildFailAuthResult(returnApplyUrl, actionId, resourceType, resourceId);
        }
    }


    private AuthResult buildFailAuthResult(boolean returnApplyUrl, String actionId, ResourceTypeEnum resourceType,
                                           String resourceId) {
        AuthResult authResult = AuthResult.fail();
        if (resourceType == null || StringUtils.isEmpty(resourceId)) {
            if (returnApplyUrl) {
                authResult.setApplyUrl(getApplyUrl(actionId));
            }
            authResult.addRequiredPermission(actionId, null);
        } else {
            String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
            if (returnApplyUrl) {
                authResult.setApplyUrl(getApplyUrl(actionId, resourceType, resourceId));
            }
            authResult.addRequiredPermission(actionId, new PermissionResource(resourceType, resourceId, resourceName));
        }
        return authResult;
    }

    private AuthResult buildFailAuthResult(String actionId, ResourceTypeEnum resourceType,
                                           Collection<String> resourceIds) {
        AuthResult authResult = AuthResult.fail();
        if (resourceType == null) {
            authResult.addRequiredPermission(actionId, null);
        } else {
            for (String resourceId : resourceIds) {
                String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
                authResult.addRequiredPermission(actionId, new PermissionResource(resourceType, resourceId,
                    resourceName));
            }
        }
        return authResult;
    }

    @Override
    public AuthResult auth(boolean isReturnApplyUrl, String username, List<PermissionActionResource> actionResources) {
        AuthResult authResult = new AuthResult();
        authResult.setPass(true);
        List<PermissionActionResource> requiredActionResources = new ArrayList<>();

        for (PermissionActionResource actionResource : actionResources) {
            String actionId = actionResource.getActionId();
            List<PermissionResourceGroup> relatedResourceGroups = actionResource.getResourceGroups();
            if (relatedResourceGroups == null || relatedResourceGroups.isEmpty()) {
                if (!authHelper.isAllowed(username, actionId)) {
                    authResult.setPass(false);
                    PermissionActionResource requiredActionResource = new PermissionActionResource();
                    requiredActionResource.setActionId(actionId);
                    requiredActionResources.add(requiredActionResource);
                    authResult.addRequiredPermission(actionId, null);
                }
            } else {
                // Job当前的场景，暂时只需要支持操作依赖一个资源
                ResourceTypeEnum resourceType = relatedResourceGroups.get(0).getResourceType();
                List<PermissionResource> resources = relatedResourceGroups.get(0).getPermissionResources();
                // All resources are under one application, so choose any one for authentication
                if (authSpecialAppByMaintainer(username, resourceType, resources.get(0).getResourceId())) {
                    authResult.setPass(true);
                } else {
                    List<String> allowedResourceIds =
                        authHelper.isAllowed(username, actionId, buildInstanceList(resources));
                    List<String> notAllowResourceIds =
                        resources.stream().filter(resource -> !allowedResourceIds.contains(resource.getResourceId()))
                            .map(PermissionResource::getResourceId).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(notAllowResourceIds)) {
                        authResult.setPass(false);
                        resources.forEach(resource -> {
                            if (notAllowResourceIds.contains(resource.getResourceId())) {
                                if (isReturnApplyUrl) {
                                    resource.setResourceName(resourceNameQueryService.getResourceName(resourceType,
                                        resource.getResourceId()));
                                }
                                PermissionActionResource requiredActionResource = new PermissionActionResource();
                                requiredActionResource.setActionId(actionId);
                                requiredActionResource.addResource(resource);
                                requiredActionResources.add(requiredActionResource);
                                authResult.addRequiredPermission(actionId, resource);
                            }
                        });
                    }
                }
            }
        }

        if (!authResult.isPass() && isReturnApplyUrl) {
            authResult.setApplyUrl(getApplyUrl(requiredActionResources));
        }
        return authResult;
    }

    @Override
    public List<String> batchAuth(String username, String actionId, ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return authHelper.isAllowed(username, actionId, buildInstanceList(resourceType, resourceIdList));
    }

    @Override
    public List<String> batchAuth(String username, String actionId, Long appId, ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        // 业务集、全业务特殊鉴权
        if (resourceAppInfoQueryService != null) {
            ResourceAppInfo resourceAppInfo =
                resourceAppInfoQueryService.getResourceAppInfo(ResourceTypeEnum.BUSINESS, appId.toString());
            if (resourceAppInfo != null && resourceAppInfo.getAppType() != AppTypeEnum.NORMAL) {
                if (resourceAppInfo.getMaintainerList().contains(username)) {
                    return resourceIdList;
                } else {
                    return Collections.emptyList();
                }
            }
        } else {
            log.warn("appInfoQueryService not set, cannot auth special business");
        }
        return authHelper.isAllowed(username, actionId, buildAppInstanceList(appId, resourceType, resourceIdList));
    }

    @Override
    public AuthResult batchAuthResources(String username, String actionId, Long appId,
                                         List<PermissionResource> resources) {
        // 业务集、全业务特殊鉴权
        if (resourceAppInfoQueryService != null) {
            ResourceAppInfo resourceAppInfo =
                resourceAppInfoQueryService.getResourceAppInfo(ResourceTypeEnum.BUSINESS, appId.toString());
            if (resourceAppInfo != null && resourceAppInfo.getAppType() != AppTypeEnum.NORMAL) {
                if (resourceAppInfo.getMaintainerList().contains(username)) {
                    return AuthResult.pass();
                } else {
                    return AuthResult.fail();
                }
            }
        } else {
            log.warn("appInfoQueryService not set, cannot auth special business");
        }

        ResourceTypeEnum resourceType = resources.get(0).getResourceType();
        List<String> allowResourceIds = authHelper.isAllowed(username, actionId, buildInstanceList(resources));
        List<String> notAllowResourceIds =
            resources.stream().filter(resource -> !allowResourceIds.contains(resource.getResourceId()))
                .map(PermissionResource::getResourceId).collect(Collectors.toList());
        AuthResult authResult = new AuthResult();
        if (!notAllowResourceIds.isEmpty()) {
            authResult = buildFailAuthResult(actionId, resourceType, notAllowResourceIds);
        } else {
            authResult.setPass(true);
        }
        return authResult;
    }

    @Override
    public List<String> batchAuth(String username, String actionId, Long appId, List<PermissionResource> resourceList) {
        // 业务集、全业务特殊鉴权
        if (resourceAppInfoQueryService != null) {
            ResourceAppInfo resourceAppInfo =
                resourceAppInfoQueryService.getResourceAppInfo(ResourceTypeEnum.BUSINESS, appId.toString());
            if (resourceAppInfo != null && resourceAppInfo.getAppType() != AppTypeEnum.NORMAL) {
                if (resourceAppInfo.getMaintainerList().contains(username)) {
                    return resourceList.parallelStream()
                        .map(PermissionResource::getResourceId).collect(Collectors.toList());
                } else {
                    return Collections.emptyList();
                }
            }
        } else {
            log.warn("appInfoQueryService not set, cannot auth special business");
        }
        return authHelper.isAllowed(username, actionId, buildInstanceList(resourceList));
    }


    @Override
    public AppIdResult getAppIdList(String username, List<Long> allAppIdList) {
        AppIdResult result = new AppIdResult();
        result.setAppId(new ArrayList<>());
        result.setAny(false);

        ActionDTO action = new ActionDTO();
        action.setId(ActionId.LIST_BUSINESS);
        ExpressionDTO expression = policyService.getPolicyByAction(username, action, null);
        if (ExpressionOperationEnum.ANY == expression.getOperator()) {
            result.setAny(true);
        } else {
            if (StringUtils.isNotBlank(expression.getField())
                && expression.getField().equals(ResourceId.APP + "." + "id")) {
                if (expression.getValue() instanceof List) {
                    List<?> list = ((List<?>) expression.getValue());
                    if (list.size() > 0) {
                        ((List<String>) expression.getValue()).forEach(id -> result.getAppId().add(Long.parseLong(id)));
                    }
                } else if (ExpressionOperationEnum.EQUAL == expression.getOperator()) {
                    result.getAppId().add(Long.parseLong(String.valueOf(expression.getValue())));
                } else {
                    result.getAppId().addAll(businessAuthHelper.getAuthedAppIdList(username, expression, allAppIdList));
                }
            } else {
                result.getAppId().addAll(businessAuthHelper.getAuthedAppIdList(username, expression, allAppIdList));
            }
        }
        return result;
    }

    private List<InstanceDTO> buildInstanceList(ResourceTypeEnum resourceType,
                                                List<String> resourceIds) {
        List<InstanceDTO> instances = new LinkedList<>();
        resourceIds.forEach(resourceId -> instances.add(buildInstance(resourceType, resourceId, null)));
        return instances;
    }

    private List<InstanceDTO> buildAppInstanceList(Long appId, ResourceTypeEnum resourceType,
                                                   List<String> resourceIds) {
        List<InstanceDTO> instances = new LinkedList<>();
        resourceIds.forEach(resourceId -> instances.add(buildInstance(resourceType, resourceId,
            PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build())));
        return instances;
    }

    private List<InstanceDTO> buildInstanceList(List<PermissionResource> resources) {
        List<InstanceDTO> instances = new LinkedList<>();
        resources.forEach(resource -> instances.add(buildInstance(resource.getResourceType(), resource.getResourceId(),
            resource.getPathInfo())));
        return instances;
    }

    @Override
    public String getApplyUrl(List<PermissionActionResource> permissionActionResources) {
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
        resourcesGroupByActionAndType.forEach((actionId, resourceGroups) -> {
            ActionDTO action = new ActionDTO();
            action.setId(actionId);
            actions.add(action);

            if (resourceGroups == null || resourceGroups.isEmpty()) {
                return;
            }

            ActionInfo actionInfo = Actions.getActionInfo(actionId);
            if (actionInfo == null) {
                log.error("Invalid Action, actionId: {}", actionId);
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }

            List<RelatedResourceTypeDTO> relatedResourceTypes = new ArrayList<>();
            // IAM 鉴权API对于依赖资源类型的顺序有要求，需要按照注册资源时候的顺序
            for (ResourceTypeEnum resourceType : actionInfo.getRelatedResourceTypes()) {
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
        });
        return actions;
    }

    private Map<String, Map<String, List<PermissionResource>>> groupResourcesByActionAndResourceType(
        List<PermissionActionResource> permissionActionResources) {
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

    private InstanceDTO convertPermissionResourceToInstance(PermissionResource permissionResource) {
        InstanceDTO instance = new InstanceDTO();
        instance.setId(permissionResource.getResourceId());
        if (StringUtils.isEmpty(permissionResource.getType())) {
            instance.setType(permissionResource.getResourceType().getId());
        } else {
            instance.setType(permissionResource.getType());
        }
        instance.setName(permissionResource.getResourceName());
        return instance;
    }

    @Override
    public <T> EsbResp<T> buildEsbAuthFailResp(List<PermissionActionResource> permissionActionResources) {
        List<ActionDTO> actions = buildApplyActions(permissionActionResources);
        EsbApplyPermissionDTO applyPermission = new EsbApplyPermissionDTO();
        applyPermission.setSystemId(SystemId.JOB);
        applyPermission.setSystemName(i18nService.getI18n("system.bk_job"));
        applyPermission.setActions(actions.stream().map(this::convertToEsbAction).collect(Collectors.toList()));
        return EsbResp.buildAuthFailResult(applyPermission);
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
    public String getApplyUrl(String actionId, ResourceTypeEnum resourceType, String resourceId) {
        InstanceDTO instance = new InstanceDTO();
        instance.setId(resourceId);
        instance.setType(resourceType.getId());

        RelatedResourceTypeDTO relatedResourceType = new RelatedResourceTypeDTO();
        relatedResourceType.setSystemId(resourceType.getSystemId());
        relatedResourceType.setType(resourceType.getId());
        relatedResourceType.setInstance(Collections.singletonList(Collections.singletonList(instance)));

        ActionDTO action = new ActionDTO();
        action.setId(actionId);
        action.setRelatedResourceTypes(Collections.singletonList(relatedResourceType));

        return iamClient.getApplyUrl(Collections.singletonList(action));
    }

    @Override
    public String getApplyUrl(String actionId) {
        ActionDTO action = new ActionDTO();
        action.setId(actionId);
        action.setRelatedResourceTypes(Collections.emptyList());
        return iamClient.getApplyUrl(Collections.singletonList(action));
    }

    @Override
    public String getBusinessApplyUrl(Long appId) {
        ActionDTO action = new ActionDTO();
        action.setId(ActionId.LIST_BUSINESS);
        List<RelatedResourceTypeDTO> relatedResourceTypes = new ArrayList<>();
        RelatedResourceTypeDTO businessResourceTypeDTO = new RelatedResourceTypeDTO();
        businessResourceTypeDTO.setType(ResourceTypeEnum.BUSINESS.getId());
        businessResourceTypeDTO.setSystemId(SystemId.CMDB);
        if (appId != null) {
            List<InstanceDTO> instanceDTOList = new ArrayList<>();
            InstanceDTO instanceDTO = new InstanceDTO();
            instanceDTO.setSystem(SystemId.CMDB);
            instanceDTO.setType(ResourceTypeEnum.BUSINESS.getId());
            instanceDTO.setId(appId.toString());
            instanceDTOList.add(instanceDTO);
            businessResourceTypeDTO.setInstance(Collections.singletonList(instanceDTOList));
        } else {
            businessResourceTypeDTO.setInstance(Collections.emptyList());
        }
        relatedResourceTypes.add(businessResourceTypeDTO);
        action.setRelatedResourceTypes(relatedResourceTypes);
        return iamClient.getApplyUrl(Collections.singletonList(action));
    }

    @Override
    public boolean registerResource(String id, String name, String type, String creator, List<ResourceDTO> ancestors) {
        return iamClient.registerResource(id, name, type, creator, ancestors);
    }

    private InstanceDTO buildInstance(ResourceTypeEnum resourceType, String resourceId, PathInfoDTO path) {
        InstanceDTO instance = new InstanceDTO();
        instance.setId(resourceId);
        instance.setType(resourceType.getId());
        instance.setSystem(resourceType.getSystemId());
        instance.setPath(path);
        return instance;
    }
}
