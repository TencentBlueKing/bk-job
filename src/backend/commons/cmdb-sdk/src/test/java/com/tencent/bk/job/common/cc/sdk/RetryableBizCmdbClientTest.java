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

package com.tencent.bk.job.common.cc.sdk;

import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 可重试 CMDB 业务客户端测试用例
 */
@DisplayName("RetryableBizCmdbClient 测试")
public class RetryableBizCmdbClientTest {

    private IBizCmdbClient mockDelegate;
    private RetryMetricsRecorder mockMetricsRecorder;
    private ExponentialBackoffRetryPolicy retryPolicy;
    private RetryableBizCmdbClient retryableClient;

    @BeforeEach
    void setUp() {
        mockDelegate = mock(IBizCmdbClient.class);
        mockMetricsRecorder = mock(RetryMetricsRecorder.class);
        // 使用较短的重试间隔以加快测试速度
        retryPolicy = ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(10L)
            .maxIntervalMs(100L)
            .maxAttempts(3)
            .multiplier(2.0)
            .build();
        retryableClient = new RetryableBizCmdbClient(mockDelegate, retryPolicy, mockMetricsRecorder);
    }

    @Nested
    @DisplayName("成功场景测试")
    class SuccessScenarioTest {

        @Test
        @DisplayName("首次调用成功直接返回结果")
        void testFirstAttemptSuccess() {
            List<ApplicationDTO> expectedApps = Collections.singletonList(new ApplicationDTO());
            when(mockDelegate.getAllBizApps(anyString())).thenReturn(expectedApps);

            List<ApplicationDTO> result = retryableClient.getAllBizApps("tenant1");

            assertEquals(expectedApps, result);
            verify(mockDelegate, times(1)).getAllBizApps("tenant1");
            verify(mockMetricsRecorder, times(1)).recordRetry("cmdb", "getAllBizApps", 0, true);
        }

        @Test
        @DisplayName("重试后成功返回结果")
        void testSuccessAfterRetry() {
            AtomicInteger attempts = new AtomicInteger(0);
            List<ApplicationHostDTO> expectedHosts = Collections.singletonList(new ApplicationHostDTO());

            when(mockDelegate.listHostsByHostIds(anyString(), anyList())).thenAnswer(invocation -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new RuntimeException("Temporary CMDB failure");
                }
                return expectedHosts;
            });

            List<ApplicationHostDTO> result = retryableClient.listHostsByHostIds("tenant1", Arrays.asList(1L, 2L));

            assertEquals(expectedHosts, result);
            assertEquals(2, attempts.get());
            verify(mockDelegate, times(2)).listHostsByHostIds(anyString(), anyList());
            verify(mockMetricsRecorder, times(1)).recordRetry("cmdb", "listHostsByHostIds", 1, true);
        }
    }

    @Nested
    @DisplayName("失败场景测试")
    class FailureScenarioTest {

        @Test
        @DisplayName("达到最大重试次数后抛出异常")
        void testFailureAfterMaxAttempts() {
            when(mockDelegate.getAllBizApps(anyString()))
                .thenThrow(new RuntimeException("CMDB unavailable"));

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                retryableClient.getAllBizApps("tenant1")
            );

            assertEquals("CMDB unavailable", exception.getMessage());
            verify(mockDelegate, times(3)).getAllBizApps("tenant1"); // maxAttempts = 3
            verify(mockMetricsRecorder, times(1)).recordRetry("cmdb", "getAllBizApps", 3, false);
        }
    }

    @Nested
    @DisplayName("代理方法测试")
    class DelegateMethodsTest {

        @Test
        @DisplayName("验证 listHostsByCloudIps 方法被正确代理")
        void testListHostsByCloudIps() {
            List<ApplicationHostDTO> expectedHosts = Collections.singletonList(new ApplicationHostDTO());
            when(mockDelegate.listHostsByCloudIps(anyString(), anyList())).thenReturn(expectedHosts);

            List<ApplicationHostDTO> result = retryableClient.listHostsByCloudIps("tenant1", Arrays.asList("0:192.168.1.1"));

            assertEquals(expectedHosts, result);
            verify(mockDelegate, times(1)).listHostsByCloudIps("tenant1", Arrays.asList("0:192.168.1.1"));
        }

        @Test
        @DisplayName("验证 getCloudAreaList 方法被正确代理")
        void testGetCloudAreaList() {
            when(mockDelegate.getCloudAreaList(anyString())).thenReturn(Collections.emptyList());

            retryableClient.getCloudAreaList("tenant1");

            verify(mockDelegate, times(1)).getCloudAreaList("tenant1");
        }
    }
}
