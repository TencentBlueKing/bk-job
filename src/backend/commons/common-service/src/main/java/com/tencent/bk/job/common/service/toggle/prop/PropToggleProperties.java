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

package com.tencent.bk.job.common.service.toggle.prop;

import com.tencent.bk.job.common.service.toggle.strategy.config.ToggleStrategyConfig;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 属性值动态切换开关配置
 */
@ConfigurationProperties(prefix = "job.toggle", ignoreInvalidFields = true)
@ToString
@Getter
@Setter
@Slf4j
public class PropToggleProperties {

    /**
     * key: prop_name; value: PropToggleConfig
     */
    private Map<String, PropToggleConfig> props = new HashMap<>();


    @Data
    @NoArgsConstructor
    public static class PropToggleConfig {
        private String defaultValue;

        private List<ConditionConfig> conditions;
    }

    @Data
    @NoArgsConstructor
    public static class ConditionConfig {
        private String value;
        private ToggleStrategyConfig strategy;
    }

    @PostConstruct
    public void print() {
        log.info("PropToggleProperties init: {}", JsonUtils.toJson(this));
    }
}
