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

package com.tencent.bk.job.analysis.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DimensionEnum {
    /**
     * 执行过的任务：启动方式
     */
    TASK_STARTUP_MODE("job.analysis.dimensions.executedTask.taskStartupMode"),
    /**
     * 执行过的任务：类型
     */
    TASK_TYPE("job.analysis.dimensions.executedTask.taskType"),
    /**
     * 执行过的任务：耗时
     */
    TASK_TIME_CONSUMING("job.analysis.dimensions.executedTask.taskTimeConsuming"),
    /**
     * 执行过的快速执行脚本：脚本类型
     */
    SCRIPT_TYPE("job.analysis.dimensions.executedFastScript.scriptType"),
    /**
     * 执行过的快速分发文件：文件传输模式
     */
    FILE_TRANSFER_MODE("job.analysis.dimensions.executedFastFile.fileTransferMode");

    private final String i18nCode;

    public static String getI18nCodeByName(String name) {
        return DimensionEnum.valueOf(name).getI18nCode();
    }
}
