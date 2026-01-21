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

import lombok.Getter;

/**
 * 熔断器状态枚举
 */
@Getter
public enum CircuitBreakerState {
    /**
     * 关闭状态：正常执行，记录调用结果，当失败率超过阈值时转换为 OPEN
     */
    CLOSED(0),

    /**
     * 开启状态：拒绝所有调用（或继续调用但不重试），等待配置的时间后转换为 HALF_OPEN
     */
    OPEN(1),

    /**
     * 半开状态：允许有限次数调用，根据结果决定转换为 CLOSED 或 OPEN
     */
    HALF_OPEN(2);

    /**
     * 状态值
     */
    private final int value;

    CircuitBreakerState(int value) {
        this.value = value;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 对应的枚举
     */
    public static CircuitBreakerState valueOf(int value) {
        for (CircuitBreakerState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid circuit breaker state value: " + value);
    }
}
