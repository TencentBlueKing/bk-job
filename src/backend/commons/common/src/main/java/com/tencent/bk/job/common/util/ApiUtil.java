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

import java.util.Map;

public class ApiUtil {

    /**
     * 根据映射关系表或URI本身获取API名称，主要用于解决Prometheus标签不支持斜杠的问题
     *
     * @param interfaceNameMap key:uri,value:API名称
     * @param uri              路径
     * @return API名称
     */
    public static String getApiNameByUri(Map<String, String> interfaceNameMap, String uri) {
        if (interfaceNameMap != null && interfaceNameMap.containsKey(uri)) {
            return interfaceNameMap.get(uri);
        }
        String uriSeparator = "/";
        uri = StringUtil.removePrefix(uri, uriSeparator);
        uri = StringUtil.removeSuffix(uri, uriSeparator);
        if (uri.contains(uriSeparator)) {
            String[] uriArr = uri.split(uriSeparator);
            int len = uriArr.length;
            String joinSeparator = "_";
            if (len >= 2) {
                return uriArr[len - 2] + joinSeparator + uriArr[len - 1];
            }
        }
        return uri;
    }
}
