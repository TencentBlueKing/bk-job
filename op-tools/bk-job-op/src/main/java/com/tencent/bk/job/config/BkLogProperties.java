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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BK-Log 配置属性，支持多日志接入点（endpoints）与多日志源（sources）配置。
 * <p>
 * 配置结构：
 * <pre>
 * bk.log:
 *   defaultSource: prod-service
 *   endpoints:
 *     env-a:
 *       url: http://bklog.aaa.com
 *       bkAppCode: "xxx"        # 可选，缺省回退到 bk-api-gateway.bkAppCode
 *       bkAppSecret: "xxx"      # 可选，缺省回退到 bk-api-gateway.bkAppSecret
 *       username: admin
 *       retryCount: 3
 *       retryInterval: 3
 *     env-b:
 *       url: http://bklog.bbb.com
 *       retryCount: 3
 *       retryInterval: 3
 *   sources:
 *     prod-service:
 *       label: "xxx环境-微服务日志"
 *       endpoint: env-a
 *       sortField: "log_time"
 *       indices: "..."
 *       indexSetId: 100
 *       expireTime: 7
 *     othergame-service:
 *       label: "yyy环境-微服务日志"
 *       endpoint: env-b
 *       sortField: "log_time"
 *       indexSetId: 300
 *       expireTime: 7
 * </pre>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "bk.log")
public class BkLogProperties {

    /**
     * 兼容回退时使用的 endpoint 名称
     */
    public static final String DEFAULT_ENDPOINT = "default";

    /**
     * 默认日志源 key
     */
    private String defaultSource;

    /**
     * 日志接入点（不同环境的 bklog 网关）配置表，key 为接入点名称
     */
    private Map<String, Endpoint> endpoints = new LinkedHashMap<>();

    /**
     * 日志源配置表，key 为日志源短标识
     */
    private Map<String, LogSource> sources = new LinkedHashMap<>();

    // ---- 以下为兼容旧配置的字段 ----

    private String indices;
    private Integer indexSetId;
    private Integer expireTime = 7;

    /**
     * 兼容旧配置：如果未配置 sources，用旧字段构造一个 default 日志源，
     * 并把它的 endpoint 指向 {@link #DEFAULT_ENDPOINT}
     */
    @PostConstruct
    public void init() {
        if (sources.isEmpty() && indexSetId != null) {
            LogSource fallback = new LogSource();
            fallback.setLabel("默认日志");
            fallback.setIndices(indices);
            fallback.setIndexSetId(indexSetId);
            fallback.setExpireTime(expireTime);
            fallback.setEndpoint(DEFAULT_ENDPOINT);
            sources.put("default", fallback);
            if (StringUtils.isBlank(defaultSource)) {
                defaultSource = "default";
            }
            log.warn("bk.log.sources not configured, using fallback config");
        }
    }

    /**
     * 获取指定日志源的配置，source 为空时返回默认日志源
     */
    public LogSource getSource(String source) {
        String key = StringUtils.isBlank(source) ? defaultSource : source;
        LogSource logSource = sources.get(key);
        if (logSource == null) {
            throw new IllegalArgumentException(
                "未知的日志源: " + key + "，可选日志源: " + sources.keySet()
            );
        }
        return logSource;
    }

    /**
     * 获取指定日志接入点配置，name 为空时使用 {@link #DEFAULT_ENDPOINT}
     */
    public Endpoint getEndpoint(String name) {
        String key = StringUtils.isBlank(name) ? DEFAULT_ENDPOINT : name;
        Endpoint endpoint = endpoints.get(key);
        if (endpoint == null) {
            throw new IllegalArgumentException(
                "未知的日志接入点: " + key + "，可选接入点: " + endpoints.keySet()
            );
        }
        return endpoint;
    }

    @Data
    public static class Endpoint {
        /**
         * bklog 网关 base url
         */
        private String url;

        /**
         * 应用 Code，可空；为空时回退到 bk-api-gateway.bkAppCode
         */
        private String bkAppCode;

        /**
         * 应用 Secret，可空；为空时回退到 bk-api-gateway.bkAppSecret
         */
        private String bkAppSecret;

        /**
         * 调用别的系统时使用的用户名，可空；为空时回退到 "admin"
         */
        private String username;

        /**
         * 多租户 ID，会作为请求头 X-Bk-Tenant-Id 发送；缺省为 "default"
         */
        private String tenantId = "default";

        /**
         * 重试次数
         */
        private Integer retryCount = 1;

        /**
         * 重试间隔，单位：秒
         */
        private Integer retryInterval = 5;
    }

    @Data
    public static class LogSource {
        /**
         * 日志源显示名称
         */
        private String label;
        /**
         * 日志源排序字段
         */
        private String sortField;

        /**
         * 日志索引名称
         */
        private String indices;

        /**
         * 索引集 ID
         */
        private Integer indexSetId;

        /**
         * 日志保存时间（天）
         */
        private Integer expireTime = 7;

        /**
         * 该日志源所属的接入点名称（对应 {@link BkLogProperties#endpoints} 的 key）。
         * 为空时回退到 {@link #DEFAULT_ENDPOINT}。
         */
        private String endpoint;
    }
}
