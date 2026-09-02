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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.util.FileTransferModeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把 dryRun 预检解析出的执行信息组装成审批单据概要。
 * <p>
 * 组装的是<b>解析之后的实际影响面</b>：审批人看到的应当是"将在 37 台主机上以 root 执行脚本 X、
 * 命中高危规则 Y"，而不是一个动态分组 ID —— 后者等于让人盲签。
 */
public class ResolvedSummaryBuilder {

    /**
     * 高危账号别名。以这些账号执行意味着目标机上的任何操作都不受限，单据必须显著标注
     */
    private static final Set<String> HIGH_RISK_ACCOUNTS = new HashSet<>(
        Arrays.asList("root", "administrator", "system"));

    private ResolvedSummaryBuilder() {
    }

    /**
     * 由预检返回的作业实例组装概要。快速执行脚本、快速分发文件没有全局变量，走这个重载。
     */
    public static ResolvedSummary build(TaskInstanceDTO taskInstance) {
        return build(taskInstance, Collections.emptySet());
    }

    /**
     * 由预检返回的作业实例组装概要。
     * <p>
     * 不设置 operationType：审批操作类型是审批域的概念，由 job-analysis 侧填充，
     * 避免在下游服务里复制一份操作类型枚举。
     *
     * @param taskInstance     预检返回的作业实例，其 stepInstances 与 variables 已由预检返回点填充
     * @param assignedVarNames 本次请求显式指定了取值的全局变量名，其余变量沿用执行方案的默认值
     */
    public static ResolvedSummary build(TaskInstanceDTO taskInstance, Set<String> assignedVarNames) {
        ResolvedSummary summary = new ResolvedSummary();
        if (taskInstance == null) {
            return summary;
        }
        fillGlobalVars(summary, taskInstance.getVariables(), assignedVarNames);
        summary.setName(taskInstance.getName());
        if (taskInstance.getPlanId() != null && taskInstance.getPlanId() > 0) {
            summary.addField("job_plan_id", String.valueOf(taskInstance.getPlanId()));
        }

        List<StepInstanceDTO> stepInstances = taskInstance.getStepInstances();
        if (CollectionUtils.isEmpty(stepInstances)) {
            return summary;
        }

        // 跨步骤按执行对象去重：多个步骤打同一批主机时，"将影响多少台机器"应当只算一次
        Set<ExecuteObject> allExecuteObjects = new HashSet<>();
        boolean containsDynamicTarget = false;
        boolean dangerousRuleMatched = false;
        for (StepInstanceDTO stepInstance : stepInstances) {
            ResolvedSummary.ResolvedStep step = buildStep(stepInstance);
            summary.addStep(step);
            allExecuteObjects.addAll(resolveExecuteObjects(stepInstance.getTargetExecuteObjects()));
            containsDynamicTarget |= Boolean.TRUE.equals(step.getContainsDynamicTarget());
            dangerousRuleMatched |= StringUtils.isNotBlank(step.getDangerousCheckSummary());
        }
        summary.setTotalExecuteObjectCount(allExecuteObjects.size());
        // 全局变量里的动态目标已在 fillGlobalVars 里标过，别在这里覆盖掉
        summary.setContainsDynamicTarget(containsDynamicTarget
            || Boolean.TRUE.equals(summary.getContainsDynamicTarget()));
        summary.setDangerousRuleMatched(dangerousRuleMatched);
        return summary;
    }

    /**
     * 启动执行方案的全局变量：这次操作到底拿什么参数去跑的直接答案。
     * <p>
     * <b>全部变量都要列出</b>，包括本次请求没指定、沿用执行方案默认值的那些——沿用来的取值一样会被执行，
     * 只列本次传的等于让审批人蒙着眼放行。变量在预检时已完成解析（主机变量的机器已带出 IP），
     * 这里只做形态转换，不再回查 CMDB。密文变量的取值一律不带：概要整份明文落库
     */
    private static void fillGlobalVars(ResolvedSummary summary,
                                       List<TaskVariableDTO> variables,
                                       Set<String> assignedVarNames) {
        if (CollectionUtils.isEmpty(variables)) {
            return;
        }
        for (TaskVariableDTO variable : variables) {
            ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
            globalVar.setName(variable.getName());
            TaskVariableTypeEnum type = variable.getType() == null
                ? null : TaskVariableTypeEnum.valOf(variable.getType());
            globalVar.setType(type == null ? null : type.name());
            globalVar.setAssigned(assignedVarNames.contains(variable.getName()));
            if (type == TaskVariableTypeEnum.EXECUTE_OBJECT_LIST) {
                fillGlobalVarTarget(summary, globalVar, variable.getExecuteTarget());
            } else if (type != TaskVariableTypeEnum.CIPHER) {
                globalVar.setValue(variable.getValue());
            }
            summary.addGlobalVar(globalVar);
        }
    }

