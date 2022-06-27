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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.client.EsbIamClient;
import com.tencent.bk.job.common.iam.config.EsbConfiguration;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO;
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.PolicyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppAuthServiceImpl extends BasicAuthService implements AppAuthService {
    private final AuthHelper authHelper;
    private final BusinessAuthHelper businessAuthHelper;
    private final PolicyService policyService;
    private final EsbIamClient iamClient;
    private ResourceNameQueryService resourceNameQueryService;
    private ResourceAppInfoQueryService resourceAppInfoQueryService;


    public AppAuthServiceImpl(@Autowired AuthHelper authHelper,
                              @Autowired BusinessAuthHelper businessAuthHelper,
                              @Autowired IamConfiguration iamConfiguration,
                              @Autowired PolicyService policyService,
                              @Autowired EsbConfiguration esbConfiguration) {
        this.authHelper = authHelper;
        this.businessAuthHelper = businessAuthHelper;
        this.policyService = policyService;
        this.iamClient = new EsbIamClient(esbConfiguration.getEsbUrl(), iamConfiguration.getAppCode(),
            iamConfiguration.getAppSecret(), esbConfiguration.isUseEsbTestEnv());
    }

    @Override
    public void setResourceAppInfoQueryService(ResourceAppInfoQueryService resourceAppInfoQueryService) {
        this.resourceAppInfoQueryService = resourceAppInfoQueryService;
    }

    @Override
    public void setResourceNameQueryService(ResourceNameQueryService resourceNameQueryService) {
        this.resourceNameQueryService = resourceNameQueryService;
        super.setResourceNameQueryService(resourceNameQueryService);
    }

    private ResourceAppInfo getResourceApp(AppResourceScope appResourceScope) {
        return resourceAppInfoQueryService.getResourceAppInfo(
            IamUtil.getIamResourceTypeForResourceScope(appResourceScope),
            appResourceScope.getId()
        );
    }

    @Override
    public AuthResult auth(String username,
                           String actionId,
                           AppResourceScope appResourceScope) {
        boolean isAllowed = authHelper.isAllowed(username, actionId, buildInstanceWithPath(appResourceScope));
        if (isAllowed) {
            return AuthResult.pass();
        } else {
            return buildFailAuthResult(actionId, appResourceScope);
        }
    }

    public String getApplyUrl(String actionId, AppResourceScope appResourceScope) {
        InstanceDTO instance = new InstanceDTO();
        RelatedResourceTypeDTO relatedResourceType = new RelatedResourceTypeDTO();
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            instance.setId(appResourceScope.getId());
            instance.setType(ResourceTypeEnum.BUSINESS.getId());

            relatedResourceType.setSystemId(ResourceTypeEnum.BUSINESS.getSystemId());
            relatedResourceType.setType(ResourceTypeEnum.BUSINESS.getId());
            relatedResourceType.setInstance(Collections.singletonList(Collections.singletonList(instance)));

        } else if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ_SET) {
            instance.setType(ResourceTypeEnum.BUSINESS_SET.getId());
            instance.setId(appResourceScope.getId());
            instance.setPath(buildResourceScopePath(appResourceScope));

            relatedResourceType.setSystemId(ResourceTypeEnum.BUSINESS.getSystemId());
            relatedResourceType.setType(ResourceTypeEnum.BUSINESS.getId());
            relatedResourceType.setInstance(Collections.singletonList(Collections.singletonList(instance)));
        } else {
            FormattingTuple msg = MessageFormatter.format(
                "not supported resourceType:{}",
                appResourceScope.getType().getValue());
            throw new RuntimeException(msg.getMessage());
        }
        ActionDTO action = new ActionDTO();
        action.setId(actionId);
        action.setRelatedResourceTypes(Collections.singletonList(relatedResourceType));

        return iamClient.getApplyUrl(Collections.singletonList(action));
    }

    private AuthResult buildFailAuthResult(String actionId, AppResourceScope appResourceScope) {
        AuthResult authResult = AuthResult.fail();
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        String resourceId = appResourceScope.getId();
        String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
        PermissionResource permissionResource = new PermissionResource(
            ResourceTypeEnum.BUSINESS, resourceId, resourceName
        );
        if (resourceType == ResourceTypeEnum.BUSINESS_SET) {
            // 层级节点资源类型
            permissionResource.setType(ResourceTypeId.BUSINESS_SET);
            permissionResource.setSubResourceType(resourceType.getId());
        }
        permissionResource.setPathInfo(buildResourceScopePath(appResourceScope));
        authResult.addRequiredPermission(actionId, permissionResource);
        return authResult;
    }

    private PathInfoDTO buildResourceScopePath(AppResourceScope appResourceScope) {
        return IamUtil.buildScopePathInfo(appResourceScope);
    }

    private InstanceDTO buildInstanceWithPath(AppResourceScope appResourceScope) {
        InstanceDTO instance = new InstanceDTO();
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            instance.setId(appResourceScope.getId());
            instance.setType(ResourceTypeEnum.BUSINESS.getId());
            instance.setSystem(ResourceTypeEnum.BUSINESS.getSystemId());
            instance.setPath(null);
        } else if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ_SET) {
            instance.setType(ResourceTypeEnum.BUSINESS.getId());
            instance.setSystem(ResourceTypeEnum.BUSINESS.getSystemId());
            instance.setPath(buildResourceScopePath(appResourceScope));
        } else {
            FormattingTuple msg = MessageFormatter.format(
                "not supported resourceType:{}",
                appResourceScope.getType().getValue());
            throw new RuntimeException(msg.getMessage());
        }
        return instance;
    }

    private InstanceDTO buildInstance(AppResourceScope appResourceScope) {
        InstanceDTO instance = new InstanceDTO();
        instance.setId(appResourceScope.getId());
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        instance.setType(resourceType.getId());
        instance.setSystem(resourceType.getSystemId());
        return instance;
    }

    @Override
    public List<String> batchAuth(String username,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return authHelper.isAllowed(
            username, actionId,
            buildAppResourceScopeInstanceList(appResourceScope, resourceType, resourceIdList));
    }

    @Override
    public AuthResult batchAuthResources(String username,
                                         String actionId,
                                         AppResourceScope appResourceScope,
                                         List<PermissionResource> resources) {
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
    public List<String> batchAuth(String username,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  List<PermissionResource> resourceList) {
        return authHelper.isAllowed(username, actionId, buildInstanceList(resourceList));
    }

    @Override
    public AppResourceScopeResult getAppResourceScopeList(String username,
                                                          List<AppResourceScope> allAppResourceScopeList) {
        AppResourceScopeResult result = new AppResourceScopeResult();
        result.setAppResourceScopeList(new ArrayList<>());
        result.setAny(false);

        ActionDTO action = new ActionDTO();
        action.setId(ActionId.ACCESS_BUSINESS);
        ExpressionDTO expression = policyService.getPolicyByAction(username, action, null);
        if (ExpressionOperationEnum.ANY == expression.getOperator()) {
            result.setAny(true);
        } else {
            if (StringUtils.isNotBlank(expression.getField())
                && expression.getField().equals(ResourceTypeId.BIZ + "." + "id")) {
                if (expression.getValue() instanceof List) {
                    List<?> list = ((List<?>) expression.getValue());
                    if (list.size() > 0) {
                        ((List<String>) expression.getValue()).forEach(id -> {
                            AppResourceScope appResourceScope = new AppResourceScope(
                                ResourceScopeTypeEnum.BIZ, id, null);
                            result.getAppResourceScopeList().add(appResourceScope);
                        });
                    }
                } else if (ExpressionOperationEnum.EQUAL == expression.getOperator()) {
                    result.getAppResourceScopeList().add(
                        new AppResourceScope(ResourceScopeTypeEnum.BIZ, String.valueOf(expression.getValue()), null));
                } else {
                    result.getAppResourceScopeList().addAll(
                        businessAuthHelper.getAuthedAppResourceScopeList(expression, allAppResourceScopeList));
                }
            } else {
                result.getAppResourceScopeList().addAll(
                    businessAuthHelper.getAuthedAppResourceScopeList(expression, allAppResourceScopeList));
            }
        }
        return result;
    }

    private List<InstanceDTO> buildAppResourceScopeInstanceList(AppResourceScope appResourceScope,
                                                                ResourceTypeEnum resourceType,
                                                                List<String> resourceIds) {
        List<InstanceDTO> instances = new LinkedList<>();
        resourceIds.forEach(resourceId -> instances.add(buildInstance(resourceType, resourceId,
            buildResourceScopePath(appResourceScope))));
        return instances;
    }

    @Override
    public String getBusinessApplyUrl(AppResourceScope appResourceScope) {
        ActionDTO action = new ActionDTO();
        action.setId(ActionId.ACCESS_BUSINESS);
        List<RelatedResourceTypeDTO> relatedResourceTypes = new ArrayList<>();
        RelatedResourceTypeDTO businessResourceTypeDTO = new RelatedResourceTypeDTO();
        businessResourceTypeDTO.setType(ResourceTypeEnum.BUSINESS.getId());
        businessResourceTypeDTO.setSystemId(SystemId.CMDB);
        if (appResourceScope != null) {
            List<InstanceDTO> instanceDTOList = new ArrayList<>();
            instanceDTOList.add(buildInstance(appResourceScope));
            businessResourceTypeDTO.setInstance(Collections.singletonList(instanceDTOList));
        } else {
            businessResourceTypeDTO.setInstance(Collections.emptyList());
        }
        relatedResourceTypes.add(businessResourceTypeDTO);
        action.setRelatedResourceTypes(relatedResourceTypes);
        return iamClient.getApplyUrl(Collections.singletonList(action));
    }
}
