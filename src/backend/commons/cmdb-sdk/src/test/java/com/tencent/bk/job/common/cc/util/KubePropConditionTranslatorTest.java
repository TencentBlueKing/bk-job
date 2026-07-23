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
import com.tencent.bk.job.common.exception.InvalidParamException;
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
        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.hasRule()).isFalse();
        assertThat(podFilter.hasRule()).isFalse();
    }

    @Test
    @DisplayName("container_container_uid equal 'docker://...' → containerFilter 多一条 equal/container_uid 规则")
    void containerUidEqual() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_container_uid", "equal", "docker://abcdefg")),
            containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("container_uid");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(rule.getValue()).isEqualTo("docker://abcdefg");
        assertThat(podFilter.hasRule()).isFalse();
    }

    @Test
    @DisplayName("not_equal / not_in / contains 翻译正确")
    void otherOperators() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("container_container_uid", "not_equal", "old"),
            new KubePropCondition("container_container_uid", "not_in", Arrays.asList("a", "b")),
            new KubePropCondition("container_container_uid", "contains", "tencent")
        ), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
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

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
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

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
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

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
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

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
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

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(2);
        IRule existing = containerFilter.getRules().get(0);
        assertThat(((BaseRuleDTO) existing).getField()).isEqualTo("id");
    }

    // ============ , 多值分隔符（container_name / pod_name / container_container_uid）============

    @Test
    @DisplayName("container_name contains 'a,b' → OR(contains(name,a), contains(name,b))")
    void containerNameContainsMultiValue_multi() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_name", "contains", "a,b")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        ComposeRuleDTO or = (ComposeRuleDTO) containerFilter.getRules().get(0);
        assertThat(or.getCondition()).isEqualTo(RuleConditionEnum.OR.getCondition());
        assertThat(or.getRules()).hasSize(2);
        BaseRuleDTO r0 = (BaseRuleDTO) or.getRules().get(0);
        BaseRuleDTO r1 = (BaseRuleDTO) or.getRules().get(1);
        assertThat(r0.getField()).isEqualTo("name");
        assertThat(r0.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(r0.getValue()).isEqualTo("a");
        assertThat(r1.getField()).isEqualTo("name");
        assertThat(r1.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(r1.getValue()).isEqualTo("b");
    }

    @Test
    @DisplayName("container_name contains 'nginx' 单值（无 ,）→ 单条 contains，不折叠")
    void containerNameContainsMultiValue_single() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_name", "contains", "nginx")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(rule.getValue()).isEqualTo("nginx");
    }

    @Test
    @DisplayName("pod_name contains 'x,y' → podFilter OR(contains,contains)")
    void podNameContainsMultiValue_multi() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("pod_name", "contains", "x,y")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getRules()).hasSize(1);
        ComposeRuleDTO or = (ComposeRuleDTO) podFilter.getRules().get(0);
        assertThat(or.getCondition()).isEqualTo(RuleConditionEnum.OR.getCondition());
        assertThat(or.getRules()).hasSize(2);
        assertThat(((BaseRuleDTO) or.getRules().get(0)).getValue()).isEqualTo("x");
        assertThat(((BaseRuleDTO) or.getRules().get(1)).getValue()).isEqualTo("y");
    }

    @Test
    @DisplayName("container_name equal 'a,b' → in(name, [a, b])（equal 多值等价 in）")
    void containerNameEqualMultiValue_multi() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_name", "equal", "a,b")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("name");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.IN.getOperator());
        assertThat(rule.getValue()).isEqualTo(Arrays.asList("a", "b"));
    }

    @Test
    @DisplayName("container_name equal 'a' 单值 → equal(name,a)，不折叠成 in")
    void containerNameEqualMultiValue_single() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_name", "equal", "a")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("name");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(rule.getValue()).isEqualTo("a");
    }

    @Test
    @DisplayName("container_container_uid equal 'u1,u2' → in(container_uid,[u1,u2])")
    void containerUidEqualMultiValue_multi() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_container_uid", "equal", "u1,u2")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("container_uid");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.IN.getOperator());
        assertThat(rule.getValue()).isEqualTo(Arrays.asList("u1", "u2"));
    }

    @Test
    @DisplayName("container_container_uid equal 'u1' 单值 → equal(container_uid,u1)")
    void containerUidEqualMultiValue_single() {
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_container_uid", "equal", "u1")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getField()).isEqualTo("container_uid");
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(rule.getValue()).isEqualTo("u1");
    }

    @Test
    @DisplayName("边界规范化：' a , b , a , ' → 去空段 + trim + 去重 后为 [a, b]，走 OR/contains 或 in")
    void multiValueSplit_trimAndDedup() {
        // contains 分支
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_name", "contains", " a , b , a , ")), containerFilter, podFilter);
        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        ComposeRuleDTO or = (ComposeRuleDTO) containerFilter.getRules().get(0);
        assertThat(or.getRules()).hasSize(2);
        assertThat(((BaseRuleDTO) or.getRules().get(0)).getValue()).isEqualTo("a");
        assertThat(((BaseRuleDTO) or.getRules().get(1)).getValue()).isEqualTo("b");

        // equal 分支
        PropertyFilterDTO cf2 = new PropertyFilterDTO();
        cf2.setCondition(RuleConditionEnum.AND.getCondition());
        PropertyFilterDTO pf2 = new PropertyFilterDTO();
        pf2.setCondition(RuleConditionEnum.AND.getCondition());
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_container_uid", "equal", " u1 , u2 , u1 ")), cf2, pf2);
        assertThat(cf2.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(pf2.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(cf2.getRules()).hasSize(1);
        BaseRuleDTO in = (BaseRuleDTO) cf2.getRules().get(0);
        assertThat(in.getOperator()).isEqualTo(RuleOperatorEnum.IN.getOperator());
        assertThat(in.getValue()).isEqualTo(Arrays.asList("u1", "u2"));
    }

    @Test
    @DisplayName("空/仅 , 输入：抛 InvalidParamException（400）")
    void multiValueSplit_emptyThrows() {
        assertThatThrownBy(() -> KubePropConditionTranslator.appendRules(
            Collections.singletonList(new KubePropCondition("container_name", "contains", "")),
            containerFilter, podFilter))
            .isInstanceOf(InvalidParamException.class);

        assertThatThrownBy(() -> KubePropConditionTranslator.appendRules(
            Collections.singletonList(new KubePropCondition("container_name", "contains", ",")),
            containerFilter, podFilter))
            .isInstanceOf(InvalidParamException.class);

        assertThatThrownBy(() -> KubePropConditionTranslator.appendRules(
            Collections.singletonList(new KubePropCondition("container_container_uid", "equal", " , ")),
            containerFilter, podFilter))
            .isInstanceOf(InvalidParamException.class);

        assertThatThrownBy(() -> KubePropConditionTranslator.appendRules(
            Collections.singletonList(new KubePropCondition("pod_name", "equal", null)),
            containerFilter, podFilter))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("container_container_uid not_equal 不在 , 白名单：value 中的 , 不触发拆分，按原语义传给 cmdb")
    void multiValueSplit_notEqualNotEnabled() {
        // not_equal 未加入 UID 的 , 白名单，即便 value 含 , 也应走标量 not_equal
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("container_container_uid", "not_equal", "u1,u2")), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(containerFilter.getRules()).hasSize(1);
        BaseRuleDTO rule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(rule.getOperator()).isEqualTo(RuleOperatorEnum.NOT_EQUAL.getOperator());
        assertThat(rule.getValue()).isEqualTo("u1,u2");
    }

    @Test
    @DisplayName("pod_labels 值中即便出现 , 也不触发多值拆分，仍走 label selector 表达式解析")
    void multiValueSplit_podLabelsUnaffected() {
        // pod_labels 表达式本身就用 , 分隔多项（如 "k1=v1,k2 in (a,b)"），
        // 白名单机制保证 pod_labels 绝不进入多值拆分分支。此处直接验证含 , 的表达式仍能正确解析。
        KubePropConditionTranslator.appendRules(Collections.singletonList(
            new KubePropCondition("pod_labels", "equal", "app=nginx,env in (prod,test)")
        ), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getRules()).hasSize(1);
        BaseRuleDTO outer = (BaseRuleDTO) podFilter.getRules().get(0);
        assertThat(outer.getOperator()).isEqualTo(RuleOperatorEnum.FILTER_OBJECT.getOperator());
        // 内部表达式应被正确解析为 2 项（app=nginx 、 env in (prod,test)）
        ComposeRuleDTO inner = (ComposeRuleDTO) outer.getValue();
        assertThat(inner.getRules()).hasSize(2);
    }

    @Test
    @DisplayName("复合：container_name contains 'c1,c2' + pod_name contains 'p1,p2' " +
        "→ 两个 filter 各一条 OR(contains,contains)")
    void multiValueSplit_containerAndPodContainsCompound() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("container_name", "contains", "c1,c2"),
            new KubePropCondition("pod_name", "contains", "p1,p2")
        ), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());

        // containerFilter: 1 条 OR(contains(name,c1), contains(name,c2))
        assertThat(containerFilter.getRules()).hasSize(1);
        ComposeRuleDTO cOr = (ComposeRuleDTO) containerFilter.getRules().get(0);
        assertThat(cOr.getCondition()).isEqualTo(RuleConditionEnum.OR.getCondition());
        assertThat(cOr.getRules()).hasSize(2);
        BaseRuleDTO c0 = (BaseRuleDTO) cOr.getRules().get(0);
        BaseRuleDTO c1 = (BaseRuleDTO) cOr.getRules().get(1);
        assertThat(c0.getField()).isEqualTo("name");
        assertThat(c0.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(c0.getValue()).isEqualTo("c1");
        assertThat(c1.getField()).isEqualTo("name");
        assertThat(c1.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(c1.getValue()).isEqualTo("c2");

        // podFilter: 1 条 OR(contains(name,p1), contains(name,p2))
        assertThat(podFilter.getRules()).hasSize(1);
        ComposeRuleDTO pOr = (ComposeRuleDTO) podFilter.getRules().get(0);
        assertThat(pOr.getCondition()).isEqualTo(RuleConditionEnum.OR.getCondition());
        assertThat(pOr.getRules()).hasSize(2);
        BaseRuleDTO p0 = (BaseRuleDTO) pOr.getRules().get(0);
        BaseRuleDTO p1 = (BaseRuleDTO) pOr.getRules().get(1);
        assertThat(p0.getField()).isEqualTo("name");
        assertThat(p0.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(p0.getValue()).isEqualTo("p1");
        assertThat(p1.getField()).isEqualTo("name");
        assertThat(p1.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(p1.getValue()).isEqualTo("p2");
    }

    @Test
    @DisplayName("复合：container_name contains 'c1,c2' + container_container_uid equal 'id1,id2' " +
        "→ containerFilter 一条 OR(contains,contains) + 一条 in([...])")
    void multiValueSplit_containerNameContainsAndUidEqualCompound() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("container_name", "contains", "c1,c2"),
            new KubePropCondition("container_container_uid", "equal", "id1,id2")
        ), containerFilter, podFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.hasRule()).isFalse();
        assertThat(containerFilter.getRules()).hasSize(2);

        // 第 1 条：container_name contains 'c1,c2' → OR(contains(name,c1), contains(name,c2))
        ComposeRuleDTO nameOr = (ComposeRuleDTO) containerFilter.getRules().get(0);
        assertThat(nameOr.getCondition()).isEqualTo(RuleConditionEnum.OR.getCondition());
        assertThat(nameOr.getRules()).hasSize(2);
        BaseRuleDTO n0 = (BaseRuleDTO) nameOr.getRules().get(0);
        BaseRuleDTO n1 = (BaseRuleDTO) nameOr.getRules().get(1);
        assertThat(n0.getField()).isEqualTo("name");
        assertThat(n0.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(n0.getValue()).isEqualTo("c1");
        assertThat(n1.getField()).isEqualTo("name");
        assertThat(n1.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(n1.getValue()).isEqualTo("c2");

        // 第 2 条：container_container_uid equal 'id1,id2' → in(container_uid,[id1,id2])
        BaseRuleDTO uidRule = (BaseRuleDTO) containerFilter.getRules().get(1);
        assertThat(uidRule.getField()).isEqualTo("container_uid");
        assertThat(uidRule.getOperator()).isEqualTo(RuleOperatorEnum.IN.getOperator());
        assertThat(uidRule.getValue()).isEqualTo(Arrays.asList("id1", "id2"));
    }

    @Test
    @DisplayName("复合单值：container_container_uid equal 'id' + container_name contains 'a' " +
        "→ containerFilter 一条 equal(container_uid,id) + 一条 contains(name,a)，均不折叠")
    void compound_uidEqualAndNameContains_singleValues() {
        KubePropConditionTranslator.appendRules(Arrays.asList(
            new KubePropCondition("container_container_uid", "equal", "id"),
            new KubePropCondition("container_name", "contains", "a")
        ), containerFilter, podFilter);

        System.out.println("containerFilter: " + containerFilter);

        assertThat(containerFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.getCondition()).isEqualTo(RuleConditionEnum.AND.getCondition());
        assertThat(podFilter.hasRule()).isFalse();
        assertThat(containerFilter.getRules()).hasSize(2);

        // 第 1 条：container_container_uid equal 'id' → equal(container_uid, 'id')
        BaseRuleDTO uidRule = (BaseRuleDTO) containerFilter.getRules().get(0);
        assertThat(uidRule.getField()).isEqualTo("container_uid");
        assertThat(uidRule.getOperator()).isEqualTo(RuleOperatorEnum.EQUAL.getOperator());
        assertThat(uidRule.getValue()).isEqualTo("id");

        // 第 2 条：container_name contains 'a' → contains(name, 'a')
        BaseRuleDTO nameRule = (BaseRuleDTO) containerFilter.getRules().get(1);
        assertThat(nameRule.getField()).isEqualTo("name");
        assertThat(nameRule.getOperator()).isEqualTo(RuleOperatorEnum.CONTAINS.getOperator());
        assertThat(nameRule.getValue()).isEqualTo("a");
    }
}
