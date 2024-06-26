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

package com.tencent.bk.job.common.resource.quota;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Data;

/**
 * 资源配额限制-基础类
 */
@Data
public class ResourceQuotaLimit {

    public static final long UNLIMITED_VALUE = Long.MAX_VALUE;
    /**
     * 是否启用配额限制
     */
    private boolean enabled;
    /**
     * 配额容量表达式
     */
    private String capacityExpr;
    /**
     * 解析后的配额总量限制
     */
    private Long capacity;

    /**
     * 资源管理空间配额限制
     */
    private ResourceScopeQuotaLimit resourceScopeQuotaLimit;

    /**
     * 应用配额限制
     */
    private AppQuotaLimit appQuotaLimit;

    public long getLimitByResourceScope(ResourceScope resourceScope) {
        if (resourceScopeQuotaLimit == null) {
            return Long.MAX_VALUE;
        }
        return resourceScopeQuotaLimit.getLimit(resourceScope);
    }

    public long getLimitByBkAppCode(String bkAppCode) {
        if (appQuotaLimit == null) {
            return Long.MAX_VALUE;
        }
        return appQuotaLimit.getLimit(bkAppCode);
    }

}
