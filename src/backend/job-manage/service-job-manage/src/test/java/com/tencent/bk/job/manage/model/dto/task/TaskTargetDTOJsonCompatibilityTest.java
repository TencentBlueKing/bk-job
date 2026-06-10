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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 回归测试：mock {@code task_template_step_*.destination_host_list} /
 * {@code task_plan_step_*.destination_host_list} / {@code task_*_variable.default_value} 列里
 * 历史持久化的 JSON，确认 {@link TaskTargetDTO#fromJsonString(String)} 仍能正确反序列化。
 * <p>
 * 关键回归：
 * <ul>
 *   <li>老 JSON 没有 {@code containerFilters} → 反序列化为 null，{@code toVO()} 行为与现状一致</li>
 *   <li>新 JSON 含 {@code containerFilters[].propConditions} → 完整保真</li>
 *   <li>未知顶层字段不抛异常（FAIL_ON_UNKNOWN_PROPERTIES=disabled）</li>
 *   <li>NON_EMPTY 序列化：{@code containerFilters} 为 null/空时不出现在 JSON 字节面</li>
 * </ul>
 */
@DisplayName("TaskTargetDTO: 历史 / 新版本 destination_host_list / variable.default_value JSON 兼容")
class TaskTargetDTOJsonCompatibilityTest {

    @Nested
    @DisplayName("Legacy")
    class Legacy {

        @Test
        @DisplayName("最常见的老 JSON（hostNodeList + containerList）→ containerFilters 为 null")
        void legacyHostAndContainerListLoads() {
            // 历史持久化样本（来自 task_template_step_script.destination_host_list 典型形态）
            String json = "{"
                + "\"hostNodeList\":{"
                + "  \"hostList\":[{\"hostId\":101,\"cloudAreaId\":0,\"ip\":\"10.0.0.1\"}],"
                + "  \"dynamicGroupId\":[\"dg-1\"]"
                + "},"
                + "\"containerList\":[{\"id\":555,\"containerId\":\"docker://abc\"}]"
                + "}";

            TaskTargetDTO target = TaskTargetDTO.fromJsonString(json);

            assertThat(target).isNotNull();
            assertThat(target.getHostNodeList()).isNotNull();
            assertThat(target.getContainerList()).hasSize(1);
            // 关键回归：老数据无 containerFilters 字段 → 必须 null
            assertThat(target.getContainerFilters()).isNull();
        }

        @Test
        @DisplayName("variable.default_value 老 JSON（仅 variable 字段） → 反序列化稳定")
        void legacyVariableDefaultValueLoads() {
            String json = "{\"variable\":\"hostListVar\"}";
            TaskTargetDTO target = TaskTargetDTO.fromJsonString(json);
            assertThat(target).isNotNull();
            assertThat(target.getVariable()).isEqualTo("hostListVar");
            assertThat(target.getContainerFilters()).isNull();
            assertThat(target.getHostNodeList()).isNull();
        }

        @Test
        @DisplayName("空字符串/null：fromJsonString 安全返回 null")
        void blankReturnsNull() {
            assertThat(TaskTargetDTO.fromJsonString(null)).isNull();
            assertThat(TaskTargetDTO.fromJsonString("")).isNull();
            assertThat(TaskTargetDTO.fromJsonString("   ")).isNull();
        }

        @Test
        @DisplayName("含未知顶层字段：FAIL_ON_UNKNOWN_PROPERTIES=disabled 不抛异常")
        void unknownFieldsTolerated() {
            String json = "{"
                + "\"variable\":\"hostListVar\","
                + "\"futureExtension\":{\"foo\":\"bar\"}"
                + "}";
            assertThatCode(() -> {
                TaskTargetDTO target = TaskTargetDTO.fromJsonString(json);
                assertThat(target).isNotNull();
                assertThat(target.getVariable()).isEqualTo("hostListVar");
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("New feature")
    class NewFeature {

        @Test
        @DisplayName("含 Web 入口拓扑对象 + propConditions 的新 JSON 完整保真")
        void newContainerFiltersJsonLoads() {
            String json = "{"
                + "\"containerFilters\":[{"
                + "  \"clusterNodes\":[{\"id\":1000,\"name\":\"集群1000\"}],"
                + "  \"namespaceNodes\":[{\"id\":10000,\"name\":\"命名空间10000\"}],"
                + "  \"propConditions\":["
                + "    {\"field\":\"container_container_uid\",\"operator\":\"equal\",\"value\":\"docker://nginx\"}"
                + "  ]"
                + "}]"
                + "}";

            TaskTargetDTO target = TaskTargetDTO.fromJsonString(json);
            
            assertThat(target).isNotNull();
            assertThat(target.getContainerFilters()).hasSize(1);
            KubeContainerFilter cf = target.getContainerFilters().get(0);
            assertThat(cf.getClusterNodes()).hasSize(1);
            assertThat(cf.getClusterNodes().get(0).getId()).isEqualTo(1000L);
            assertThat(cf.getClusterNodes().get(0).getName()).isEqualTo("集群1000");
            assertThat(cf.getPropConditions()).hasSize(1);
            assertThat(cf.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
            assertThat(cf.getPropConditions().get(0).getValue()).isEqualTo("docker://nginx");
        }

        @Test
        @DisplayName("NON_EMPTY：containerFilters 为 null/空时不出现在序列化输出（保持老数据字节面）")
        void emptyContainerFiltersOmitted() {
            TaskTargetDTO target = new TaskTargetDTO();
            target.setVariable("hostListVar");

            // 走和 fromJsonString 同一个 mapper（JsonMapper.nonEmptyMapper），保证一致
            String json = JsonMapper.nonEmptyMapper().toJson(target);
            assertThat(json).doesNotContain("containerFilters");

            target.setContainerFilters(Collections.emptyList());
            assertThat(JsonMapper.nonEmptyMapper().toJson(target)).doesNotContain("containerFilters");
        }

        @Test
        @DisplayName("Round-trip：新 DTO（Web 入口形态）序列化 → 反序列化 → 字段保真")
        void roundTripPreservesFields() {
            TaskTargetDTO original = new TaskTargetDTO();
            original.setVariable("backendContainers");

            KubeContainerFilter cf = new KubeContainerFilter();
            cf.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(1000L, "集群1000")));
            cf.setPropConditions(Arrays.asList(
                new KubePropCondition("container_container_uid", "equal", "docker://nginx"),
                new KubePropCondition("pod_id", "equal", 100)
            ));
            original.setContainerFilters(Collections.singletonList(cf));

            String json = JsonUtils.toJson(original);
            TaskTargetDTO back = TaskTargetDTO.fromJsonString(json);
            
            assertThat(back).isNotNull();
            assertThat(back.getVariable()).isEqualTo("backendContainers");
            assertThat(back.getContainerFilters()).hasSize(1);
            assertThat(back.getContainerFilters().get(0).getClusterNodes().get(0).getName())
                .isEqualTo("集群1000");
            assertThat(back.getContainerFilters().get(0).getPropConditions()).hasSize(2);
            assertThat(back.getContainerFilters().get(0).getPropConditions().get(1).getField())
                .isEqualTo("pod_id");
        }
    }

    /**
     * {@link TaskTargetDTO#toJsonString()} 的兜底/回归覆盖：
     * <ul>
     *   <li>纯 containerList / 纯 containerFilters / 两者皆有：直接构造 DTO 不应被吞成字面量 "null"</li>
     *   <li>三类目标全空：仍兜底返回字面量 "null"，保持历史语义</li>
     *   <li>variable 非空：清空 hostNodeList 行为保持不变（既有契约）</li>
     *   <li>历史"fromVO 造空 hostNodeList 对象"路径：序列化字节面与历史一致（NON_EMPTY 整体省略）</li>
     * </ul>
     */
    @Nested
    @DisplayName("toJsonString 兜底/回归")
    class ToJsonStringBoundary {

        @Test
        @DisplayName("仅 containerList：不再被吞成 \"null\"，能正常序列化并 round-trip 保真")
        void containerListOnlyIsPersisted() {
            TaskTargetDTO original = new TaskTargetDTO();
            TaskTargetContainerDTO container = new TaskTargetContainerDTO();
            container.setId(555L);
            original.setContainerList(Collections.singletonList(container));

            String json = original.toJsonString();

            assertThat(json).isNotEqualTo("null");
            assertThat(json).contains("containerList");

            TaskTargetDTO back = TaskTargetDTO.fromJsonString(json);
            assertThat(back).isNotNull();
            assertThat(back.getContainerList()).hasSize(1);
            assertThat(back.getContainerList().get(0).getId()).isEqualTo(555L);
            assertThat(back.getHostNodeList()).isNull();
            assertThat(back.getContainerFilters()).isNull();
        }

        @Test
        @DisplayName("仅 containerFilters：不再被吞成 \"null\"，能正常序列化并 round-trip 保真")
        void containerFiltersOnlyIsPersisted() {
            TaskTargetDTO original = new TaskTargetDTO();
            KubeContainerFilter cf = new KubeContainerFilter();
            cf.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(1000L, "集群1000")));
            cf.setPropConditions(Collections.singletonList(
                new KubePropCondition("container_container_uid", "equal", "docker://nginx")));
            original.setContainerFilters(Collections.singletonList(cf));

            String json = original.toJsonString();

            assertThat(json).isNotEqualTo("null");
            assertThat(json).contains("containerFilters");

            TaskTargetDTO back = TaskTargetDTO.fromJsonString(json);
            assertThat(back).isNotNull();
            assertThat(back.getContainerFilters()).hasSize(1);
            assertThat(back.getContainerFilters().get(0).getClusterNodes().get(0).getId()).isEqualTo(1000L);
            assertThat(back.getHostNodeList()).isNull();
            assertThat(back.getContainerList()).isNull();
        }

        @Test
        @DisplayName("containerList + containerFilters 同时存在：两者均可正常序列化")
        void containerListAndFiltersCoexist() {
            TaskTargetDTO original = new TaskTargetDTO();
            TaskTargetContainerDTO container = new TaskTargetContainerDTO();
            container.setId(555L);
            original.setContainerList(Collections.singletonList(container));

            KubeContainerFilter cf = new KubeContainerFilter();
            cf.setPropConditions(Collections.singletonList(
                new KubePropCondition("pod_name", "equal", "nginx")));
            original.setContainerFilters(Collections.singletonList(cf));

            String json = original.toJsonString();

            TaskTargetDTO back = TaskTargetDTO.fromJsonString(json);
            
            assertThat(back).isNotNull();
            assertThat(back.getContainerList()).hasSize(1);
            assertThat(back.getContainerFilters()).hasSize(1);
        }

        @Test
        @DisplayName("三类目标全空：仍兜底返回字面量 \"null\"（保持历史语义）")
        void allTargetsEmptyReturnsNullLiteral() {
            TaskTargetDTO empty = new TaskTargetDTO();
            assertThat(empty.toJsonString()).isEqualTo("null");

            TaskTargetDTO emptyCollections = new TaskTargetDTO();
            emptyCollections.setContainerList(Collections.emptyList());
            emptyCollections.setContainerFilters(Collections.emptyList());
            assertThat(emptyCollections.toJsonString()).isEqualTo("null");
        }

        @Test
        @DisplayName("variable 非空：清空 hostNodeList 的既有契约保持不变")
        void variablePresentClearsHostNodeListAsBefore() {
            TaskTargetDTO original = new TaskTargetDTO();
            original.setVariable("hostListVar");
            original.setHostNodeList(new TaskHostNodeDTO());

            String json = original.toJsonString();

            assertThat(json).isNotEqualTo("null");
            assertThat(json).contains("\"variable\":\"hostListVar\"");
            // 与历史行为一致：hostNodeList 在 toJsonString 内部被清空
            assertThat(original.getHostNodeList()).isNull();
        }

        @Test
        @DisplayName("历史路径回归：variable 空 + 仅 hostNodeList=空对象 + containerList → 字节面与改前一致")
        void legacyEmptyHostNodeListWithContainerListByteFaceUnchanged() {
            // 模拟 fromVO 历史路径：hostNodeList 为空对象（lists 全 null），containerList 非空。
            // 注意：NON_EMPTY 对自定义 POJO 不递归判空，因此空 hostNodeList 仍会作为 {} 出现在 JSON
            // 字节面——这是历史落盘的真实形态，本次修复不能改变它。
            TaskTargetDTO original = new TaskTargetDTO();
            original.setHostNodeList(new TaskHostNodeDTO()); // 内部 list 全 null
            TaskTargetContainerDTO container = new TaskTargetContainerDTO();
            container.setId(555L);
            original.setContainerList(Collections.singletonList(container));

            String json = original.toJsonString();

            // 与改前字节面完全一致
            assertThat(json).isEqualTo("{\"hostNodeList\":{},\"containerList\":[{\"id\":555}]}");

            // 反序列化后行为与历史一致
            TaskTargetDTO back = TaskTargetDTO.fromJsonString(json);
            
            assertThat(back).isNotNull();
            assertThat(back.getHostNodeList()).isNotNull();
            assertThat(back.getContainerList()).hasSize(1);
        }
    }
}
