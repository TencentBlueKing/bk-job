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

package com.tencent.bk.job.common.service.feature.strategy;

import com.tencent.bk.job.common.util.feature.ToggleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 组合策略抽象实现
 */
@Slf4j
public abstract class AbstractCompositeToggleStrategy extends AbstractToggleStrategy {
    /**
     * 组合策略
     */
    protected final List<ToggleStrategy> compositeStrategies;

    public AbstractCompositeToggleStrategy(String strategyId,
                                           List<ToggleStrategy> compositeStrategies,
                                           Map<String, String> initParams) {
        super(strategyId, initParams);
        this.compositeStrategies = compositeStrategies;
        assertRequiredAtLeastOneStrategy();
    }

    protected void assertRequiredAtLeastOneStrategy() {
        if (CollectionUtils.isEmpty(this.compositeStrategies)) {
            String msg = "Required at least one strategy for this ToggleStrategy";
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
    }
}