    private static void fillGlobalVarTarget(ResolvedSummary summary,
                                            ResolvedSummary.ResolvedGlobalVar globalVar,
                                            ExecuteTargetDTO executeTarget) {
        if (executeTarget == null) {
            return;
        }
        for (ExecuteObject executeObject : resolveExecuteObjects(executeTarget)) {
            globalVar.addHost(executeObject.getResourceId(), executeObject.getExecuteObjectName());
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getDynamicServerGroups())) {
            globalVar.setDynamicGroupCount(executeTarget.getDynamicServerGroups().size());
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getTopoNodes())) {
            globalVar.setTopoNodeCount(executeTarget.getTopoNodes().size());
        }
        if (containsDynamicTarget(executeTarget)) {
            // 台数为 0 会被读成"不动机器"，动态目标必须给出提示
            summary.setContainsDynamicTarget(true);
        }
    }

    private static ResolvedSummary.ResolvedStep buildStep(StepInstanceDTO stepInstance) {
        ResolvedSummary.ResolvedStep step = new ResolvedSummary.ResolvedStep();
        step.setName(stepInstance.getName());
        if (stepInstance.getExecuteType() != null) {
            step.setExecuteType(stepInstance.getExecuteType().name());
        }
        // 账号别名由 checkAndSetAccountInfo 解析产出，比用户传的 accountId 可读
        String accountAlias = StringUtils.isNotBlank(stepInstance.getAccountAlias())
            ? stepInstance.getAccountAlias() : stepInstance.getAccount();
        step.setAccountAlias(accountAlias);
        step.setHighRiskAccount(isHighRiskAccount(accountAlias));
        step.setScriptName(stepInstance.getScriptName());
        step.setScriptVersionId(stepInstance.getScriptVersionId());
        step.setScriptSource(stepInstance.getScriptSource());
        // 预检期高危命中结果不落 dangerous_record，只能从这里带出来
        step.setDangerousCheckSummary(stepInstance.getDangerousCheckSummary());
        fillScriptParam(step, stepInstance);
        fillExecuteObjects(step, stepInstance.getTargetExecuteObjects());
        fillFileSources(step, stepInstance);
        fillStepFields(step, stepInstance);
        return step;
    }

    /**
     * 脚本参数决定了同一份脚本这次到底干什么（是灰度还是全量、清哪个目录），审批人必须看到。
     * <p>
     * <b>调用方声明为敏感的参数一律不带取值</b>：概要整份明文落库，只带一个敏感标记，
     * 由单据渲染成占位符。没传参数时两个字段都不带，免得单据上出一行空的敏感提示
     */
    private static void fillScriptParam(ResolvedSummary.ResolvedStep step, StepInstanceDTO stepInstance) {
        if (!stepInstance.isScriptStep() || StringUtils.isEmpty(stepInstance.getScriptParam())) {
            return;
        }
        step.setParamSensitive(stepInstance.isSecureParam());
        if (!stepInstance.isSecureParam()) {
            step.setScriptParam(stepInstance.getScriptParam());
        }
    }

    private static void fillExecuteObjects(ResolvedSummary.ResolvedStep step, ExecuteTargetDTO executeTarget) {
        if (executeTarget == null) {
            step.setExecuteObjectCount(0);
            step.setContainsDynamicTarget(false);
            return;
        }
        step.setContainsDynamicTarget(containsDynamicTarget(executeTarget));
        List<ExecuteObject> executeObjects = resolveExecuteObjects(executeTarget);
        step.setExecuteObjectCount(executeObjects.size());
        int keepCount = Math.min(executeObjects.size(), ResolvedSummary.MAX_EXECUTE_OBJECT_COUNT);
        step.setExecuteObjectTruncated(executeObjects.size() > keepCount);
        List<ResolvedSummary.ResolvedExecuteObject> resolved = new ArrayList<>(keepCount);
        for (int i = 0; i < keepCount; i++) {
            ExecuteObject executeObject = executeObjects.get(i);
            resolved.add(new ResolvedSummary.ResolvedExecuteObject(
                executeObject.getType() == null ? null : executeObject.getType().name(),
                executeObject.getResourceId(),
                executeObject.getExecuteObjectName()
            ));
        }
        step.setExecuteObjects(resolved);
    }

    private static void fillStepFields(ResolvedSummary.ResolvedStep step, StepInstanceDTO stepInstance) {
        step.addField("timeout", stepInstance.getTimeout() == null ? null : stepInstance.getTimeout() + "s");
        if (stepInstance.isFileStep()) {
            step.addField("file_target_path", stepInstance.getFileTargetPath());
            step.addField("file_target_name", stepInstance.getFileTargetName());
            step.addField("transfer_mode", describeTransferMode(stepInstance));
        }
    }

    /**
     * 各分发模式对已有文件的处置差别很大（强制模式会建目录并覆盖同名文件，保险模式则分目录存放），必须让审批人看到
     */
    private static String describeTransferMode(StepInstanceDTO stepInstance) {
        if (stepInstance.getNotExistPathHandler() == null) {
            return null;
        }
        FileTransferModeEnum transferMode = FileTransferModeUtil.getTransferMode(
            stepInstance.getFileDuplicateHandle(), stepInstance.getNotExistPathHandler());
        return transferMode == null ? null : transferMode.name();
    }

    /**
     * 源文件带上"以什么身份、从哪台机器、取哪些文件"三件事。
     * <p>
     * <b>结构化带回而不是在这里拼成一句话</b>：源机器超过上限时要说的"共 N 台"、本地文件要标的
     * "本地文件"都是给人看的文案，而预检发生在调用方的语言环境、单据却按审批人的语言渲染
     */
    private static void fillFileSources(ResolvedSummary.ResolvedStep step, StepInstanceDTO stepInstance) {
        if (!stepInstance.isFileStep() || CollectionUtils.isEmpty(stepInstance.getFileSourceList())) {
            return;
        }
        for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
            step.addFileSource(buildFileSource(fileSource));
        }
    }

    private static ResolvedSummary.ResolvedFileSource buildFileSource(FileSourceDTO fileSource) {
        ResolvedSummary.ResolvedFileSource resolved = new ResolvedSummary.ResolvedFileSource();
        resolved.setLocalUpload(fileSource.isLocalUpload());
        resolved.setAccountAlias(StringUtils.isNotBlank(fileSource.getAccountAlias())
            ? fileSource.getAccountAlias() : fileSource.getAccount());
        if (CollectionUtils.isNotEmpty(fileSource.getFiles())) {
            for (FileDetailDTO file : fileSource.getFiles()) {
                resolved.addFilePath(file.getFilePath());
            }
        }
        for (ExecuteObject executeObject : resolveExecuteObjects(fileSource.getServers())) {
            resolved.addHost(executeObject.getResourceId(), executeObject.getExecuteObjectName());
        }
        return resolved;
    }

    private static List<ExecuteObject> resolveExecuteObjects(ExecuteTargetDTO executeTarget) {
        if (executeTarget == null) {
            return Collections.emptyList();
        }
        return executeTarget.getExecuteObjectsCompatibly();
    }

    /**
     * 动态分组 / 拓扑节点 / 容器过滤器的实际执行对象在放行时会重新解析，可能与预检结果不同
     */
    private static boolean containsDynamicTarget(ExecuteTargetDTO executeTarget) {
        return CollectionUtils.isNotEmpty(executeTarget.getDynamicServerGroups())
            || CollectionUtils.isNotEmpty(executeTarget.getTopoNodes())
            || CollectionUtils.isNotEmpty(executeTarget.getContainerFilters());
    }

    private static boolean isHighRiskAccount(String accountAlias) {
        return StringUtils.isNotBlank(accountAlias)
            && HIGH_RISK_ACCOUNTS.contains(accountAlias.trim().toLowerCase());
    }
}
