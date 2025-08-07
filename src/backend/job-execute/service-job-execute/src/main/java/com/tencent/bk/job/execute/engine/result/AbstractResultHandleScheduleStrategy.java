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

import com.tencent.bk.job.execute.config.PollingStrategyProperties;
import com.tencent.bk.job.execute.engine.model.ScheduleIntervalRule;
import lombok.extern.slf4j.Slf4j;

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
     * 默认最大的轮训间隔时间（毫秒）
     */
    protected final int DEFAULT_MAX_DELAY = 10000;

    protected AbstractResultHandleScheduleStrategy(PollingStrategyProperties.PollingConfig config) {
        if (config != null && config.getIntervalMap() != null) {
            this.rules = parseRules(config.getIntervalMap());
            this.finalInterval = config.getFinalInterval() > 0 ? config.getFinalInterval() : DEFAULT_MAX_DELAY;
            log.debug("Loaded polling rules: {}, finalInterval: {}", rules, finalInterval);
        } else {
            this.rules = null;
            this.finalInterval = DEFAULT_MAX_DELAY;
        }
    }

    /**
     * 解析轮轮询规则
     */
    private List<ScheduleIntervalRule> parseRules(Map<String, Integer> intervalMap) {
        if (intervalMap == null || intervalMap.isEmpty()) {
            return null;
        }
        // 微服务启动时，pollingStrategyProperties对象有合法性校验，这里不需要再次校验
        return intervalMap.entrySet().stream()
            .map(e -> parseMapEntry(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingInt(ScheduleIntervalRule::getStart))
            .collect(Collectors.toList());
    }

    private ScheduleIntervalRule parseMapEntry(String key, Integer value) {
        String[] parts = key.trim().split("-");
        return new ScheduleIntervalRule(Integer.parseInt(parts[0].trim()) , Integer.parseInt(parts[1].trim()), value);
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
     * 没有配置延迟规则，由子类具体实现
     */
    protected abstract long getDelayWithoutRules(int count);
}
