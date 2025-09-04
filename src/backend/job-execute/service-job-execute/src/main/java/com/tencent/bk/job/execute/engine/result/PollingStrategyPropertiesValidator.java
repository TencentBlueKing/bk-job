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

package com.tencent.bk.job.execute.engine.result;

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.execute.config.PollingStrategyProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 微服务启动时，校验用户配置的任务执行结果轮询策略是否合法，若不合法终止启动
 */
@Slf4j
@Component
public class PollingStrategyPropertiesValidator {

    private final PollingStrategyProperties properties;

    public PollingStrategyPropertiesValidator(PollingStrategyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        PollingStrategyProperties.PollingConfig filePollingConfig = properties.getFile();
        PollingStrategyProperties.PollingConfig scriptPollingConfig = properties.getScript();
        if (filePollingConfig != null) {
            log.info("Validating polling strategy config, filePollingConfig={}", filePollingConfig);
            validatePollingStrategy(filePollingConfig.getIntervalMap());
        }
        if (scriptPollingConfig != null) {
            log.info("Validating polling strategy config, scriptPollingConfig={}", scriptPollingConfig);
            validatePollingStrategy(scriptPollingConfig.getIntervalMap());
        }
    }

    /**
     * 校验轮训策略的合法性
     */
    private void validatePollingStrategy(Map<String, Integer> intervalMap) {
        if (intervalMap == null || intervalMap.isEmpty()) {
            return ;
        }
        intervalMap.forEach(this::validateStrategyEntry);
    }

    private void validateStrategyEntry(String key, Integer value) {
        if (StringUtils.isBlank(key) || value == null) {
            throw new InternalException("Polling rule configuration error, key or value is empty, key=" + key + ", " +
                "value=" + value);
        }
        if (value <= 0) {
            throw new InternalException("Polling rule configuration error, interval must > 0, interval: " + value);
        }
        if (!key.contains("-")) {
            throw new InternalException("Polling rule configuration error, missing separator '-', key: " + key);
        }

        String[] parts = key.trim().split("-");
        if (parts.length != 2) {
            throw new InternalException("Polling rule configuration error, the interval format should be " +
                "'start-end', actual:" + key);
        }
        int start = Integer.parseInt(parts[0].trim());
        int end = Integer.parseInt(parts[1].trim());
        if (start <= 0 || end <= 0 || end < start) {
            throw new InternalException(String.format("Polling rule configuration error, start must be > 0 and " +
                "end >= start, start='%s', end='%s'", start, end));
        }
    }
}
