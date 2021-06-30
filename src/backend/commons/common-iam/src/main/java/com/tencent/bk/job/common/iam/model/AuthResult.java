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

package com.tencent.bk.job.common.iam.model;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权结果
 */
@Data
public class AuthResult {
    /**
     * 是否鉴权通过
     */
    private boolean pass;
    /**
     * 权限申请url
     */
    private String applyUrl;
    /**
     * 依赖的权限操作与资源
     */
    private List<PermissionActionResource> requiredActionResources = new ArrayList<>();

    public static AuthResult pass() {
        AuthResult authResult = new AuthResult();
        authResult.pass = true;
        return authResult;
    }

    public static AuthResult fail() {
        AuthResult authResult = new AuthResult();
        authResult.pass = false;
        return authResult;
    }

    /**
     * 添加依赖的操作与资源
     *
     * @param actionId           操作ID
     * @param permissionResource 资源
     */
    public void addRequiredPermission(String actionId, PermissionResource permissionResource) {
        this.pass = false;

        if (permissionResource == null) {
            PermissionActionResource actionResource = new PermissionActionResource();
            actionResource.setActionId(actionId);
            this.requiredActionResources.add(actionResource);
            return;
        }

        PermissionActionResource actionResource = createOrGetPermissionActionResource(actionId);

        PermissionResourceGroup resourceGroup = null;
        for (PermissionResourceGroup existedResourceGroup : actionResource.getResourceGroups()) {
            if (existedResourceGroup.getResourceType() == permissionResource.getResourceType()) {
                resourceGroup = existedResourceGroup;
            }
        }
        if (resourceGroup == null) {
            resourceGroup = new PermissionResourceGroup();
            resourceGroup.setResourceType(permissionResource.getResourceType());
            resourceGroup.setSystemId(permissionResource.getSystemId());
            actionResource.addPermissionResourceGroup(resourceGroup);
        }
        resourceGroup.addPermissionResource(permissionResource);
    }

    /**
     * 添加依赖的操作与资源
     *
     * @param actionId            操作ID
     * @param permissionResources 资源列表
     */
    public void addRequiredPermissions(String actionId, List<PermissionResource> permissionResources) {
        if (CustomCollectionUtils.isEmptyCollection(permissionResources)) {
            return;
        }
        this.pass = false;

        PermissionActionResource actionResource = createOrGetPermissionActionResource(actionId);

        PermissionResourceGroup resourceGroup = null;
        ResourceTypeEnum resourceType = permissionResources.get(0).getResourceType();
        for (PermissionResourceGroup existedResourceGroup : actionResource.getResourceGroups()) {
            if (existedResourceGroup.getResourceType() == resourceType) {
                resourceGroup = existedResourceGroup;
            }
        }
        if (resourceGroup == null) {
            resourceGroup = new PermissionResourceGroup();
            resourceGroup.setResourceType(resourceType);
            resourceGroup.setSystemId(permissionResources.get(0).getSystemId());
            actionResource.addPermissionResourceGroup(resourceGroup);
        }
        resourceGroup.addPermissionResources(permissionResources);
    }

    private PermissionActionResource createOrGetPermissionActionResource(String actionId) {
        PermissionActionResource actionResource = null;
        for (PermissionActionResource existedActionResource : requiredActionResources) {
            if (existedActionResource.getActionId().equals(actionId)) {
                actionResource = existedActionResource;
                break;
            }
        }
        if (actionResource == null) {
            actionResource = new PermissionActionResource();
            actionResource.setActionId(actionId);
            requiredActionResources.add(actionResource);
        }
        return actionResource;
    }

    /**
     * 对2个鉴权结果进行合并处理
     *
     * @param otherAuthResult 其他鉴权结果
     * @return 合并后的鉴权结果
     */
    public AuthResult mergeAuthResult(AuthResult otherAuthResult) {
        AuthResult authResult = new AuthResult();
        boolean isPass = this.pass && otherAuthResult.isPass();
        authResult.setPass(isPass);
        if (!isPass) {
            List<PermissionActionResource> requiredActionResourceList = new ArrayList<>();
            if (this.requiredActionResources != null && !this.requiredActionResources.isEmpty()) {
                requiredActionResourceList.addAll(this.requiredActionResources);
            }
            if (otherAuthResult.getRequiredActionResources() != null
                && !otherAuthResult.getRequiredActionResources().isEmpty()) {
                requiredActionResourceList.addAll(otherAuthResult.getRequiredActionResources());
            }
            authResult.setRequiredActionResources(requiredActionResourceList);
        }
        return authResult;
    }

}
