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
import reactor.util.annotation.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 网络地址相关格式化工具类
 */
public class AccessLogNetUtils {

    /**
     * 格式化SocketAddress，返回host:port格式的字符串
     * 如果地址不存在或无法解析，返回统一占位符-
     */
    public static String formatSocketAddress(@Nullable SocketAddress socketAddress) {
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            return inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
        }
        else {
            return AccessLogConstants.VAL_MISSING;
        }
    }
}
