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
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    @DisplayName("文件分发步骤概要带出源文件的账号、源机器与文件路径，以及目标路径与传输模式")
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
        assertThat(step.getFileSources()).hasSize(1);
        ResolvedSummary.ResolvedFileSource resolvedFileSource = step.getFileSources().get(0);
        assertThat(resolvedFileSource.getAccountAlias()).isEqualTo("root");
        assertThat(resolvedFileSource.getFilePaths()).containsExactly("/data/a.tar.gz");
        assertThat(resolvedFileSource.getHosts())
            .as("从哪台机器取文件与取哪些文件同等重要，只给路径等于没说清来源")
            .containsExactly("0:127.0.0.1");
        assertThat(resolvedFileSource.getLocalUpload()).isFalse();
        // 强制模式会自动建目录并覆盖同名文件，后果远大于严格模式，必须在单据里说清
        assertThat(fieldValue(step.getFields(), "transfer_mode")).isEqualTo("FORCE");
        assertThat(summary.getDangerousRuleMatched()).isFalse();
    }

    @Test
    @DisplayName("多个文件源逐个带回，单据渲染侧据此逐条展示与截断")
    void buildFileStepSummaryWithMultipleFileSources() {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("fast_file_step");
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setFileTargetPath("/tmp/");
        stepInstance.setFileSourceList(Arrays.asList(
            buildFileSource("root", 201L, "/data/a.tar.gz"),
            buildFileSource("mysql", 202L, "/data/b.tar.gz")));
        stepInstance.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR.getValue());
        stepInstance.setTargetExecuteObjects(buildResolvedTarget(101L));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        List<ResolvedSummary.ResolvedFileSource> fileSources = summary.getSteps().get(0).getFileSources();
        assertThat(fileSources).hasSize(2);
        assertThat(fileSources.get(0).getAccountAlias()).isEqualTo("root");
        assertThat(fileSources.get(0).getFilePaths()).containsExactly("/data/a.tar.gz");
        assertThat(fileSources.get(1).getAccountAlias()).isEqualTo("mysql");
        assertThat(fileSources.get(1).getFilePaths()).containsExactly("/data/b.tar.gz");
    }

    @Test
    @DisplayName("源文件没解析出账号时只带文件路径，渲染侧据此不拼出孤零零的连接符")
    void buildFileStepSummaryWithoutFileSourceAccount() {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setFileTargetPath("/tmp/");
        stepInstance.setFileSourceList(Collections.singletonList(
            buildFileSource(null, 201L, "/data/a.tar.gz")));
        stepInstance.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR.getValue());

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        ResolvedSummary.ResolvedFileSource resolvedFileSource = summary.getSteps().get(0).getFileSources().get(0);
        assertThat(resolvedFileSource.getAccountAlias()).isNull();
        assertThat(resolvedFileSource.getFilePaths()).containsExactly("/data/a.tar.gz");
    }

    @Test
    @DisplayName("本地上传的文件没有源机器与源账号，标出来才不会被当成漏填")
    void buildLocalUploadFileSourceSummary() {
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setLocalUpload(true);
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/tmp/20260901/app.sh");
        fileSource.setFiles(Collections.singletonList(fileDetail));

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setFileTargetPath("/tmp/");
        stepInstance.setFileSourceList(Collections.singletonList(fileSource));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary.ResolvedFileSource resolvedFileSource =
            ResolvedSummaryBuilder.build(taskInstance).getSteps().get(0).getFileSources().get(0);
        assertThat(resolvedFileSource.getLocalUpload()).isTrue();
        assertThat(resolvedFileSource.getHosts()).isNull();
        assertThat(resolvedFileSource.getHostCount()).isNull();
        assertThat(resolvedFileSource.getFilePaths()).containsExactly("/tmp/20260901/app.sh");
    }

    @Test
    @DisplayName("源机器超过展示上限时只报台数：几十台源机器逐台列出会把源文件那行铺成一堵墙")
    void buildFileSourceSummaryWithOverLimitHosts() {
        int hostCount = ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1;
        Long[] hostIds = new Long[hostCount];
        for (int i = 0; i < hostCount; i++) {
            hostIds[i] = 200L + i;
        }
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccountAlias("root");
        fileSource.setServers(buildResolvedTarget(hostIds));
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/data/a.tar.gz");
        fileSource.setFiles(Collections.singletonList(fileDetail));

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setFileTargetPath("/tmp/");
        stepInstance.setFileSourceList(Collections.singletonList(fileSource));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary.ResolvedFileSource resolvedFileSource =
            ResolvedSummaryBuilder.build(taskInstance).getSteps().get(0).getFileSources().get(0);
        assertThat(resolvedFileSource.getHosts()).isNull();
        assertThat(resolvedFileSource.getHostCount()).isEqualTo(hostCount);
    }

    @ParameterizedTest(name = "{0} 生效时概要里的分发模式为 {0}")
    @DisplayName("每种分发模式都按底层的同名文件与路径处置方式反推出来，保险模式不能被说成强制模式")
    @EnumSource(FileTransferModeEnum.class)
    void buildFileStepSummaryWithEachTransferMode(FileTransferModeEnum transferMode) {
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
        stepInstance.setFileTargetPath("/tmp/");
        stepInstance.setFileSourceList(Collections.singletonList(fileSource));
        stepInstance.setFileDuplicateHandle(transferMode.getDuplicateHandler().getId());
        stepInstance.setNotExistPathHandler(transferMode.getNotExistPathHandler().getValue());
        stepInstance.setTargetExecuteObjects(buildResolvedTarget(101L));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("fast_file_task");
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);

        ResolvedSummary.ResolvedStep step = summary.getSteps().get(0);
        assertThat(fieldValue(step.getFields(), "transfer_mode")).isEqualTo(transferMode.name());
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
    @DisplayName("脚本参数带进概要：同一份脚本，参数决定了这次到底干什么")
    void buildScriptParamSummary() {
        StepInstanceDTO stepInstance = buildScriptStep("--env=prod --force", false);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary.ResolvedStep step = ResolvedSummaryBuilder.build(taskInstance).getSteps().get(0);
        assertThat(step.getScriptParam()).isEqualTo("--env=prod --force");
        assertThat(step.getParamSensitive()).isFalse();
    }

    @Test
    @DisplayName("敏感脚本参数只带标记不带取值：概要整份明文落库")
    void buildSensitiveScriptParamSummary() {
        StepInstanceDTO stepInstance = buildScriptStep("--token=secret", true);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary.ResolvedStep step = ResolvedSummaryBuilder.build(taskInstance).getSteps().get(0);
        assertThat(step.getScriptParam()).isNull();
        assertThat(step.getParamSensitive())
            .as("取值不带但敏感标记要带，否则单据上看着像是这次执行不带参数")
            .isTrue();
    }

    @Test
    @DisplayName("没传脚本参数时不留敏感标记，免得单据上出一行空的敏感提示")
    void buildSummaryWithoutScriptParam() {
        StepInstanceDTO stepInstance = buildScriptStep(null, true);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary.ResolvedStep step = ResolvedSummaryBuilder.build(taskInstance).getSteps().get(0);
        assertThat(step.getScriptParam()).isNull();
        assertThat(step.getParamSensitive()).isNull();
    }

    @Test
    @DisplayName("超长脚本参数被截断：单个取值就能把整份概要撑到落库失败")
    void buildOverlongScriptParamSummary() {
        StepInstanceDTO stepInstance =
            buildScriptStep(StringUtils.repeat('a', ResolvedSummary.MAX_DISPLAY_VALUE_LENGTH + 100), false);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));

        ResolvedSummary.ResolvedStep step = ResolvedSummaryBuilder.build(taskInstance).getSteps().get(0);
        assertThat(step.getScriptParam()).hasSize(ResolvedSummary.MAX_DISPLAY_VALUE_LENGTH + 1);
    }

    @Test
    @DisplayName("启动执行方案列出全部生效变量，本次没传的标为沿用方案默认值")
    void buildGlobalVarSummary() {
        TaskInstanceDTO taskInstance = buildPlanTaskInstance(
            stringVar(1L, "version", "v1.2.3"),
            stringVar(2L, "port", "8080"));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance,
            Collections.singleton("version"));

        assertThat(summary.getGlobalVars()).hasSize(2);
        assertThat(globalVar(summary, "version").getAssigned()).isTrue();
        assertThat(globalVar(summary, "version").getValue()).isEqualTo("v1.2.3");
        assertThat(globalVar(summary, "port").getAssigned())
            .as("沿用方案默认值的变量不标注出来，审批人会以为这个值是本次改的")
            .isFalse();
        assertThat(globalVar(summary, "port").getValue()).isEqualTo("8080");
    }

    @Test
    @DisplayName("密文变量只带变量名与类型，取值一律不进概要")
    void buildCipherGlobalVarSummary() {
        TaskVariableDTO cipherVar = stringVar(1L, "password", "P@ssw0rd");
        cipherVar.setType(TaskVariableTypeEnum.CIPHER.getType());

        ResolvedSummary summary = ResolvedSummaryBuilder.build(buildPlanTaskInstance(cipherVar),
            Collections.singleton("password"));

        assertThat(globalVar(summary, "password").getType()).isEqualTo(TaskVariableTypeEnum.CIPHER.name());
        assertThat(globalVar(summary, "password").getValue()).isNull();
    }

    @Test
    @DisplayName("主机类变量带出预检解析好的主机，不再回查 CMDB")
    void buildHostGlobalVarSummary() {
        TaskVariableDTO hostVar = new TaskVariableDTO();
        hostVar.setId(1L);
        hostVar.setName("target_hosts");
        hostVar.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType());
        hostVar.setExecuteTarget(buildResolvedTarget(101L, 102L));

        ResolvedSummary summary = ResolvedSummaryBuilder.build(buildPlanTaskInstance(hostVar),
            Collections.singleton("target_hosts"));

        ResolvedSummary.ResolvedGlobalVar globalVar = globalVar(summary, "target_hosts");
        assertThat(globalVar.getHosts()).containsExactly("0:127.0.0.1", "0:127.0.0.1");
        assertThat(globalVar.getHostCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("变量里的动态分组同样打开「放行时重新解析」提示，不能被步骤目标的判定覆盖掉")
    void buildDynamicGlobalVarSummary() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setDynamicServerGroups(Collections.singletonList(new DynamicServerGroupDTO("group-1")));
        TaskVariableDTO hostVar = new TaskVariableDTO();
        hostVar.setId(1L);
        hostVar.setName("dynamic_hosts");
        hostVar.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType());
        hostVar.setExecuteTarget(target);

        ResolvedSummary summary = ResolvedSummaryBuilder.build(buildPlanTaskInstance(hostVar),
            Collections.singleton("dynamic_hosts"));

        assertThat(globalVar(summary, "dynamic_hosts").getDynamicGroupCount()).isEqualTo(1);
        assertThat(summary.getContainsDynamicTarget()).isTrue();
    }

    @Test
    @DisplayName("作业实例为空时返回空概要而非抛异常")
    void buildSummaryWithoutTaskInstance() {
        assertThat(ResolvedSummaryBuilder.build(null)).isNotNull();
        assertThat(ResolvedSummaryBuilder.build(new TaskInstanceDTO()).getSteps()).isNull();
    }

    private StepInstanceDTO buildScriptStep(String scriptParam, boolean secureParam) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("script_step");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setScriptParam(scriptParam);
        stepInstance.setSecureParam(secureParam);
        stepInstance.setTargetExecuteObjects(buildResolvedTarget(101L));
        return stepInstance;
    }

    /**
     * 启动执行方案的预检结果：步骤已解析，变量是方案默认值与请求取值合并后的全集
     */
    private TaskInstanceDTO buildPlanTaskInstance(TaskVariableDTO... variables) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("step_1");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setTargetExecuteObjects(buildResolvedTarget(101L));

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setPlanId(1000L);
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));
        taskInstance.setVariables(Arrays.asList(variables));
        return taskInstance;
    }

    private TaskVariableDTO stringVar(Long id, String name, String value) {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING.getType());
        variable.setValue(value);
        return variable;
    }

    private ResolvedSummary.ResolvedGlobalVar globalVar(ResolvedSummary summary, String name) {
        return summary.getGlobalVars().stream()
            .filter(globalVar -> name.equals(globalVar.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("概要里没有变量：" + name));
    }

    private FileSourceDTO buildFileSource(String accountAlias, Long hostId, String filePath) {
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccountAlias(accountAlias);
        fileSource.setServers(buildResolvedTarget(hostId));
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath(filePath);
        fileSource.setFiles(Collections.singletonList(fileDetail));
        return fileSource;
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
