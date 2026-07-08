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
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.vo.TaskExecuteObjectsInfoVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import com.tencent.bk.job.common.model.vo.WebKubeClusterObject;
import com.tencent.bk.job.common.model.vo.WebKubeNamespaceObject;
import com.tencent.bk.job.common.util.converter.WebContainerConditionFilterConverter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 端到端伪集成测试：跨真实的 Bean Validation、Converter、DTO 序列化器、DAO 反序列化路径，
 * 验证「**老数据兼容**」与「**新特性容器动态条件**」两条核心契约在完整链路上都不破。
 * <p>
 * 模拟的真实链路（每个用例至少穿越其中 3 个阶段）：
 * <pre>
 * 前端请求体 (Web JSON)
 *   → Jackson 反序列化为 TaskTargetVO
 *   → Bean Validation (jakarta.validation)
 *   → ExecuteTargetDTO.fromTaskTargetVO()      // Web → 内部
 *   → JsonUtils.toJson()                       // 模拟 DAO 入库到 step_instance.target_servers
 *   → JsonUtils.fromJson(..., ExecuteTargetDTO.class)   // 模拟 DAO 出库
 *   → 下游可观测断言 (hasContainerExecuteObject / hasPropConditions / 业务字段)
 * </pre>
 * 与同目录下其它测试的分工：
 * <ul>
 *   <li>{@link ExecuteTargetDTOJsonCompatibilityTest}：仅覆盖「DAO 出库」一段，断言反序列化保真度</li>
 *   <li>{@code WebContainerConditionFilterConverterTest}：仅覆盖
 *       「Web → 内部」一段的字段映射 / 防御性拷贝</li>
 *   <li>{@code WebContainerConditionFilterBeanValidationTest}：仅覆盖
 *       「Bean Validation」一段</li>
 *   <li>本测试：把以上几段串起来跑，确保**生产链路在端到端上**仍兼容老数据，也能正确承载新特性</li>
 * </ul>
 */
