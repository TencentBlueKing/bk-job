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

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 工具类，集合了多个工具方法。
 */
public class Utils {
    /**
     * UUID生成器
     */
    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 分隔字符串,过滤空串,去重
     */
    public static List<String> getNotBlankSplitList(String str, String splitter) {
        LinkedHashSet<String> itemSet = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(str)) {
            String[] itemList = str.split(splitter);
            for (String item : itemList) {
                if (StringUtils.isNotBlank(item)) {
                    itemSet.add(StringUtils.trim(item));
                }
            }
        }
        return new ArrayList<>(itemSet);
    }

    public static String htmlEncode(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        str = str.replace("&", "&amp;");
        str = str.replace(">", "&gt;");
        str = str.replace("<", "&lt;");
        return str;
    }

    public static Date tryParseDate(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static int tryParseInt(Object o) {
        return Integer.parseInt(o.toString());
    }

    public static String concatStringWithSeperator(Collection<String> strs, String seperator) {
        if (strs == null || strs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String str : strs) {
            sb.append(str).append(seperator);
        }
        String result = sb.toString();
        return result.substring(0, result.length() - 1);
    }

}
