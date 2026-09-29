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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.config.JobIamProperties;
import com.tencent.bk.job.common.iam.config.JobIamV4Properties;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthExecutor;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.util.IamAuthVersionHelper;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务范围级鉴权的统一入口 / 路由层。
 *
 * <p>持有 v3 / v4 两个 {@link AppAuthExecutor}，按热更灰度开关 {@link IamAuthVersionHelper#useV4(String)}
 * 选择执行器,只做灰度路由 + 把执行器返回的原始结果包装成 {@link AuthResult}。不构造任何版本相关的资源 DTO,
 * 也不直接依赖 v3/v4 的 IAM 客户端。</p>
 */
@Slf4j
public class AppAuthServiceImpl extends AbstractAuthResultBuilder implements AppAuthService {

    private final AppAuthExecutor v3Executor;
    private final AppAuthExecutor v4Executor;
    private final JobIamProperties jobIamProperties;
    private final JobIamV4Properties jobIamV4Properties;

    public AppAuthServiceImpl(AppAuthExecutor v3Executor,
                              AppAuthExecutor v4Executor,
                              JobIamProperties jobIamProperties,
                              JobIamV4Properties jobIamV4Properties) {
        this.v3Executor = v3Executor;
        this.v4Executor = v4Executor;
        this.jobIamProperties = jobIamProperties;
        this.jobIamV4Properties = jobIamV4Properties;
    }

    private AppAuthExecutor pickExecutor(User user) {
        return IamAuthVersionHelper.useV4(user.getUsername()) ? v4Executor : v3Executor;
    }

    private AppAuthExecutor pickExecutorByContext() {
        return IamAuthVersionHelper.useV4(JobContextUtil.getUsername()) ? v4Executor : v3Executor;
    }

    @Override
    public AuthResult auth(User user, String actionId, AppResourceScope appResourceScope) {
        boolean isAllowed = pickExecutor(user).auth(user, actionId, appResourceScope);
        return isAllowed ? AuthResult.pass(user) : buildScopeFailAuthResult(user, actionId, appResourceScope);
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return pickExecutor(user).batchAuth(user, actionId, appResourceScope, resourceType, resourceIdList);
    }

    @Override
    public AuthResult batchAuthResources(User user,
                                         String actionId,
                                         AppResourceScope appResourceScope,
                                         List<PermissionResource> resources) {
        ResourceTypeEnum resourceType = resources.get(0).getResourceType();
        List<String> allowResourceIds =
            pickExecutor(user).listAllowedResourceIds(user, actionId, appResourceScope, resources);
        List<String> notAllowResourceIds = resources.stream()
            .filter(resource -> !allowResourceIds.contains(resource.getResourceId()))
            .map(PermissionResource::getResourceId)
            .collect(Collectors.toList());
        if (notAllowResourceIds.isEmpty()) {
            return AuthResult.pass(user);
        }
        return buildFailAuthResult(user, actionId, resourceType, notAllowResourceIds);
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  List<PermissionResource> resourceList) {
        return pickExecutor(user).batchAuth(user, actionId, appResourceScope, resourceList);
    }

    @Override
    public AppResourceScopeResult getAppResourceScopeList(User user,
                                                          List<AppResourceScope> allAppResourceScopeList) {
        return pickExecutor(user).getAppResourceScopeList(user, allAppResourceScopeList);
    }

    @Override
    public String getBusinessApplyUrl(String tenantId, AppResourceScope appResourceScope) {
        if (appResourceScope == null) {
            if (IamAuthVersionHelper.useV4(JobContextUtil.getUsername())
                && StringUtils.isNotBlank(jobIamV4Properties.getApplyPageUrl())) {
                return jobIamV4Properties.getApplyPageUrl();
            }
            return jobIamProperties.getWebUrl() + "/apply-join-user-group";
        }
        return pickExecutorByContext().getBusinessApplyUrl(tenantId, appResourceScope);
    }

    private AuthResult buildScopeFailAuthResult(User user, String actionId, AppResourceScope appResourceScope) {
        AuthResult authResult = AuthResult.fail(user);
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        String resourceId = appResourceScope.getId();
        String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
        PermissionResource permissionResource = new PermissionResource(
            ResourceTypeEnum.BUSINESS, resourceId, resourceName);
        if (resourceType == ResourceTypeEnum.BUSINESS_SET) {
            permissionResource.setType(ResourceTypeId.BUSINESS_SET);
            permissionResource.setSubResourceType(resourceType.getId());
        } else if (appResourceScope.getType() == ResourceScopeTypeEnum.TENANT_SET) {
            permissionResource.setType(ResourceTypeId.TENANT_SET);
            permissionResource.setSubResourceType(resourceType.getId());
        }
        permissionResource.setPathInfo(IamUtil.buildScopePathInfo(appResourceScope));
        authResult.addRequiredPermission(actionId, permissionResource);
        return authResult;
    }
}
