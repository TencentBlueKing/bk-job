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

package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * IAM v3 资源实例（{@link InstanceDTO}）构造工具。仅供 v3 鉴权执行器与仍走 v3 的申请 URL 逻辑复用，
 * v4 侧不使用（v4 直接构造 {@code V4AuthResourceDTO}）。
 */
public class IamV3InstanceBuilder {

    private IamV3InstanceBuilder() {
    }

    public static InstanceDTO buildInstance(ResourceTypeEnum resourceType, String resourceId, PathInfoDTO path) {
        InstanceDTO instance = new InstanceDTO();
        instance.setId(resourceId);
        instance.setType(resourceType.getId());
        instance.setSystem(resourceType.getSystemId());
        instance.setPath(path);
        return instance;
    }

    public static List<InstanceDTO> buildInstanceList(ResourceTypeEnum resourceType, List<String> resourceIds) {
        List<InstanceDTO> instances = new LinkedList<>();
        resourceIds.forEach(resourceId -> instances.add(buildInstance(resourceType, resourceId, null)));
        return instances;
    }

    public static List<InstanceDTO> buildInstanceList(List<PermissionResource> resources) {
        List<InstanceDTO> instances = new LinkedList<>();
        resources.forEach(resource -> instances.add(
            buildInstance(resource.getResourceType(), resource.getResourceId(), resource.getPathInfo())));
        return instances;
    }

    public static InstanceDTO convertPermissionResourceToInstance(PermissionResource permissionResource) {
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
}
