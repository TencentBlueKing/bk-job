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
public enum ResourceEnum {
    /**
     * 执行过的任务（包含快速执行脚本、快速分发文件、作业）
     */
    EXECUTED_TASK("job.analysis.resources.executedTask"),
    /**
     * 滚动执行过的任务（包含快速执行脚本、快速分发文件、作业）
     */
    EXECUTED_ROLLING_TASK("job.analysis.resources.executedRollingTask"),
    /**
     * 执行过的快速执行脚本
     */
    EXECUTED_FAST_SCRIPT("job.analysis.resources.executedFastScript"),
    /**
     * 执行过的快速分发文件
     */
    EXECUTED_FAST_FILE("job.analysis.resources.executedFastFile");

    private final String i18nCode;

    public static String getI18nCodeByName(String name) {
        return ResourceEnum.valueOf(name).getI18nCode();
    }
}