@DisplayName("ExecuteTarget 端到端：Web → Validate → Convert → 入库 → 出库 → 下游消费")
class ExecuteTargetEndToEndCompatibilityTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Nested
    @DisplayName("老数据兼容：本特性发布前已存在的 Web 请求 / DB JSON 仍能正常工作")
    class LegacyCompatibility {

        @Test
        @DisplayName("L1 — Web 老格式（仅 hostList）端到端：Validate 通过，DTO 出入库后下游不误激活容器路径")
        void legacyHostOnlyRequestEndToEnd() {
            // 模拟前端真实 JSON 请求体（不含 containerFilterList，等价于本特性发布前所有用户的请求形态）
            String webJson = "{"
                + "\"executeObjectsInfo\":{"
                + "  \"hostList\":[{\"hostId\":101,\"cloudAreaId\":0,\"ip\":\"10.0.0.1\"}]"
                + "}"
                + "}";

            TaskTargetVO web = JsonUtils.fromJson(webJson, TaskTargetVO.class);
            // 这里不对 TaskTargetVO 跑 Bean Validation —— 老路径上类级注解就一直没在外层校验过，
            // 关键是 containerFilterList 字段如果传了才走 Validation；不传时不应触发任何 violation
            assertThat(web.getExecuteObjectsInfo().getContainerFilterList()).isNull();

            // Web → 内部 DTO
            ExecuteTargetDTO target = ExecuteTargetDTO.fromTaskTargetVO(web);
            assertThat(target.getStaticIpList()).hasSize(1);
            assertThat(target.getContainerFilters()).isNull();

            // 模拟 DAO 入库到 step_instance.target_servers，再出库
            String dbJson = JsonUtils.toJson(target);
            // 入库 JSON 字节面不含新字段：不污染老数据列
            assertThat(dbJson).doesNotContain("containerFilters");

            ExecuteTargetDTO reloaded = JsonUtils.fromJson(dbJson, ExecuteTargetDTO.class);
            // 下游可观测断言：hasContainerExecuteObject 是执行引擎判断分支的真实入口
            assertThat(reloaded.hasContainerExecuteObject()).isFalse();
            assertThat(reloaded.getStaticIpList()).hasSize(1);
            assertThat(reloaded.getStaticIpList().get(0).getHostId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("L2 — DB 历史 JSON（init_step_instance_data.sql 形态）出库后下游可用")
        void legacyDbJsonStillFlowsToDownstream() {
            // 来自 init_step_instance_data.sql 的真实历史持久化形态：仅 ipList
            String dbJson = "{\"ipList\":[{\"cloudAreaId\":0,\"ip\":\"127.0.0.1\"}]}";

            ExecuteTargetDTO target = JsonUtils.fromJson(dbJson, ExecuteTargetDTO.class);
            assertThat(target.getIpList()).hasSize(1);
            assertThat(target.hasContainerExecuteObject()).isFalse();
            // 把 reloaded DTO 塞进 StepInstance —— 这是 StepInstanceDAOImpl 真实做的事
            StepInstanceDTO step = new StepInstanceDTO();
            step.setTargetExecuteObjects(target);
            assertThat(step.getTargetExecuteObjects().hasContainerExecuteObject()).isFalse();
        }

        @Test
        @DisplayName("L3 — DB 历史 JSON（含 containerFilters 但无 propConditions）端到端")
        void legacyContainerFiltersWithoutPropConditions() {
            // 引入 propConditions 字段之前的存量数据形态，执行引擎不关心由哪个上游入口写入
            String dbJson = "{"
                + "\"containerFilters\":[{"
                + "  \"clusterFilter\":{\"clusterUIDs\":[\"BCS-K8S-00001\"]},"
                + "  \"namespaceFilter\":{\"namespaces\":[\"default\"]}"
                + "}]"
                + "}";

            ExecuteTargetDTO target = JsonUtils.fromJson(dbJson, ExecuteTargetDTO.class);
            // 关键：老数据 propConditions 必须 null —— 不能误触发新解析分支
            KubeContainerFilter cf = target.getContainerFilters().get(0);
            assertThat(cf.hasPropConditions()).isFalse();
            assertThat(cf.getPropConditions()).isNull();
            assertThat(target.hasContainerExecuteObject()).isTrue();
        }

        @Test
        @DisplayName("L4 — Web 老格式：variable（变量入口）端到端，回显形态保留")
        void legacyVariableEntryEndToEnd() {
            // 老的「执行目标=变量」形态：variable=变量名，executeObjectsInfo.hostList=空
            String webJson = "{"
                + "\"variable\":\"hostListVar\","
                + "\"executeObjectsInfo\":{}"
                + "}";

            TaskTargetVO web = JsonUtils.fromJson(webJson, TaskTargetVO.class);
            ExecuteTargetDTO target = ExecuteTargetDTO.fromTaskTargetVO(web);
            assertThat(target.getVariable()).isEqualTo("hostListVar");
            assertThat(target.getContainerFilters()).isNull();

            // 入库 → 出库 → 反向回显
            ExecuteTargetDTO reloaded = JsonUtils.fromJson(JsonUtils.toJson(target), ExecuteTargetDTO.class);
            TaskTargetVO echoed = reloaded.convertToTaskTargetVO();
            assertThat(echoed.getVariable()).isEqualTo("hostListVar");
            assertThat(echoed.getExecuteObjectsInfo().getContainerFilterList()).isNull();
        }
    }

    @Nested
    @DisplayName("新特性：容器动态条件 端到端")
    class NewFeature {

        @Test
        @DisplayName("F1 — Web 新格式（clusterList/namespaceList + propConditions）端到端：Validate→Convert→入库→出库→下游可用")
        void containerConditionsEndToEnd() {
            // 模拟前端真实 JSON 请求体：拓扑节点 id+name 直接来自 /topology/container
            String webJson = "{"
                + "\"executeObjectsInfo\":{"
                + "  \"containerFilterList\":[{"
                + "    \"clusterList\":[{\"id\":1000,\"name\":\"集群1000\"}],"
                + "    \"namespaceList\":[{\"id\":10000,\"name\":\"命名空间10000\"}],"
                + "    \"propConditions\":["
                + "      {\"field\":\"container_container_uid\",\"operator\":\"contains\",\"value\":\"docker://nginx\"},"
                + "      {\"field\":\"pod_name\",\"operator\":\"equal\",\"value\":\"pod-a\"}"
                + "    ]"
                + "  }]"
                + "}"
                + "}";

            TaskTargetVO web = JsonUtils.fromJson(webJson, TaskTargetVO.class);

            // 1. 真实 Bean Validation 跑一次：clusterList 非空 + 元素字段非空，必须无 violation
            Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(
                web.getExecuteObjectsInfo().getContainerFilterList().get(0));
            assertThat(violations).isEmpty();

            // 2. Web → 内部
            ExecuteTargetDTO target = ExecuteTargetDTO.fromTaskTargetVO(web);
            assertThat(target.getContainerFilters()).hasSize(1);
            assertThat(target.getContainerFilters().get(0).hasPropConditions()).isTrue();
            assertThat(target.getContainerFilters().get(0).hasKubeTopoObjects()).isTrue();

            // 3. 入库：ns/cluster 仅写入 id，name 不落库
            String dbJson = JsonUtils.toJson(target);
            assertThat(dbJson).contains("\"containerFilters\"")
                .contains("\"clusterNodes\"")
                .doesNotContain("\"集癀1000\"")  // name 已从 DTO 移除，不落库
                .contains("\"propConditions\"")
                .contains("\"container_container_uid\"")
                .contains("\"pod_name\"");

            // 4. 出库（DAO 视角）→ 下游消费
            ExecuteTargetDTO reloaded = JsonUtils.fromJson(dbJson, ExecuteTargetDTO.class);
            assertThat(reloaded.hasContainerExecuteObject()).isTrue();
            KubeContainerFilter cf = reloaded.getContainerFilters().get(0);
            assertThat(cf.getClusterNodes()).hasSize(1);
            assertThat(cf.getClusterNodes().get(0).getId()).isEqualTo(1000L);
            assertThat(cf.hasPropConditions()).isTrue();
            assertThat(cf.getPropConditions()).hasSize(2);
            assertThat(cf.getPropConditions().get(0).getValue()).isEqualTo("docker://nginx");
            assertThat(cf.getPropConditions().get(1).getValue()).isEqualTo("pod-a");

            // 5. 反向回显：前端详情页（name 不再由后台回显，由前端携带或运行时查询）
            TaskTargetVO echoed = reloaded.convertToTaskTargetVO();
            List<WebContainerConditionFilter> echoedFilters =
                echoed.getExecuteObjectsInfo().getContainerFilterList();
            assertThat(echoedFilters).hasSize(1);
            assertThat(echoedFilters.get(0).getClusterList()).hasSize(1);
            assertThat(echoedFilters.get(0).getClusterList().get(0).getId()).isEqualTo(1000L);
            assertThat(echoedFilters.get(0).getClusterList().get(0).getName()).isNull();
            assertThat(echoedFilters.get(0).getPropConditions()).hasSize(2);
        }

        @Test
        @DisplayName("F2 — 静态 IP + 动态容器条件混合：两条数据源独立入库出库后都能被识别")
        void staticHostsCoexistWithContainerConditions() {
            ExecuteTargetDTO target = new ExecuteTargetDTO();
            target.setStaticIpList(Collections.singletonList(new HostDTO(100L)));
            target.setContainerFilters(Collections.singletonList(buildFilter()));

            ExecuteTargetDTO reloaded = JsonUtils.fromJson(JsonUtils.toJson(target), ExecuteTargetDTO.class);

            assertThat(reloaded.hasContainerExecuteObject()).isTrue();
            assertThat(reloaded.getStaticIpList()).hasSize(1);
            assertThat(reloaded.getContainerFilters().get(0).hasPropConditions()).isTrue();
            assertThat(reloaded.getContainerFilters().get(0).hasKubeTopoObjects()).isTrue();
        }

        @Test
        @DisplayName("F3 — Web Bean Validation 反例：clusterList 缺失被拦截，不流入下游")
        void invalidWebRequestRejectedByValidation() {
            String webJson = "{"
                + "\"executeObjectsInfo\":{"
                + "  \"containerFilterList\":[{"
                + "    \"propConditions\":["
                + "      {\"field\":\"pod_name\",\"operator\":\"equal\",\"value\":\"pod-a\"}"
                + "    ]"
                + "  }]"
                + "}"
                + "}";

            TaskTargetVO web = JsonUtils.fromJson(webJson, TaskTargetVO.class);
            Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(
                web.getExecuteObjectsInfo().getContainerFilterList().get(0));

            assertThat(violations).isNotEmpty();
            assertThat(violations).anySatisfy(v ->
                assertThat(v.getPropertyPath().toString()).isEqualTo("clusterList"));
            // 注意：本测试只断言被识别出违反，不断言生产路径会自动拒绝：
            //   实际拒绝点在 @Valid 修饰的 controller 入参上，那由 Spring MVC 框架兜底，不在本类目标内。
        }
    }

    @Nested
    @DisplayName("Round-trip 与 Web 多次回显")
    class RoundTrip {

        @Test
        @DisplayName("R1 — Web → DTO → JSON → DTO → Web 多次回显字段保真，集合数 / 关键字段不退化")
        void multiHopRoundTrip() {
            // 前端首次提交
            TaskTargetVO web1 = new TaskTargetVO();
            TaskExecuteObjectsInfoVO info = new TaskExecuteObjectsInfoVO();
            info.setContainerFilterList(Collections.singletonList(buildWebFilter()));
            web1.setExecuteObjectsInfo(info);

            // 第一次入库出库
            ExecuteTargetDTO dto1 = ExecuteTargetDTO.fromTaskTargetVO(web1);
            ExecuteTargetDTO dto2 = JsonUtils.fromJson(JsonUtils.toJson(dto1), ExecuteTargetDTO.class);

            // 用户重新打开作业 → 前端拉详情 → 再次提交
            TaskTargetVO web2 = dto2.convertToTaskTargetVO();
            ExecuteTargetDTO dto3 = ExecuteTargetDTO.fromTaskTargetVO(web2);
            ExecuteTargetDTO dto4 = JsonUtils.fromJson(JsonUtils.toJson(dto3), ExecuteTargetDTO.class);

            // 经过 4 跳后：id / propConditions 保真（name 已从 DTO 移除）
            assertThat(dto4.getContainerFilters()).hasSize(1);
            KubeContainerFilter cf = dto4.getContainerFilters().get(0);
            assertThat(cf.getClusterNodes()).hasSize(1);
            assertThat(cf.getClusterNodes().get(0).getId()).isEqualTo(1000L);
            assertThat(cf.getNamespaceNodes()).hasSize(1);
            assertThat(cf.getNamespaceNodes().get(0).getId()).isEqualTo(10000L);
            assertThat(cf.getPropConditions()).hasSize(2);
            assertThat(cf.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
            assertThat(cf.getPropConditions().get(1).getValue()).isEqualTo("pod-a");
            // emptyFilter / fetchAnyOneContainer 这两个内部开关在 Web 链路上始终为 false
            assertThat(cf.isEmptyFilter()).isFalse();
            assertThat(cf.isFetchAnyOneContainer()).isFalse();
        }

        @Test
        @DisplayName("R2 — 老 Web 请求 4 跳后仍不会引入 containerFilters 字段（不污染老数据 JSON 字节面）")
        void legacyRequestStaysClean() {
            TaskTargetVO web1 = new TaskTargetVO();
            TaskExecuteObjectsInfoVO info = new TaskExecuteObjectsInfoVO();
            web1.setExecuteObjectsInfo(info);

            ExecuteTargetDTO dto1 = ExecuteTargetDTO.fromTaskTargetVO(web1);
            String json1 = JsonUtils.toJson(dto1);
            ExecuteTargetDTO dto2 = JsonUtils.fromJson(json1, ExecuteTargetDTO.class);
            TaskTargetVO web2 = dto2.convertToTaskTargetVO();
            ExecuteTargetDTO dto3 = ExecuteTargetDTO.fromTaskTargetVO(web2);
            String json3 = JsonUtils.toJson(dto3);

            // 多跳后入库 JSON 仍不出现新字段
            assertThatCode(() -> {
                assertThat(json1).doesNotContain("containerFilters");
                assertThat(json3).doesNotContain("containerFilters");
            }).doesNotThrowAnyException();
        }
    }

    private static KubeContainerFilter buildFilter() {
        // 借用 Web Converter 的入口构造一个完整的内部 filter，避免本类用 setter 重写一遍构造
        return WebContainerConditionFilterConverter.toKubeContainerFilter(buildWebFilter());
    }

    private static WebContainerConditionFilter buildWebFilter() {
        WebContainerConditionFilter web = new WebContainerConditionFilter();
        WebKubeClusterObject cluster = new WebKubeClusterObject();
        cluster.setId(1000L);
        cluster.setName("集群1000");
        web.setClusterList(Collections.singletonList(cluster));
        WebKubeNamespaceObject ns = new WebKubeNamespaceObject();
        ns.setId(10000L);
        ns.setName("命名空间10000");
        web.setNamespaceList(Collections.singletonList(ns));
        web.setPropConditions(Arrays.asList(
            new KubePropCondition("container_container_uid", "contains", "docker://nginx"),
            new KubePropCondition("pod_name", "equal", "pod-a")
        ));
        return web;
    }
}
