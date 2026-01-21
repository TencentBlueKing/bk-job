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

package com.tencent.bk.job.common.service.quota.config.parser;

import com.tencent.bk.job.common.resource.quota.AppQuotaLimit;
import com.tencent.bk.job.common.resource.quota.QuotaLimit;
import com.tencent.bk.job.common.resource.quota.ResourceScopeQuotaLimit;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 资源配额配置解析-抽象实现
 */
public abstract class AbstractResourceQuotaConfigParser implements ResourceQuotaConfigParser {

    protected abstract Long parseCapacity(String capacityExpr);

    protected ResourceScopeQuotaLimit parseResourceScopeQuotaLimit(Long capacity,
                                                                   String globalLimitExpr,
                                                                   String customLimitExpr) {

        ResourceScopeQuotaLimit resourceScopeQuotaLimit = new ResourceScopeQuotaLimit(
            globalLimitExpr,
            customLimitExpr
        );

        parseGlobalLimit(resourceScopeQuotaLimit, capacity, globalLimitExpr);
        parseCustomLimit(resourceScopeQuotaLimit, capacity, customLimitExpr);

        return resourceScopeQuotaLimit;
    }

    protected AppQuotaLimit parseAppQuotaLimit(Long capacity,
                                               String globalLimitExpr,
                                               String customLimitExpr) {
        AppQuotaLimit appQuotaLimit = new AppQuotaLimit(
            globalLimitExpr,
            customLimitExpr
        );

        parseGlobalLimit(appQuotaLimit, capacity, globalLimitExpr);
        parseCustomLimit(appQuotaLimit, capacity, customLimitExpr);

        return appQuotaLimit;
    }

    private void parseGlobalLimit(QuotaLimit quotaLimit, Long capacity, String globalLimitExpr) {
        long globalLimit = computeLimitValue(capacity, globalLimitExpr);
        quotaLimit.setGlobalLimit(globalLimit);
    }

    private void parseCustomLimit(ResourceScopeQuotaLimit quotaLimit, Long capacity, String customLimitExpr) {
        quotaLimit.setCustomLimits(parseCustomLimit(capacity, customLimitExpr, key -> key));
    }

    private void parseCustomLimit(AppQuotaLimit quotaLimit, Long capacity, String customLimitExpr) {
        quotaLimit.setCustomLimits(parseCustomLimit(capacity, customLimitExpr, key -> key));
    }

    protected <K> Map<K, Long> parseCustomLimit(Long capacity,
                                                String customLimitExpr,
                                                Function<String, K> customKeyParser) {
        Map<K, Long> customLimits = new HashMap<>();
        if (StringUtils.isNotBlank(customLimitExpr)) {

            customLimitExpr = customLimitExpr.trim();
            String[] limitExprParts = customLimitExpr.split(",");
            for (String limitExpr : limitExprParts) {
                String[] limitExprKV = limitExpr.split("=");
                K key = customKeyParser.apply(limitExprKV[0]);
                Long limit = computeLimitValue(capacity, limitExprKV[1]);
                customLimits.put(key, limit);
            }

        }
        return customLimits;
    }

    protected long computeLimitValue(Long capacity, String limitExpr) {
        long limit;
        if (limitExpr.endsWith("%")) {
            limit = computePercentageLimitValue(capacity, limitExpr);
        } else {
            limit = Long.parseLong(limitExpr);
        }
        return limit;
    }

    protected long computePercentageLimitValue(Long capacity, String limitExpr) {
        String percentageValueStr = limitExpr.substring(0, limitExpr.length() - 1);
        int percentageValue = Integer.parseInt(percentageValueStr);
        return capacity * percentageValue / 100;
    }
}
