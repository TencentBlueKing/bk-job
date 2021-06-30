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

package com.tencent.bk.job.gateway.filter;

import com.tencent.bk.job.gateway.model.LicenseCheckResultDTO;
import com.tencent.bk.job.gateway.service.LicenseCheckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.mockito.Mockito.*;

public class LicenseCheckGatewayFilterFactoryTest {
    @Test
    public void whenLicenseCheckPassThenForwardFilter() {
        ServerWebExchange mockExchange = spy(ServerWebExchange.class);
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);
        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);

        when(mockExchange.getResponse()).thenReturn(mockResponse);
        when(mockExchange.getRequest()).thenReturn(mockRequest);

        LicenseCheckService mockLicenseService = mock(LicenseCheckService.class);
        LicenseCheckResultDTO checkPassResult = new LicenseCheckResultDTO();
        checkPassResult.setOk(true);
        when(mockLicenseService.checkLicense()).thenReturn(checkPassResult);

        LicenseCheckGatewayFilterFactory factory = new LicenseCheckGatewayFilterFactory(mockLicenseService);
        factory.filter(mockExchange, mockChain);

        verify(mockResponse, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(mockChain, times(1)).filter(mockExchange);
    }

    @DisplayName("证书不合法返回401")
    @Test
    public void whenLicenseIsInvalidThenReturnUnauthorizedResponseStatus() {
        ServerWebExchange mockExchange = spy(ServerWebExchange.class);
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);
        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);

        when(mockExchange.getResponse()).thenReturn(mockResponse);
        when(mockExchange.getRequest()).thenReturn(mockRequest);

        LicenseCheckService mockLicenseService = mock(LicenseCheckService.class);
        LicenseCheckResultDTO checkPassResult = new LicenseCheckResultDTO();
        checkPassResult.setOk(false);
        when(mockLicenseService.checkLicense()).thenReturn(checkPassResult);

        LicenseCheckGatewayFilterFactory factory = new LicenseCheckGatewayFilterFactory(mockLicenseService);
        factory.filter(mockExchange, mockChain);

        verify(mockChain, never()).filter(any(ServerWebExchange.class));
        verify(mockResponse, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);

    }
}
