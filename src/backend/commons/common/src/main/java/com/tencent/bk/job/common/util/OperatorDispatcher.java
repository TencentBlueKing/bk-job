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

import com.tencent.bk.job.common.constant.KubeContainerOperator;
import com.tencent.bk.job.common.constant.QueryableContainerField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 动态条件过滤器：按 {@link QueryableContainerField} 字段维度派发对外暴露的运算符集合。
 * <p>
 * KubeContainerOperator 全集取值：equal、not_equal、in、not_in、less、less_or_equal、greater、greater_or_equal、
 * begins_with、not_begins_with、contains、not_contains、ends_with、not_ends_with、datetime_less、
 * datetime_less_or_equal、datetime_greater、datetime_greater_or_equal、is_null、is_not_null、exist、not_exist、
 * is_empty、is_not_empty、size、filter_object。
 * <p>
 * 「对外暴露集合」是 QueryableContainerField / KubeContainerOperator 全集的子集，按产品交互需要按字段裁剪：
 * <ul>
 *   <li>{@link QueryableContainerField#CONTAINER_NAME}：equal / contains</li>
 *   <li>{@link QueryableContainerField#CONTAINER_CONTAINER_UID}：equal</li>
 *   <li>{@link QueryableContainerField#POD_NAME}：equal / contains</li>
 *   <li>{@link QueryableContainerField#POD_LABELS}：equal（value 形态为 K8s label selector 表达式字符串）</li>
 * </ul>
 * 未暴露的字段或运算符即使底层 bk-cmdb 支持，也不允许用户提交。
 */
public final class OperatorDispatcher {

    /**
     * 字段维度的对外暴露运算符表。Map.keySet() 即对外暴露的字段集合。
     */
    private static final Map<QueryableContainerField, Set<KubeContainerOperator>> FIELD_ALLOWED_OPERATORS;

    static {
        EnumMap<QueryableContainerField, Set<KubeContainerOperator>> map =
            new EnumMap<>(QueryableContainerField.class);

        // container_name：等值 / 包含
        map.put(QueryableContainerField.CONTAINER_NAME, immutableOperatorSet(
            KubeContainerOperator.EQUAL,
            KubeContainerOperator.CONTAINS));

        // container_container_uid：仅等值（UID 是精确标识，contains 无业务意义）
        map.put(QueryableContainerField.CONTAINER_CONTAINER_UID, immutableOperatorSet(
            KubeContainerOperator.EQUAL));

        // pod_name：等值 / 包含
        map.put(QueryableContainerField.POD_NAME, immutableOperatorSet(
            KubeContainerOperator.EQUAL,
            KubeContainerOperator.CONTAINS));

        // pod_labels：等值（value 形态为 K8s label selector 表达式字符串，如 "k1=v1,k2=v2"）
        map.put(QueryableContainerField.POD_LABELS, immutableOperatorSet(
            KubeContainerOperator.EQUAL));

        FIELD_ALLOWED_OPERATORS = Collections.unmodifiableMap(map);
    }

    private static Set<KubeContainerOperator> immutableOperatorSet(KubeContainerOperator... ops) {
        EnumSet<KubeContainerOperator> set = EnumSet.noneOf(KubeContainerOperator.class);
        Collections.addAll(set, ops);
        return Collections.unmodifiableSet(set);
    }

    private OperatorDispatcher() {
    }

    /**
     * 当前对外暴露的字段集合（{@link QueryableContainerField} 子集）。
     */
    public static Set<QueryableContainerField> getExposedFields() {
        return FIELD_ALLOWED_OPERATORS.keySet();
    }

    /**
     * 指定字段是否对外暴露。
     */
    public static boolean isFieldExposed(QueryableContainerField field) {
        return field != null && FIELD_ALLOWED_OPERATORS.containsKey(field);
    }

    /**
     * 指定字段对外暴露的运算符集合；字段未暴露返回空集。
     */
    public static Set<KubeContainerOperator> getAllowedOperators(QueryableContainerField field) {
        if (field == null) {
            return Collections.emptySet();
        }
        return FIELD_ALLOWED_OPERATORS.getOrDefault(field, Collections.emptySet());
    }

    /**
     * 指定字段-运算符组合是否对外允许。
     */
    public static boolean isOperatorAllowed(QueryableContainerField field, KubeContainerOperator op) {
        if (op == null) {
            return false;
        }
        return getAllowedOperators(field).contains(op);
    }
}
