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
import com.tencent.bk.job.common.model.tenant.TenantDTO;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.api.inner.ServiceTenantResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CachedTenantServiceImpl implements TenantService {

    // AppId与租户ID映射表的缓存
    private final LoadingCache<Long, String> appIdToTenantIdMapCache =
        CacheBuilder.newBuilder().maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, String>() {
                       @Override
                       public @Nonnull String load(@Nonnull Long appId) {
                           return serviceTenantResource.getTenantIdByAppId(appId).getData();
                       }
                   }
            );

    private final EnabledTenantIdsCache enabledTenantIdsCache =
        new EnabledTenantIdsCache(this::getEnabledTenantIdsFromRemote);

    private final ServiceTenantResource serviceTenantResource;

    public CachedTenantServiceImpl(ServiceTenantResource serviceTenantResource) {
        this.serviceTenantResource = serviceTenantResource;
    }

    @Override
    public List<TenantDTO> listEnabledTenant() {
        return serviceTenantResource.listEnabledTenant().getData();
    }


    /**
     * 根据本地缓存（过期时间1分钟）的租户数据判断某个租户是否启用
     *
     * @param tenantId 租户ID
     * @return 布尔值
     */
    @Override
    public boolean isTenantEnabledPreferCache(String tenantId) {
        return enabledTenantIdsCache.getEnabledTenantIds().contains(tenantId);
    }

    /**
     * 通过服务间调用获取启用的租户ID
     *
     * @return 启用的租户ID集合
     */
    private Set<String> getEnabledTenantIdsFromRemote() {
        List<TenantDTO> enabledTenantList = listEnabledTenant();
        if (CollectionUtils.isEmpty(enabledTenantList)) {
            return Collections.emptySet();
        }
        return enabledTenantList.stream().map(TenantDTO::getId).collect(Collectors.toSet());
    }

    @Override
    public String getTenantIdByAppId(long appId) {
        try {
            return appIdToTenantIdMapCache.get(appId);
        } catch (ExecutionException e) {
            String message = MessageFormatter.format(
                "Fail to getTenantIdByAppId from cache, appId={}",
                appId
            ).getMessage();
            log.warn(message, e);
            return getTenantIdByAppIdFromRemote(appId);
        }
    }

    /**
     * 通过服务间调用获取租户ID
     *
     * @param appId Job业务ID
     * @return 租户ID
     */
    private String getTenantIdByAppIdFromRemote(long appId) {
        return serviceTenantResource.getTenantIdByAppId(appId).getData();
    }

}
