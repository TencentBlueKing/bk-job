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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 把业务层使用的资源类型转换为 IAM v4 的资源类型 ID。
 *
 * <p>业务层统一用 {@link ResourceTypeEnum} 描述资源，其 {@code id} 为 v3 取值；v4 模型中业务集与动态分组
 * 使用了不同的 ID，需要在调用 v4 接口前转换。</p>
 */
@Slf4j
public class IamV4ResourceTypeMapper {

    private static final Map<String, String> V3_TO_V4 = new HashMap<>();

    static {
        V3_TO_V4.put(ResourceTypeId.BUSINESS_SET, IamV4ResourceTypeId.BIZ_SET);
        V3_TO_V4.put(ResourceTypeId.DYNAMIC_GROUP, IamV4ResourceTypeId.DYNAMIC_GROUP);
    }

    /**
     * 按 v3 资源类型 ID 取 v4 资源类型 ID；两版取值一致的资源类型原样返回。
     */
    public static String toV4ResourceTypeId(String v3ResourceTypeId) {
        if (v3ResourceTypeId == null) {
            return null;
        }
        return V3_TO_V4.getOrDefault(v3ResourceTypeId, v3ResourceTypeId);
    }

    public static String toV4ResourceTypeId(ResourceTypeEnum resourceType) {
        return resourceType == null ? null : toV4ResourceTypeId(resourceType.getId());
    }

    /**
     * 取资源范围在 v4 中的资源类型 ID，用于拼接路径首段。
     */
    public static String getV4ResourceTypeIdForScope(ResourceScope resourceScope) {
        ResourceScopeTypeEnum scopeType = resourceScope.getType();
        switch (scopeType) {
            case BIZ:
                return IamV4ResourceTypeId.BIZ;
            case BIZ_SET:
                return IamV4ResourceTypeId.BIZ_SET;
            case TENANT_SET:
                return IamV4ResourceTypeId.TENANT_SET;
            default:
                log.error("Unsupported resource scope type for iam v4: {}", scopeType);
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private IamV4ResourceTypeMapper() {
    }
}
