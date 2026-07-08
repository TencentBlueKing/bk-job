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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 回归测试：mock {@code step_instance.target_servers} / {@code task_*_variable.value} 列里
 * 历史持久化的 JSON，确认新代码 {@link com.tencent.bk.job.execute.dao.impl.StepInstanceDAOImpl}
 * 路径上的 {@code JsonUtils.fromJson(json, ExecuteTargetDTO.class)} 行为与现状一致。
 * <p>
 * 这是 propose / design D4 的兼容性硬约束：
 * <ul>
 *   <li>无 {@code containerFilters} 字段的老数据反序列化为 null，所有现有逻辑分支不受影响</li>
 *   <li>有 {@code containerFilters} 但无 {@code propConditions} 的老数据仍可反序列化，propConditions = null</li>
 *   <li>含 {@code propConditions} 的新数据反序列化完整，传到执行引擎时 hasContainerExecuteObject() = true</li>
 *   <li>含未知顶层字段的 JSON（依赖 FAIL_ON_UNKNOWN_PROPERTIES=disabled）不抛异常</li>
 * </ul>
 * 注：执行引擎对上游来源无感知，本测试只校验 DTO 形态契约，与具体写入入口解耦。
 */
@DisplayName("ExecuteTargetDTO: 老 / 新 target_servers JSON 反序列化回归")
class ExecuteTargetDTOJsonCompatibilityTest {

    @Nested
    @DisplayName("Legacy（无 containerFilters）")
    class Legacy {

        @Test
        @DisplayName("最老的 ipList-only JSON：staticIpList=null，ipList 兼容字段保留")
        void legacyIpListOnly() {
            // 来自 init_step_instance_data.sql 的真实历史形态
            String json = "{\"ipList\":[{\"cloudAreaId\":0,\"ip\":\"127.0.0.1\"}]}";

            ExecuteTargetDTO target = JsonUtils.fromJson(json, ExecuteTargetDTO.class);

            assertThat(target).isNotNull();
            assertThat(target.getIpList()).hasSize(1);
            assertThat(target.getIpList().get(0).getIp()).isEqualTo("127.0.0.1");
            assertThat(target.getStaticIpList()).isNull();
            // 老数据无容器字段：containerFilters 必须 null，避免误激活容器分支
            assertThat(target.getContainerFilters()).isNull();
            assertThat(target.getStaticContainerList()).isNull();
            assertThat(target.hasContainerExecuteObject()).isFalse();
        }

        @Test
        @DisplayName("含 staticIpList + topoNodes + dynamicServerGroups 的老 JSON")
        void legacyMultiSource() {
            String json = "{"
                + "\"staticIpList\":[{\"hostId\":101,\"ip\":\"10.0.0.1\"}],"
                + "\"topoNodes\":[{\"topoNodeId\":1001,\"nodeType\":\"module\"}],"
                + "\"dynamicServerGroups\":[{\"groupId\":\"dyngrp-1\"}]"
                + "}";

            ExecuteTargetDTO target = JsonUtils.fromJson(json, ExecuteTargetDTO.class);

            assertThat(target.getStaticIpList()).hasSize(1);
            assertThat(target.getStaticIpList().get(0).getHostId()).isEqualTo(101L);
            assertThat(target.getTopoNodes()).hasSize(1);
            assertThat(target.getDynamicServerGroups()).hasSize(1);
            assertThat(target.getContainerFilters()).isNull();
            assertThat(target.hasContainerExecuteObject()).isFalse();
        }

        @Test
        @DisplayName("含 staticContainerList 的老 JSON（support-container-magic-variable 已合入后场景）")
        void legacyStaticContainer() {
            String json = "{"
                + "\"staticContainerList\":[{\"id\":555,\"containerId\":\"docker://abc\",\"name\":\"app\"}]"
                + "}";

            ExecuteTargetDTO target = JsonUtils.fromJson(json, ExecuteTargetDTO.class);

            assertThat(target.getStaticContainerList()).hasSize(1);
            assertThat(target.getStaticContainerList().get(0).getId()).isEqualTo(555L);
            // 老数据无 containerFilters → null，hasContainerExecuteObject 由 staticContainerList 决定
            assertThat(target.getContainerFilters()).isNull();
            assertThat(target.hasContainerExecuteObject()).isTrue();
        }

