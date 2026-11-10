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

package com.tencent.bk.job.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BK API网关配置
 * 支持默认认证配置和各平台独立配置覆盖
 */
@Data
@ConfigurationProperties(prefix = "bk-api-gateway")
public class BkApiGwProperties {

    /**
     * 默认应用Code，跟当前是一个环境的APP Code
     */
    private String bkAppCode;

    /**
     * 默认应用Secret，跟当前是一个环境的APP Secret
     */
    private String bkAppSecret;

    /**
     * BK-Log网关配置，跟当前不在同一个环境
     */
    private ApiGwConfig bkLog;

    @Getter
    @Setter
    @ToString
    public static class ApiGwConfig {

        /**
         * API网关地址
         */
        private String url;

        /**
         * appCode
         */
        private String bkAppCode;

        /**
         * appSecret
         */
        private String bkAppSecret;

        /**
         * 调用别的系统时使用的用户名
         */
        private String username = "admin";

        /**
         * 重试次数
         */
        private Integer retryCount = 1;

        /**
         * 重试间隔，单位：秒
         */
        private Integer retryInterval = 5;
    }
}
