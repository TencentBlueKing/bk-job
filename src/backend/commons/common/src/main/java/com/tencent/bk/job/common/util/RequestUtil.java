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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RequestUtil {
    public static String getHeaderValue(ServerHttpRequest request, String header) {
        HttpHeaders httpHeaders = request.getHeaders();
        if (httpHeaders == null) {
            return null;
        }
        List<String> values = httpHeaders.get(header);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    public static String getCookieValue(ServerHttpRequest request, String cookieName) {
        MultiValueMap<String, HttpCookie> multiValueCookieMap = request.getCookies();
        if (multiValueCookieMap == null) {
            return null;
        }
        Map<String, HttpCookie> cookieMap = multiValueCookieMap.toSingleValueMap();
        if (cookieMap == null) {
            return null;
        }
        HttpCookie cookie = cookieMap.get(cookieName);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    /**
     * 从Header中解析同一个Key对应的多个Cookie值
     *
     * @param request    请求对象
     * @param cookieName 目标Cookie的Key
     * @return 多个Cookie值列表
     */
    public static List<String> getCookieValuesFromHeader(ServerHttpRequest request, String cookieName) {
        HttpHeaders headers = request.getHeaders();
        List<String> cookieList = headers.get("cookie");
        return getCookieValuesFromCookies(cookieList, cookieName);
    }

    /**
     * 从Cookie中解析同一个Key对应的多个Cookie值
     *
     * @param cookieList cookie列表
     * @param cookieName 目标Cookie的Key
     * @return 多个Cookie值列表
     */
    public static List<String> getCookieValuesFromCookies(List<String> cookieList, String cookieName) {
        if (CollectionUtils.isEmpty(cookieList)) {
            return Collections.emptyList();
        }
        String cookieStr = cookieList.get(0);
        log.debug("cookie from headers: {}", cookieStr);
        String[] cookies = cookieStr.split(";");
        List<String> cookieValueList = new ArrayList<>();
        for (String cookie : cookies) {
            cookie = cookie.trim();
            String targetPrefix = cookieName + "=";
            if (cookie.startsWith(targetPrefix)) {
                cookieValueList.add(cookie.replaceFirst(targetPrefix, ""));
            }
        }
        return cookieValueList;
    }
}
