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
import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthExecutor;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 业务范围级鉴权 v3 执行器：走 {@link AuthHelper} 拉取策略表达式本地计算，构造 v3 的 {@link InstanceDTO}。
 */
@Slf4j
public class V3AppAuthExecutor extends AbstractV3AuthExecutor implements AppAuthExecutor {

    private final BusinessAuthHelper businessAuthHelper;
    private final PolicyService policyService;
    private final IIamClient iamClient;

    public V3AppAuthExecutor(AuthHelper authHelper,
                             BusinessAuthHelper businessAuthHelper,
                             PolicyService policyService,
                             IIamClient iamClient) {
        super(authHelper);
        this.businessAuthHelper = businessAuthHelper;
        this.policyService = policyService;
        this.iamClient = iamClient;
    }

    @Override
    public boolean auth(User user, String actionId, AppResourceScope appResourceScope) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstanceWithPath(appResourceScope));
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(), actionId,
            buildAppResourceScopeInstanceList(appResourceScope, resourceType, resourceIdList));
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  List<PermissionResource> resourceList) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(), actionId,
            buildInstanceList(resourceList));
    }

    @Override
    public List<String> listAllowedResourceIds(User user,
                                               String actionId,
                                               AppResourceScope appResourceScope,
                                               List<PermissionResource> resources) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(), actionId,
            buildInstanceList(resources));
    }

    @Override
    @SuppressWarnings("unchecked")
    public AppResourceScopeResult getAppResourceScopeList(User user,
                                                          List<AppResourceScope> allAppResourceScopeList) {
        AppResourceScopeResult result = new AppResourceScopeResult();
        result.setAppResourceScopeList(new ArrayList<>());
        result.setAny(false);

        ActionDTO action = new ActionDTO();
        action.setId(ActionId.ACCESS_BUSINESS);
        ExpressionDTO expression = policyService.getPolicyByAction(user.getTenantId(),
            user.getUsername(), action, null);
        if (ExpressionOperationEnum.ANY == expression.getOperator()) {
            result.setAny(true);
        } else {
            if (StringUtils.isNotBlank(expression.getField())
                && expression.getField().equals(ResourceTypeId.BIZ + "." + "id")) {
                if (expression.getValue() instanceof List) {
                    List<?> list = ((List<?>) expression.getValue());
                    if (!list.isEmpty()) {
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

    @Override
    public String getBusinessApplyUrl(String tenantId, AppResourceScope appResourceScope) {
        ActionDTO action = new ActionDTO();
        action.setId(ActionId.ACCESS_BUSINESS);
        RelatedResourceTypeDTO businessResourceType = new RelatedResourceTypeDTO();
        businessResourceType.setType(ResourceTypeEnum.BUSINESS.getId());
        businessResourceType.setSystemId(SystemId.CMDB);
        InstanceDTO instance = new InstanceDTO();
        instance.setId(appResourceScope.getId());
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        instance.setType(resourceType.getId());
        instance.setSystem(resourceType.getSystemId());
        businessResourceType.setInstance(Collections.singletonList(Collections.singletonList(instance)));
        action.setRelatedResourceTypes(Collections.singletonList(businessResourceType));
        return iamClient.getApplyUrl(Collections.singletonList(action));
    }

    private InstanceDTO buildInstanceWithPath(AppResourceScope appResourceScope) {
        InstanceDTO instance = new InstanceDTO();
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            instance.setId(appResourceScope.getId());
            instance.setType(ResourceTypeEnum.BUSINESS.getId());
            instance.setSystem(ResourceTypeEnum.BUSINESS.getSystemId());
            instance.setPath(null);
        } else if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ_SET
            || appResourceScope.getType() == ResourceScopeTypeEnum.TENANT_SET) {
            instance.setType(ResourceTypeEnum.BUSINESS.getId());
            instance.setSystem(ResourceTypeEnum.BUSINESS.getSystemId());
            instance.setPath(IamUtil.buildScopePathInfo(appResourceScope));
        } else {
            FormattingTuple msg = MessageFormatter.format(
                "not supported resourceType:{}", appResourceScope.getType().getValue());
            throw new RuntimeException(msg.getMessage());
        }
        return instance;
    }

    private List<InstanceDTO> buildAppResourceScopeInstanceList(AppResourceScope appResourceScope,
                                                                ResourceTypeEnum resourceType,
                                                                List<String> resourceIds) {
        PathInfoDTO scopePath = IamUtil.buildScopePathInfo(appResourceScope);
        List<InstanceDTO> instances = new LinkedList<>();
        resourceIds.forEach(resourceId -> instances.add(buildInstance(resourceType, resourceId, scopePath)));
        return instances;
    }
}
