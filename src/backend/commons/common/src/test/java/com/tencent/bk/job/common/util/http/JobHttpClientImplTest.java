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

package com.tencent.bk.job.common.util.http;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.http.HttpReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobHttpClientImplTest {

    @Test
    @DisplayName("非法协议在发请求前被拒绝")
    void rejectNonHttpBeforeRequest() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        JobHttpClientImpl client = new JobHttpClientImpl(restTemplate);
        HttpReq req = new HttpReq();
        req.setUrl("ftp://127.0.0.1/x");
        req.setBody("{}");
        req.setHeaders(new org.apache.http.Header[0]);

        InternalException ex = assertThrows(InternalException.class, () -> client.post(req));
        assertEquals(ErrorCode.INTERNAL_HTTP_URL_INVALID, ex.getErrorCode());
        verify(restTemplate, never()).postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("POST 同样走 URI 安全校验")
    void postUsesUri() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        JobHttpClientImpl client = new JobHttpClientImpl(restTemplate);
        HttpReq req = new HttpReq();
        req.setUrl("http://127.0.0.1:19809/remote/fileWorker/heartBeat");
        req.setBody("{}");
        req.setHeaders(new org.apache.http.Header[0]);

        assertEquals("ok", client.post(req));
        verify(restTemplate).postForEntity(
            eq(URI.create("http://127.0.0.1:19809/remote/fileWorker/heartBeat")),
            any(HttpEntity.class),
            eq(String.class)
        );
    }
}
