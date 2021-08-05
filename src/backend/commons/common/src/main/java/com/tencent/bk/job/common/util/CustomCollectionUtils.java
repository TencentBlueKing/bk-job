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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomCollectionUtils {

    public static <T> List<T> mergeList(List<T> list1, List<T> list2) {
        if (list1 == null || list1.isEmpty()) return list2;
        if (list2 == null || list2.isEmpty()) return list1;
        Set<T> set = new HashSet<>(list1);
        List<T> resultList = new ArrayList<>(list1);
        list2.forEach(it -> {
            if (!set.contains(it)) {
                resultList.add(it);
                set.add(it);
            }
        });
        return resultList;
    }

    public static List<String> getNoDuplicateList(String rawStr, String separator) {
        if (rawStr == null || rawStr.isEmpty()) {
            return new ArrayList<>();
        }
        String[] arr = rawStr.split(separator);
        Set<String> set = new HashSet<String>(Arrays.asList(arr));
        return new ArrayList<>(set);
    }

    public static <T> boolean isEmptyCollection(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }
}
