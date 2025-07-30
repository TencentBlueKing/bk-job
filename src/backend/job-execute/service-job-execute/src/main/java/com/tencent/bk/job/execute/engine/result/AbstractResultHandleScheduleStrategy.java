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
import com.tencent.bk.job.execute.config.ScheduleStrategyProperties;
import com.tencent.bk.job.execute.engine.model.ScheduleDelayRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
    private final List<ScheduleDelayRule> rules;

    /**
     * 超出规则的延迟时间（毫秒）
     */
    private final int finalDelay;

    /**
     * 默认最大延迟（毫秒）
     */
    protected final int DEFAULT_MAX_DELAY = 10000;

    protected AbstractResultHandleScheduleStrategy(ScheduleStrategyProperties.DelayConfig config) {
        if (config != null && config.getDelayRules() != null) {
            this.rules = parseRules(config.getDelayRules());
            this.finalDelay = config.getFinalDelay() > 0 ? config.getFinalDelay() : DEFAULT_MAX_DELAY;
            log.debug("Loaded schedule delay rules: {}, finalDelay: {}", rules, finalDelay);
        } else {
            this.rules = null;
            this.finalDelay = DEFAULT_MAX_DELAY;
        }
    }

    @Override
    public long getDelay() {
        int count = times.incrementAndGet();
        if (rules != null && !rules.isEmpty()) {
            for (ScheduleDelayRule rule : rules) {
                if (count <= rule.getMaxCount()) {
                    return rule.getDelay();
                }
            }
            return finalDelay;
        }
        return getDelayWithoutRules(count);
    }

    /**
     * 解析延迟规则
     */
    private List<ScheduleDelayRule> parseRules(String ruleStr) {
        if (StringUtils.isEmpty(ruleStr)) {
            return null;
        }
        try {
            return Arrays.stream(ruleStr.split(","))
                .map(this::parseRulePart)
                .sorted(Comparator.comparingInt(ScheduleDelayRule::getMaxCount))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse schedule delay rule, ruleStr:" + ruleStr, e);
            return null;
        }
    }

    private ScheduleDelayRule parseRulePart(String part) {
        String[] pair = part.trim().split(":");
        if (pair.length != 2) {
            throw new InternalException("Delay rule format is wrong, expected 'count:delay', actual is:{}" + part);
        }
        int maxCount = Integer.parseInt(pair[0].trim());
        int delay = Integer.parseInt(pair[1].trim());
        if (maxCount <= 0 || delay <= 0) {
            throw new InternalException("The delay rule is incorrect and must be greater than 0");
        }
        return new ScheduleDelayRule(maxCount, delay);
    }

    /**
     * 没有配置延迟规则，由子类具体实现
     */
    protected abstract long getDelayWithoutRules(int count);
}
