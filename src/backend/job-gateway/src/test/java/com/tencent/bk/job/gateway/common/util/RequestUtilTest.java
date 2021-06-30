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

package com.tencent.bk.job.gateway.common.util;

import com.tencent.bk.job.common.util.RequestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestUtilTest {
    @Test
    public void whenGetExistHeaderThenReturnHeaderValue() {
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
        List<String> headerValues = new ArrayList<>();
        headerValues.add("test");
        when(mockHttpHeaders.get("existKey")).thenReturn(headerValues);
        when(mockRequest.getHeaders()).thenReturn(mockHttpHeaders);

        String headerValue = RequestUtil.getHeaderValue(mockRequest, "existKey");
        assertThat(headerValue).isEqualTo("test");
    }

    @Test
    public void whenGetNotExistHeaderThenReturnNull() {
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
        when(mockHttpHeaders.get("notExistKey")).thenReturn(null);
        when(mockRequest.getHeaders()).thenReturn(mockHttpHeaders);

        String headerValue = RequestUtil.getHeaderValue(mockRequest, "notExistKey");
        assertThat(headerValue).isNull();
    }

    @Test
    public void whenNoHeadersThenReturnNull() {
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        when(mockRequest.getHeaders()).thenReturn(null);

        String headerValue = RequestUtil.getHeaderValue(mockRequest, "existKey");
        assertThat(headerValue).isNull();
    }

    @Test
    public void whenGetExistCookieThenReturnHeaderValue() {
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        MultiValueMap<String, HttpCookie> mockCookieMap = mock(MultiValueMap.class);
        Map<String, HttpCookie> cookieMap = new HashMap<>();
        HttpCookie httpCookie1 = new HttpCookie("cookie1", "value1");
        cookieMap.put("cookie1", httpCookie1);
        when(mockCookieMap.toSingleValueMap()).thenReturn(cookieMap);
        when(mockRequest.getCookies()).thenReturn(mockCookieMap);

        String cookieValue = RequestUtil.getCookieValue(mockRequest, "cookie1");
        assertThat(cookieValue).isEqualTo("value1");
    }

    @Test
    public void whenGetNotExistCookieThenReturnNull() {
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        MultiValueMap<String, HttpCookie> mockCookieMap = mock(MultiValueMap.class);
        Map<String, HttpCookie> cookieMap = new HashMap<>();
        HttpCookie httpCookie1 = new HttpCookie("cookie1", "value1");
        cookieMap.put("cookie1", httpCookie1);
        when(mockCookieMap.toSingleValueMap()).thenReturn(cookieMap);
        when(mockRequest.getCookies()).thenReturn(mockCookieMap);

        String cookieValue = RequestUtil.getCookieValue(mockRequest, "cookie2");
        assertThat(cookieValue).isNull();
    }

    @Test
    public void whenNoExistAnyCookieThenReturnNull() {
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        when(mockRequest.getCookies()).thenReturn(null);

        String actualCookieValue = RequestUtil.getCookieValue(mockRequest, "cookie1");
        assertThat(actualCookieValue).isNull();

        MultiValueMap<String, HttpCookie> mockHttpCookieMap = mock(MultiValueMap.class);
        when(mockHttpCookieMap.toSingleValueMap()).thenReturn(null);
        when(mockRequest.getCookies()).thenReturn(mockHttpCookieMap);
        String actualCookieValue1 = RequestUtil.getCookieValue(mockRequest, "cookie1");
        assertThat(actualCookieValue1).isNull();
    }


}
