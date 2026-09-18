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
import com.tencent.bk.job.common.iam.util.IamV4CreatorRoleMapper;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import com.tencent.bk.sdk.iamv4.constants.IamV4SubjectType;
import com.tencent.bk.sdk.iamv4.dto.V4ApplyResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4AuthorizationResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4PermissionDTO;
import com.tencent.bk.sdk.iamv4.dto.V4SubjectDTO;
import com.tencent.bk.sdk.iamv4.dto.request.V4AuthorizationDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 一般资源级鉴权 v4 执行器：走 {@link IamV4AuthService}，构造 v4 的 V4AuthResourceDTO。
 */
@Slf4j
public class V4ActionAuthExecutor extends AbstractV4AuthExecutor implements ActionAuthExecutor {

    /**
     * v4 角色授权的过期时间上限（天）
     */
    private static final long MAX_AUTHORIZE_EXPIRE_DAYS = 365;

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

    /**
     * 资源创建者授权：v4 无资源实例注册接口，改为把该资源类型对应的单资源角色连同新建实例授予创建者，
     * 创建者即拥有该实例上的全部操作（见 openspec design.md D17）。
     *
     * <p>授权失败不抛异常、不阻断资源创建主流程，与 v3 保持一致。</p>
     */
    @Override
    public boolean registerResource(User creator, String id, String name, String type, List<ResourceDTO> ancestors) {
        String v4ResourceTypeId = IamV4ResourceTypeMapper.toV4ResourceTypeId(type);
        String roleId = IamV4CreatorRoleMapper.getCreatorRole(v4ResourceTypeId);
        if (roleId == null) {
            log.warn("No creator role for resource type, skip creator authorization|type={}|id={}",
                v4ResourceTypeId, id);
            return false;
        }
        V4AuthorizationDTO authorization = V4AuthorizationDTO.builder()
            .subject(V4SubjectDTO.builder()
                .type(IamV4SubjectType.USER)
                .id(creator.getUsername())
                .build())
            .roleId(roleId)
            .relatedResourceTypeId(v4ResourceTypeId)
            .resources(Collections.singletonList(V4AuthorizationResourceDTO.builder()
                .type(v4ResourceTypeId)
                .id(id)
                .build()))
            .expiredAt(buildExpiredAt())
            .build();
        try {
            // 创建者授权是 Job 代替用户发起的系统行为，操作人为系统
            iamV4AuthService.authorize(
                creator.getTenantId(), SystemId.JOB, Collections.singletonList(authorization));
            return true;
        } catch (Exception e) {
            log.error("Authorize creator failed|username={}|role={}|type={}|id={}|name={}",
                creator.getUsername(), roleId, v4ResourceTypeId, id, name, e);
            return false;
        }
    }

    /**
     * 授权过期时间。v3 创建者权限是永久的，而 v4 的 {@code expired_at} 上限为 365 天，故取该上限。
     */
    private long buildExpiredAt() {
        return Instant.now().plus(MAX_AUTHORIZE_EXPIRE_DAYS, ChronoUnit.DAYS).getEpochSecond();
    }
}
