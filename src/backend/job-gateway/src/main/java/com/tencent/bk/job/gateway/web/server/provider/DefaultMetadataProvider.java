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

package com.tencent.bk.job.gateway.web.server.provider;

import com.tencent.bk.job.common.constant.HttpHeader;
import com.tencent.bk.job.gateway.web.server.AccessLogConstants;
import com.tencent.bk.job.gateway.web.server.utils.AccessLogValueSafeUtil;
import reactor.netty.http.server.logging.AccessLogArgProvider;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从netty默认的AccessLogArgProvider获取日志数据
 */
public class DefaultMetadataProvider implements AccessLogMetadataProvider {

    @Override
    public Map<String, Object> extract(AccessLogArgProvider provider) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(AccessLogConstants.LogField.METHOD, provider.method());
        map.put(AccessLogConstants.LogField.STATUS, provider.status());
        map.put(AccessLogConstants.LogField.DURATION, provider.duration() + "ms");
        map.put(AccessLogConstants.LogField.PROTOCOL, provider.protocol());
        map.put(AccessLogConstants.LogField.CLIENT_IP,
            AccessLogValueSafeUtil.clientIP(provider.connectionInformation()));
        map.put(AccessLogConstants.LogField.USER_AGENT, provider.requestHeader(HttpHeader.HDR_UER_AGENT));
        map.put(AccessLogConstants.LogField.RESPONSE_SIZE, provider.contentLength());
        map.put(AccessLogConstants.LogField.START_TIME,
            AccessLogValueSafeUtil.dateTime(provider.accessDateTime(), AccessLogConstants.Format.DEFAULT_TIME));
        map.put(AccessLogConstants.LogField.END_TIME,
            AccessLogValueSafeUtil.dateTime(ZonedDateTime.now(), AccessLogConstants.Format.DEFAULT_TIME));
        String uri = AccessLogValueSafeUtil.uri(provider.uri());
        map.put(AccessLogConstants.LogField.PATH,
            uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri);
        return map;
    }
}
