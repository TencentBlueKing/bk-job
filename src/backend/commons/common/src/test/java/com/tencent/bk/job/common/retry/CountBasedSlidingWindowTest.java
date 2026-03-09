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

package com.tencent.bk.job.common.retry;

import com.tencent.bk.job.common.retry.circuitbreaker.CountBasedSlidingWindow;
import com.tencent.bk.job.common.retry.circuitbreaker.SlidingWindowMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基于计数的滑动窗口测试用例
 */
@DisplayName("CountBasedSlidingWindow 测试")
public class CountBasedSlidingWindowTest {

    private CountBasedSlidingWindow slidingWindow;

    @BeforeEach
    void setUp() {
        // 创建窗口大小为 10 的滑动窗口
        slidingWindow = new CountBasedSlidingWindow(10);
    }

    @Nested
    @DisplayName("成功调用记录测试")
    class SuccessRecordTest {

        @Test
        @DisplayName("记录成功调用，失败率为 0%")
        void testRecordSuccess() {
            // 记录 10 次成功调用
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordSuccess(100L, false);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(0, metrics.getFailureCalls());
            assertEquals(0.0f, metrics.getFailureRate());
        }

        @Test
        @DisplayName("记录成功调用（包含慢调用），慢调用率正确")
        void testRecordSuccessWithSlowCall() {
            // 记录 5 次正常调用
            for (int i = 0; i < 5; i++) {
                slidingWindow.recordSuccess(100L, false);
            }
            // 记录 5 次慢调用
            for (int i = 0; i < 5; i++) {
                slidingWindow.recordSuccess(6000L, true);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(5, metrics.getSlowCalls());
            assertEquals(50.0f, metrics.getSlowCallRate());
        }
    }

    @Nested
    @DisplayName("失败调用记录测试")
    class FailureRecordTest {

        @Test
        @DisplayName("记录失败调用，失败率正确")
        void testRecordFailure() {
            // 记录 7 次成功调用
            for (int i = 0; i < 7; i++) {
                slidingWindow.recordSuccess(100L, false);
            }
            // 记录 3 次失败调用
            for (int i = 0; i < 3; i++) {
                slidingWindow.recordFailure(100L);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(3, metrics.getFailureCalls());
            assertEquals(30, metrics.getFailureRate(), 1e-3);
        }

        @Test
        @DisplayName("全部失败，失败率为 100%")
        void testAllFailures() {
            // 记录 10 次失败调用
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordFailure(100L);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(10, metrics.getFailureCalls());
            assertEquals(100.0f, metrics.getFailureRate());
        }
    }

    @Nested
    @DisplayName("滑动窗口测试")
    class SlidingWindowTest {

        @Test
        @DisplayName("超过窗口大小后，旧数据被移除")
        void testSlidingWindowOverflow() {
            // 记录 10 次失败调用（填满窗口）
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordFailure(100L);
            }

            SlidingWindowMetrics metrics1 = slidingWindow.getMetrics();
            assertEquals(100.0f, metrics1.getFailureRate());

            // 再记录 10 次成功调用（旧的失败记录被移除）
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordSuccess(100L, false);
            }

            SlidingWindowMetrics metrics2 = slidingWindow.getMetrics();
            assertEquals(10, metrics2.getTotalCalls());
            assertEquals(0, metrics2.getFailureCalls());
            assertEquals(0.0f, metrics2.getFailureRate());
        }

        @Test
        @DisplayName("部分数据被移除后，失败率正确")
        void testPartialSliding() {
            // 记录 10 次失败调用
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordFailure(100L);
            }

            // 再记录 5 次成功调用（移除 5 次失败记录）
            for (int i = 0; i < 5; i++) {
                slidingWindow.recordSuccess(100L, false);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(5, metrics.getFailureCalls());
            assertEquals(50.0f, metrics.getFailureRate());
        }
    }

