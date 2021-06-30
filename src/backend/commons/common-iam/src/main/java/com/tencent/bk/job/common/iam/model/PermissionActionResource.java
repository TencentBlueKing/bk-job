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
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作与关联资源
 */
@Data
public class PermissionActionResource {
    /**
     * 操作ID
     *
     * @see com.tencent.bk.job.common.iam.constant.ActionId
     */
    private String actionId;
    /**
     * 操作关联的资源，按照ResourceType分组
     */
    private List<PermissionResourceGroup> resourceGroups = new ArrayList<>();

    public void addPermissionResourceGroup(PermissionResourceGroup resourceGroup) {
        resourceGroups.add(resourceGroup);
    }

    public void addResource(ResourceTypeEnum resourceType, String resourceId, PathInfoDTO pathInfo) {
        PermissionResource resource = new PermissionResource();
        resource.setResourceId(resourceId);
        resource.setResourceType(resourceType);
        resource.setSystemId(resourceType.getSystemId());
        resource.setPathInfo(pathInfo);

        boolean isAdded = false;
        for (PermissionResourceGroup resourceGroup : resourceGroups) {
            if (resourceGroup.getResourceType() == resourceType) {
                resourceGroup.addPermissionResource(resource);
                isAdded = true;
                break;
            }
        }
        if (!isAdded) {
            PermissionResourceGroup resourceGroup = new PermissionResourceGroup();
            resourceGroup.setResourceType(resourceType);
            resourceGroup.setSystemId(resourceType.getSystemId());
            resourceGroup.addPermissionResource(resource);
            this.resourceGroups.add(resourceGroup);
        }
    }

    public void addResource(PermissionResource resource) {
        boolean isAdded = false;
        for (PermissionResourceGroup resourceGroup : resourceGroups) {
            if (resourceGroup.getResourceType() == resource.getResourceType()) {
                resourceGroup.addPermissionResource(resource);
                isAdded = true;
                break;
            }
        }
        if (!isAdded) {
            PermissionResourceGroup resourceGroup = new PermissionResourceGroup();
            resourceGroup.setResourceType(resource.getResourceType());
            resourceGroup.setSystemId(resource.getSystemId());
            resourceGroup.addPermissionResource(resource);
            resourceGroups.add(resourceGroup);
        }
    }
}
