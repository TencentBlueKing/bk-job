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

package com.tencent.bk.job.common.service.quota;

import com.tencent.bk.job.common.resource.quota.ResourceQuotaLimit;
import com.tencent.bk.job.common.service.quota.config.ResourceQuotaLimitProperties;
import com.tencent.bk.job.common.service.quota.config.parser.CounterResourceQuotaConfigParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterResourceQuotaConfigParserTest {

    @Test
    void parseByPercentageLimitValue() {
        CounterResourceQuotaConfigParser parser = new CounterResourceQuotaConfigParser();

        ResourceQuotaLimitProperties.QuotaLimitProp resourceScopeQuotaLimit =
            new ResourceQuotaLimitProperties.QuotaLimitProp("1%", "biz:2=10%,biz:3=15%");
        ResourceQuotaLimitProperties.QuotaLimitProp appQuotaLimit =
            new ResourceQuotaLimitProperties.QuotaLimitProp("2%", "bk-soap=20%,bk-nodeman=10%");
        ResourceQuotaLimitProperties.ResourceQuotaLimitProp resourceQuotaLimitProp =
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(true, "1000", resourceScopeQuotaLimit,
                    appQuotaLimit);

        ResourceQuotaLimit resourceQuota = parser.parse(resourceQuotaLimitProp);

        assertThat(resourceQuota.getCapacityExpr()).isEqualTo("1000");
        assertThat(resourceQuota.getCapacity()).isEqualTo(1000L);

        assertThat(resourceQuota.getResourceScopeQuotaLimit().getGlobalLimitExpr()).isEqualTo("1%");
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimitExpr()).isEqualTo("biz:2=10%,biz:3=15%");
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getGlobalLimit()).isEqualTo(10L);
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits()).isNotEmpty();
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits().get("biz:2")).isEqualTo(100L);
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits().get("biz:3")).isEqualTo(150L);

        assertThat(resourceQuota.getAppQuotaLimit().getGlobalLimitExpr()).isEqualTo("2%");
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimitExpr()).isEqualTo("bk-soap=20%,bk-nodeman=10%");
        assertThat(resourceQuota.getAppQuotaLimit().getGlobalLimit()).isEqualTo(20L);
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits()).isNotEmpty();
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits().get("bk-soap")).isEqualTo(200L);
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits().get("bk-nodeman")).isEqualTo(100L);

        resourceScopeQuotaLimit =
            new ResourceQuotaLimitProperties.QuotaLimitProp("10%", "biz:2=20%");
        appQuotaLimit =
            new ResourceQuotaLimitProperties.QuotaLimitProp("20%", "bk-soap=40%");
        resourceQuotaLimitProp =
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(true, "16", resourceScopeQuotaLimit,
                appQuotaLimit);
        resourceQuota = parser.parse(resourceQuotaLimitProp);

        assertThat(resourceQuota.getCapacityExpr()).isEqualTo("16");
        assertThat(resourceQuota.getCapacity()).isEqualTo(16L);

        // 测试配额计算值向下取整
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getGlobalLimitExpr()).isEqualTo("10%");
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getGlobalLimit()).isEqualTo(1L);
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits().get("biz:2")).isEqualTo(3L);
        assertThat(resourceQuota.getAppQuotaLimit().getGlobalLimitExpr()).isEqualTo("20%");
        assertThat(resourceQuota.getAppQuotaLimit().getGlobalLimit()).isEqualTo(3L);
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits().get("bk-soap")).isEqualTo(6L);
    }

    @Test
    void parseByExplicitLimitValue() {
        CounterResourceQuotaConfigParser parser = new CounterResourceQuotaConfigParser();

        ResourceQuotaLimitProperties.QuotaLimitProp resourceScopeQuotaLimit =
            new ResourceQuotaLimitProperties.QuotaLimitProp("10", "biz:2=100,biz:3=150");
        ResourceQuotaLimitProperties.QuotaLimitProp appQuotaLimit =
            new ResourceQuotaLimitProperties.QuotaLimitProp("20", "bk-soap=100,bk-nodeman=150");
        ResourceQuotaLimitProperties.ResourceQuotaLimitProp resourceQuotaLimitProp =
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(true, null, resourceScopeQuotaLimit, appQuotaLimit);

        ResourceQuotaLimit resourceQuota = parser.parse(resourceQuotaLimitProp);

        assertThat(resourceQuota.getCapacityExpr()).isNull();
        assertThat(resourceQuota.getCapacity()).isNull();

        assertThat(resourceQuota.getResourceScopeQuotaLimit().getGlobalLimitExpr()).isEqualTo("10");
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimitExpr()).isEqualTo("biz:2=100,biz:3=150");
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getGlobalLimit()).isEqualTo(10L);
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits()).isNotEmpty();
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits().get("biz:2")).isEqualTo(100L);
        assertThat(resourceQuota.getResourceScopeQuotaLimit().getCustomLimits().get("biz:3")).isEqualTo(150L);

        assertThat(resourceQuota.getAppQuotaLimit().getGlobalLimitExpr()).isEqualTo("20");
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimitExpr()).isEqualTo("bk-soap=100,bk-nodeman=150");
        assertThat(resourceQuota.getAppQuotaLimit().getGlobalLimit()).isEqualTo(20L);
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits()).isNotEmpty();
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits().get("bk-soap")).isEqualTo(100L);
        assertThat(resourceQuota.getAppQuotaLimit().getCustomLimits().get("bk-nodeman")).isEqualTo(150L);
    }

    @Test
    void parseInvalidConfig() {
        CounterResourceQuotaConfigParser parser = new CounterResourceQuotaConfigParser();

        assertThatThrownBy(() -> parser.parse(
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(
                true,
                "",
                new ResourceQuotaLimitProperties.QuotaLimitProp("2%", null),
                null)))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(
                true,
                "",
                null,
                new ResourceQuotaLimitProperties.QuotaLimitProp("2%", null))))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(
                true,
                "1000",
                new ResourceQuotaLimitProperties.QuotaLimitProp("2%", "error_custom_settings"),
                null)))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(
                true,
                "1000",
                null,
                new ResourceQuotaLimitProperties.QuotaLimitProp("2%", "error_custom_settings"))))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(
            new ResourceQuotaLimitProperties.ResourceQuotaLimitProp(
                true,
                "1000G",
                null,
                new ResourceQuotaLimitProperties.QuotaLimitProp("2%", null))))
            .isInstanceOf(ResourceQuotaConfigParseException.class);
    }
}
