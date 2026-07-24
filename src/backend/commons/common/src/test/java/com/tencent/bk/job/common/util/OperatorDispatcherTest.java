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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 {@link OperatorDispatcher} 字段维度暴露集合 / 运算符派发结果与产品方案一致：
 * <ul>
 *   <li>对外暴露 4 个字段：container_name / container_container_uid / pod_name / pod_labels</li>
 *   <li>每个字段独立配置允许的运算符（不再按字段类型派发）</li>
 * </ul>
 */
@DisplayName("OperatorDispatcher: 按字段维度的暴露集合 / 运算符派发")
class OperatorDispatcherTest {

    @Nested
    @DisplayName("EXPOSED_FIELDS")
    class ExposedFields {

        @Test
        @DisplayName("当前对外暴露 4 个字段")
        void shouldExposeFourFields() {
            Set<QueryableContainerField> exposed = OperatorDispatcher.getExposedFields();
            assertThat(exposed).containsExactlyInAnyOrder(
                QueryableContainerField.CONTAINER_NAME,
                QueryableContainerField.CONTAINER_CONTAINER_UID,
                QueryableContainerField.POD_NAME,
                QueryableContainerField.POD_LABELS
            );
        }

        @Test
        @DisplayName("isFieldExposed: null 返回 false")
        void shouldReturnFalseForNullField() {
            assertThat(OperatorDispatcher.isFieldExposed(null)).isFalse();
        }

        @Test
        @DisplayName("isFieldExposed: 已暴露字段返回 true")
        void shouldReturnTrueForExposedField() {
            assertThat(OperatorDispatcher.isFieldExposed(QueryableContainerField.CONTAINER_NAME)).isTrue();
        }

        @Test
        @DisplayName("isFieldExposed: 未暴露字段（CONTAINER_ID/POD_ID）返回 false")
        void shouldReturnFalseForUnexposedField() {
            assertThat(OperatorDispatcher.isFieldExposed(QueryableContainerField.CONTAINER_ID)).isFalse();
            assertThat(OperatorDispatcher.isFieldExposed(QueryableContainerField.POD_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("getAllowedOperators(field): 按字段独立暴露")
    class AllowedOperatorsByField {

        @Test
        @DisplayName("CONTAINER_NAME：equal / contains")
        void containerName() {
            assertThat(OperatorDispatcher.getAllowedOperators(QueryableContainerField.CONTAINER_NAME))
                .containsExactlyInAnyOrder(KubeContainerOperator.EQUAL, KubeContainerOperator.CONTAINS);
        }

        @Test
        @DisplayName("CONTAINER_CONTAINER_UID：仅 equal")
        void containerUid() {
            assertThat(OperatorDispatcher.getAllowedOperators(QueryableContainerField.CONTAINER_CONTAINER_UID))
                .containsExactlyInAnyOrder(KubeContainerOperator.EQUAL);
        }

        @Test
        @DisplayName("POD_NAME：equal / contains")
        void podName() {
            assertThat(OperatorDispatcher.getAllowedOperators(QueryableContainerField.POD_NAME))
                .containsExactlyInAnyOrder(KubeContainerOperator.EQUAL, KubeContainerOperator.CONTAINS);
        }

        @Test
        @DisplayName("POD_LABELS：仅 equal（value 形态为标签选择器表达式）")
        void podLabels() {
            assertThat(OperatorDispatcher.getAllowedOperators(QueryableContainerField.POD_LABELS))
                .containsExactlyInAnyOrder(KubeContainerOperator.EQUAL);
        }

        @Test
        @DisplayName("未暴露字段（CONTAINER_ID）：返回空集")
        void unexposedFieldReturnsEmpty() {
            assertThat(OperatorDispatcher.getAllowedOperators(QueryableContainerField.CONTAINER_ID)).isEmpty();
            assertThat(OperatorDispatcher.getAllowedOperators(QueryableContainerField.POD_ID)).isEmpty();
        }

        @Test
        @DisplayName("null 字段：返回空集，不抛 NPE")
        void nullFieldReturnsEmpty() {
            assertThat(OperatorDispatcher.getAllowedOperators(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isOperatorAllowed: 字段-运算符派发组合")
    class IsOperatorAllowed {

        @Test
        @DisplayName("CONTAINER_NAME + CONTAINS 允许")
        void containerNameContainsAllowed() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_NAME, KubeContainerOperator.CONTAINS)).isTrue();
        }

        @Test
        @DisplayName("CONTAINER_NAME + EQUAL 允许")
        void containerNameEqualAllowed() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_NAME, KubeContainerOperator.EQUAL)).isTrue();
        }

        @Test
        @DisplayName("CONTAINER_CONTAINER_UID + EQUAL 允许，+ CONTAINS 拒绝（UID 不暴露 contains）")
        void containerUidOnlyEqual() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_CONTAINER_UID, KubeContainerOperator.EQUAL)).isTrue();
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_CONTAINER_UID, KubeContainerOperator.CONTAINS)).isFalse();
        }

        @Test
        @DisplayName("POD_NAME + CONTAINS 允许")
        void podNameContainsAllowed() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.POD_NAME, KubeContainerOperator.CONTAINS)).isTrue();
        }

        @Test
        @DisplayName("POD_LABELS + EQUAL 允许，+ CONTAINS / FILTER_OBJECT 拒绝")
        void podLabelsOnlyEqual() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.POD_LABELS, KubeContainerOperator.EQUAL)).isTrue();
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.POD_LABELS, KubeContainerOperator.CONTAINS)).isFalse();
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.POD_LABELS, KubeContainerOperator.FILTER_OBJECT)).isFalse();
        }

        @Test
        @DisplayName("CONTAINER_NAME + LESS / NOT_EQUAL / IN 等未暴露运算符一律拒绝")
        void containerNameUnexposedOpsRejected() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_NAME, KubeContainerOperator.LESS)).isFalse();
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_NAME, KubeContainerOperator.NOT_EQUAL)).isFalse();
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_NAME, KubeContainerOperator.IN)).isFalse();
        }

        @Test
        @DisplayName("未暴露字段（CONTAINER_ID）一律拒绝")
        void unexposedFieldRejected() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_ID, KubeContainerOperator.EQUAL)).isFalse();
        }

        @Test
        @DisplayName("BEGINS_WITH 等未对外暴露的运算符一律拒绝")
        void notExposedOperatorRejected() {
            assertThat(OperatorDispatcher.isOperatorAllowed(
                QueryableContainerField.CONTAINER_NAME, KubeContainerOperator.BEGINS_WITH)).isFalse();
        }

        @Test
        @DisplayName("null 入参返回 false")
        void nullInputsReturnFalse() {
            assertThat(OperatorDispatcher.isOperatorAllowed(null, KubeContainerOperator.EQUAL)).isFalse();
            assertThat(OperatorDispatcher.isOperatorAllowed(QueryableContainerField.CONTAINER_NAME, null)).isFalse();
        }
    }
}
