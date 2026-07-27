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

package com.tencent.bk.job.manage.manager.variable;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.service.VariableResolver;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 从步骤内容推导各步骤引用了哪些全局变量，并写入 {@link TaskStepDTO#setRefVariables}。
 *
 * <p>适用场景：响应或 VO 需要展示 {@code ref_variables}（步骤级引用变量名列表）时，在转 VO/ESB 之前调用。
 * 例如 Web 模板详情 {@link TaskStepDTO#toVO}、V3 执行方案详情、备份内部接口等。
 *
 * <p>不适用于仅返回步骤原始配置、且协议不含 {@code ref_variables} 的 OpenAPI（如 V4
 * {@code get_job_template_detail}）：本解析不修改 {@code execute_target}、全局变量默认值等持久化字段，
 * 调用后也不会被 V4 转换器读取，属于多余开销。
 *
 * <p>副作用：仅就地填充每个 {@link TaskStepDTO} 的 {@code refVariables}，入参 {@code variables} 只读。
 */
public class StepRefVariableParser {

    private StepRefVariableParser() {
    }

    /**
     * 遍历步骤列表，按步骤类型解析引用变量并写入 {@link TaskStepDTO#setRefVariables}。
     *
     * @param steps     待解析的步骤列表（通常与模板/方案上的 {@code stepList} 为同一引用）
     * @param variables 模板或方案的全局变量全集，用于按名称匹配出 {@link TaskVariableDTO} 对象
     */
    public static void parseStepRefVars(List<TaskStepDTO> steps,
                                        List<TaskVariableDTO> variables) {
        steps.forEach(step -> {
            if (CollectionUtils.isEmpty(variables)) {
                return;
            }

            TaskStepTypeEnum stepType = step.getType();
            switch (stepType) {
                case SCRIPT:
                    parseScriptStepRefVars(step, variables);
                    break;
                case FILE:
                    parseFileStepRefVars(step, variables);
                    break;
                case APPROVAL:
                default:
                    break;
            }
        });
    }

    /**
     * 脚本步骤：从脚本参数、Shell 正文（含 job_import / 魔法命名空间）、执行目标变量名等收集引用，再过滤全局变量列表。
     */
    private static void parseScriptStepRefVars(TaskStepDTO step,
                                               List<TaskVariableDTO> variables) {
        TaskScriptStepDTO scriptStep = step.getScriptStepInfo();
        List<String> refVarNames = new ArrayList<>();

        // 解析脚本参数
        List<String> jobStandardVarNames = VariableResolver.resolveJobStandardVar(scriptStep.getScriptParam());
        if (CollectionUtils.isNotEmpty(jobStandardVarNames)) {
            refVarNames.addAll(jobStandardVarNames);
        }

        // 解析 shell 脚本引用的一些特殊变量
        if (scriptStep.getLanguage() == ScriptTypeEnum.SHELL) {
            // 从shell脚本中匹配所有符合shell变量命名的变量
            List<String> shellVarNames = VariableResolver.resolveShellScriptVar(scriptStep.getContent());
            // 使用 job_import 方式引用的变量
            List<String> jobImportedVarNames = VariableResolver.resolveJobImportVariables(scriptStep.getContent());
            if (CollectionUtils.isNotEmpty(shellVarNames)) {
                refVarNames.addAll(shellVarNames);
            }
            if (CollectionUtils.isNotEmpty(jobImportedVarNames)) {
                refVarNames.addAll(jobImportedVarNames);
            }
            // 魔法变量
            refVarNames.addAll(resolveJobMagicNamespaceVar(jobImportedVarNames, variables));
        }

        // 主机变量
        if (scriptStep.getExecuteTarget() != null
            && StringUtils.isNoneBlank(scriptStep.getExecuteTarget().getVariable())) {
            refVarNames.add(scriptStep.getExecuteTarget().getVariable());
        }

        // 执行账号全局变量
        if (StringUtils.isNotBlank(scriptStep.getAccountVar())) {
            refVarNames.add(scriptStep.getAccountVar());
        }

        step.setRefVariables(variables.stream().filter(variable -> refVarNames.contains(variable.getName()))
            .distinct().collect(Collectors.toList()));
    }

    /**
     * 将 Shell 脚本中的 {@code JOB_NAMESPACE_*} 魔法引用展开为具体命名空间类全局变量名。
     *
     * @param jobImportedVarNames 通过 job_import 方式引用的变量名
     * @param variables           全局变量全集（用于筛选命名空间类型变量）
     * @return 展开后的命名空间变量名列表
     */
    private static List<String> resolveJobMagicNamespaceVar(List<String> jobImportedVarNames,
                                                            List<TaskVariableDTO> variables) {
        List<String> namespaceVarNames = variables.stream()
            .filter(var -> var.getType() == TaskVariableTypeEnum.NAMESPACE)
            .map(TaskVariableDTO::getName)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(namespaceVarNames)) {
            return Collections.emptyList();
        }

        List<String> resolvedNamespaceVars = new ArrayList<>();
        jobImportedVarNames.stream()
            .filter(jobImportedVarName -> jobImportedVarName.startsWith("JOB_NAMESPACE_"))
            .forEach(jobImportedVarName -> {
                if ("JOB_NAMESPACE_ALL".equals(jobImportedVarName)) {
                    resolvedNamespaceVars.addAll(namespaceVarNames);
                } else {
                    String jobImportNamespaceVar = jobImportedVarName.substring("JOB_NAMESPACE_".length());
                    if (namespaceVarNames.contains(jobImportNamespaceVar)) {
                        resolvedNamespaceVars.add(jobImportNamespaceVar);
                    }
                }
            });
        return resolvedNamespaceVars.stream().distinct().collect(Collectors.toList());
    }


    /**
     * 文件步骤：从源/目标路径中的 {@code ${var}}、源与目标执行目标变量名等收集引用，再过滤全局变量列表。
     */
    private static void parseFileStepRefVars(TaskStepDTO step,
                                             List<TaskVariableDTO> variables) {
        TaskFileStepDTO fileStep = step.getFileStepInfo();
        List<String> refVarNames = new ArrayList<>();

        fileStep.getOriginFileList().forEach(originFile -> {
            if (CollectionUtils.isNotEmpty(originFile.getFileLocation())) {
                originFile.getFileLocation().forEach(filePath -> {
                    List<String> filePathVarNames = VariableResolver.resolveJobStandardVar(filePath);
                    if (CollectionUtils.isNotEmpty(filePathVarNames)) {
                        refVarNames.addAll(filePathVarNames);
                    }

                });
            }
            if (originFile.getHost() != null && StringUtils.isNoneBlank(originFile.getHost().getVariable())) {
                refVarNames.add(originFile.getHost().getVariable());
            }
            if (StringUtils.isNotBlank(originFile.getHostAccountVar())) {
                refVarNames.add(originFile.getHostAccountVar());
            }
        });
        List<String> destFilePathVarNames =
            VariableResolver.resolveJobStandardVar(fileStep.getDestinationFileLocation());
        if (CollectionUtils.isNotEmpty(destFilePathVarNames)) {
            refVarNames.addAll(destFilePathVarNames);
        }

        if (fileStep.getDestinationHostList() != null
            && StringUtils.isNoneBlank(fileStep.getDestinationHostList().getVariable())) {
            refVarNames.add(fileStep.getDestinationHostList().getVariable());
        }
        if (StringUtils.isNotBlank(fileStep.getExecuteAccountVar())) {
            refVarNames.add(fileStep.getExecuteAccountVar());
        }

        step.setRefVariables(variables.stream().filter(variable -> refVarNames.contains(variable.getName()))
            .distinct().collect(Collectors.toList()));
    }


}