    @Nested
    @DisplayName("慢调用统计测试")
    class SlowCallTest {

        @Test
        @DisplayName("慢调用率计算正确")
        void testSlowCallRate() {
            // 记录 6 次正常调用
            for (int i = 0; i < 6; i++) {
                slidingWindow.recordSuccess(100L, false);
            }
            // 记录 4 次慢调用
            for (int i = 0; i < 4; i++) {
                slidingWindow.recordSuccess(6000L, true);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(4, metrics.getSlowCalls());
            assertEquals(40.0f, metrics.getSlowCallRate());
        }

        @Test
        @DisplayName("全部为慢调用，慢调用率为 100%")
        void testAllSlowCalls() {
            // 记录 10 次慢调用
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordSuccess(6000L, true);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(10, metrics.getSlowCalls());
            assertEquals(100.0f, metrics.getSlowCallRate());
        }

        @Test
        @DisplayName("失败调用不计入慢调用统计")
        void testFailureNotCountedAsSlowCall() {
            // 记录 5 次成功调用
            for (int i = 0; i < 5; i++) {
                slidingWindow.recordSuccess(100L, false);
            }
            // 记录 5 次失败调用
            for (int i = 0; i < 5; i++) {
                slidingWindow.recordFailure(100L);
            }

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(10, metrics.getTotalCalls());
            assertEquals(0, metrics.getSlowCalls());
            assertEquals(0.0f, metrics.getSlowCallRate());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTest {

        @Test
        @DisplayName("空窗口，失败率和慢调用率为 0%")
        void testEmptyWindow() {
            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(0, metrics.getTotalCalls());
            assertEquals(0.0f, metrics.getFailureRate());
            assertEquals(0.0f, metrics.getSlowCallRate());
        }

        @Test
        @DisplayName("单次调用，失败率计算正确")
        void testSingleCall() {
            slidingWindow.recordFailure(100L);

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(1, metrics.getTotalCalls());
            assertEquals(1, metrics.getFailureCalls());
            assertEquals(100.0f, metrics.getFailureRate());
        }

        @Test
        @DisplayName("窗口大小为 1，只保留最新记录")
        void testWindowSizeOne() {
            CountBasedSlidingWindow smallWindow = new CountBasedSlidingWindow(1);

            smallWindow.recordFailure(100L);
            assertEquals(100.0f, smallWindow.getMetrics().getFailureRate());

            smallWindow.recordSuccess(100L, false);
            assertEquals(0.0f, smallWindow.getMetrics().getFailureRate());
        }

        @Test
        @DisplayName("大量调用，性能测试")
        void testLargeNumberOfCalls() {
            CountBasedSlidingWindow largeWindow = new CountBasedSlidingWindow(1000);

            // 记录 10000 次调用
            for (int i = 0; i < 10000; i++) {
                if (i % 2 == 0) {
                    largeWindow.recordSuccess(100L, false);
                } else {
                    largeWindow.recordFailure(100L);
                }
            }

            SlidingWindowMetrics metrics = largeWindow.getMetrics();
            assertEquals(1000, metrics.getTotalCalls());
            // 最后 1000 次调用中，失败率应该接近 50%
            assertTrue(metrics.getFailureRate() >= 45.0f && metrics.getFailureRate() <= 55.0f);
        }
    }

    @Nested
    @DisplayName("重置测试")
    class ResetTest {

        @Test
        @DisplayName("重置后窗口为空")
        void testReset() {
            // 记录一些调用
            for (int i = 0; i < 10; i++) {
                slidingWindow.recordFailure(100L);
            }

            assertEquals(10, slidingWindow.getMetrics().getTotalCalls());

            // 重置
            slidingWindow.reset();

            SlidingWindowMetrics metrics = slidingWindow.getMetrics();
            assertEquals(0, metrics.getTotalCalls());
            assertEquals(0, metrics.getFailureCalls());
            assertEquals(0, metrics.getSlowCalls());
        }
    }
}
