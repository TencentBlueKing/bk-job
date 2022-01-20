/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
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
 * 步骤引用的全局变量解析
 */
public class StepRefVariableParser {

    /**
     * 解析步骤引用的全局变量
     *
     * @param steps     步骤列表
     * @param variables 全局变量列表
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

        step.setRefVariables(variables.stream().filter(variable -> refVarNames.contains(variable.getName()))
            .distinct().collect(Collectors.toList()));
    }

    /**
     * 获取魔法变量解析之后的变量列表
     *
     * @param jobImportedVarNames 通过 job_import 方式引用的变量
     * @param variables           全局变量
     * @return 魔法变量解析之后的变量列表
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

        step.setRefVariables(variables.stream().filter(variable -> refVarNames.contains(variable.getName()))
            .distinct().collect(Collectors.toList()));
    }


}
