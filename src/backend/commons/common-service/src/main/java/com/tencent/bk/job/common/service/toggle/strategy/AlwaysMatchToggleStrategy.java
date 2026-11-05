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

package com.tencent.bk.job.common.service.toggle.strategy;

import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 全量命中策略：不做任何条件判断，恒定返回开启。
 *
 * <p>用于「全量放开」场景，例如 IAM v4 灰度收尾后配置该策略即表示所有用户始终走 v4。</p>
 */
public class AlwaysMatchToggleStrategy extends AbstractToggleStrategy {
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "AlwaysMatchToggleStrategy";

    public AlwaysMatchToggleStrategy(Map<String, String> initParams) {
        super(STRATEGY_ID, initParams);
    }

    @Override
    public boolean evaluate(String toggleName, ToggleEvaluateContext ctx) {
        return true;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AlwaysMatchToggleStrategy.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("initParams=" + initParams)
            .toString();
    }
}
