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

/**
 * 熔断器指标常量
 */
public class CircuitBreakerMetricsConstants {

    /**
     * 熔断器状态变更指标名
     */
    public static final String NAME_CIRCUIT_BREAKER_STATE_TRANSITION = "circuit.breaker.state.transition";

    /**
     * 熔断器调用指标名
     */
    public static final String NAME_CIRCUIT_BREAKER_CALL = "circuit.breaker.call";

    /**
     * 熔断器拒绝调用指标名
     */
    public static final String NAME_CIRCUIT_BREAKER_REJECTED = "circuit.breaker.rejected";

    /**
     * 熔断器指标 - 失败率
     */
    public static final String NAME_CIRCUIT_BREAKER_FAILURE_RATE = "circuit.breaker.failure.rate";

    /**
     * 熔断器指标 - 慢调用率
     */
    public static final String NAME_CIRCUIT_BREAKER_SLOW_CALL_RATE = "circuit.breaker.slow.call.rate";

    /**
     * 标签键 - 系统名称
     */
    public static final String TAG_KEY_SYSTEM = "system";

    /**
     * 标签键 - API 名称
     */
    public static final String TAG_KEY_API = "api";

    /**
     * 标签键 - 状态
     */
    public static final String TAG_KEY_STATE = "state";

    /**
     * 标签键 - 从状态
     */
    public static final String TAG_KEY_FROM_STATE = "from_state";

    /**
     * 标签键 - 到状态
     */
    public static final String TAG_KEY_TO_STATE = "to_state";

    /**
     * 标签键 - 结果
     */
    public static final String TAG_KEY_RESULT = "result";

    /**
     * 标签值 - 成功
     */
    public static final String TAG_VALUE_RESULT_SUCCESS = "success";

    /**
     * 标签值 - 失败
     */
    public static final String TAG_VALUE_RESULT_FAILURE = "failure";

    /**
     * 标签值 - 慢调用
     */
    public static final String TAG_VALUE_RESULT_SLOW = "slow";

    /**
     * 标签值 - 拒绝
     */
    public static final String TAG_VALUE_RESULT_REJECTED = "rejected";
}
