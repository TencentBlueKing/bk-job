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
import com.tencent.bk.job.execute.engine.model.ScheduleIntervalRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 抽象任务调度策略
 */
@Slf4j
public abstract class AbstractResultHandleScheduleStrategy implements ScheduleStrategy {
    /**
     * 任务累计执行次数
     */
    protected final AtomicInteger times = new AtomicInteger(0);

    /**
     * 调度延迟规则
     */
    private final List<ScheduleIntervalRule> rules;

    /**
     * 超出规则的延迟时间（毫秒）
     */
    private final int finalInterval;

    /**
     * 默认最大延迟（毫秒）
     */
    protected final int DEFAULT_MAX_DELAY = 10000;

    protected AbstractResultHandleScheduleStrategy(PollingStrategyProperties.PollingConfig config) {
        if (config != null && config.getIntervalMap() != null) {
            this.rules = parseRules(config.getIntervalMap());
            this.finalInterval = config.getFinalInterval() > 0 ? config.getFinalInterval() : DEFAULT_MAX_DELAY;
            log.debug("Loaded rolling rules: {}, finalInterval: {}", rules, finalInterval);
        } else {
            this.rules = null;
            this.finalInterval = DEFAULT_MAX_DELAY;
        }
    }

    @Override
    public long getDelay() {
        int count = times.incrementAndGet();
        if (rules != null && !rules.isEmpty()) {
            for (ScheduleIntervalRule rule : rules) {
                if (rule.matches(count)) {
                    return rule.getInterval();
                }
            }
            return finalInterval;
        }
        return getDelayWithoutRules(count);
    }

    /**
     * 解析轮训规则
     */
    private List<ScheduleIntervalRule> parseRules(Map<String, Integer> intervalMap) {
        if (intervalMap == null || intervalMap.isEmpty()) {
            return null;
        }
        try {
            List<ScheduleIntervalRule> rules = intervalMap.entrySet().stream()
                .map(e -> parseMapEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(ScheduleIntervalRule::getStart))
                .collect(Collectors.toList());
            return rules;
        } catch (Exception e) {
            log.error("Parse polling rule configuration error.", e);
            return null;
        }
    }

    private static ScheduleIntervalRule parseMapEntry(String key, Integer value) {
        if (StringUtils.isBlank(key) || value == null) {
            throw new InternalException("Polling rule configuration error, key or value is empty, key=" + key + ", " +
                "value=" + value);
        }
        int interval = value;
        if (interval <= 0) {
            throw new InternalException("Polling rule configuration error, interval must > 0, interval: " + interval);
        }
        if (key.contains("-")) {
            String[] parts = key.trim().split("-");
            if (parts.length != 2) {
                throw new InternalException("Polling rule configuration error, the interval format should be " +
                    "'start~end', actual:" + key);
            }
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            if (start <= 0 || end <= 0 || end < start) {
                throw new InternalException(String.format("Polling rule configuration error, start must be > 0 and " +
                    "end >= start, start='%s', end='%s'", start, end));
            }
            return new ScheduleIntervalRule(start, end, interval);
        } else {
            throw new InternalException("Polling rule configuration error, missing separator '-', key: " + key);
        }
    }

    /**
     * 没有配置延迟规则，由子类具体实现
     */
    protected abstract long getDelayWithoutRules(int count);
}
