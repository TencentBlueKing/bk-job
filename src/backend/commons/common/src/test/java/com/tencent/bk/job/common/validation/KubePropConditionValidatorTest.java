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

package com.tencent.bk.job.common.validation;

import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 覆盖 {@link KubePropConditionValidator} 的字段白名单、按字段维度的运算符派发、value 形态分支
 * 以及 pod_labels 标签选择器表达式合法性。
 */
@DisplayName("KubePropConditionValidator: 单条 propCondition 校验")
class KubePropConditionValidatorTest {

    @Nested
    @DisplayName("字段 / 运算符 白名单")
    class FieldAndOperatorWhitelist {

        @Test
        @DisplayName("空字段名 → InvalidParamException")
        void blankFieldRejected() {
            KubePropCondition cond = new KubePropCondition("", "equal", "x");
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("未知字段 → InvalidParamException")
        void unknownFieldRejected() {
            KubePropCondition cond = new KubePropCondition("not_in_whitelist", "equal", "x");
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("空运算符 → InvalidParamException")
        void blankOperatorRejected() {
            KubePropCondition cond = new KubePropCondition("container_name", "", "x");
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("未知运算符 → InvalidParamException")
        void unknownOperatorRejected() {
            KubePropCondition cond = new KubePropCondition("container_name", "foo_bar", "x");
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("未暴露字段（CONTAINER_ID）→ InvalidParamException")
        void unexposedFieldRejected() {
            KubePropCondition cond = new KubePropCondition("container_id", "equal", 1L);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("未暴露字段（POD_ID）→ InvalidParamException")
        void podIdRejected() {
            KubePropCondition cond = new KubePropCondition("pod_id", "equal", 1L);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("CONTAINER_CONTAINER_UID 不暴露 contains（仅 equal）→ InvalidParamException")
        void containerUidContainsRejected() {
            KubePropCondition cond = new KubePropCondition("container_container_uid", "contains", "tencent");
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("CONTAINER_NAME 不暴露 NOT_EQUAL（按字段独立配置） → InvalidParamException")
        void containerNameNotEqualRejected() {
            KubePropCondition cond = new KubePropCondition("container_name", "not_equal", "x");
            assertThatThrownBy(() -> KubePropConditionValidator.validate(cond))
                .isInstanceOf(InvalidParamException.class);
        }
    }

    @Nested
    @DisplayName("Value 形态 - 暴露字段")
    class ValueShape {

        @Test
        @DisplayName("CONTAINER_NAME / POD_NAME 暴露 equal/contains：value 必须为标量")
        void exposedStringOperatorsRequireScalar() {
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "equal", null)))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "contains", Collections.singletonList("nginx"))))
                .isInstanceOf(InvalidParamException.class);

            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "equal", "nginx"))).doesNotThrowAnyException();
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "contains", "ngx"))).doesNotThrowAnyException();
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_name", "equal", "pod-a"))).doesNotThrowAnyException();
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_name", "contains", "api"))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CONTAINER_CONTAINER_UID equal：value 必须为非空标量字符串")
        void containerUidEqual() {
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_container_uid", "equal",
                    "docker://abcdefgb1edd509a70140768c212822faa3833eb0d29d600ef4877cb0ed")))
                .doesNotThrowAnyException();

            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_container_uid", "equal", null)))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("POD_LABELS equal：value 必须为合法 K8s 标签选择器表达式字符串")
        void podLabelsEqualRequiresValidExpression() {
            // 空值
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", null)))
                .isInstanceOf(InvalidParamException.class);
            // 非字符串
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", new LinkedHashMap<>())))
                .isInstanceOf(InvalidParamException.class);
            // 集合
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", Collections.singletonList("app=nginx"))))
                .isInstanceOf(InvalidParamException.class);
            // 空字符串 / 仅空白
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "")))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "   ")))
                .isInstanceOf(InvalidParamException.class);
            // 非法 key（含非法字符 @）
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "ab@c=x")))
                .isInstanceOf(InvalidParamException.class);
            // 非法语法（多余的 = 或解析失败）
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "k=v=v")))
                .isInstanceOf(InvalidParamException.class);

            // 合法用例：单 key=value
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "app=nginx"))).doesNotThrowAnyException();
            // 合法用例：多 key 逗号分隔，含 in / exists
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "app=nginx,env in (prod,test),tier")))
                .doesNotThrowAnyException();
            // 合法用例：!= 与 notin
            assertThatCode(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "equal", "stage!=dev,role notin (db,cache)")))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("未暴露运算符 in/not_in/exist/not_exist/filter_object 一律拒绝")
        void unexposedOperatorsRejected() {
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "in", Collections.singletonList("nginx"))))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "not_in", Collections.singletonList("nginx"))))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "exist", null)))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("container_name", "not_exist", null)))
                .isInstanceOf(InvalidParamException.class);
            // pod_labels 不再对外暴露 filter_object
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                new KubePropCondition("pod_labels", "filter_object", new LinkedHashMap<>())))
                .isInstanceOf(InvalidParamException.class);
        }
    }

    @Nested
    @DisplayName("批量校验")
    class ListValidation {

        @Test
        @DisplayName("null 列表 / 空列表：no-op，不抛异常")
        void nullOrEmptyListNoOp() {
            assertThatCode(() -> KubePropConditionValidator.validate((List<KubePropCondition>) null))
                .doesNotThrowAnyException();
            assertThatCode(() -> KubePropConditionValidator.validate(Collections.emptyList()))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("列表中含 null 元素 → InvalidParamException")
        void nullElementInListRejected() {
            assertThatThrownBy(() -> KubePropConditionValidator.validate(
                Collections.singletonList(null)))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("混合多条 happy path 通过")
        void happyPathMultiple() {
            List<KubePropCondition> conds = Arrays.asList(
                new KubePropCondition("container_container_uid", "equal", "docker://abcdefg"),
                new KubePropCondition("container_name", "contains", "nginx"),
                new KubePropCondition("pod_name", "equal", "pod-a"),
                new KubePropCondition("pod_name", "contains", "api"),
                new KubePropCondition("pod_labels", "equal", "app=nginx,env in (prod,test)")
            );
            assertThatCode(() -> KubePropConditionValidator.validate(conds)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("单条 happy path")
    void singleHappyPath() {
        KubePropCondition cond = new KubePropCondition("pod_name", "contains", "api");
        assertThatCode(() -> KubePropConditionValidator.validate(cond)).doesNotThrowAnyException();
        assertThat(cond.getValue()).isEqualTo("api");
    }
}
