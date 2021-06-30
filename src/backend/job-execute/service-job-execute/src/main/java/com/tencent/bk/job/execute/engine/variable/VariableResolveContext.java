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

package com.tencent.bk.job.execute.engine.variable;

import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;


/**
 * 变量解析上下文
 */
@Getter
@Setter
@ToString
public class VariableResolveContext {
    /**
     * 作业实例
     */
    private TaskInstanceDTO taskInstance;
    /**
     * 步骤实例
     */
    private StepInstanceDTO stepInstance;
    /**
     * 全局变量定义
     */
    private Map<String, TaskVariableDTO> globalVariables;
    /**
     * 步骤输入变量值
     */
    private StepInstanceVariableValuesDTO stepInputVariables;

    public VariableResolveContext(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance,
                                  Map<String, TaskVariableDTO> globalVariables,
                                  StepInstanceVariableValuesDTO stepInputVariables) {
        this.taskInstance = taskInstance;
        this.stepInstance = stepInstance;
        this.globalVariables = globalVariables;
        this.stepInputVariables = stepInputVariables;
    }

    public long getTaskInstanceId() {
        return taskInstance.getId();
    }

    public long getStepInstanceId() {
        return stepInstance.getId();
    }
}
