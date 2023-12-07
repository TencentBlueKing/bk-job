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
import lombok.Data;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * Http 请求
 */
@Data
public class HttpRequest {
    private HttpMethodEnum method;
    private String url;
    /**
     * 文本格式的 entity
     */
    private String stringEntity;
    /**
     * 标准 httpEntity，优先级最高
     */
    private HttpEntity httpEntity;
    private Header[] headers;
    /**
     * 请求重试模式
     */
    private RetryModeEnum retryMode;
    /**
     * 请求是否幂等
     */
    private final Boolean idempotent;

    private boolean keepAlive;

    public HttpRequest(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.stringEntity = builder.stringEntity;
        this.httpEntity = builder.httpEntity;
        this.headers = builder.headers;
        this.retryMode = builder.retryMode;
        this.idempotent = builder.idempotent;
        this.keepAlive = builder.keepAlive;
    }

    public static Builder builder(HttpMethodEnum method, String url) {
        return new Builder(method, url);
    }

    public static class Builder {
        private final HttpMethodEnum method;
        private final String url;
        private String stringEntity;
        private HttpEntity httpEntity;
        private Header[] headers;
        private RetryModeEnum retryMode = RetryModeEnum.SAFE_GUARANTEED;
        private Boolean idempotent;
        /**
         * 按照 http1.1 之后的标准，默认都使用长连接
         */
        private boolean keepAlive = true;

        public Builder(HttpMethodEnum method, String url) {
            this.method = method;
            this.url = url;
        }

        public Builder setStringEntity(String stringEntity) {
            this.stringEntity = stringEntity;
            return this;
        }

        public Builder setHttpEntity(HttpEntity httpEntity) {
            this.httpEntity = httpEntity;
            return this;
        }

        public Builder setHeaders(Header[] headers) {
            this.headers = headers;
            return this;
        }

        public Builder setRetryMode(RetryModeEnum retryMode) {
            this.retryMode = retryMode;
            return this;
        }

        public Builder setIdempotent(Boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }


}
