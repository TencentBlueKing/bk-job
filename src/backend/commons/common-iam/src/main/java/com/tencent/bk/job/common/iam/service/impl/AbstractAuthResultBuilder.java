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
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

/**
 * 鉴权结果包装基类（版本无关）。供路由层（{@code AppAuthServiceImpl}/{@code AuthServiceImpl}）复用：
 * 把执行器返回的原始结果包装为对外的 {@link AuthResult}，并解析资源显示名用于无权限申请。
 *
 * <p>不涉及任何 IAM 版本相关的资源 DTO 构造（v3 的 {@code InstanceDTO} 由 {@code IamV3InstanceBuilder}
 * 及 v3 执行器负责，v4 的 {@code V4AuthResourceDTO} 由 v4 执行器负责）。</p>
 */
@Slf4j
public abstract class AbstractAuthResultBuilder {

    protected ResourceNameQueryService resourceNameQueryService;

    public void setResourceNameQueryService(ResourceNameQueryService resourceNameQueryService) {
        this.resourceNameQueryService = resourceNameQueryService;
    }

    protected AuthResult buildFailAuthResult(User user,
                                             String actionId,
                                             ResourceTypeEnum resourceType,
                                             Collection<String> resourceIds) {
        AuthResult authResult = AuthResult.fail(user);
        if (resourceType == null) {
            authResult.addRequiredPermission(actionId, null);
        } else {
            for (String resourceId : resourceIds) {
                String resourceName = resourceNameQueryService.getResourceName(resourceType, resourceId);
                authResult.addRequiredPermission(actionId,
                    new PermissionResource(resourceType, resourceId, resourceName));
            }
        }
        return authResult;
    }

    /**
     * 用原始无权限资源构造失败结果，保留 path 和父级层级，避免按 id 重建时丢掉申请单需要的父路径。
     */
    protected AuthResult buildFailAuthResultFromResources(User user,
                                                          String actionId,
                                                          Collection<PermissionResource> deniedResources) {
        AuthResult authResult = AuthResult.fail(user);
        deniedResources.forEach(resource ->
            authResult.addRequiredPermission(actionId, copyAndCompleteDeniedResource(actionId, resource)));
        return authResult;
    }

    private PermissionResource copyAndCompleteDeniedResource(String actionId, PermissionResource source) {
        if (source == null
            || source.getResourceType() == null
            || StringUtils.isBlank(source.getResourceId())) {
            log.error("Invalid denied permission resource, actionId={}, resource={}", actionId, source);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        ResourceTypeEnum resourceType = source.getResourceType();
        PermissionResource target = new PermissionResource();
        target.setResourceType(resourceType);
        target.setResourceId(source.getResourceId());
        target.setSystemId(StringUtils.defaultIfBlank(source.getSystemId(), resourceType.getSystemId()));
        target.setType(StringUtils.defaultIfBlank(source.getType(), resourceType.getId()));
        String resourceName = source.getResourceName();
        if (StringUtils.isBlank(resourceName)) {
            resourceName = resourceNameQueryService.getResourceName(resourceType, source.getResourceId());
        }
        target.setResourceName(resourceName);
        target.setSubResourceType(source.getSubResourceType());
        target.setPathInfo(source.getPathInfo());
        target.setParentHierarchicalResources(source.getParentHierarchicalResources());
        return target;
    }
}
