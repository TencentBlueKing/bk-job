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

package com.tencent.bk.job.manage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 业务与资源范围转换
 */
@Slf4j
public class AppScopeMappingServiceImpl implements AppScopeMappingService {

    /**
     * appId 与 resourceScope 的映射关系缓存
     * 由于appId与resourceScope映射关系一旦确定之后就不会再发生变化，所以使用本地缓存来优化查询性能
     */
    private final LoadingCache<Long, ResourceScope> appIdAndScopeCache =
        CacheBuilder.newBuilder().maximumSize(100_000).expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<Long, ResourceScope>() {
                       @Override
                       public ResourceScope load(Long appId) {
                           try {
                               ServiceApplicationDTO app = applicationResource.queryAppById(appId);
                               if (app == null) {
                                   return null;
                               }
                               return new ResourceScope(app.getScopeType(), app.getScopeId());
                           } catch (NotFoundException e) {
                               log.error("App not found for appId: {}", appId);
                               return null;
                           }
                       }
                   }
            );

    /**
     * resourceScope 与 appId 的映射关系缓存
     * 由于resourceScope与appId映射关系一旦确定之后就不会再发生变化，所以使用本地缓存来优化查询性能
     */
    private final LoadingCache<ResourceScope, Long> scopeAndAppIdCache =
        CacheBuilder.newBuilder().maximumSize(100_000).expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<ResourceScope, Long>() {
                       @Override
                       public Long load(ResourceScope resourceScope) {
                           try {
                               ServiceApplicationDTO app = applicationResource.queryAppByScope(
                                   resourceScope.getType().getValue(),
                                   resourceScope.getId());
                               if (app == null) {
                                   return null;
                               }
                               return app.getId();
                           } catch (NotFoundException e) {
                               log.error("App not found for scope: {}", resourceScope);
                               return null;
                           }
                       }
                   }
            );


    private final ServiceApplicationResource applicationResource;

    public AppScopeMappingServiceImpl(ServiceApplicationResource applicationResource) {
        this.applicationResource = applicationResource;
    }

    public Long getAppIdByScope(ResourceScope resourceScope) {
        try {
            return scopeAndAppIdCache.get(resourceScope);
        } catch (ExecutionException e) {
            log.error("Get appId from cache error", e);
            throw new InternalException("Get appId from cache error", e, ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Long getAppIdByScope(String scopeType, String scopeId) {
        return getAppIdByScope(new ResourceScope(scopeType, scopeId));
    }

    public ResourceScope getScopeByAppId(Long appId) {
        try {
            return appIdAndScopeCache.get(appId);
        } catch (ExecutionException e) {
            log.error("Get scope from cache error", e);
            throw new InternalException("Get scope from cache error", e, ErrorCode.INTERNAL_ERROR);
        }
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
}
