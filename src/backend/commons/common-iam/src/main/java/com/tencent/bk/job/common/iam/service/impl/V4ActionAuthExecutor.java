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

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.ActionAuthExecutor;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4ApplyResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4PermissionDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 一般资源级鉴权 v4 执行器：走 {@link IamV4AuthService}，构造 v4 的 V4AuthResourceDTO。
 */
@Slf4j
public class V4ActionAuthExecutor extends AbstractV4AuthExecutor implements ActionAuthExecutor {

    public V4ActionAuthExecutor(IamV4AuthService iamV4AuthService) {
        super(iamV4AuthService);
    }

    @Override
    public boolean auth(User user, String actionId) {
        return iamV4AuthService.auth(user.getTenantId(), user.getUsername(), actionId);
    }

    @Override
    public boolean auth(User user,
                        String actionId,
                        ResourceTypeEnum resourceType,
                        String resourceId,
                        PathInfoDTO pathInfo) {
        return iamV4AuthService.auth(user.getTenantId(), user.getUsername(), actionId,
            buildV4Resource(resourceType.getId(), resourceId, pathInfo));
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        Map<String, Boolean> resultMap = iamV4AuthService.authByResources(user.getTenantId(), user.getUsername(),
            actionId, buildV4Resources(resourceType.getId(), resourceIdList, null));
        return extractAllowedResourceIds(resultMap);
    }

    @Override
    public List<String> listAllowedResourceIds(User user, String actionId, List<PermissionResource> resources) {
        Map<String, Boolean> resultMap = iamV4AuthService.authByResources(user.getTenantId(), user.getUsername(),
            actionId, buildV4Resources(resources));
        return extractAllowedResourceIds(resultMap);
    }

    @Override
    public String getApplyUrl(String actionId, ResourceTypeEnum resourceType) {
        V4ApplyResourceDTO resource = V4ApplyResourceDTO.builder()
            .type(IamV4ResourceTypeMapper.toV4ResourceTypeId(resourceType))
            .build();
        V4PermissionDTO permission = V4PermissionDTO.builder()
            .actionId(actionId)
            .resources(Collections.singletonList(resource))
            .build();
        return iamV4AuthService.getApplyUrl(null, Collections.singletonList(permission));
    }

    @Override
    public String getApplyUrl(String tenantId, List<PermissionActionResource> permissionActionResources) {
        return iamV4AuthService.getApplyUrl(tenantId, buildV4Permissions(permissionActionResources));
    }

    @Override
    public boolean registerResource(User creator, String id, String name, String type, List<ResourceDTO> ancestors) {
        // TODO 资源创建者授权待定：IAM v4 通过角色授权(authorize)承载，role_id/expired_at 等语义确认后再实现
        log.warn("registerResource is not implemented for IAM v4 yet, skip. id={}, name={}, type={}", id, name, type);
        return true;
    }
}
