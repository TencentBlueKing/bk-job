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

public class CompareUtil {

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

    public static int compareVersion(String version1, String version2) {
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
            int result = compareIntegerStr(arr1[i], arr2[i]);
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
