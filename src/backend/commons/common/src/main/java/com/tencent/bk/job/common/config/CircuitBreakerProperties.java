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

package com.tencent.bk.job.common.config;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 熔断器配置
 */
@Data
public class CircuitBreakerProperties {
    /**
     * 是否启用熔断器
     */
    private Boolean enabled;

    /**
     * 失败率阈值（百分比，0-100）
     */
    private Float failureRateThreshold;

    /**
     * 慢调用率阈值（百分比，0-100）
     */
    private Float slowCallRateThreshold;

    /**
     * 慢调用时长阈值（毫秒）
     */
    private Long slowCallDurationThresholdMs;

    /**
     * 滑动窗口大小（调用次数）
     */
    private Integer slidingWindowSize;

    /**
     * 最小调用次数（达到此次数后才开始计算失败率）
     */
    private Integer minimumNumberOfCalls;

    /**
     * 熔断器开启状态下的等待时间（毫秒）
     */
    private Long waitDurationInOpenStateMs;

    /**
     * 半开状态下允许的调用次数
     */
    private Integer permittedCallsInHalfOpenState;

    /**
     * 熔断器开启时是否强制快速失败（true：抛出异常，false：继续调用但不重试）
     */
    private Boolean fastFail;

    /**
     * 白名单（API名称列表，这些API不参与熔断）
     */
    private List<String> whiteApiList;

    /**
     * 获取默认的熔断器配置参数
     *
     * @return 默认的熔断器配置参数
     */
    public static CircuitBreakerProperties defaultConfig() {
        CircuitBreakerProperties circuitBreakerProperties = new CircuitBreakerProperties();
        // 是否启用熔断器：默认关闭
        circuitBreakerProperties.setEnabled(false);
        // 失败率阈值（百分比，0-100），默认 80%
        circuitBreakerProperties.setFailureRateThreshold(80.0f);
        // 慢调用率阈值（百分比，0-100），默认 90%
        circuitBreakerProperties.setSlowCallRateThreshold(90.0f);
        // 慢调用时长阈值（毫秒），默认 30000ms (30秒)
        circuitBreakerProperties.setSlowCallDurationThresholdMs(30000L);
        // 滑动窗口大小（调用次数），默认 100
        circuitBreakerProperties.setSlidingWindowSize(100);
        // 最小调用次数（达到此次数后才开始计算失败率），默认 10
        circuitBreakerProperties.setMinimumNumberOfCalls(10);
        // 熔断器开启状态下的等待时间（毫秒），默认 30000ms (30秒)
        circuitBreakerProperties.setWaitDurationInOpenStateMs(30000L);
        // 半开状态下允许的调用次数，默认 10
        circuitBreakerProperties.setPermittedCallsInHalfOpenState(10);
        // 熔断器开启时是否强制快速失败，默认 false
        circuitBreakerProperties.setFastFail(false);
        // 白名单：默认空列表
        circuitBreakerProperties.setWhiteApiList(new ArrayList<>());
        return circuitBreakerProperties;
    }

    /**
     * 使用传入的默认配置的值填充当前对象的空字段
     *
     * @param defaultProperties 默认配置
     */
    public void fillDefault(CircuitBreakerProperties defaultProperties) {
        if (defaultProperties == null) {
            return;
        }
        if (enabled == null) {
            enabled = defaultProperties.getEnabled();
        }
        if (failureRateThreshold == null) {
            failureRateThreshold = defaultProperties.getFailureRateThreshold();
        }
        if (slowCallRateThreshold == null) {
            slowCallRateThreshold = defaultProperties.getSlowCallRateThreshold();
        }
        if (slowCallDurationThresholdMs == null) {
            slowCallDurationThresholdMs = defaultProperties.getSlowCallDurationThresholdMs();
        }
        if (slidingWindowSize == null) {
            slidingWindowSize = defaultProperties.getSlidingWindowSize();
        }
        if (minimumNumberOfCalls == null) {
            minimumNumberOfCalls = defaultProperties.getMinimumNumberOfCalls();
        }
        if (waitDurationInOpenStateMs == null) {
            waitDurationInOpenStateMs = defaultProperties.getWaitDurationInOpenStateMs();
        }
        if (permittedCallsInHalfOpenState == null) {
            permittedCallsInHalfOpenState = defaultProperties.getPermittedCallsInHalfOpenState();
        }
        if (fastFail == null) {
            fastFail = defaultProperties.getFastFail();
        }
        if (whiteApiList == null) {
            whiteApiList = defaultProperties.getWhiteApiList();
        }
    }
}
