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

package com.tencent.bk.job.common.cc.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CloudAreaCache {

    public CloudAreaCache(MeterRegistry meterRegistry) {
        CaffeineCacheMetrics.monitor(meterRegistry, cloudAreaNameCache, "cloudAreaNameCache");
    }

    private static final String UNKNOWN_CLOUD_AREA_NAME = "Unknown";

    private final LoadingCache<Long, String> cloudAreaNameCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .recordStats()
        .build(new CacheLoader<Long, String>() {
            @Override
            public String load(@NotNull Long bkCloudId) {
                IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
                CcCloudAreaInfoDTO cloudArea = bizCmdbClient.getCloudAreaByBkCloudId(bkCloudId);
                // 默认设置为"Unknown",避免本地缓存穿透
                return cloudArea == null ? UNKNOWN_CLOUD_AREA_NAME : cloudArea.getName();
            }

            @NotNull
            @Override
            public Map<Long, String> loadAll(@NotNull Iterable<? extends Long> bkCloudIds) {
                IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
                List<CcCloudAreaInfoDTO> cloudAreaList = bizCmdbClient.getCloudAreaList();
                Map<Long, String> result = new HashMap<>();
                // 默认设置为"Unknown",避免本地缓存穿透
                bkCloudIds.forEach(bkCloudId -> result.put(bkCloudId, UNKNOWN_CLOUD_AREA_NAME));

                if (CollectionUtils.isEmpty(cloudAreaList)) {
                    log.warn("Get all cloud area return empty!");
                    return result;
                }

                cloudAreaList.forEach(cloudArea -> result.put(cloudArea.getId(), cloudArea.getName()));
                return result;
            }
        });

    public Map<Long, String> batchGetCloudAreaNames(Collection<Long> bkCloudIds) {
        if (CollectionUtils.isEmpty(bkCloudIds)) {
            return Collections.emptyMap();
        }
        try {
            long start = System.currentTimeMillis();
            Map<Long, String> cloudAreaIdNames = cloudAreaNameCache.getAll(bkCloudIds);
            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("Batch get cloud area names slow, cost: {}", cost);
            }
            return cloudAreaIdNames;
        } catch (Exception e) {
            log.warn("Fail to get cloud area name", e);
            // 降级实现
            Map<Long, String> failResult = new HashMap<>();
            bkCloudIds.forEach(bkCloudId -> failResult.put(bkCloudId, UNKNOWN_CLOUD_AREA_NAME));
            return failResult;
        }
    }
}
