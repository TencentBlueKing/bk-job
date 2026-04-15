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

package com.tencent.bk.job.gateway.web.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AccessLog数据中转Store。
 * <p>
 * 由于Spring Framework 6.x中request.mutate().header()不再修改底层Netty请求头，
 * Gateway过滤器无法通过请求头向Reactor Netty AccessLog传递数据。
 * 本Store作为Spring层（Gateway过滤器）与Netty层（AccessLog回调）之间的数据桥梁，
 * 以traceId为key，避免将内部数据通过HTTP响应头暴露给API调用方。
 */
@Component
public class AccessLogExchangeDataStore {

    private final Cache<String, Map<String, String>> cache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();

    /**
     * 写入单个字段
     *
     * @param traceId 请求的traceId，作为唯一标识
     * @param key     字段名
     * @param value   字段值
     */
    public void put(String traceId, String key, String value) {
        if (traceId == null || key == null) {
            return;
        }
        cache.asMap().computeIfAbsent(traceId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    /**
     * 读取并移除指定traceId的所有字段（原子操作）
     *
     * @param traceId 请求的traceId
     * @return 字段Map，如果不存在返回null
     */
    public Map<String, String> getAndRemove(String traceId) {
        if (traceId == null) {
            return null;
        }
        return cache.asMap().remove(traceId);
    }
}
