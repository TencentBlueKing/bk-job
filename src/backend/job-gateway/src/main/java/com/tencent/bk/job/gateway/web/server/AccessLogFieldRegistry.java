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

import java.util.ArrayList;
import java.util.List;

/**
 * AccessLog输出字段注册器
 */
public class AccessLogFieldRegistry {
    private final List<String> fields = new ArrayList<>();

    public AccessLogFieldRegistry() {
        // AccessLog按此注册顺序输出
        register(AccessLogConstants.LogField.ACCESS_TYPE);
        register(AccessLogConstants.LogField.START_TIME);
        register(AccessLogConstants.LogField.TRACE_ID);
        register(AccessLogConstants.LogField.SPAN_ID);
        register(AccessLogConstants.LogField.USER_NAME);
        register(AccessLogConstants.LogField.METHOD);
        register(AccessLogConstants.LogField.URI);
        register(AccessLogConstants.LogField.PROTOCOL);
        register(AccessLogConstants.LogField.CLIENT_IP);
        register(AccessLogConstants.LogField.USER_AGENT);
        register(AccessLogConstants.LogField.UPSTREAM);
        register(AccessLogConstants.LogField.END_TIME);
        register(AccessLogConstants.LogField.STATUS);
        register(AccessLogConstants.LogField.RESPONSE_SIZE);
        register(AccessLogConstants.LogField.DURATION_MS);
    }

    public void register(String key) {
        fields.add(key);
    }

    public List<String> getFields() {
        return fields;
    }
}
