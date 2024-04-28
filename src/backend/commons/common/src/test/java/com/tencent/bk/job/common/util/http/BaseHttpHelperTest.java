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
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BaseHttpHelperTest {
    @Test
    @DisplayName("测试 BaseHttpHelper 异常重试")
    void whenGetThrowSocketTimeoutExceptionThenRetry() {
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
                    throw new NoHttpResponseException("");
                });
            }));
        HttpHelper httpHelper = new BaseHttpHelper(retryableHttpClient);
        assertThrows(InternalException.class,
            () -> httpHelper.requestForSuccessResp(
                HttpRequest.builder(HttpMethodEnum.GET, "http://localhost:8080/test").build()));

        // GET + NoHttpResponseException 会被重试
        Mockito.verify(mockedRetryHandler, Mockito.times(4))
            .retryRequest(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }
}
