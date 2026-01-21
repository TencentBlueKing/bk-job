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

package com.tencent.bk.job.common.service.quota.config;

import com.tencent.bk.job.common.resource.quota.QuotaResourceId;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 资源配额限制-配置信息
 */
@ConfigurationProperties(prefix = "job.resource-quota-limit", ignoreInvalidFields = true)
@Data
@Slf4j
public class ResourceQuotaLimitProperties {

    /**
     * key: 资源 ID ； value: 资源对应的配额限制
     *
     * @see QuotaResourceId
     */
    private Map<String, ResourceQuotaLimitProp> resources;

    @Data
    @NoArgsConstructor
    public static class ResourceQuotaLimitProp {
        /**
         * 是否启用配额限制
         */
        private boolean enabled;
        /**
         * 配额容量（比如任务数量、存储占用大小等）
         */
        private String capacity;

        /**
         * 资源管理空间-配额
         */
        private QuotaLimitProp resourceScopeQuotaLimit;

        /**
         * 蓝鲸应用-配额
         */
        private QuotaLimitProp appQuotaLimit;

        public ResourceQuotaLimitProp(boolean enabled,
                                      String capacity,
                                      QuotaLimitProp resourceScopeQuotaLimit,
                                      QuotaLimitProp appQuotaLimit) {
            this.enabled = enabled;
            this.capacity = capacity;
            this.resourceScopeQuotaLimit = resourceScopeQuotaLimit;
            this.appQuotaLimit = appQuotaLimit;
        }
    }

    @Data
    @NoArgsConstructor
    public static class QuotaLimitProp {

        /**
         * 全局配额表达式
         */
        private String global;

        /**
         * 自定义配额表达式。会覆盖全局配额配置
         */
        private String custom;

        public QuotaLimitProp(String global, String custom) {
            this.global = global;
            this.custom = custom;
        }
    }

    @PostConstruct
    public void print() {
        log.info("ResourceQuotaLimitProperties init: {}", JsonUtils.toJson(this));
    }
}
