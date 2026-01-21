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
import com.tencent.bk.job.common.resource.quota.ResourceQuotaLimit;
import com.tencent.bk.job.common.resource.quota.ResourceScopeQuotaLimit;
import com.tencent.bk.job.common.service.quota.ResourceQuotaConfigParseException;
import com.tencent.bk.job.common.service.quota.config.ResourceQuotaLimitProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * 配额限制配置解析-计数类型
 */
public class CounterResourceQuotaConfigParser extends AbstractResourceQuotaConfigParser {

    @Override
    public ResourceQuotaLimit parse(ResourceQuotaLimitProperties.ResourceQuotaLimitProp resourceQuotaLimitProp)
        throws ResourceQuotaConfigParseException {

        ResourceQuotaLimit resourceQuota = new ResourceQuotaLimit();
        try {
            resourceQuota.setEnabled(resourceQuotaLimitProp.isEnabled());
            resourceQuota.setCapacityExpr(resourceQuotaLimitProp.getCapacity());
            Long capacity = parseCapacity(resourceQuotaLimitProp.getCapacity());
            resourceQuota.setCapacity(capacity);

            ResourceQuotaLimitProperties.QuotaLimitProp resourceScopeQuotaLimitProp
                = resourceQuotaLimitProp.getResourceScopeQuotaLimit();
            if (resourceScopeQuotaLimitProp != null) {
                ResourceScopeQuotaLimit resourceScopeQuotaLimit = parseResourceScopeQuotaLimit(
                    capacity,
                    resourceScopeQuotaLimitProp.getGlobal(),
                    resourceScopeQuotaLimitProp.getCustom()
                );
                resourceQuota.setResourceScopeQuotaLimit(resourceScopeQuotaLimit);
            }

            ResourceQuotaLimitProperties.QuotaLimitProp appQuotaLimitProp
                = resourceQuotaLimitProp.getAppQuotaLimit();
            if (resourceQuotaLimitProp.getAppQuotaLimit() != null) {
                AppQuotaLimit appQuotaLimit = parseAppQuotaLimit(
                    capacity,
                    appQuotaLimitProp.getGlobal(),
                    appQuotaLimitProp.getCustom()
                );
                resourceQuota.setAppQuotaLimit(appQuotaLimit);
            }

            return resourceQuota;
        } catch (Throwable e) {
            throw new ResourceQuotaConfigParseException(e);
        }
    }

    @Override
    protected Long parseCapacity(String capacityExpr) {
        return StringUtils.isNotBlank(capacityExpr) ? Long.parseLong(capacityExpr) : null;
    }
}
