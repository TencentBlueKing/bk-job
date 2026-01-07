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
     * 是否启用重试（默认关闭）
     */
    private boolean enabled = false;

    /**
     * 初始重试间隔（毫秒），默认 500ms
     */
    private long initialIntervalMs = 500L;

    /**
     * 最大重试次数，默认 5 次
     */
    private int maxAttempts = 5;

    /**
     * 最大重试间隔（毫秒），默认 30000ms (30秒)
     */
    private long maxIntervalMs = 30000L;

    /**
     * 间隔增长倍数，默认 2.0
     */
    private double multiplier = 2.0;

    /**
     * 是否启用指标采集（默认开启）
     */
    private boolean metricsEnabled = true;

    /**
     * CMDB 系统单独配置
     */
    private SystemRetryProperties cmdb = new SystemRetryProperties();

    /**
     * IAM 系统单独配置
     */
    private SystemRetryProperties iam = new SystemRetryProperties();

    /**
     * GSE 系统单独配置
     */
    private SystemRetryProperties gse = new SystemRetryProperties();

    /**
     * BK-Login 系统单独配置
     */
    private SystemRetryProperties bkLogin = new SystemRetryProperties();

    /**
     * BK-User 系统单独配置
     */
    private SystemRetryProperties bkUser = new SystemRetryProperties();

    /**
     * 判断指定系统是否启用重试
     *
     * @param systemProperties 系统配置
     * @return 是否启用重试
     */
    public boolean isSystemRetryEnabled(SystemRetryProperties systemProperties) {
        if (systemProperties == null) {
            return enabled;
        }
        // 如果系统级别有明确配置，使用系统级别的配置；否则使用全局配置
        return systemProperties.getEnabled() != null ? systemProperties.getEnabled() : enabled;
    }

    /**
     * 获取系统的最大重试次数
     *
     * @param systemProperties 系统配置
     * @return 最大重试次数
     */
    public int getSystemMaxAttempts(SystemRetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMaxAttempts() != null) {
            return systemProperties.getMaxAttempts();
        }
        return maxAttempts;
    }

    /**
     * 获取系统的初始重试间隔
     *
     * @param systemProperties 系统配置
     * @return 初始重试间隔（毫秒）
     */
    public long getSystemInitialIntervalMs(SystemRetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getInitialIntervalMs() != null) {
            return systemProperties.getInitialIntervalMs();
        }
        return initialIntervalMs;
    }

    /**
     * 获取系统的最大重试间隔
     *
     * @param systemProperties 系统配置
     * @return 最大重试间隔（毫秒）
     */
    public long getSystemMaxIntervalMs(SystemRetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMaxIntervalMs() != null) {
            return systemProperties.getMaxIntervalMs();
        }
        return maxIntervalMs;
    }

    /**
     * 获取系统的间隔增长倍数
     *
     * @param systemProperties 系统配置
     * @return 间隔增长倍数
     */
    public double getSystemMultiplier(SystemRetryProperties systemProperties) {
        if (systemProperties != null && systemProperties.getMultiplier() != null) {
            return systemProperties.getMultiplier();
        }
        return multiplier;
    }

    /**
     * 各外部系统的单独配置
     */
    @Data
    public static class SystemRetryProperties {
        /**
         * 是否启用（null 表示使用全局配置）
         */
        private Boolean enabled;

        /**
         * 最大重试次数（null 表示使用全局配置）
         */
        private Integer maxAttempts;

        /**
         * 初始重试间隔（毫秒，null 表示使用全局配置）
         */
        private Long initialIntervalMs;

        /**
         * 最大重试间隔（毫秒，null 表示使用全局配置）
         */
        private Long maxIntervalMs;

        /**
         * 间隔增长倍数（null 表示使用全局配置）
         */
        private Double multiplier;
    }
}