        @Test
        @DisplayName("含 containerFilters 但无 propConditions 的老 JSON：propConditions = null，不误激活新解析分支")
        void containerFiltersWithoutPropConditions() {
            String json = "{"
                + "\"containerFilters\":[{"
                + "\"clusterFilter\":{\"clusterUIDs\":[\"BCS-K8S-00001\"]},"
                + "\"namespaceFilter\":{\"namespaces\":[\"default\"]},"
                + "\"workloadFilter\":{\"kind\":\"deployment\",\"workloadNames\":[\"backend-app\"]}"
                + "}]"
                + "}";

            ExecuteTargetDTO target = JsonUtils.fromJson(json, ExecuteTargetDTO.class);

            assertThat(target.getContainerFilters()).hasSize(1);
            KubeContainerFilter cf = target.getContainerFilters().get(0);
            assertThat(cf.getClusterFilter().getClusterUIDs()).containsExactly("BCS-K8S-00001");
            // 关键回归：老数据 propConditions 必须为 null，避免触发新解析分支
            assertThat(cf.getPropConditions()).isNull();
            assertThat(cf.hasPropConditions()).isFalse();
            assertThat(target.hasContainerExecuteObject()).isTrue();
        }

        @Test
        @DisplayName("含未知顶层字段的 JSON：FAIL_ON_UNKNOWN_PROPERTIES=disabled 保证不抛")
        void unknownFieldsTolerated() {
            String json = "{"
                + "\"ipList\":[{\"cloudAreaId\":0,\"ip\":\"127.0.0.1\"}],"
                + "\"unknownFromFutureService\":{\"foo\":\"bar\"}"
                + "}";

            assertThatCode(() -> {
                ExecuteTargetDTO target = JsonUtils.fromJson(json, ExecuteTargetDTO.class);
                assertThat(target.getIpList()).hasSize(1);
                assertThat(target.getContainerFilters()).isNull();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("新功能：含 propConditions 的 JSON")
    class NewFeature {

        @Test
        @DisplayName("新 JSON：Web 入口写入的 clusterNodes/propConditions 完整反序列化，传执行引擎链路")
        void newPropConditionsLoad() {
            String json = "{"
                + "\"containerFilters\":[{"
                + "\"clusterNodes\":[{\"id\":100,\"name\":\"公共集群\"}],"
                + "\"namespaceNodes\":[{\"id\":10000,\"name\":\"命名空间10000\"}],"
                + "\"propConditions\":["
                + "  {\"field\":\"container_container_uid\",\"operator\":\"equal\",\"value\":\"docker://nginx\"},"
                + "  {\"field\":\"pod_name\",\"operator\":\"equal\",\"value\":\"pod-a\"}"
                + "]"
                + "}]"
                + "}";

            ExecuteTargetDTO target = JsonUtils.fromJson(json, ExecuteTargetDTO.class);

            assertThat(target.getContainerFilters()).hasSize(1);
            KubeContainerFilter cf = target.getContainerFilters().get(0);
            assertThat(cf.hasPropConditions()).isTrue();
            assertThat(cf.hasKubeTopoObjects()).isTrue();
            assertThat(cf.getClusterNodes().get(0).getId()).isEqualTo(100L);
            assertThat(cf.getPropConditions()).hasSize(2);
            assertThat(cf.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
            assertThat(cf.getPropConditions().get(0).getValue()).isEqualTo("docker://nginx");
            assertThat(cf.getPropConditions().get(1).getValue()).isEqualTo("pod-a");

            // 模拟「DAO 读取 → 设进 stepInstance.targetExecuteObjects → 进入执行引擎」的链路
            StepInstanceDTO step = new StepInstanceDTO();
            step.setTargetExecuteObjects(target);
            assertThat(step.getTargetExecuteObjects().hasContainerExecuteObject()).isTrue();
            assertThat(step.getTargetExecuteObjects().getContainerFilters().get(0).hasPropConditions()).isTrue();
        }

        @Test
        @DisplayName("Round-trip：新数据（Web 入口形态）写入 → 反序列化 → 字段保真")
        void roundTripPreservesFields() {
            ExecuteTargetDTO original = new ExecuteTargetDTO();
            original.setStaticIpList(Collections.singletonList(new HostDTO(100L)));

            KubeContainerFilter cf = new KubeContainerFilter();
            cf.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(100L)));
            cf.setPropConditions(Collections.singletonList(
                new KubePropCondition("pod_name", "equal", "pod-a")));
            original.setContainerFilters(Collections.singletonList(cf));

            ExecuteTargetDTO back = JsonUtils.fromJson(JsonUtils.toJson(original), ExecuteTargetDTO.class);

            assertThat(back.getStaticIpList().get(0).getHostId()).isEqualTo(100L);
            assertThat(back.getContainerFilters()).hasSize(1);
            assertThat(back.getContainerFilters().get(0).getClusterNodes()).hasSize(1);
            assertThat(back.getContainerFilters().get(0).getClusterNodes().get(0).getId())
                .isEqualTo(100L);
            assertThat(back.getContainerFilters().get(0).getPropConditions().get(0).getValue())
                .isEqualTo("pod-a");
        }
    }
}
