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

import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.exception.InternalException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Http Client 组件调用测试
 */
public class HttpClientTest {
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
        HttpHelper httpHelper = new BaseHttpHelper(retryableHttpClient);

        assertThrows(InternalException.class,
            () -> httpHelper.request(HttpRequest.builder(HttpMethodEnum.GET, "http://127.0.0.1:8080/test").build()));


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
        HttpHelper httpHelper = new BaseHttpHelper(retryableHttpClient);

        assertThrows(InternalException.class,
            () -> httpHelper.request(HttpRequest.builder(HttpMethodEnum.GET, "http://127.0.0.1:8080/test").build()));


        Mockito.verify(mockedRetryHandler, Mockito.times(4))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }




}
