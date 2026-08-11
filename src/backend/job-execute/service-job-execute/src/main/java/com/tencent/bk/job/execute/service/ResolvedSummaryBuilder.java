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

import com.tencent.bk.job.common.api.model.ResolvedSummary;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 由预检返回的作业实例组装概要。
     * <p>
     * 不设置 operationType：审批操作类型是审批域的概念，由 job-analysis 侧填充，
     * 避免在下游服务里复制一份操作类型枚举。
     *
     * @param taskInstance 预检返回的作业实例，其 stepInstances 已由预检返回点填充
     */
    public static ResolvedSummary build(TaskInstanceDTO taskInstance) {
        ResolvedSummary summary = new ResolvedSummary();
        if (taskInstance == null) {
            return summary;
        }
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
        summary.setContainsDynamicTarget(containsDynamicTarget);
        summary.setDangerousRuleMatched(dangerousRuleMatched);
        return summary;
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
        fillExecuteObjects(step, stepInstance.getTargetExecuteObjects());
        fillStepFields(step, stepInstance);
        return step;
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
            step.addField("file_source_list", summaryFileSources(stepInstance.getFileSourceList()));
            step.addField("transfer_mode", describeTransferMode(stepInstance.getNotExistPathHandler()));
        }
    }

    /**
     * 强制模式会在目标路径不存在时自动创建目录并覆盖同名文件，与严格模式的后果差别很大，必须让审批人看到
     */
    private static String describeTransferMode(Integer notExistPathHandler) {
        if (notExistPathHandler == null) {
            return null;
        }
        return NotExistPathHandlerEnum.STEP_FAIL.getValue() == notExistPathHandler ? "STRICT" : "FORCE";
    }

    /**
     * 源文件按"账号@目标 -> 文件路径"的形式概述，让审批人看清要从哪台机器取哪些文件
     */
    private static String summaryFileSources(List<FileSourceDTO> fileSources) {
        if (CollectionUtils.isEmpty(fileSources)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (FileSourceDTO fileSource : fileSources) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            if (StringUtils.isNotBlank(fileSource.getAccountAlias())) {
                sb.append(fileSource.getAccountAlias()).append('@');
            }
            List<ExecuteObject> sourceExecuteObjects = resolveExecuteObjects(fileSource.getServers());
            if (!sourceExecuteObjects.isEmpty()) {
                sb.append(sourceExecuteObjects.size()).append(" target(s)");
            }
            if (CollectionUtils.isNotEmpty(fileSource.getFiles())) {
                sb.append(" -> ");
                sb.append(fileSource.getFiles().stream()
                    .map(FileDetailDTO::getFilePath)
                    .collect(Collectors.joining(",")));
            }
        }
        return sb.toString();
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
