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
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.BasicApp;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 业务基础信息缓存公共抽象类
 */
@Slf4j
public abstract class AbstractBasicAppCache {

    private final LoadingCache<ResourceScope, BasicApp> scopeAndAppCache =
        CacheBuilder.newBuilder().maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<ResourceScope, BasicApp>() {
                       @Override
                       public BasicApp load(ResourceScope resourceScope) {
                           return loadAppToCache(resourceScope);
                       }
                   }
            );

    public BasicApp get(ResourceScope resourceScope) {
        try {
            return scopeAndAppCache.get(resourceScope);
        } catch (ExecutionException e) {
            // 处理被CacheLoader包装的原始异常
            log.error("Get immutable application from cache error", e);
            throw new InternalException("Get immutable application from cache error", e, ErrorCode.INTERNAL_ERROR);
        } catch (UncheckedExecutionException e) {
            // 处理被CacheLoader包装的原始异常
            Throwable t = e.getCause();
            if (t instanceof ServiceException) {
                throw (ServiceException) e.getCause();
            } else {
                log.error("Get immutable application from cache error", e);
                throw new InternalException("Get immutable application from cache error", e, ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    /**
     * 加载到缓存
     *
     * @param resourceScope 资源管理空间
     * @return 业务基础信息
     */
    protected abstract BasicApp loadAppToCache(ResourceScope resourceScope);

}
