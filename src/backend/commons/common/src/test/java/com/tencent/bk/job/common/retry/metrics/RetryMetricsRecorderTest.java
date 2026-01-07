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

package com.tencent.bk.job.common.retry.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 重试指标记录器测试用例
 */
@DisplayName("RetryMetricsRecorder 测试")
public class RetryMetricsRecorderTest {

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionalityTest {

        @Test
        @DisplayName("启用指标记录时正确记录指标")
        void testRecordRetryWhenEnabled() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            recorder.recordRetry("cmdb", "listHosts", 0, true);

            Counter counter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "cmdb")
                .tag(RetryMetricsConstants.TAG_KEY_API, "listHosts")
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "0")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS)
                .counter();

            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("禁用指标记录时不记录指标")
        void testRecordRetryWhenDisabled() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, false);

            recorder.recordRetry("cmdb", "listHosts", 0, true);

            // 应该没有记录任何指标
            Counter counter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY).counter();
            assertEquals(null, counter);
        }

        @Test
        @DisplayName("MeterRegistry 为 null 时不抛出异常")
        void testNullMeterRegistry() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(null, true);

            // 不应该抛出异常
            recorder.recordRetry("cmdb", "listHosts", 0, true);
        }
    }

    @Nested
    @DisplayName("指标维度测试")
    class MetricsDimensionTest {

        @Test
        @DisplayName("首次成功时 retry_count=0, final_result=success")
        void testFirstAttemptSuccess() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            recorder.recordRetry("gse", "getScriptResult", 0, true);

            Counter counter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "0")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS)
                .counter();

            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("重试后成功时 retry_count=N, final_result=success")
        void testSuccessAfterRetry() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            // 模拟重试2次后成功
            recorder.recordRetry("iam", "getApplyUrl", 2, true);

            Counter counter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "iam")
                .tag(RetryMetricsConstants.TAG_KEY_API, "getApplyUrl")
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "2")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS)
                .counter();

            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("最终失败时 retry_count=N, final_result=failure")
        void testFinalFailure() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            // 模拟重试5次后失败
            recorder.recordRetry("bk-login", "getUserInfo", 5, false);

            Counter counter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "bk-login")
                .tag(RetryMetricsConstants.TAG_KEY_API, "getUserInfo")
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "5")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_FAILURE)
                .counter();

            assertEquals(1.0, counter.count());
        }
    }

    @Nested
    @DisplayName("不同外部系统测试")
    class DifferentSystemsTest {

        @Test
        @DisplayName("正确记录各外部系统的指标")
        void testDifferentSystems() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            // 记录不同系统的指标
            recorder.recordRetry(RetryMetricsConstants.TAG_VALUE_SYSTEM_GSE, "listAgentState", 0, true);
            recorder.recordRetry(RetryMetricsConstants.TAG_VALUE_SYSTEM_CMDB, "listHostsByHostIds", 1, true);
            recorder.recordRetry(RetryMetricsConstants.TAG_VALUE_SYSTEM_IAM, "registerResource", 2, false);
            recorder.recordRetry(RetryMetricsConstants.TAG_VALUE_SYSTEM_BK_LOGIN, "getUserInfoByToken", 0, true);
            recorder.recordRetry(RetryMetricsConstants.TAG_VALUE_SYSTEM_BK_USER, "listUsersByUsernames", 3, false);

            // 验证 GSE
            Counter gseCounter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "gse")
                .counter();
            assertEquals(1.0, gseCounter.count());

            // 验证 CMDB
            Counter cmdbCounter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "cmdb")
                .counter();
            assertEquals(1.0, cmdbCounter.count());

            // 验证 IAM
            Counter iamCounter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "iam")
                .counter();
            assertEquals(1.0, iamCounter.count());

            // 验证 BK-Login
            Counter bkLoginCounter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "bk-login")
                .counter();
            assertEquals(1.0, bkLoginCounter.count());

            // 验证 BK-User
            Counter bkUserCounter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "bk-user")
                .counter();
            assertEquals(1.0, bkUserCounter.count());
        }
    }

    @Nested
    @DisplayName("累计计数测试")
    class AccumulativeCountTest {

        @Test
        @DisplayName("多次记录同一维度的指标累计计数")
        void testAccumulativeCount() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            // 同一维度记录多次
            recorder.recordRetry("cmdb", "listHosts", 0, true);
            recorder.recordRetry("cmdb", "listHosts", 0, true);
            recorder.recordRetry("cmdb", "listHosts", 0, true);

            Counter counter = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, "cmdb")
                .tag(RetryMetricsConstants.TAG_KEY_API, "listHosts")
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "0")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS)
                .counter();

            assertEquals(3.0, counter.count());
        }

        @Test
        @DisplayName("不同维度的指标分别计数")
        void testDifferentDimensionsSeparateCount() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);

            // 不同维度
            recorder.recordRetry("cmdb", "listHosts", 0, true);
            recorder.recordRetry("cmdb", "listHosts", 1, true);
            recorder.recordRetry("cmdb", "listHosts", 0, false);

            // 验证各维度分别计数
            Counter successNoRetry = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "0")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS)
                .counter();
            assertEquals(1.0, successNoRetry.count());

            Counter successWithRetry = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "1")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS)
                .counter();
            assertEquals(1.0, successWithRetry.count());

            Counter failureNoRetry = meterRegistry.find(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, "0")
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, RetryMetricsConstants.TAG_VALUE_RESULT_FAILURE)
                .counter();
            assertEquals(1.0, failureNoRetry.count());
        }
    }

    @Nested
    @DisplayName("isEnabled 方法测试")
    class IsEnabledTest {

        @Test
        @DisplayName("启用时返回 true")
        void testIsEnabledWhenEnabled() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, true);
            assertTrue(recorder.isEnabled());
        }

        @Test
        @DisplayName("禁用时返回 false")
        void testIsEnabledWhenDisabled() {
            RetryMetricsRecorder recorder = new RetryMetricsRecorder(meterRegistry, false);
            assertFalse(recorder.isEnabled());
        }
    }
}
