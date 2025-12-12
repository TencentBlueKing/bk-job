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

package com.tencent.bk.job.gateway.web.server;

/**
 * Access log相关常量
 */
public class AccessLogConstants {

    /**
     * accessLog属性，会在日志中输出
     */
    public static final class LogField {
        public static final String STATUS = "status";
        public static final String USER_NAME = "userName";
        public static final String METHOD = "method";
        public static final String PATH = "path";
        public static final String START_TIME = "startTime";
        public static final String END_TIME = "endTime";
        public static final String DURATION = "duration";
        public static final String PROTOCOL = "protocol";
        public static final String CLIENT_IP = "clientIp";
        public static final String USER_AGENT = "userAgent";
        public static final String UPSTREAM = "upstream";
        public static final String RESPONSE_SIZE = "responseSize";
        public static final String TRACE_ID = "traceId";
        public static final String SPAN_ID = "spanId";
    }

    /**
     * 请求头，用于上下文传递信息
     */
    public static final class Header {
        public static final String UPSTREAM_SERVER = "X-Bk-Job-Upstream-Server";
        public static final String SPAN_ID = "X-Bk-Job-Span-Id";
    }

    /**
     * 格式化常量
     */
    public static final class Format {
        public static final String DEFAULT_TIME = "yyyy-MM-dd HH:mm:ss.SSS";
        public static final String DEFAULT_LOG = "{} - {} [{}] \"{} {} {}\" {} {} {}ms";
    }

    public static final class Default {
        public static final String MISSING = "-";
        public static final String KEY_META_NAMESPACE = "meta.helm.sh/release-namespace";
    }
}
