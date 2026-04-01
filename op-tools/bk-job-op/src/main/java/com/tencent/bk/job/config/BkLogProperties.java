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
 * BK-Log 配置属性，支持多日志源索引集配置。
 * <p>
 * 配置结构：
 * <pre>
 * bk.log:
 *   defaultSource: prod-service
 *   sources:
 *     prod-service:
 *       label: "生产环境-微服务日志"
 *       indices: "..."
 *       indexSetId: 100
 *       expireTime: 7
 *     prod-gateway:
 *       label: "生产环境-网关Access日志"
 *       indices: "..."
 *       indexSetId: 101
 *       expireTime: 7
 * </pre>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "bk.log")
public class BkLogProperties {

    /**
     * 默认日志源 key
     */
    private String defaultSource;

    /**
     * 日志源配置表，key 为日志源短标识
     */
    private Map<String, LogSource> sources = new LinkedHashMap<>();

    // ---- 以下为兼容旧配置的字段 ----

    private String indices;
    private Integer indexSetId;
    private Integer expireTime = 7;

    /**
     * 兼容旧配置：如果未配置 sources，用旧字段构造一个 default 日志源
     */
    @PostConstruct
    public void init() {
        if (sources.isEmpty() && indexSetId != null) {
            LogSource fallback = new LogSource();
            fallback.setLabel("默认日志");
            fallback.setIndices(indices);
            fallback.setIndexSetId(indexSetId);
            fallback.setExpireTime(expireTime);
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
    }
}
