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

package com.tencent.bk.job.manage.service.auth.impl;

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.manage.service.auth.EsbAuthService;
import com.tencent.bk.sdk.iam.constants.SystemId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EsbAuthServiceImpl implements EsbAuthService {

    private final AuthService authService;

    private final AppAuthService appAuthService;

    public EsbAuthServiceImpl(AuthService authService,
                              AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    @Override
    public EsbResp batchAuthJobResources(String username, String actionId, Long appId, ResourceTypeEnum resourceType,
                                         List<String> resourceIds, Map<String, String> idNameMap) {
        List<String> hasPermissionIds = appAuthService.batchAuth(username, actionId, appId, resourceType, resourceIds);
        resourceIds.removeAll(hasPermissionIds);
        List<String> noPermissionIds = resourceIds;
        if (!noPermissionIds.isEmpty()) {
            // 构造无权限数据
            List<PermissionActionResource> permissionActionResources = new ArrayList<>();
            PermissionActionResource permissionActionResource = new PermissionActionResource();
            permissionActionResource.setActionId(actionId);
            List<PermissionResourceGroup> resourceGroups = new ArrayList<>();
            PermissionResourceGroup permissionResourceGroup = new PermissionResourceGroup();
            permissionResourceGroup.setSystemId(SystemId.JOB);
            permissionResourceGroup.setResourceType(resourceType);
            List<PermissionResource> permissionResources = new ArrayList<>();
            for (int i = 0; i < noPermissionIds.size(); i++) {
                PermissionResource permissionResource = new PermissionResource();
                permissionResource.setSystemId(SystemId.JOB);
                permissionResource.setResourceType(resourceType);
                String resourceId = noPermissionIds.get(i);
                permissionResource.setResourceId(resourceId);
                permissionResource.setResourceName(idNameMap.get(resourceId));
                permissionResources.add(permissionResource);
            }
            permissionResourceGroup.setPermissionResources(permissionResources);
            resourceGroups.add(permissionResourceGroup);
            permissionActionResource.setResourceGroups(resourceGroups);
            permissionActionResources.add(permissionActionResource);
            return authService.buildEsbAuthFailResp(permissionActionResources);
        }
        return null;
    }
}
