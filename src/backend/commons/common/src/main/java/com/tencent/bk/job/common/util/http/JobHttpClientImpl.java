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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
public class JobHttpClientImpl implements JobHttpClient {

    private final RestTemplate restTemplate;

    public JobHttpClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String get(HttpReq req) {
        logReq(req);
        URI uri = toSafeUri(req);
        ResponseEntity<String> respEntity = restTemplate.getForEntity(
            uri,
            String.class
        );
        if (respEntity.getStatusCode() == HttpStatus.OK) {
            String respStr = respEntity.getBody();
            logRespStr(respStr);
            return respStr;
        }
        logAndThrow(respEntity);
        return null;
    }

    @Override
    public String post(HttpReq req) {
        logReq(req);
        HttpHeaders httpHeaders = new HttpHeaders();
        Header[] headers = req.getHeaders();
        for (Header header : headers) {
            if (header.getName() != null && header.getValue() != null) {
                httpHeaders.add(header.getName(), header.getValue());
            }
        }
        String requestJson = req.getBody();
        HttpEntity<String> entity = new HttpEntity<>(requestJson, httpHeaders);
        URI uri = toSafeUri(req);
        ResponseEntity<String> respEntity = restTemplate.postForEntity(
            uri,
            entity,
            String.class
        );
        if (respEntity.getStatusCode() == HttpStatus.OK) {
            String respStr = respEntity.getBody();
            logRespStr(respStr);
            return respStr;
        }
        logAndThrow(respEntity);
        return null;
    }

    /**
     * 使用 {@link URI} 发起请求，避免 RestTemplate 把 String URL 当 URI template 展开；
     * 同时拦截非 http(s)、userinfo、危险解析地址。
     */
    private URI toSafeUri(HttpReq req) {
        String url = req == null ? null : req.getUrl();
        URI uri = HttpUrlSafetyUtils.parseSafeInternalHttpUri(url);
        if (uri == null) {
            log.warn("Reject unsafe internal http url");
            throw new InternalException(ErrorCode.INTERNAL_HTTP_URL_INVALID);
        }
        return uri;
    }

    private void logReq(HttpReq req) {
        if (log.isDebugEnabled()) {
            // 内容中可能有敏感信息，非必要不开启
            log.debug(
                "url={},body={},headers={}",
                req.getUrl(),
                req.getBody(),
                JsonUtils.toJson(req.getHeaders())
            );
        }
    }

    private void logRespStr(String respStr) {
        log.info("respStr={}", respStr);
    }

    private void logAndThrow(ResponseEntity<String> respEntity) {
        log.error("Fail to request, status={}, msg={}", respEntity.getStatusCode(), respEntity.getBody());
        throw new ServiceException(
            ErrorType.INTERNAL,
            ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_WITH_REASON,
            new Object[]{
                "status=" + respEntity.getStatusCode() + ", msg=" + respEntity.getBody()
            }
        );
    }
}
