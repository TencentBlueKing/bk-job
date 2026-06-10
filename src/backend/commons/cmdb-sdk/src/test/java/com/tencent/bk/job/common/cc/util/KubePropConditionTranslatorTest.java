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

package com.tencent.bk.job.common.cc.util;

import com.tencent.bk.job.common.cc.model.filter.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.ComposeRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.IRule;
import com.tencent.bk.job.common.cc.model.filter.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.filter.RuleConditionEnum;
import com.tencent.bk.job.common.cc.model.filter.RuleOperatorEnum;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 校验 {@link KubePropConditionTranslator} 把 KubePropCondition 翻译为 bk-cmdb container/pod 过滤规则的正确性。
 * 关键点：
 * <ul>
 *   <li>按字段前缀（{@code container_} / {@code pod_}）路由到对应过滤器</li>
 *   <li>cmdb 字段名去掉前缀（{@code container_container_uid} → {@code container_uid}、{@code pod_name} → {@code name}）</li>
 *   <li>每个运算符使用 bk-cmdb 协议名（equal/not_equal/in/not_in/less/less_or_equal/greater/greater_or_equal/contains/exist/not_exist）</li>
 *   <li>{@code pod_labels} 走标签选择器表达式特殊路径：字符串解析 → 嵌套 filter_object 规则</li>
 * </ul>
 */
@DisplayName("KubePropConditionTranslator: propConditions → bk-cmdb 规则")
class KubePropConditionTranslatorTest {

    private PropertyFilterDTO containerFilter;
    private PropertyFilterDTO podFilter;

    @BeforeEach
    void setUp() {
        containerFilter = new PropertyFilterDTO();
        containerFilter.setCondition(RuleConditionEnum.AND.getCondition());
        podFilter = new PropertyFilterDTO();
        podFilter.setCondition(RuleConditionEnum.AND.getCondition());
    }

    @Test
    @DisplayName("null / 空列表：no-op，两个过滤器都不变化")
    void noOpForEmpty() {
        assertThatCode(() -> KubePropConditionTranslator.appendRules(null, containerFilter, podFilter))
            .doesNotThrowAnyException();
        assertThatCode(() ->
            KubePropConditionTranslator.appendRules(Collections.emptyList(), containerFilter, podFilter))
            .doesNotThrowAnyException();
        assertThat(containerFilter.hasRule()).isFalse();
        assertThat(podFilter.hasRule()).isFalse();
    }

