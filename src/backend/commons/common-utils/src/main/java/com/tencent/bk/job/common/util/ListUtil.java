package com.tencent.bk.job.common.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ListUtil {

    /**
     * 判断list中的元素是否全部为true
     *
     * @param list 元素列表
     * @return 最终结果
     */
    public static boolean isAllTrue(List<Boolean> list) {
        for (Boolean ele : list) {
            if (ele == null || !ele) return false;
        }
        return true;
    }

    /**
     * 将列表中的元素按规则分隔为两个列表
     *
     * @param list             原始列表
     * @param separateFunction 分隔依据函数
     * @param <T>              列表元素类型
     * @return 列表Pair, 左元素为函数判断为true的元素列表，右元素为函数判断为false的元素列表
     */
    public static <T> Pair<List<T>, List<T>> separate(List<T> list, Function<? super T, Boolean> separateFunction) {
        List<T> trueList = new ArrayList<>();
        List<T> falseList = new ArrayList<>();
        for (T t : list) {
            Boolean result = separateFunction.apply(t);
            if (result != null && result) {
                trueList.add(t);
            } else {
                falseList.add(t);
            }
        }
        return Pair.of(trueList, falseList);
    }

    /**
     * 合并集合（不去重), 并返回List
     *
     * @param collection1 集合列表1
     * @param collection2 集合列表2
     * @param <E>         集合中的元素
     * @return 合并后的List
     */
    public static <E> List<E> union(Collection<? extends E> collection1, Collection<? extends E> collection2) {
        List<E> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(collection1)) {
            result.addAll(collection1);
        }
        if (CollectionUtils.isNotEmpty(collection2)) {
            result.addAll(collection2);
        }
        return result;
    }

}
