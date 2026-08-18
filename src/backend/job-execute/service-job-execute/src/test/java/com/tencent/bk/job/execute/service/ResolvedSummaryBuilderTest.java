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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批单据概要组装单测。
 * <p>
 * 概要区是审批人唯一能看清"到底要在哪些机器上执行什么"的信息来源，这里逐项锁定其内容：
 * 一旦解析后的目标机数、账号、脚本版本或高危命中丢失，审批就退化成盲签，而这种缺失在功能上
 * 不会报错、只会静默少显示，只能靠单测拦住。
 */
class ResolvedSummaryBuilderTest {

    @Test
    @DisplayName("脚本步骤概要带出解析后的目标机、高危账号与高危规则命中")
    void buildScriptStepSummary() {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("fast_script_step");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setAccountAlias("root");
        stepInstance.setScriptName("clean_disk.sh");
        stepInstance.setScriptVersionId(88L);
        stepInstance.setScriptSource(2);
        stepInstance.setTimeout(300);
        stepInstance.setDangerousCheckSummary("命中高危规则：rm -rf");
        stepInstance.setTargetExecuteObjects(buildResolvedTarget(101L, 102L));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("fast_script_task");
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        assertThat(summary.getName()).isEqualTo("fast_script_task");
        // 操作类型属审批域概念，由 job-analysis 侧填充，下游不得预设
        assertThat(summary.getOperationType()).isNull();
        assertThat(summary.getTotalExecuteObjectCount()).isEqualTo(2);
        assertThat(summary.getContainsDynamicTarget()).isFalse();
        assertThat(summary.getDangerousRuleMatched()).isTrue();

        assertThat(summary.getSteps()).hasSize(1);
        ResolvedSummary.ResolvedStep step = summary.getSteps().get(0);
        assertThat(step.getName()).isEqualTo("fast_script_step");
        assertThat(step.getExecuteType()).isEqualTo(StepExecuteTypeEnum.EXECUTE_SCRIPT.name());
        assertThat(step.getAccountAlias()).isEqualTo("root");
        assertThat(step.getHighRiskAccount()).isTrue();
        assertThat(step.getScriptName()).isEqualTo("clean_disk.sh");
        assertThat(step.getScriptVersionId()).isEqualTo(88L);
        assertThat(step.getScriptSource()).isEqualTo(2);
        assertThat(step.getDangerousCheckSummary()).isEqualTo("命中高危规则：rm -rf");
        assertThat(step.getExecuteObjectCount()).isEqualTo(2);
        assertThat(step.getExecuteObjectTruncated()).isFalse();
        assertThat(step.getExecuteObjects()).hasSize(2);
        assertThat(step.getExecuteObjects().get(0).getType()).isEqualTo("HOST");
        assertThat(step.getExecuteObjects().get(0).getId()).isEqualTo(101L);
        assertThat(step.getExecuteObjects().get(0).getDisplay()).isEqualTo("0:127.0.0.1");
        assertThat(fieldValue(step.getFields(), "timeout")).isEqualTo("300s");
    }

