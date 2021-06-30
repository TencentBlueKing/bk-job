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

package com.tencent.bk.job.gateway.predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.*;
import java.util.function.Predicate;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.putUriTemplateVariables;
import static org.springframework.http.server.PathContainer.parsePath;

/**
 * 作业平台自定义ESB-JOB-V2请求Route Predicate
 */
@Slf4j
@Component
public class JobEsbV2PathRoutePredicateFactory
    extends AbstractRoutePredicateFactory<JobEsbV2PathRoutePredicateFactory.Config> {
    private static final String PATTERN = "pattern";

    private PathPatternParser pathPatternParser = new PathPatternParser();

    public JobEsbV2PathRoutePredicateFactory() {
        super(Config.class);
    }

    private static void traceMatch(String prefix, Object desired, Object actual,
                                   boolean match) {
        if (log.isTraceEnabled()) {
            String message = String.format("%s \"%s\" %s against value \"%s\"", prefix,
                desired, match ? "matches" : "does not match", actual);
            log.trace(message);
        }
    }

    public void setPathPatternParser(PathPatternParser pathPatternParser) {
        this.pathPatternParser = pathPatternParser;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList(PATTERN);
    }

    @Override
    public Predicate<ServerWebExchange> apply(JobEsbV2PathRoutePredicateFactory.Config config) {
        final ArrayList<PathPattern> pathPatterns = new ArrayList<>();
        synchronized (this.pathPatternParser) {
            pathPatternParser.setMatchOptionalTrailingSeparator(
                true);
            pathPatterns.add(pathPatternParser.parse(config.getPattern()));
        }

        return exchange -> {
            PathContainer path = parsePath(exchange.getRequest().getURI().getRawPath());

            Optional<PathPattern> optionalPathPattern = pathPatterns.stream()
                .filter(pattern -> pattern.matches(path)).findFirst();

            if (optionalPathPattern.isPresent()) {
                PathPattern pathPattern = optionalPathPattern.get();
                traceMatch("Pattern", pathPattern.getPatternString(), path, true);
                PathPattern.PathMatchInfo pathMatchInfo = pathPattern.matchAndExtract(path);
                if (pathMatchInfo == null) {
                    return false;
                }
                String apiName = pathMatchInfo.getUriVariables().get("api_name");
                Map<String, String> routeVariables = new HashMap<>();
                routeVariables.put("api_name", apiName);
                putUriTemplateVariables(exchange, routeVariables);
                return true;
            } else {
                traceMatch("Pattern", config.getPattern(), path, false);
                return false;
            }
        };
    }

    @Validated
    public static class Config {
        private String pattern;

        String getPattern() {
            return pattern;
        }

        public Config setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }
}
