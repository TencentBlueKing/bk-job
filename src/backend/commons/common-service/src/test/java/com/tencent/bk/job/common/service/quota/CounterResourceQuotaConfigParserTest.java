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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterResourceQuotaConfigParserTest {

    @Test
    void parse() {
        CounterResourceQuotaConfigParser parser = new CounterResourceQuotaConfigParser();

        ResourceQuotaConfig resourceQuotaConfig = new ResourceQuotaConfig("1000", "1%", "biz:2=100,biz:3=20%");
        CounterResourceQuota resourceQuota = (CounterResourceQuota) parser.parse(resourceQuotaConfig);
        assertThat(resourceQuota.getCapacityExpr()).isEqualTo("1000");
        assertThat(resourceQuota.getGlobalLimitExpr()).isEqualTo("1%");
        assertThat(resourceQuota.getCustomLimitExpr()).isEqualTo("biz:2=100,biz:3=20%");
        assertThat(resourceQuota.getCapacity()).isEqualTo(1000L);
        assertThat(resourceQuota.getGlobalLimit()).isEqualTo(10L);
        assertThat(resourceQuota.getCustomResourceScopeLimits()).isNotEmpty();
        assertThat(resourceQuota.getCustomResourceScopeLimits().get("biz:2")).isEqualTo(100L);
        assertThat(resourceQuota.getCustomResourceScopeLimits().get("biz:3")).isEqualTo(200L);

        resourceQuotaConfig = new ResourceQuotaConfig("1000", "10", "biz:2=100,biz:3=20%");
        resourceQuota = (CounterResourceQuota) parser.parse(resourceQuotaConfig);
        assertThat(resourceQuota.getCapacity()).isEqualTo(1000L);
        assertThat(resourceQuota.getGlobalLimit()).isEqualTo(10L);
        assertThat(resourceQuota.getCustomResourceScopeLimits()).isNotEmpty();
        assertThat(resourceQuota.getCustomResourceScopeLimits().get("biz:2")).isEqualTo(100L);
        assertThat(resourceQuota.getCustomResourceScopeLimits().get("biz:3")).isEqualTo(200L);

        resourceQuotaConfig = new ResourceQuotaConfig(null, "10", "biz:2=100");
        resourceQuota = (CounterResourceQuota) parser.parse(resourceQuotaConfig);
        assertThat(resourceQuota.getCapacity()).isNull();
        assertThat(resourceQuota.getGlobalLimit()).isEqualTo(10L);
        assertThat(resourceQuota.getCustomResourceScopeLimits()).isNotEmpty();
        assertThat(resourceQuota.getCustomResourceScopeLimits().get("biz:2")).isEqualTo(100L);

        resourceQuotaConfig = new ResourceQuotaConfig(null, "10", null);
        resourceQuota = (CounterResourceQuota) parser.parse(resourceQuotaConfig);
        assertThat(resourceQuota.getCapacity()).isNull();
        assertThat(resourceQuota.getGlobalLimit()).isEqualTo(10L);
        assertThat(resourceQuota.getCustomResourceScopeLimits()).isEmpty();
    }

    @Test
    void parseInvalidConfig() {
        CounterResourceQuotaConfigParser parser = new CounterResourceQuotaConfigParser();

        assertThatThrownBy(() -> parser.parse(new ResourceQuotaConfig("", "", "")))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(new ResourceQuotaConfig("", "2%", "")))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(new ResourceQuotaConfig("", "2.1%", "")))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(new ResourceQuotaConfig("1000", "2%", "bizss")))
            .isInstanceOf(ResourceQuotaConfigParseException.class);

        assertThatThrownBy(() -> parser.parse(new ResourceQuotaConfig("1000GB", "2%", null)))
            .isInstanceOf(ResourceQuotaConfigParseException.class);
    }
}
