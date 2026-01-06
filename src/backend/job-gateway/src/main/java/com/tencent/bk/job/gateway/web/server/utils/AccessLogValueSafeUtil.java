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

package com.tencent.bk.job.gateway.web.server.utils;

import com.tencent.bk.job.gateway.web.server.AccessLogConstants;
import reactor.netty.http.server.ConnectionInformation;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Access Log安全工具类，消除null警告
 */
public class AccessLogValueSafeUtil {

    public static String clientIP(@Nullable ConnectionInformation connectionInformation) {
        if (connectionInformation == null) {
            return AccessLogConstants.Default.MISSING;
        }
        SocketAddress socketAddress = connectionInformation.connectionRemoteAddress();
        if (socketAddress == null) {
            return AccessLogConstants.Default.MISSING;
        }
        return AccessLogNetUtil.formatSocketAddress(socketAddress);
    }

    public static String dateTime(@Nullable ZonedDateTime dateTime, String formatter) {
        if (dateTime == null) {
            return AccessLogConstants.Default.MISSING;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(formatter));
    }

    public static String uri(@Nullable CharSequence charSequence) {
        if (charSequence == null) {
            return AccessLogConstants.Default.MISSING;
        }
        return charSequence.toString();
    }
}
