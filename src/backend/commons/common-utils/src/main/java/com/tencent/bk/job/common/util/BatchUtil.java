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
import java.util.List;

/**
 * 分批工具
 */
public class BatchUtil {
    /**
     * 分批
     *
     * @param elements  用于分批的集合
     * @param batchSize 每一批的最大数量
     * @return 分批结果
     */
    public static <E> List<List<E>> buildBatchList(List<E> elements, int batchSize) {
        List<List<E>> batchList = new ArrayList<>();
        int total = elements.size();
        if (total <= batchSize) {
            batchList.add(elements);
            return batchList;
        }

        int batch = total / batchSize;
        int left = total % batchSize;
        if (left > 0) {
            batch += 1;
        }
        int startIndex = 0;
        for (int i = 1; i <= batch; i++) {
            List<E> subList;
            if (i == batch && left > 0) {
                subList = elements.subList(startIndex, startIndex + left);
                startIndex += left;
            } else {
                subList = elements.subList(startIndex, startIndex + batchSize);
                startIndex += batchSize;
            }
            batchList.add(subList);
        }
        return batchList;
    }
}
