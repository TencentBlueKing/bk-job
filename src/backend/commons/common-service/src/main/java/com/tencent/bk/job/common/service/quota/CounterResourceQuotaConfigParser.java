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

import com.tencent.bk.job.common.service.quota.config.ResourceQuotaConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 计数类型的配额配置解析
 */
public class CounterResourceQuotaConfigParser implements ResourceQuotaConfigParser {

    @Override
    public ResourceQuota parse(ResourceQuotaConfig resourceQuotaConfig) throws ResourceQuotaConfigParseException {
        CounterResourceQuota resourceQuota = new CounterResourceQuota(
                resourceQuotaConfig.getCapacity(),
                resourceQuotaConfig.getGlobalLimit(),
                resourceQuotaConfig.getCustomLimit()
        );
        try {
            Long capacity = null;
            if (StringUtils.isNotBlank(resourceQuotaConfig.getCapacity())) {
                capacity = Long.parseLong(resourceQuotaConfig.getCapacity());
                resourceQuota.setCapacity(capacity);
            }

            long globalLimit = computeLimitValue(capacity, resourceQuotaConfig.getGlobalLimit());
            resourceQuota.setGlobalLimit(globalLimit);

            String customLimitExpr = resourceQuotaConfig.getCustomLimit();
            if (StringUtils.isNotBlank(customLimitExpr)) {
                Map<String, Long> resourceScopeLimits = new HashMap<>();

                customLimitExpr = customLimitExpr.trim();
                String[] limitExprForResourceScopes = customLimitExpr.split(",");
                for (String resourceScopeLimit : limitExprForResourceScopes) {
                    String[] resourceScopeLimitParts = resourceScopeLimit.split("=");
                    String resourceScope = resourceScopeLimitParts[0];
                    String limitExpr = resourceScopeLimitParts[1];
                    Long limit = computeLimitValue(capacity, limitExpr);
                    resourceScopeLimits.put(resourceScope, limit);
                }
                resourceQuota.setCustomResourceScopeLimits(resourceScopeLimits);
            }
            return resourceQuota;
        } catch (Throwable e) {
            throw new ResourceQuotaConfigParseException(e);
        }
    }

    private long computeLimitValue(Long capacity, String limitExpr) {
        long limit;
        if (limitExpr.endsWith("%")) {
            String percentageValueStr = limitExpr.substring(0, limitExpr.length() - 1);
            int percentageValue = Integer.parseInt(percentageValueStr);
            limit = capacity * percentageValue / 100;
        } else {
            limit = Long.parseLong(limitExpr);
        }
        return limit;
    }
}