    @Test
    @DisplayName("文件分发步骤概要带出源文件、目标路径与传输模式")
    void buildFileStepSummary() {
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccountAlias("root");
        fileSource.setServers(buildResolvedTarget(201L));
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/data/a.tar.gz");
        fileSource.setFiles(Collections.singletonList(fileDetail));

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("fast_file_step");
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setAccountAlias("mysql");
        stepInstance.setTimeout(600);
        stepInstance.setFileTargetPath("/tmp/");
        stepInstance.setFileSourceList(Collections.singletonList(fileSource));
        stepInstance.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR.getValue());
        stepInstance.setTargetExecuteObjects(buildResolvedTarget(101L));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("fast_file_task");
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        ResolvedSummary.ResolvedStep step = summary.getSteps().get(0);
        assertThat(step.getHighRiskAccount()).isFalse();
        assertThat(fieldValue(step.getFields(), "file_target_path")).isEqualTo("/tmp/");
        assertThat(fieldValue(step.getFields(), "file_source_list")).isEqualTo("root@1 target(s) -> /data/a.tar.gz");
        // 强制模式会自动建目录并覆盖同名文件，后果远大于严格模式，必须在单据里说清
        assertThat(fieldValue(step.getFields(), "transfer_mode")).isEqualTo("FORCE");
        assertThat(summary.getDangerousRuleMatched()).isFalse();
    }

    @Test
    @DisplayName("多步骤概要按执行对象去重统计总台数，并带出执行方案 ID")
    void buildJobPlanSummary() {
        StepInstanceDTO firstStep = new StepInstanceDTO();
        firstStep.setName("step_1");
        firstStep.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        firstStep.setTargetExecuteObjects(buildResolvedTarget(101L, 102L));

        StepInstanceDTO secondStep = new StepInstanceDTO();
        secondStep.setName("step_2");
        secondStep.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        secondStep.setTargetExecuteObjects(buildResolvedTarget(102L, 103L));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("plan_task");
        taskInstance.setPlanId(1000L);
        taskInstance.setStepInstances(Arrays.asList(firstStep, secondStep));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        assertThat(fieldValue(summary.getFields(), "job_plan_id")).isEqualTo("1000");
        assertThat(summary.getSteps()).hasSize(2);
        // 两个步骤打同一批主机时，"将影响多少台机器"只能算一次，否则会把影响面夸大
        assertThat(summary.getTotalExecuteObjectCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("目标含动态分组时打上重新解析标记")
    void buildSummaryWithDynamicTarget() {
        ExecuteTargetDTO target = buildResolvedTarget(101L);
        target.setDynamicServerGroups(Collections.singletonList(new DynamicServerGroupDTO("group-1")));

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("step_1");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setTargetExecuteObjects(target);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        // 动态目标在放行时会重新解析，实际台数可能与预检不同，单据必须提示
        assertThat(summary.getContainsDynamicTarget()).isTrue();
        assertThat(summary.getSteps().get(0).getContainsDynamicTarget()).isTrue();
    }

    @Test
    @DisplayName("执行对象超过上限时截断列表但保留总数")
    void buildSummaryWithTruncatedExecuteObjects() {
        int totalCount = ResolvedSummary.MAX_EXECUTE_OBJECT_COUNT + 20;
        List<ExecuteObject> executeObjects = new ArrayList<>(totalCount);
        for (int i = 0; i < totalCount; i++) {
            executeObjects.add(new ExecuteObject(buildHost(1000L + i)));
        }
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setExecuteObjects(executeObjects);

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("step_1");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setTargetExecuteObjects(target);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        ResolvedSummary.ResolvedStep step = summary.getSteps().get(0);
        assertThat(step.getExecuteObjectCount()).isEqualTo(totalCount);
        assertThat(step.getExecuteObjects()).hasSize(ResolvedSummary.MAX_EXECUTE_OBJECT_COUNT);
        assertThat(step.getExecuteObjectTruncated()).isTrue();
        assertThat(summary.getTotalExecuteObjectCount()).isEqualTo(totalCount);
    }

    @Test
    @DisplayName("作业实例为空时返回空概要而非抛异常")
    void buildSummaryWithoutTaskInstance() {
        assertThat(ResolvedSummaryBuilder.build(null)).isNotNull();
        assertThat(ResolvedSummaryBuilder.build(new TaskInstanceDTO()).getSteps()).isNull();
    }

    private ExecuteTargetDTO buildResolvedTarget(Long... hostIds) {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        for (Long hostId : hostIds) {
            executeObjects.add(new ExecuteObject(buildHost(hostId)));
        }
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setExecuteObjects(executeObjects);
        return target;
    }

    private HostDTO buildHost(Long hostId) {
        HostDTO host = new HostDTO();
        host.setHostId(hostId);
        host.setBkCloudId(0L);
        host.setIp("127.0.0.1");
        return host;
    }

    private String fieldValue(List<ResolvedSummary.ResolvedField> fields, String label) {
        if (fields == null) {
            return null;
        }
        return fields.stream()
            .filter(field -> label.equals(field.getLabel()))
            .map(ResolvedSummary.ResolvedField::getValue)
            .findFirst()
            .orElse(null);
    }
}
