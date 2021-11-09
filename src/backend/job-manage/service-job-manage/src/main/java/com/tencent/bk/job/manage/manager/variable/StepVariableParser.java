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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StepVariableParser {

    public static List<String> parseShellScriptVar(String shellScriptContent) {
        Matcher m = Pattern.compile("\\$\\{[#!]?([_a-zA-Z][0-9_a-zA-Z]*)\\S*}").matcher(shellScriptContent);
        List<String> varNames = new ArrayList<>();
        while (m.find()) {
            String varName = m.group(1);
            if (!varNames.contains(varName)) {
                varNames.add(varName);
            }
        }
        return varNames;
    }
    /**
     * 解析步骤和引用的全局变量
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

        List<String> jobStandardVarNames = VariableResolver.resolveJobStandardVar(scriptStep.getScriptParam());
        if (CollectionUtils.isNotEmpty(jobStandardVarNames)) {
            refVarNames.addAll(jobStandardVarNames);
        }
        if (scriptStep.getLanguage() == ScriptTypeEnum.SHELL) {
            List<String> shellVarNames = VariableResolver.resolveShellScriptVar(scriptStep.getContent());
            List<String> jobImportedVarNames = VariableResolver.resolveJobImportVariables(scriptStep.getContent());
            if (CollectionUtils.isNotEmpty(shellVarNames)) {
                refVarNames.addAll(shellVarNames);
            }
            if (CollectionUtils.isNotEmpty(jobImportedVarNames)) {
                refVarNames.addAll(jobImportedVarNames);
            }
        }

        if (scriptStep.getExecuteTarget() != null
            && StringUtils.isNoneBlank(scriptStep.getExecuteTarget().getVariable())) {
            refVarNames.add(scriptStep.getExecuteTarget().getVariable());
        }

        step.setRefVariables(variables.stream().filter(variable -> refVarNames.contains(variable.getName()))
            .distinct().collect(Collectors.toList()));
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
