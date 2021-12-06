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

import com.tencent.bk.job.common.model.PageData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PageUtil {

    public static final int DEFAULT_START = 0;
    public static final int DEFAULT_POSITIVE_LENGTH = 10;

    public static Pair<Integer, Integer> normalizePageParam(Integer start, Integer pageSize) {
        if (start == null) {
            start = 0;
        }
        if (pageSize == null) {
            pageSize = -1;
        }
        Pair<Long, Long> pair = normalizePageParam((long) start, (long) pageSize);
        return Pair.of(pair.getLeft().intValue(), pair.getRight().intValue());
    }

    /**
     * 标准化分页参数start与pageSize
     *
     * @param start
     * @param pageSize
     * @return
     */
    public static Pair<Long, Long> normalizePageParam(Long start, Long pageSize) {
        Long finalStart = start;
        if (start == null || start < 0) {
            finalStart = 0L;
        }
        Long finalPageSize = pageSize;
        if (pageSize == null || pageSize < 0) {
            finalPageSize = -1L;
        }
        return Pair.of(finalStart, finalPageSize);
    }

    /**
     * 内存中分页
     *
     * @param completeList
     * @param start
     * @param pageSize
     * @param <T>
     * @return
     */
    public static <T> PageData<T> pageInMem(List<T> completeList, Integer start, Integer pageSize) {
        if (start == null || start < 0) {
            start = 0;
        }
        if (pageSize == null || pageSize < 0) {
            pageSize = -1;
        }
        PageData<T> pageData = new PageData<>();
        int size = completeList.size();
        pageData.setTotal((long) size);
        pageData.setStart(start);
        pageData.setPageSize(pageSize);
        if (pageSize < 0) {
            pageData.setData(completeList);
        } else {
            if (start >= size) {
                pageData.setData(Collections.emptyList());
            } else {
                int end = start + pageSize;
                if (end > size) {
                    end = size;
                }
                pageData.setData(completeList.subList(start, end));
            }
        }
        return pageData;
    }

    /**
     * 分页查询
     * @param isGetAll 是否全量获取
     * @param prioritizedElements 需要优先的元素
     * @param start 原始分页起始
     * @param pageSize 分页大小
     * @param dbQuery 查询
     * @return 分页结果
     */
    public static <T> PageData<T> pageQuery(boolean isGetAll,
                                            List<T> prioritizedElements,
                                            int start,
                                            int pageSize,
                                            DbQuery<T> dbQuery) {
        boolean isExistPrioritizedElement = CollectionUtils.isNotEmpty(prioritizedElements);
        int actualStart = start;
        if (isExistPrioritizedElement && !isGetAll) {
            if (prioritizedElements.size() <= start) {
                actualStart = start - prioritizedElements.size();
            } else {
                actualStart = 0;
            }
        }
        PageData<T> pageData = dbQuery.query(actualStart);
        rebuildPageData(pageData, prioritizedElements, isGetAll, isExistPrioritizedElement, start, pageSize);
        return pageData;
    }

    @FunctionalInterface
    public interface DbQuery<T> {
        PageData<T> query(int start);
    }


    private static <T> void rebuildPageData(PageData<T> pageData,
                                            List<T> prioritizedElements,
                                            boolean isGetAll,
                                            boolean isExistPrioritizedElement,
                                            Integer start,
                                            Integer length) {
        if (isExistPrioritizedElement) {
            if (isGetAll) {
                pageData.getData().addAll(0, prioritizedElements);
            } else {
                putPrioritizedElementsInFrontIfExist(pageData, prioritizedElements, start, length);
            }
        }

        if (!isGetAll) {
            pageData.setStart(start);
            pageData.setPageSize(length);
            if (isExistPrioritizedElement) {
                pageData.setTotal(prioritizedElements.size() + pageData.getTotal());
            }
        }
    }

    private static <T> void putPrioritizedElementsInFrontIfExist(PageData<T> pageData,
                                                                 List<T> prioritizedElements,
                                                                 Integer start,
                                                                 Integer length) {
        // 处理需要优先的元素
        if (CollectionUtils.isNotEmpty(prioritizedElements)) {
            if (prioritizedElements.size() > start) {
                pageData.getData().addAll(0, prioritizedElements.stream().skip(start).limit(length)
                    .collect(Collectors.toList()));
            }
        }

        // subList
        if (pageData.getData().size() > length) {
            List<T> templates = new ArrayList<>(pageData.getData().subList(0, length));
            pageData.setData(templates);
        }
    }
}
