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

package com.tencent.bk.job.common.model.dto;

import com.tencent.bk.job.common.util.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 回归测试：DB 中以 JSON 形式持久化的 {@link KubeContainerFilter} 在新代码中仍可正确反序列化。
 * <p>
 * 这是 propose 文档要求的兼容性硬约束：老数据无 {@code propConditions} 字段，反序列化为 null；
 * 多版本并存期老代码读到含新字段的 JSON 不报错（依赖全局 {@code FAIL_ON_UNKNOWN_PROPERTIES=disabled}）。
 */
@DisplayName("KubeContainerFilter: 历史 / 新版本 JSON 互兼容")
class KubeContainerFilterJsonCompatibilityTest {

    @Nested
    @DisplayName("反序列化：历史 JSON → 新 DTO")
    class DeserializeLegacy {

        @Test
        @DisplayName("仅含拓扑字段、无 propConditions 的老 JSON 反序列化：propConditions = null，其它字段保留")
        void legacyTopologyJsonStillLoads() {
            String legacyJson = "{"
                + "\"clusterFilter\":{\"clusterUIDs\":[\"BCS-K8S-00001\"]},"
                + "\"namespaceFilter\":{\"namespaces\":[\"default\"]},"
                + "\"workloadFilter\":{\"kind\":\"deployment\",\"workloadNames\":[\"backend-app\"]},"
                + "\"emptyFilter\":false,"
                + "\"fetchAnyOneContainer\":false"
                + "}";

            KubeContainerFilter filter = JsonUtils.fromJson(legacyJson, KubeContainerFilter.class);

            assertThat(filter).isNotNull();
            assertThat(filter.getClusterFilter().getClusterUIDs()).containsExactly("BCS-K8S-00001");
            assertThat(filter.getNamespaceFilter().getNamespaces()).containsExactly("default");
            assertThat(filter.getWorkloadFilter().getKind()).isEqualTo("deployment");
            assertThat(filter.getWorkloadFilter().getWorkloadNames()).containsExactly("backend-app");
            assertThat(filter.isEmptyFilter()).isFalse();
            assertThat(filter.isFetchAnyOneContainer()).isFalse();
            // 关键回归：旧数据无 propConditions 字段，必须为 null，不能误造空集合
            assertThat(filter.getPropConditions()).isNull();
            assertThat(filter.hasPropConditions()).isFalse();
        }

