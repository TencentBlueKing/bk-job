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

package com.tencent.bk.job.common.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ResolvedSummary 既随 v4 开放接口的 dry_run_summary 返回，又原样落库做审批单据快照，
 * 字段名一旦回退成驼峰就是接口不兼容变更，因此这里逐字段守住下划线命名
 */
@DisplayName("ResolvedSummary JSON字段命名测试")
class ResolvedSummaryJsonNamingTest {

    @Test
    @DisplayName("所有字段均序列化为下划线命名")
    void allFieldsAreSerializedInSnakeCase() {
        JsonNode root = JsonUtils.toJsonNode(JsonUtils.toJson(buildFullSummary()));

        assertSnakeCaseFieldNames(ResolvedSummary.class, root);
        assertSnakeCaseFieldNames(ResolvedSummary.ResolvedField.class, root.get("fields").get(0));
        assertSnakeCaseFieldNames(ResolvedSummary.ResolvedStep.class, root.get("steps").get(0));
        assertSnakeCaseFieldNames(ResolvedSummary.ResolvedExecuteObject.class,
            root.get("steps").get(0).get("execute_objects").get(0));
        assertSnakeCaseFieldNames(ResolvedSummary.ResolvedFileSource.class,
            root.get("steps").get(0).get("file_sources").get(0));
        assertSnakeCaseFieldNames(ResolvedSummary.ResolvedGlobalVar.class, root.get("global_vars").get(0));
    }

    @Test
    @DisplayName("下划线JSON可完整反序列化回来")
    void snakeCaseJsonCanBeDeserialized() {
        ResolvedSummary summary = buildFullSummary();

        ResolvedSummary parsed = JsonUtils.fromJson(JsonUtils.toJson(summary), ResolvedSummary.class);

        assertThat(parsed).isEqualTo(summary);
    }

    @Test
    @DisplayName("历史驼峰快照读回不抛异常，字段退化为空")
    void legacyCamelCaseJsonDegradesWithoutError() {
        String legacyJson = "{\"operationType\":\"FAST_TRANSFER_FILE\",\"name\":\"文件分发\","
            + "\"totalExecuteObjectCount\":2,\"containsDynamicTarget\":true,"
            + "\"steps\":[{\"executeType\":\"SEND_FILE\",\"accountAlias\":\"root\"}]}";

        assertThatCode(() -> JsonUtils.fromJson(legacyJson, ResolvedSummary.class)).doesNotThrowAnyException();

        ResolvedSummary parsed = JsonUtils.fromJson(legacyJson, ResolvedSummary.class);
        assertThat(parsed.getName()).isEqualTo("文件分发");
        assertThat(parsed.getOperationType()).isNull();
        assertThat(parsed.getTotalExecuteObjectCount()).isNull();
        assertThat(parsed.getContainsDynamicTarget()).isNull();
        assertThat(parsed.getSteps()).hasSize(1);
        assertThat(parsed.getSteps().get(0).getExecuteType()).isNull();
        assertThat(parsed.getSteps().get(0).getAccountAlias()).isNull();
    }

    /**
     * 按反射逐个字段比对，新增字段忘记标注 {@code @JsonProperty} 时同样会失败
     */
    private void assertSnakeCaseFieldNames(Class<?> clazz, JsonNode node) {
        List<String> expectedNames = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            String expectedName = toSnakeCase(field.getName());
            assertThat(node.has(expectedName))
                .as("%s.%s 应序列化为 %s", clazz.getSimpleName(), field.getName(), expectedName)
                .isTrue();
            expectedNames.add(expectedName);
        }
        assertThat(jsonFieldNames(node))
            .as("%s 不应出现下划线命名之外的字段", clazz.getSimpleName())
            .containsExactlyInAnyOrderElementsOf(expectedNames);
    }

    private List<String> jsonFieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            names.add(iterator.next());
        }
        return names;
    }

    private String toSnakeCase(String fieldName) {
        StringBuilder sb = new StringBuilder();
        for (char c : fieldName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private ResolvedSummary buildFullSummary() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType("FAST_TRANSFER_FILE");
        summary.setName("快速分发文件");
        summary.setTotalExecuteObjectCount(2);
        summary.setContainsDynamicTarget(true);
        summary.setDangerousRuleMatched(false);
        summary.addField("执行方案ID", "1000");
        summary.addDefaultApplied("传输模式", "强制模式，会覆盖同名文件");

        ResolvedSummary.ResolvedStep step = new ResolvedSummary.ResolvedStep();
        step.setName("分发文件");
        step.setExecuteType("SEND_FILE");
        step.setAccountAlias("root");
        step.setHighRiskAccount(true);
        step.setScriptName("check.sh");
        step.setScriptVersionId(100L);
        step.setScriptSource(2);
        step.setDangerousCheckSummary("命中高危规则：rm -rf");
        step.setExecuteObjectCount(2);
        step.setExecuteObjectTruncated(false);
        step.setContainsDynamicTarget(true);
        step.setExecuteObjects(Collections.singletonList(
            new ResolvedSummary.ResolvedExecuteObject("HOST", 1L, "0:127.0.0.1")));
        step.setScriptParam("--env=prod");
        step.setParamSensitive(false);
        step.addField("目标路径", "/tmp/");

        ResolvedSummary.ResolvedFileSource fileSource = new ResolvedSummary.ResolvedFileSource();
        fileSource.setAccountAlias("root");
        fileSource.setLocalUpload(false);
        fileSource.addHost(1L, "0:127.0.0.1");
        fileSource.addFilePath("/data/a.tar.gz");
        step.addFileSource(fileSource);
        summary.addStep(step);

        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        globalVar.setName("target_hosts");
        globalVar.setType("EXECUTE_OBJECT_LIST");
        globalVar.setValue("v1.2.3");
        globalVar.setAssigned(true);
        globalVar.addHost(1L, "0:127.0.0.1");
        globalVar.setDynamicGroupCount(1);
        globalVar.setTopoNodeCount(2);
        globalVar.setContainerCount(3);
        summary.addGlobalVar(globalVar);

        return summary;
    }
}
