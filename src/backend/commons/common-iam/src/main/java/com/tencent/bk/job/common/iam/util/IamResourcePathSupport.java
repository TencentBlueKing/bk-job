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
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 IAM path 解析父级资源。path 可能只含祖先，也可能带上资源自身，后者需要按 type+id 去掉。
 */
public class IamResourcePathSupport {

    private IamResourcePathSupport() {
    }

    public static List<PermissionResource> parentResources(PathInfoDTO pathInfo, PermissionResource resource) {
        List<PermissionResource> parents = new ArrayList<>();
        if (pathInfo == null || resource == null) {
            return parents;
        }
        String resourceType = StringUtils.isNotEmpty(resource.getType())
            ? resource.getType() : resource.getResourceType().getId();
        String resourceId = resource.getResourceId();
        PathInfoDTO node = pathInfo;
        while (node != null) {
            ResourceTypeEnum nodeType = ResourceTypeEnum.getByResourceTypeId(node.getType());
            String nodeId = node.getId();
            if (nodeType != null && StringUtils.isNotEmpty(nodeId)
                && !(node.getType().equals(resourceType) && nodeId.equals(resourceId))) {
                parents.add(new PermissionResource(nodeType, nodeId, null));
            }
            node = node.getChild();
        }
        return parents;
    }
}
