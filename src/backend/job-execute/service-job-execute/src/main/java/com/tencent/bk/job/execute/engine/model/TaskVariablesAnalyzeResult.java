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

package com.tencent.bk.job.execute.engine.model;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/*
 *全局变量分析结果
 */
@Getter
@ToString
public class TaskVariablesAnalyzeResult {
    private List<TaskVariableDTO> taskVars;
    private boolean existAnyVar = false; //作业中是否存在变量
    private boolean existOnlyConstVar = true;  //作业中是否仅存在常量
    private boolean existNamespaceVar = false; //是否存在命名空间变量
    private boolean existChangeableGlobalVar = false; //是否存在可赋值全局变量
    private List<String> allVarNames = new ArrayList<>(); // 所有变量列表
    private List<String> constVarNames = new ArrayList<>(); // 常量列表
    private List<String> namespaceVarNames = new ArrayList<>(); // 命名空间变量列表
    private List<String> changeableGlobalVarNames = new ArrayList<>(); // 赋值可变变量列表

    public TaskVariablesAnalyzeResult(List<TaskVariableDTO> taskVars) {
        if (CollectionUtils.isEmpty(taskVars)) {
            return;
        }
        this.taskVars = taskVars;
        this.existAnyVar = true;

        for (TaskVariableDTO taskVar : taskVars) {
            if (taskVar.isChangeable()) {
                existOnlyConstVar = false;
                existChangeableGlobalVar = true;
                changeableGlobalVarNames.add(taskVar.getName());
                if (taskVar.getType() == TaskVariableTypeEnum.NAMESPACE.getType()) {
                    existNamespaceVar = true;
                    namespaceVarNames.add(taskVar.getName());
                }
            } else {
                constVarNames.add(taskVar.getName());
            }
        }
        if (!changeableGlobalVarNames.isEmpty()) {
            changeableGlobalVarNames.sort(String.CASE_INSENSITIVE_ORDER);
        }
        if (!namespaceVarNames.isEmpty()) {
            namespaceVarNames.sort(String.CASE_INSENSITIVE_ORDER);
        }
        allVarNames.addAll(changeableGlobalVarNames);
        allVarNames.addAll(namespaceVarNames);
        allVarNames.addAll(constVarNames);
    }

    public boolean isNamespaceVar(String varName) {
        return namespaceVarNames.contains(varName);
    }

    public boolean isChangeableGlobalVar(String varName) {
        return changeableGlobalVarNames.contains(varName);
    }

    public boolean isConstVar(String varName) {
        return constVarNames.contains(varName);
    }

    public TaskVariableDTO getTaskVarByVarName(String varName) {
        for (TaskVariableDTO taskVar : taskVars) {
            if (taskVar.getName().equals(varName)) {
                return taskVar;
            }
        }
        return null;
    }

    public boolean isExistConstVar() {
        return !constVarNames.isEmpty();
    }
}
