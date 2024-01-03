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

package com.tencent.bk.job.common.esb.model;

import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.util.http.RetryModeEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Open API 请求封装
 *
 * @param <T>
 */
@Data
public class OpenApiRequestInfo<T> {
    private final HttpMethodEnum method;
    private final String uri;
    /**
     * http url 格式 Query 参数，比如 ?name=admin&type=1
     */
    private final String queryParams;
    /**
     * Query 参数 Map, 优先于 queryParams 参数
     */
    private Map<String, String> queryParamsMap;
    private final T body;
    private final BkApiAuthorization authorization;
    /**
     * 请求重试模式
     */
    private final RetryModeEnum retryMode;
    /**
     * 请求是否幂等
     */
    private final Boolean idempotent;

    public OpenApiRequestInfo(Builder<T> builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.queryParams = builder.queryParams;
        this.queryParamsMap = builder.queryParamsMap;
        this.body = builder.body;
        this.authorization = builder.authorization;
        this.retryMode = builder.retryMode;
        this.idempotent = builder.idempotent;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private HttpMethodEnum method;
        private String uri;
        private String queryParams;
        private Map<String, String> queryParamsMap;
        private T body;
        private BkApiAuthorization authorization;
        private RetryModeEnum retryMode = RetryModeEnum.SAFE_GUARANTEED;
        private Boolean idempotent;

        public Builder<T> method(HttpMethodEnum method) {
            this.method = method;
            return this;
        }

        public Builder<T> uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder<T> queryParams(String queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder<T> body(T body) {
            this.body = body;
            return this;
        }

        public Builder<T> authorization(BkApiAuthorization authorization) {
            this.authorization = authorization;
            return this;
        }

        public Builder<T> queryParamsMap(Map<String, String> queryParamsMap) {
            this.queryParamsMap = queryParamsMap;
            return this;
        }

        public Builder<T> addQueryParam(String name, String value) {
            initQueryParamsMapIfNull();
            queryParamsMap.put(name, value);
            return this;
        }

        private void initQueryParamsMapIfNull() {
            if (queryParamsMap == null) {
                queryParamsMap = new HashMap<>();
            }
        }

        public Builder<T> addQueryParams(Map<String, String> paramsMap) {
            initQueryParamsMapIfNull();
            queryParamsMap.putAll(paramsMap);
            return this;
        }

        public Builder<T> setRetryMode(RetryModeEnum retryMode) {
            this.retryMode = retryMode;
            return this;
        }

        public Builder<T> setIdempotent(Boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        public OpenApiRequestInfo<T> build() {
            return new OpenApiRequestInfo<>(this);
        }
    }

    /**
     * 构造最终请求的 uri
     */
    public String buildFinalUri() {
        String result = uri;
        String queryParamUrl = buildQueryParamUrl();
        if (StringUtils.isNotBlank(queryParamUrl)) {
            if (queryParamUrl.startsWith("?")) {
                result += queryParamUrl;
            } else {
                result += "?" + queryParamUrl;
            }
        }
        return result;
    }

    private String buildQueryParamUrl() {
        if (queryParamsMap != null && !queryParamsMap.isEmpty()) {
            StringBuilder urlString = new StringBuilder(512);
            queryParamsMap.forEach((name, value) ->
                urlString.append('&').append(name).append('=').append(urlEncode(value)));
            return urlString.toString().substring(1);
        } else if (StringUtils.isNotBlank(this.queryParams)) {
            return this.queryParams;
        } else {
            return null;
        }
    }

    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encode failed");
        }
    }
}
