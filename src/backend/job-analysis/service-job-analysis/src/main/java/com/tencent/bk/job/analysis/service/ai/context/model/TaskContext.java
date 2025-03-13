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

package com.tencent.bk.job.analysis.service.ai.context.model;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 任务上下文，用作动态数据填充模板，进而构建向AI提问的Prompt
 */
@AllArgsConstructor
@Data
public class TaskContext {

    /**
     * 步骤类型
     */
    private Integer executeType;
    /**
     * 步骤状态
     */
    private Integer status;
    /**
     * 脚本任务上下文
     */
    private ScriptTaskContext scriptTaskContext;
    /**
     * 文件任务上下文
     */
    private FileTaskContext fileTaskContext;

    public boolean isScriptTask() {
        StepExecuteTypeEnum stepExecuteTypeEnum = StepExecuteTypeEnum.valOf(executeType);
        return stepExecuteTypeEnum == StepExecuteTypeEnum.EXECUTE_SCRIPT
            || stepExecuteTypeEnum == StepExecuteTypeEnum.EXECUTE_SQL;
    }

    public boolean isFileTask() {
        StepExecuteTypeEnum stepExecuteTypeEnum = StepExecuteTypeEnum.valOf(executeType);
        return stepExecuteTypeEnum == StepExecuteTypeEnum.SEND_FILE;
    }

    public boolean isTaskFail() {
        return RunStatusEnum.FAIL.getValue().equals(status);
    }
}
