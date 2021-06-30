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

package com.tencent.bk.job.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CORS 配置
 */
@Configuration
@Slf4j
public class CorsConfig {
    private static String buildAllowOriginFromUrl(String url) {
        String pattern = "(http|https)?://([a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+):?(\\d+)?";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(url);
        if (m.find()) {
            String domain = m.group();
            String scheme = m.group(1);
            String domainHost = m.group(2);
            String domainPort = m.group(4);
            // 使用80或者443标准端口,浏览器传过来的Origin会把端口去掉
            if (StringUtils.isNotBlank(domainPort) && (domainPort.equals("80") || domainPort.equals("443"))) {
                return (scheme + "://" + domainHost);
            } else {
                return domain;
            }
        } else {
            log.info("Invalid web url!");
            return null;
        }
    }

    @Bean
    public CorsWebFilter corsFilter(@Autowired BkConfig bkConfig) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        List<String> allowOrigins = new ArrayList<>();
        if (StringUtils.isNotBlank(bkConfig.getJobWebUrl())) {
            for (String webUrl : bkConfig.getJobWebUrl().split(",")) {
                if (StringUtils.isNotBlank(webUrl)) {
                    String allowOrigin = buildAllowOriginFromUrl(webUrl.trim());
                    if (StringUtils.isNotBlank(allowOrigin)) {
                        allowOrigins.add(allowOrigin);
                    }
                }
            }
        } else {
            log.error("Init cors config fail! job.web.url is not configured!");
        }
        source.registerCorsConfiguration("/**", buildConfig(allowOrigins));
        return new CorsWebFilter(source);
    }

    private CorsConfiguration buildConfig(List<String> allowOrigins) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        List<String> allowMethods = new ArrayList<>();
        allowMethods.add("OPTIONS");
        allowMethods.add("GET");
        allowMethods.add("POST");
        allowMethods.add("PUT");
        allowMethods.add("DELETE");
        corsConfiguration.setAllowedMethods(allowMethods);

        List<String> allowHeaders = new ArrayList<>();
        allowHeaders.add("x-login-url");
        allowHeaders.add("x-csrf-token");
        // 用户preflight OPTIONS请求会携带该header
        allowHeaders.add("Content-Type");
        corsConfiguration.setAllowedHeaders(allowHeaders);

        log.info("Add cors allowOrigins:{}", allowOrigins);
        for (String allowOrigin : allowOrigins) {
            corsConfiguration.addAllowedOrigin(allowOrigin);
        }
        corsConfiguration.addExposedHeader("x-login-url");
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);
        return corsConfiguration;
    }
}
