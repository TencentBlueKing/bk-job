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

package com.tencent.bk.job.common.redis;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Redis 缓存基础实现类，提供通用能力
 */
public class BaseRedisCache {

    private final MeterRegistry meterRegistry;

    private final String cacheName;

    private static final String METRIC_NAME_JOB_REDIS_CACHE_HITS_TOTAL = "job_redis_cache_hits_total";

    private static final String METRIC_NAME_JOB_REDIS_CACHE_MISSES_TOTAL = "job_redis_cache_misses_total";

    private static final String TAG_CACHE_NAME = "cacheName";

    public BaseRedisCache(MeterRegistry meterRegistry, String cacheName) {
        this.meterRegistry = meterRegistry;
        this.cacheName = cacheName;
    }

    /**
     * 增加缓存命中 key 次数
     */
    public void addHits(long hitCount) {
        meterRegistry.counter(METRIC_NAME_JOB_REDIS_CACHE_HITS_TOTAL, TAG_CACHE_NAME, cacheName)
            .increment(hitCount);
    }

    /**
     * 增加缓存命中 key 次数
     */
    public void addMisses(long missCount) {
        meterRegistry.counter(METRIC_NAME_JOB_REDIS_CACHE_MISSES_TOTAL, TAG_CACHE_NAME, cacheName)
            .increment(missCount);
    }
}
