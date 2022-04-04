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
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class BasicAuthService {

    private ResourceNameQueryService resourceNameQueryService;

    public void setResourceNameQueryService(ResourceNameQueryService resourceNameQueryService) {
        this.resourceNameQueryService = resourceNameQueryService;
    }

    /**
     * 判断用户对Job业务资源是否有直接权限
     *
     * @param username        用户名
     * @param resourceAppInfo Job业务资源
     * @return 是否有权限
     */
    protected boolean hasAppPermission(String username, ResourceAppInfo resourceAppInfo) {
        if (resourceAppInfo == null) {
            return false;
        }
        ResourceScopeTypeEnum resourceScopeType = ResourceScopeTypeEnum.from(resourceAppInfo.getScopeType());
        // 普通业务/CMDB业务集特性开启了的业务集：使用IAM鉴权
        if (resourceScopeType != ResourceScopeTypeEnum.BIZ_SET
            || FeatureToggle.isCmdbBizSetEnabledForApp(resourceAppInfo.getAppId())) {
            return false;
        }
        // 未开启CMDB业务集特性的业务集：使用Job的运维人员鉴权
        log.debug("use maintainers to auth app {}", username);
        return resourceAppInfo.getMaintainerList().contains(username);
    }

    protected AuthResult buildFailAuthResult(String actionId, ResourceTypeEnum resourceType,
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

    protected InstanceDTO convertPermissionResourceToInstance(PermissionResource permissionResource) {
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

    protected InstanceDTO buildInstance(ResourceTypeEnum resourceType, String resourceId, PathInfoDTO path) {
        InstanceDTO instance = new InstanceDTO();
        instance.setId(resourceId);
        instance.setType(resourceType.getId());
        instance.setSystem(resourceType.getSystemId());
        instance.setPath(path);
        return instance;
    }

    protected List<InstanceDTO> buildInstanceList(List<PermissionResource> resources) {
        List<InstanceDTO> instances = new LinkedList<>();
        resources.forEach(resource -> instances.add(buildInstance(resource.getResourceType(), resource.getResourceId(),
            resource.getPathInfo())));
        return instances;
    }
}
