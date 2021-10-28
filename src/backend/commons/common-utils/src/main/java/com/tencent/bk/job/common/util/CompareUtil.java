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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompareUtil {

    /**
     * 比较由数字组成的字符串所代表数字的实际大小
     *
     * @param str1
     * @param str2
     * @return 1：str1所含数>str2所含数字
     * 0：str1所含数==str2所含数字
     * -1：str1所含数<str2所含数字
     */
    public static int compareIntegerStr(String str1, String str2) {
        if (StringUtils.isBlank(str1)) {
            if (StringUtils.isBlank(str2))
                return 0;
            else
                return -1;
        } else if (StringUtils.isBlank(str2)) {
            return 1;
        }
        int value1 = Integer.parseInt(str1);
        int value2 = Integer.parseInt(str2);
        return Integer.compare(value1, value2);
    }

    /**
     * 判断字符串是否由纯数字组成
     *
     * @param str
     * @return
     */
    public static boolean isNumberStr(String str) {
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(str);
        return m.matches();
    }

    private static int compareElement(String ele1, String ele2) {
        if (isNumberStr(ele1)) {
            if (isNumberStr(ele2)) {
                return compareIntegerStr(ele1, ele2);
            } else {
                // 数字比alpha,beta,rc等版本大
                return 1;
            }
        }
        // ele1为非数字
        if (isNumberStr(ele2)) {
            return -1;
        }
        int result = ele1.compareTo(ele2);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * 比较版本号（含语义化版本号）用于确定先后顺序（如作业平台版本号，3.3.3.0，3.3.3-alpha.1等）
     *
     * @param version1 版本号1
     * @param version2 版本号2
     * @return 1 ： version1在version2之后
     * 0 ： version1与version2位置相同
     * -1 ： version1在version2之前
     */
    public static int compareVersion(String version1, String version2) {
        // 标准化为点分形式
        version1 = version1.replace("-", ".").toLowerCase();
        version2 = version2.replace("-", ".").toLowerCase();
        String[] arr1 = version1.split("\\.");
        String[] arr2 = version2.split("\\.");
        int len1 = arr1.length;
        int len2 = arr2.length;
        if (len1 == 0) {
            if (len2 == 0)
                return 0;
            else
                return -1;
        } else if (len2 == 0) {
            return 1;
        }
        int shortLen = Math.min(len1, len2);
        for (int i = 0; i < shortLen; i++) {
            int result = compareElement(arr1[i].trim(), arr2[i].trim());
            if (result != 0) return result;
        }
        if (len1 < len2) return -1;
        else if (len1 > len2) return 1;
        return 0;
    }

    public static int safeCompareNullFront(Comparable p1, Comparable p2) {
        if (p1 == null && p2 == null) {
            return 0;
        }
        if (p1 == null) {
            return -1;
        }
        if (p2 == null) {
            return 1;
        }
        return p1.compareTo(p2);
    }

    public static int safeCompareNullBack(Comparable p1, Comparable p2) {
        if (p1 == null && p2 == null) {
            return 0;
        }
        if (p1 == null) {
            return 1;
        }
        if (p2 == null) {
            return -1;
        }
        return p1.compareTo(p2);
    }
}
