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
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthExecutor;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iamv4.dto.V4ApplyResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4AuthResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4PermissionDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 业务范围级鉴权 v4 执行器：走 {@link IamV4AuthService} 服务端直接鉴权，构造 v4 的 {@link V4AuthResourceDTO}。
 */
@Slf4j
public class V4AppAuthExecutor extends AbstractV4AuthExecutor implements AppAuthExecutor {

    public V4AppAuthExecutor(IamV4AuthService iamV4AuthService) {
        super(iamV4AuthService);
    }

    @Override
    public boolean auth(User user, String actionId, AppResourceScope appResourceScope) {
        return iamV4AuthService.auth(user.getTenantId(), user.getUsername(), actionId,
            buildV4ResourceFromScope(appResourceScope));
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        PathInfoDTO scopePath = IamUtil.buildScopePathInfo(appResourceScope);
        List<V4AuthResourceDTO> resources = buildV4Resources(resourceType.getId(), resourceIdList, scopePath);
        Map<String, Boolean> resultMap = iamV4AuthService.authByResources(
            user.getTenantId(), user.getUsername(), actionId, resources);
        return extractAllowedResourceIds(resultMap);
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  AppResourceScope appResourceScope,
                                  List<PermissionResource> resourceList) {
        return listAllowedResourceIds(user, actionId, appResourceScope, resourceList);
    }

    @Override
    public List<String> listAllowedResourceIds(User user,
                                               String actionId,
                                               AppResourceScope appResourceScope,
                                               List<PermissionResource> resources) {
        Map<String, Boolean> resultMap = iamV4AuthService.authByResources(
            user.getTenantId(), user.getUsername(), actionId, buildV4Resources(resources));
        return extractAllowedResourceIds(resultMap);
    }

    @Override
    public AppResourceScopeResult getAppResourceScopeList(User user,
                                                          List<AppResourceScope> allAppResourceScopeList) {
        // TODO 需通过关系查询 API(/rbac/.../relations/*) 实现，SDK 尚未封装，暂不支持
        throw new UnsupportedOperationException(
            "getAppResourceScopeList is not supported by IAM v4 yet (relations query API pending)");
    }

    @Override
    public String getBusinessApplyUrl(String tenantId, AppResourceScope appResourceScope) {
        V4ApplyResourceDTO resource = V4ApplyResourceDTO.builder()
            .id(appResourceScope.getId())
            .type(IamV4ResourceTypeMapper.getV4ResourceTypeIdForScope(appResourceScope))
            .build();
        V4PermissionDTO permission = V4PermissionDTO.builder()
            .actionId(ActionId.ACCESS_BUSINESS)
            .resources(Collections.singletonList(resource))
            .build();
        return iamV4AuthService.getApplyUrl(tenantId, Collections.singletonList(permission));
    }

    private V4AuthResourceDTO buildV4ResourceFromScope(AppResourceScope appResourceScope) {
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            return buildV4Resource(ResourceTypeEnum.BUSINESS.getId(), appResourceScope.getId(), null);
        } else if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ_SET
            || appResourceScope.getType() == ResourceScopeTypeEnum.TENANT_SET) {
            return buildV4Resource(ResourceTypeEnum.BUSINESS.getId(), null,
                IamUtil.buildScopePathInfo(appResourceScope));
        } else {
            FormattingTuple msg = MessageFormatter.format(
                "not supported resourceType:{}", appResourceScope.getType().getValue());
            throw new RuntimeException(msg.getMessage());
        }
    }
}
