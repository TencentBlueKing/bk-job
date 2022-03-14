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

package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IamUtil {
    /**
     * 根据资源范围获取IAM资源ID
     *
     * @param resourceScope 资源范围
     * @return ResourceId
     */
    public static String getIamResourceTypeIdForResourceScope(ResourceScope resourceScope) {
        return getIamResourceTypeForResourceScope(resourceScope).getId();
    }

    /**
     * 根据资源范围获取IAM资源类型
     *
     * @param resourceScope 资源范围
     * @return ResourceType
     */
    public static ResourceTypeEnum getIamResourceTypeForResourceScope(ResourceScope resourceScope) {
        ResourceScopeTypeEnum scopeType = resourceScope.getType();
        switch (scopeType) {
            case BIZ:
                return ResourceTypeEnum.BUSINESS;
            case BIZ_SET:
                return ResourceTypeEnum.BUSINESS_SET;
            default:
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 根据IAM资源类型获取资源范围
     *
     * @param resourceType 权限资源类型
     * @param resourceId   权限资源ID
     * @return 资源范围
     */
    public static ResourceScope getResourceScopeFromIamResource(ResourceTypeEnum resourceType, String resourceId) {
        ResourceScopeTypeEnum resourceScopeType;
        if (resourceType == ResourceTypeEnum.BUSINESS) {
            resourceScopeType = ResourceScopeTypeEnum.BIZ;
        } else if (resourceType == ResourceTypeEnum.BUSINESS_SET) {
            resourceScopeType = ResourceScopeTypeEnum.BIZ_SET;
        } else {
            log.error("Invalid iam resource type: {}", resourceType);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        return new ResourceScope(resourceScopeType, resourceId);
    }

    public static PathInfoDTO buildScopePathInfo(ResourceScope resourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(resourceScope),
            resourceScope.getId()).build();
    }
}
