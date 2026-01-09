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

package com.tencent.bk.job.common.retry.circuitbreaker;

import com.tencent.bk.job.common.config.CircuitBreakerProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统熔断管理器，按单个API粒度进行熔断
 */
@Slf4j
public class SystemCircuitBreakerManager {
    /**
     * 系统名称
     */
    private final String systemName;

    /**
     * 熔断器配置
     */
    private final CircuitBreakerProperties circuitBreakerProperties;

    /**
     * 白名单API
     */
    private final Set<String> whiteApiSet;

    /**
     * 熔断器Map<API名称，熔断器>
     */
    private final Map<String, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();


    public SystemCircuitBreakerManager(String systemName, CircuitBreakerProperties circuitBreakerProperties) {
        this.systemName = systemName;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.whiteApiSet = new HashSet<>(circuitBreakerProperties.getWhiteApiList());
    }

    /**
     * 针对某个API获取对应熔断器
     *
     * @param apiName API名称
     * @return 熔断器
     */
    public CircuitBreaker getCircuitBreaker(String apiName) {
        if (!circuitBreakerProperties.getEnabled()) {
            return null;
        }
        if (whiteApiSet.contains(apiName)) {
            return null;
        }
        return circuitBreakerMap.computeIfAbsent(apiName, this::buildCircuitBreaker);
    }

    /**
     * 为某个API构建熔断器
     *
     * @param apiName API名称
     * @return 熔断器
     */
    private CircuitBreaker buildCircuitBreaker(String apiName) {
        String name = systemName + ":" + apiName;
        return new SlidingWindowCircuitBreaker(name, circuitBreakerProperties);
    }
}
