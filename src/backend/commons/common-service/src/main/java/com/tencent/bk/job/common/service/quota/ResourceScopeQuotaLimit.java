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

package com.tencent.bk.job.common.service.quota;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 配额限制-资源管理空间
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ResourceScopeQuotaLimit extends QuotaLimit {

    /**
     * 解析之后的自定义业务配额限制
     */
    private Map<String, Long> customLimits = new HashMap<>();

    public ResourceScopeQuotaLimit(String capacityExpr, String globalLimitExpr, String customLimitExpr) {
        super(capacityExpr, globalLimitExpr, customLimitExpr);
    }

    public long getLimit(ResourceScope resourceScope) {
        String resourceScopeUniqueId = resourceScope.getResourceScopeUniqueId();
        Long limit = customLimits.get(resourceScopeUniqueId);
        if (limit == null) {
            limit = getGlobalLimit();
        }
        return limit;
    }
}
