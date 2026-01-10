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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于滑动窗口的熔断器实现
 */
@Slf4j
public class SlidingWindowCircuitBreaker implements CircuitBreaker {
    /**
     * 熔断器名称
     */
    private final String name;

    /**
     * 熔断器配置
     */
    private final CircuitBreakerProperties circuitBreakerProperties;

    /**
     * 滑动窗口
     */
    private final SlidingWindow slidingWindow;

    /**
     * 当前状态
     */
    private final AtomicReference<CircuitBreakerState> state;

    /**
     * 状态转换时间戳
     */
    private final AtomicLong stateChangeTime;

    /**
     * 半开状态下的调用计数
     */
    private final AtomicInteger halfOpenCallCount;

    /**
     * 半开状态下的成功调用计数
     */
    private final AtomicInteger halfOpenSuccessCount;

    public SlidingWindowCircuitBreaker(String name, CircuitBreakerProperties circuitBreakerProperties) {
        this.name = name;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.slidingWindow = createSlidingWindow(circuitBreakerProperties);
        this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        this.stateChangeTime = new AtomicLong(System.currentTimeMillis());
        this.halfOpenCallCount = new AtomicInteger(0);
        this.halfOpenSuccessCount = new AtomicInteger(0);
    }

    /**
     * 获取熔断器全名
     *
     * @return 熔断器全名
     */
    @Override
    public String getFullName() {
        return "CircuitBreaker(" + name + ")";
    }

    /**
     * 创建滑动窗口
     */
    private SlidingWindow createSlidingWindow(CircuitBreakerProperties config) {
        return new CountBasedSlidingWindow(config.getSlidingWindowSize());
    }

    @Override
    public boolean canExecute() {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.CLOSED) {
            log.trace("{}:currentState=CLOSED", getFullName());
            return true;
        }

        if (currentState == CircuitBreakerState.OPEN) {
            refreshOpenState();
            currentState = state.get();
        }

        if (currentState == CircuitBreakerState.OPEN) {
            log.debug("{}:currentState=OPEN", getFullName());
            return false;
        }

        if (currentState == CircuitBreakerState.HALF_OPEN) {
            // 半开状态下，只允许有限次数的调用
            if (halfOpenCallCount.get() < circuitBreakerProperties.getPermittedCallsInHalfOpenState()) {
                halfOpenCallCount.incrementAndGet();
                log.debug("{}:currentState=HALF_OPEN, halfOpenCallCount in permit", getFullName());
                return true;
            }
            log.debug("{}:currentState=HALF_OPEN, halfOpenCallCount not in permit", getFullName());
            return false;
        }
        log.warn("{}:Unknown state: {}", getFullName(), currentState);
        return true;
    }

    @Override
    public void onSuccess(long durationMs) {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            halfOpenSuccessCount.incrementAndGet();
            refreshHalfOpenState();
        } else if (currentState == CircuitBreakerState.CLOSED) {
            // 判断是否为慢调用
            boolean isSlowCall = durationMs >= circuitBreakerProperties.getSlowCallDurationThresholdMs();
            slidingWindow.recordSuccess(durationMs, isSlowCall);
            refreshClosedState();
        } else {
            log.debug("{} is open, ignore concurrent success request", getFullName());
        }
    }

    @Override
    public void onError(long durationMs, Throwable throwable) {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            // 半开状态下失败，检查统计数据，决定是否更新状态
            refreshHalfOpenState();
        } else if (currentState == CircuitBreakerState.CLOSED) {
            slidingWindow.recordFailure(durationMs);
            refreshClosedState();
        }
    }

    /**
     * 检查Open状态并刷新
     */
    private void refreshOpenState() {
        // 检查是否可以转换为半开状态
        long now = System.currentTimeMillis();
        long waitTimeInOpen = now - stateChangeTime.get();
        if (waitTimeInOpen >= circuitBreakerProperties.getWaitDurationInOpenStateMs()) {
            log.info("{}:waitTimeInOpen={}ms, change to HALF_OPEN", getFullName(), waitTimeInOpen);
            changeToHalfOpen();
        }
    }

    /**
     * 检查关闭状态并刷新
     */
    private void refreshClosedState() {
        SlidingWindowMetrics metrics = slidingWindow.getMetrics();
        // 检查是否达到最小调用次数
        if (metrics.getTotalCalls() < circuitBreakerProperties.getMinimumNumberOfCalls()) {
            return;
        }

        // 检查失败率是否超过阈值
        if (metrics.getFailureRate() >= circuitBreakerProperties.getFailureRateThreshold()) {
            log.warn(
                "{} failure rate {}% exceeds threshold {}%, change to OPEN",
                getFullName(),
                metrics.getFailureRate(),
                circuitBreakerProperties.getFailureRateThreshold()
            );
            changeToOpen();
            return;
        }

        // 检查慢调用率是否超过阈值
        if (metrics.getSlowCallRate() >= circuitBreakerProperties.getSlowCallRateThreshold()) {
            log.warn(
                "{} slow call rate {}% exceeds threshold {}%, change to OPEN",
                getFullName(),
                metrics.getSlowCallRate(),
                circuitBreakerProperties.getSlowCallRateThreshold()
            );
            changeToOpen();
        }
    }

    /**
     * 检查半开状态并刷新
     */
    private void refreshHalfOpenState() {
        int totalCalls = halfOpenCallCount.get();
        int successCalls = halfOpenSuccessCount.get();
        // 尚未达到允许的最大调用次数
        if (totalCalls < circuitBreakerProperties.getPermittedCallsInHalfOpenState()) {
            return;
        }
        // 已达到允许的最大调用次数，检查成功率，进行状态切换
        float successRate = (float) successCalls / totalCalls * 100;
        // 如果成功率高于阈值，转换为关闭状态
        if (successRate >= (100 - circuitBreakerProperties.getFailureRateThreshold())) {
            log.info(
                "{} success rate {}% in HALF_OPEN state, change to CLOSED",
                getFullName(),
                successRate
            );
            changeToClosed();
        } else {
            log.warn(
                "{} success rate {}% in HALF_OPEN state is too low, change to OPEN",
                getFullName(),
                successRate
            );
            changeToOpen();
        }
    }

    /**
     * 转换为关闭状态
     */
    private void changeToClosed() {
        state.set(CircuitBreakerState.CLOSED);
        stateChangeTime.set(System.currentTimeMillis());
        halfOpenCallCount.set(0);
        halfOpenSuccessCount.set(0);
        slidingWindow.reset();
    }

    /**
     * 转换为开启状态
     */
    private void changeToOpen() {
        state.set(CircuitBreakerState.OPEN);
        stateChangeTime.set(System.currentTimeMillis());
        halfOpenCallCount.set(0);
        halfOpenSuccessCount.set(0);
    }

    /**
     * 转换为半开状态
     */
    private void changeToHalfOpen() {
        state.set(CircuitBreakerState.HALF_OPEN);
        stateChangeTime.set(System.currentTimeMillis());
        halfOpenCallCount.set(0);
        halfOpenSuccessCount.set(0);
    }

    @Override
    public boolean shouldFastFail() {
        return circuitBreakerProperties.getFastFail() != null && circuitBreakerProperties.getFastFail();
    }
}
