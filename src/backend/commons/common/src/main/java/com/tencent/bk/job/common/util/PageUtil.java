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

import com.tencent.bk.job.common.model.PageCondition;
import com.tencent.bk.job.common.model.PageData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具
 */
public class PageUtil {

    /**
     * 标准化分页参数start与pageSize
     *
     * @param start    分页起始位置
     * @param pageSize 每页大小
     * @return 标准化分页参数
     */
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
     * @param start    分页起始位置
     * @param pageSize 每页大小
     * @return 标准化分页参数
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
     * @param completeList 需要分页的实例列表
     * @param start        分页起始位置
     * @param pageSize     每页大小
     * @param <T>          分页的实例
     * @return 分页结果
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
     *
     * @param isGetAll            是否全量获取
     * @param prioritizedElements 需要优先的实例
     * @param start               原始分页起始
     * @param pageSize            分页大小
     * @param dbQuery             查询
     * @return 分页结果
     */
    public static <T> PageData<T> pageQuery(boolean isGetAll,
                                            List<T> prioritizedElements,
                                            int start,
                                            int pageSize,
                                            DbQuery<T> dbQuery) {
        boolean isExistPrioritizedElement = CollectionUtils.isNotEmpty(prioritizedElements);
        int actualStart = start;
        if (isGetAll) {
            actualStart = -1;
        } else if (isExistPrioritizedElement) {
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

    /**
     * 对分页数据内含的数据进行类型转换后得到新的分页数据
     *
     * @param srcPageData 源分页数据
     * @param mapper      映射函数
     * @param <T>         原始元素类型
     * @param <R>         目标元素类型
     * @return 转换后的分页数据
     */
    public static <T, R> PageData<R> transferPageData(PageData<T> srcPageData,
                                                      Function<? super T, ? extends R> mapper) {
        if (srcPageData == null) {
            return null;
        }
        List<T> data = srcPageData.getData();
        List<R> newDataList = null;
        if (data != null) {
            newDataList = data.stream().map(mapper).collect(Collectors.toList());
        }
        return copyPageWithNewData(srcPageData, newDataList);
    }

    /**
     * 拷贝分页数据，并使用新的列表数据填充
     *
     * @param srcPageData 源分页数据
     * @return 新的分页数据
     */
    public static <T, R> PageData<R> copyPageWithNewData(PageData<T> srcPageData, List<R> newDataList) {
        if (srcPageData == null) {
            return null;
        }
        PageData<R> targetPageData = new PageData<>();
        targetPageData.setStart(srcPageData.getStart());
        targetPageData.setPageSize(srcPageData.getPageSize());
        targetPageData.setTotal(srcPageData.getTotal());
        targetPageData.setCanCreate(srcPageData.getCanCreate());
        targetPageData.setExistAny(srcPageData.getExistAny());
        targetPageData.setData(newDataList);
        return targetPageData;
    }

    /**
     * 查询全量 - 通过循环分页查询
     * <p>
     * 注意：如果多次分页查询期间原始数据发生变化，使用该方法可能出现数据重复/遗漏的情况。该方法仅适用于对数据准确度要求不高的场景！！！
     * 如果需要准确的查询结果，可以使用 queryAllWithLoopPageQueryInOrder 代替
     *
     * @param pageLimit               每页限制大小
     * @param pageQuery               查询操作
     * @param extractElementsFunction 从分页查询结果提取返回的对象列表
     * @param elementConverter        分页查询对象转换为最终对象
     * @param <T1>                    分页查询返回的对象
     * @param <T2>                    转换之后作为方法返回值的对象
     * @param <R>                     分页查询结果
     * @return 全量对象列表
     * @see #queryAllWithLoopPageQueryInOrder
     */
    public static <T1, T2, R> List<T2> queryAllWithLoopPageQuery(int pageLimit,
                                                                 Function<PageCondition, R> pageQuery,
                                                                 Function<R, Collection<T1>> extractElementsFunction,
                                                                 Function<T1, T2> elementConverter) {
        int start = 0;
        List<T2> elements = new ArrayList<>();
        while (true) {
            PageCondition pageCondition = PageCondition.build(start, pageLimit);
            R result = pageQuery.apply(pageCondition);

            Collection<T1> originElements = extractElementsFunction.apply(result);
            if (CollectionUtils.isEmpty(originElements)) {
                // 如果本次没有获取到数据记录，说明数据已经全部拉取完成
                break;
            }
            elements.addAll(originElements.stream()
                .map(elementConverter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

            if (originElements.size() < pageLimit) {
                // 如果实际查询数据记录的数量小于分页大小，说明数据已经全部拉取完成
                break;
            }
            start += pageLimit;
        }
        return elements;
    }

    /**
     * 查询全量 - 按指定顺序循环分页查询
     *
     * @param pageLimit               每页限制大小
     * @param queryInputGenerator     分页查询参数生成。输入: 当前页的最后一个元素；输出：下一页查询输入
     * @param query                   查询操作
     * @param extractElementsFunction 从分页查询结果提取返回的对象列表
     * @param elementConverter        分页查询对象转换为最终对象
     * @param <T1>                    分页查询返回的对象类型
     * @param <T2>                    转换之后作为方法返回值的对象类型
     * @param <R>                     分页查询结果
     * @param <Q>                     分页查询输入参数
     * @return 全量对象列表
     */
    public static <T1, T2, R, Q> List<T2> queryAllWithLoopPageQueryInOrder(
        int pageLimit,
        Function<T1, Q> queryInputGenerator,
        Function<Q, R> query,
        Function<R, List<T1>> extractElementsFunction,
        Function<T1, T2> elementConverter) {

        // 查询结果
        List<T2> elements = new ArrayList<>();

        T1 latestElement = null;  // 排序之后的最后一个元素，用于作为下一次分页查询的 offset 条件
        while (true) {
            Q queryInput = queryInputGenerator.apply(latestElement);
            R result = query.apply(queryInput);

            List<T1> originElements = extractElementsFunction.apply(result);
            if (CollectionUtils.isEmpty(originElements)) {
                // 如果本次没有获取到数据记录，说明数据已经全部拉取完成
                break;
            }
            elements.addAll(originElements.stream()
                .map(elementConverter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
            latestElement = originElements.get(originElements.size() - 1);

            if (originElements.size() < pageLimit) {
                // 如果实际查询数据记录的数量小于分页大小，说明数据已经全部拉取完成
                break;
            }
        }
        return elements;
    }
}
