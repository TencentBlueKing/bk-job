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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 令牌桶算法实现的流控工具
 */
@Slf4j
public class FlowControlUtil {

    public static Timer timer = new Timer();
    // token生成精度：1s内分为多少次生成token
    private static volatile boolean initFlag = false;
    private static Integer defaultMaxTokenNum = 300;
    private static Integer precision = 10;
    private static ConcurrentHashMap<String, ResourceToken> tokenMap = new ConcurrentHashMap<>();
    public static TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            long timeStart = System.currentTimeMillis();
            tokenMap.forEach((resourceId, resourceToken) -> {
                AtomicInteger tokenNum = resourceToken.tokenNum;
                int delta = resourceToken.maxTokenNum / precision;
                boolean result = false;
                while (!result) {
                    int currentNum = tokenNum.get();
                    if (currentNum <= resourceToken.maxTokenNum - delta) {
                        result = tokenNum.compareAndSet(currentNum, currentNum + delta);
                    } else {
                        result = true;
                    }
                }
            });
            long duration = System.currentTimeMillis() - timeStart;
            if (duration >= 1000 / precision) {
                log.error("tokenMap update time consuming:" + duration + "ms, need more threads");
            } else if (duration >= 10) {
                log.warn("tokenMap update time consuming:" + duration + "ms");
            }
        }
    };

    /**
     * 工具初始化
     *
     * @param configMap 按资源配置流控峰值/s，持续吞吐量为峰值一半
     */
    public static void init(java.util.Map<String, Integer> configMap) {
        init(configMap, 10, 300);
    }

    /**
     * 工具初始化
     *
     * @param configMap  按资源配置流控峰值/s，持续吞吐量为峰值一半
     * @param pPrecision 每秒内分多少次向令牌桶中添加令牌，该值越小，支持的瞬间并发越高，该值越大，1s内流量分布越均匀
     */
    public static void init(java.util.Map<String, Integer> configMap, Integer pPrecision, Integer pDefaultMaxTokenNum) {
        if (precision != null && precision > 0) {
            precision = pPrecision;
        }
        if (pDefaultMaxTokenNum != null && pDefaultMaxTokenNum > 0) {
            defaultMaxTokenNum = pDefaultMaxTokenNum;
        }
        configMap.forEach((key, value) -> {
            tokenMap.computeIfAbsent(key, new java.util.function.Function<String, ResourceToken>() {
                @Override
                public ResourceToken apply(String s) {
                    return new ResourceToken(new AtomicInteger(value / 2), value / 2);
                }
            });
        });
        timer.schedule(timerTask, 0, 1000 / pPrecision);
        initFlag = true;
    }

    public static boolean initialized() {
        return initFlag;
    }

    /**
     * 在流控下，当前资源是否可用
     *
     * @param resourceId 资源Id
     * @return 资源可用状态
     */
    public static boolean isUsable(String resourceId) {
        if (!tokenMap.containsKey(resourceId)) {
            // 该资源无流控配置，走默认流控参数：最高300/s，最大平均吞吐量150/s
            log.info("No flow control for resource:" + resourceId + ", use default " + defaultMaxTokenNum);
            tokenMap.computeIfAbsent(resourceId, new Function<String, ResourceToken>() {
                @Override
                public ResourceToken apply(String s) {
                    return new ResourceToken(new AtomicInteger(defaultMaxTokenNum / 2), defaultMaxTokenNum / 2);
                }
            });
        }
        AtomicInteger tokenNum = tokenMap.get(resourceId).tokenNum;
        int currentNum = tokenNum.get();
        if (currentNum > 0) {
            return tokenNum.compareAndSet(currentNum, currentNum - 1);
        } else {
            return false;
        }
    }

    static class ResourceToken {
        public AtomicInteger tokenNum;
        public int maxTokenNum;

        public ResourceToken(AtomicInteger tokenNum, int maxTokenNum) {
            this.tokenNum = tokenNum;
            this.maxTokenNum = maxTokenNum;
        }
    }
}