    @Test
    @DisplayName("container_container_uid equal 'docker://...' → containerFilter 多一条 equal/container_uid 规则")
    void containerUidEqual() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_container_uid", "equal", "docker://abcdefg")),
            containerFilter, podFilter);

        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("container_uid");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(rule.getValue()).isEqualTo("docker://abcdefg");
        assertThat(podFilter.hasRule()).isFalse();
    }

    @Test
    @DisplayName("pod_name in [...] → podFilter 多一条 in/name 规则")
    void podNameIn() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("pod_name", "in", Arrays.asList("p-1", "p-2"))), containerFilter, podFilter);

        assertThat(podFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) podFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("name");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.IN.getOperator());
        assertThat(rule.getValue()).isEqualTo(Arrays.asList("p-1", "p-2"));
        assertThat(containerFilter.hasRule()).isFalse();
    }

    @Test
    @DisplayName("pod_id 全部比较运算符正确翻译")
    void numericComparisons() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("pod_id", "less", 10),
            new KubePropCondition("pod_id", "less_or_equal", 20),
            new KubePropCondition("pod_id", "greater", 30),
            new KubePropCondition("pod_id", "greater_or_equal", 40)
        ), containerFilter, podFilter);

        assertThat(podFilter.getRules()).hasSize(4);
        assertThat(((BaseRuleDTO) podFilter.getRules().get(0)).getOperator())
            .isEqualTo(RuleOperatorEnum.LESS.getOperator());
        assertThat(((BaseRuleDTO) podFilter.getRules().get(1)).getOperator())
            .isEqualTo(RuleOperatorEnum.LESS_OR_EQUAL.getOperator());
        assertThat(((BaseRuleDTO) podFilter.getRules().get(2)).getOperator())
            .isEqualTo(RuleOperatorEnum.GREATER.getOperator());
        assertThat(((BaseRuleDTO) podFilter.getRules().get(3)).getOperator())
            .isEqualTo(RuleOperatorEnum.GREATER_OR_EQUAL.getOperator());
    }

    @Test
    @DisplayName("not_equal / not_in / contains 翻译正确")
    void otherOperators() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("container_container_uid", "not_equal", "old"),
            new KubePropCondition("container_container_uid", "not_in", Arrays.asList("a", "b")),
            new KubePropCondition("container_container_uid", "contains", "tencent")
        ), containerFilter, podFilter);

        assertThat(containerFilter.getRules()).hasSize(3);
        assertThat(((BaseRuleDTO) containerFilter.getRules().get(0)).getOperator())
            .isEqualTo(RuleOperatorEnum.NOT_EQUAL.getOperator());
        assertThat(((BaseRuleDTO) containerFilter.getRules().get(1)).getOperator())
            .isEqualTo(RuleOperatorEnum.NOT_IN.getOperator());
        assertThat(((BaseRuleDTO) containerFilter.getRules().get(2)).getOperator())
            .isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
    }

    @Test
    @DisplayName("exist / not_exist valueless 运算符：value 取 true 兜底（cmdb 协议要求非空 value）")
    void valuelessOperators() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("pod_name", "exist", null),
            new KubePropCondition("pod_name", "not_exist", null)
        ), containerFilter, podFilter);

        assertThat(podFilter.getRules()).hasSize(2);
        BaseRuleDTO exist = (BaseRuleDTO) podFilter.getRules().get(0);
        BaseRuleDTO notExist = (BaseRuleDTO) podFilter.getRules().get(1);
        assertThat(exist.getOperator()).isEqualTo(RuleOperatorEnum.EXIST.getOperator());
        assertThat(exist.getValue()).isEqualTo(true);
        assertThat(notExist.getOperator()).isEqualTo(RuleOperatorEnum.NOT_EXIST.getOperator());
        assertThat(notExist.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("pod_labels equal 单标签 'k=v' → podFilter 一条嵌套 filter_object/labels 规则（内部 1 条 equal）")
    void podLabelsSingleEquals() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("pod_labels", "equal", "app=nginx")), containerFilter, podFilter);

        assertThat(containerFilter.hasRule()).isFalse();
        assertThat(podFilter.getRules()).hasSize(1);

        BaseRuleDTO outer = (BaseRuleDTO) podFilter.getRules().get(0);
        assertThat(outer.getField()).isEqualTo("labels");
        assertThat(outer.getOperator()).isEqualTo(RuleOperatorEnum.FILTER_OBJECT.getOperator());

        ComposeRuleDTO inner = (ComposeRuleDTO) outer.getValue();
        assertThat(inner.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(inner.getRules()).hasSize(1);
        BaseRuleDTO labelRule = (BaseRuleDTO) inner.getRules().get(0);
        assertThat(labelRule.getField()).isEqualTo("app");
        assertThat(labelRule.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(labelRule.getValue()).isEqualTo("nginx");
    }

    @Test
    @DisplayName("pod_labels equal 'k1=v1,k2 in (a,b),k3' → 嵌套 AND，equal/in/exists 三条规则")
    void podLabelsMultiOperators() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("pod_labels", "equal", "app=nginx,env in (prod,test),tier")
        ), containerFilter, podFilter);

        assertThat(podFilter.getRules()).hasSize(1);
        BaseRuleDTO outer = (BaseRuleDTO) podFilter.getRules().get(0);
        assertThat(outer.getField()).isEqualTo("labels");
        assertThat(outer.getOperator()).isEqualTo(RuleOperatorEnum.FILTER_OBJECT.getOperator());

        ComposeRuleDTO inner = (ComposeRuleDTO) outer.getValue();
        assertThat(inner.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(inner.getRules()).hasSize(3);

        BaseRuleDTO r0 = (BaseRuleDTO) inner.getRules().get(0);
        assertThat(r0.getField()).isEqualTo("app");
        assertThat(r0.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(r0.getValue()).isEqualTo("nginx");

        BaseRuleDTO r1 = (BaseRuleDTO) inner.getRules().get(1);
        assertThat(r1.getField()).isEqualTo("env");
        assertThat(r1.getOperator()).isEqualTo(RuleOperatorEnum.IN.getOperator());
        assertThat(r1.getValue()).isEqualTo(Arrays.asList("prod", "test"));

        BaseRuleDTO r2 = (BaseRuleDTO) inner.getRules().get(2);
        assertThat(r2.getField()).isEqualTo("tier");
        // 仅 key（无操作符）→ exists
        assertThat(r2.getOperator()).isEqualTo(RuleOperatorEnum.EXIST.getOperator());
    }

    @Test
    @DisplayName("混合 container/pod 字段 + pod_labels：按前缀正确分流且 pod_labels 走 filter_object")
    void mixedFieldsSplit() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("container_container_uid", "equal", "docker://abcdefg"),
            new KubePropCondition("pod_name", "equal", "pod-a"),
            new KubePropCondition("container_name", "in", Arrays.asList("a", "b")),
            new KubePropCondition("pod_labels", "equal", "app=nginx")
        ), containerFilter, podFilter);

        assertThat(containerFilter.getRules()).hasSize(2);
        assertThat(podFilter.getRules()).hasSize(2);
        // pod_labels 在 podFilter 第二条
        BaseRuleDTO labelsRule = (BaseRuleDTO) podFilter.getRules().get(1);
        assertThat(labelsRule.getField()).isEqualTo("labels");
        assertThat(labelsRule.getOperator()).isEqualTo(RuleOperatorEnum.FILTER_OBJECT.getOperator());
    }

    @Test
    @DisplayName("枚举里存在但当前不暴露的运算符（如 begins_with）→ IllegalArgumentException")
    void unsupportedOperatorRejected() {
        // 校验阶段会拦截 begins_with；但 translator 是 sdk 内部协议层，对未知运算符必须抛错以暴露漏配
        assertThatThrownBy(() -> KubePropConditionTranslator.appendRules(
            Collections.singletonList(new KubePropCondition("container_name", "begins_with", "x")),
            containerFilter, podFilter))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("追加规则不会清除已有规则（保留 caller 在调用前已 addRule 的内容）")
    void appendsToExistingRules() {
        containerFilter.addRule(BaseRuleDTO.in("id", Arrays.asList(1L, 2L)));
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_name", "equal", "nginx")), containerFilter, podFilter);

        assertThat(containerFilter.getRules()).hasSize(2);
        IRule existing = containerFilter.getRules().get(0);
        assertThat(((BaseRuleDTO) existing).getField()).isEqualTo("id");
    }
}
