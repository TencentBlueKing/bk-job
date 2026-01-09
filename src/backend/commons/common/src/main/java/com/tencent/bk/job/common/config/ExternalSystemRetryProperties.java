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

package com.tencent.bk.job.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部系统重试配置属性
 * <p>
 * 配置前缀: external-system.retry
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "external-system.retry")
public class ExternalSystemRetryProperties {

    /**
     * 全局配置
     */
    private RetryProperties global = RetryProperties.defaultGlobalConfig();

    /**
     * CMDB 系统单独配置
     */
    private RetryProperties cmdb;

    /**
     * IAM 系统单独配置
     */
    private RetryProperties iam;

    /**
     * GSE 系统单独配置
     */
    private RetryProperties gse;

    /**
     * BK-Login 系统单独配置
     */
    private RetryProperties bkLogin;

    /**
     * BK-User 系统单独配置
     */
    private RetryProperties bkUser;

    /**
     * 判断指定系统是否应当启用重试
     *
     * @param systemProperties 系统配置
     * @return 是否启用重试
     */
    public boolean isSystemRetryEnabled(RetryProperties systemProperties) {
        if (systemProperties == null) {
            return global.getEnabled();
        }
        // 如果系统级别有明确配置，使用系统级别的配置；否则使用全局配置
        return systemProperties.getEnabled() != null ? systemProperties.getEnabled() : global.getEnabled();
    }

    /**
     * 获取系统的最大重试次数
     *
     * @param systemProperties 系统配置
     * @return 最大重试次数
     */
    public int getSystemMaxAttempts(RetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMaxAttempts() != null) {
            return systemProperties.getMaxAttempts();
        }
        return global.getMaxAttempts();
    }

    /**
     * 获取系统的初始重试间隔
     *
     * @param systemProperties 系统配置
     * @return 初始重试间隔（毫秒）
     */
    public long getSystemInitialIntervalMs(RetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getInitialIntervalMs() != null) {
            return systemProperties.getInitialIntervalMs();
        }
        return global.getInitialIntervalMs();
    }

    /**
     * 获取系统的最大重试间隔
     *
     * @param systemProperties 系统配置
     * @return 最大重试间隔（毫秒）
     */
    public long getSystemMaxIntervalMs(RetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMaxIntervalMs() != null) {
            return systemProperties.getMaxIntervalMs();
        }
        return global.getMaxIntervalMs();
    }

    /**
     * 获取系统的间隔增长倍数
     *
     * @param systemProperties 系统配置
     * @return 间隔增长倍数
     */
    public double getSystemMultiplier(RetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMultiplier() != null) {
            return systemProperties.getMultiplier();
        }
        return global.getMultiplier();
    }

    /**
     * 是否启用重试指标采集
     *
     * @param systemProperties 系统配置
     * @return 布尔值
     */
    public boolean isMetricsEnabled(RetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMetricsEnabled() != null) {
            return systemProperties.getMetricsEnabled();
        }
        return global.getMetricsEnabled();
    }


}
