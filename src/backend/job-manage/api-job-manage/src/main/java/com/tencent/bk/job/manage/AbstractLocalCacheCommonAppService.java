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
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 业务信息缓存
 */
@Slf4j
public abstract class AbstractLocalCacheCommonAppService implements CommonAppService {

    private final LoadingCache<Long, BasicApp> appIdAndAppCache =
        CacheBuilder.newBuilder().maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<Long, BasicApp>() {
                       @Nonnull
                       @Override
                       public BasicApp load(@Nonnull Long appId) {
                           return queryAppByAppId(appId);
                       }
                   }
            );

    private final LoadingCache<ResourceScope, BasicApp> scopeAndAppCache =
        CacheBuilder.newBuilder().maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<ResourceScope, BasicApp>() {
                       @Nonnull
                       @Override
                       public BasicApp load(@Nonnull ResourceScope resourceScope) {
                           return queryAppByScope(resourceScope);
                       }
                   }
            );


    public AbstractLocalCacheCommonAppService() {
    }

    public Long getAppIdByScope(ResourceScope resourceScope) {
        BasicApp app = getApp(resourceScope);
        return app == null ? null : app.getId();
    }

    @Override
    public Long getAppIdByScope(String scopeType, String scopeId) {
        return getAppIdByScope(new ResourceScope(scopeType, scopeId));
    }

    public ResourceScope getScopeByAppId(Long appId) {
        BasicApp app = queryAppByAppId(appId);
        return app == null ? null : app.getScope();
    }

    @Override
    public AppResourceScope getAppResourceScope(Long appId, String scopeType, String scopeId) {
        if (StringUtils.isNotBlank(scopeType) && StringUtils.isNotBlank(scopeId)) {
            return new AppResourceScope(scopeType, scopeId, getAppIdByScope(scopeType, scopeId));
        } else {
            ResourceScope resourceScope = getScopeByAppId(appId);
            return new AppResourceScope(appId, resourceScope);
        }
    }

    @Override
    public AppResourceScope getAppResourceScope(Long appId) {
        ResourceScope resourceScope = getScopeByAppId(appId);
        return new AppResourceScope(appId, resourceScope);
    }

    @Override
    public AppResourceScope getAppResourceScope(String scopeType, String scopeId) {
        return new AppResourceScope(scopeType, scopeId, getAppIdByScope(scopeType, scopeId));
    }

    public Map<Long, ResourceScope> getScopeByAppIds(Collection<Long> appIds) {
        Map<Long, ResourceScope> mapping = new HashMap<>();
        appIds.forEach(appId -> {
            ResourceScope resourceScope = getScopeByAppId(appId);
            if (resourceScope != null) {
                mapping.put(appId, resourceScope);
            }
        });
        return mapping;
    }

    @Override
    public Map<ResourceScope, Long> getAppIdByScopeList(Collection<ResourceScope> scopeList) {
        Map<ResourceScope, Long> mapping = new HashMap<>();
        scopeList.forEach(scope -> {
            Long appId = getAppIdByScope(scope);
            if (appId != null) {
                mapping.put(scope, appId);
            }
        });
        return mapping;
    }

    /**
     * 根据资源管理空间查询JOB业务信息
     *
     * @param resourceScope 资源管理空间 ID
     * @return 业务
     * @throws NotFoundException 如果业务不存在，抛出NotFoundException
     */
    protected abstract BasicApp queryAppByScope(ResourceScope resourceScope) throws NotFoundException;

    /**
     * 根据 appId 查询 业务
     *
     * @param appId Job业务ID
     * @return 业务
     * @throws NotFoundException 如果业务不存在，抛出NotFoundException
     */
    protected abstract BasicApp queryAppByAppId(Long appId) throws NotFoundException;

    @Override
    public BasicApp getApp(ResourceScope resourceScope) {
        return queryCache(() -> scopeAndAppCache.get(resourceScope));
    }

    @Override
    public BasicApp getApp(Long appId) {
        return queryCache(() -> appIdAndAppCache.get(appId));
    }

    @FunctionalInterface
    public interface QueryCache<T> {
        T query() throws ExecutionException, UncheckedExecutionException;
    }

    private BasicApp queryCache(QueryCache<BasicApp> query) {
        try {
            return query.query();
        } catch (ExecutionException e) {
            // 处理被CacheLoader包装的原始异常
            log.error("Get app from cache error", e);
            throw new InternalException("Get app from cache error", e, ErrorCode.INTERNAL_ERROR);
        } catch (UncheckedExecutionException e) {
            // 处理被CacheLoader包装的原始异常
            Throwable t = e.getCause();
            if (t instanceof ServiceException) {
                throw (ServiceException) e.getCause();
            } else {
                log.error("Get app from cache error", e);
                throw new InternalException("Get app from cache error", e, ErrorCode.INTERNAL_ERROR);
            }
        }
    }
}
