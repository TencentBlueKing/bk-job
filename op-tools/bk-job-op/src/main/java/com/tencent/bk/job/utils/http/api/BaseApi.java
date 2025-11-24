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

package com.tencent.bk.job.utils.http.api;

import com.tencent.bk.job.utils.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP请求基础API
 */
@Slf4j
@Component
public class BaseApi {

    private final RestTemplate restTemplate;

    /**
     * 带RestTemplate参数的构造函数
     */
    public BaseApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param params  URL参数
     * @param body    请求体
     * @param headers 请求头
     * @return 原始响应字符串
     */
    public String doPostAndGetResponseStr(String url, Map<String, String> params, Object body, HttpHeaders headers) {
        try {
            String fullUrl = buildUrlWithParams(url, params);
            HttpEntity<Object> requestEntity = buildRequestEntity(body, headers);

            log.info("POST request: url={}, params={}, body={}", fullUrl, params, JsonUtils.toJson(body));

            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.POST,
                requestEntity,
                String.class);

            String respStr = response.getBody();
            log.info("POST response: url={}, response={}", fullUrl, respStr);
            return respStr;
        } catch (Exception e) {
            log.error("POST request failed: url={}, params={}, error={}", url, params, e.getMessage(), e);
            throw new InternalApiException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 发送GET请求
     *
     * @param url     请求URL
     * @param params  URL参数
     * @param headers 请求头
     * @return 原始响应字符串
     */
    public String doGetAndGetResponseStr(String url, Map<String, String> params, HttpHeaders headers) {
        try {
            String fullUrl = buildUrlWithParams(url, params);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);


            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                requestEntity,
                String.class);

            String respStr = response.getBody();
            log.info("GET response: url={}, response={}", fullUrl, respStr);
            return respStr;
        } catch (Exception e) {
            log.error("GET request failed: url={}, params={}, error={}", url, params, e.getMessage(), e);
            throw new InternalApiException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构建带参数的URL
     */
    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        params.forEach((key, value) -> {
            if (value != null) {
                builder.queryParam(key, value);
            }
        });

        return builder.build().toUriString();
    }

    /**
     * 构建请求实体
     */
    private HttpEntity<Object> buildRequestEntity(Object body, HttpHeaders headers) {
        HttpHeaders finalHeaders = headers != null ? new HttpHeaders(headers) : new HttpHeaders();

        // 设置默认Content-Type为application/json
        if (!finalHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            finalHeaders.setContentType(MediaType.APPLICATION_JSON);
        }

        return new HttpEntity<>(body, finalHeaders);
    }

}
