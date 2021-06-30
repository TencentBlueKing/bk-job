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

package com.tencent.bk.job.common.esb.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cookie工具类
 */
public class CookieUtil {

    /**
     * 读取Cookie指定key的值
     *
     * @param request
     * @param key
     * @return
     */
    public static String getCookieValue(HttpServletRequest request, String key) {
        String value = null;

        // cookie数组
        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(key)) {
                    value = cookie.getValue();
                    break;
                }
            }
        }

        if (null == value) {
            // Cookie属性中没有获取到，那么从Headers里面获取
            String cookieStr = request.getHeader("Cookie");
            if (cookieStr != null) {
                // 去掉所有空白字符，不限于空格
                cookieStr = cookieStr.replaceAll("\\s*", "");
                String[] cookieArr = cookieStr.split(";");

                for (String cookieItem : cookieArr) {
                    String[] cookieItemArr = cookieItem.split("=");
                    if (cookieItemArr[0].equals(key)) {
                        value = cookieItemArr[1];
                        break;
                    }
                }
            }
        }
        return value;
    }

    /**
     * 将指定key，value设置到Cookie，并设置domain为子域名共享Cookie，以及HttpOnly=false
     *
     * @param httpResponse httpResponse
     * @param key          key
     * @param value        value
     */
    public static void addToCookie(HttpServletResponse httpResponse, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setHttpOnly(false);
        cookie.setPath("/");//用于生效即使相同的值
        cookie.setMaxAge(7200);
        httpResponse.addCookie(cookie);
    }
}
