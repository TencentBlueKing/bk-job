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

package com.tencent.bk.job.analysis.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TotalMetricEnum {
    /**
     * 接入业务量
     */
    APP_COUNT("job.analysis.metrics.total.appCount"),

    /**
     * 活跃业务量
     */
    ACTIVE_APP_COUNT("job.analysis.metrics.total.activeAppCount"),

    /**
     * 作业模板量
     */
    TASK_TEMPLATE_COUNT("job.analysis.metrics.total.taskTemplateCount"),

    /**
     * 执行方案量
     */
    TASK_PLAN_COUNT("job.analysis.metrics.total.taskPlanCount"),

    /**
     * 脚本量
     */
    SCRIPT_COUNT("job.analysis.metrics.total.scriptCount"),

    /**
     * 累计任务执行量
     */
    EXECUTED_TASK_COUNT("job.analysis.metrics.total.executedTaskCount"),

    /**
     * 任务执行失败量
     */
    FAILED_TASK_COUNT("job.analysis.metrics.total.failedTaskCount");

    private final String i18nCode;

    public static String getI18nCodeByName(String name) {
        return TotalMetricEnum.valueOf(name).getI18nCode();
    }
}
