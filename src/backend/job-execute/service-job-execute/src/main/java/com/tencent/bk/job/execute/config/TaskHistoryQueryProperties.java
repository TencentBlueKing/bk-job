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

package com.tencent.bk.job.execute.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 任务历史查询配置
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "job.execute.task-history-query")
@Component
public class TaskHistoryQueryProperties {

    /**
     * 复杂查询限制，用于阻止大业务下大时间范围的复杂查询导致慢查询影响整个系统
     */
    private ComplexQueryLimitConfig complexQueryLimit = new ComplexQueryLimitConfig();

    @Getter
    @Setter
    @ToString
    public static class ComplexQueryLimitConfig {
        /**
         * 是否启用复杂查询限制，默认启用
         */
        private boolean enabled = true;

        /**
         * 允许查询扫描的最大数据量，默认2000万
         */
        private long maxQueryDataNum = 2000_0000L;

        /**
         * 样本天数，用于估算平均每天任务量，默认7天
         */
        private int sampleDays = 7;
    }
}
