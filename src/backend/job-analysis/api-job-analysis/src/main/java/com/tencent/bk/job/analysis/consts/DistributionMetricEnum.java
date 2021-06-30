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
public enum DistributionMetricEnum {
    /**
     * 主机操作系统类型
     */
    HOST_SYSTEM_TYPE("job.analysis.metrics.distribution.hostSystemType"),

    /**
     * 作业步骤类型
     */
    STEP_TYPE("job.analysis.metrics.distribution.stepType"),

    /**
     * 脚本类型
     */
    SCRIPT_TYPE("job.analysis.metrics.distribution.scriptType"),

    /**
     * 脚本版本状态
     */
    SCRIPT_VERSION_STATUS("job.analysis.metrics.distribution.scriptVersionStatus"),

    /**
     * 定时任务状态
     */
    CRON_STATUS("job.analysis.metrics.distribution.cronStatus"),

    /**
     * 定时任务类型
     */
    CRON_TYPE("job.analysis.metrics.distribution.cronType"),

    /**
     * 标签
     */
    TAG("job.analysis.metrics.distribution.tag"),

    /**
     * 账号类型
     */
    ACCOUNT_TYPE("job.analysis.metrics.distribution.accountType");

    private final String i18nCode;

    public static String getI18nCodeByName(String name) {
        return DistributionMetricEnum.valueOf(name).getI18nCode();
    }
}
