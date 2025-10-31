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

package com.tencent.bk.job.manage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 已启用的租户ID缓存
 */
@Slf4j
public class EnabledTenantIdsCache {

    private final Supplier<Set<String>> tenantIdsSupplier;
    private final String CACHE_KEY_ENABLED_TENANT_IDS = "enabledTenantIds";
    // 已启用的租户ID缓存
    private final LoadingCache<String, Set<String>> enabledTenantIdsCache =
        CacheBuilder.newBuilder().maximumSize(1)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<String>>() {
                       @Override
                       public @Nonnull Set<String> load(@Nonnull String key) {
                           if (CACHE_KEY_ENABLED_TENANT_IDS.equals(key)) {
                               return tenantIdsSupplier.get();
                           }
                           return Collections.emptySet();
                       }
                   }
            );

    public EnabledTenantIdsCache(Supplier<Set<String>> tenantIdsSupplier) {
        this.tenantIdsSupplier = tenantIdsSupplier;
    }

    /**
     * 优先从缓存获取启用的租户ID
     *
     * @return 启用的租户ID集合
     */
    public Set<String> getEnabledTenantIds() {
        try {
            return enabledTenantIdsCache.get(CACHE_KEY_ENABLED_TENANT_IDS);
        } catch (Exception e) {
            log.warn("Fail to get enabled tenantIds from cache", e);
            return tenantIdsSupplier.get();
        }
    }

}