        @Test
        @DisplayName("含 propConditions 的新版本 JSON 反序列化完整保真")
        void newPropConditionsJsonLoads() {
            String newJson = "{"
                + "\"clusterFilter\":{\"clusterUIDs\":[\"BCS-K8S-00001\"]},"
                + "\"propConditions\":["
                + "  {\"field\":\"container_container_uid\",\"operator\":\"contains\",\"value\":\"docker://nginx\"},"
                + "  {\"field\":\"pod_name\",\"operator\":\"equal\",\"value\":\"pod-a\"},"
                + "  {\"field\":\"container_id\",\"operator\":\"equal\",\"value\":12345}"
                + "]"
                + "}";

            KubeContainerFilter filter = JsonUtils.fromJson(newJson, KubeContainerFilter.class);

            assertThat(filter.getPropConditions()).hasSize(3);
            assertThat(filter.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
            assertThat(filter.getPropConditions().get(0).getOperator()).isEqualTo("contains");
            assertThat(filter.getPropConditions().get(0).getValue()).isEqualTo("docker://nginx");
            assertThat(filter.getPropConditions().get(1).getValue()).isEqualTo("pod-a");
            assertThat(filter.getPropConditions().get(2).getValue()).isEqualTo(12345);
        }

        @Test
        @DisplayName("含未知顶层字段的 JSON：依赖全局 FAIL_ON_UNKNOWN_PROPERTIES=disabled，反序列化不抛异常")
        void unknownFieldsTolerated() {
            String jsonWithUnknown = "{"
                + "\"clusterFilter\":{\"clusterUIDs\":[\"BCS-K8S-00001\"]},"
                + "\"someFutureField\":{\"foo\":\"bar\"},"
                + "\"anotherFutureField\":42"
                + "}";

            assertThatCode(() -> {
                KubeContainerFilter filter = JsonUtils.fromJson(jsonWithUnknown, KubeContainerFilter.class);
                assertThat(filter.getClusterFilter().getClusterUIDs()).containsExactly("BCS-K8S-00001");
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("序列化：新 DTO → JSON")
    class SerializeNew {

        @Test
        @DisplayName("propConditions 为 null/空时不出现在 JSON（NON_EMPTY），保持与老数据一致的字节面")
        void emptyPropConditionsOmitted() {
            KubeContainerFilter filter = new KubeContainerFilter();
            KubeClusterFilter cluster = new KubeClusterFilter();
            cluster.setClusterUIDs(Collections.singletonList("BCS-K8S-00001"));
            filter.setClusterFilter(cluster);

            String jsonNull = JsonUtils.toJson(filter);
            assertThat(jsonNull).doesNotContain("propConditions");

            filter.setPropConditions(Collections.emptyList());
            String jsonEmpty = JsonUtils.toJson(filter);
            assertThat(jsonEmpty).doesNotContain("propConditions");
        }

        @Test
        @DisplayName("propConditions 非空时正常输出")
        void propConditionsRendered() {
            KubeContainerFilter filter = new KubeContainerFilter();
            KubeClusterFilter cluster = new KubeClusterFilter();
            cluster.setClusterUIDs(Collections.singletonList("BCS-K8S-00001"));
            filter.setClusterFilter(cluster);
            filter.setPropConditions(Collections.singletonList(
                new KubePropCondition("pod_name", "equal", "pod-a")));

            String json = JsonUtils.toJson(filter);
            assertThat(json).contains("\"propConditions\"")
                .contains("\"field\":\"pod_name\"")
                .contains("\"operator\":\"equal\"")
                .contains("\"value\":\"pod-a\"");
        }

        @Test
        @DisplayName("Round-trip：序列化 → 反序列化 → 字段全保真")
        void roundTripPreservesFields() {
            KubeContainerFilter original = new KubeContainerFilter();
            KubeClusterFilter cluster = new KubeClusterFilter();
            cluster.setClusterUIDs(Arrays.asList("BCS-K8S-00001", "BCS-K8S-00002"));
            original.setClusterFilter(cluster);
            original.setPropConditions(Arrays.asList(
                new KubePropCondition("container_container_uid", "contains", "docker://nginx"),
                new KubePropCondition("pod_id", "equal", 100)
            ));

            KubeContainerFilter back = JsonUtils.fromJson(JsonUtils.toJson(original), KubeContainerFilter.class);

            assertThat(back.getClusterFilter().getClusterUIDs())
                .containsExactly("BCS-K8S-00001", "BCS-K8S-00002");
            assertThat(back.getPropConditions()).hasSize(2);
            assertThat(back.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
            assertThat(back.getPropConditions().get(0).getValue()).isEqualTo("docker://nginx");
            assertThat(back.getPropConditions().get(1).getField()).isEqualTo("pod_id");
        }
    }

    @Nested
    @DisplayName("Web 入口形态：kubeTopoList 拓扑路径列表")
    class WebObjectForm {

        @Test
        @DisplayName("反序列化：Web 入口写入的 kubeTopoList JSON（对象内可能含已废弃 name）→ DTO 仅保留 id，name 被忽略")
        void webObjectFormDeserializes() {
            String json = "{"
                + "\"kubeTopoList\":["
                + "  {\"cluster\":{\"id\":1000,\"name\":\"集群1000\"},"
                + "   \"namespace\":{\"id\":10000},"
                + "   \"workload\":{\"kind\":\"deployment\",\"id\":20000}},"
                + "  {\"cluster\":{\"id\":1001},"
                + "   \"workload\":{\"kind\":\"daemonSet\",\"id\":20001}}"
                + "],"
                + "\"propConditions\":[{\"field\":\"pod_name\",\"operator\":\"equal\",\"value\":\"pod-a\"}]"
                + "}";

            KubeContainerFilter filter = JsonUtils.fromJson(json, KubeContainerFilter.class);

            assertThat(filter.hasKubeTopoObjects()).isTrue();
            assertThat(filter.getKubeTopoList()).hasSize(2);
            KubeTopoDTO topo0 = filter.getKubeTopoList().get(0);
            assertThat(topo0.getCluster().getId()).isEqualTo(1000L);
            assertThat(topo0.getNamespace().getId()).isEqualTo(10000L);
            assertThat(topo0.getWorkload().getKind()).isEqualTo("deployment");
            KubeTopoDTO topo1 = filter.getKubeTopoList().get(1);
            assertThat(topo1.getCluster().getId()).isEqualTo(1001L);
            assertThat(topo1.getNamespace()).isNull();
            assertThat(topo1.getWorkload().getKind()).isEqualTo("daemonSet");
            // v4 字段未被污染
            assertThat(filter.getClusterFilter()).isNull();
            assertThat(filter.getNamespaceFilter()).isNull();
            assertThat(filter.getWorkloadFilter()).isNull();
        }

        @Test
        @DisplayName("序列化：Web 入口 DTO → JSON，仅写出 id（name 已从 DTO 移除）")
        void webObjectFormSerializes() {
            KubeContainerFilter filter = new KubeContainerFilter();
            filter.setKubeTopoList(Collections.singletonList(new KubeTopoDTO(
                new KubeClusterObjectDTO(1000L),
                new KubeNamespaceObjectDTO(10000L),
                new KubeWorkloadObjectDTO("deployment", 20000L))));

            String json = JsonUtils.toJson(filter);
            assertThat(json).contains("\"kubeTopoList\"")
                .contains("\"cluster\"")
                .contains("\"id\":1000")
                .contains("\"kind\":\"deployment\"")
                .doesNotContain("\"name\"");
        }

        @Test
        @DisplayName("Round-trip：Web 入口 → JSON → DTO 字段保真")
        void webObjectFormRoundTrip() {
            KubeContainerFilter original = new KubeContainerFilter();
            original.setKubeTopoList(Arrays.asList(
                new KubeTopoDTO(new KubeClusterObjectDTO(1000L), null,
                    new KubeWorkloadObjectDTO("deployment", 20000L)),
                new KubeTopoDTO(new KubeClusterObjectDTO(1001L), null,
                    new KubeWorkloadObjectDTO("daemonSet", 20001L))
            ));

            KubeContainerFilter back = JsonUtils.fromJson(JsonUtils.toJson(original), KubeContainerFilter.class);

            assertThat(back.getKubeTopoList()).hasSize(2);
            assertThat(back.getKubeTopoList().get(0).getCluster().getId()).isEqualTo(1000L);
            assertThat(back.getKubeTopoList().get(0).getWorkload().getKind()).isEqualTo("deployment");
            assertThat(back.getKubeTopoList().get(0).getWorkload().getId()).isEqualTo(20000L);
            assertThat(back.getKubeTopoList().get(1).getCluster().getId()).isEqualTo(1001L);
            assertThat(back.getKubeTopoList().get(1).getWorkload().getKind()).isEqualTo("daemonSet");
        }
    }
}
