/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 常用的集合工具
 */
public class CollectionUtil {
    /**
     * 对 List 进行分片，每一分片的大小相同（最后一个分片可能小于)
     *
     * @param list 需要分片的集合
     * @param size 分片大小
     * @return 集合分片
     */
    public static <E> List<List<E>> partitionList(List<E> list, int size) {
        List<List<E>> batchList = new ArrayList<>();
        int total = list.size();
        if (total <= size) {
            batchList.add(list);
            return batchList;
        }

        int batch = total / size;
        int left = total % size;
        if (left > 0) {
            batch += 1;
        }
        int startIndex = 0;
        for (int i = 1; i <= batch; i++) {
            List<E> subList;
            if (i == batch && left > 0) {
                subList = list.subList(startIndex, startIndex + left);
                startIndex += left;
            } else {
                subList = list.subList(startIndex, startIndex + size);
                startIndex += size;
            }
            batchList.add(subList);
        }
        return batchList;
    }

    /**
     * 对集合进行分片，每一分片的大小相同（最后一个分片可能小于)
     *
     * @param collection 需要分片的集合
     * @param size       分片大小
     * @return 集合分片
     */
    public static <E> List<List<E>> partitionCollection(Collection<E> collection, int size) {
        int limit = (collection.size() + size - 1) / size;
        return Stream.iterate(0, n -> n + 1)
            .limit(limit)
            .map(a -> collection.stream()
                .skip(a * size)
                .limit(size)
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    /**
     * 从集合元素中抽取两个字段构成Map
     *
     * @param entityCollection 实体集合
     * @param keyFunc          抽取Key的方法
     * @param valueFunc        抽取Value的方法
     * @param <E>              实体类型
     * @param <K>              Key类型
     * @param <V>              Value类型
     * @return HashMap<K, V>，null可以为key或value，entityCollection为null时返回空Map
     */
    public static <E, K, V> Map<K, V> convertToMap(List<E> entityCollection,
                                                   Function<E, K> keyFunc,
                                                   Function<E, V> valueFunc) {
        if (CollectionUtils.isEmpty(entityCollection)) {
            return Collections.emptyMap();
        }
        Map<K, V> map = new HashMap<>();
        for (E entity : entityCollection) {
            map.put(keyFunc.apply(entity), valueFunc.apply(entity));
        }
        return map;
    }
}
