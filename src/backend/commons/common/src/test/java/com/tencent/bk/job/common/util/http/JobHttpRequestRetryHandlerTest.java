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

package com.tencent.bk.job.common.util.http;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Http Client 组件调用测试
 */
public class JobHttpRequestRetryHandlerTest {
    @Test
    @DisplayName("测试 http client ConnectTimeoutException 异常重试")
    void testRetryConnectTimeoutException() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new ConnectTimeoutException();
                });
            }));

        HttpGet get = new HttpGet("http://127.0.0.1:8080/test");
        assertThrows(ConnectTimeoutException.class, () -> retryableHttpClient.execute(get));


        Mockito.verify(mockedRetryHandler, Mockito.times(4))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    @DisplayName("测试 http client SocketTimeoutException 异常重试")
    void testRetrySocketTimeoutException() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new SocketTimeoutException();
                });
            }));
        HttpGet get = new HttpGet("http://127.0.0.1:8080/test");
        assertThrows(SocketTimeoutException.class, () -> retryableHttpClient.execute(get));


        Mockito.verify(mockedRetryHandler, Mockito.times(4))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    @DisplayName("测试 http client NoHttpResponseException 异常重试")
    void whenNoHttpResponseExceptionThenRetry() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new NoHttpResponseException("error");
                });
            }));

        HttpGet get = new HttpGet("http://127.0.0.1:8080/test");
        assertThrows(NoHttpResponseException.class, () -> retryableHttpClient.execute(get));


        Mockito.verify(mockedRetryHandler, Mockito.times(4))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    @DisplayName("其他异常不重试")
    void whenThrowNotRetryableExceptionThenDoNotRetry() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new UnknownHostException("");
                });
            }));

        HttpGet get = new HttpGet("http://127.0.0.1:8080/test");
        assertThrows(UnknownHostException.class, () -> retryableHttpClient.execute(get));


        Mockito.verify(mockedRetryHandler, Mockito.times(1))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    @DisplayName("测试 http client 指定重试模式为RetryModeEnum.ALWAYS, 虽然 POST 不是幂等方法，但是仍然能够重试")
    void whenRetryModeAlwaysAndPostThenRetry() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new SocketTimeoutException();
                });
            }));

        HttpPost post = new HttpPost("http://127.0.0.1:8080/test");
        HttpCoreContext httpContext = HttpCoreContext.create();
        httpContext.setAttribute(HttpConstants.RETRY_MODE, RetryModeEnum.ALWAYS.getValue());
        assertThrows(SocketTimeoutException.class, () -> retryableHttpClient.execute(post, httpContext));


        // 虽然 POST 不是幂等的方法，但是通过设置重试模式为RetryModeEnum.ALWAYS，依然会被重试3 次，共计调用方法 4 次
        Mockito.verify(mockedRetryHandler, Mockito.times(4))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    @DisplayName("测试 http client 指定重试模式为RetryModeEnum.NEVER, 虽然 GET 是幂等方法，但是仍然不能够重试")
    void whenRetryModeNeverAndPostThenRetry() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new SocketTimeoutException();
                });
            }));

        HttpGet get = new HttpGet("http://127.0.0.1:8080/test");
        HttpCoreContext httpContext = HttpCoreContext.create();
        httpContext.setAttribute(HttpConstants.RETRY_MODE, RetryModeEnum.NEVER.getValue());
        assertThrows(SocketTimeoutException.class, () -> retryableHttpClient.execute(get, httpContext));


        // 虽然 GET 是幂等的方法，但是通过设置重试模式为RetryModeEnum.NEVER，不会被重试
        Mockito.verify(mockedRetryHandler, Mockito.times(1))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    @DisplayName("测试 http client 默认策略，非幂等方法比如 POST 不会被重试")
    void whenPostThenDoNotRetry() {
        JobHttpRequestRetryHandler mockedRetryHandler = Mockito.spy(new JobHttpRequestRetryHandler());

        CloseableHttpClient retryableHttpClient = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            1,
            2,
            60,
            true,
            mockedRetryHandler,
            (httpClientBuilder -> {
                httpClientBuilder.addInterceptorFirst((org.apache.http.HttpRequest request, HttpContext context) -> {
                    throw new SocketTimeoutException();
                });
            }));
        HttpPost post = new HttpPost("http://127.0.0.1:8080/test");
        HttpCoreContext httpContext = HttpCoreContext.create();
        assertThrows(SocketTimeoutException.class, () -> retryableHttpClient.execute(post, httpContext));


        // POST 不幂等的方法，所以不重试，只会调用一次
        Mockito.verify(mockedRetryHandler, Mockito.times(1))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }



}
