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
import com.tencent.bk.job.common.service.quota.config.ResourceScopeResourceQuotaConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 配额限制存储
 */
@Slf4j
public class ResourceQuotaStore {

    /**
     * key: QuotaResourceId; value: ResourceQuota
     */
    private volatile Map<String, ResourceQuota> resourceQuotas = new HashMap<>();
    /**
     * 是否初始化
     */
    private volatile boolean isInitial = false;


    public ResourceQuota getResourceQuota(String resourceId) {
        if (!isInitial) {
            synchronized (this) {
                if (!isInitial) {
                    load(true);
                }
            }
        }
        return resourceQuotas.get(resourceId);
    }

    public void load(boolean ignoreException) {
        try {
            loadInternal();
        } catch (Throwable e) {
            log.warn("Load ResourceQuota error", e);
            if (ignoreException) {
                log.warn("Ignore ResourceQuota load error");
            } else {
                throw e;
            }
        }
    }

    private void loadInternal() {
        synchronized (this) {
            log.info("Load ResourceQuota start ...");
            ResourceScopeResourceQuotaConfig resourceScopeResourceQuotaConfig =
                ApplicationContextRegister.getBean(ResourceScopeResourceQuotaConfig.class);

            if (resourceScopeResourceQuotaConfig.getLimitedResources() == null
                || resourceScopeResourceQuotaConfig.getLimitedResources().isEmpty()) {
                log.info("ResourceQuota empty!");
                return;
            }

            log.info("Parse ResourceQuota config: {}", JsonUtils.toJson(resourceScopeResourceQuotaConfig));

            Map<String, ResourceQuota> tmpResourceQuotas = new HashMap<>();
            resourceScopeResourceQuotaConfig.getLimitedResources()
                .forEach((resourceId, resourceQuotaConfig) -> {
                    ResourceQuota resourceQuota = parseResourceQuotaConfig(resourceId, resourceQuotaConfig);
                    tmpResourceQuotas.put(resourceId, resourceQuota);
                });

            // 使用新的配置完全替换老的配置
            resourceQuotas = tmpResourceQuotas;
            log.info("Load ResourceQuota config done! resourceQuotas: {}", resourceQuotas);
            isInitial = true;
        }
    }

    /**
     * 解析特性配置
     *
     * @param resourceId          资源 ID
     * @param resourceQuotaConfig 配置信息
     * @throws ResourceQuotaConfigParseException 如果解析报错，抛出异常
     */
    private ResourceQuota parseResourceQuotaConfig(
        String resourceId,
        ResourceQuotaConfig resourceQuotaConfig
    ) throws ResourceQuotaConfigParseException {

        if (StringUtils.isBlank(resourceId)) {
            log.error("resourceId is blank");
            throw new ResourceQuotaConfigParseException("resourceId is blank");
        }

        ResourceQuotaConfigParser configParser = chooseResourceQuotaConfigParser(resourceId);
        return configParser.parse(resourceQuotaConfig);
    }


    private ResourceQuotaConfigParser chooseResourceQuotaConfigParser(String resourceId) {
        ResourceQuotaConfigParser configParser;
        switch (resourceId) {
            case QuotaResourceId.JOB_INSTANCE:
                configParser = new CounterResourceQuotaConfigParser();
                break;
            default:
                log.error("Unsupported resource: {}", resourceId);
                throw new ResourceQuotaConfigParseException("Unsupported resource: " + resourceId);
        }
        return configParser;
    }
}
